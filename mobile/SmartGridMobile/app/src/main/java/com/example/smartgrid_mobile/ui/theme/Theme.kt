/* ============================================================================
 * File        : Theme.kt
 * Purpose     : Material 3 theme for the SmartGrid mobile client. Dynamic
 *               colour is deliberately disabled so the app keeps a consistent
 *               identity across devices and in screenshots for the report.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GridGreen,
    onPrimary = Color.White,
    primaryContainer = GridGreenContainer,
    onPrimaryContainer = GridOnGreenContainer,
    secondary = GridMuted,
    onSecondary = Color.White,
    secondaryContainer = GridSurfaceVariant,
    onSecondaryContainer = GridInk,
    tertiary = GridAmber,
    onTertiary = Color.White,
    tertiaryContainer = GridAmberContainer,
    onTertiaryContainer = Color(0xFF4A2B00),
    background = GridBackground,
    onBackground = GridInk,
    surface = GridSurface,
    onSurface = GridInk,
    surfaceVariant = GridSurfaceVariant,
    onSurfaceVariant = GridMuted,
    outline = GridOutline,
    outlineVariant = GridOutline,
    error = GridError,
    onError = Color.White,
    errorContainer = GridErrorContainer,
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColors = darkColorScheme(
    primary = GridGreenLight,
    onPrimary = Color(0xFF00391F),
    primaryContainer = GridGreenContainerDark,
    onPrimaryContainer = GridGreenContainer,
    secondary = GridMutedDark,
    onSecondary = Color(0xFF1B211F),
    secondaryContainer = GridSurfaceVariantDark,
    onSecondaryContainer = GridInkDark,
    tertiary = Color(0xFFE8B972),
    onTertiary = Color(0xFF3F2A00),
    tertiaryContainer = Color(0xFF4F3400),
    onTertiaryContainer = GridAmberContainer,
    background = GridBackgroundDark,
    onBackground = GridInkDark,
    surface = GridSurfaceDark,
    onSurface = GridInkDark,
    surfaceVariant = GridSurfaceVariantDark,
    onSurfaceVariant = GridMutedDark,
    outline = GridOutlineDark,
    outlineVariant = GridOutlineDark,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun SmartGridMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
