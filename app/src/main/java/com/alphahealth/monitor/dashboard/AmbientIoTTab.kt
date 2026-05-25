package com.alphahealth.monitor.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.Thermostat

@Composable
fun AmbientIoTTab(
    isNocturnal: Boolean,
    ringEda: Float,
    ringHydration: Float,
    liveEda: Float,
    liveHydration: Float,
    onRingEdaChange: (Float) -> Unit,
    onRingHydrationChange: (Float) -> Unit
) {
    val scrollState = rememberScrollState()
    var targetTemp by remember { mutableStateOf(19.5f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Viewing Area: Active multi-wearable topology (Diagram Canvas mapping Active watch / Standby Ring)
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
                        text = "MULTI-WEARABLE NET TOPOLOGY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Outlined.SettingsInputAntenna,
                        contentDescription = "Topology Antenna",
                        tint = AlphaAccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val primaryColor = MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Drawing connecting nodes
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        val watchX = centerX - 100.dp.toPx()
                        val ringX = centerX + 100.dp.toPx()

                        // Draw connection lines
                        drawLine(
                            color = if (!isNocturnal) AlphaAccentBlue else Color.Gray.copy(alpha = 0.5f),
                            start = Offset(watchX, centerY),
                            end = Offset(centerX, centerY),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        drawLine(
                            color = if (isNocturnal) AlphaMintGreen else Color.Gray.copy(alpha = 0.5f),
                            start = Offset(ringX, centerY),
                            end = Offset(centerX, centerY),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Draw core hub node
                        drawCircle(
                            color = primaryColor,
                            radius = 18.dp.toPx(),
                            center = Offset(centerX, centerY)
                        )

                        // Draw watch node
                        drawCircle(
                            color = if (!isNocturnal) AlphaAccentBlue else Color.Gray,
                            radius = 14.dp.toPx(),
                            center = Offset(watchX, centerY)
                        )

                        // Draw ring node
                        drawCircle(
                            color = if (isNocturnal) AlphaMintGreen else Color.Gray,
                            radius = 12.dp.toPx(),
                            center = Offset(ringX, centerY)
                        )
                    }

                    // Nodes descriptors
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Smart Watch\n${if (!isNocturnal) "ACTIVE" else "STANDBY"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isNocturnal) AlphaAccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp)
                        )

                        Text(
                            text = "Ecosystem Hub\nON",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        Text(
                            text = "Smart Ring\n${if (isNocturnal) "ACTIVE" else "STANDBY"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNocturnal) AlphaMintGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                        )
                    }
                }
            }
        }

        // Interaction Area: Ambient IoT SmartThings controls
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
                        text = "SMARTTHINGS sleep automation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Outlined.Thermostat,
                        contentDescription = "Thermostat",
                        tint = AlphaWarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Confirm target climate baseline. When biosensors declare sleep transitions, SmartThings Matter nodes drop temperatures to optimize deep sleep profiles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Automated Temperature Target",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${String.format("%.1f", targetTemp)} °C",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlphaWarningAmber,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = targetTemp,
                    onValueChange = { targetTemp = it },
                    valueRange = 16f..24f,
                    steps = 16,
                    colors = SliderDefaults.colors(
                        thumbColor = AlphaWarningAmber,
                        activeTrackColor = AlphaWarningAmber,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Ring Live Sensor Simulation Data Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "SMART RING SIMULATION DATA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryGridItem(
                        label = "RING EDA SWEAT",
                        value = "${String.format("%.2f", ringEda)} uS",
                        valueColor = AlphaWarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryGridItem(
                        label = "RING FLUID SHIFT",
                        value = "${String.format("%.0f%%", ringHydration * 100f)}",
                        valueColor = AlphaAccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = ringEda,
                    onValueChange = onRingEdaChange,
                    valueRange = 0.5f..5.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = AlphaWarningAmber,
                        activeTrackColor = AlphaWarningAmber
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Slider(
                    value = ringHydration,
                    onValueChange = onRingHydrationChange,
                    valueRange = 0.2f..0.9f,
                    colors = SliderDefaults.colors(
                        thumbColor = AlphaAccentBlue,
                        activeTrackColor = AlphaAccentBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
