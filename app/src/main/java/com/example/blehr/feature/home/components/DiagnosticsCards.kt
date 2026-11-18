package com.example.blehr.feature.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.blehr.R
import com.example.blehr.core.text.asUiText
import com.example.blehr.data.heartrate.HeartRateState
import com.example.blehr.ui.components.StatusChip
import com.example.blehr.ui.theme.ErrorRed
import com.example.blehr.ui.theme.Ink
import com.example.blehr.ui.theme.LocalAppShapes
import com.example.blehr.ui.theme.Muted
import com.example.blehr.ui.theme.WarnAmber
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun PacketCard(state: HeartRateState) {
    val sample = state.sample
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.packet_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "0x2A37",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Muted,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.lastRawHex.ifBlank { "--" },
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = Ink,
            )
            if (sample != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.packet_metadata,
                        sample.flags,
                        sample.energyExpendedKj?.let { "$it kJ" } ?: "--",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Muted,
                )
                sample.warnings.forEach { warning ->
                    Text(
                        text = "! ${warning.asUiText().asString()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarnAmber,
                    )
                }
            }
        }
    }
}


@Composable
internal fun ErrorCard(state: HeartRateState) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { SimpleDateFormat("HH:mm:ss", locale) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.error_packets),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                StatusChip(
                    text = state.parseErrorCount.toString(),
                    color = if (state.parseErrorCount > 0) ErrorRed else Muted,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (state.errors.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_error_packets),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            } else {
                state.errors.forEach { entry ->
                    Text(
                        text = "${formatter.format(Date(entry.atMillis))}  ${entry.reason.asString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed,
                    )
                    Text(
                        text = stringResource(R.string.packet_raw, entry.rawHex),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
