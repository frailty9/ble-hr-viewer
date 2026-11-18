package io.github.frailty9.blehr.feature.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.ble.SensorContact
import io.github.frailty9.blehr.core.text.asUiText
import io.github.frailty9.blehr.data.heartrate.ConnectionPhase
import io.github.frailty9.blehr.data.heartrate.HeartRateState
import io.github.frailty9.blehr.ui.components.StatusChip
import io.github.frailty9.blehr.ui.theme.Accent
import io.github.frailty9.blehr.ui.theme.ErrorRed
import io.github.frailty9.blehr.ui.theme.LocalAppShapes
import io.github.frailty9.blehr.ui.theme.Muted
import io.github.frailty9.blehr.ui.theme.OkGreen
import io.github.frailty9.blehr.ui.theme.WarnAmber
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun BpmCard(state: HeartRateState, overlayVisible: Boolean, onToggleOverlay: () -> Unit) {
    val context = LocalContext.current
    val sample = state.sample
    val bpm = sample?.bpm ?: 0
    val contact = sample?.sensorContact ?: SensorContact.NOT_SUPPORTED
    val hasSample = sample != null
    val bpmColor = when {
        !hasSample -> Muted
        bpm <= 0 -> WarnAmber
        contact == SensorContact.NOT_DETECTED -> WarnAmber
        else -> Accent
    }
    val summary = if (state.connection == ConnectionPhase.CONNECTED) {
        val name = state.connectedName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.unknown_device)
        val address = state.connectedAddress?.takeIf { it.isNotBlank() }
        if (address == null) name else "$name · $address"
    } else {
        when (state.status.resourceId) {
            R.string.status_disconnected, R.string.scan_finished, R.string.scan_stopped ->
                stringResource(R.string.status_idle)
            else -> state.status.asString()
        }
    }
    val contactText = when {
        !hasSample -> stringResource(R.string.contact_unknown)
        contact == SensorContact.NOT_DETECTED -> stringResource(R.string.contact_not_detected)
        bpm == 0 -> stringResource(R.string.no_pulse)
        contact == SensorContact.DETECTED -> stringResource(R.string.contact_detected)
        else -> stringResource(R.string.contact_unsupported)
    }
    val contactColor = when {
        !hasSample -> Muted
        contact == SensorContact.NOT_DETECTED -> WarnAmber
        bpm == 0 -> WarnAmber
        contact == SensorContact.DETECTED -> OkGreen
        else -> Muted
    }
    val rrText = sample?.rrIntervalsMs?.takeIf { it.isNotEmpty() }?.let { intervals ->
        "RR " + intervals.takeLast(2).joinToString(" / ") { "%.0f".format(Locale.ROOT, it) } + " ms"
    } ?: "RR --"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            BpmHeader(bpm, hasSample, contact, bpmColor, overlayVisible, onToggleOverlay)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusChip(contactText, contactColor)
                StatusChip(rrText, Muted)
            }
            if (sample != null && sample.warnings.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.warnings_summary,
                        sample.warnings.joinToString("; ") { it.asUiText().resolve(context) },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = WarnAmber,
                )
            }
            if (hasSample && (bpm == 0 || contact == SensorContact.NOT_DETECTED)) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (bpm == 0) {
                        stringResource(R.string.no_pulse_description)
                    } else {
                        stringResource(R.string.no_contact_description)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WarnAmber,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = summary,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = if (state.connection == ConnectionPhase.ERROR) ErrorRed else Muted,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BpmHeader(
    bpm: Int,
    hasSample: Boolean,
    contact: SensorContact,
    bpmColor: Color,
    overlayVisible: Boolean,
    onToggleOverlay: () -> Unit,
) {
    val heartSize = 70.dp
    val heartGap = 18.dp
    val unitGap = 6.dp
    val bpmStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 84.sp,
        lineHeight = 86.sp,
        fontWeight = FontWeight.Black,
    )
    val unitStyle = MaterialTheme.typography.labelLarge
    val textMeasurer = rememberTextMeasurer()
    // Keep the button in the same row as readings change between two and three digits.
    val readoutTextWidth = remember(textMeasurer, bpmStyle, unitStyle) {
        ('0'..'9').maxOf { textMeasurer.measure(it.toString().repeat(3), style = bpmStyle).size.width } +
            textMeasurer.measure("BPM", style = unitStyle).size.width
    }
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingHeart(bpm, contact, heartSize)
                Spacer(Modifier.width(heartGap))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(unitGap)) {
                    Text(
                        text = if (hasSample) bpm.toString() else "--",
                        modifier = Modifier.align(Alignment.Bottom),
                        style = bpmStyle,
                        color = bpmColor,
                    )
                    Text(
                        text = "BPM",
                        modifier = Modifier.align(Alignment.Bottom).padding(bottom = 14.dp),
                        style = unitStyle,
                        color = Muted,
                    )
                }
            }
            OverlayToggle(overlayVisible, onToggleOverlay)
        },
    ) { (readout, toggle), constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val readoutPlaceable = readout.measure(childConstraints)
        val togglePlaceable = toggle.measure(childConstraints)
        val width = constraints.maxWidth
        val gap = 8.dp.roundToPx()
        val reservedReadoutWidth = heartSize.roundToPx() + heartGap.roundToPx() +
            unitGap.roundToPx() + readoutTextWidth
        val fitsOnFirstRow = maxOf(readoutPlaceable.width, reservedReadoutWidth) + gap + togglePlaceable.width <= width
        val toggleTop = if (fitsOnFirstRow) 0 else readoutPlaceable.height
        layout(width, maxOf(readoutPlaceable.height, toggleTop + togglePlaceable.height)) {
            readoutPlaceable.placeRelative(x = 0, y = 0)
            togglePlaceable.placeRelative(x = width - togglePlaceable.width, y = toggleTop)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun OverlayToggle(visible: Boolean, onToggle: () -> Unit) {
    val action = stringResource(if (visible) R.string.overlay_close else R.string.overlay_show)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(action) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(if (visible) R.drawable.ic_close else R.drawable.ic_overlay),
                contentDescription = action,
                tint = if (visible) Accent else Muted,
            )
        }
    }
}

@Composable
private fun PulsingHeart(bpm: Int, contact: SensorContact, heartSize: Dp) {
    val beatMillis = if (bpm > 0) (60_000 / bpm / 2).coerceIn(180, 900) else 1_200
    val transition = rememberInfiniteTransition(label = "heartbeat")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = if (bpm > 0) 1.08f else 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = beatMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heartScale",
    )
    val color = when {
        bpm <= 0 -> Muted
        contact == SensorContact.NOT_DETECTED -> WarnAmber
        else -> Accent
    }
    Canvas(
        modifier = Modifier
            .size(heartSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        val base = size.minDimension / 38f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val path = Path()
        for (index in 0..71) {
            val angle = index * (2.0 * PI / 72.0)
            val x = 16.0 * sin(angle) * sin(angle) * sin(angle)
            val y = 13.0 * cos(angle) -
                5.0 * cos(2.0 * angle) -
                2.0 * cos(3.0 * angle) -
                cos(4.0 * angle)
            val px = centerX + (x * base).toFloat()
            val py = centerY - (y * base).toFloat()
            if (index == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.close()
        drawPath(path = path, color = color)
    }
}
