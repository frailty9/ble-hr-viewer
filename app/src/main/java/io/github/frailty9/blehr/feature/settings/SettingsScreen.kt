package io.github.frailty9.blehr.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.data.heartrate.HeartRateState
import io.github.frailty9.blehr.data.settings.AppLanguage
import io.github.frailty9.blehr.data.settings.AppTheme
import io.github.frailty9.blehr.data.settings.DEFAULT_HEART_RATE_TIMEOUT_SECONDS
import io.github.frailty9.blehr.data.settings.OverlayAppearance
import io.github.frailty9.blehr.feature.settings.components.AboutCard
import io.github.frailty9.blehr.feature.settings.components.BluetoothConnectionSettings
import io.github.frailty9.blehr.feature.settings.components.ErrorCard
import io.github.frailty9.blehr.feature.settings.components.HelpCard
import io.github.frailty9.blehr.feature.settings.components.LanguageSelector
import io.github.frailty9.blehr.feature.settings.components.OverlayAppearanceCard
import io.github.frailty9.blehr.feature.settings.components.PacketCard
import io.github.frailty9.blehr.feature.settings.components.ThemeSelector
import io.github.frailty9.blehr.ui.theme.PageMargin

@Composable
fun SettingsScreen(
    language: AppLanguage,
    heartRateState: HeartRateState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    appearance: OverlayAppearance,
    onBackgroundColorChanged: (Int) -> Unit,
    onBackgroundAlphaChanged: (Float) -> Unit,
    onFontColorChanged: (Int) -> Unit,
    onFontAlphaChanged: (Float) -> Unit,
    onResetAppearance: () -> Unit,
    theme: AppTheme = AppTheme.SYSTEM,
    onThemeSelected: (AppTheme) -> Unit = {},
    heartRateTimeoutSeconds: Int = DEFAULT_HEART_RATE_TIMEOUT_SECONDS,
    onHeartRateTimeoutChanged: (Int) -> Unit = {},
    versionName: String = "",
    contactEmail: String = "",
    filingNumber: String = "",
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenUserAgreement: () -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    onWithdrawConsent: () -> Unit = {},
) {
    Box(
        Modifier.fillMaxSize().testTag("settings_screen")
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = PageMargin),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = PageMargin),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back),
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("settings_scroll")
                    .verticalScroll(rememberScrollState()).padding(vertical = PageMargin),
                verticalArrangement = Arrangement.spacedBy(PageMargin),
            ) {
                LanguageSelector(language, onLanguageSelected)
                ThemeSelector(theme, onThemeSelected)
                OverlayAppearanceCard(
                    appearance, onBackgroundColorChanged, onBackgroundAlphaChanged,
                    onFontColorChanged, onFontAlphaChanged, onResetAppearance,
                )
                BluetoothConnectionSettings(heartRateTimeoutSeconds, onHeartRateTimeoutChanged)
                PacketCard(heartRateState)
                ErrorCard(heartRateState)
                HelpCard()
                AboutCard(
                    versionName = versionName,
                    contactEmail = contactEmail,
                    filingNumber = filingNumber,
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    onOpenUserAgreement = onOpenUserAgreement,
                    onOpenLicenses = onOpenLicenses,
                    onWithdrawConsent = onWithdrawConsent,
                )
            }
        }
    }
}
