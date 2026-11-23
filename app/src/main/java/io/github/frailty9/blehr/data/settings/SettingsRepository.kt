package io.github.frailty9.blehr.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository private constructor(context: Context) {

    companion object {
        private const val PREFS = "overlay_appearance"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_BG_COLOR = "bg_color"
        private const val KEY_BG_ALPHA = "bg_alpha"
        private const val KEY_FONT_COLOR = "font_color"
        private const val KEY_FONT_ALPHA = "font_alpha"
        private const val KEY_LEGACY_OVERLAY_LAYOUT = "overlay_layout"
        private const val KEY_OVERLAY_SHOW_HEART = "overlay_show_heart"
        private const val KEY_OVERLAY_SHOW_UNIT = "overlay_show_unit"
        private const val KEY_COLOR_RULES = "heart_rate_color_rules"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_HEART_RATE_TIMEOUT = "heart_rate_timeout_seconds"

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
    private val _theme = MutableStateFlow(AppTheme.fromKey(prefs.getString(KEY_THEME, null)))
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _heartRateTimeoutSeconds = MutableStateFlow(
        prefs.getInt(KEY_HEART_RATE_TIMEOUT, DEFAULT_HEART_RATE_TIMEOUT_SECONDS)
            .coerceIn(MIN_HEART_RATE_TIMEOUT_SECONDS, MAX_HEART_RATE_TIMEOUT_SECONDS),
    )
    val heartRateTimeoutSeconds: StateFlow<Int> = _heartRateTimeoutSeconds.asStateFlow()

    fun setHeartRateTimeoutSeconds(seconds: Int) {
        val value = seconds.coerceIn(MIN_HEART_RATE_TIMEOUT_SECONDS, MAX_HEART_RATE_TIMEOUT_SECONDS)
        if (value == _heartRateTimeoutSeconds.value) return
        prefs.edit { putInt(KEY_HEART_RATE_TIMEOUT, value) }
        _heartRateTimeoutSeconds.value = value
    }

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
        prefs.edit { putString(KEY_THEME, theme.key) }
    }

    val isOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_ENABLED, false)

    fun setOverlayEnabled(enabled: Boolean) {
        if (isOverlayEnabled != enabled) prefs.edit { putBoolean(KEY_OVERLAY_ENABLED, enabled) }
    }

    fun setBackgroundColor(color: Int) =
        commit(_appearance.value.copy(backgroundColor = color))

    fun setBackgroundAlpha(alpha: Float) =
        commit(_appearance.value.copy(backgroundAlpha = alpha.coerceIn(0f, 1f)))

    fun setFontColor(color: Int) =
        commit(_appearance.value.copy(fontColor = color))

    fun setFontAlpha(alpha: Float) =
        commit(_appearance.value.copy(fontAlpha = alpha.coerceIn(0f, 1f)))

    fun setOverlayShowHeart(show: Boolean) = commit(_appearance.value.copy(showHeart = show))

    fun setOverlayShowUnit(show: Boolean) = commit(_appearance.value.copy(showUnit = show))

    fun setHeartRateColorRules(rules: List<HeartRateColorRule>) {
        val snapshot = rules.toList()
        HeartRateColorRules.validate(snapshot)
        commit(_appearance.value.copy(colorRules = snapshot))
    }

    fun reset() = commit(OverlayAppearance.Default)

    private fun commit(value: OverlayAppearance) {
        _appearance.value = value
        prefs.edit {
            putInt(KEY_BG_COLOR, value.backgroundColor)
            putFloat(KEY_BG_ALPHA, value.backgroundAlpha)
            putInt(KEY_FONT_COLOR, value.fontColor)
            putFloat(KEY_FONT_ALPHA, value.fontAlpha)
            putBoolean(KEY_OVERLAY_SHOW_HEART, value.showHeart)
            putBoolean(KEY_OVERLAY_SHOW_UNIT, value.showUnit)
            remove(KEY_LEGACY_OVERLAY_LAYOUT)
            putString(KEY_COLOR_RULES, HeartRateColorRules.encode(value.colorRules))
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
        // Preserve the visible elements chosen in the earlier snapshot until the next save.
        showHeart = prefs.getBoolean(
            KEY_OVERLAY_SHOW_HEART,
            prefs.getString(KEY_LEGACY_OVERLAY_LAYOUT, null) !in listOf("minimal", "large"),
        ),
        showUnit = prefs.getBoolean(
            KEY_OVERLAY_SHOW_UNIT,
            prefs.getString(KEY_LEGACY_OVERLAY_LAYOUT, null) == "large",
        ),
        // Keep thresholds from earlier builds and apply the fixed warning color.
        colorRules = HeartRateColorRules.decode(prefs.all[KEY_COLOR_RULES] as? String)
            .map { it.copy(foregroundRgb = OverlayWarningRedRgb) },
    )
}
