package com.example.blehr.data.heartrate

import com.example.blehr.R
import com.example.blehr.ble.HeartRateSample
import com.example.blehr.ble.ScannedDevice
import com.example.blehr.core.text.UiText
import com.example.blehr.core.text.uiText

enum class ConnectionPhase {
    IDLE,
    SCANNING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    ERROR,
}

data class ErrorEntry(
    val reason: UiText,
    val rawHex: String,
    val atMillis: Long,
)

data class HeartRateState(
    val scanning: Boolean = false,
    val devices: List<ScannedDevice> = emptyList(),
    val connection: ConnectionPhase = ConnectionPhase.IDLE,
    val connectedName: String? = null,
    val connectedAddress: String? = null,
    val status: UiText = uiText(R.string.status_idle),
    val sample: HeartRateSample? = null,
    val history: List<Int> = emptyList(),
    val lastRawHex: String = "",
    val scanError: UiText? = null,
    val parseErrorCount: Int = 0,
    val errors: List<ErrorEntry> = emptyList(),
) {
    internal fun withoutLiveReadings() = copy(sample = null, history = emptyList())
}
