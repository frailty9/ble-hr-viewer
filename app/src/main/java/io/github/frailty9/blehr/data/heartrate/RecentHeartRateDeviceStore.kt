package io.github.frailty9.blehr.data.heartrate

import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.core.content.edit

internal data class RecentHeartRateDevice(val address: String, val name: String?, val addressType: Int?)

internal class RecentHeartRateDeviceStore(context: Context) {
    private val preferences = context.getSharedPreferences("recent_heart_rate_device", Context.MODE_PRIVATE)

    fun read(): RecentHeartRateDevice? {
        val address = preferences.getString("address", null) ?: return null
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return null
        return RecentHeartRateDevice(
            address,
            preferences.getString("name", null),
            preferences.getInt("address_type", -1).takeIf { it >= 0 },
        )
    }

    fun save(device: RecentHeartRateDevice) {
        if (!BluetoothAdapter.checkBluetoothAddress(device.address) || device == read()) return
        preferences.edit {
            putString("address", device.address)
            putString("name", device.name)
            putInt("address_type", device.addressType ?: -1)
        }
    }

    fun clear() {
        preferences.edit { clear() }
    }
}
