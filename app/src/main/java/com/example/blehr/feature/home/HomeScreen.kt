package com.example.blehr.feature.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.blehr.ble.ScannedDevice
import com.example.blehr.data.heartrate.HeartRateState
import com.example.blehr.feature.home.components.AppHeader
import com.example.blehr.feature.home.components.BpmCard
import com.example.blehr.feature.home.components.DeviceCard
import com.example.blehr.feature.home.components.TrendCard
import com.example.blehr.ui.theme.PageMargin

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
    // Declare each layout's scroll state outside the orientation branch so it survives rotation.
    val portraitScroll = rememberScrollState()
    val landscapeLeftScroll = rememberScrollState()
    val landscapeRightScroll = rememberScrollState()
    // This space belongs to the content, so it enters the viewport only at the end of scrolling.
    val bottomContentSpace = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
        .asPaddingValues().calculateBottomPadding() + PageMargin
    Column(
        modifier = Modifier
            .testTag("home_screen")
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = PageMargin),
    ) {
        AppHeader(state, onOpenSettings)
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewportHeight = constraints.maxHeight
            val landscape = maxWidth > maxHeight
            val nearbyDevices: @Composable () -> Unit = {
                DeviceCard(state, onStartScan, onStopScan, onConnect, onDisconnect, onOpenBluetoothSettings)
            }
            if (landscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                            .testTag("home_left_scroll")
                            .verticalScroll(landscapeLeftScroll),
                    ) {
                        BpmCard(state, overlayVisible, onToggleOverlay)
                        Spacer(Modifier.height(10.dp))
                        TrendCard(state)
                        Spacer(Modifier.height(bottomContentSpace))
                    }
                    DeviceFillColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("home_right_scroll")
                            .verticalScroll(landscapeRightScroll),
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
                        .verticalScroll(portraitScroll),
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
