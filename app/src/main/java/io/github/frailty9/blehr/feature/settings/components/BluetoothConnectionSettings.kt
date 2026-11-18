package io.github.frailty9.blehr.feature.settings.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.data.settings.DEFAULT_HEART_RATE_TIMEOUT_SECONDS
import io.github.frailty9.blehr.data.settings.MAX_HEART_RATE_TIMEOUT_SECONDS
import io.github.frailty9.blehr.data.settings.MIN_HEART_RATE_TIMEOUT_SECONDS
import io.github.frailty9.blehr.ui.theme.LocalAppShapes
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun BluetoothConnectionSettings(timeoutSeconds: Int, onTimeoutChanged: (Int) -> Unit) {
    val label = stringResource(R.string.heart_rate_timeout_title)
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.bluetooth_connection_settings),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(
                    onClick = { onTimeoutChanged(DEFAULT_HEART_RATE_TIMEOUT_SECONDS) },
                    modifier = Modifier.testTag("heart_rate_timeout_reset"),
                    shape = LocalAppShapes.current.button,
                ) {
                    Text(stringResource(R.string.reset_defaults))
                }
            }
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
                interactionSource = interactionSource,
                thumb = {
                    SliderDefaults.Thumb(interactionSource, thumbSize = DpSize(4.dp, 24.dp))
                },
                track = { SliderDefaults.Track(sliderState = it, thumbTrackGapSize = 1.dp) },
            )
        }
    }
}
