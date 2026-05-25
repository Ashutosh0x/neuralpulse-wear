package com.alphahealth.monitor.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alphahealth.monitor.data.PredictiveRiskScore

/**
 * VulnerabilityTransitionOrchestrator
 *
 * Physics-backed spring card that expands from a compact summary dial into a
 * full deep-diagnostic timeline console. The arc sweep and metric values are
 * driven by animate*AsState, ensuring smooth rolling transitions when live
 * bio-stream packets arrive from the Galaxy Watch without any layout jitter.
 *
 * Motion design references:
 *   - Material 3 Expressive MotionScheme spatial tokens (DampingRatioLowBouncy / StiffnessLow)
 *   - Samsung One UI 6 card morphing guidelines (24dp corner, AMOLED black surface)
 *   - Statistical Kurtosis SQI validation window 2.3 <= K <= 5.2 [Samsung SDK, Kotlin]
 */
@Composable
fun VulnerabilityTransitionOrchestrator(
    risk: PredictiveRiskScore,
    calendarBlocked: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Context-aware value interpolation: arc sweep animates smoothly as new telemetry arrives
    val animatedVulnerabilityIndex by animateIntAsState(
        targetValue = risk.vulnerabilityIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "VulnerabilityIndexArc"
    )

    // Animate individual recovery dimension bars using rolling float interpolation
    val animatedStrainScore by animateFloatAsState(
        targetValue = risk.strainScore,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "StrainScoreFloat"
    )

    val animatedSleepCapacity by animateIntAsState(
        targetValue = risk.sleepCapacityPercent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SleepCapacityInt"
    )

    val animatedAutonomicRecovery by animateIntAsState(
        targetValue = risk.autonomicRecoveryPercent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AutonomicRecoveryInt"
    )

    // Compute arc sweep using the animated (interpolated) index to prevent canvas jitter
    val progressColor = when {
        animatedVulnerabilityIndex >= 70 -> Color(0xFFEF4444)
        animatedVulnerabilityIndex >= 40 -> AlphaWarningAmber
        else -> AlphaMintGreen
    }
    val animatedSweep = (animatedVulnerabilityIndex.toFloat() / 100f) * 270f

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                // Spatial spring: expressive overshoot so card expansion feels alive
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { isExpanded = !isExpanded }
    ) {
        if (!isExpanded) {
            // COMPACT MODE: Single-row dial + status summary
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Animated arc canvas — sweep driven by animatedSweep not raw index
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp)
                ) {
                    Canvas(modifier = Modifier.size(90.dp)) {
                        drawArc(
                            color = Color(0xFF262626),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = progressColor,
                            startAngle = 135f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$animatedVulnerabilityIndex",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "/100",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = "Systemic Recovery Index",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "SYSTEMIC RECOVERY INDEX",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = risk.conditionRisk,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Tap to expand autonomic analytics",
                        fontSize = 10.sp,
                        color = AlphaTextSecondary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        } else {
            // EXPANDED MODE: Full deep-diagnostic timeline console
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AUTONOMIC DEEP DIAGNOSTIC CONSOLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlphaAccentBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap to collapse",
                        fontSize = 10.sp,
                        color = AlphaTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large expanded arc with animated sweep
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            drawArc(
                                color = Color(0xFF262626),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = progressColor,
                                startAngle = 135f,
                                sweepAngle = animatedSweep,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$animatedVulnerabilityIndex",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Autonomic",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column {
                        Text(
                            text = risk.conditionRisk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Confidence Interval: ${String.format("%.0f%%", risk.confidenceInterval * 100f)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Source: ${risk.resolvedDeviceSource}",
                            fontSize = 10.sp,
                            color = AlphaTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "RECOVERY BUDGET DIMENSIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // All dimension values are animated — no raw jumps in the UI
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DiagnosticDetailRow(
                        label = "Cardiovascular Strain (0-21.0 scale)",
                        value = String.format("%.1f", animatedStrainScore)
                    )
                    DiagnosticDetailRow(
                        label = "Sleep Capacity Restoration",
                        value = "${animatedSleepCapacity}%"
                    )
                    DiagnosticDetailRow(
                        label = "Autonomic Vagal Recovery Capacity",
                        value = "${animatedAutonomicRecovery}%"
                    )
                    DiagnosticDetailRow(
                        label = "Postprandial Glycemic Load Risk",
                        value = "${risk.glycemicRiskPercent}%"
                    )
                }

                if (calendarBlocked) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = AlphaMutedRuby,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Clinical Action: 90-minute Google Calendar recovery block scheduled to buffer sympathetic exhaustion cascade.",
                            fontSize = 11.sp,
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AlphaAccentBlue,
            fontFamily = FontFamily.Monospace
        )
    }
}
