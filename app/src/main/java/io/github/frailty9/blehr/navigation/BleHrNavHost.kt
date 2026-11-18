package io.github.frailty9.blehr.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.frailty9.blehr.feature.privacy.PrivacyConsentScreen
import io.github.frailty9.blehr.ui.legal.LegalDocument
import io.github.frailty9.blehr.ui.legal.LegalDocumentScreen

private const val TRANSITION_DURATION_MS = 250

/** Screen code sees navigation callbacks; only this host owns the back stack and transitions. */
@Composable
fun BleHrNavHost(
    homeContent: @Composable (onOpenSettings: () -> Unit) -> Unit,
    settingsContent: @Composable (onBack: () -> Unit, onOpenDocument: (LegalDocument) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    hasPrivacyConsent: Boolean = true,
    onAcceptPrivacyConsent: () -> Unit = {},
    onDeclinePrivacyConsent: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = if (hasPrivacyConsent) HomeRoute else PrivacyConsentRoute,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(SlideDirection.Left, tween(TRANSITION_DURATION_MS)) +
                fadeIn(tween(TRANSITION_DURATION_MS))
        },
        exitTransition = {
            slideOutOfContainer(SlideDirection.Left, tween(TRANSITION_DURATION_MS)) { it / 4 } +
                fadeOut(tween(TRANSITION_DURATION_MS))
        },
        popEnterTransition = {
            slideIntoContainer(SlideDirection.Right, tween(TRANSITION_DURATION_MS)) { it / 4 } +
                fadeIn(tween(TRANSITION_DURATION_MS))
        },
        popExitTransition = {
            slideOutOfContainer(SlideDirection.Right, tween(TRANSITION_DURATION_MS)) +
                fadeOut(tween(TRANSITION_DURATION_MS))
        },
    ) {
        if (hasPrivacyConsent) {
            composable<HomeRoute> {
                homeContent { navController.openSettings() }
            }
            composable<SettingsRoute> {
                settingsContent({ navController.popBackStack() }, navController::openLegalDocument)
            }
        } else {
            composable<PrivacyConsentRoute> {
                PrivacyConsentScreen(
                    onAgree = onAcceptPrivacyConsent,
                    onDecline = onDeclinePrivacyConsent,
                    onOpenDocument = navController::openLegalDocument,
                )
            }
        }
        composable<LegalDocumentRoute> { entry ->
            LegalDocumentScreen(entry.toRoute<LegalDocumentRoute>().document) {
                navController.popBackStack()
            }
        }
    }
}

internal fun NavController.openSettings() {
    navigate(SettingsRoute) { launchSingleTop = true }
}

private fun NavController.openLegalDocument(document: LegalDocument) {
    navigate(LegalDocumentRoute(document)) { launchSingleTop = true }
}
