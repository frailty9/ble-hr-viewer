package io.github.frailty9.blehr.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.data.heartrate.ConnectionPhase
import io.github.frailty9.blehr.data.heartrate.HeartRateState
import io.github.frailty9.blehr.ui.theme.Accent
import io.github.frailty9.blehr.ui.theme.GridLine
import io.github.frailty9.blehr.ui.theme.Ink
import io.github.frailty9.blehr.ui.theme.LocalAppShapes
import io.github.frailty9.blehr.ui.theme.Muted
import kotlin.math.max

@Composable
internal fun TrendCard(state: HeartRateState, availableHeight: Dp = 0.dp) {
    val history = state.history
    val padding = 18.dp
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Layout(
            modifier = Modifier.fillMaxWidth().padding(padding),
            content = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                Box(
                    modifier = Modifier.fillMaxWidth(),
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
                Row(Modifier.fillMaxWidth()) {
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
            },
        ) { (title, plot, footer), constraints ->
            val childConstraints = constraints.copy(minHeight = 0)
            val titlePlaceable = title.measure(childConstraints)
            val footerPlaceable = footer.measure(childConstraints)
            val topGap = 12.dp.roundToPx()
            val bottomGap = 8.dp.roundToPx()
            val chromeHeight = 2 * padding.roundToPx() + titlePlaceable.height + footerPlaceable.height + topGap + bottomGap
            val plotHeight = (availableHeight.roundToPx() - chromeHeight)
                .coerceIn(108.dp.roundToPx(), 320.dp.roundToPx())
            val plotPlaceable = plot.measure(childConstraints.copy(minHeight = plotHeight, maxHeight = plotHeight))
            val plotTop = titlePlaceable.height + topGap
            val footerTop = plotTop + plotPlaceable.height + bottomGap
            layout(constraints.maxWidth, footerTop + footerPlaceable.height) {
                titlePlaceable.placeRelative(0, 0)
                plotPlaceable.placeRelative(0, plotTop)
                footerPlaceable.placeRelative(0, footerTop)
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
