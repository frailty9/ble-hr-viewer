package io.github.frailty9.blehr.feature.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.frailty9.blehr.BuildConfig
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.data.heartrate.HeartRateRepository
import io.github.frailty9.blehr.data.settings.AppLanguage
import io.github.frailty9.blehr.data.settings.LanguageSettings
import io.github.frailty9.blehr.ui.legal.LegalDocument
import io.github.frailty9.blehr.ui.legal.LegalDocuments
import io.github.frailty9.blehr.ui.theme.ErrorRed

@Composable
fun SettingsRoute(
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onOpenDocument: (LegalDocument) -> Unit,
    onConsentWithdrawn: () -> Unit,
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
    var confirmWithdraw by remember { mutableStateOf(false) }
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
        onOverlayShowHeartChanged = viewModel::setOverlayShowHeart,
        onOverlayShowUnitChanged = viewModel::setOverlayShowUnit,
        versionName = BuildConfig.VERSION_NAME,
        contactEmail = LegalDocuments.CONTACT_EMAIL,
        filingNumber = BuildConfig.APP_FILING_NUMBER,
        onOpenPrivacyPolicy = { onOpenDocument(LegalDocument.PRIVACY) },
        onOpenUserAgreement = { onOpenDocument(LegalDocument.AGREEMENT) },
        onOpenLicenses = { onOpenDocument(LegalDocument.LICENSES) },
        onWithdrawConsent = { confirmWithdraw = true },
    )
    if (confirmWithdraw) {
        AlertDialog(
            onDismissRequest = { confirmWithdraw = false },
            title = { Text(stringResource(R.string.withdraw_consent_title)) },
            text = { Text(stringResource(R.string.withdraw_consent_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmWithdraw = false
                        viewModel.withdrawConsent()
                        onConsentWithdrawn()
                    },
                ) {
                    Text(stringResource(R.string.withdraw_consent_confirm), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWithdraw = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
