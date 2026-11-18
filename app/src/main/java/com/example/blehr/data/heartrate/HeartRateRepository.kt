package com.example.blehr.data.heartrate

import android.content.Context
import com.example.blehr.R
import com.example.blehr.ble.BleEvent
import com.example.blehr.ble.BleHeartRateClient
import com.example.blehr.ble.HeartRateSample
import com.example.blehr.ble.ScannedDevice
import com.example.blehr.ble.SensorContact
import com.example.blehr.core.text.UiText
import com.example.blehr.core.text.uiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide BLE state shared by the Compose screen and the overlay service.
 * It must not be tied to an Activity, otherwise the overlay loses its stream
 * when the Activity is backgrounded or destroyed.
 */
class HeartRateRepository private constructor(context: Context) {
    companion object {
        @Volatile
        private var instance: HeartRateRepository? = null

        fun get(context: Context): HeartRateRepository {
            return instance ?: synchronized(this) {
                instance ?: HeartRateRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val recentDevice = RecentHeartRateDeviceStore(context)
    private val startupReconnect = StartupReconnect()
    private val client = BleHeartRateClient(context) { event -> onBleEvent(event) }
    private val _ui = MutableStateFlow(HeartRateState())
    val ui: StateFlow<HeartRateState> = _ui.asStateFlow()

    fun reconnectAtStartup() {
        startupReconnect.tryOnce {
            val device = recentDevice.read() ?: return@tryOnce false
            client.connectRecent(device.address, device.name, device.addressType)
        }
    }

    fun startScan() {
        if (startupReconnect.finish()) client.disconnect()
        _ui.update {
            it.copy(
                scanning = true,
                devices = emptyList(),
                status = uiText(R.string.status_scanning),
                connection = if (it.connection == ConnectionPhase.CONNECTED) {
                    it.connection
                } else {
                    ConnectionPhase.SCANNING
                },
            )
        }
        client.startScan()
    }

    fun stopScan() {
        client.stopScan()
    }

    fun connect(device: ScannedDevice) {
        startupReconnect.finish()
        _ui.update {
            it.copy(
                scanning = false,
                connection = ConnectionPhase.CONNECTING,
                connectedName = device.name,
                connectedAddress = device.address,
                status = uiText(R.string.status_connecting, device.name ?: device.address),
                sample = null,
                history = emptyList(),
                lastRawHex = "",
            )
        }
        client.connect(device)
    }

    fun disconnect() {
        startupReconnect.finish()
        client.disconnect()
        _ui.update {
            it.copy(
                connection = ConnectionPhase.DISCONNECTED,
                status = uiText(R.string.status_disconnected),
            )
        }
    }

    fun shutdown() {
        startupReconnect.finish()
        client.close()
    }

    private fun onBleEvent(event: BleEvent) {
        when (event) {
            is BleEvent.ScanResults -> {
                val filtered = event.devices
                    .filter { !it.name.isNullOrBlank() || it.hasHeartRateService }
                    .sortedWith(
                        compareByDescending<ScannedDevice> { it.hasHeartRateService }
                            .thenByDescending { it.rssi }
                    )
                    .take(25)
                _ui.update { it.copy(devices = filtered) }
            }

            is BleEvent.ScanFinished -> _ui.update {
                it.copy(
                    scanning = false,
                    connection = if (it.connection == ConnectionPhase.SCANNING) {
                        ConnectionPhase.IDLE
                    } else {
                        it.connection
                    },
                    status = if (it.connection == ConnectionPhase.CONNECTED) {
                        uiText(R.string.status_waiting)
                    } else {
                        event.message
                    },
                )
            }

            is BleEvent.ScanFailed -> _ui.update {
                it.copy(
                    scanning = false,
                    connection = ConnectionPhase.ERROR,
                    status = event.message,
                )
            }

            is BleEvent.Connecting -> _ui.update {
                it.copy(
                    connection = ConnectionPhase.CONNECTING,
                    connectedName = event.name,
                    connectedAddress = event.address,
                    status = uiText(R.string.status_connecting, event.name ?: event.address),
                )
            }

            is BleEvent.Connected -> {
                startupReconnect.finish()
                val name = event.name ?: _ui.value.connectedName
                recentDevice.save(RecentHeartRateDevice(event.address, name, event.addressType))
                _ui.update {
                    it.copy(
                        connection = ConnectionPhase.CONNECTED,
                        connectedName = name,
                        connectedAddress = event.address,
                        status = uiText(R.string.status_subscribed),
                        sample = null,
                        history = emptyList(),
                        lastRawHex = "",
                    )
                }
            }

            is BleEvent.Disconnected -> {
                if (startupReconnect.finish()) {
                    showStartupReconnectFailure()
                } else {
                    _ui.update {
                        it.copy(
                            connection = ConnectionPhase.DISCONNECTED,
                            status = uiText(R.string.status_reconnect, event.message),
                        )
                    }
                }
            }

            is BleEvent.Notification -> {
                val sample = event.sample
                _ui.update { state ->
                    val history = (state.history + sample.bpm).takeLast(90)
                    state.copy(
                        sample = sample,
                        history = history,
                        lastRawHex = sample.rawHex,
                        status = statusFor(sample),
                    )
                }
            }

            is BleEvent.BadPacket -> _ui.update { state ->
                val entry = ErrorEntry(event.reason, event.rawHex, System.currentTimeMillis())
                state.copy(
                    parseErrorCount = state.parseErrorCount + 1,
                    errors = (listOf(entry) + state.errors).take(5),
                    lastRawHex = event.rawHex,
                    status = uiText(R.string.status_bad_packet),
                )
            }

            is BleEvent.Error -> {
                if (startupReconnect.finish()) {
                    showStartupReconnectFailure()
                } else {
                    _ui.update {
                        it.copy(
                            scanning = false,
                            connection = ConnectionPhase.ERROR,
                            status = event.message,
                        )
                    }
                }
            }
        }
    }

    private fun showStartupReconnectFailure() {
        _ui.update {
            it.copy(
                connection = ConnectionPhase.IDLE,
                connectedName = null,
                connectedAddress = null,
                status = uiText(R.string.status_startup_reconnect_failed),
            )
        }
    }

    private fun statusFor(sample: HeartRateSample): UiText = when {
        sample.sensorContact == SensorContact.NOT_DETECTED -> uiText(R.string.status_no_contact)
        sample.bpm == 0 -> uiText(R.string.status_zero_bpm)
        sample.warnings.isNotEmpty() -> uiText(R.string.status_warnings, sample.warnings.size)
        else -> uiText(R.string.status_receiving)
    }
}
