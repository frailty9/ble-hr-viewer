package com.example.blehr.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.blehr.data.settings.SettingsRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository.get(application)
    val appearance = repository.appearance

    fun setBackgroundColor(color: Int) = repository.setBackgroundColor(color)
    fun setBackgroundAlpha(alpha: Float) = repository.setBackgroundAlpha(alpha)
    fun setFontColor(color: Int) = repository.setFontColor(color)
    fun setFontAlpha(alpha: Float) = repository.setFontAlpha(alpha)
    fun resetAppearance() = repository.reset()
}
