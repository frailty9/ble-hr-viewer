package com.example.blehr.ble



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
