package com.example.blehr.feature.home.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blehr.R
import com.example.blehr.ble.SensorContact
import com.example.blehr.core.text.asUiText
import com.example.blehr.data.heartrate.ConnectionPhase
import com.example.blehr.data.heartrate.HeartRateState
import com.example.blehr.ui.components.StatusChip
import com.example.blehr.ui.theme.Accent
import com.example.blehr.ui.theme.ErrorRed
import com.example.blehr.ui.theme.LocalAppShapes
import com.example.blehr.ui.theme.Muted
import com.example.blehr.ui.theme.OkGreen
import com.example.blehr.ui.theme.WarnAmber
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun BpmCard(state: HeartRateState) {
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
    val deviceLine = when {
        state.connection == ConnectionPhase.CONNECTED ->
            "${state.connectedName ?: stringResource(R.string.unknown_device)} · ${state.connectedAddress ?: "--"}"
        state.connection == ConnectionPhase.CONNECTING -> stringResource(R.string.device_connecting)
        state.connection == ConnectionPhase.RECONNECTING -> state.connectedName ?: state.connectedAddress.orEmpty()
        state.connection == ConnectionPhase.SCANNING -> stringResource(R.string.device_scanning)
        state.connection == ConnectionPhase.ERROR -> stringResource(R.string.device_connection_error)
        else -> stringResource(R.string.device_not_connected)
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
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingHeart(bpm = bpm, contact = contact)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (hasSample) bpm.toString() else "--",
                            fontSize = 84.sp,
                            lineHeight = 86.sp,
                            fontWeight = FontWeight.Black,
                            color = bpmColor,
                        )
                        Text(
                            text = "BPM",
                            modifier = Modifier.padding(start = 6.dp, bottom = 14.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Muted,
                        )
                    }
                    Text(
                        text = deviceLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
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
                text = state.status.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (state.connection == ConnectionPhase.ERROR) ErrorRed else Muted,
            )
        }
    }
}


@Composable
private fun PulsingHeart(bpm: Int, contact: SensorContact) {
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
            .size(70.dp)
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
