package com.example.blehr.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blehr.data.heartrate.HeartRateRepository
import com.example.blehr.data.settings.AppLanguage
import com.example.blehr.data.settings.LanguageSettings

@Composable
fun SettingsRoute(
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val heartRateTimeoutSeconds by viewModel.heartRateTimeoutSeconds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val heartRateRepository = remember(context.applicationContext) { HeartRateRepository.get(context) }
    val heartRateState by heartRateRepository.ui.collectAsStateWithLifecycle()
    // Read configuration so a framework app-locale change refreshes the selected segment too.
    LocalConfiguration.current
    SettingsScreen(
        language = LanguageSettings.selected(context),
        heartRateState = heartRateState,
        appearance = appearance,
        theme = theme,
        onThemeSelected = viewModel::setTheme,
        heartRateTimeoutSeconds = heartRateTimeoutSeconds,
        onHeartRateTimeoutChanged = viewModel::setHeartRateTimeoutSeconds,
        onLanguageSelected = onLanguageSelected,
        onBack = onBack,
        onBackgroundColorChanged = viewModel::setBackgroundColor,
        onBackgroundAlphaChanged = viewModel::setBackgroundAlpha,
        onFontColorChanged = viewModel::setFontColor,
        onFontAlphaChanged = viewModel::setFontAlpha,
        onResetAppearance = viewModel::resetAppearance,
    )
}
