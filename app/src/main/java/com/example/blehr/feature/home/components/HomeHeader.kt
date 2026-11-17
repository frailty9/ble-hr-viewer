package com.example.blehr.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.blehr.R
import com.example.blehr.data.heartrate.ConnectionPhase
import com.example.blehr.data.heartrate.HeartRateState
import com.example.blehr.ui.components.StatusChip
import com.example.blehr.ui.theme.ErrorRed
import com.example.blehr.ui.theme.Ink
import com.example.blehr.ui.theme.Muted
import com.example.blehr.ui.theme.OkGreen
import com.example.blehr.ui.theme.WarnAmber

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun AppHeader(state: HeartRateState, onOpenSettings: () -> Unit) {
    val (label, color) = when (state.connection) {
        ConnectionPhase.SCANNING -> stringResource(R.string.phase_scanning) to WarnAmber
        ConnectionPhase.CONNECTING -> stringResource(R.string.phase_connecting) to WarnAmber
        ConnectionPhase.CONNECTED -> stringResource(R.string.phase_connected) to OkGreen
        ConnectionPhase.DISCONNECTED -> stringResource(R.string.phase_disconnected) to Muted
        ConnectionPhase.ERROR -> stringResource(R.string.phase_error) to ErrorRed
        ConnectionPhase.IDLE -> stringResource(R.string.phase_idle) to Muted
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Box(Modifier.align(Alignment.CenterVertically)) { StatusChip(label, color) }
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
