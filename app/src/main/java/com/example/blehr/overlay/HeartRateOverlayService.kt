package com.example.blehr.overlay

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.blehr.MainActivity
import com.example.blehr.R
import com.example.blehr.data.heartrate.HeartRateRepository
import com.example.blehr.data.settings.LanguageSettings
import com.example.blehr.data.settings.SettingsRepository
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HeartRateOverlayService : Service() {

    companion object {
        private const val TAG = "HeartRateOverlay"
        private const val CHANNEL_ID = "heart_rate_overlay"
        private const val NOTIFICATION_ID = 0x4852
        private const val ACTION_SHOW = "com.example.blehr.action.SHOW_OVERLAY"
        private const val ACTION_RESTORE = "com.example.blehr.action.RESTORE_OVERLAY"
        private const val ACTION_HIDE = "com.example.blehr.action.HIDE_OVERLAY"

        fun show(context: Context) = start(context, ACTION_SHOW)

        fun restoreIfEnabled(context: Context) {
            if (!SettingsRepository.get(context).isOverlayEnabled || !Settings.canDrawOverlays(context)) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
            ) return

            try {
                start(context, ACTION_RESTORE)
            } catch (exc: IllegalStateException) {
                Log.w(TAG, "Could not restore the overlay from this activity state", exc)
            } catch (exc: SecurityException) {
                Log.w(TAG, "Could not restore the overlay with current permissions", exc)
            }
        }

        private fun start(context: Context, action: String) {
            val intent = Intent(context, HeartRateOverlayService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository by lazy { HeartRateRepository.get(this) }
    private val settings by lazy { SettingsRepository.get(this) }
    private lateinit var windowManager: WindowManager
    private var overlay: HeartRateOverlayView? = null
    private var params: WindowManager.LayoutParams? = null
    private var foregroundStarted = false
    private val languageListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        refreshNotificationLanguage()
    }

    private fun localizedString(@StringRes resourceId: Int, vararg arguments: Any): String =
        LanguageSettings.localizedContext(this).getString(resourceId, *arguments)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        LanguageSettings.preferences(this).registerOnSharedPreferenceChangeListener(languageListener)
        scope.launch {
            repository.ui.collect { state ->
                overlay?.bindState(state)
            }
        }
        scope.launch {
            settings.appearance.collect { appearance ->
                overlay?.bindAppearance(appearance)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HIDE) {
            hideOverlay()
            return START_NOT_STICKY
        }
        if (!startForegroundCompat()) {
            stopSelf()
            return START_NOT_STICKY
        }
        val restoring = intent?.action == ACTION_RESTORE
        // A close action may have arrived while the activity's restore request was queued.
        if (restoring && !settings.isOverlayEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            if (!restoring) {
                Toast.makeText(this, localizedString(R.string.overlay_permission_denied), Toast.LENGTH_SHORT).show()
            }
            stopSelf()
            return START_NOT_STICKY
        }
        showOverlay(restoring)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshNotificationLanguage()
        val view = overlay ?: return
        val layout = params ?: return
        clampToScreen(view, layout)
        windowManager.updateViewLayout(view, layout)
    }

    private fun refreshNotificationLanguage() {
        createNotificationChannel()
        if (foregroundStarted && (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        ) {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
        }
    }

    override fun onDestroy() {
        LanguageSettings.preferences(this).unregisterOnSharedPreferenceChangeListener(languageListener)
        removeOverlay()
        scope.cancel()
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        super.onDestroy()
    }

    private fun startForegroundCompat(): Boolean {
        if (foregroundStarted) return true
        return try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            foregroundStarted = true
            true
        } catch (exc: Exception) {
            Log.e(TAG, "startForeground failed", exc)
            Toast.makeText(this, localizedString(R.string.overlay_service_failed, exc.message ?: localizedString(R.string.unknown_error)), Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun showOverlay(restoring: Boolean) {
        if (overlay != null) return
        val view = HeartRateOverlayView(
            this,
            onClose = ::hideOverlay,
            onDrag = ::dragBy,
        )
        val layout = WindowManager.LayoutParams(
            view.windowWidthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - view.windowWidthPx - view.windowMarginPx)
                .coerceAtLeast(0)
            y = view.initialTopPx
            title = "BleHrOverlay"
        }
        try {
            windowManager.addView(view, layout)
            overlay = view
            params = layout
            view.bindAppearance(settings.appearance.value)
            view.bindState(repository.ui.value)
            if (!restoring) {
                settings.setOverlayEnabled(true)
                Toast.makeText(this, localizedString(R.string.overlay_gestures), Toast.LENGTH_SHORT).show()
            }
        } catch (exc: Exception) {
            Log.e(TAG, "add overlay view failed", exc)
            Toast.makeText(this, localizedString(R.string.overlay_add_failed, exc.message ?: localizedString(R.string.unknown_error)), Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    private fun hideOverlay() {
        settings.setOverlayEnabled(false)
        removeOverlay()
        stopSelf()
    }

    private fun dragBy(dx: Int, dy: Int) {
        val view = overlay ?: return
        val layout = params ?: return
        layout.x += dx
        layout.y += dy
        clampToScreen(view, layout)
        windowManager.updateViewLayout(view, layout)
    }

    private fun clampToScreen(view: HeartRateOverlayView, layout: WindowManager.LayoutParams) {
        val screen = resources.displayMetrics
        val width = if (view.width > 0) view.width else view.windowWidthPx
        val height = if (view.height > 0) view.height else view.fallbackWindowHeightPx
        layout.x = layout.x.coerceIn(0, max(0, screen.widthPixels - width))
        layout.y = layout.y.coerceIn(0, max(0, screen.heightPixels - height))
    }

    private fun removeOverlay() {
        val view = overlay ?: return
        overlay = null
        params = null
        try {
            windowManager.removeView(view)
        } catch (exc: IllegalArgumentException) {
            Log.w(TAG, "overlay already detached", exc)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        // Re-register to update translated names while preserving the user's channel settings.
        val channel = NotificationChannel(
            CHANNEL_ID,
            localizedString(R.string.overlay_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
            description = localizedString(R.string.overlay_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val closeIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, HeartRateOverlayService::class.java).setAction(ACTION_HIDE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_heart)
            .setContentTitle(localizedString(R.string.app_name))
            .setContentText(localizedString(R.string.overlay_notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, localizedString(R.string.overlay_close), closeIntent)
            .build()
    }
}
