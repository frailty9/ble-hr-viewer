package io.github.frailty9.blehr.overlay

import io.github.frailty9.blehr.ble.SensorContact
import io.github.frailty9.blehr.data.settings.HeartRateColorRule
import io.github.frailty9.blehr.data.settings.OverlayAppearance
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayReadoutTest {
    private val appearance = OverlayAppearance(
        fontColor = 0xFFFF0066.toInt(),
        fontAlpha = 0.5f,
        colorRules = listOf(HeartRateColorRule(150, 0xAA2222), HeartRateColorRule(120, 0xD29922)),
    )
    private val base = 0x80FF0066.toInt()
    private val amber = 0x80D29922.toInt()
    private val higher = 0x80AA2222.toInt()

    @Test
    fun usesHighestReachedThresholdIncludingEqualityRegardlessOfRuleOrder() {
        val expected = mapOf(119 to base, 120 to amber, 149 to amber, 150 to higher, 151 to higher, 250 to higher)
        for ((bpm, color) in expected) {
            assertEquals("$bpm BPM", color, resolveOverlayForeground(appearance, bpm, SensorContact.DETECTED))
        }
        assertEquals(amber, resolveOverlayForeground(appearance, 120, SensorContact.NOT_SUPPORTED))
    }

    @Test
    fun unavailableOrInvalidReadingAndDisabledRulesUseBaseColor() {
        for (bpm in listOf(null, -1, 0, 251, 999)) {
            assertEquals(base, resolveOverlayForeground(appearance, bpm, SensorContact.DETECTED))
        }
        assertEquals(base, resolveOverlayForeground(appearance, 150, SensorContact.NOT_DETECTED))
        assertEquals(base, resolveOverlayForeground(appearance.copy(colorRules = emptyList()), 150, SensorContact.DETECTED))
    }
}
