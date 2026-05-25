package com.alphahealth.monitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Custom color system for NeuralPulse, providing health-specific semantic tokens
 * beyond Material3's default scheme. Inspired by Google AI Edge Gallery's CustomColors pattern.
 *
 * Accessed via `MaterialTheme.neuralPulseColors` extension property.
 */
@Immutable
data class NeuralPulseCustomColors(
    // App branding
    val appTitleGradientColors: List<Color> = listOf(Color(0xFF80CBC4), Color(0xFF00BFA5)),

    // Health metric gradients (for icons, charts, accents)
    val heartRateGradient: List<Color> = listOf(Color(0xFFE25F57), Color(0xFFDB372D)),
    val spo2Gradient: List<Color> = listOf(Color(0xFF669DF6), Color(0xFF3174F1)),
    val stressGradient: List<Color> = listOf(Color(0xFFFDD45D), Color(0xFFCAA12A)),
    val ecgGradient: List<Color> = listOf(Color(0xFF41A15F), Color(0xFF128937)),

    // Combined gradient list for indexed access (HR=0, SpO2=1, Stress=2, ECG=3)
    val pulseLoaderGradientColors: List<List<Color>> = listOf(
        listOf(Color(0xFFE25F57), Color(0xFFDB372D)),
        listOf(Color(0xFF669DF6), Color(0xFF3174F1)),
        listOf(Color(0xFFFDD45D), Color(0xFFCAA12A)),
        listOf(Color(0xFF41A15F), Color(0xFF128937)),
    ),

    // Vital status colors
    val vitalNormalColor: Color = Color(0xFF10B981),
    val vitalWarningColor: Color = Color(0xFFF59E0B),
    val vitalCriticalColor: Color = Color(0xFFEF4444),

    // Chat UI
    val chatUserBubbleBg: Color = Color(0xFF00695C),
    val chatAgentBubbleBg: Color = Color(0xFF1b1c1d),

    // Card surfaces
    val cardGlassBg: Color = Color(0xFF0B0B0C),

    // Task/metric card backgrounds (per category)
    val metricCardBgColors: List<Color> = listOf(
        Color(0xFF181210), // HR - warm dark
        Color(0xFF131711), // SpO2 - cool dark
        Color(0xFF191924), // Stress - deep dark
        Color(0xFF1A1813), // ECG - earthy dark
    ),

    // Bottom gradient overlay
    val homeBottomGradient: List<Color> = listOf(Color(0x00000000), Color(0xFF000000)),

    // Status colors
    val successColor: Color = Color(0xFF10B981),
    val linkColor: Color = Color(0xFF9DCAFC),

    // Record / voice
    val recordButtonBgColor: Color = Color(0xFFEE675C),
    val waveFormBgColor: Color = Color(0xFFAAAAAA),

    // Warning / error containers
    val warningContainerColor: Color = Color(0xFF554C33),
    val warningTextColor: Color = Color(0xFFFCC934),
    val errorContainerColor: Color = Color(0xFF523A3B),
    val errorTextColor: Color = Color(0xFFEE675C),

    // Info
    val modelInfoIconColor: Color = Color(0xFFCCCCCC),

    // Promo banner brush
    val promoBannerBgBrush: Brush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Color(0x4200BFA5),
            0.6f to Color(0x4200897B),
            1.0f to Color(0x42004D40),
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
    ),
)

val LocalNeuralPulseColors = staticCompositionLocalOf { NeuralPulseCustomColors() }

// ── Light Mode ──────────────────────────────────────────────────────────────

val lightNeuralPulseColors = NeuralPulseCustomColors(
    appTitleGradientColors = listOf(Color(0xFF80CBC4), Color(0xFF00897B)),
    chatUserBubbleBg = Color(0xFF00897B),
    chatAgentBubbleBg = Color(0xFFE0F2F1),
    cardGlassBg = Color(0xFFFFFFFF),
    metricCardBgColors = listOf(
        Color(0xFFFFF5F5), // HR
        Color(0xFFF1F6FE), // SpO2
        Color(0xFFFFFBF0), // Stress
        Color(0xFFF4FBF6), // ECG
    ),
    homeBottomGradient = listOf(Color(0x00F8F9FF), Color(0xFFE0F2F1)),
    linkColor = Color(0xFF00695C),
    successColor = Color(0xFF0D9668),
    warningContainerColor = Color(0xFFFEF7E0),
    warningTextColor = Color(0xFFE37400),
    errorContainerColor = Color(0xFFFCE8E6),
    errorTextColor = Color(0xFFD93025),
    promoBannerBgBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Color(0x4280CBC4),
            0.6f to Color(0x4200BFA5),
            1.0f to Color(0x42009688),
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
    ),
)

// ── Dark Mode (default) ─────────────────────────────────────────────────────

val darkNeuralPulseColors = NeuralPulseCustomColors()
// Uses all default values which are already tuned for dark mode

// ── Extension property ──────────────────────────────────────────────────────

val MaterialTheme.neuralPulseColors: NeuralPulseCustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalNeuralPulseColors.current
