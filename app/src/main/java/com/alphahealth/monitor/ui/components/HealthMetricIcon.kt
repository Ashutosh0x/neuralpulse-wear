package com.alphahealth.monitor.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Health metric icon with gradient-filled background shape and centered foreground icon.
 *
 * Creates a layered icon effect:
 * 1. Background: A circular/organic shape filled with a gradient color (via BlendMode.SrcIn)
 * 2. Foreground: A white Material icon centered on top
 *
 * The icon can optionally animate in with a reveal effect controlled by [animationProgress].
 *
 * Ported from Google AI Edge Gallery's TaskIcon pattern.
 *
 * @param icon The Material icon to display
 * @param gradientColors The gradient colors for the background shape fill
 * @param width The total width/height of the icon composable
 * @param animationProgress 0f = hidden, 1f = fully visible (for staggered reveal)
 */
@Composable
fun HealthMetricIcon(
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    animationProgress: Float = 1f,
) {
    val brush = linearGradient(colors = gradientColors)

    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        // Background gradient circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    alpha = 0.99f, // Required for BlendMode to work
                    compositingStrategy = CompositingStrategy.Offscreen,
                    translationX = 80 * (1 - animationProgress),
                    rotationZ = -180 * (1 - animationProgress),
                )
                .drawWithContent {
                    // Draw a circle as the base shape
                    drawCircle(color = Color.White)
                    // Apply gradient fill via SrcIn blend
                    drawRect(brush = brush, blendMode = BlendMode.SrcIn)
                },
        )

        // Foreground icon with delayed appear
        val iconProgress = if (animationProgress >= 0.8f) {
            (animationProgress - 0.8f) / 0.2f
        } else {
            0f
        }

        Icon(
            imageVector = icon,
            tint = Color.White,
            modifier = Modifier
                .size(width * 0.55f)
                .graphicsLayer { alpha = iconProgress }
                .scale(iconProgress),
            contentDescription = null,
        )
    }
}

/**
 * Composable that returns a staggered animation progress value.
 *
 * Useful for card entrance animations where each card fades in with a delay.
 *
 * @param delayMs Initial delay before animation starts
 * @param durationMs Duration of the fade animation
 * @return A float from 0f to 1f representing the animation progress
 */
@Composable
fun rememberStaggeredProgress(
    delayMs: Long,
    durationMs: Int = 600,
): Float {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs)
        animatable.animateTo(
            1f,
            animationSpec = tween(durationMs, easing = LinearEasing),
        )
    }

    return animatable.value
}
