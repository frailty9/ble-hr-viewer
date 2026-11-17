package com.example.blehr.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blehr.data.settings.AppLanguage
import com.example.blehr.data.settings.LanguageSettings

@Composable
fun SettingsRoute(
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Read configuration so a framework app-locale change refreshes the selected segment too.
    LocalConfiguration.current
    SettingsScreen(
        language = LanguageSettings.selected(context),
        appearance = appearance,
        onLanguageSelected = onLanguageSelected,
        onBack = onBack,
        onBackgroundColorChanged = viewModel::setBackgroundColor,
        onBackgroundAlphaChanged = viewModel::setBackgroundAlpha,
        onFontColorChanged = viewModel::setFontColor,
        onFontAlphaChanged = viewModel::setFontAlpha,
        onResetAppearance = viewModel::resetAppearance,
    )
}
