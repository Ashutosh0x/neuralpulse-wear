package com.alphahealth.monitor.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alphahealth.monitor.data.PulmonologyReport
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.PictureAsPdf

@Composable
fun ClinicalVaultTab(
    pulmonologyReport: PulmonologyReport?,
    sleepApneaRecent: Boolean,
    heartRate: Int,
    onGenerateReport: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Viewing Area: Chronological Trend Chart for Sleep Apnea / SpO2 trajectory
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "MULTI-NIGHT TRAJECTORY TREND",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Oxygen Saturation (SpO2) Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Line Chart for oxygen saturation trajectory
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val points = listOf(98f, 97f, 96f, 92f, 85f, 93f, 97f)
                        val widthBetween = size.width / (points.size - 1)
                        val heightMultiplier = size.height / 30f // Mapping range 70-100

                        // Draw background grid lines
                        val gridLines = 3
                        for (i in 0..gridLines) {
                            val y = size.height * i / gridLines
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.15f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw path
                        for (i in 0 until points.size - 1) {
                            val startX = i * widthBetween
                            val startY = size.height - ((points[i] - 70f) * heightMultiplier)
                            val endX = (i + 1) * widthBetween
                            val endY = size.height - ((points[i + 1] - 70f) * heightMultiplier)

                            drawLine(
                                color = if (points[i] < 90f || points[i+1] < 90f) Color(0xFFEF4444) else AlphaMintGreen,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            drawCircle(
                                color = if (points[i] < 90f) Color(0xFFEF4444) else AlphaMintGreen,
                                radius = 4.dp.toPx(),
                                center = Offset(startX, startY)
                            )
                        }

                        // Draw last circle
                        val lastX = (points.size - 1) * widthBetween
                        val lastY = size.height - ((points.last() - 70f) * heightMultiplier)
                        drawCircle(
                            color = AlphaMintGreen,
                            radius = 4.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Night 1 (98%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Night 4 (85% Desat)", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    Text(text = "Night 7 (97%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Interaction Area: Clinical observationsSynced over Android PHR interface
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLINICAL LOGS & PHR ARCHIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Outlined.HistoryEdu,
                        contentDescription = "Clinical Vault",
                        tint = AlphaAccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClinicalRecordRow(
                        category = "Mindfulness / Vagal Tone",
                        description = "Systemic recoverability parameters registered in offline datastore.",
                        timestamp = "2 hours ago"
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ClinicalRecordRow(
                        category = "Respiratory Event Profile",
                        description = if (sleepApneaRecent) "Detected 4 apnea sequences during deep sleep cycles." else "No significant sleep apnea events recorded over the last 24h.",
                        timestamp = "Last sleep session"
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ClinicalRecordRow(
                        category = "Telemetry Ingestion Rate",
                        description = "Heart rate streaming resolved at standard clinical precision: $heartRate BPM.",
                        timestamp = "Just now"
                    )
                }
            }
        }

        // Provider Ready PDF Export Action
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "CLINICAL REPORT EXPORT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (pulmonologyReport == null) {
                    Text(
                        text = "Assemble sleep apnea indicators, heart rate variances, and blood work panels into a secure provider-ready PDF document encrypted using Android KeyStore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "Generated Cryptographic Document:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = pulmonologyReport.fileName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column {
                                Text(text = "Total Apnea Events", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${pulmonologyReport.totalApneaEvents}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column {
                                Text(text = "Min SpO2 Level", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${pulmonologyReport.lowestOxygenSaturation}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pulmonologyReport.lowestOxygenSaturation < 90) Color(0xFFEF4444) else AlphaMintGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onGenerateReport,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF1C1C1E),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = "PDF Exporter",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = "Export Pulmonologist Report", fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ClinicalRecordRow(category: String, description: String, timestamp: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = timestamp,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}
