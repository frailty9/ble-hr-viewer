package com.example.blehr.feature.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.blehr.R
import com.example.blehr.ui.theme.Ink
import com.example.blehr.ui.theme.LocalAppShapes
import com.example.blehr.ui.theme.Muted

@Composable
internal fun HelpCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.help_title),
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
            )
            Spacer(Modifier.height(8.dp))
            DetailLine(stringResource(R.string.help_enable_sharing))
            DetailLine(stringResource(R.string.help_standard_service))
            DetailLine(stringResource(R.string.help_no_contact))
            DetailLine(stringResource(R.string.help_bad_packets))
        }
    }
}


@Composable
private fun DetailLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Muted,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
