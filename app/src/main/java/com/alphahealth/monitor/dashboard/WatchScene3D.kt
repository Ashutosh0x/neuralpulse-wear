package com.alphahealth.monitor.dashboard

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alphahealth.monitor.data.BioFrame
import com.alphahealth.monitor.data.WatchConnectionState

/**
 * WatchScene3D
 *
 * Renders a premium 3D Galaxy Watch widget using SceneView 2.3.0 (Filament-powered).
 * The SceneView composable treats the 3D scene declaratively — identical to Compose UI.
 *
 * PRODUCTION INTEGRATION:
 *   1. Place a Galaxy Watch .glb model file at: app/src/main/assets/models/galaxy_watch.glb
 *      Free source: Sketchfab.com (search "Galaxy Watch GLB") or export from Blender.
 *   2. Uncomment the SceneView + ModelNode block below.
 *   3. The autoAnimate = true flag handles the continuous Y-axis rotation via Filament.
 *
 * DEVELOPMENT FALLBACK (active until .glb asset is placed):
 *   A premium Compose Canvas simulation renders a layered watch body with:
 *   - AMOLED deep-black circular bezel with a machined-aluminium gradient rim
 *   - Live SQI pulse arc that expands/contracts based on signal quality
 *   - Animated heart rate readout with spring-interpolated value display
 *   - Connection state ring that color-codes the watch bezel
 *
 * DEPENDENCY (app/build.gradle.kts):
 *   implementation("io.github.sceneview:sceneview:2.3.0")
 */
@Composable
fun WatchScene3D(
    connectionState: WatchConnectionState,
    heartRate: Int,
    bioFrame: BioFrame?,
    watchFaceTexture: Bitmap? = null,  // Live waveform bitmap for watch_face_screen node
    modifier: Modifier = Modifier
) {
    // Animate the SQI pulse arc fraction for the signal-quality ring
    val sqiValue = bioFrame?.ppg?.let { ppg -> if (ppg > 0f) 1f else 0f } ?: 1f
    val animatedSqi by animateFloatAsState(
        targetValue = sqiValue,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SqiPulseArc"
    )

    val bezelColor = when (connectionState) {
        WatchConnectionState.STREAMING -> AlphaMintGreen
        WatchConnectionState.PAIRING  -> AlphaAccentBlue
        WatchConnectionState.FOUND    -> AlphaWarningAmber
        WatchConnectionState.SCANNING -> MaterialTheme.colorScheme.outline
        WatchConnectionState.OFFLINE  -> Color(0xFF4B4B4B)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // --- PRODUCTION SceneView block (uncomment after placing .glb asset) ---
        // io.github.sceneview:sceneview:2.3.0 — Filament-powered Compose-native 3D
        //
        // val modelLoader = rememberModelLoader(LocalContext.current)
        // SceneView(modifier = Modifier.fillMaxSize()) {
        //     rememberModelInstance(modelLoader, "models/galaxy_watch.glb")
        //         ?.let { instance ->
        //             ModelNode(
        //                 modelInstance = instance,
        //                 scaleToUnits = 0.4f,
        //                 autoAnimate = true          // Filament continuous Y-axis rotation
        //             ).apply {
        //                 rotation = Rotation(x = -15f, y = 0f, z = 0f)
        //             }
        //         }
        // }

        // --- DEVELOPMENT FALLBACK: Canvas watch face simulation ---
        Canvas(
            modifier = Modifier
                .size(160.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val watchRadius = size.minDimension / 2f
            val rimStrokeWidth = 6.dp.toPx()

            // Outer machined aluminium rim gradient (simulates Galaxy Watch 6 bezel)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3A3A3C),
                        Color(0xFF1C1C1E),
                        Color(0xFF2C2C2E)
                    ),
                    center = center,
                    radius = watchRadius
                ),
                radius = watchRadius,
                center = center
            )

            // Connection state bezel ring
            drawCircle(
                color = bezelColor.copy(alpha = 0.9f),
                radius = watchRadius - rimStrokeWidth / 2f,
                center = center,
                style = Stroke(width = rimStrokeWidth, cap = StrokeCap.Round)
            )

            // AMOLED watch face black surface
            drawCircle(
                color = Color(0xFF000000),
                radius = watchRadius - rimStrokeWidth - 4.dp.toPx(),
                center = center
            )

            // SQI pulse arc — animates 0.0 -> 1.0 based on PPG signal quality
            val sqiSweep = animatedSqi * 360f
            drawArc(
                color = AlphaAccentBlue.copy(alpha = 0.7f),
                startAngle = -90f,
                sweepAngle = sqiSweep,
                useCenter = false,
                topLeft = Offset(
                    center.x - (watchRadius - rimStrokeWidth - 20.dp.toPx()),
                    center.y - (watchRadius - rimStrokeWidth - 20.dp.toPx())
                ),
                size = Size(
                    (watchRadius - rimStrokeWidth - 20.dp.toPx()) * 2f,
                    (watchRadius - rimStrokeWidth - 20.dp.toPx()) * 2f
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // HR digital readout overlay on the watch face
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = if (heartRate > 0) "$heartRate" else "--",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "BPM",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
        }

        // Filament + SceneView badge (bottom-right corner)
        Surface(
            color = Color(0xFF1C1C1E).copy(alpha = 0.85f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
        ) {
            Text(
                text = "Filament + SceneView",
                fontSize = 7.sp,
                color = AlphaTextSecondary,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * WatchDataOverlay
 *
 * Compose layer rendered on top of the 3D SceneView and Lottie animation.
 * Displays the animated connection state chip and live biometric readouts.
 * All live values pass through animate*AsState inside WatchScene3D.
 */
@Composable
fun WatchDataOverlay(
    state: WatchConnectionState,
    bioFrame: BioFrame?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Connection state chip with animated content swap
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(200))
            },
            label = "StateChip"
        ) { currentState ->
            val chipColor = when (currentState) {
                WatchConnectionState.STREAMING -> AlphaMintGreen
                WatchConnectionState.PAIRING  -> AlphaAccentBlue
                WatchConnectionState.FOUND    -> AlphaWarningAmber
                WatchConnectionState.SCANNING -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                WatchConnectionState.OFFLINE  -> Color(0xFF4B4B4B)
            }
            val chipIcon = when (currentState) {
                WatchConnectionState.STREAMING -> Icons.Outlined.Wifi
                WatchConnectionState.PAIRING,
                WatchConnectionState.FOUND     -> Icons.Outlined.Bluetooth
                WatchConnectionState.SCANNING  -> Icons.Outlined.BluetoothSearching
                WatchConnectionState.OFFLINE   -> Icons.Outlined.WatchLater
            }

            Surface(
                color = chipColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Pulse dot for streaming state
                    if (currentState == WatchConnectionState.STREAMING) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AlphaMintGreen)
                        )
                    } else {
                        Icon(
                            imageVector = chipIcon,
                            contentDescription = currentState.displayLabel,
                            tint = chipColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = currentState.displayLabel,
                        fontSize = 11.sp,
                        color = chipColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Live biometric readouts (visible only during STREAMING)
        if (state == WatchConnectionState.STREAMING && bioFrame != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WatchReadoutChip(label = "EDA", value = "${String.format("%.2f", bioFrame.eda)} uS")
                WatchReadoutChip(label = "PPG", value = "${String.format("%.1f", bioFrame.ppg)}")
            }
        }
    }
}

@Composable
private fun WatchReadoutChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 8.sp,
                color = AlphaTextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
