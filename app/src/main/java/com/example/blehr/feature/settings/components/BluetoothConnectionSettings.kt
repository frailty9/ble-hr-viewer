package com.example.blehr.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.blehr.R
import com.example.blehr.data.settings.MAX_HEART_RATE_TIMEOUT_SECONDS
import com.example.blehr.data.settings.MIN_HEART_RATE_TIMEOUT_SECONDS
import com.example.blehr.ui.theme.LocalAppShapes
import kotlin.math.roundToInt

@Composable
internal fun BluetoothConnectionSettings(timeoutSeconds: Int, onTimeoutChanged: (Int) -> Unit) {
    val label = stringResource(R.string.heart_rate_timeout_title)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(stringResource(R.string.bluetooth_connection_settings), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    R.string.heart_rate_timeout_value,
                    label,
                    pluralStringResource(R.plurals.timeout_seconds, timeoutSeconds, timeoutSeconds),
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Slider(
                value = timeoutSeconds.toFloat(),
                onValueChange = { onTimeoutChanged(it.roundToInt()) },
                valueRange = MIN_HEART_RATE_TIMEOUT_SECONDS.toFloat()..MAX_HEART_RATE_TIMEOUT_SECONDS.toFloat(),
                steps = MAX_HEART_RATE_TIMEOUT_SECONDS - MIN_HEART_RATE_TIMEOUT_SECONDS - 1,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            )
        }
    }
}
