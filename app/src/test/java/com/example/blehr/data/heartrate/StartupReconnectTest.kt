package com.example.blehr.data.heartrate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupReconnectTest {
    @Test
    fun repeatedResumesDoNotStartAnotherAttemptDuringOrAfterAConnection() {
        val reconnect = StartupReconnect()
        reconnect.tryOnce { true }
        reconnect.tryOnce { error("An attempt is already connecting") }

        assertTrue(reconnect.finish())
        reconnect.tryOnce { error("A later resume must not reconnect") }
    }

    @Test
    fun manualActionBeforeStartupPreventsReconnect() {
        val reconnect = StartupReconnect()
        assertFalse(reconnect.finish())
        reconnect.tryOnce { error("Manual action has priority") }
    }

    @Test
    fun missingDeviceOrDisabledBluetoothLeavesNoConnectionToCancel() {
        val reconnect = StartupReconnect()
        reconnect.tryOnce { false }

        assertFalse(reconnect.finish())
        reconnect.tryOnce { error("A skipped attempt is not retried on resume") }
    }

    @Test
    fun synchronousFailureCanFinishTheAttemptBeforeConnectReturns() {
        val reconnect = StartupReconnect()
        reconnect.tryOnce {
            assertTrue(reconnect.finish())
            true
        }

        assertFalse(reconnect.finish())
        reconnect.tryOnce { error("A failed attempt is not retried on resume") }
    }
}
