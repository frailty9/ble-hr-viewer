package com.example.blehr.overlay

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Default colors and presets. Change these if you want different swatches. */
object OverlayColors {
    val DefaultBackground = 0xFF141A22.toInt()
    val DefaultFont = 0xFFFFFFFF.toInt()

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
    val backgroundAlpha: Float = 0.9f,
    val fontColor: Int = OverlayColors.DefaultFont,
    val fontAlpha: Float = 1f,
) {
    companion object {
        val Default = OverlayAppearance()
    }
}

class OverlaySettingsStore private constructor(context: Context) {

    companion object {
        private const val PREFS = "overlay_appearance"
        private const val KEY_BG_COLOR = "bg_color"
        private const val KEY_BG_ALPHA = "bg_alpha"
        private const val KEY_FONT_COLOR = "font_color"
        private const val KEY_FONT_ALPHA = "font_alpha"

        @Volatile
        private var instance: OverlaySettingsStore? = null

        fun get(context: Context): OverlaySettingsStore {
            return instance ?: synchronized(this) {
                instance ?: OverlaySettingsStore(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _appearance = MutableStateFlow(load())
    val appearance: StateFlow<OverlayAppearance> = _appearance.asStateFlow()

    fun setBackgroundColor(color: Int) =
        commit(_appearance.value.copy(backgroundColor = color))

    fun setBackgroundAlpha(alpha: Float) =
        commit(_appearance.value.copy(backgroundAlpha = alpha.coerceIn(0f, 1f)))

    fun setFontColor(color: Int) =
        commit(_appearance.value.copy(fontColor = color))

    fun setFontAlpha(alpha: Float) =
        commit(_appearance.value.copy(fontAlpha = alpha.coerceIn(0f, 1f)))

    fun reset() = commit(OverlayAppearance.Default)

    private fun commit(value: OverlayAppearance) {
        _appearance.value = value
        prefs.edit {
            putInt(KEY_BG_COLOR, value.backgroundColor)
            putFloat(KEY_BG_ALPHA, value.backgroundAlpha)
            putInt(KEY_FONT_COLOR, value.fontColor)
            putFloat(KEY_FONT_ALPHA, value.fontAlpha)
        }
    }

    private fun load(): OverlayAppearance = OverlayAppearance(
        backgroundColor = prefs.getInt(
            KEY_BG_COLOR,
            OverlayAppearance.Default.backgroundColor,
        ),
        backgroundAlpha = prefs.getFloat(
            KEY_BG_ALPHA,
            OverlayAppearance.Default.backgroundAlpha,
        ),
        fontColor = prefs.getInt(
            KEY_FONT_COLOR,
            OverlayAppearance.Default.fontColor,
        ),
        fontAlpha = prefs.getFloat(
            KEY_FONT_ALPHA,
            OverlayAppearance.Default.fontAlpha,
        ),
    )
}
