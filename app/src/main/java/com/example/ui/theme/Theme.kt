package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = CyberCyan,
    secondary = CyberGreen,
    onSecondary = Color(0xFF003915),
    secondaryContainer = Color(0xFF005320),
    onSecondaryContainer = CyberGreenGlow,
    tertiary = CyberPurple,
    onTertiary = Color(0xFF2C0050),
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkSurfaceContainer,
    outline = DarkBorder,
    error = CyberRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Cyber suite defaults to high-contrast dark theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
