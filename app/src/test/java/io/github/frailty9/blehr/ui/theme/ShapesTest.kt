package io.github.frailty9.blehr.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ShapesTest {
    @Test
    fun largestValidScreenCornerUsesDensityAndPageInset() {
        val radii = resolveCornerRadii(listOf(0, 100, 128, 120), 3.5f)
        assertEquals(128f / 3.5f - 14f, radii.card.value, 0.001f)
        assertEquals(14f * radii.card.value / 20f, radii.row.value, 0.001f)
        assertEquals(12f * radii.card.value / 20f, radii.button.value, 0.001f)
        assertEquals(8f * radii.card.value / 20f, radii.segment.value, 0.001f)
        assertEquals(radii.segment.value - 3f, radii.indicator.value, 0.001f)
        assertEquals(6f * radii.card.value / 20f, radii.chip.value, 0.001f)
    }

    @Test
    fun cardAndIndicatorHaveLowerBoundsAndCardHasUpperBound() {
        val small = resolveCornerRadii(listOf(1), 4f)
        assertEquals(12f, small.card.value)
        assertEquals(2f, small.indicator.value)
        assertEquals(28f, resolveCornerRadii(listOf(500), 1f).card.value)
    }

    @Test
    fun unavailableCornersInvalidDensityAndMultiWindowUseDefaults() {
        val fallback = AppCornerRadii()
        assertEquals(fallback, resolveCornerRadii(emptyList(), 3f))
        assertEquals(fallback, resolveCornerRadii(listOf(0, -1), 3f))
        for (density in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(fallback, resolveCornerRadii(listOf(128), density))
        }
        assertEquals(fallback, resolveCornerRadii(listOf(128), 3.5f, inMultiWindow = true))
        assertEquals(20f, fallback.card.value)
        assertEquals(14f, fallback.row.value)
        assertEquals(12f, fallback.button.value)
        assertEquals(8f, fallback.segment.value)
        assertEquals(5f, fallback.indicator.value)
        assertEquals(6f, fallback.chip.value)
    }
}
