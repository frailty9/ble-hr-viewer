package io.github.frailty9.blehr.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.data.heartrate.ConnectionPhase
import io.github.frailty9.blehr.data.heartrate.HeartRateState
import io.github.frailty9.blehr.ui.theme.ErrorRed
import io.github.frailty9.blehr.ui.theme.Ink
import io.github.frailty9.blehr.ui.theme.Muted
import io.github.frailty9.blehr.ui.theme.OkGreen
import io.github.frailty9.blehr.ui.theme.WarnAmber

@Composable
internal fun AppHeader(state: HeartRateState, onOpenSettings: () -> Unit) {
    val (label, color) = when (state.connection) {
        ConnectionPhase.SCANNING -> stringResource(R.string.phase_scanning) to WarnAmber
        ConnectionPhase.CONNECTING -> stringResource(R.string.phase_connecting) to WarnAmber
        ConnectionPhase.CONNECTED -> stringResource(R.string.phase_connected) to OkGreen
        ConnectionPhase.RECONNECTING -> stringResource(R.string.phase_reconnecting) to WarnAmber
        ConnectionPhase.DISCONNECTED -> stringResource(R.string.phase_disconnected) to Muted
        ConnectionPhase.ERROR -> stringResource(R.string.phase_error) to ErrorRed
        ConnectionPhase.IDLE -> stringResource(R.string.phase_idle) to Muted
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Box(
                    Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                        .semantics { contentDescription = label },
                )
            }
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.settings_title),
                tint = Muted,
            )
        }
    }
}
