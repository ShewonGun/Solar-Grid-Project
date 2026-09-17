/* ============================================================================
 * File        : Color.kt
 * Purpose     : Colour tokens for the SmartGrid mobile client. A warm solar
 *               palette: yellowish-orange brand colours over warm neutrals,
 *               with a cool teal kept aside for informational surfaces and a
 *               separate green reserved for success feedback.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Light palette ---------------------------------------------------------
// Brand. SolarOrange is deep enough to carry white text on filled buttons;
// SolarGlow is the brighter yellow-orange used for icons and accents that sit
// on tinted containers rather than behind body text.
val SolarOrange = Color(0xFFB35C00)
val SolarOrangeDark = Color(0xFF8A4600)
val SolarGlow = Color(0xFFF59E0B)
val SolarContainer = Color(0xFFFFE3BF)
val OnSolarContainer = Color(0xFF4A2600)
val SolarContainerSoft = Color(0xFFFFF3E2)

// Informational (kept cool so notices read as neutral next to the brand).
val GridTeal = Color(0xFF1F6D7C)
val GridTealContainer = Color(0xFFD3EDF3)
val OnGridTealContainer = Color(0xFF06353F)

// Success feedback stays green even though the brand is orange.
val GridSuccess = Color(0xFF1E6B45)
val GridSuccessContainer = Color(0xFFD6EFE0)
val OnGridSuccessContainer = Color(0xFF0A3A24)

// Warm neutrals.
val GridBackground = Color(0xFFFDF8F2)
val GridSurface = Color(0xFFFFFFFF)
val GridSurfaceVariant = Color(0xFFF4EDE3)
val GridOutline = Color(0xFFE8DCCC)
val GridInk = Color(0xFF241B12)
val GridMuted = Color(0xFF6D6155)
val GridError = Color(0xFFB3261E)
val GridErrorContainer = Color(0xFFF9DEDC)

// ---- Dark palette ----------------------------------------------------------
val SolarOrangeLight = Color(0xFFFFB765)
val OnSolarOrangeDark = Color(0xFF472500)
val SolarContainerDark = Color(0xFF6A3800)
val SolarContainerSoftDark = Color(0xFF2E2013)

val GridTealLight = Color(0xFF86D2E2)
val GridTealContainerDark = Color(0xFF0C4956)

val GridSuccessLight = Color(0xFF7FD3A6)
val GridSuccessContainerDark = Color(0xFF12432D)

val GridBackgroundDark = Color(0xFF14100C)
val GridSurfaceDark = Color(0xFF1C1712)
val GridSurfaceVariantDark = Color(0xFF2A231B)
val GridOutlineDark = Color(0xFF453B31)
val GridInkDark = Color(0xFFF0E7DC)
val GridMutedDark = Color(0xFFB7A99A)
