package com.example.blehr

import android.content.Context
import com.example.blehr.ble.BleEvent
import com.example.blehr.ble.BleHeartRateClient
import com.example.blehr.ble.HeartRateSample
import com.example.blehr.ble.ScannedDevice
import com.example.blehr.ble.SensorContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ConnectionPhase {
    IDLE,
    SCANNING,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR,
}

data class ErrorEntry(
    val reason: String,
    val rawHex: String,
    val atMillis: Long,
)

data class HeartRateUiState(
    val scanning: Boolean = false,
    val devices: List<ScannedDevice> = emptyList(),
    val connection: ConnectionPhase = ConnectionPhase.IDLE,
    val connectedName: String? = null,
    val connectedAddress: String? = null,
    val status: String = "点击“扫描 BLE”搜索手环",
    val sample: HeartRateSample? = null,
    val history: List<Int> = emptyList(),
    val lastRawHex: String = "",
    val parseErrorCount: Int = 0,
    val errors: List<ErrorEntry> = emptyList(),
)

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

    private val client = BleHeartRateClient(context) { event -> onBleEvent(event) }
    private val _ui = MutableStateFlow(HeartRateUiState())
    val ui: StateFlow<HeartRateUiState> = _ui.asStateFlow()

    fun startScan() {
        _ui.update {
            it.copy(
                scanning = true,
                devices = emptyList(),
                status = "正在扫描附近 BLE 设备…",
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
        _ui.update {
            it.copy(
                scanning = false,
                connection = ConnectionPhase.CONNECTING,
                connectedName = device.name,
                connectedAddress = device.address,
                status = "正在连接 ${device.name ?: device.address}…",
                sample = null,
                history = emptyList(),
                lastRawHex = "",
            )
        }
        client.connect(device)
    }

    fun disconnect() {
        client.disconnect()
        _ui.update {
            it.copy(
                connection = ConnectionPhase.DISCONNECTED,
                status = "已断开连接",
            )
        }
    }

    fun shutdown() {
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
                        "已连接，等待心率通知"
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
                    status = "正在连接 ${event.name}…",
                )
            }

            is BleEvent.Connected -> _ui.update {
                it.copy(
                    connection = ConnectionPhase.CONNECTED,
                    connectedName = event.name,
                    connectedAddress = event.address,
                    status = "已订阅心率通知，等待数据（未佩戴时可能没有通知）",
                    sample = null,
                    history = emptyList(),
                    lastRawHex = "",
                )
            }

            is BleEvent.Disconnected -> _ui.update {
                it.copy(
                    connection = ConnectionPhase.DISCONNECTED,
                    status = "${event.message}，可重新连接",
                )
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
                    status = "收到无法解析的包，已忽略并继续监听",
                )
            }

            is BleEvent.Error -> _ui.update {
                it.copy(
                    scanning = false,
                    connection = ConnectionPhase.ERROR,
                    status = event.message,
                )
            }
        }
    }

    private fun statusFor(sample: HeartRateSample): String = when {
        sample.sensorContact == SensorContact.NOT_DETECTED -> "未佩戴，心率可能无效"
        sample.bpm == 0 -> "收到 0 bpm（未佩戴/无脉搏）"
        sample.warnings.isNotEmpty() -> "接收中 · 告警 ${sample.warnings.size}"
        else -> "接收中"
    }
}
