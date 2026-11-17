package com.example.blehr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.blehr.data.settings.AppTheme

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF4D6D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF442B3B),
    onPrimaryContainer = Color(0xFFFF4D6D),
    secondary = Color(0xFF3FB950),
    onSecondary = Color(0xFF03210C),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFF2F5F9),
    surface = Color(0xFF141A22),
    onSurface = Color(0xFFF2F5F9),
    surfaceVariant = Color(0xFF1B2430),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF596574),
    outlineVariant = Color(0xFF273140),
    error = Color(0xFFF85149),
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFC92C50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF9E1E7),
    onPrimaryContainer = Color(0xFF8F1537),
    secondary = Color(0xFF24763A),
    onSecondary = Color.White,
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF18212D),
    surface = Color.White,
    onSurface = Color(0xFF18212D),
    surfaceVariant = Color(0xFFE7ECF2),
    onSurfaceVariant = Color(0xFF526172),
    outline = Color(0xFF6E7B89),
    outlineVariant = Color(0xFFD5DDE7),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val LocalDarkTheme = staticCompositionLocalOf { true }

// Semantic colors remain shared across cards, charts and settings, and resolve in composition.
val Accent: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val Ink: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
val Muted: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val SurfaceRaised: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
val GridLine: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
val OkGreen: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
val ErrorRed: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
val WarnAmber: Color @Composable @ReadOnlyComposable get() =
    if (LocalDarkTheme.current) Color(0xFFD29922) else Color(0xFF855800)

@Composable
fun BleHrTheme(theme: AppTheme = AppTheme.SYSTEM, content: @Composable () -> Unit) {
    val dark = theme.isDark(isSystemInDarkTheme())
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = Typography(),
            content = content,
        )
    }
}
