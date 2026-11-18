package io.github.frailty9.blehr.feature.settings

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import io.github.frailty9.blehr.data.heartrate.HeartRateRepository
import io.github.frailty9.blehr.data.privacy.PrivacyConsentStore
import io.github.frailty9.blehr.data.settings.AppTheme
import io.github.frailty9.blehr.data.settings.SettingsRepository
import io.github.frailty9.blehr.overlay.HeartRateOverlayService

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository.get(application)
    val appearance = repository.appearance
    val theme = repository.theme
    val heartRateTimeoutSeconds = repository.heartRateTimeoutSeconds

    fun setTheme(theme: AppTheme) = repository.setTheme(theme)
    fun setHeartRateTimeoutSeconds(seconds: Int) = repository.setHeartRateTimeoutSeconds(seconds)

    fun setBackgroundColor(color: Int) = repository.setBackgroundColor(color)
    fun setBackgroundAlpha(alpha: Float) = repository.setBackgroundAlpha(alpha)
    fun setFontColor(color: Int) = repository.setFontColor(color)
    fun setFontAlpha(alpha: Float) = repository.setFontAlpha(alpha)
    fun resetAppearance() = repository.reset()

    // Lint misreads the Class reference in the Intent constructor as a SAM conversion.
    @SuppressLint("ImplicitSamInstance")
    fun withdrawConsent() {
        val context = getApplication<Application>()
        PrivacyConsentStore.withdraw(context)
        HeartRateRepository.get(context).clearSessionAndForgetDevice()
        repository.setOverlayEnabled(false)
        context.stopService(Intent(context, HeartRateOverlayService::class.java))
    }
}
