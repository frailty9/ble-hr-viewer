package io.github.frailty9.blehr.feature.privacy

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.frailty9.blehr.data.heartrate.HeartRateRepository
import io.github.frailty9.blehr.data.heartrate.HeartRateState
import io.github.frailty9.blehr.data.heartrate.RecentHeartRateDevice
import io.github.frailty9.blehr.data.heartrate.RecentHeartRateDeviceStore
import io.github.frailty9.blehr.data.privacy.PrivacyConsentStore
import io.github.frailty9.blehr.data.settings.SettingsRepository
import io.github.frailty9.blehr.feature.settings.SettingsViewModel
import java.io.FileInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivacyWithdrawalTest {
    @Test
    fun withdrawingDuringScanClearsTheSessionAndKeepsPreferences() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissions.forEach { permission ->
            instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${application.packageName} $permission",
            ).use { descriptor -> FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
        }
        assumeTrue(application.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true)

        val repository = HeartRateRepository.get(application)
        val recentDevice = RecentHeartRateDeviceStore(application)
        val settings = SettingsRepository.get(application)
        val viewModel = SettingsViewModel(application)
        val originalTimeout = settings.heartRateTimeoutSeconds.value
        val originalDevice = recentDevice.read()
        val originallyAccepted = PrivacyConsentStore.isAccepted(application)
        val originallyEnabled = settings.isOverlayEnabled
        try {
            instrumentation.runOnMainSync {
                PrivacyConsentStore.accept(application)
                recentDevice.save(RecentHeartRateDevice("02:00:00:00:00:01", "Test device", null))
                settings.setOverlayEnabled(true)
                settings.setHeartRateTimeoutSeconds(if (originalTimeout == 11) 12 else 11)
                repository.startScan()
            }
            // Check in the test thread before the scan can time out.
            assertTrue(repository.ui.value.scanning)
            val timeout = settings.heartRateTimeoutSeconds.value
            instrumentation.runOnMainSync { viewModel.withdrawConsent() }
            instrumentation.waitForIdleSync()

            assertFalse(PrivacyConsentStore.isAccepted(application))
            assertNull(recentDevice.read())
            assertEquals(HeartRateState(), repository.ui.value)
            assertFalse(settings.isOverlayEnabled)
            assertEquals(timeout, settings.heartRateTimeoutSeconds.value)
        } finally {
            instrumentation.runOnMainSync {
                viewModel.withdrawConsent()
                settings.setHeartRateTimeoutSeconds(originalTimeout)
                if (originallyAccepted) PrivacyConsentStore.accept(application)
                if (originalDevice != null) recentDevice.save(originalDevice)
                settings.setOverlayEnabled(originallyEnabled)
            }
        }
    }
}
