package io.github.frailty9.blehr.navigation

import io.github.frailty9.blehr.ui.legal.LegalDocument
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute

@Serializable
internal data object PrivacyConsentRoute

@Serializable
internal data class LegalDocumentRoute(val document: LegalDocument)
