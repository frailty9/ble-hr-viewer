package io.github.frailty9.blehr.data.heartrate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualScanReconnectPolicyTest {
    @Test
    fun matchesRememberedAddressIgnoringCaseAndConsumesItOnce() {
        val policy = ManualScanReconnectPolicy()
        policy.beginScan("AA:BB:CC:DD:EE:FF", connected = false)

        assertEquals("aa:bb:cc:dd:ee:ff", policy.findMatch(listOf("11:22:33:44:55:66", "aa:bb:cc:dd:ee:ff")))
        assertNull(policy.findMatch(listOf("AA:BB:CC:DD:EE:FF")))
    }

    @Test
    fun doesNotMatchByNameWhenAddressDiffers() {
        val policy = ManualScanReconnectPolicy()
        policy.beginScan("AA:BB:CC:DD:EE:FF", connected = false)

        assertNull(policy.findMatch(listOf("11:22:33:44:55:66")))
    }

    @Test
    fun canMatchDeviceOutsideTheDisplayedResultLimit() {
        val policy = ManualScanReconnectPolicy()
        policy.beginScan("AA:BB:CC:DD:EE:FF", connected = false)
        val addresses = (0..30).map { "00:00:00:00:00:%02X".format(it) } + "AA:BB:CC:DD:EE:FF"

        assertEquals("AA:BB:CC:DD:EE:FF", policy.findMatch(addresses))
    }

    @Test
    fun noTargetIsScheduledWhenThereIsNoRecentDeviceOrAlreadyConnected() {
        val policy = ManualScanReconnectPolicy()
        policy.beginScan(recentAddress = null, connected = false)
        assertNull(policy.findMatch(listOf("AA:BB:CC:DD:EE:FF")))

        policy.beginScan(recentAddress = "AA:BB:CC:DD:EE:FF", connected = true)
        assertNull(policy.findMatch(listOf("AA:BB:CC:DD:EE:FF")))
    }

    @Test
    fun explicitDisconnectSuppressesUntilAnExplicitConnection() {
        val policy = ManualScanReconnectPolicy()
        policy.onExplicitDisconnect()
        policy.beginScan("AA:BB:CC:DD:EE:FF", connected = false)
        assertNull(policy.findMatch(listOf("AA:BB:CC:DD:EE:FF")))

        policy.onExplicitConnect()
        policy.beginScan("AA:BB:CC:DD:EE:FF", connected = false)
        assertEquals("AA:BB:CC:DD:EE:FF", policy.findMatch(listOf("AA:BB:CC:DD:EE:FF")))
    }

    @Test
    fun endingScanCancelsItsPendingMatch() {
        val policy = ManualScanReconnectPolicy()
        policy.beginScan("AA:BB:CC:DD:EE:FF", connected = false)
        policy.endScan()

        assertNull(policy.findMatch(listOf("AA:BB:CC:DD:EE:FF")))
    }

    @Test
    fun aNewPolicyStartsWithoutDisconnectSuppression() {
        val newProcessPolicy = ManualScanReconnectPolicy()
        newProcessPolicy.beginScan("AA:BB:CC:DD:EE:FF", connected = false)

        assertEquals("AA:BB:CC:DD:EE:FF", newProcessPolicy.findMatch(listOf("AA:BB:CC:DD:EE:FF")))
    }
}
