package io.github.frailty9.blehr.feature.settings

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inspector.WindowInspector
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.frailty9.blehr.MainActivity
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.ble.HeartRateParser
import io.github.frailty9.blehr.data.heartrate.HeartRateRepository
import io.github.frailty9.blehr.data.heartrate.HeartRateState
import io.github.frailty9.blehr.data.privacy.PrivacyConsentStore
import io.github.frailty9.blehr.data.settings.AppLanguage
import io.github.frailty9.blehr.data.settings.AppTheme
import io.github.frailty9.blehr.data.settings.LanguageSettings
import io.github.frailty9.blehr.data.settings.HeartRateColorRule
import io.github.frailty9.blehr.data.settings.HeartRateColorRules
import io.github.frailty9.blehr.data.settings.OverlayAppearance
import io.github.frailty9.blehr.data.settings.SettingsRepository
import io.github.frailty9.blehr.feature.settings.components.LanguageSelector
import io.github.frailty9.blehr.overlay.HeartRateOverlayService
import io.github.frailty9.blehr.overlay.HeartRateOverlayView
import io.github.frailty9.blehr.ui.theme.BleHrTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsExperienceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var activity: MainActivity
    private lateinit var repository: SettingsRepository
    private lateinit var originalAppearance: OverlayAppearance
    private lateinit var originalLanguage: AppLanguage
    private lateinit var originalTheme: AppTheme
    private var originalTimeoutSeconds = 5

    @Before
    fun launchActualActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        PrivacyConsentStore.accept(context)
        repository = SettingsRepository.get(context)
        originalAppearance = repository.appearance.value
        originalTheme = repository.theme.value
        originalTimeoutSeconds = repository.heartRateTimeoutSeconds.value
        originalLanguage = LanguageSettings.selected(context)
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT")
        } else listOf("android.permission.ACCESS_FINE_LOCATION")
        for (permission in permissions) {
            instrumentation.uiAutomation.executeShellCommand("pm grant ${instrumentation.targetContext.packageName} $permission")
                .use { java.io.FileInputStream(it.fileDescriptor).use { input -> input.readBytes() } }
        }
        instrumentation.uiAutomation.executeShellCommand("am start -W -n ${instrumentation.targetContext.packageName}/${MainActivity::class.java.name}")
            .use { java.io.FileInputStream(it.fileDescriptor).use { input -> input.readBytes() } }
        compose.waitUntil(10_000) {
            instrumentation.runOnMainSync {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>().firstOrNull()?.let { activity = it }
            }
            ::activity.isInitialized
        }
    }

    @After
    fun restoreUserSettings() {
        compose.mainClock.autoAdvance = true
        if (::activity.isInitialized) compose.runOnUiThread {
            LanguageSettings.apply(activity, originalLanguage)
            repository.setTheme(originalTheme)
            repository.setHeartRateTimeoutSeconds(originalTimeoutSeconds)
            repository.setBackgroundColor(originalAppearance.backgroundColor)
            repository.setBackgroundAlpha(originalAppearance.backgroundAlpha)
            repository.setFontColor(originalAppearance.fontColor)
            repository.setFontAlpha(originalAppearance.fontAlpha)
            repository.setOverlayShowHeart(originalAppearance.showHeart)
            repository.setOverlayShowUnit(originalAppearance.showUnit)
            repository.setHeartRateColorRules(originalAppearance.colorRules)
            activity.finish()
        }
    }

    private fun openSettings() {
        compose.onNodeWithContentDescription(activity.getString(R.string.settings_title)).performClick()
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
    }

    @Test
    fun timeoutSliderPersistsWholeSecondsAndResetsIndependentlyOfOverlayAppearance() {
        openSettings()
        val slider = compose.onNodeWithContentDescription(activity.getString(R.string.heart_rate_timeout_title))
        slider.performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }
        compose.runOnIdle { assertEquals(30, repository.heartRateTimeoutSeconds.value) }
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(3f) }
        compose.runOnIdle {
            assertEquals(3, repository.heartRateTimeoutSeconds.value)
            assertEquals(3, activity.getSharedPreferences("overlay_appearance", 0)
                .getInt("heart_rate_timeout_seconds", -1))
        }
        compose.onNodeWithTag("overlay_appearance_reset").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(3, repository.heartRateTimeoutSeconds.value) }
        compose.onNodeWithTag("background_opacity_slider").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.79f) }
        compose.onNodeWithTag("heart_rate_timeout_reset").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(5, repository.heartRateTimeoutSeconds.value)
            assertEquals(5, activity.getSharedPreferences("overlay_appearance", 0)
                .getInt("heart_rate_timeout_seconds", -1))
            assertEquals(0.79f, repository.appearance.value.backgroundAlpha)
        }
    }

    @Test
    fun changingLanguageKeepsTheSameActivityAndSettingsDestination() {
        openSettings()
        val before = activity
        for ((tag, title) in listOf("en" to "Language", "zh" to "语言")) {
            compose.onNodeWithTag("language_$tag").performClick()
            compose.onNodeWithText(title).assertIsDisplayed()
            compose.onNodeWithTag("language_$tag").assertIsSelected()
            compose.onNodeWithTag("settings_screen").assertIsDisplayed()
            compose.runOnIdle {
                val current = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>().single()
                assertSame("Language changes must not recreate the Activity (changes=0x${before.changingConfigurations.toString(16)})", before, current)
            }
        }
        compose.onNodeWithTag("language_system").performClick()
        compose.onNodeWithTag("language_system").assertIsSelected()
        compose.runOnIdle {
            val current = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().single()
            assertSame(before, current)
        }
    }

    @Test
    fun slidersAcceptEndpointsAndResetUsesTheRequestedDefaults() {
        openSettings()
        compose.onNodeWithTag("background_opacity_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.onNodeWithTag("font_opacity_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        compose.runOnIdle {
            assertEquals(0f, repository.appearance.value.backgroundAlpha)
            assertEquals(1f, repository.appearance.value.fontAlpha)
        }
        compose.onNodeWithTag("background_opacity_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.79f) }
        compose.onNodeWithTag("font_opacity_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.22f) }
        compose.onNodeWithTag("overlay_appearance_reset").performScrollTo().performClick()
        compose.runOnIdle {
            val defaults = repository.appearance.value
            assertEquals(0xFF141A22.toInt(), defaults.backgroundColor)
            assertEquals(0.30f, defaults.backgroundAlpha)
            assertEquals(0xFFFF4D6D.toInt(), defaults.fontColor)
            assertEquals(0.65f, defaults.fontAlpha)
        }
    }

    @Test
    fun themeChangesKeepDestinationActivityAndOverlayAndSurviveRecreation() {
        openSettings()
        val before = activity
        for ((key, dark) in listOf("light" to false, "dark" to true)) {
            compose.onNodeWithTag("theme_$key").performScrollTo().performClick()
            compose.onNodeWithTag("theme_$key").assertIsSelected()
            compose.onNodeWithTag("settings_screen").assertIsDisplayed()
            assertThemeBackground(dark)
            compose.runOnIdle {
                assertSame(before, resumedActivity())
                assertEquals(originalAppearance, repository.appearance.value)
                assertEquals(key, repository.theme.value.key)
                assertEquals(key, activity.getSharedPreferences("overlay_appearance", 0).getString("theme_mode", null))
                assertEquals(!dark, WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
                assertEquals(!dark, WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightNavigationBars)
            }
        }
        compose.runOnUiThread { before.recreate() }
        compose.waitUntil(10_000) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                resumedActivity()?.takeIf { it !== before }?.let { activity = it }
            }
            activity !== before
        }
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
        compose.onNodeWithTag("theme_dark").assertIsSelected()
        compose.runOnIdle {
            assertEquals(AppTheme.DARK, repository.theme.value)
            assertEquals(false, WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
        }
        compose.onNodeWithTag("overlay_appearance_reset").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(AppTheme.DARK, repository.theme.value) }
    }

    @Test
    fun followSystemRespondsToNightModeWithoutRecreatingActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command)
            .use { descriptor -> java.io.FileInputStream(descriptor.fileDescriptor).use { String(it.readBytes()) } }
        val originalNightMode = shell("cmd uimode night").substringAfter(":").trim()
        try {
            openSettings()
            compose.onNodeWithTag("theme_system").performScrollTo().performClick()
            val before = activity
            for ((mode, dark) in listOf("yes" to true, "no" to false)) {
                shell("cmd uimode night $mode")
                compose.waitUntil(10_000) {
                    var updated = false
                    instrumentation.runOnMainSync {
                        updated = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                            .isAppearanceLightStatusBars == !dark
                    }
                    updated
                }
                compose.onNodeWithTag("theme_system").assertIsSelected()
                compose.onNodeWithTag("settings_screen").assertIsDisplayed()
                assertThemeBackground(dark)
                compose.runOnIdle {
                    assertSame(before, resumedActivity())
                    assertEquals(originalAppearance, repository.appearance.value)
                }
            }
        } finally {
            shell("cmd uimode night $originalNightMode")
        }
    }

    @Test
    fun overlayButtonAndNotificationCloseStayInSyncWithTheSavedState() {
        assumeTrue("Overlay permission is needed on the test device", Settings.canDrawOverlays(activity))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            automation.executeShellCommand("pm grant ${activity.packageName} $permission")
                .use { java.io.FileInputStream(it.fileDescriptor).use { input -> input.readBytes() } }
            assertEquals(
                "The notification close action requires notification permission",
                PackageManager.PERMISSION_GRANTED,
                activity.checkSelfPermission(permission),
            )
        }
        val wasEnabled = repository.isOverlayEnabled
        val notifications = activity.getSystemService(NotificationManager::class.java)
        val strings = LanguageSettings.localizedContext(activity)
        val show = compose.onNodeWithContentDescription(strings.getString(R.string.overlay_show))
        val hide = compose.onNodeWithContentDescription(strings.getString(R.string.overlay_close))
        try {
            compose.runOnUiThread { HeartRateOverlayService.hide(activity) }
            compose.waitUntil(10_000) { !repository.isOverlayEnabled && !HeartRateOverlayService.isVisible.value }
            show.assertIsDisplayed().performClick()
            compose.waitUntil(10_000) { repository.isOverlayEnabled && HeartRateOverlayService.isVisible.value }
            hide.assertIsDisplayed().performClick()
            compose.waitUntil(10_000) { !repository.isOverlayEnabled && !HeartRateOverlayService.isVisible.value }
            show.assertIsDisplayed().performClick()
            compose.waitUntil(10_000) {
                repository.isOverlayEnabled && HeartRateOverlayService.isVisible.value &&
                    notifications.activeNotifications.any { it.id == 0x4852 }
            }
            val close = notifications.activeNotifications.single { it.id == 0x4852 }.notification.actions.single()
            close.actionIntent.send()
            compose.waitUntil(10_000) {
                !repository.isOverlayEnabled && !HeartRateOverlayService.isVisible.value &&
                    notifications.activeNotifications.none { it.id == 0x4852 }
            }
            show.assertIsDisplayed()
            compose.runOnUiThread { HeartRateOverlayService.restoreIfEnabled(activity) }
            compose.runOnIdle { assertFalse(repository.isOverlayEnabled) }
        } finally {
            compose.runOnUiThread {
                repository.setOverlayEnabled(wasEnabled)
                if (wasEnabled) {
                    HeartRateOverlayService.restoreIfEnabled(activity)
                } else {
                    activity.stopService(android.content.Intent(activity, HeartRateOverlayService::class.java))
                }
            }
        }
    }

    @Test
    fun overlayOptionsSaveAListAndResetTogether() {
        openSettings()
        val heart = compose.onNodeWithTag("overlay_show_heart").performScrollTo()
        val unit = compose.onNodeWithTag("overlay_show_unit").performScrollTo()
        heart.assertIsOn().performClick().assertIsOff()
        unit.assertIsOff().performClick().assertIsOn()
        heart.assertIsOff()
        val input = compose.onNodeWithTag("overlay_threshold_input")
        val save = compose.onNodeWithTag("overlay_threshold_save")
        input.performScrollTo().performTextReplacement("251")
        save.assertIsNotEnabled()
        input.performTextReplacement("120")
        save.performScrollTo().performClick()
        compose.runOnIdle {
            val preferences = activity.getSharedPreferences("overlay_appearance", 0)
            assertFalse(preferences.getBoolean("overlay_show_heart", true))
            assertTrue(preferences.getBoolean("overlay_show_unit", false))
            val rules = listOf(HeartRateColorRule(120, 0xFF0000))
            assertEquals(rules, repository.appearance.value.colorRules)
            assertEquals(rules, HeartRateColorRules.decode(preferences.getString("heart_rate_color_rules", null)))
            assertTrue(HeartRateColorRules.decode("""[{"minBpm":120.5,"foregroundRgb":0}]""").isEmpty())
            assertTrue(HeartRateColorRules.decode("""[{"minBpm":120,"foregroundRgb":0},{"minBpm":120,"foregroundRgb":1}]""").isEmpty())
        }
        compose.onNodeWithTag("overlay_threshold_preview").performScrollTo().performClick()
        input.performScrollTo().performTextClearance()
        save.performScrollTo().performClick()
        compose.runOnIdle { assertTrue(repository.appearance.value.colorRules.isEmpty()) }
        compose.onNodeWithTag("overlay_appearance_reset").performScrollTo().performClick()
        heart.assertIsOn()
        unit.assertIsOff()
        compose.runOnIdle { assertEquals(OverlayAppearance.Default, repository.appearance.value) }
    }

    @Test
    fun liveOverlayChangesLayoutAndColorForSimulatedReadings() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Settings.canDrawOverlays(activity))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val heartRate = HeartRateRepository.get(activity)
        // Supply samples without a BLE peripheral or a production test entry point.
        val field = HeartRateRepository::class.java.getDeclaredField("_ui").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val state = field.get(heartRate) as MutableStateFlow<HeartRateState>
        val originalState = state.value
        val wasEnabled = repository.isOverlayEnabled
        val base = 0x80FF0066.toInt()
        val warningRed = 0x80FF0000.toInt()
        fun publish(bpm: Int?) = instrumentation.runOnMainSync {
            val sample = bpm?.let { HeartRateParser.parse(byteArrayOf(0, it.toByte())) }
            state.value = originalState.copy(sample = sample)
        }
        fun view(): HeartRateOverlayView? = WindowInspector.getGlobalWindowViews()
            .filterIsInstance<HeartRateOverlayView>().singleOrNull()
        fun readout(text: String, color: Int) {
            compose.waitUntil(10_000) {
                var matches = false
                instrumentation.runOnMainSync {
                    val number = view()?.getChildAt(1) as? TextView
                    matches = number?.text?.toString() == text && number?.currentTextColor == color
                }
                matches
            }
        }
        fun shell(command: String) {
            instrumentation.uiAutomation.executeShellCommand(command).use {
                java.io.FileInputStream(it.fileDescriptor).use { input -> input.readBytes() }
            }
        }
        try {
            instrumentation.runOnMainSync {
                repository.setOverlayShowHeart(true)
                repository.setOverlayShowUnit(false)
                repository.setFontColor(0xFFFF0066.toInt())
                repository.setFontAlpha(0.5f)
                repository.setHeartRateColorRules(listOf(HeartRateColorRule(120, 0xFF0000)))
                HeartRateOverlayService.show(activity)
            }
            publish(99)
            readout("99", base)
            var classicWidth = 0
            var numberSize = 0f
            instrumentation.runOnMainSync {
                val overlay = checkNotNull(view())
                classicWidth = overlay.width
                numberSize = (overlay.getChildAt(1) as TextView).textSize
            }
            publish(120)
            readout("120", warningRed)
            instrumentation.runOnMainSync {
                assertEquals(classicWidth, checkNotNull(view()).width)
                repository.setOverlayShowHeart(false)
            }
            compose.waitUntil(10_000) {
                var minimal = false
                instrumentation.runOnMainSync { minimal = (view()?.width ?: classicWidth) < classicWidth }
                minimal
            }
            var x = 0
            var y = 0
            instrumentation.runOnMainSync {
                val overlay = checkNotNull(view())
                val position = IntArray(2)
                overlay.getLocationOnScreen(position)
                x = position[0] + overlay.width / 2
                y = position[1] + overlay.height / 2
            }
            val screenWidth = activity.resources.displayMetrics.widthPixels
            shell("input swipe $x $y ${screenWidth - 2} $y 300")
            fun assertVisibleElements(showHeart: Boolean, showUnit: Boolean) {
                compose.waitUntil(10_000) {
                    var matches = false
                    instrumentation.runOnMainSync {
                        val overlay = view()
                        matches = overlay != null &&
                            (overlay.getChildAt(0).visibility == View.VISIBLE) == showHeart &&
                            (overlay.getChildAt(2).visibility == View.VISIBLE) == showUnit &&
                            overlay.width == overlay.windowWidthPx
                        if (matches && overlay != null) {
                            val position = overlay.layoutParams as WindowManager.LayoutParams
                            assertEquals(numberSize, (overlay.getChildAt(1) as TextView).textSize, 0f)
                            assertTrue(position.x + overlay.width <= screenWidth)
                        }
                    }
                    matches
                }
            }
            instrumentation.runOnMainSync { repository.setOverlayShowUnit(true) }
            assertVisibleElements(showHeart = false, showUnit = true)
            instrumentation.runOnMainSync { repository.setOverlayShowHeart(true) }
            assertVisibleElements(showHeart = true, showUnit = true)
            publish(null)
            readout("--", base)
            instrumentation.runOnMainSync {
                val overlay = checkNotNull(view())
                val position = IntArray(2)
                overlay.getLocationOnScreen(position)
                x = position[0] + overlay.width / 2
                y = position[1] + overlay.height / 2
            }
            shell("input swipe $x $y $x $y 800")
            compose.waitUntil(10_000) { !HeartRateOverlayService.isVisible.value }
        } finally {
            instrumentation.runOnMainSync {
                state.value = originalState
                if (wasEnabled) HeartRateOverlayService.show(activity) else HeartRateOverlayService.hide(activity)
            }
        }
    }

    private fun resumedActivity(): MainActivity? = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<MainActivity>().firstOrNull()

    private fun assertThemeBackground(dark: Boolean) {
        val pixels = compose.onNodeWithTag("settings_screen").captureToImage().toPixelMap()
        assertEquals(if (dark) 0xFF0B0F14.toInt() else 0xFFF4F6F9.toInt(), pixels[0, 0].toArgb())
    }

    @Test
    fun languageIndicatorMovesThroughIntermediatePositions() {
        var language by mutableStateOf(AppLanguage.SYSTEM)
        compose.runOnUiThread {
            activity.setContent { BleHrTheme { LanguageSelector(language) { language = it } } }
        }
        val start = compose.onNodeWithTag("language_indicator").fetchSemanticsNode().boundsInRoot.left
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("language_en").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(100)
        val middle = compose.onNodeWithTag("language_indicator").fetchSemanticsNode().boundsInRoot.left
        compose.mainClock.advanceTimeBy(250)
        val end = compose.onNodeWithTag("language_indicator").fetchSemanticsNode().boundsInRoot.left
        assertTrue("Indicator should slide, not jump", middle > start && middle < end)
        compose.onNodeWithTag("language_en").assertIsSelected()
        compose.mainClock.autoAdvance = true
    }
}
