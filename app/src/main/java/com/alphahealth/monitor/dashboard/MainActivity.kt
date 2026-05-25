package com.alphahealth.monitor.dashboard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.alphahealth.monitor.data.HealthDataManager
import com.alphahealth.monitor.data.VulnerabilityEngine
import com.alphahealth.monitor.data.PredictiveRiskScore
import com.alphahealth.monitor.data.OnDeviceExplainabilityEngine
import com.alphahealth.monitor.data.connect.HealthConnectFhirOrchestrator
import com.alphahealth.monitor.data.WatchDataTransporter
import com.alphahealth.monitor.data.ClinicalReportExporter
import com.alphahealth.monitor.data.CalendarScheduler
import com.alphahealth.monitor.data.GeminiAppFunctions
import com.alphahealth.monitor.data.PulmonologyReport
import com.alphahealth.monitor.vision.FoodScanResult
import com.alphahealth.monitor.vision.FoodVisionEngine
import com.alphahealth.monitor.shared.SyncProtocols
import kotlinx.coroutines.launch
import com.alphahealth.monitor.dashboard.NeuralPulseDataCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Shield
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var healthDataManager: HealthDataManager
    private lateinit var vulnerabilityEngine: VulnerabilityEngine
    private lateinit var foodVisionEngine: FoodVisionEngine
    private lateinit var reportExporter: ClinicalReportExporter
    private lateinit var calendarScheduler: CalendarScheduler
    private lateinit var geminiAppFunctions: GeminiAppFunctions
    private lateinit var explainEngine: OnDeviceExplainabilityEngine
    private lateinit var fhirOrchestrator: HealthConnectFhirOrchestrator
    private var watchTransporter: WatchDataTransporter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthDataManager = HealthDataManager(applicationContext)
        vulnerabilityEngine = VulnerabilityEngine(applicationContext)
        foodVisionEngine = FoodVisionEngine(applicationContext)
        reportExporter = ClinicalReportExporter(applicationContext)
        calendarScheduler = CalendarScheduler(applicationContext)
        geminiAppFunctions = GeminiAppFunctions(applicationContext)
        explainEngine = OnDeviceExplainabilityEngine(applicationContext)
        fhirOrchestrator = HealthConnectFhirOrchestrator(applicationContext)

        setContent {
            NeuralPulseTheme {
                val connectionState by healthDataManager.connectionState.collectAsState()
                
                var liveEda by remember { mutableStateOf(1.8f) }
                var liveHydration by remember { mutableStateOf(0.62f) }
                var heartRate by remember { mutableStateOf(72) }
                
                var sleepApneaRecent by remember { mutableStateOf(false) }
                var energyScore by remember { mutableStateOf(78) }

                var scannedFood by remember { mutableStateOf<FoodScanResult?>(null) }
                var hoursSinceScannedMeal by remember { mutableStateOf(0.5f) }

                var gaitGct by remember { mutableStateOf(240) }
                var gaitOscillation by remember { mutableStateOf(8.5f) }
                var gaitAsymmetry by remember { mutableStateOf(false) }
                var gaitBalanceLeft by remember { mutableStateOf(50.0f) }

                var pulmonologyReport by remember { mutableStateOf<PulmonologyReport?>(null) }
                var calendarBlocked by remember { mutableStateOf(false) }
                var geminiVoiceFeedback by remember { mutableStateOf("") }

                // Simulator and priority state variables
                var isSignalDegraded by remember { mutableStateOf(false) }
                var isNocturnal by remember { mutableStateOf(false) }
                var ringEda by remember { mutableStateOf(1.2f) }
                var ringHydration by remember { mutableStateOf(0.68f) }
                var onDeviceExplanation by remember { mutableStateOf("Analyzing system indicators...") }

                var riskAnalysis by remember {
                    mutableStateOf(
                        PredictiveRiskScore(
                            conditionRisk = "Calibrating",
                            vulnerabilityIndex = 15,
                            confidenceInterval = 0.78f,
                            recommendedMicroIntervention = "Syncing local biosensor feeds...",
                            glycemicRiskPercent = 5,
                            thermalNutritionalWarning = "Thermal baselines stable.",
                            isDegradedState = false,
                            resolvedDeviceSource = "Smart Watch"
                        )
                    )
                }

                var watchPpgSqi by remember { mutableStateOf(1.0) }

                DisposableEffect(Unit) {
                    watchTransporter = WatchDataTransporter(
                        context = applicationContext,
                        onTelemetryReceived = { eda, hr ->
                            liveEda = eda.toFloat()
                            heartRate = hr
                        },
                        onSqiReceived = { sqi ->
                            watchPpgSqi = sqi
                        }
                    )
                    onDispose {
                        watchTransporter?.unregister(applicationContext)
                        watchTransporter = null
                    }
                }

                LaunchedEffect(
                    liveEda, liveHydration, heartRate, energyScore, sleepApneaRecent, 
                    scannedFood, hoursSinceScannedMeal, isSignalDegraded, isNocturnal, 
                    ringEda, ringHydration
                ) {
                    vulnerabilityEngine.analyzeVulnerability(
                        liveEda = liveEda,
                        liveHydration = liveHydration,
                        heartRateCurrent = heartRate,
                        avgEnergyScore = energyScore,
                        sleepApneaActive = sleepApneaRecent,
                        lastScannedFood = scannedFood,
                        hoursSinceScannedMeal = hoursSinceScannedMeal,
                        isSignalDegraded = isSignalDegraded,
                        isNocturnal = isNocturnal,
                        ringEda = ringEda,
                        ringHydration = ringHydration
                    ).collect { score ->
                        riskAnalysis = score
                        
                        if (score.vulnerabilityIndex >= 70 && !calendarBlocked) {
                            val success = calendarScheduler.scheduleRecoveryBlock(90L)
                            if (success) {
                                calendarBlocked = true
                            }
                        }
                    }
                }

                // Dynamic local explainability generation and Health Connect FHIR data-pipeline sync
                LaunchedEffect(riskAnalysis, scannedFood, hoursSinceScannedMeal) {
                    val food = scannedFood
                    val carbs = food?.macronutrients?.get("Carbs") ?: 0f
                    val foodName = food?.foodItemName ?: "N/A"
                    onDeviceExplanation = explainEngine.generateExplainabilityReport(
                        recoveryCapacity = 100 - riskAnalysis.vulnerabilityIndex,
                        skinTempElevation = if (sleepApneaRecent && food != null && food.baselineCalories > 450) 0.4f else 0.0f,
                        hoursSinceMeal = hoursSinceScannedMeal,
                        scannedFoodCarbs = carbs,
                        scannedFoodName = foodName
                    )

                    // Export observation record using FHIR standard on low recovery budgets
                    if (100 - riskAnalysis.vulnerabilityIndex < 40) {
                        val fhirJson = """
                            {
                              "resourceType": "Observation",
                              "id": "neuralpulse-telemetry-${System.currentTimeMillis()}",
                              "status": "final",
                              "category": [{
                                "coding": [{
                                  "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                                  "code": "laboratory"
                                }]
                              }],
                              "code": {
                                "coding": [{
                                  "system": "http://loinc.org",
                                  "code": "883-9",
                                  "display": "Activity Intensity & Autonomic Homeostasis"
                                }]
                              },
                              "subject": { "reference": "Patient/example" },
                              "valueQuantity": {
                                "value": ${100 - riskAnalysis.vulnerabilityIndex},
                                "unit": "%",
                                "system": "http://unitsofmeasure.org",
                                "code": "%"
                              },
                              "note": [{ "text": "$onDeviceExplanation" }]
                            }
                        """.trimIndent()
                        fhirOrchestrator.exportValidatedTelemetryToFhir(fhirJson)
                    }
                }

                DashboardScreen(
                    connectionState = connectionState,
                    risk = riskAnalysis,
                    liveEda = liveEda,
                    liveHydration = liveHydration,
                    heartRate = heartRate,
                    watchPpgSqi = watchPpgSqi,
                    sleepApneaRecent = sleepApneaRecent,
                    energyScore = energyScore,
                    scannedFood = scannedFood,
                    gaitGct = gaitGct,
                    gaitOscillation = gaitOscillation,
                    gaitAsymmetry = gaitAsymmetry,
                    gaitBalanceLeft = gaitBalanceLeft,
                    pulmonologyReport = pulmonologyReport,
                    calendarBlocked = calendarBlocked,
                    geminiVoiceFeedback = geminiVoiceFeedback,
                    onDeviceExplanation = onDeviceExplanation,
                    isSignalDegraded = isSignalDegraded,
                    onSignalDegradedChange = { isSignalDegraded = it },
                    isNocturnal = isNocturnal,
                    onNocturnalChange = { isNocturnal = it },
                    ringEda = ringEda,
                    onRingEdaChange = { ringEda = it },
                    ringHydration = ringHydration,
                    onRingHydrationChange = { ringHydration = it },
                    onTriggerConsent = {
                        healthDataManager.connectToSamsungHealth(
                            onConnected = {
                                lifecycleScope.launch {
                                    val data = healthDataManager.fetchHealthData()
                                    if (data.containsKey("EnergyScore")) {
                                        energyScore = data["EnergyScore"] as Int
                                        sleepApneaRecent = (data["SleepApneaOccurrences"] as Int) > 0
                                    }
                                }
                            },
                            onFailure = {}
                        )
                    },
                    onTriggerFoodScan = { foodToken ->
                        scannedFood = when (foodToken) {
                            "chicken" -> FoodScanResult("Grilled Chicken Breast", 0.94f, 165, mapOf("Protein" to 31f, "Carbs" to 0f, "Fats" to 3.6f))
                            "avocado" -> FoodScanResult("Avocado Slice", 0.88f, 161, mapOf("Protein" to 2f, "Carbs" to 8.5f, "Fats" to 14.7f))
                            "pasta" -> FoodScanResult("Pasta Carbonara", 0.91f, 490, mapOf("Protein" to 14f, "Carbs" to 58f, "Fats" to 19f))
                            else -> null
                        }
                    },
                    onClearFood = {
                        scannedFood = null
                    },
                    onRunGaitTracking = {
                        gaitGct = 265
                        gaitOscillation = 9.2f
                        gaitAsymmetry = true
                        gaitBalanceLeft = 47.2f
                        Toast.makeText(this, "Gait metrics synced.", Toast.LENGTH_SHORT).show()
                    },
                    onGenerateReport = {
                        pulmonologyReport = reportExporter.generatePulmonologyReport(
                            sleepApneaCount = if (sleepApneaRecent) 4 else 0,
                            minSpO2Value = if (sleepApneaRecent) 82 else 96,
                            avgHR = heartRate
                        )
                        Toast.makeText(this, "Clinical anomalies PDF generated successfully with password-encryption.", Toast.LENGTH_SHORT).show()
                    },
                    onQueryGemini = {
                        val response = geminiAppFunctions.checkPhysicalRecoveryStatus(riskAnalysis.vulnerabilityIndex)
                        geminiVoiceFeedback = response.vocalBreakdown
                    },
                    onSimulateStress = {
                        liveEda = 5.2f
                        liveHydration = 0.56f
                        heartRate = 96
                        energyScore = 48
                        sleepApneaRecent = true
                    },
                    onResetSimulation = {
                        liveEda = 1.8f
                        liveHydration = 0.62f
                        heartRate = 72
                        energyScore = 80
                        sleepApneaRecent = false
                        scannedFood = null
                        gaitGct = 240
                        gaitOscillation = 8.5f
                        gaitAsymmetry = false
                        gaitBalanceLeft = 50.0f
                        pulmonologyReport = null
                        calendarBlocked = false
                        geminiVoiceFeedback = ""
                        isSignalDegraded = false
                        isNocturnal = false
                        ringEda = 1.2f
                        ringHydration = 0.68f
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        explainEngine.close()
    }
}

@Composable
fun NeuralPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF3B82F6),
            secondary = Color(0xFF10B981),
            error = Color(0xFFF87171),
            background = Color(0xFF0B0B0C),
            surface = Color(0xFF18181A)
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
fun DashboardScreen(
    connectionState: HealthDataManager.ConnectionState,
    risk: PredictiveRiskScore,
    liveEda: Float,
    liveHydration: Float,
    heartRate: Int,
    watchPpgSqi: Double,
    sleepApneaRecent: Boolean,
    energyScore: Int,
    scannedFood: FoodScanResult?,
    gaitGct: Int,
    gaitOscillation: Float,
    gaitAsymmetry: Boolean,
    gaitBalanceLeft: Float,
    pulmonologyReport: PulmonologyReport?,
    calendarBlocked: Boolean,
    geminiVoiceFeedback: String,
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
    onTriggerFoodScan: (String) -> Unit,
    onClearFood: () -> Unit,
    onRunGaitTracking: () -> Unit,
    onGenerateReport: () -> Unit,
    onQueryGemini: () -> Unit,
    onSimulateStress: () -> Unit,
    onResetSimulation: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Bold, condensed display font family to let large stats scale without wrapping
    val condensedFontFamily = remember { VariableFontProvider.getFontFamily(weight = 800, width = 75f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0C))
    ) {
        
        // ----------------------------------------------------
        // TOP 1/3: NON-INTERRUPTIVE SCANNABLE ZONE (ONE UI 6 ETHOS)
        // ----------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF151518), Color(0xFF0B0B0C))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Nav header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NEURALPULSE",
                            style = TextStyle(
                                fontFamily = condensedFontFamily,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        )
                        Text(
                            text = "Ecosystem Command",
                            style = TextStyle(
                                fontFamily = condensedFontFamily,
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                    
                    Surface(
                        color = when (connectionState) {
                            is HealthDataManager.ConnectionState.Connected -> Color(0xFF065F46)
                            else -> Color(0xFF27272A)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (connectionState is HealthDataManager.ConnectionState.Connected) "Store Online" else "Store Offline",
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Giant Dial Gauge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VULNERABILITY INDEX",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${risk.vulnerabilityIndex}",
                                style = TextStyle(
                                    fontFamily = condensedFontFamily,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        risk.vulnerabilityIndex >= 70 -> Color(0xFFF87171)
                                        risk.vulnerabilityIndex >= 40 -> Color(0xFFFBBF24)
                                        else -> Color(0xFF10B981)
                                    }
                                )
                            )
                            Text(
                                text = "/100",
                                style = TextStyle(
                                    fontFamily = condensedFontFamily,
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                ),
                                modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                            )
                        }
                    }
                    
                    // State description
                    Text(
                        text = risk.conditionRisk,
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(150.dp)
                    )
                }
            }
        }

        // ----------------------------------------------------
        // LOWER 2/3: COMFORT ACTIVE THUMB ZONE (SCROLLABLE INTERACTION)
        // ----------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2.5f)
                .background(Color(0xFF0B0B0C))
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (calendarBlocked) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Google Calendar Rest Slot Active: 90 minutes blocked to buffer metabolic exhaustions.",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // AI coach card
            NeuralPulseDataCard(title = "PREDICTIVE AI INTERVENTION") {
                Column {
                    Text(
                        text = risk.recommendedMicroIntervention,
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    if (risk.thermalNutritionalWarning != "Thermal baselines stable.") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = risk.thermalNutritionalWarning,
                            fontSize = 11.sp,
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // On-Device Explainability card (MediaPipe Text SLM Gemma engine)
            NeuralPulseDataCard(title = "ON-DEVICE AI EXPLAINABILITY") {
                Column {
                    Text(
                        text = onDeviceExplanation,
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Source: On-Device Gemma (INT8) | Active Input: ${risk.resolvedDeviceSource}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Gemini Chat bubble card
            NeuralPulseDataCard(title = "GEMINI APP-FUNCTIONS DIALOG") {
                Column {
                    if (geminiVoiceFeedback.isNotEmpty()) {
                        Text(
                            text = "\"$geminiVoiceFeedback\"",
                            fontSize = 12.sp,
                            color = Color.White,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = onQueryGemini,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Ask Gemini: recovery status", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            // MediaPipe zero-shutter food scanner UI
            NeuralPulseDataCard(title = "MEDIAPIPE AI NUTRITION VIEWFINDER") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Zero-Shutter Vision Pipeline",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = "Camera Icon",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Stylized viewfinder screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color.Black, shape = RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (scannedFood == null) {
                            Text(
                                text = "Zero-Shutter Viewfinder Standby\nPoint camera at nutrition item",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        } else {
                            // Instant-Add checklist overlay in zero-shutter
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFF0F172A),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Scanned: ${scannedFood.foodItemName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${scannedFood.baselineCalories} kcal | Confidence: ${String.format("%.0f%%", scannedFood.confidence * 100f)}",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                                
                                // Single-Tap Ingestion add checkmark button
                                Button(
                                    onClick = onClearFood,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "✓ Log", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    if (scannedFood != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            scannedFood.macronutrients.forEach { (macro, value) ->
                                Column {
                                    Text(text = macro, fontSize = 9.sp, color = Color.Gray)
                                    Text(text = "${value}g", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Glycemic Clearance Curve", fontSize = 11.sp, color = Color.LightGray)
                            Text(
                                text = "${risk.glycemicRiskPercent}% Risk",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (risk.glycemicRiskPercent >= 70) Color(0xFFF87171) else Color(0xFF10B981)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onTriggerFoodScan("avocado") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = "Avocado", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { onTriggerFoodScan("chicken") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = "Chicken", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { onTriggerFoodScan("pasta") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = "Pasta", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Sports gait dynamic card
            NeuralPulseDataCard(title = "SPORTS SCIENCE & GAIT DYNAMICS") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Biomechanical Correlation Matrix",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Rounded.Analytics,
                            contentDescription = "Analytics Icon",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricRow(label = "Ground Contact Time", value = "$gaitGct ms", highlight = Color.White)
                        MetricRow(label = "Vertical Oscillation", value = "${gaitOscillation} cm", highlight = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Left/Right Step Balance", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = "${gaitBalanceLeft}% L / ${100f - gaitBalanceLeft}% R",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gaitAsymmetry) Color(0xFFF87171) else Color(0xFF10B981)
                            )
                        }
                        Button(
                            onClick = onRunGaitTracking,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = "Simulate Gait Run", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }

            // Pulmonology report card
            NeuralPulseDataCard(title = "PULMONOLOGY & SLEEP APNEA LOGS") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Medical Report PDF Exporter",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = "PDF Icon",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (pulmonologyReport == null) {
                        Text(
                            text = "Analyze sleep apnea sequences across multi-night trajectories, exporting encrypted clinical files for pulmonology consultations.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    } else {
                        Text(
                            text = "Report: ${pulmonologyReport.fileName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Lowest SpO2 desaturation: ${pulmonologyReport.lowestOxygenSaturation}% | Events: ${pulmonologyReport.totalApneaEvents}",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onGenerateReport,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Export Pulmonologist Report", color = Color.White)
                    }
                }
            }

            // v1.1.0 telemetry summary card
            NeuralPulseDataCard(title = "V1.1.0 DATASTORE LOGS") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Security Privacy Sandbox Logs",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = "Shield Icon",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricRow(label = "Energy Score", value = "$energyScore", highlight = Color(0xFF10B981))
                        MetricRow(label = "Sleep Apnea", value = if (sleepApneaRecent) "Detected" else "Clear", highlight = if (sleepApneaRecent) Color(0xFFF87171) else Color.White)
                    }
                }
            }

            // Sensory stream card
            NeuralPulseDataCard(title = "WEAR OS BIO-STREAM TELEMETRY") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricRow(label = "EDA CONDUCTANCE", value = "${String.format("%.2f", liveEda)} uS", highlight = if (liveEda > 4f) Color(0xFFF87171) else Color.White)
                        MetricRow(label = "CELL HYDRATION", value = "${String.format("%.0f%%", liveHydration * 100f)}", highlight = Color(0xFF3B82F6))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricRow(label = "WATCH PPG SQI", value = if (watchPpgSqi == 1.0) "Clinical Grade" else "Motion Noise", highlight = if (watchPpgSqi == 1.0) Color(0xFF10B981) else Color(0xFFF87171))
                        MetricRow(label = "HEART RATE", value = "$heartRate BPM", highlight = Color.White)
                    }
                }
            }

            // Developer actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onSignalDegradedChange(!isSignalDegraded) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSignalDegraded) Color(0xFF7F1D1D) else Color(0xFF27272A))
                    ) {
                        Text(text = if (isSignalDegraded) "Wrist Shift Active" else "Signal Normal", fontSize = 10.sp, color = Color.White)
                    }

                    Button(
                        onClick = { onNocturnalChange(!isNocturnal) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isNocturnal) Color(0xFF065F46) else Color(0xFF27272A))
                    ) {
                        Text(text = if (isNocturnal) "Nocturnal: Ring" else "Active: Watch", fontSize = 10.sp, color = Color.White)
                    }
                }

                Button(
                    onClick = onTriggerConsent,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Connect Health SDK Store", color = Color.White)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onSimulateStress,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A))
                    ) {
                        Text(text = "Drift Stress", color = Color.White)
                    }
                    Button(
                        onClick = onResetSimulation,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A))
                    ) {
                        Text(text = "Reset Sync", color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MetricRow(label: String, value: String, highlight: Color) {
    Column {
        Text(text = label, fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.SansSerif)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = highlight, fontFamily = FontFamily.SansSerif)
    }
}
