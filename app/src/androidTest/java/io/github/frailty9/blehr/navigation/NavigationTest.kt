package io.github.frailty9.blehr.navigation

import android.bluetooth.BluetoothManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.RoundedCorner
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.frailty9.blehr.MainActivity
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.ble.HeartRateSample
import io.github.frailty9.blehr.ble.ScannedDevice
import io.github.frailty9.blehr.ble.SensorContact
import io.github.frailty9.blehr.data.privacy.PrivacyConsentStore
import io.github.frailty9.blehr.data.heartrate.ConnectionPhase
import io.github.frailty9.blehr.data.heartrate.HeartRateState
import io.github.frailty9.blehr.data.settings.AppLanguage
import io.github.frailty9.blehr.data.settings.AppTheme
import io.github.frailty9.blehr.data.settings.OverlayAppearance
import io.github.frailty9.blehr.feature.home.HomeScreen
import io.github.frailty9.blehr.feature.home.components.DeviceCard
import io.github.frailty9.blehr.feature.settings.SettingsScreen
import io.github.frailty9.blehr.ui.theme.AppCornerRadii
import io.github.frailty9.blehr.ui.theme.BleHrTheme
import io.github.frailty9.blehr.ui.theme.LocalAppShapes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
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
        PrivacyConsentStore.accept(instrumentation.targetContext)
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT")
        } else {
            listOf("android.permission.ACCESS_FINE_LOCATION")
        }
        for (permission in permissions) {
            instrumentation.uiAutomation.executeShellCommand("pm grant io.github.frailty9.blehr $permission")
                .use { descriptor -> java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
        }
        // Avoid startActivitySync: Android 17 OEM builds can leave its ActivityWaiter waiting.
        instrumentation.uiAutomation.executeShellCommand(
            "am start -W -n io.github.frailty9.blehr/.MainActivity",
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
    private var observedRadii: AppCornerRadii? = null
    private val settingsLabel get() = activity.getString(R.string.settings_title)
    private val backLabel get() = activity.getString(R.string.back)

    @Composable
    private fun TestPages(useTestController: Boolean = false, showManyDevices: Boolean = false) {
        BleHrTheme(theme = AppTheme.LIGHT) {
            val radii = LocalAppShapes.current.radii
            SideEffect { observedRadii = radii }
            val state = remember(showManyDevices) {
                val devices = if (showManyDevices) {
                    val adapter = activity.getSystemService(BluetoothManager::class.java).adapter
                    (10..25).map { index ->
                        val address = "00:11:22:33:44:$index"
                        ScannedDevice(adapter.getRemoteDevice(address), "Preview HR $index", address, -55, true)
                    }
                } else emptyList()
                HeartRateState(devices = devices)
            }
            // Constrain height to exercise real screen scrolling on phones of any size.
            Box(Modifier.widthIn(max = 600.dp).fillMaxWidth().height(600.dp).background(MaterialTheme.colorScheme.background)) {
                if (useTestController) {
                    val context = LocalContext.current
                    val testController = remember {
                        TestNavHostController(context).apply {
                            navigatorProvider.addNavigator(ComposeNavigator())
                        }
                    }
                    controller = testController
                    BleHrNavHost(
                        homeContent = { HomeContent(state, it) },
                        settingsContent = { SettingsContent(it) },
                        navController = testController,
                    )
                } else {
                    BleHrNavHost(
                        homeContent = { HomeContent(state, it) },
                        settingsContent = { SettingsContent(it) },
                    )
                }
            }
        }
    }

    @Composable
    private fun HomeContent(state: HeartRateState, openSettings: () -> Unit) {
        HomeScreen(
            state = state,
            overlayVisible = false,
            onOpenSettings = openSettings,
            onStartScan = {}, onStopScan = {}, onConnect = {}, onDisconnect = {},
            onOpenBluetoothSettings = {}, onToggleOverlay = {},
        )
    }

    @Composable
    private fun SettingsContent(back: () -> Unit) {
        SettingsScreen(
            language = AppLanguage.SYSTEM,
            heartRateState = HeartRateState(),
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
    fun shortWideWindowScrollsColumnsIndependentlyAtLargeFontScale() {
        compose.runOnUiThread { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10_000) {
            activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        assumeTrue("The emulator or phone must support a wide landscape window", activity.resources.configuration.screenWidthDp >= 760)
        val name = "Long heart rate monitor name for adaptive layout verification"
        val address = "00:11:22:33:44:10"
        val adapter = activity.getSystemService(BluetoothManager::class.java).adapter
        val state = HeartRateState(
            connection = ConnectionPhase.CONNECTED,
            connectedName = name,
            connectedAddress = address,
            sample = HeartRateSample(123, SensorContact.DETECTED, null, listOf(500), 0, byteArrayOf(), emptyList(), 0L),
            history = listOf(99, 105, 118, 123),
            devices = (10..25).map { index ->
                val deviceAddress = "00:11:22:33:44:$index"
                ScannedDevice(adapter.getRemoteDevice(deviceAddress), "Preview HR $index", deviceAddress, -55, true)
            },
        )
        compose.runOnUiThread {
            activity.setContent {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, 1.5f)) {
                    BleHrTheme {
                        Box(Modifier.fillMaxWidth().height(400.dp)) { HomeContent(state) {} }
                    }
                }
            }
        }
        val readout = compose.onNodeWithText("123").assertIsDisplayed()
        val toggle = compose.onNodeWithContentDescription(activity.getString(R.string.overlay_show)).assertIsDisplayed()
        val readoutBounds = readout.fetchSemanticsNode().boundsInRoot
        val toggleBounds = toggle.fetchSemanticsNode().boundsInRoot
        assertTrue(!readoutBounds.overlaps(toggleBounds))
        val summary = compose.onNodeWithText("$name · $address")
        val textLayout = mutableListOf<TextLayoutResult>()
        summary.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayout) }
        assertTrue(textLayout.single().lineCount > 1)
        assertTrue(!textLayout.single().hasVisualOverflow)

        val left = "home_left_scroll"
        val right = "home_right_scroll"
        compose.onNodeWithText("Preview HR 25").performScrollTo().assertIsDisplayed()
        val rightPosition = scrollPosition(right)
        assertTrue(rightPosition > 0)
        assertEquals(0f, scrollPosition(left), 1f)
        compose.onNodeWithTag(left).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 160f) }
        assertTrue(scrollPosition(left) > 0)
        assertEquals(rightPosition, scrollPosition(right), 1f)
    }

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
        compose.runOnUiThread { activity.setContent { TestPages(showManyDevices = true) } }
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
        compose.onNodeWithText("Preview HR 25").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun scrollingViewportReachesBottomWithoutAnInsetRoundedFrame() {
        compose.runOnUiThread { activity.setContent { TestPages(showManyDevices = true) } }
        compose.onNodeWithTag("home_scroll").performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, 200f)
        }
        val screen = compose.onNodeWithTag("home_screen")
        val screenBounds = screen.fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("home_scroll").fetchSemanticsNode().boundsInRoot
        val pixels = screen.captureToImage().toPixelMap()
        val left = (viewport.left - screenBounds.left).toInt()
        val right = (viewport.right - screenBounds.left).toInt() - 1
        val top = (viewport.top - screenBounds.top).toInt()
        val middle = (left + right) / 2
        assertEquals(screenBounds.bottom, viewport.bottom, 1f)
        // The middle of the first card is now at the viewport top, not its own rounded edge.
        assertEquals(0xFFFFFFFF.toInt(), pixels[middle, top + 2].toArgb())
        assertEquals(0xFFFFFFFF.toInt(), pixels[left + 2, top + 2].toArgb())
        assertEquals(0xFFFFFFFF.toInt(), pixels[right - 2, top + 2].toArgb())
    }

    @Test
    fun emptyDeviceCardFillsAvailableHeightAndDiagnosticsAreInSettings() {
        compose.runOnUiThread {
            activity.setContent {
                BleHrTheme {
                    BleHrNavHost(
                        homeContent = { HomeContent(HeartRateState(), it) },
                        settingsContent = { SettingsContent(it) },
                    )
                }
            }
        }
        val viewport = compose.onNodeWithTag("home_scroll").fetchSemanticsNode()
        val deviceCard = compose.onNodeWithTag("nearby_devices_card").fetchSemanticsNode()
        val bottomGap = viewport.boundsInRoot.bottom - deviceCard.boundsInRoot.bottom
        val density = activity.resources.displayMetrics.density
        assertTrue(
            "Expected only a bottom margin and navigation inset, got $bottomGap px",
            bottomGap in 12 * density..64 * density,
        )
        assertEquals(0f, viewport.config[SemanticsProperties.VerticalScrollAxisRange].maxValue(), 1f)

        val movedCards = listOf(R.string.packet_title, R.string.error_packets, R.string.help_title)
            .map { compose.onNodeWithText(activity.getString(it)) }
        movedCards.forEach { it.assertDoesNotExist() }
        compose.onNodeWithContentDescription(settingsLabel).performClick()
        val back = compose.onNodeWithContentDescription(backLabel).assertIsDisplayed()
        val initialBackBounds = back.fetchSemanticsNode().boundsInRoot
        movedCards.forEach { it.performScrollTo().assertIsDisplayed() }
        assertEquals(initialBackBounds, back.assertIsDisplayed().fetchSemanticsNode().boundsInRoot)
        back.performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test
    fun rootThemeUsesWindowCornerDataAndRefreshesAfterRotation() {
        compose.runOnUiThread { activity.setContent { TestPages() } }
        fun assertWindowRadius() {
            var expected = 20f
            compose.runOnIdle {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !activity.isInMultiWindowMode) {
                    val insets = activity.window.decorView.rootWindowInsets
                    val radiusPx = listOf(
                        RoundedCorner.POSITION_TOP_LEFT, RoundedCorner.POSITION_TOP_RIGHT,
                        RoundedCorner.POSITION_BOTTOM_RIGHT, RoundedCorner.POSITION_BOTTOM_LEFT,
                    ).mapNotNull { insets?.getRoundedCorner(it)?.radius }.filter { it > 0 }.maxOrNull()
                    if (radiusPx != null) expected = (radiusPx / activity.resources.displayMetrics.density - 14f)
                        .coerceIn(12f, 28f)
                }
            }
            compose.waitUntil(10_000) { observedRadii?.card?.value == expected }
        }
        assertWindowRadius()
        compose.runOnUiThread { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10_000) {
            activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        // The fixture stays under the width threshold even when rotated.
        compose.onNodeWithTag("home_scroll").assertIsDisplayed()
        assertWindowRadius()
    }

    @Test
    fun sampleDeviceRowRemainsClickableWithoutStartingBle() {
        val device = activity.getSystemService(BluetoothManager::class.java).adapter
            .getRemoteDevice("00:11:22:33:44:55")
        val sample = ScannedDevice(device, "Preview HR", "00:11:22:33:44:55", -55, true)
        var clicked = false
        compose.runOnUiThread {
            activity.setContent {
                BleHrTheme(theme = AppTheme.LIGHT) {
                    DeviceCard(
                        state = HeartRateState(devices = listOf(sample)),
                        onStartScan = {}, onStopScan = {}, onConnect = { clicked = it == sample },
                        onDisconnect = {}, openBluetoothSettings = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Preview HR").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun destinationAndSettingsScrollPositionSurviveSavedStateRestoration() {
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
