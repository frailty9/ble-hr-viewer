package com.example.blehr.ble

import java.util.Locale

enum class SensorContact {
    NOT_SUPPORTED,
    NOT_DETECTED,
    DETECTED,
}

enum class DiagnosticCode {
    EMPTY_NOTIFICATION,
    TRUNCATED_HEART_RATE,
    RESERVED_CONTACT,
    TRUNCATED_ENERGY,
    UNKNOWN_ENERGY,
    MISSING_RR,
    ODD_RR_BYTE,
    TRAILING_BYTES,
    RESERVED_FLAGS,
    IMPLAUSIBLE_HEART_RATE,
    ZERO_HEART_RATE,
}

/** Structured parser output; no Android dependencies or pretranslated text. */
data class HeartRateDiagnostic(
    val code: DiagnosticCode,
    val arguments: List<Any> = emptyList(),
)

data class HeartRateSample(
    val bpm: Int,
    val sensorContact: SensorContact,
    val energyExpendedKj: Int?,
    val rrIntervalsRaw: List<Int>,
    val flags: Int,
    val raw: ByteArray,
    val warnings: List<HeartRateDiagnostic>,
    val receivedAtMillis: Long,
) {
    val rrIntervalsMs: List<Double>
        get() = rrIntervalsRaw.map { it * 1000.0 / 1024.0 }

    val rawHex: String
        get() = HeartRateParser.hex(raw)
}

class HeartRateParseException(
    val reason: HeartRateDiagnostic,
    val raw: ByteArray,
) : Exception(reason.toString())

/**
 * Decoder for the Bluetooth SIG Heart Rate Measurement characteristic (0x2A37).
 *
 * Truncated packets raise [HeartRateParseException]. Packets that carry extra or
 * reserved data still produce a sample and add human-readable warnings, because
 * apps should surface bad input instead of silently dropping the stream.
 */
object HeartRateParser {

    fun parse(
        payload: ByteArray,
        receivedAtMillis: Long = System.currentTimeMillis(),
    ): HeartRateSample {
        if (payload.isEmpty()) {
            throw HeartRateParseException(HeartRateDiagnostic(DiagnosticCode.EMPTY_NOTIFICATION), payload)
        }

        val flags = payload[0].toInt() and 0xFF
        val warnings = mutableListOf<HeartRateDiagnostic>()
        var index = 1

        // bit 0: 0 = uint8, 1 = uint16 little-endian
        val valueWidth = if (flags and 0x01 != 0) 2 else 1
        if (payload.size < index + valueWidth) {
            throw HeartRateParseException(
                HeartRateDiagnostic(DiagnosticCode.TRUNCATED_HEART_RATE, listOf(index + valueWidth, payload.size)),
                payload,
            )
        }
        var bpm = 0
        for (byteIndex in 0 until valueWidth) {
            bpm = bpm or ((payload[index + byteIndex].toInt() and 0xFF) shl (8 * byteIndex))
        }
        index += valueWidth

        // bits 2:1: 0/1 = not supported, 2 = supported but no contact, 3 = detected
        val contactBits = (flags shr 1) and 0x03
        val contact = when (contactBits) {
            0, 1 -> SensorContact.NOT_SUPPORTED
            2 -> SensorContact.NOT_DETECTED
            else -> SensorContact.DETECTED
        }
        if (contactBits == 1) {
            warnings += HeartRateDiagnostic(DiagnosticCode.RESERVED_CONTACT)
        }

        // bit 3: energy expended, uint16 kilojoules
        var energy: Int? = null
        if (flags and 0x08 != 0) {
            if (payload.size < index + 2) {
                throw HeartRateParseException(
                    HeartRateDiagnostic(DiagnosticCode.TRUNCATED_ENERGY, listOf(payload.size - index)),
                    payload,
                )
            }
            energy = readLe16(payload, index)
            index += 2
            if (energy == 0xFFFF) {
                warnings += HeartRateDiagnostic(DiagnosticCode.UNKNOWN_ENERGY)
            }
        }

        // bit 4: one or more RR intervals in units of 1/1024 second
        val rrValues = mutableListOf<Int>()
        if (flags and 0x10 != 0) {
            val remaining = payload.size - index
            val wholeIntervals = remaining / 2
            if (wholeIntervals == 0) {
                warnings += HeartRateDiagnostic(DiagnosticCode.MISSING_RR)
            }
            repeat(wholeIntervals) {
                rrValues += readLe16(payload, index)
                index += 2
            }
            if (remaining % 2 != 0) {
                warnings += HeartRateDiagnostic(DiagnosticCode.ODD_RR_BYTE)
                index = payload.size
            }
        }

        if (index < payload.size) {
            warnings += HeartRateDiagnostic(DiagnosticCode.TRAILING_BYTES, listOf(payload.size - index))
        }

        val reserved = flags and 0xE0
        if (reserved != 0) {
            warnings += HeartRateDiagnostic(DiagnosticCode.RESERVED_FLAGS, listOf(reserved))
        }
        if (bpm > 250) {
            warnings += HeartRateDiagnostic(DiagnosticCode.IMPLAUSIBLE_HEART_RATE, listOf(bpm))
        }
        if (bpm == 0) {
            warnings += HeartRateDiagnostic(DiagnosticCode.ZERO_HEART_RATE)
        }

        return HeartRateSample(
            bpm = bpm,
            sensorContact = contact,
            energyExpendedKj = energy,
            rrIntervalsRaw = rrValues,
            flags = flags,
            raw = payload,
            warnings = warnings,
            receivedAtMillis = receivedAtMillis,
        )
    }

    private fun readLe16(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    fun hex(data: ByteArray, limit: Int? = null): String {
        val visible = if (limit != null && data.size > limit) {
            data.copyOf(limit)
        } else {
            data
        }
        val text = visible.joinToString(" ") { "%02x".format(Locale.ROOT, it.toInt() and 0xFF) }
        return if (limit != null && data.size > limit) {
            "$text ... (${data.size} bytes)"
        } else {
            text
        }
    }
}
