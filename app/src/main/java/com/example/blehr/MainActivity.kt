package com.example.blehr

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.blehr.data.settings.LanguageSettings
import com.example.blehr.data.settings.SettingsRepository
import com.example.blehr.feature.home.HomeViewModel
import com.example.blehr.overlay.HeartRateOverlayService

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
        HeartRateOverlayService.restoreIfEnabled(this)
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
