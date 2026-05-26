package com.fenlight.companion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand: #3C4B5E (dark blue-grey)
private val Brand = Color(0xFF3C4B5E)
private val BrandLight = Color(0xFF5C7591)
private val BrandContainer = Color(0xFFD0DCE9)
private val BrandDark = Color(0xFF2A3545)

private val DarkColorScheme = darkColorScheme(
    primary = BrandLight,
    onPrimary = Color(0xFF0D1B28),
    primaryContainer = BrandDark,
    onPrimaryContainer = Color(0xFFD0DCE9),
    secondary = Color(0xFFAABBCC),
    onSecondary = Color(0xFF1A2A38),
    secondaryContainer = Color(0xFF2A3A4A),
    onSecondaryContainer = Color(0xFFCCDDE8),
    background = Color(0xFF111820),
    onBackground = Color(0xFFDCE4EC),
    surface = Color(0xFF111820),
    onSurface = Color(0xFFDCE4EC),
    surfaceVariant = Color(0xFF1E2C3A),
    onSurfaceVariant = Color(0xFFAABBCC),
    outline = Color(0xFF4A5E70),
)

private val LightColorScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BrandContainer,
    onPrimaryContainer = Color(0xFF0D1B28),
    secondary = Color(0xFF4A6070),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E4F0),
    onSecondaryContainer = Color(0xFF0D1E28),
    background = Color(0xFFF4F7FA),
    onBackground = Color(0xFF141C24),
    surface = Color(0xFFF4F7FA),
    onSurface = Color(0xFF141C24),
    surfaceVariant = Color(0xFFDAE4EC),
    onSurfaceVariant = Color(0xFF3D4E5C),
    outline = Color(0xFF6A7E8E),
)

@Composable
fun FenLightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography(),
        content = content,
    )
}
