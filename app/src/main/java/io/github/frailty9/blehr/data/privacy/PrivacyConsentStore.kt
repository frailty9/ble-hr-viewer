package io.github.frailty9.blehr.data.privacy

import android.content.Context
import androidx.core.content.edit
import io.github.frailty9.blehr.data.heartrate.RECENT_DEVICE_PREFERENCES

object PrivacyConsentStore {
    private const val PREFERENCES = "privacy_consent"
    private const val KEY_VERSION = "consent_version"
    const val CONSENT_VERSION = 1

    fun isAccepted(context: Context): Boolean =
        preferences(context).getInt(KEY_VERSION, 0) >= CONSENT_VERSION

    fun accept(context: Context) {
        preferences(context).edit { putInt(KEY_VERSION, CONSENT_VERSION) }
    }

    fun withdraw(context: Context) {
        preferences(context).edit { clear() }
        context.getSharedPreferences(RECENT_DEVICE_PREFERENCES, Context.MODE_PRIVATE).edit { clear() }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
