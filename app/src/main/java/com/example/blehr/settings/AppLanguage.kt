package com.example.blehr.settings

enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    CHINESE("zh"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage = when (tag?.substringBefore('-')) {
            "zh" -> CHINESE
            "en" -> ENGLISH
            else -> SYSTEM
        }
    }
}
