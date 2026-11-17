package com.example.blehr.navigation

import com.example.blehr.MainActivity
import androidx.activity.compose.setContent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Before
import org.junit.After
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.blehr.R
import com.example.blehr.data.heartrate.HeartRateState
import com.example.blehr.data.settings.AppLanguage
import com.example.blehr.data.settings.OverlayAppearance
import com.example.blehr.feature.home.HomeScreen
import com.example.blehr.feature.settings.SettingsScreen
import com.example.blehr.ui.theme.BleHrTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val compose = createEmptyComposeRule()
    private lateinit var activity: MainActivity

    @Before
    fun launchActualActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT")
        } else {
            listOf("android.permission.ACCESS_FINE_LOCATION")
        }
        for (permission in permissions) {
            instrumentation.uiAutomation.executeShellCommand("pm grant com.example.blehr $permission")
                .use { descriptor -> java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
        }
        // Avoid startActivitySync: Android 17 OEM builds can leave its ActivityWaiter waiting.
        instrumentation.uiAutomation.executeShellCommand(
            "am start -W -n com.example.blehr/.MainActivity",
        ).use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
        compose.waitUntil(10_000) {
            instrumentation.runOnMainSync {
                val resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED).filterIsInstance<MainActivity>().firstOrNull()
                if (resumed != null) activity = resumed
            }
            ::activity.isInitialized
        }
    }

    @After
    fun finishActivity() {
        if (::activity.isInitialized) compose.runOnUiThread {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity.finish()
        }
    }

    private lateinit var controller: NavHostController
    private val settingsLabel get() = activity.getString(R.string.settings_title)
    private val backLabel get() = activity.getString(R.string.back)

    @Composable
    private fun TestPages(useTestController: Boolean = false) {
        BleHrTheme {
            // Constrain height to exercise real screen scrolling on phones of any size.
            Box(Modifier.fillMaxWidth().height(600.dp)) {
                if (useTestController) {
                    val context = LocalContext.current
                    val testController = remember {
                        TestNavHostController(context).apply {
                            navigatorProvider.addNavigator(ComposeNavigator())
                        }
                    }
                    controller = testController
                    BleHrNavHost(
                        homeContent = { HomeContent(it) },
                        settingsContent = { SettingsContent(it) },
                        navController = testController,
                    )
                } else {
                    BleHrNavHost(
                        homeContent = { HomeContent(it) },
                        settingsContent = { SettingsContent(it) },
                    )
                }
            }
        }
    }

    @Composable
    private fun HomeContent(openSettings: () -> Unit) {
        HomeScreen(
            state = HeartRateState(),
            overlayGranted = true,
            onOpenSettings = openSettings,
            onStartScan = {}, onStopScan = {}, onConnect = {}, onDisconnect = {},
            onOpenBluetoothSettings = {}, onShowOverlay = {},
        )
    }

    @Composable
    private fun SettingsContent(back: () -> Unit) {
        SettingsScreen(
            language = AppLanguage.SYSTEM,
            onLanguageSelected = {},
            onBack = back,
            appearance = OverlayAppearance.Default,
            onBackgroundColorChanged = {}, onBackgroundAlphaChanged = {},
            onFontColorChanged = {}, onFontAlphaChanged = {}, onResetAppearance = {},
        )
    }

    private fun scrollPosition(tag: String): Float = compose.onNodeWithTag(tag)
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    @Test
    fun repeatedSettingsNavigationCreatesOnlyOneBackStackEntry() {
        compose.runOnUiThread { activity.setContent { TestPages(useTestController = true) } }
        compose.runOnIdle {
            controller.openSettings()
            controller.openSettings()
        }
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(controller.currentDestination!!.hasRoute<SettingsRoute>())
            assertTrue(controller.previousBackStackEntry!!.destination.hasRoute<HomeRoute>())
        }
        compose.onNodeWithContentDescription(backLabel).performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        compose.runOnIdle { assertNull(controller.previousBackStackEntry) }
    }

    @Test
    fun toolbarAndSystemBackReturnToHomeAndKeepItsScrollPosition() {
        compose.runOnUiThread { activity.setContent { TestPages() } }
        compose.onNodeWithTag("home_scroll").performTouchInput { swipeUp() }
        val before = scrollPosition("home_scroll")
        assertTrue(before > 0)
        compose.onNodeWithContentDescription(settingsLabel).performClick()
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
        compose.onNodeWithContentDescription(backLabel).performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(before, scrollPosition("home_scroll"), 1f)
        compose.onNodeWithContentDescription(settingsLabel).performClick()
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
        compose.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(before, scrollPosition("home_scroll"), 1f)
    }

    @Test
    fun destinationAndBothScrollPositionsSurviveSavedStateRestoration() {
        // Exercise actual app navigation across an Activity recreation (as with app language changes).
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        compose.onNodeWithTag("home_scroll").performTouchInput { swipeUp() }
        val homeBefore = scrollPosition("home_scroll")
        compose.onNodeWithContentDescription(settingsLabel).performClick()
        // Landscape makes the real settings page scrollable even on tall phones.
        compose.runOnUiThread { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10_000) {
            activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        // Use a mid-page position: transient system-bar insets can change the maximum at the bottom.
        compose.onNodeWithTag("settings_scroll").performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, 200f)
        }
        val settingsBefore = scrollPosition("settings_scroll")
        assertTrue(homeBefore > 0)
        assertTrue(settingsBefore > 0)
        val originalActivity = activity
        compose.runOnUiThread { originalActivity.recreate() }
        compose.waitUntil(10_000) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>().firstOrNull { it !== originalActivity }
                    ?.let { activity = it }
            }
            activity !== originalActivity
        }
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
        assertEquals(settingsBefore, scrollPosition("settings_scroll"), 1f)
        compose.runOnUiThread { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        compose.waitUntil(10_000) {
            activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        }
        compose.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(homeBefore, scrollPosition("home_scroll"), 1f)
    }
}
