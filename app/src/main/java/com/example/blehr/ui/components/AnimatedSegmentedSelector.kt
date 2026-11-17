package com.example.blehr.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.blehr.ui.theme.GridLine
import com.example.blehr.ui.theme.Ink
import com.example.blehr.ui.theme.SurfaceRaised

@Composable
internal fun AnimatedSegmentedSelector(
    labels: List<String>,
    keys: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    tagPrefix: String,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceRaised)
            .border(1.dp, GridLine, RoundedCornerShape(8.dp)),
    ) {
        val segmentWidth = maxWidth / labels.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            label = "segmentIndicator",
        )
        Box(
            Modifier.offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(segmentWidth).fillMaxHeight()
                .padding(3.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .testTag("${tagPrefix}_indicator"),
        )
        Row(Modifier.fillMaxSize().selectableGroup()) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer else Ink,
                    animationSpec = tween(160),
                    label = "segmentLabel",
                )
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .testTag("${tagPrefix}_${keys[index]}")
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) },
                        )
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
