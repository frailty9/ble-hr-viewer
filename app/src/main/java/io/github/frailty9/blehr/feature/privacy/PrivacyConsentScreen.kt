package io.github.frailty9.blehr.feature.privacy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.ui.legal.LegalDocument
import io.github.frailty9.blehr.ui.theme.Ink
import io.github.frailty9.blehr.ui.theme.LocalAppShapes
import io.github.frailty9.blehr.ui.theme.Muted
import io.github.frailty9.blehr.ui.theme.PageMargin

@Composable
fun PrivacyConsentScreen(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
    onOpenDocument: (LegalDocument) -> Unit,
) {
    BackHandler(onBack = onDecline)
    var sensitiveAccepted by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().testTag("privacy_consent")
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PageMargin, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
            Text(
                text = stringResource(R.string.privacy_consent_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.privacy_consent_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.privacy_consent_data),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.privacy_consent_permissions),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { onOpenDocument(LegalDocument.PRIVACY) },
                    shape = LocalAppShapes.current.button,
                    modifier = Modifier.testTag("privacy_policy_link"),
                ) {
                    Text(stringResource(R.string.privacy_policy_title))
                }
                TextButton(
                    onClick = { onOpenDocument(LegalDocument.AGREEMENT) },
                    shape = LocalAppShapes.current.button,
                    modifier = Modifier.testTag("user_agreement_link"),
                ) {
                    Text(stringResource(R.string.user_agreement_title))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { sensitiveAccepted = !sensitiveAccepted }
                    .padding(vertical = 4.dp)
                    .testTag("privacy_sensitive_consent"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = sensitiveAccepted,
                    onCheckedChange = { sensitiveAccepted = it },
                )
                Text(
                    text = stringResource(R.string.privacy_sensitive_consent),
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.privacy_consent_statement),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAgree,
                enabled = sensitiveAccepted,
                shape = LocalAppShapes.current.button,
                modifier = Modifier.fillMaxWidth().testTag("privacy_agree"),
            ) {
                Text(stringResource(R.string.privacy_consent_agree))
            }
            TextButton(
                onClick = onDecline,
                shape = LocalAppShapes.current.button,
                modifier = Modifier.fillMaxWidth().testTag("privacy_decline"),
            ) {
                Text(stringResource(R.string.privacy_consent_decline))
            }
            Text(
                text = stringResource(R.string.privacy_consent_decline_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
