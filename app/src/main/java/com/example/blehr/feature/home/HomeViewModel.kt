package com.example.blehr.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.blehr.ble.ScannedDevice
import com.example.blehr.data.heartrate.HeartRateRepository
import com.example.blehr.data.heartrate.HeartRateState
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HeartRateRepository.get(application)
    val ui: StateFlow<HeartRateState> = repository.ui

    fun reconnectAtStartup() = repository.reconnectAtStartup()

    fun startScan() = repository.startScan()

    fun stopScan() = repository.stopScan()

    fun connect(device: ScannedDevice) = repository.connect(device)

    fun disconnect() = repository.disconnect()
}
