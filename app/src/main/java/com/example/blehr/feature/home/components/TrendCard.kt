package com.example.blehr.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.blehr.R
import com.example.blehr.data.heartrate.ConnectionPhase
import com.example.blehr.data.heartrate.HeartRateState
import com.example.blehr.ui.theme.Accent
import com.example.blehr.ui.theme.GridLine
import com.example.blehr.ui.theme.Ink
import com.example.blehr.ui.theme.LocalAppShapes
import com.example.blehr.ui.theme.Muted
import kotlin.math.max

@Composable
internal fun TrendCard(state: HeartRateState) {
    val history = state.history
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.trend_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (history.isEmpty()) "-- BPM" else "${history.last()} BPM",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Accent,
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
            ) {
                Sparkline(history, Modifier.fillMaxSize())
                if (history.size < 2) {
                    Text(
                        text = stringResource(
                            if (state.connection == ConnectionPhase.CONNECTED) {
                                R.string.trend_waiting
                            } else {
                                R.string.trend_no_live_data
                            },
                        ),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    text = if (history.isEmpty()) stringResource(R.string.trend_range_empty) else
                        stringResource(R.string.trend_range, history.minOrNull() ?: 0, history.maxOrNull() ?: 0),
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = pluralStringResource(R.plurals.recent_notifications, history.size, history.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                )
            }
        }
    }
}


@Composable
private fun Sparkline(values: List<Int>, modifier: Modifier) {
    val gridColor = GridLine
    val lineColor = Accent
    Canvas(modifier) {
        val width = size.width
        val height = size.height
        for (index in 1..3) {
            val y = height * index / 4f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
            )
        }
        if (values.size < 2) return@Canvas

        val minimum = values.minOrNull() ?: 0
        val maximum = values.maxOrNull() ?: 0
        val span = max(10, maximum - minimum).toFloat()
        val low = minimum - span * 0.15f
        val high = maximum + span * 0.15f
        val denominator = (values.size - 1).toFloat()

        fun xAt(index: Int): Float = width * index / denominator
        fun yAt(value: Int): Float = height - height * (value - low) / (high - low)

        val path = Path()
        values.forEachIndexed { index, value ->
            if (index == 0) {
                path.moveTo(xAt(index), yAt(value))
            } else {
                path.lineTo(xAt(index), yAt(value))
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        drawCircle(
            color = lineColor,
            radius = 3.5.dp.toPx(),
            center = Offset(xAt(values.lastIndex), yAt(values.last())),
        )
    }
}
