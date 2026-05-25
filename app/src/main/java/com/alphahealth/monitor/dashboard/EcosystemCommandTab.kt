package com.alphahealth.monitor.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alphahealth.monitor.data.PredictiveRiskScore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.Air

@Composable
fun EcosystemCommandTab(
    risk: PredictiveRiskScore,
    liveEda: Float,
    liveHydration: Float,
    heartRate: Int,
    watchPpgSqi: Double,
    sleepApneaRecent: Boolean,
    energyScore: Int,
    calendarBlocked: Boolean,
    onDeviceExplanation: String,
    isSignalDegraded: Boolean,
    onSignalDegradedChange: (Boolean) -> Unit,
    isNocturnal: Boolean,
    onNocturnalChange: (Boolean) -> Unit,
    ringEda: Float,
    onRingEdaChange: (Float) -> Unit,
    ringHydration: Float,
    onRingHydrationChange: (Float) -> Unit,
    onTriggerConsent: () -> Unit,
    onSimulateStress: () -> Unit,
    onResetSimulation: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Viewing Area: Hero Card with Vulnerability Index Arc
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
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
                        val sweepAngleValue = (risk.vulnerabilityIndex.toFloat() / 100f) * 270f
                        val progressColor = when {
                            risk.vulnerabilityIndex >= 70 -> Color(0xFFEF4444)
                            risk.vulnerabilityIndex >= 40 -> AlphaWarningAmber
                            else -> AlphaMintGreen
                        }
                        drawArc(
                            color = progressColor,
                            startAngle = 135f,
                            sweepAngle = sweepAngleValue,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${risk.vulnerabilityIndex}",
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
                            contentDescription = "Analytics Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "VULNERABILITY INDEX",
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
                }
            }
        }

        // Floating Status Banner (Muted Ruby)
        if (calendarBlocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AlphaMutedRuby, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = "Intervention Status",
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Google Calendar Rest Slot Active: 90 minutes blocked to buffer metabolic exhaustions.",
                        color = Color(0xFFFCA5A5),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Split Status Pill for Datastore Logs
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Shield Icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "V1.1.0 STORAGE LOGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Energy Score",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$energyScore",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlphaWarningAmber,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Sleep Apnea",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (sleepApneaRecent) "Detected" else "Clear",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sleepApneaRecent) Color(0xFFFCA5A5) else AlphaMintGreen,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // On-Device AI Explainability
        NeuralPulseDataCard(title = "ON-DEVICE AI EXPLAINABILITY") {
            Column {
                Text(
                    text = onDeviceExplanation,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Source: On-Device Gemma (INT8) | Active Input: ${risk.resolvedDeviceSource}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bio-Stream Telemetry (2x2 Grid)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "WEAR OS BIO-STREAM TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryGridItem(
                        label = "EDA CONDUCTANCE",
                        value = "${String.format("%.2f", liveEda)} uS",
                        valueColor = if (liveEda > 4f) Color(0xFFEF4444) else AlphaWarningAmber,
                        icon = Icons.Outlined.ElectricBolt,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryGridItem(
                        label = "CELL HYDRATION",
                        value = "${String.format("%.0f%%", liveHydration * 100f)}",
                        valueColor = AlphaAccentBlue,
                        icon = Icons.Outlined.InvertColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryGridItem(
                        label = "WATCH PPG SQI",
                        value = if (watchPpgSqi == 1.0) "Clinical Grade" else "Motion Noise",
                        valueColor = if (watchPpgSqi == 1.0) AlphaMintGreen else Color(0xFFEF4444),
                        icon = Icons.Outlined.Air,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryGridItem(
                        label = "HEART RATE",
                        value = "$heartRate BPM",
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        icon = Icons.Outlined.FavoriteBorder,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action Toggles & Controls Array
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DIAGNOSTIC & DEVELOPMENT CONTROL PANEL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSignalDegradedChange(!isSignalDegraded) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSignalDegraded) Color(0xFFEF4444) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSignalDegraded) Color(0x1AEF4444) else Color(0xFF1C1C1E),
                        contentColor = if (isSignalDegraded) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = if (isSignalDegraded) "Wrist Shift Active" else "Signal Normal",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedButton(
                    onClick = { onNocturnalChange(!isNocturnal) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF1C1C1E),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = if (isNocturnal) "Nocturnal: Ring" else "Active: Watch",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            OutlinedButton(
                onClick = onTriggerConsent,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF1C1C1E),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "Connect Health SDK Store",
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulateStress,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF1C1C1E),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = "Drift Stress",
                        fontSize = 10.sp
                    )
                }

                OutlinedButton(
                    onClick = onResetSimulation,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF1C1C1E),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = "Reset Sync",
                        fontSize = 10.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
