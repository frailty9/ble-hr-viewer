package com.example.blehr.ble

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
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class ScannedDevice(
    val device: BluetoothDevice,
    val name: String?,
    val address: String,
    val rssi: Int,
    val hasHeartRateService: Boolean,
)

sealed interface BleEvent {
    data class ScanResults(val devices: List<ScannedDevice>) : BleEvent
    data class ScanFinished(val message: String) : BleEvent
    data class ScanFailed(val message: String) : BleEvent
    data class Connecting(val name: String, val address: String) : BleEvent
    data class Connected(
        val name: String,
        val address: String,
        val services: List<String>,
    ) : BleEvent
    data class Disconnected(val message: String) : BleEvent
    data class Notification(val sample: HeartRateSample) : BleEvent
    data class BadPacket(val reason: String, val rawHex: String) : BleEvent
    data class Error(val message: String) : BleEvent
}

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
            onEvent(BleEvent.ScanFinished("扫描结束"))
        }
    }

    private val connectTimeout = Runnable {
        if (gatt != null && !connectedEmitted) {
            onEvent(BleEvent.Error("连接/订阅超时，请重试"))
            closeGatt()
        }
    }

    val isScanning: Boolean get() = scanner

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
            scanner = false
            handler.removeCallbacks(scanTimeout)
            handler.removeCallbacks(scanFlushRunnable)
            scanFlushScheduled.set(false)
            Log.w(TAG, "BLE scan failed: errorCode=$errorCode")
            onEvent(BleEvent.ScanFailed("扫描失败，errorCode=$errorCode"))
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
                    onEvent(BleEvent.Error("发现服务失败，请重试"))
                    closeGatt()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                val message = if (status == BluetoothGatt.GATT_SUCCESS) {
                    "连接已断开"
                } else {
                    "连接断开 (status=$status)"
                }
                onEvent(BleEvent.Disconnected(message))
                closeGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (gatt !== this@BleHeartRateClient.gatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onEvent(BleEvent.Error("服务发现失败 (status=$status)"))
                closeGatt()
                return
            }

            val service = gatt.getService(HR_SERVICE_UUID)
            if (service == null) {
                onEvent(BleEvent.Error("设备没有 0x180D 心率服务（确认心率广播已开启）"))
                closeGatt()
                return
            }
            val measurement = service.getCharacteristic(HR_MEASUREMENT_UUID)
            if (measurement == null) {
                onEvent(BleEvent.Error("设备没有 0x2A37 心率测量特征"))
                closeGatt()
                return
            }

            if (!gatt.setCharacteristicNotification(measurement, true)) {
                onEvent(BleEvent.Error("无法开启心率通知"))
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
                onEvent(BleEvent.Error("订阅心率通知失败"))
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
                onEvent(BleEvent.Error("开启通知失败 (status=$status)"))
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
            onEvent(BleEvent.Error("此设备不支持蓝牙"))
            return
        }
        if (!adapter.isEnabled) {
            onEvent(BleEvent.Error("蓝牙未开启，请打开系统蓝牙后重试"))
            return
        }
        val leScanner = adapter.bluetoothLeScanner
        if (leScanner == null) {
            onEvent(BleEvent.Error("无法获取 BLE 扫描器"))
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
            onEvent(BleEvent.Error("缺少蓝牙扫描权限"))
        } catch (exc: Exception) {
            scanner = false
            onEvent(BleEvent.Error("扫描启动失败：${exc.message}"))
        }
    }

    fun stopScan() {
        if (!scanner) return
        stopScanInternal()
        onEvent(BleEvent.ScanFinished("扫描已停止"))
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
        stopScanInternal()
        closeGatt()
        connectedEmitted = false
        val adapter = adapter
        if (adapter == null || !adapter.isEnabled) {
            onEvent(BleEvent.Error("蓝牙未开启，请打开系统蓝牙后重试"))
            return
        }
        val name = scanned.name ?: "未知设备"
        onEvent(BleEvent.Connecting(name, scanned.address))
        try {
            val newGatt = scanned.device.connectGatt(
                appContext,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE,
            )
            if (newGatt == null) {
                onEvent(BleEvent.Error("无法创建 GATT 连接，请重试"))
                return
            }
            gatt = newGatt
            handler.postDelayed(connectTimeout, CONNECT_TIMEOUT_MS)
        } catch (exc: SecurityException) {
            onEvent(BleEvent.Error("缺少蓝牙连接权限"))
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
        val name = safeName(gatt.device) ?: "未知设备"
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
        onEvent(BleEvent.Connected(name, address, services))
        Log.d(TAG, "Subscribed to 0x2A37 on $address")
    }

    private fun handleNotification(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        if (characteristic.uuid != HR_MEASUREMENT_UUID) return
        try {
            val sample = HeartRateParser.parse(value)
            Log.d(TAG, "HR ${sample.bpm} bpm raw=${sample.rawHex}")
            onEvent(BleEvent.Notification(sample))
        } catch (exc: HeartRateParseException) {
            Log.w(TAG, "Bad HR packet: ${exc.reason} raw=${HeartRateParser.hex(exc.raw)}")
            onEvent(BleEvent.BadPacket(exc.reason, HeartRateParser.hex(exc.raw)))
        } catch (exc: Exception) {
            Log.w(TAG, "HR packet parse failure", exc)
            onEvent(
                BleEvent.BadPacket(
                    exc.message ?: "unknown parse failure",
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
