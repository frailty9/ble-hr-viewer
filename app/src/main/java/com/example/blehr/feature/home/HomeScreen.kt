package com.example.blehr.feature.home

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.blehr.R
import com.example.blehr.feature.home.components.AppHeader
import com.example.blehr.feature.home.components.BpmCard
import com.example.blehr.feature.home.components.DeviceCard
import com.example.blehr.feature.home.components.ErrorCard
import com.example.blehr.feature.home.components.HelpCard
import com.example.blehr.feature.home.components.OverlayCard
import com.example.blehr.feature.home.components.PacketCard
import com.example.blehr.feature.home.components.TrendCard
import com.example.blehr.overlay.HeartRateOverlayService

@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.ui.collectAsState()
    val context = LocalContext.current
    val openBluetoothSettings: () -> Unit = {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    var overlayPending by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        overlayGranted = Settings.canDrawOverlays(context)
        if (overlayPending && overlayGranted) {
            HeartRateOverlayService.show(context)
            Toast.makeText(context, R.string.overlay_enabled, Toast.LENGTH_SHORT).show()
        } else if (overlayPending) {
            Toast.makeText(context, R.string.overlay_permission_denied, Toast.LENGTH_SHORT).show()
        }
        overlayPending = false
    }
    val showOverlay: () -> Unit = {
        if (Settings.canDrawOverlays(context)) {
            HeartRateOverlayService.show(context)
            Toast.makeText(context, R.string.overlay_gestures, Toast.LENGTH_SHORT).show()
        } else {
            overlayPending = true
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri(),
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(14.dp),
    ) {
        AppHeader(state, onOpenSettings)
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        BpmCard(state)
                        Spacer(Modifier.height(10.dp))
                        TrendCard(state)
                        Spacer(Modifier.height(10.dp))
                        PacketCard(state)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        DeviceCard(state, viewModel, openBluetoothSettings)
                        Spacer(Modifier.height(10.dp))
                        OverlayCard(overlayGranted, showOverlay)
                        Spacer(Modifier.height(10.dp))
                        ErrorCard(state)
                        Spacer(Modifier.height(10.dp))
                        HelpCard()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    BpmCard(state)
                    Spacer(Modifier.height(10.dp))
                    TrendCard(state)
                    Spacer(Modifier.height(10.dp))
                    DeviceCard(state, viewModel, openBluetoothSettings)
                    Spacer(Modifier.height(10.dp))
                    OverlayCard(overlayGranted, showOverlay)
                    Spacer(Modifier.height(10.dp))
                    ErrorCard(state)
                    Spacer(Modifier.height(10.dp))
                    PacketCard(state)
                    Spacer(Modifier.height(10.dp))
                    HelpCard()
                }
            }
        }
    }
}
