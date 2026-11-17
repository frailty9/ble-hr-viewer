package com.example.blehr

import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.blehr.data.settings.AppLanguage
import com.example.blehr.data.settings.LanguageSettings
import com.example.blehr.feature.home.HomeRoute
import com.example.blehr.feature.home.HomeViewModel
import com.example.blehr.feature.settings.SettingsRoute
import com.example.blehr.navigation.BleHrNavHost
import com.example.blehr.ui.theme.BleHrTheme

@Composable
fun BleHrApp(homeViewModel: HomeViewModel, onLanguageSelected: (AppLanguage) -> Unit) {
    LocalizedContent {
        BleHrTheme {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BleHrNavHost(
                    homeContent = { openSettings -> HomeRoute(homeViewModel, openSettings) },
                    settingsContent = { back -> SettingsRoute(onLanguageSelected, back) },
                )
            }
        }
    }
}

/** Keep the Activity, navigation and animation state alive when the app language changes. */
@Composable
private fun LocalizedContent(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemConfiguration = LocalConfiguration.current
    val preferences = remember(context) { LanguageSettings.preferences(context) }
    var language by remember(context) { mutableStateOf(LanguageSettings.selected(context)) }
    DisposableEffect(preferences, context) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            language = LanguageSettings.selected(context)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val localizedContext = remember(context, systemConfiguration, language) {
        LanguageSettings.localizedContext(context)
    }
    val configuration = remember(localizedContext, systemConfiguration, language) {
        Configuration(localizedContext.resources.configuration)
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides configuration,
        LocalLayoutDirection provides if (configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        },
        content = content,
    )
}
