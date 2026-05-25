package com.alphahealth.monitor.dashboard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Menu
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
            var darkTheme by remember { mutableStateOf(true) }
            
            NeuralPulseTheme(darkTheme = darkTheme) {
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
                        isSignalDegraded = false
                        isNocturnal = false
                        ringEda = 1.2f
                        ringHydration = 0.68f
                    },
                    onSimulateStress = {
                        liveEda = 5.2f
                        liveHydration = 0.56f
                        heartRate = 96
                        energyScore = 48
                        sleepApneaRecent = true
                    },
                    darkTheme = darkTheme,
                    onThemeToggle = { darkTheme = !darkTheme }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        explainEngine.close()
    }
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
    onResetSimulation: () -> Unit,
    onSimulateStress: () -> Unit,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    val condensedFontFamily = remember { VariableFontProvider.getFontFamily(weight = 800, width = 75f) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    AlphaAdminNavigationDrawer(
        drawerState = drawerState,
        onExportFhir = {
            onGenerateReport()
        },
        onComplianceCheck = {
            activeTab = 5
        }
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NEURALPULSE",
                                style = TextStyle(
                                    fontFamily = condensedFontFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            )
                            Text(
                                text = "Ecosystem Command",
                                style = TextStyle(
                                    fontFamily = condensedFontFamily,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onThemeToggle) {
                                Icon(
                                    imageVector = if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                    contentDescription = "Theme Toggle",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Outlined.Menu,
                                    contentDescription = "Open Administrative Settings",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                color = when (connectionState) {
                                    is HealthDataManager.ConnectionState.Connected -> AlphaMintGreen
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
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
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Outlined.Analytics, contentDescription = "Dashboard Hub") },
                    label = { Text("Command", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AlphaAccentBlue,
                        selectedTextColor = AlphaAccentBlue
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = "Diet Vision") },
                    label = { Text("Vision", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AlphaAccentBlue,
                        selectedTextColor = AlphaAccentBlue
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Outlined.HistoryEdu, contentDescription = "Clinical Vault") },
                    label = { Text("Vault", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AlphaAccentBlue,
                        selectedTextColor = AlphaAccentBlue
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Outlined.SettingsInputAntenna, contentDescription = "Automation Map") },
                    label = { Text("IoT", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AlphaAccentBlue,
                        selectedTextColor = AlphaAccentBlue
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Outlined.DirectionsRun, contentDescription = "Gait Tracker") },
                    label = { Text("Biometrics", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AlphaAccentBlue,
                        selectedTextColor = AlphaAccentBlue
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 5,
                    onClick = { activeTab = 5 },
                    icon = { Icon(Icons.Outlined.Shield, contentDescription = "Identity Profile") },
                    label = { Text("Profile", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AlphaAccentBlue,
                        selectedTextColor = AlphaAccentBlue
                    )
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (activeTab) {
                0 -> EcosystemCommandTab(
                    risk = risk,
                    liveEda = liveEda,
                    liveHydration = liveHydration,
                    heartRate = heartRate,
                    watchPpgSqi = watchPpgSqi,
                    sleepApneaRecent = sleepApneaRecent,
                    energyScore = energyScore,
                    calendarBlocked = calendarBlocked,
                    onDeviceExplanation = onDeviceExplanation,
                    isSignalDegraded = isSignalDegraded,
                    onSignalDegradedChange = onSignalDegradedChange,
                    isNocturnal = isNocturnal,
                    onNocturnalChange = onNocturnalChange,
                    ringEda = ringEda,
                    onRingEdaChange = onRingEdaChange,
                    ringHydration = ringHydration,
                    onRingHydrationChange = onRingHydrationChange,
                    onTriggerConsent = onTriggerConsent,
                    onSimulateStress = onSimulateStress,
                    onResetSimulation = onResetSimulation
                )
                1 -> AiVisionTab(
                    scannedFood = scannedFood,
                    glycemicRiskPercent = risk.glycemicRiskPercent,
                    onTriggerFoodScan = onTriggerFoodScan,
                    onClearFood = onClearFood
                )
                2 -> ClinicalVaultTab(
                    pulmonologyReport = pulmonologyReport,
                    sleepApneaRecent = sleepApneaRecent,
                    heartRate = heartRate,
                    onGenerateReport = onGenerateReport
                )
                3 -> AmbientIoTTab(
                    isNocturnal = isNocturnal,
                    ringEda = ringEda,
                    ringHydration = ringHydration,
                    liveEda = liveEda,
                    liveHydration = liveHydration,
                    onRingEdaChange = onRingEdaChange,
                    onRingHydrationChange = onRingHydrationChange
                )
                4 -> BiomechanicalTab(
                    gaitGct = gaitGct,
                    gaitOscillation = gaitOscillation,
                    gaitAsymmetry = gaitAsymmetry,
                    gaitBalanceLeft = gaitBalanceLeft,
                    onRunGaitTracking = onRunGaitTracking
                )
                5 -> ProfileVaultTab()
            }
        }
    }
}
}
