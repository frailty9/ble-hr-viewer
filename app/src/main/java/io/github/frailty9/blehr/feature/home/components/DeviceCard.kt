package io.github.frailty9.blehr.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.ble.ScannedDevice
import io.github.frailty9.blehr.data.heartrate.ConnectionPhase
import io.github.frailty9.blehr.data.heartrate.HeartRateState
import io.github.frailty9.blehr.ui.components.StatusChip
import io.github.frailty9.blehr.ui.theme.Accent
import io.github.frailty9.blehr.ui.theme.ErrorRed
import io.github.frailty9.blehr.ui.theme.Ink
import io.github.frailty9.blehr.ui.theme.LocalAppShapes
import io.github.frailty9.blehr.ui.theme.Muted
import io.github.frailty9.blehr.ui.theme.OkGreen
import io.github.frailty9.blehr.ui.theme.SurfaceRaised
import io.github.frailty9.blehr.ui.theme.WarnAmber

@Composable
internal fun DeviceCard(
    state: HeartRateState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (ScannedDevice) -> Unit,
    onDisconnect: () -> Unit,
    openBluetoothSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("nearby_devices_card"),
        shape = LocalAppShapes.current.card,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.nearby_devices),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                if (state.scanning) {
                    StatusChip(stringResource(R.string.phase_scanning), WarnAmber)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { if (state.scanning) onStopScan() else onStartScan() },
                    shape = LocalAppShapes.current.button,
                ) {
                    Text(if (state.scanning) stringResource(R.string.stop_scan) else stringResource(R.string.scan_ble))
                }
                if (state.connection == ConnectionPhase.CONNECTED ||
                    state.connection == ConnectionPhase.CONNECTING ||
                    state.connection == ConnectionPhase.RECONNECTING
                ) {
                    OutlinedButton(onClick = onDisconnect, shape = LocalAppShapes.current.button) {
                        Text(stringResource(R.string.disconnect))
                    }
                }
            }
            TextButton(onClick = openBluetoothSettings, shape = LocalAppShapes.current.button) {
                Text(stringResource(R.string.open_bluetooth_settings))
            }
            state.scanError?.let {
                Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = ErrorRed)
            }
            if (state.scanning) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = WarnAmber,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (state.devices.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (state.scanning) stringResource(R.string.searching) else stringResource(R.string.no_devices),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.scan_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (device in state.devices) {
                        key(device.address) {
                            DeviceRow(
                                device = device,
                                selected = state.connection == ConnectionPhase.CONNECTED &&
                                    device.address == state.connectedAddress,
                                onClick = { onConnect(device) },
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DeviceRow(
    device: ScannedDevice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = LocalAppShapes.current.row,
        color = if (selected) Accent.copy(alpha = 0.12f) else SurfaceRaised,
        border = if (selected) BorderStroke(1.dp, Accent) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = device.name ?: stringResource(R.string.unnamed_device),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Muted,
                )
            }
            if (device.hasHeartRateService) {
                StatusChip("HR", OkGreen)
            }
            Text(
                text = "${device.rssi} dBm",
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
            )
        }
    }
}
