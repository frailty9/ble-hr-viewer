package com.example.blehr.ble

import android.bluetooth.BluetoothDevice
import com.example.blehr.core.text.UiText

data class ScannedDevice(
    val device: BluetoothDevice,
    val name: String?,
    val address: String,
    val rssi: Int,
    val hasHeartRateService: Boolean,
)

sealed interface BleEvent {
    data class ScanResults(val devices: List<ScannedDevice>) : BleEvent
    data class ScanFinished(val message: UiText) : BleEvent
    data class ScanFailed(val message: UiText) : BleEvent
    data class Connecting(val name: String?, val address: String) : BleEvent
    data class Connected(
        val name: String?,
        val address: String,
        val services: List<String>,
    ) : BleEvent
    data class Disconnected(val message: UiText) : BleEvent
    data class Notification(val sample: HeartRateSample) : BleEvent
    data class BadPacket(val reason: UiText, val rawHex: String) : BleEvent
    data class Error(val message: UiText) : BleEvent
}
