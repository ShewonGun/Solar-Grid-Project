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
    primary = SolarOrange,
    onPrimary = Color.White,
    primaryContainer = SolarContainer,
    onPrimaryContainer = OnSolarContainer,
    secondary = SolarOrangeDark,
    onSecondary = Color.White,
    secondaryContainer = SolarContainerSoft,
    onSecondaryContainer = OnSolarContainer,
    tertiary = GridTeal,
    onTertiary = Color.White,
    tertiaryContainer = GridTealContainer,
    onTertiaryContainer = OnGridTealContainer,
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
    primary = SolarOrangeLight,
    onPrimary = OnSolarOrangeDark,
    primaryContainer = SolarContainerDark,
    onPrimaryContainer = SolarContainer,
    secondary = SolarGlow,
    onSecondary = OnSolarOrangeDark,
    secondaryContainer = SolarContainerSoftDark,
    onSecondaryContainer = SolarContainer,
    tertiary = GridTealLight,
    onTertiary = Color(0xFF00363F),
    tertiaryContainer = GridTealContainerDark,
    onTertiaryContainer = GridTealContainer,
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

/** Success colours, which sit outside the Material scheme so the brand can be orange. */
data class SuccessColors(val container: Color, val onContainer: Color, val accent: Color)

/** Returns the success pair that matches the active light or dark scheme. */
@Composable
fun successColors(darkTheme: Boolean = isSystemInDarkTheme()): SuccessColors = if (darkTheme) {
    SuccessColors(GridSuccessContainerDark, GridSuccessLight, GridSuccessLight)
} else {
    SuccessColors(GridSuccessContainer, OnGridSuccessContainer, GridSuccess)
}

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
