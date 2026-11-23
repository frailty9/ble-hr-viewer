package io.github.frailty9.blehr.feature.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.ui.theme.Ink
import io.github.frailty9.blehr.ui.theme.LocalAppShapes
import io.github.frailty9.blehr.ui.theme.Muted

private val AboutCardInset = 18.dp

@Composable
internal fun AboutCard(
    versionName: String,
    contactEmail: String,
    filingNumber: String,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenUserAgreement: () -> Unit,
    onOpenLicenses: () -> Unit,
    onWithdrawConsent: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("about_card"),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(vertical = AboutCardInset)) {
            Column(Modifier.padding(horizontal = AboutCardInset)) {
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(8.dp))
                AboutLine(stringResource(R.string.about_version, versionName))
                AboutLine(stringResource(R.string.about_contact, contactEmail))
                if (filingNumber.isNotBlank()) {
                    AboutLine(stringResource(R.string.about_filing_number, filingNumber))
                }
            }
            Spacer(Modifier.height(4.dp))
            AboutNavigationRow(
                label = stringResource(R.string.about_privacy_policy),
                onClick = onOpenPrivacyPolicy,
                modifier = Modifier.testTag("about_privacy"),
            )
            AboutNavigationRow(
                label = stringResource(R.string.about_user_agreement),
                onClick = onOpenUserAgreement,
                modifier = Modifier.testTag("about_agreement"),
            )
            AboutNavigationRow(
                label = stringResource(R.string.about_licenses),
                onClick = onOpenLicenses,
                modifier = Modifier.testTag("about_licenses"),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.about_disclaimer),
                modifier = Modifier.padding(horizontal = AboutCardInset),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Button(
                onClick = onWithdrawConsent,
                shape = LocalAppShapes.current.button,
                modifier = Modifier.padding(horizontal = AboutCardInset).testTag("about_withdraw"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.about_withdraw_consent))
            }
        }
    }
}

@Composable
private fun AboutNavigationRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = AboutCardInset, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Ink)
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Muted,
        )
    }
}

@Composable
private fun AboutLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Muted,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
