package io.github.frailty9.blehr.data.heartrate

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.ble.BleEvent
import io.github.frailty9.blehr.ble.BleHeartRateClient
import io.github.frailty9.blehr.ble.HeartRateSample
import io.github.frailty9.blehr.ble.ScannedDevice
import io.github.frailty9.blehr.ble.SensorContact
import io.github.frailty9.blehr.core.text.UiText
import io.github.frailty9.blehr.core.text.uiText
import io.github.frailty9.blehr.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        private val activePhases = setOf(
            ConnectionPhase.CONNECTING,
            ConnectionPhase.CONNECTED,
            ConnectionPhase.RECONNECTING,
        )

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
    private val settings = SettingsRepository.get(context)
    private val handler = Handler(Looper.getMainLooper())
    private var recoveryDevice: BluetoothDevice? = null
    private val recovery = HeartRateRecovery(
        nowMillis = SystemClock::elapsedRealtime,
        schedule = { task, delay -> handler.postDelayed(task, delay) },
        cancel = handler::removeCallbacks,
        timeoutMillis = { settings.heartRateTimeoutSeconds.value * 1_000L },
        onDataTimeout = ::handleDataTimeout,
        reconnect = ::startRecoveryAttempt,
        onRecoveryFailed = { showDisconnected(uiText(R.string.status_reconnect_exhausted)) },
    )

    init {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            settings.heartRateTimeoutSeconds.collect { recovery.checkDeadline() }
        }
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED && !client.canConnect() &&
                        (_ui.value.connection in activePhases || _ui.value.scanning)
                    ) {
                        startupReconnect.finish()
                        recovery.reset()
                        client.close()
                        _ui.update { it.copy(scanning = false) }
                        showDisconnected(uiText(R.string.bluetooth_disabled))
                    } else if (intent.action == Intent.ACTION_SCREEN_ON) {
                        recovery.checkDeadline()
                    }
                }
            },
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    fun reconnectAtStartup() {
        recovery.checkDeadline()
        startupReconnect.tryOnce {
            val device = recentDevice.read() ?: return@tryOnce false
            client.connectRecent(device.address, device.name, device.addressType)
        }
    }

    fun startScan() {
        val startupActive = startupReconnect.finish()
        if (startupActive || recovery.isRecovering || _ui.value.connection == ConnectionPhase.CONNECTING) {
            recovery.reset()
            recoveryDevice = null
            client.disconnect()
        }
        _ui.update {
            it.copy(
                scanning = true,
                devices = emptyList(),
                scanError = null,
                status = if (it.connection == ConnectionPhase.CONNECTED) it.status else uiText(R.string.status_scanning),
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
        recovery.reset()
        recoveryDevice = null
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
                scanError = null,
            )
        }
        client.connect(device)
    }

    fun disconnect() {
        startupReconnect.finish()
        recovery.reset()
        client.disconnect()
        showDisconnected(uiText(R.string.status_disconnected))
    }

    fun clearSession() {
        startupReconnect.finish()
        recovery.reset()
        recoveryDevice = null
        client.close()
        _ui.value = HeartRateState()
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
                    status = if (it.connection == ConnectionPhase.SCANNING) event.message else it.status,
                )
            }

            is BleEvent.ScanFailed -> _ui.update {
                if (it.connection == ConnectionPhase.SCANNING) {
                    it.copy(scanning = false, connection = ConnectionPhase.ERROR, status = event.message)
                } else {
                    it.copy(scanning = false, scanError = event.message)
                }
            }

            is BleEvent.Connecting -> _ui.update {
                it.copy(
                    connection = if (recovery.isRecovering) ConnectionPhase.RECONNECTING else ConnectionPhase.CONNECTING,
                    connectedName = event.name,
                    connectedAddress = event.address,
                    status = if (recovery.isRecovering) it.status else {
                        uiText(R.string.status_connecting, event.name ?: event.address)
                    },
                )
            }

            is BleEvent.Connected -> {
                startupReconnect.finish()
                val name = event.name ?: _ui.value.connectedName
                recoveryDevice = event.device
                recentDevice.save(RecentHeartRateDevice(event.address, name, event.addressType))
                _ui.update {
                    it.copy(
                        connection = if (recovery.isRecovering) ConnectionPhase.RECONNECTING else ConnectionPhase.CONNECTED,
                        connectedName = name,
                        connectedAddress = event.address,
                        status = if (recovery.isRecovering) it.status else uiText(R.string.status_subscribed),
                        sample = null,
                        history = emptyList(),
                    )
                }
                recovery.subscribed()
            }

            is BleEvent.Disconnected -> {
                handleConnectionEnd(uiText(R.string.status_reconnect, event.message), ConnectionPhase.DISCONNECTED)
            }

            is BleEvent.Notification -> {
                if (!recovery.isMonitoring) return
                recovery.receivedReading()
                val sample = event.sample
                _ui.update { state ->
                    val history = (state.history + sample.bpm).takeLast(90)
                    state.copy(
                        sample = sample,
                        connection = ConnectionPhase.CONNECTED,
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
                )
            }

            is BleEvent.Error -> {
                handleConnectionEnd(event.message, ConnectionPhase.ERROR)
            }
        }
    }

    private fun handleDataTimeout(): Boolean {
        client.disconnect()
        if (_ui.value.scanning || !client.canConnect() || recoveryDevice == null) {
            showDisconnected(uiText(R.string.status_data_timeout))
            return false
        }
        _ui.update { it.withoutLiveReadings() }
        return true
    }

    private fun startRecoveryAttempt(attempt: Int): Boolean {
        val device = recoveryDevice ?: return false
        _ui.update {
            it.copy(
                connection = ConnectionPhase.RECONNECTING,
                status = uiText(R.string.status_reconnecting, attempt, MAX_RECONNECT_ATTEMPTS),
            )
        }
        return client.reconnect(device, _ui.value.connectedName)
    }

    private fun handleConnectionEnd(message: UiText, phase: ConnectionPhase) {
        if (recovery.isRecovering && !client.canConnect()) {
            recovery.reset()
            showDisconnected(message)
        } else if (!recovery.connectionEnded()) {
            if (startupReconnect.finish()) {
                showDisconnected(uiText(R.string.status_startup_reconnect_failed), ConnectionPhase.IDLE)
            } else {
                showDisconnected(message, phase)
            }
        }
    }

    private fun showDisconnected(message: UiText, phase: ConnectionPhase = ConnectionPhase.DISCONNECTED) {
        recoveryDevice = null
        _ui.update {
            it.withoutLiveReadings().copy(
                connection = phase,
                connectedName = null,
                connectedAddress = null,
                status = message,
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
