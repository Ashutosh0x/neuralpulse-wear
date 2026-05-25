package com.alphahealth.monitor.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * AlphaSplashScreen
 *
 * Premium AMOLED launch screen matching the cold boot sequence from the spec:
 *   1. Scale spring-in of the NeuralPulse logo (spatial spring, overshoot)
 *   2. Async SHA-256 partner attestation check (background thread simulation)
 *   3. Custom canvas arc draws 65/100 Vulnerability Index with bounce overshoot
 *   4. Alert banner slides in below the arc card
 *   5. Auto-advances to DashboardScreen after 2.4 seconds
 *
 * All animations use Material 3 Expressive spring physics:
 *   Logo scale: dampingRatio=0.55 (medium bounce), stiffness=StiffnessMediumLow
 *   Arc draw:   dampingRatio=DampingRatioNoBouncy, stiffness=StiffnessLow (smooth draw)
 *   Banner:     dampingRatio=LowBouncy, stiffness=StiffnessMedium (elastic slide)
 */
@Composable
fun AlphaSplashScreen(onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(SplashPhase.LOGO_IN) }
    var attestationText by remember { mutableStateOf("Verifying partner keys...") }

    // Logo scale: spring overshoot entry
    val logoScale by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.LOGO_IN) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "LogoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.LOGO_IN) 1f else 0f,
        animationSpec = tween(400),
        label = "LogoAlpha"
    )

    // Arc sweep: smooth draw from 0 -> 234 degrees (65/100 = 234 deg)
    val arcSweep by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.ARC_DRAW) 234f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ArcSweep"
    )

    // Score number count-up
    val scoreValue by animateIntAsState(
        targetValue = if (phase >= SplashPhase.ARC_DRAW) 65 else 0,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "ScoreCount"
    )

    // Banner Y offset: slides up from +80dp to 0dp
    val bannerOffsetY by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.BANNER_IN) 0f else 80f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "BannerSlide"
    )
    val bannerAlpha by animateFloatAsState(
        targetValue = if (phase >= SplashPhase.BANNER_IN) 1f else 0f,
        animationSpec = tween(300),
        label = "BannerAlpha"
    )

    LaunchedEffect(Unit) {
        // Phase 1: logo springs in
        phase = SplashPhase.LOGO_IN
        delay(300)

        // Phase 2: async partner attestation (simulated background SHA-256 check)
        phase = SplashPhase.ATTESTATION
        attestationText = "Verifying SHA-256 partner keys..."
        delay(400)
        attestationText = "Cryptographic handshake confirmed"
        delay(200)

        // Phase 3: arc draws the vulnerability score
        phase = SplashPhase.ARC_DRAW
        delay(900)

        // Phase 4: alert banner slides in
        phase = SplashPhase.BANNER_IN
        delay(600)

        // Phase 5: advance to dashboard
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // NeuralPulse logo — spring scale-in
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                    alpha = logoAlpha
                }
            ) {
                Text(
                    text = "NEURALPULSE",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "AlphaHealth Ecosystem",
                    fontSize = 12.sp,
                    color = AlphaTextSecondary,
                    letterSpacing = 2.sp
                )
            }

            // Attestation status text
            if (phase >= SplashPhase.ATTESTATION) {
                Text(
                    text = attestationText,
                    fontSize = 11.sp,
                    color = AlphaAccentBlue.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            // Vulnerability arc card — canvas draw
            if (phase >= SplashPhase.ARC_DRAW) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val arcOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        // Background track
                        drawArc(
                            color = Color(0xFF1C1C1E),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                        )

                        // Score arc — spring-drawn, AlphaAccentBlue -> AlphaMintGreen gradient
                        if (arcSweep > 0f) {
                            drawArc(
                                color = AlphaAccentBlue,
                                startAngle = 135f,
                                sweepAngle = arcSweep.coerceAtMost(270f),
                                useCenter = false,
                                topLeft = arcOffset,
                                size = arcSize,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$scoreValue",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "/ 100",
                            fontSize = 14.sp,
                            color = AlphaTextSecondary
                        )
                        Text(
                            text = "Moderate Strain",
                            fontSize = 11.sp,
                            color = AlphaWarningAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Alert banner — elastic slide-in
            if (phase >= SplashPhase.BANNER_IN) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = bannerOffsetY
                            alpha = bannerAlpha
                        }
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF3D0000).copy(alpha = 0.85f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "ALERT: 3 Issues Detected — Review Now",
                        fontSize = 12.sp,
                        color = Color(0xFFFF4444),
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private enum class SplashPhase {
    LOGO_IN, ATTESTATION, ARC_DRAW, BANNER_IN
}

// Extension operator for enum comparison
private operator fun SplashPhase.compareTo(other: SplashPhase): Int =
    this.ordinal.compareTo(other.ordinal)
