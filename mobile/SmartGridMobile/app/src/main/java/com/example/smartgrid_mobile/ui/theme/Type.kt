/* ============================================================================
 * File        : Type.kt
 * Purpose     : Typography for the SmartGrid mobile client. Applies the bundled
 *               Outfit family across the whole Material 3 type scale, so every
 *               screen picks it up from the theme with no local overrides.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.smartgrid_mobile.R

/**
 * Outfit, bundled in res/font rather than downloaded, so the app renders the
 * same offline and on devices without Google Play services. The four weights
 * are the ones the screens actually ask for; anything else is synthesised from
 * the nearest of them by the system.
 */
val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold)
)

/** Material 3 defaults, kept for their sizes, line heights and letter spacing. */
private val Default = Typography()

// Only the font family is swapped: the metrics stay on the Material 3 scale.
val Typography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = Outfit),
    displayMedium = Default.displayMedium.copy(fontFamily = Outfit),
    displaySmall = Default.displaySmall.copy(fontFamily = Outfit),
    headlineLarge = Default.headlineLarge.copy(fontFamily = Outfit),
    headlineMedium = Default.headlineMedium.copy(fontFamily = Outfit),
    headlineSmall = Default.headlineSmall.copy(fontFamily = Outfit),
    titleLarge = Default.titleLarge.copy(fontFamily = Outfit),
    titleMedium = Default.titleMedium.copy(fontFamily = Outfit),
    titleSmall = Default.titleSmall.copy(fontFamily = Outfit),
    bodyLarge = Default.bodyLarge.copy(fontFamily = Outfit),
    bodyMedium = Default.bodyMedium.copy(fontFamily = Outfit),
    bodySmall = Default.bodySmall.copy(fontFamily = Outfit),
    labelLarge = Default.labelLarge.copy(fontFamily = Outfit),
    labelMedium = Default.labelMedium.copy(fontFamily = Outfit),
    labelSmall = Default.labelSmall.copy(fontFamily = Outfit)
)
