package io.github.frailty9.blehr.data.privacy

import android.content.Context
import androidx.core.content.edit
import io.github.frailty9.blehr.BuildConfig

object PrivacyConsentStore {
    private const val PREFERENCES = "privacy_consent"
    private const val KEY_VERSION = "consent_version"

    fun isAccepted(context: Context): Boolean =
        preferences(context).getInt(KEY_VERSION, 0) >= BuildConfig.PRIVACY_CONSENT_VERSION

    fun accept(context: Context) {
        preferences(context).edit { putInt(KEY_VERSION, BuildConfig.PRIVACY_CONSENT_VERSION) }
    }

    fun withdraw(context: Context) {
        preferences(context).edit { clear() }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
