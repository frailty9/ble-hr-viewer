package io.github.frailty9.blehr.data.settings

enum class AppTheme(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromKey(key: String?): AppTheme = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
