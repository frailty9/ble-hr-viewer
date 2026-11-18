package io.github.frailty9.blehr.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.frailty9.blehr.ble.ScannedDevice
import io.github.frailty9.blehr.data.heartrate.HeartRateState
import io.github.frailty9.blehr.feature.home.components.AppHeader
import io.github.frailty9.blehr.feature.home.components.BpmCard
import io.github.frailty9.blehr.feature.home.components.DeviceCard
import io.github.frailty9.blehr.feature.home.components.TrendCard
import io.github.frailty9.blehr.ui.theme.PageMargin

@Composable
fun HomeScreen(
    state: HeartRateState,
    overlayVisible: Boolean,
    onOpenSettings: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (ScannedDevice) -> Unit,
    onDisconnect: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onToggleOverlay: () -> Unit,
) {
    // Keep each scroll position when a resize switches the layout.
    val singleColumnScroll = rememberScrollState()
    val leftColumnScroll = rememberScrollState()
    val rightColumnScroll = rememberScrollState()
    // This space belongs to the content, so it enters the viewport only at the end of scrolling.
    val bottomContentSpace = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
        .asPaddingValues().calculateBottomPadding() + PageMargin
    Box(
        modifier = Modifier
            .testTag("home_screen")
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = PageMargin),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 1120.dp).fillMaxSize()) {
            AppHeader(state, onOpenSettings)
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val viewportHeight = constraints.maxHeight
                val useTwoColumns = maxWidth >= 680.dp
                val nearbyDevices: @Composable () -> Unit = {
                    DeviceCard(state, onStartScan, onStopScan, onConnect, onDisconnect, onOpenBluetoothSettings)
                }
                if (useTwoColumns) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HeartRateColumn(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                                .testTag("home_left_scroll")
                                .verticalScroll(leftColumnScroll),
                            viewportHeight = viewportHeight,
                            bottomSpace = { Spacer(Modifier.height(bottomContentSpace)) },
                            bpmCard = { BpmCard(state, overlayVisible, onToggleOverlay) },
                            state = state,
                        )
                        DeviceFillColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("home_right_scroll")
                                .verticalScroll(rightColumnScroll),
                            viewportHeight = viewportHeight,
                            bottomSpace = { Spacer(Modifier.height(bottomContentSpace)) },
                            devices = nearbyDevices,
                        )
                    }
                } else {
                    DeviceFillColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("home_scroll")
                            .verticalScroll(singleColumnScroll),
                        viewportHeight = viewportHeight,
                        top = {
                            BpmCard(state, overlayVisible, onToggleOverlay)
                            Spacer(Modifier.height(10.dp))
                            TrendCard(state)
                            Spacer(Modifier.height(10.dp))
                        },
                        bottomSpace = { Spacer(Modifier.height(bottomContentSpace)) },
                        devices = nearbyDevices,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeartRateColumn(
    modifier: Modifier,
    viewportHeight: Int,
    bottomSpace: @Composable () -> Unit,
    bpmCard: @Composable () -> Unit,
    state: HeartRateState,
) {
    SubcomposeLayout(modifier) { constraints ->
        val cardConstraints = constraints.copy(minHeight = 0)
        val bpm = subcompose("bpm", bpmCard).single().measure(cardConstraints)
        val bottom = subcompose("bottom", bottomSpace).single().measure(cardConstraints)
        val trendTop = bpm.height + 10.dp.roundToPx()
        val remaining = (viewportHeight - trendTop - bottom.height).coerceAtLeast(0)
        val trend = subcompose("trend") {
            TrendCard(state, availableHeight = remaining.toDp())
        }.single().measure(cardConstraints)
        val bottomY = trendTop + trend.height
        val contentHeight = maxOf(viewportHeight, bottomY + bottom.height)
        layout(constraints.maxWidth, contentHeight) {
            bpm.placeRelative(0, 0)
            trend.placeRelative(0, trendTop)
            bottom.placeRelative(0, bottomY)
        }
    }
}

@Composable
private fun DeviceFillColumn(
    modifier: Modifier,
    viewportHeight: Int,
    top: @Composable () -> Unit = {},
    bottomSpace: @Composable () -> Unit,
    devices: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = {
        Column(content = { top() })
        devices()
        bottomSpace()
    }) { (topContent, deviceCard, bottom), constraints ->
        // Measure the other cards first; the device card can grow beyond the viewport for long lists.
        val childConstraints = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val topPlaceable = topContent.measure(childConstraints)
        val bottomPlaceable = bottom.measure(childConstraints)
        val remaining = (viewportHeight - topPlaceable.height - bottomPlaceable.height).coerceAtLeast(0)
        val devicesPlaceable = deviceCard.measure(childConstraints.copy(minHeight = remaining))
        val bottomY = topPlaceable.height + devicesPlaceable.height
        layout(constraints.maxWidth, bottomY + bottomPlaceable.height) {
            topPlaceable.placeRelative(0, 0)
            devicesPlaceable.placeRelative(0, topPlaceable.height)
            bottomPlaceable.placeRelative(0, bottomY)
        }
    }
}
