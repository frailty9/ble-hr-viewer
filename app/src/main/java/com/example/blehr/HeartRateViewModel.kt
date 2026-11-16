package com.example.blehr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.blehr.ble.ScannedDevice
import kotlinx.coroutines.flow.StateFlow

class HeartRateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HeartRateRepository.get(application)
    val ui: StateFlow<HeartRateUiState> = repository.ui

    fun startScan() = repository.startScan()

    fun stopScan() = repository.stopScan()

    fun connect(device: ScannedDevice) = repository.connect(device)

    fun disconnect() = repository.disconnect()
}
