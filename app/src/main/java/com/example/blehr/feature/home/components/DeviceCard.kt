package com.example.blehr.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.blehr.R
import com.example.blehr.ble.ScannedDevice
import com.example.blehr.data.heartrate.ConnectionPhase
import com.example.blehr.data.heartrate.HeartRateState
import com.example.blehr.ui.components.StatusChip
import com.example.blehr.ui.theme.Accent
import com.example.blehr.ui.theme.Ink
import com.example.blehr.ui.theme.Muted
import com.example.blehr.ui.theme.OkGreen
import com.example.blehr.ui.theme.SurfaceRaised
import com.example.blehr.ui.theme.WarnAmber

@Composable
internal fun DeviceCard(
    state: HeartRateState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (ScannedDevice) -> Unit,
    onDisconnect: () -> Unit,
    openBluetoothSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Button(onClick = {
                    if (state.scanning) onStopScan() else onStartScan()
                }) {
                    Text(if (state.scanning) stringResource(R.string.stop_scan) else stringResource(R.string.scan_ble))
                }
                if (state.connection == ConnectionPhase.CONNECTED ||
                    state.connection == ConnectionPhase.CONNECTING
                ) {
                    OutlinedButton(onClick = onDisconnect) {
                        Text(stringResource(R.string.disconnect))
                    }
                }
            }
            TextButton(onClick = openBluetoothSettings) {
                Text(stringResource(R.string.open_bluetooth_settings))
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(238.dp),
            ) {
                if (state.devices.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (state.scanning) stringResource(R.string.searching) else stringResource(R.string.no_devices),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
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
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.devices, key = { it.address }) { device ->
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
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
