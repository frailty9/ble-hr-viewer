package io.github.frailty9.blehr.overlay

import io.github.frailty9.blehr.ble.SensorContact
import io.github.frailty9.blehr.data.settings.OverlayAppearance
import io.github.frailty9.blehr.data.settings.OverlayThresholdRange
import kotlin.math.roundToInt

internal fun resolveOverlayForeground(appearance: OverlayAppearance, bpm: Int?, contact: SensorContact?): Int {
    val rule = if (bpm != null && bpm in OverlayThresholdRange && contact != SensorContact.NOT_DETECTED) {
        appearance.colorRules.filter { it.minBpm <= bpm }.maxByOrNull { it.minBpm }
    } else {
        null
    }
    val rgb = rule?.foregroundRgb ?: appearance.fontColor
    val alpha = (appearance.fontAlpha.coerceIn(0f, 1f) * 255f).roundToInt()
    return (alpha shl 24) or (rgb and 0x00FFFFFF)
}
