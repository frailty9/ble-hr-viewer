package com.example.blehr.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var controller: NavHostController
    private val settingsLabel get() = compose.activity.getString(R.string.settings_title)
    private val backLabel get() = compose.activity.getString(R.string.back)

    @Composable
    private fun TestPages(useTestController: Boolean = false) {
        BleHrTheme {
            // Constrain height to exercise real screen scrolling on phones of any size.
            Box(Modifier.fillMaxWidth().height(480.dp)) {
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
        compose.setContent { TestPages(useTestController = true) }
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
        compose.setContent { TestPages() }
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
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(before, scrollPosition("home_scroll"), 1f)
    }

    @Test
    fun destinationAndBothScrollPositionsSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TestPages() }
        compose.onNodeWithTag("home_scroll").performTouchInput { swipeUp() }
        val homeBefore = scrollPosition("home_scroll")
        compose.onNodeWithContentDescription(settingsLabel).performClick()
        compose.onNodeWithTag("settings_scroll").performTouchInput { swipeUp() }
        val settingsBefore = scrollPosition("settings_scroll")
        assertTrue(settingsBefore > 0)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
        assertEquals(settingsBefore, scrollPosition("settings_scroll"), 1f)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(homeBefore, scrollPosition("home_scroll"), 1f)
    }
}
