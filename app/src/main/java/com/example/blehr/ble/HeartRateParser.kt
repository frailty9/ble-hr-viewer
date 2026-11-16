package com.example.blehr.ble

import java.util.Locale

enum class SensorContact {
    NOT_SUPPORTED,
    NOT_DETECTED,
    DETECTED,
}

data class HeartRateSample(
    val bpm: Int,
    val sensorContact: SensorContact,
    val energyExpendedKj: Int?,
    val rrIntervalsRaw: List<Int>,
    val flags: Int,
    val raw: ByteArray,
    val warnings: List<String>,
    val receivedAtMillis: Long,
) {
    val rrIntervalsMs: List<Double>
        get() = rrIntervalsRaw.map { it * 1000.0 / 1024.0 }

    val rawHex: String
        get() = HeartRateParser.hex(raw)
}

class HeartRateParseException(
    val reason: String,
    val raw: ByteArray,
) : Exception(reason)

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
            throw HeartRateParseException("empty notification", payload)
        }

        val flags = payload[0].toInt() and 0xFF
        val warnings = mutableListOf<String>()
        var index = 1

        // bit 0: 0 = uint8, 1 = uint16 little-endian
        val valueWidth = if (flags and 0x01 != 0) 2 else 1
        if (payload.size < index + valueWidth) {
            throw HeartRateParseException(
                "truncated heart rate value: need ${index + valueWidth} bytes, got ${payload.size}",
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
            warnings += "sensor-contact bits 0b01 treated as not supported"
        }

        // bit 3: energy expended, uint16 kilojoules
        var energy: Int? = null
        if (flags and 0x08 != 0) {
            if (payload.size < index + 2) {
                throw HeartRateParseException(
                    "energy-expended flag set but only ${payload.size - index} byte(s) left",
                    payload,
                )
            }
            energy = readLe16(payload, index)
            index += 2
            if (energy == 0xFFFF) {
                warnings += "energy expended is 0xFFFF (unknown)"
            }
        }

        // bit 4: one or more RR intervals in units of 1/1024 second
        val rrValues = mutableListOf<Int>()
        if (flags and 0x10 != 0) {
            val remaining = payload.size - index
            val wholeIntervals = remaining / 2
            if (wholeIntervals == 0) {
                warnings += "RR-interval flag set but no complete RR value present"
            }
            repeat(wholeIntervals) {
                rrValues += readLe16(payload, index)
                index += 2
            }
            if (remaining % 2 != 0) {
                warnings += "odd trailing byte after RR intervals"
                index = payload.size
            }
        }

        if (index < payload.size) {
            warnings += "${payload.size - index} trailing byte(s) ignored"
        }

        val reserved = flags and 0xE0
        if (reserved != 0) {
            warnings += "reserved flag bits set: 0x%02x".format(Locale.ROOT, reserved)
        }
        if (bpm > 250) {
            warnings += "implausible heart rate: $bpm bpm"
        }
        if (bpm == 0) {
            warnings += "heart rate is 0 bpm (no pulse detected)"
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
