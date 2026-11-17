package com.example.blehr.feature.settings

import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.blehr.MainActivity
import com.example.blehr.R
import com.example.blehr.data.settings.AppLanguage
import com.example.blehr.data.settings.LanguageSettings
import com.example.blehr.data.settings.OverlayAppearance
import com.example.blehr.data.settings.SettingsRepository
import com.example.blehr.feature.settings.components.LanguageSelector
import com.example.blehr.ui.theme.BleHrTheme
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Before
    fun launchActualActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        repository = SettingsRepository.get(context)
        originalAppearance = repository.appearance.value
        originalLanguage = LanguageSettings.selected(context)
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT")
        } else listOf("android.permission.ACCESS_FINE_LOCATION")
        for (permission in permissions) {
            instrumentation.uiAutomation.executeShellCommand("pm grant com.example.blehr $permission")
                .use { java.io.FileInputStream(it.fileDescriptor).use { input -> input.readBytes() } }
        }
        instrumentation.uiAutomation.executeShellCommand("am start -W -n com.example.blehr/.MainActivity")
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
            repository.setBackgroundColor(originalAppearance.backgroundColor)
            repository.setBackgroundAlpha(originalAppearance.backgroundAlpha)
            repository.setFontColor(originalAppearance.fontColor)
            repository.setFontAlpha(originalAppearance.fontAlpha)
            activity.finish()
        }
    }

    private fun openSettings() {
        compose.onNodeWithContentDescription(activity.getString(R.string.settings_title)).performClick()
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
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
        compose.onNodeWithText(activity.getString(R.string.reset_defaults)).performClick()
        compose.runOnIdle {
            val defaults = repository.appearance.value
            assertEquals(0xFF141A22.toInt(), defaults.backgroundColor)
            assertEquals(0.30f, defaults.backgroundAlpha)
            assertEquals(0xFFFF4D6D.toInt(), defaults.fontColor)
            assertEquals(0.65f, defaults.fontAlpha)
        }
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
