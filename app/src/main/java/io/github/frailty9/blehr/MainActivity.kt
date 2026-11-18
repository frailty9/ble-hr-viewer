package io.github.frailty9.blehr

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.frailty9.blehr.data.privacy.PrivacyConsentStore
import io.github.frailty9.blehr.data.settings.LanguageSettings
import io.github.frailty9.blehr.data.settings.SettingsRepository
import io.github.frailty9.blehr.feature.home.HomeViewModel
import io.github.frailty9.blehr.overlay.HeartRateOverlayService

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val systemDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        updateSystemBars(SettingsRepository.get(this).theme.value.isDark(systemDark))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            BleHrApp(
                homeViewModel = homeViewModel,
                onLanguageSelected = { LanguageSettings.apply(this, it) },
                onDarkThemeChanged = ::updateSystemBars,
            )
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (PrivacyConsentStore.isAccepted(this)) {
            HeartRateOverlayService.restoreIfEnabled(this)
        }
    }

    private fun updateSystemBars(dark: Boolean) {
        val style = if (dark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
