package io.github.frailty9.blehr.data.heartrate

import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.ble.HeartRateParser
import io.github.frailty9.blehr.core.text.uiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateStateTest {
    @Test
    fun expiringLiveReadingsPreservesScanAndPacketDiagnostics() {
        val sample = HeartRateParser.parse(byteArrayOf(0, 72), receivedAtMillis = 1)
        val error = ErrorEntry(uiText(R.string.diagnostic_empty), "", 2)
        val expired = HeartRateState(
            scanning = true,
            sample = sample,
            history = listOf(70, 72),
            lastRawHex = sample.rawHex,
            errors = listOf(error),
            parseErrorCount = 1,
        ).withoutLiveReadings()

        assertNull(expired.sample)
        assertTrue(expired.history.isEmpty())
        assertTrue(expired.scanning)
        assertEquals(sample.rawHex, expired.lastRawHex)
        assertEquals(listOf(error), expired.errors)
        assertEquals(1, expired.parseErrorCount)
    }
}
