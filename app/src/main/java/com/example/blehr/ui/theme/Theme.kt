package com.example.blehr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFFF4D6D)
val OkGreen = Color(0xFF3FB950)
val WarnAmber = Color(0xFFD29922)
val ErrorRed = Color(0xFFF85149)
val Ink = Color(0xFFF2F5F9)
val Muted = Color(0xFF8B949E)
val Background = Color(0xFF0B0F14)
val SurfaceDark = Color(0xFF141A22)
val SurfaceRaised = Color(0xFF1B2430)
val GridLine = Color(0xFF273140)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = OkGreen,
    onSecondary = Color(0xFF03210C),
    background = Background,
    onBackground = Ink,
    surface = SurfaceDark,
    onSurface = Ink,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = Muted,
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun BleHrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content,
    )
}
