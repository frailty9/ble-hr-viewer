package io.github.frailty9.blehr.data.settings

/** Default colors and presets. Change these if you want different swatches. */
object OverlayColors {
    val DefaultBackground = 0xFF141A22.toInt()
    val DefaultFont = 0xFFFF4D6D.toInt()

    val BackgroundPresets = listOf(
        0xFF141A22.toInt(),
        0xFF000000.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF0D47A1.toInt(),
        0xFF1B5E20.toInt(),
        0xFF4A148C.toInt(),
        0xFF37474F.toInt(),
        0xFFB71C1C.toInt(),
    )

    val FontPresets = listOf(
        0xFFFFFFFF.toInt(),
        0xFFFF4D6D.toInt(),
        0xFFD29922.toInt(),
        0xFF3FB950.toInt(),
        0xFF58A6FF.toInt(),
        0xFF000000.toInt(),
    )
}

data class OverlayAppearance(
    val backgroundColor: Int = OverlayColors.DefaultBackground,
    val backgroundAlpha: Float = 0.3f,
    val fontColor: Int = OverlayColors.DefaultFont,
    val fontAlpha: Float = 0.65f,
) {
    companion object {
        val Default = OverlayAppearance()
    }
}
