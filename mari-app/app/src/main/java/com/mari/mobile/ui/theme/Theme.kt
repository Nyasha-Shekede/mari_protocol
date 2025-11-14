package com.Mari.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFF6B337),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF3E0),
    secondary = Color(0xFFE79B1A),
    background = Color(0xFFFFFBFE),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF3F4F6),
    error = Color(0xFFB00020)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF6B337),
    onPrimary = Color(0xFF1C1B1F),
    primaryContainer = Color(0xFF3F2A00),
    secondary = Color(0xFFE79B1A),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF2C2C2C),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF3C3C3C),
    error = Color(0xFFCF6679)
)

@Composable
fun MariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
