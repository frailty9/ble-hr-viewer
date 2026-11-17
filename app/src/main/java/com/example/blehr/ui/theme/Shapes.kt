package com.example.blehr.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.RoundedCorner
import android.view.ViewTreeObserver
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** The same inset is used by the home/settings layouts and the concentric corner calculation. */
internal val PageMargin = 14.dp

@Immutable
internal data class AppCornerRadii(val card: Dp = 20.dp) {
    private val scale: Float get() = card.value / 20f
    val row: Dp get() = (14f * scale).dp
    val button: Dp get() = (12f * scale).dp
    val segment: Dp get() = (8f * scale).dp
    val indicator: Dp get() = (segment.value - 3f).coerceAtLeast(2f).dp
    val chip: Dp get() = (6f * scale).dp
}

@Immutable
internal class AppShapes(val radii: AppCornerRadii = AppCornerRadii()) {
    val card = RoundedCornerShape(radii.card)
    val row = RoundedCornerShape(radii.row)
    val button = RoundedCornerShape(radii.button)
    val segment = RoundedCornerShape(radii.segment)
    val indicator = RoundedCornerShape(radii.indicator)
    val chip = RoundedCornerShape(radii.chip)
}

internal val LocalAppShapes = staticCompositionLocalOf { AppShapes() }

/** Missing OEM data, older Android and multi-window share a predictable 20dp baseline. */
internal fun resolveCornerRadii(
    screenRadiiPx: List<Int>,
    density: Float,
    inMultiWindow: Boolean = false,
): AppCornerRadii {
    val radius = screenRadiiPx.filter { it > 0 }.maxOrNull()
    if (inMultiWindow || radius == null || !density.isFinite() || density <= 0f) {
        return AppCornerRadii()
    }
    return AppCornerRadii((radius / density - PageMargin.value).coerceIn(12f, 28f).dp)
}

/** Observe the root window without replacing an Insets listener or consuming any Insets. */
@Composable
internal fun rememberAppCornerRadii(): AppCornerRadii {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    val density = LocalDensity.current.density
    val configuration = LocalConfiguration.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var radii by remember(view) { mutableStateOf(AppCornerRadii()) }
    DisposableEffect(view, activity, density, configuration, lifecycle) {
        fun update() {
            val screenRadii = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val insets = view.rootWindowInsets
                listOf(
                    RoundedCorner.POSITION_TOP_LEFT,
                    RoundedCorner.POSITION_TOP_RIGHT,
                    RoundedCorner.POSITION_BOTTOM_RIGHT,
                    RoundedCorner.POSITION_BOTTOM_LEFT,
                ).mapNotNull { insets?.getRoundedCorner(it)?.radius }
            } else emptyList()
            radii = resolveCornerRadii(screenRadii, density, activity?.isInMultiWindowMode == true)
        }
        val observer = view.viewTreeObserver
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { update() }
        val lifecycleListener = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) update()
        }
        observer.addOnGlobalLayoutListener(layoutListener)
        lifecycle.addObserver(lifecycleListener)
        update()
        onDispose {
            if (observer.isAlive) observer.removeOnGlobalLayoutListener(layoutListener)
            else view.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            lifecycle.removeObserver(lifecycleListener)
        }
    }
    return radii
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
