package com.example.blehr

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.blehr.data.settings.AppLanguage
import com.example.blehr.feature.home.HomeRoute
import com.example.blehr.feature.home.HomeViewModel
import com.example.blehr.feature.settings.SettingsRoute
import com.example.blehr.navigation.BleHrNavHost
import com.example.blehr.ui.theme.BleHrTheme

@Composable
fun BleHrApp(homeViewModel: HomeViewModel, onLanguageSelected: (AppLanguage) -> Unit) {
    BleHrTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BleHrNavHost(
                homeContent = { openSettings -> HomeRoute(homeViewModel, openSettings) },
                settingsContent = { back -> SettingsRoute(onLanguageSelected, back) },
            )
        }
    }
}
