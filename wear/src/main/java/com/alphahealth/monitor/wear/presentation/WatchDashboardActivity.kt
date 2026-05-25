package com.alphahealth.monitor.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Bedtime
import com.alphahealth.monitor.wear.tracking.HighPerformanceBioEngine
import com.alphahealth.monitor.wear.tracking.RunningDynamicsEngine
import com.alphahealth.monitor.shared.SyncProtocols
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.sin

class WatchDashboardActivity : ComponentActivity() {

    private lateinit var bioEngine: HighPerformanceBioEngine
    private lateinit var dynamicsEngine: RunningDynamicsEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bioEngine = HighPerformanceBioEngine(applicationContext)
        dynamicsEngine = RunningDynamicsEngine(applicationContext)

        setContent {
            WatchAppTheme {
                val edaValue by bioEngine.edaStream.collectAsState()
                val hydrationRatio by bioEngine.hydrationRatioStream.collectAsState()
                val ppgSqi by bioEngine.ppgSqiStream.collectAsState()
                val status by bioEngine.engineStatus.collectAsState()

                val gaitTelemetry by dynamicsEngine.gaitStream.collectAsState()
                var isFormTrackingActive by remember { mutableStateOf(false) }

                LaunchedEffect(isFormTrackingActive) {
                    if (isFormTrackingActive) {
                        dynamicsEngine.initiateFormTracking()
                    }
                }

                WatchDashboard(
                    status = status,
                    edaValue = edaValue,
                    hydrationRatio = hydrationRatio,
                    ppgSqi = ppgSqi,
                    gaitActive = isFormTrackingActive,
                    gaitGct = gaitTelemetry.groundContactTimeMs,
                    gaitOscillation = gaitTelemetry.verticalOscillationCm,
                    gaitAsymmetry = gaitTelemetry.asymmetryAlert,
                    gaitBalance = gaitTelemetry.balanceRatioLeftRight,
                    onToggleGait = { isFormTrackingActive = !isFormTrackingActive },
                    onBreathingTrigger = {
                        android.widget.Toast.makeText(
                            this, 
                            "Haptic pulse: Inhale... Exhale...", 
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bioEngine.disconnect()
    }
}

@Composable
fun WatchAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = Color(0xFF3B82F6),
            secondary = Color(0xFF10B981),
            error = Color(0xFFF87171),
            background = Color.Black // Pure AMOLED Saver
        ),
        content = content
    )
}

object VariableFontProvider {
    private val systemFontFile: File? by lazy {
        val paths = listOf(
            "/system/fonts/Roboto-Flex.ttf",
            "/system/fonts/RobotoFlex-Regular.ttf",
            "/system/fonts/RobotoFlex.ttf",
            "/system/fonts/BreezeSans-Regular.ttf",
            "/system/fonts/BreezeSans.ttf",
            "/system/fonts/GoogleSansFlex-Regular.ttf",
            "/system/fonts/GoogleSansFlex.ttf"
        )
        paths.map { File(it) }.firstOrNull { it.exists() }
    }

    @OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
    fun getFontFamily(weight: Int, width: Float = 100f): FontFamily {
        val file = systemFontFile
        return if (file != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                FontFamily(
                    Font(
                        file = file,
                        variationSettings = FontVariation.Settings(
                            FontVariation.weight(weight),
                            FontVariation.width(width)
                        )
                    )
                )
            } catch (e: Exception) {
                FontFamily.SansSerif
            }
        } else {
            FontFamily.SansSerif
        }
    }
}

@Composable
fun WatchDashboard(
    status: String,
    edaValue: Float,
    hydrationRatio: Float,
    ppgSqi: Double,
    gaitActive: Boolean,
    gaitGct: Float,
    gaitOscillation: Float,
    gaitAsymmetry: Boolean,
    gaitBalance: Pair<Float, Float>,
    onToggleGait: () -> Unit,
    onBreathingTrigger: () -> Unit
) {
    // Wear OS responsive column layout state for crown rotary control mapping
    val scrollState = rememberScrollState()

    // Dynamic weight mapping to prevent layout jumps by adjusting axis parameters smoothly
    val edaWeight = remember(edaValue) {
        val clamped = edaValue.coerceIn(0.0f, 10.0f)
        val ratio = clamped / 10.0f
        (100 + ratio * 700).toInt()
    }
    val edaFontFamily = remember(edaWeight) {
        VariableFontProvider.getFontFamily(weight = edaWeight)
    }

    val hydrationWeight = remember(hydrationRatio) {
        val clamped = hydrationRatio.coerceIn(0.0f, 1.0f)
        val ratio = clamped
        (100 + ratio * 700).toInt()
    }
    val hydrationFontFamily = remember(hydrationWeight) {
        VariableFontProvider.getFontFamily(weight = hydrationWeight)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 2.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Curved styling mockup (pure text here)
            Text(
                text = "NEURALPULSE WEAR",
                color = Color.LightGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp
            )

            Chip(
                label = { Text(text = status, fontSize = 11.sp) },
                onClick = {},
                colors = ChipDefaults.primaryChipColors(
                    backgroundColor = when (status) {
                        "Hardware Connected" -> Color(0xFF065F46)
                        "Connecting..." -> Color(0xFF1E3A8A)
                        else -> Color(0xFF27272A)
                    },
                    contentColor = Color.White
                ),
                modifier = Modifier.height(26.dp)
            )

            PPGWaveform()

            Card(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PPG SIGNAL QUALITY",
                            fontSize = 9.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "PPG Signal Icon",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (ppgSqi == 1.0) "Clinical Grade" else "Motion Noise (Adjust)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ppgSqi == 1.0) Color(0xFF10B981) else Color(0xFFF87171)
                    )
                }
            }

            // Monospaced Tabular Number fonts prevent layout shifting during updates
            Card(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STRESS (EDA)",
                            fontSize = 9.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = "Stress Bolt Icon",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format("%.2f", edaValue),
                            style = TextStyle(
                                fontFamily = edaFontFamily,
                                fontWeight = FontWeight(edaWeight),
                                fontFeatureSettings = "tnum",
                                fontSize = 22.sp,
                                color = if (edaValue > 4.0) Color(0xFFF87171) else Color(0xFF10B981)
                            )
                        )
                        Text(
                            text = "µS",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Card(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CELL FLUID (BIA)",
                            fontSize = 9.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Outlined.WaterDrop,
                            contentDescription = "Hydration Icon",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format("%.0f%%", hydrationRatio * 100f),
                            style = TextStyle(
                                fontFamily = hydrationFontFamily,
                                fontWeight = FontWeight(hydrationWeight),
                                fontFeatureSettings = "tnum",
                                fontSize = 22.sp,
                                color = Color(0xFF3B82F6)
                            )
                        )
                        Text(
                            text = "ICW Ratio",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Card(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SLEEP APNEA INDEX",
                            fontSize = 9.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = "Sleep Icon",
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Clear Baseline",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Running Dynamic Form Metrics
            Card(
                onClick = onToggleGait,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "GAIT DYNAMICS",
                        fontSize = 9.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    if (!gaitActive) {
                        Text(
                            text = "Tap to Track Run",
                            fontSize = 12.sp,
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "GCT: ${gaitGct.toInt()}ms | VO: ${gaitOscillation}cm",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontFeatureSettings = "tnum",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "Balance: ${String.format("%.1f%%", gaitBalance.first)} L / ${String.format("%.1f%%", gaitBalance.second)} R",
                            style = TextStyle(
                                fontFeatureSettings = "tnum",
                                fontSize = 10.sp,
                                color = if (gaitAsymmetry) Color(0xFFF87171) else Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Button(
                onClick = onBreathingTrigger,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF3B82F6)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "Breathe Guided",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PPGWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_shift"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(Color(0xFF0F172A))
    ) {
        val width = size.width
        val height = size.height
        val path = Path()

        val points = 50
        val step = width / points
        
        path.moveTo(0f, height / 2f)

        for (i in 0..points) {
            val x = i * step
            val wave = sin((i.toFloat() / points.toFloat() * 4f * Math.PI.toFloat()) - phaseShift)
            val waveHarmonic = 0.3f * sin((i.toFloat() / points.toFloat() * 8f * Math.PI.toFloat()) - phaseShift * 2)
            
            val y = (height / 2f) + (wave + waveHarmonic) * (height * 0.3f)
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFF10B981),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
