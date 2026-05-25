package com.alphahealth.monitor.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alphahealth.monitor.data.PredictiveRiskScore

/**
 * EcosystemCommandTab
 *
 * Primary dashboard view for the AlphaHealth NeuralPulse companion app.
 * Implements the "Ecosystem Command" layout described in the technical ecosystem diagram:
 *
 *   - Animated Systemic Recovery Index hero card (spring-expanded via VulnerabilityTransitionOrchestrator)
 *   - Spatial spring alert banner (AnimatedVisibility with expandVertically)
 *   - Two-column grid: AI Clinical Insights (left) | Wear Telemetry Grid (right)
 *   - On-Device Gemma SLM Explainability card
 *   - Device Conflict Resolution Matrix card
 *   - Diagnostic and Development Control Panel
 *
 * All live bio-stream values displayed to the user pass through animate*AsState rolling
 * interpolation to eliminate layout jitter when Galaxy Watch packets arrive at 25 Hz.
 */
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

    // Context-Aware Value Interpolation (animate*AsState)
    // When raw bio-streams arrive from the Galaxy Watch (e.g. HR shifts 74->96 BPM),
    // values animate along a natural spring curve instead of snapping directly.
    val animHeartRate by animateIntAsState(
        targetValue = heartRate,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "HeartRateSmooth"
    )
    val animLiveEda by animateFloatAsState(
        targetValue = liveEda,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "EdaSmoothFloat"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // -----------------------------------------------------------------------
        // 1. HERO CARD: Systemic Recovery Index with physics-backed spring expansion
        //    Card bounding box scales outward on tap; arc sweep animates via
        //    animate*AsState inside VulnerabilityTransitionOrchestrator.
        // -----------------------------------------------------------------------
        VulnerabilityTransitionOrchestrator(
            risk = risk,
            calendarBlocked = calendarBlocked
        )

        // -----------------------------------------------------------------------
        // 2. NON-DISRUPTIVE ALERT BANNER
        //    Spatial spring animation glides the banner into view.
        //    When dismissed, telemetry cards slide upward via AnimatedVisibility
        //    shrinkVertically to prevent abrupt screen jumps.
        // -----------------------------------------------------------------------
        AnimatedVisibility(
            visible = calendarBlocked,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            Surface(
                color = AlphaMutedRuby,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = "Recovery Block Active",
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Google Calendar Rest Slot Active: 90-minute buffer scheduled to protect metabolic recovery.",
                        color = Color(0xFFFCA5A5),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // -----------------------------------------------------------------------
        // 3. TWO-COLUMN DASHBOARD MATRIX
        //    Left  -> AI Clinical Insights (Glycemic Forecast, Autonomic Stress, SQI)
        //    Right -> Wear Telemetry Grid  (Cardio, Autonomic Recovery, Sleep Capacity)
        //
        //    Live values routed through animate*AsState to prevent numeric jitter.
        // -----------------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- LEFT CARD: AI CLINICAL INSIGHTS ---
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Biotech,
                            contentDescription = "AI Clinical Insights",
                            tint = AlphaAccentBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AI CLINICAL INSIGHTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Row A: AI Glycemic Impact Forecast
                    // Powered by MediaPipe INT8 MobileNetV3 food classifier
                    ClinicalInsightRow(
                        label = "Glycemic Forecast",
                        value = "${risk.glycemicRiskPercent}% Load Risk",
                        subLabel = "MediaPipe [INT8 MobileNetV3]",
                        valueColor = when {
                            risk.glycemicRiskPercent >= 70 -> Color(0xFFEF4444)
                            risk.glycemicRiskPercent >= 30 -> AlphaWarningAmber
                            else -> AlphaMintGreen
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )

                    // Row B: Autonomic Stress Transmissions
                    // EDA = Electrodermal Activity / Galvanic Skin Conductance at 25 Hz
                    // animLiveEda is the spring-interpolated value — no raw value jumps
                    ClinicalInsightRow(
                        label = "Autonomic Stress",
                        value = "${String.format("%.2f", animLiveEda)} uS",
                        subLabel = "Galvanic Skin Conductance",
                        valueColor = when {
                            animLiveEda >= 4.5f -> Color(0xFFEF4444)
                            animLiveEda >= 3.0f -> AlphaWarningAmber
                            else -> AlphaMintGreen
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )

                    // Row C: Continuous Signal Quality Index
                    // Statistical Kurtosis validation window: 2.3 <= K <= 5.2
                    // SQI=1.0 -> pristine clinical grade | SQI<1.0 -> motion artifact noise
                    ClinicalInsightRow(
                        label = "Signal Quality (SQI)",
                        value = if (watchPpgSqi == 1.0) "Pristine" else "Motion Noise",
                        subLabel = "PPG Kurtosis 2.3 < K < 5.2",
                        valueColor = if (watchPpgSqi == 1.0) AlphaMintGreen else Color(0xFFEF4444)
                    )
                }
            }

            // --- RIGHT CARD: WEAR TELEMETRY GRID ---
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MonitorHeart,
                            contentDescription = "Wear Telemetry Grid",
                            tint = AlphaMintGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "WEAR TELEMETRY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Row A: Cardiovascular Metrics
                    // Heart rate uses animHeartRate (spring-smoothed) to prevent jitter
                    // SpO2 derived from sleep apnea detection state
                    val spo2Label = if (sleepApneaRecent) "88% SpO2 (Desaturation)" else "98% SpO2 (Normal)"
                    ClinicalInsightRow(
                        label = "Cardiovascular",
                        value = "$animHeartRate BPM",
                        subLabel = spo2Label,
                        valueColor = when {
                            animHeartRate >= 90 -> Color(0xFFEF4444)
                            animHeartRate >= 80 -> AlphaWarningAmber
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )

                    // Row B: Autonomic Recovery State (Vagal HRV Index)
                    val recoveryLabel = when {
                        risk.autonomicRecoveryPercent > 60 -> "High Recovery"
                        risk.autonomicRecoveryPercent > 40 -> "Moderate Recovery"
                        else -> "Low Recovery"
                    }
                    val recoveryColor = when {
                        risk.autonomicRecoveryPercent > 60 -> AlphaMintGreen
                        risk.autonomicRecoveryPercent > 40 -> AlphaWarningAmber
                        else -> Color(0xFFEF4444)
                    }
                    ClinicalInsightRow(
                        label = "Autonomic Recovery",
                        value = "$recoveryLabel",
                        subLabel = "Capacity: ${risk.autonomicRecoveryPercent}% | Vagal HRV",
                        valueColor = recoveryColor
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )

                    // Row C: Sleep Capacity Budget
                    ClinicalInsightRow(
                        label = "Sleep Duration",
                        value = "7.5 Hours",
                        subLabel = "Budget Restoration: ${risk.sleepCapacityPercent}%",
                        valueColor = when {
                            risk.sleepCapacityPercent >= 70 -> AlphaMintGreen
                            risk.sleepCapacityPercent >= 50 -> AlphaWarningAmber
                            else -> Color(0xFFEF4444)
                        }
                    )
                }
            }
        }

        // -----------------------------------------------------------------------
        // 4. ON-DEVICE GEMMA SLM EXPLAINABILITY CARD
        //    Matches the "AI Explainability Engine" panel in the ecosystem diagram.
        //    On-Device Gemma icon translates raw biometrics into wellness suggestions.
        //    Chips: [Gemma] [MediaPipe]
        // -----------------------------------------------------------------------
        NeuralPulseDataCard(
            title = "ON-DEVICE GEMMA SLM EXPLAINABILITY",
            icon = Icons.Outlined.Psychology,
            iconColor = AlphaAccentBlue
        ) {
            Column {
                Text(
                    text = onDeviceExplanation,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gemma chip
                    Surface(
                        color = AlphaAccentBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Gemma",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlphaAccentBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    // MediaPipe chip
                    Surface(
                        color = AlphaMintGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "MediaPipe",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlphaMintGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Source: ${risk.resolvedDeviceSource}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // -----------------------------------------------------------------------
        // 5. DEVICE CONFLICT RESOLUTION MATRIX
        //    Matches the "Conflict Resolution" section in the ecosystem diagram.
        //    Prioritized wearable tier: Ring (nocturnal) vs Watch (active).
        //    Sensor degradation mode reverts to passive baseline data on wrist shift.
        // -----------------------------------------------------------------------
        NeuralPulseDataCard(
            title = "DEVICE CONFLICT RESOLUTION MATRIX",
            icon = Icons.Outlined.Analytics,
            iconColor = AlphaTextSecondary
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DiagnosticDetailRow(
                    label = "Active Wearable Source",
                    value = risk.resolvedDeviceSource
                )
                DiagnosticDetailRow(
                    label = "Signal Degradation Mode",
                    value = if (isSignalDegraded) "Wrist Shift (Passive Baseline)" else "Active (Full Resolution)"
                )
                DiagnosticDetailRow(
                    label = "Dual-Device Resolution",
                    value = if (isNocturnal) "Nocturnal: Ring Primary" else "Active: Watch Primary"
                )
                DiagnosticDetailRow(
                    label = "FDA Wellness Compliance",
                    value = "General Wellness (0-100)"
                )
            }
        }

        // -----------------------------------------------------------------------
        // 6. DIAGNOSTIC & DEVELOPMENT CONTROL PANEL
        //    Outlined minimalist buttons for signal mode toggling and simulation.
        // -----------------------------------------------------------------------
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
                    border = BorderStroke(
                        1.dp,
                        if (isSignalDegraded) Color(0xFFEF4444)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSignalDegraded) Color(0x1AEF4444) else Color(0xFF1C1C1E),
                        contentColor = if (isSignalDegraded) Color(0xFFFCA5A5)
                        else MaterialTheme.colorScheme.onSurface
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
                Text(text = "Connect Health Connect Store", fontSize = 11.sp)
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
                    Text(text = "Drift Stress", fontSize = 10.sp)
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
                    Text(text = "Reset Sync", fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * ClinicalInsightRow
 *
 * Reusable three-line metric row used inside the two-column dashboard matrix.
 * Displays a label, a prominently-colored value, and a technical sub-label.
 */
@Composable
private fun ClinicalInsightRow(
    label: String,
    value: String,
    subLabel: String,
    valueColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = subLabel,
            fontSize = 8.sp,
            color = AlphaTextSecondary,
            modifier = Modifier.padding(top = 1.dp),
            lineHeight = 12.sp
        )
    }
}
