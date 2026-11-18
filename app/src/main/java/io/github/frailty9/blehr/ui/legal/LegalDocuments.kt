package io.github.frailty9.blehr.ui.legal

import android.content.Context
import androidx.annotation.Keep
import androidx.annotation.StringRes
import io.github.frailty9.blehr.R
import io.github.frailty9.blehr.data.settings.AppLanguage
import io.github.frailty9.blehr.data.settings.LanguageSettings
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class LegalDocument(@StringRes val titleRes: Int) {
    PRIVACY(R.string.privacy_policy_title),
    AGREEMENT(R.string.user_agreement_title),
    LICENSES(R.string.open_source_licenses_title),
}

object LegalDocuments {
    const val CONTACT_EMAIL = "ydl9263@qq.com"

    private val LICENSE_ASSETS = listOf(
        "legal/notices.txt",
        "legal/Apache-2.0.txt",
        "legal/Lucide.txt",
        "legal/MIT.txt",
    )

    fun read(context: Context, document: LegalDocument): String? = runCatching {
        when (document) {
            LegalDocument.PRIVACY -> readAsset(context, "legal/privacy_${language(context)}.txt")
            LegalDocument.AGREEMENT -> readAsset(context, "legal/agreement_${language(context)}.txt")
            LegalDocument.LICENSES -> LICENSE_ASSETS.joinToString("\n\n") { readAsset(context, it) }
        }
    }.getOrNull()

    private fun language(context: Context): String {
        val systemLanguage = context.resources.configuration.locales[0].language
        return when (LanguageSettings.selected(context)) {
            AppLanguage.CHINESE -> "zh"
            AppLanguage.ENGLISH -> "en"
            AppLanguage.SYSTEM -> if (systemLanguage == "zh") "zh" else "en"
        }
    }

    private fun readAsset(context: Context, name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }
}
