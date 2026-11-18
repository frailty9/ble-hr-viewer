package io.github.frailty9.blehr.data.heartrate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateRecoveryTest {
    @Test
    fun waitsForFirstReadingThenMeasuresFromTheMostRecentValidReading() {
        val scenario = Scenario()
        scenario.recovery.subscribed()
        scenario.advanceBy(4_000)
        scenario.recovery.receivedReading()
        scenario.advanceBy(4_999)
        assertEquals(emptyList<Int>(), scenario.attempts)
        scenario.advanceBy(1)
        assertEquals(listOf(1), scenario.attempts)
        assertFalse(scenario.recovery.isMonitoring)
    }

    @Test
    fun changingTimeoutUsesElapsedTimeAndDoesNotRestartWindow() {
        val scenario = Scenario()
        scenario.recovery.subscribed()
        scenario.advanceBy(2_000)
        scenario.timeoutMillis = 10_000
        scenario.recovery.checkDeadline()
        scenario.advanceBy(4_000)
        assertTrue(scenario.attempts.isEmpty())
        scenario.timeoutMillis = 5_000
        scenario.recovery.checkDeadline()
        assertEquals(listOf(1), scenario.attempts)
    }

    @Test
    fun threeAttemptsIncludeInitialReconnectAndSubscriptionAloneDoesNotResetBudget() {
        val s = Scenario()
        s.recovery.subscribed()
        s.advanceBy(5_000)
        assertEquals(listOf(1), s.attempts)
        s.recovery.subscribed()
        s.advanceBy(5_999)
        assertEquals(listOf(1), s.attempts)
        s.advanceBy(1)
        assertEquals(listOf(1, 2), s.attempts)
        assertTrue(s.recovery.connectionEnded())
        s.advanceBy(1_000)
        assertEquals(listOf(1, 2, 3), s.attempts)
        s.recovery.subscribed()
        s.advanceBy(5_000)
        s.advanceBy(60_000)
        assertEquals(1, s.failures)
        assertEquals(listOf(1, 2, 3), s.attempts)
        assertFalse(s.recovery.isRecovering)
    }

    @Test
    fun firstReadingOnThirdAttemptStartsASeparateBudgetForFutureTimeouts() {
        val s = Scenario()
        s.recovery.subscribed()
        s.advanceBy(5_000)
        repeat(2) {
            assertTrue(s.recovery.connectionEnded())
            s.advanceBy(1_000)
        }
        s.recovery.subscribed()
        s.recovery.receivedReading()
        assertFalse(s.recovery.isRecovering)
        s.advanceBy(5_000)
        assertEquals(listOf(1, 2, 3, 1), s.attempts)
        assertEquals(0, s.failures)
    }

    @Test
    fun confirmedDisconnectAndManualCancellationRemoveScheduledWork() {
        val s = Scenario()
        s.recovery.subscribed()
        assertFalse(s.recovery.connectionEnded())
        s.advanceBy(5_000)
        assertTrue(s.attempts.isEmpty())

        s.recovery.subscribed()
        s.advanceBy(5_000)
        assertTrue(s.recovery.connectionEnded())
        s.recovery.reset()
        s.advanceBy(20_000)
        assertEquals(listOf(1), s.attempts)
        assertEquals(0, s.failures)
    }

    @Test
    fun manualScanSuppressesRecoveryAndUnavailableBluetoothStopsAnExistingCycle() {
        val s = Scenario()
        s.canRecoverOnTimeout = false
        s.recovery.subscribed()
        s.advanceBy(5_000)
        assertTrue(s.attempts.isEmpty())
        assertFalse(s.recovery.isMonitoring)

        s.canRecoverOnTimeout = true
        s.recovery.subscribed()
        s.advanceBy(5_000)
        assertTrue(s.recovery.connectionEnded())
        s.adapterAvailable = false
        s.advanceBy(60_000)
        assertEquals(listOf(1), s.attempts)
        assertEquals(1, s.failures)
        assertFalse(s.recovery.isRecovering)
    }

    private class Scenario {
        private var now = 0L
        private val scheduled = mutableMapOf<Runnable, Long>()
        var timeoutMillis = 5_000L
        var canRecoverOnTimeout = true
        var adapterAvailable = true
        var failures = 0
        val attempts = mutableListOf<Int>()
        val recovery = HeartRateRecovery(
            nowMillis = { now },
            schedule = { task, delay ->
                check(scheduled.isEmpty()) { "Expected a single scheduled action" }
                scheduled[task] = now + delay
            },
            cancel = { scheduled.remove(it) },
            timeoutMillis = { timeoutMillis },
            onDataTimeout = { canRecoverOnTimeout },
            reconnect = { adapterAvailable && attempts.add(it) },
            onRecoveryFailed = { failures++ },
        )

        fun advanceBy(duration: Long) {
            val end = now + duration
            while (true) {
                val (task, scheduledAt) = scheduled.entries.singleOrNull() ?: break
                if (scheduledAt > end) break
                now = scheduledAt
                scheduled.remove(task)
                task.run()
            }
            now = end
        }
    }
}
