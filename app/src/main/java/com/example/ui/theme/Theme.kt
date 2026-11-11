package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ResonanceGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF144525),
    onPrimaryContainer = ResonanceGreen,
    secondary = ResonanceGreenLight,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ResonanceGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder
)

@Composable
fun ResonanceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MusicfyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    ResonanceTheme(darkTheme = darkTheme, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    ResonanceTheme(darkTheme = darkTheme, content = content)
}
