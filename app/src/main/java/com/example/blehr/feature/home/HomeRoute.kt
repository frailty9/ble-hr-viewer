package com.example.blehr.feature.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blehr.R
import com.example.blehr.overlay.HeartRateOverlayService

@Composable
fun HomeRoute(viewModel: HomeViewModel, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val blePermissions = remember { requiredBlePermissions() }
    val requestedPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            blePermissions + Manifest.permission.POST_NOTIFICATIONS
        } else blePermissions
    }
    var granted by remember { mutableStateOf(hasBlePermissions(context, blePermissions)) }
    var asked by rememberSaveable { mutableStateOf(false) }
    var overlayPending by rememberSaveable { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted = hasBlePermissions(context, blePermissions) }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        overlayGranted = Settings.canDrawOverlays(context)
        if (overlayPending && overlayGranted) {
            HeartRateOverlayService.show(context)
        } else if (overlayPending) {
            Toast.makeText(context, R.string.overlay_permission_denied, Toast.LENGTH_SHORT).show()
        }
        overlayPending = false
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = hasBlePermissions(context, blePermissions)
                overlayGranted = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        if (!granted && !asked) {
            asked = true
            permissionLauncher.launch(requestedPermissions)
        }
    }
    if (granted) {
        HomeScreen(
            state = state,
            overlayGranted = overlayGranted,
            onOpenSettings = onOpenSettings,
            onStartScan = viewModel::startScan,
            onStopScan = viewModel::stopScan,
            onConnect = viewModel::connect,
            onDisconnect = viewModel::disconnect,
            onOpenBluetoothSettings = {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            },
            onShowOverlay = {
                if (Settings.canDrawOverlays(context)) {
                    HeartRateOverlayService.show(context)
                } else {
                    overlayPending = true
                    overlayPermissionLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
                    )
                }
            },
        )
    } else {
        PermissionGate(
            onOpenSettingsPage = onOpenSettings,
            onRequest = { permissionLauncher.launch(requestedPermissions) },
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
            },
        )
    }
}

private fun requiredBlePermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

private fun hasBlePermissions(
    context: Context,
    permissions: Array<String>,
): Boolean = permissions.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}
