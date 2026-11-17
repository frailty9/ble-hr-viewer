package com.example.blehr.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository private constructor(context: Context) {

    companion object {
        private const val PREFS = "overlay_appearance"
        private const val KEY_BG_COLOR = "bg_color"
        private const val KEY_BG_ALPHA = "bg_alpha"
        private const val KEY_FONT_COLOR = "font_color"
        private const val KEY_FONT_ALPHA = "font_alpha"

        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also {
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
