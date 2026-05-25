package com.alphahealth.monitor.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Health-themed rotational loader displaying 4 health metric icons in a 2×2 grid
 * with synchronized rotation and pulsing scale animation.
 *
 * Ported from Google AI Edge Gallery's RotationalLoader with health-specific icons
 * and color gradients:
 * - Heart (red) | Breathing (blue)
 * - Stress (amber) | ECG (green)
 *
 * @param size Total size of the loader composable.
 */
@Composable
fun HealthPulseLoader(size: Dp = 48.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "health-pulse")

    // Outer rotation: full 360° with custom easing for non-linear speed
    val rotationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = CubicBezierEasing(0.5f, 0.16f, 0f, 0.71f)),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    // Inner scale: breathing pulse between 1.0 and 0.4
    val scaleProgress by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    val curRotationZ = 45f + rotationProgress * 360f
    val gridSpacing = size * 0.1f
    val iconSize = size * 0.3f

    // Health metric icons and their gradient colors
    val icons = listOf(
        Icons.Outlined.FavoriteBorder,  // Heart rate
        Icons.Outlined.Air,             // SpO2 / breathing
        Icons.Outlined.Psychology,      // Stress / brain
        Icons.Outlined.MonitorHeart,    // ECG
    )

    val gradients = listOf(
        listOf(Color(0xFFE25F57), Color(0xFFDB372D)), // Red
        listOf(Color(0xFF669DF6), Color(0xFF3174F1)), // Blue
        listOf(Color(0xFFFDD45D), Color(0xFFCAA12A)), // Amber
        listOf(Color(0xFF41A15F), Color(0xFF128937)), // Green
    )

    val alignments = listOf(
        Alignment.BottomEnd,
        Alignment.BottomStart,
        Alignment.TopEnd,
        Alignment.TopStart,
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(gridSpacing),
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = curRotationZ }
            .clearAndSetSemantics {},
    ) {
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(gridSpacing)) {
                for (col in 0..1) {
                    val index = row * 2 + col
                    HealthPulseIcon(
                        icon = icons[index],
                        gradientColors = gradients[index],
                        alignment = alignments[index],
                        cellSize = (size - gridSpacing) / 2,
                        iconSize = iconSize,
                        rotationZ = -curRotationZ,
                        scale = scaleProgress,
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthPulseIcon(
    icon: ImageVector,
    gradientColors: List<Color>,
    alignment: Alignment,
    cellSize: Dp,
    iconSize: Dp,
    rotationZ: Float,
    scale: Float,
) {
    val brush = linearGradient(colors = gradientColors)

    Box(
        modifier = Modifier.size(cellSize),
        contentAlignment = alignment,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    // Important: alpha slightly below 1 enables blending
                    alpha = 0.99f
                    this.rotationZ = rotationZ
                    scaleX = scale
                    scaleY = scale
                }
                .drawWithContent {
                    drawContent()
                    drawRect(brush = brush, blendMode = BlendMode.SrcIn)
                },
        )
    }
}
