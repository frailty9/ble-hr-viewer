package io.github.frailty9.blehr.feature.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.core.findActivity
import io.github.frailty9.blehr.overlay.HeartRateOverlayService

@Composable
fun HomeRoute(viewModel: HomeViewModel, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val overlayVisible by HeartRateOverlayService.isVisible.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val blePermissions = remember { requiredBlePermissions() }
    var scanPermissionPending by rememberSaveable { mutableStateOf(false) }
    var showPermissionSettings by rememberSaveable { mutableStateOf(false) }
    var overlayPending by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (scanPermissionPending) {
            scanPermissionPending = false
            when {
                hasBlePermissions(context, blePermissions) -> {
                    viewModel.startScan()
                    HeartRateOverlayService.restoreIfEnabled(context)
                }
                context.findActivity()?.let { activity ->
                    blePermissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
                } == false -> showPermissionSettings = true
                else -> Toast.makeText(context, R.string.scan_permission_missing, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (overlayPending && Settings.canDrawOverlays(context)) {
            requestNotificationPermission()
            HeartRateOverlayService.show(context)
        } else if (overlayPending) {
            Toast.makeText(context, R.string.overlay_permission_denied, Toast.LENGTH_SHORT).show()
        }
        overlayPending = false
    }
    DisposableEffect(lifecycleOwner, context, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            // The scan that requested permission takes precedence over a startup reconnect.
            if (event == Lifecycle.Event.ON_RESUME && !scanPermissionPending &&
                hasBlePermissions(context, blePermissions)
            ) {
                viewModel.reconnectAtStartup()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    HomeScreen(
        state = state,
        overlayVisible = overlayVisible,
        onOpenSettings = onOpenSettings,
        onStartScan = {
            if (!scanPermissionPending) {
                if (hasBlePermissions(context, blePermissions)) {
                    viewModel.startScan()
                } else {
                    scanPermissionPending = true
                    permissionLauncher.launch(blePermissions)
                }
            }
        },
        onStopScan = viewModel::stopScan,
        onConnect = viewModel::connect,
        onDisconnect = viewModel::disconnect,
        onOpenBluetoothSettings = {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        },
        onToggleOverlay = {
            when {
                HeartRateOverlayService.isVisible.value -> HeartRateOverlayService.hide(context)
                !hasBlePermissions(context, blePermissions) -> {
                    Toast.makeText(context, R.string.overlay_scan_permission_required, Toast.LENGTH_SHORT).show()
                }
                Settings.canDrawOverlays(context) -> {
                    requestNotificationPermission()
                    HeartRateOverlayService.show(context)
                }
                !overlayPending -> {
                    overlayPending = true
                    overlayPermissionLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
                    )
                }
            }
        },
    )
    if (showPermissionSettings) {
        AlertDialog(
            onDismissRequest = { showPermissionSettings = false },
            text = { Text(stringResource(R.string.scan_permission_settings_hint)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionSettings = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()),
                    )
                }) { Text(stringResource(R.string.open_app_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettings = false }) { Text(stringResource(R.string.cancel)) }
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
