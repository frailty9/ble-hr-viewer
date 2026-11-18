package io.github.frailty9.blehr.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.core.text.asUiText
import io.github.frailty9.blehr.core.text.uiText
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal GATT client for the standard Heart Rate Service.
 *
 * Scan -> connect -> discover 0x180D -> notify on 0x2A37. Every notification is
 * decoded even when the packet is malformed; malformed packets are reported as
 * [BleEvent.BadPacket] and never tear down the connection.
 */
@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
class BleHeartRateClient(
    context: Context,
    private val onEvent: (BleEvent) -> Unit,
) {
    companion object {
        private const val TAG = "BleHeartRate"
        const val SCAN_TIMEOUT_MS = 12_000L
        const val SCAN_BATCH_MS = 500L
        const val CONNECT_TIMEOUT_MS = 20_000L
        private const val DIRECT_CONNECT_TIMEOUT_MS = 10_000L

        // LOW_LATENCY is the known-good discovery mode. UI updates are batched below.
        private const val SCAN_MODE = ScanSettings.SCAN_MODE_LOW_LATENCY

        val HR_SERVICE_UUID: UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HR_MEASUREMENT_UUID: UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter get() = bluetoothManager?.adapter

    @Volatile
    private var scanner = false
    private var gatt: BluetoothGatt? = null
    private var connectedEmitted = false
    private val scanResults = ConcurrentHashMap<String, ScannedDevice>()
    private val scanFlushScheduled = AtomicBoolean(false)
    private val scanFlushRunnable = Runnable {
        scanFlushScheduled.set(false)
        emitScanSnapshot()
    }

    private val scanTimeout = Runnable {
        if (scanner) {
            stopScanInternal()
            onEvent(BleEvent.ScanFinished(uiText(R.string.scan_finished)))
        }
    }

    private val connectTimeout = Runnable {
        if (gatt != null && !connectedEmitted) {
            closeGatt()
            onEvent(BleEvent.Error(uiText(R.string.connection_timeout)))
        }
    }

    internal fun canConnect(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return false
        return try {
            adapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!scanner) return
            addScanResult(result)
            scheduleScanFlush()
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            if (!scanner) return
            results.forEach(::addScanResult)
            scheduleScanFlush()
        }

        override fun onScanFailed(errorCode: Int) {
            if (!scanner) return
            scanner = false
            handler.removeCallbacks(scanTimeout)
            handler.removeCallbacks(scanFlushRunnable)
            scanFlushScheduled.set(false)
            Log.w(TAG, "BLE scan failed: errorCode=$errorCode")
            onEvent(BleEvent.ScanFailed(uiText(R.string.scan_failed, errorCode)))
        }
    }

    private fun addScanResult(result: ScanResult) {
        val record = result.scanRecord
        val uuids = record?.serviceUuids?.map { it.uuid } ?: emptyList()
        val hasHr = uuids.contains(HR_SERVICE_UUID) ||
            record?.getServiceData(ParcelUuid(HR_SERVICE_UUID)) != null
        val name = record?.deviceName ?: safeName(result.device)
        val address = try {
            result.device.address
        } catch (exc: SecurityException) {
            Log.w(TAG, "scan result address unavailable (BLUETOOTH_CONNECT?)", exc)
            return
        }
        scanResults[address] = ScannedDevice(
            device = result.device,
            name = name,
            address = address,
            rssi = result.rssi,
            hasHeartRateService = hasHr,
        )
        if (scanResults.size == 1) {
            Log.d(TAG, "first scan result: $address name=$name hr=$hasHr")
        }
    }

    private fun scheduleScanFlush() {
        if (scanFlushScheduled.compareAndSet(false, true)) {
            handler.postDelayed(scanFlushRunnable, SCAN_BATCH_MS)
        }
    }

    private fun emitScanSnapshot() {
        if (scanResults.isEmpty()) return
        val snapshot = scanResults.values.toList()
        Log.d(TAG, "scan snapshot: ${snapshot.size} devices")
        onEvent(BleEvent.ScanResults(snapshot))
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int,
        ) {
            if (gatt !== this@BleHeartRateClient.gatt) return
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT connected, discovering services")
                if (!gatt.discoverServices()) {
                    onEvent(BleEvent.Error(uiText(R.string.discover_services_failed)))
                    closeGatt()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                val message = if (status == BluetoothGatt.GATT_SUCCESS) {
                    uiText(R.string.connection_lost)
                } else {
                    uiText(R.string.connection_lost_status, status)
                }
                closeGatt()
                onEvent(BleEvent.Disconnected(message))
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (gatt !== this@BleHeartRateClient.gatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onEvent(BleEvent.Error(uiText(R.string.discovery_failed_status, status)))
                closeGatt()
                return
            }

            val service = gatt.getService(HR_SERVICE_UUID)
            if (service == null) {
                onEvent(BleEvent.Error(uiText(R.string.missing_hr_service)))
                closeGatt()
                return
            }
            val measurement = service.getCharacteristic(HR_MEASUREMENT_UUID)
            if (measurement == null) {
                onEvent(BleEvent.Error(uiText(R.string.missing_hr_characteristic)))
                closeGatt()
                return
            }

            if (!gatt.setCharacteristicNotification(measurement, true)) {
                onEvent(BleEvent.Error(uiText(R.string.enable_notify_failed)))
                closeGatt()
                return
            }

            val cccd = measurement.getDescriptor(CCCD_UUID)
            if (cccd == null) {
                markConnected(gatt)
                return
            }

            val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                writeCccdModern(gatt, cccd)
            } else {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(cccd)
            }
            if (!started) {
                onEvent(BleEvent.Error(uiText(R.string.subscribe_failed)))
                closeGatt()
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun writeCccdModern(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
        ): Boolean {
            return gatt.writeDescriptor(
                descriptor,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
            ) == BluetoothStatusCodes.SUCCESS
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (gatt !== this@BleHeartRateClient.gatt) return
            if (descriptor.uuid != CCCD_UUID) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                markConnected(gatt)
            } else {
                onEvent(BleEvent.Error(uiText(R.string.notify_failed_status, status)))
                closeGatt()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (gatt !== this@BleHeartRateClient.gatt) return
            handleNotification(characteristic, value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (gatt !== this@BleHeartRateClient.gatt) return
            val value = characteristic.value ?: return
            handleNotification(characteristic, value)
        }
    }

    fun startScan() {
        if (scanner) return
        val adapter = adapter
        if (adapter == null) {
            onEvent(BleEvent.ScanFailed(uiText(R.string.bluetooth_unsupported)))
            return
        }
        val leScanner = try {
            if (!adapter.isEnabled) {
                onEvent(BleEvent.ScanFailed(uiText(R.string.bluetooth_disabled)))
                return
            }
            adapter.bluetoothLeScanner
        } catch (_: SecurityException) {
            onEvent(BleEvent.ScanFailed(uiText(R.string.scan_permission_missing)))
            return
        }
        if (leScanner == null) {
            onEvent(BleEvent.ScanFailed(uiText(R.string.scanner_unavailable)))
            return
        }
        val settings = ScanSettings.Builder()
            .setScanMode(SCAN_MODE)
            .build()
        scanResults.clear()
        handler.removeCallbacks(scanFlushRunnable)
        scanFlushScheduled.set(false)
        scanner = true
        try {
            leScanner.startScan(null, settings, scanCallback)
            handler.postDelayed(scanTimeout, SCAN_TIMEOUT_MS)
            Log.d(TAG, "BLE scan started (mode=$SCAN_MODE, 500ms UI batches)")
        } catch (exc: SecurityException) {
            scanner = false
            onEvent(BleEvent.ScanFailed(uiText(R.string.scan_permission_missing)))
        } catch (exc: Exception) {
            scanner = false
            onEvent(BleEvent.ScanFailed(uiText(R.string.scan_start_failed, exc.message ?: uiText(R.string.unknown_error))))
        }
    }

    fun stopScan() {
        if (!scanner) return
        stopScanInternal()
        onEvent(BleEvent.ScanFinished(uiText(R.string.scan_stopped)))
    }

    private fun stopScanInternal() {
        if (!scanner) return
        scanner = false
        handler.removeCallbacks(scanTimeout)
        handler.removeCallbacks(scanFlushRunnable)
        scanFlushScheduled.set(false)
        try {
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: SecurityException) {
            // Permission revoked while scanning.
        }
        emitScanSnapshot()
    }

    fun connect(scanned: ScannedDevice) {
        connect(scanned.device, scanned.name, CONNECT_TIMEOUT_MS)
    }

    internal fun reconnect(device: BluetoothDevice, name: String?): Boolean {
        if (!canConnect()) return false
        connect(device, name, DIRECT_CONNECT_TIMEOUT_MS)
        return true
    }

    internal fun connectRecent(address: String, name: String?, addressType: Int?): Boolean {
        val device = try {
            val activeAdapter = adapter?.takeIf { it.isEnabled } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (addressType == BluetoothDevice.ADDRESS_TYPE_PUBLIC || addressType == BluetoothDevice.ADDRESS_TYPE_RANDOM)
            ) {
                activeAdapter.getRemoteLeDevice(address, addressType)
            } else {
                activeAdapter.getRemoteDevice(address)
            }
        } catch (_: SecurityException) {
            return false
        } catch (_: IllegalArgumentException) {
            return false
        }
        return reconnect(device, name)
    }

    private fun connect(device: BluetoothDevice, name: String?, timeoutMs: Long) {
        stopScanInternal()
        closeGatt()
        connectedEmitted = false
        try {
            if (adapter?.isEnabled != true) {
                onEvent(BleEvent.Error(uiText(R.string.bluetooth_disabled)))
                return
            }
            onEvent(BleEvent.Connecting(name ?: safeName(device), device.address))
            // Serialize GATT callbacks with cancellation, timeout and replacement.
            val newGatt = device.connectGatt(
                appContext,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE,
                BluetoothDevice.PHY_LE_1M_MASK,
                handler,
            )
            if (newGatt == null) {
                onEvent(BleEvent.Error(uiText(R.string.gatt_create_failed)))
                return
            }
            gatt = newGatt
            handler.postDelayed(connectTimeout, timeoutMs)
        } catch (_: SecurityException) {
            onEvent(BleEvent.Error(uiText(R.string.connect_permission_missing)))
        } catch (exc: IllegalArgumentException) {
            Log.w(TAG, "Unable to create GATT connection", exc)
            onEvent(BleEvent.Error(uiText(R.string.gatt_create_failed)))
        }
    }

    fun disconnect() {
        handler.removeCallbacks(connectTimeout)
        closeGatt()
    }

    fun close() {
        stopScanInternal()
        disconnect()
    }

    private fun markConnected(gatt: BluetoothGatt) {
        if (connectedEmitted) return
        connectedEmitted = true
        handler.removeCallbacks(connectTimeout)
        val name = safeName(gatt.device)
        val address = try {
            gatt.device.address
        } catch (_: SecurityException) {
            "--"
        }
        val services = try {
            gatt.services?.map { it.uuid.toString() } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
        val addressType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            gatt.device.addressType
        } else null
        onEvent(BleEvent.Connected(name, address, services, addressType, gatt.device))
        Log.d(TAG, "Subscribed to 0x2A37 on $address")
    }

    private fun handleNotification(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        if (!connectedEmitted || characteristic.uuid != HR_MEASUREMENT_UUID) return
        try {
            val sample = HeartRateParser.parse(value)
            Log.d(TAG, "HR ${sample.bpm} bpm raw=${sample.rawHex}")
            onEvent(BleEvent.Notification(sample))
        } catch (exc: HeartRateParseException) {
            Log.w(TAG, "Bad HR packet: ${exc.reason} raw=${HeartRateParser.hex(exc.raw)}")
            onEvent(BleEvent.BadPacket(exc.reason.asUiText(), HeartRateParser.hex(exc.raw)))
        } catch (exc: Exception) {
            Log.w(TAG, "HR packet parse failure", exc)
            onEvent(
                BleEvent.BadPacket(
                    uiText(R.string.parse_failure, exc.message ?: uiText(R.string.unknown_error)),
                    HeartRateParser.hex(value),
                )
            )
        }
    }

    private fun safeName(device: BluetoothDevice): String? {
        return try {
            device.name
        } catch (_: SecurityException) {
            null
        }
    }

    private fun closeGatt() {
        handler.removeCallbacks(connectTimeout)
        val current = gatt
        gatt = null
        connectedEmitted = false
        if (current != null) {
            try {
                current.disconnect()
            } catch (_: SecurityException) {
                // Ignore and still release the client.
            }
            try {
                current.close()
            } catch (_: Exception) {
                // Nothing else to release.
            }
        }
    }
}
