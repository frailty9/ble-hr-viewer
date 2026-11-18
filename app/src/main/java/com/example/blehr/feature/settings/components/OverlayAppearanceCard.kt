package com.example.blehr.feature.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.blehr.R
import com.example.blehr.ble.HeartRateSample
import com.example.blehr.ble.SensorContact
import com.example.blehr.data.heartrate.HeartRateState
import com.example.blehr.data.settings.OverlayAppearance
import com.example.blehr.data.settings.OverlayColors
import com.example.blehr.overlay.HeartRateOverlayView
import com.example.blehr.ui.theme.Accent
import com.example.blehr.ui.theme.Ink
import com.example.blehr.ui.theme.LocalAppShapes
import com.example.blehr.ui.theme.Muted
import com.example.blehr.ui.theme.SurfaceRaised
import kotlin.math.roundToInt

@Composable
internal fun OverlayAppearanceCard(
    appearance: OverlayAppearance,
    onBackgroundColorChanged: (Int) -> Unit,
    onBackgroundAlphaChanged: (Float) -> Unit,
    onFontColorChanged: (Int) -> Unit,
    onFontAlphaChanged: (Float) -> Unit,
    onReset: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.overlay_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onReset, shape = LocalAppShapes.current.button) {
                    Text(stringResource(R.string.reset_defaults))
                }
            }
            OverlayPreview(appearance)
            Spacer(Modifier.height(10.dp))

            AppearanceLabel(stringResource(R.string.background_color))
            Spacer(Modifier.height(6.dp))
            ColorSwatchRow(
                selected = appearance.backgroundColor,
                colors = OverlayColors.BackgroundPresets,
                onSelect = onBackgroundColorChanged,
            )

            Spacer(Modifier.height(10.dp))
            AppearanceLabel(
                stringResource(R.string.background_opacity, (appearance.backgroundAlpha * 100).roundToInt())
            )
            OpacitySlider(
                value = appearance.backgroundAlpha,
                onValueChange = onBackgroundAlphaChanged,
                modifier = Modifier.fillMaxWidth().testTag("background_opacity_slider"),
            )

            AppearanceLabel(stringResource(R.string.font_color))
            Spacer(Modifier.height(6.dp))
            ColorSwatchRow(
                selected = appearance.fontColor,
                colors = OverlayColors.FontPresets,
                onSelect = onFontColorChanged,
            )

            Spacer(Modifier.height(10.dp))
            AppearanceLabel(
                stringResource(R.string.font_opacity, (appearance.fontAlpha * 100).roundToInt())
            )
            OpacitySlider(
                value = appearance.fontAlpha,
                onValueChange = onFontAlphaChanged,
                modifier = Modifier.fillMaxWidth().testTag("font_opacity_slider"),
            )
        }
    }
}

/** Preserve the pre-0.4.0 thin track and round thumb independently of Material3 defaults. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun OpacitySlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val activeColor = Accent
    val inactiveColor = SurfaceRaised
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = 0f..1f,
        interactionSource = interactionSource,
        thumb = {
            Box(
                Modifier.size(20.dp)
                    .shadow(1.dp, CircleShape)
                    .background(Accent, CircleShape),
            )
        },
        track = { state ->
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val rtl = layoutDirection == LayoutDirection.Rtl
                val start = Offset(if (rtl) size.width else 0f, center.y)
                val end = Offset(if (rtl) 0f else size.width, center.y)
                drawLine(inactiveColor, start, end, strokeWidth = size.height, cap = StrokeCap.Round)
                if (state.value > 0f) {
                    val activeEnd = Offset(
                        if (rtl) size.width * (1f - state.value) else size.width * state.value,
                        center.y,
                    )
                    drawLine(activeColor, start, activeEnd, strokeWidth = size.height, cap = StrokeCap.Round)
                }
            }
        },
    )
}


@Composable
private fun AppearanceLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Muted,
    )
}


@Composable
private fun OverlayPreview(appearance: OverlayAppearance) {
    val previewState = remember {
        HeartRateState(
            sample = HeartRateSample(
                bpm = 123,
                sensorContact = SensorContact.NOT_SUPPORTED,
                energyExpendedKj = null,
                rrIntervalsRaw = emptyList(),
                flags = 0,
                raw = byteArrayOf(),
                warnings = emptyList(),
                receivedAtMillis = 0L,
            ),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { context ->
                HeartRateOverlayView(context, onClose = {}, onDrag = { _, _ -> }).apply {
                    bindState(previewState)
                }
            },
            update = { view -> view.bindAppearance(appearance) },
        )
    }
}


@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ColorSwatchRow(
    selected: Int,
    colors: List<Int>,
    onSelect: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = if (color == selected) 2.dp else 1.dp,
                        color = if (color == selected) Accent else Muted,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(color) },
            )
        }
    }
}
