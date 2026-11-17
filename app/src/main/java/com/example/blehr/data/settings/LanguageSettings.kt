package com.example.blehr.data.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit

/** Framework app locales on Android 13+, a persisted context override on older releases. */
object LanguageSettings {
    private const val PREFERENCES = "app_language"
    private const val KEY_LANGUAGE = "language"

    fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun selected(context: Context): AppLanguage {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
                .substringBefore(',')
        } else {
            preferences(context).getString(KEY_LANGUAGE, "")
        }
        return AppLanguage.fromTag(tag)
    }

    fun apply(activity: Activity, language: AppLanguage) {
        if (language == selected(activity)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(language.tag)
        } else {
            preferences(activity).edit(commit = true) { putString(KEY_LANGUAGE, language.tag) }
            // The root composition observes this preference and updates its resource context in place.
        }
    }

    fun localizedContext(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context
        val language = selected(context)
        if (language == AppLanguage.SYSTEM) return context
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(language.tag))
        }
        return context.createConfigurationContext(configuration)
    }
}
