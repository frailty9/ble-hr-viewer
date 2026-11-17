package com.example.blehr.feature.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.blehr.R
import com.example.blehr.ui.components.StatusChip
import com.example.blehr.ui.theme.Ink
import com.example.blehr.ui.theme.LocalAppShapes
import com.example.blehr.ui.theme.Muted
import com.example.blehr.ui.theme.OkGreen
import com.example.blehr.ui.theme.WarnAmber

@Composable
internal fun OverlayCard(granted: Boolean, onShow: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.overlay_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                StatusChip(
                    text = if (granted) stringResource(R.string.permission_granted) else stringResource(R.string.permission_required),
                    color = if (granted) OkGreen else WarnAmber,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.overlay_description),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.overlay_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onShow, shape = LocalAppShapes.current.button) {
                Text(if (granted) stringResource(R.string.overlay_show) else stringResource(R.string.overlay_allow_show))
            }
        }
    }
}
