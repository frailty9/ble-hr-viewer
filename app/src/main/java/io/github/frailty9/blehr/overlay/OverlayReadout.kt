package io.github.frailty9.blehr.overlay

import io.github.frailty9.blehr.data.settings.OverlayAppearance
import kotlin.math.roundToInt

internal fun resolveOverlayForeground(appearance: OverlayAppearance): Int {
    val alpha = (appearance.fontAlpha.coerceIn(0f, 1f) * 255f).roundToInt()
    return (alpha shl 24) or (appearance.fontColor and 0x00FFFFFF)
}
