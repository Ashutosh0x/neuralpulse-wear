package com.alphahealth.monitor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography system for NeuralPulse.
 *
 * Defines a comprehensive type scale with custom text styles for health dashboards,
 * chat interfaces, empty states, and home page titles.
 *
 * Inspired by Google AI Edge Gallery's Type.kt with Nunito font family.
 * Uses system sans-serif as default; swap to bundled fonts (Inter/Nunito) later.
 */

val appFontFamily = FontFamily.SansSerif

private val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = appFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = appFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = appFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = appFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = appFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = appFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = appFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = appFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = appFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = appFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = appFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = appFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = appFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = appFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = appFontFamily),
)

// ── Custom Text Styles ──────────────────────────────────────────────────────

/** Large display title for home page / splash screen (42sp, tight tracking). */
val homePageTitleStyle = baseline.displayMedium.copy(
    fontFamily = appFontFamily,
    fontSize = 42.sp,
    lineHeight = 42.sp,
    letterSpacing = (-1).sp,
    fontWeight = FontWeight.Medium,
)

/** Title with zero letter-spacing for tighter text. */
val titleMediumNarrow = baseline.titleMedium.copy(
    fontFamily = appFontFamily,
    letterSpacing = 0.0.sp,
)

/** Small bold title at 12sp — used for card subtitles. */
val titleSmaller = baseline.titleSmall.copy(
    fontFamily = appFontFamily,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
)

/** Label with zero letter-spacing. */
val labelSmallNarrow = baseline.labelSmall.copy(
    fontFamily = appFontFamily,
    letterSpacing = 0.0.sp,
)

/** Label with zero spacing and medium weight. */
val labelSmallNarrowMedium = baseline.labelSmall.copy(
    fontFamily = appFontFamily,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.0.sp,
)

/** Body text with zero letter-spacing. */
val bodySmallNarrow = baseline.bodySmall.copy(
    fontFamily = appFontFamily,
    letterSpacing = 0.0.sp,
)

/** Medium-sized body text with zero spacing (14sp). */
val bodySmallMediumNarrow = baseline.bodySmall.copy(
    fontFamily = appFontFamily,
    letterSpacing = 0.0.sp,
    fontSize = 14.sp,
)

/** Bold variant of medium body text. */
val bodySmallMediumNarrowBold = baseline.bodySmall.copy(
    fontFamily = appFontFamily,
    letterSpacing = 0.0.sp,
    fontSize = 14.sp,
    fontWeight = FontWeight.Bold,
)

/** Body large with slightly tighter spacing. */
val bodyLargeNarrow = baseline.bodyLarge.copy(
    letterSpacing = 0.2.sp,
)

/** Body medium with medium weight emphasis. */
val bodyMediumMedium = baseline.bodyMedium.copy(
    fontWeight = FontWeight.Medium,
)

/** Headline large with medium weight. */
val headlineLargeMedium = baseline.headlineLarge.copy(
    fontWeight = FontWeight.Medium,
)

/** Title for empty state screens (37sp, generous line height). */
val emptyStateTitle = baseline.headlineSmall.copy(
    fontSize = 37.sp,
    lineHeight = 50.sp,
)

/** Description text for empty state screens (16sp). */
val emptyStateContent = baseline.headlineSmall.copy(
    fontSize = 16.sp,
    lineHeight = 22.sp,
)
