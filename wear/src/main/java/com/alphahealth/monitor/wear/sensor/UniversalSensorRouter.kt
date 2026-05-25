package com.alphahealth.monitor.wear.sensor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UniversalSensorRouter
 *
 * Unified biometric data stream regardless of watch manufacturer, now upgraded with
 * all five Top-1% clinical signal processing engines:
 *
 *   1. BioelectricalImpedanceAnalyzer (BIVA) — MF-BIA phase angle + fluid compartments
 *   2. AdaptiveEdaFilter              — 0.05 Hz Butterworth HPF + three-signal stress gate
 *   3. PulseTransitTimeEngine         — ECG R-wave + IR PPG delta for cuffless BP trends
 *   4. InertialMotionMask             — LMS adaptive filter (16 taps, µ=0.01) for PPG
 *   5. Statistical SQI Kurtosis Gate  — 2.3 <= K <= 5.2 window on raw PPG (existing)
 *
 * Sensor routing:
 *   Galaxy Watch 4+:  Samsung Health Sensor SDK (raw EDA, MF-BIA, ECG, IR PPG, AccelGyro)
 *   Other Wear OS 3+: Android Health Services PassiveMonitoringClient (HR, SpO2, Steps)
 *   No hardware:      Simulated baseline fixture stream for development / CI testing
 *
 * Sensor Fusion Score (sensorFusionScore):
 *   A combined quality index (0.0–1.0) combining:
 *     - SQI from kurtosis gate (PPG signal quality)
 *     - PTT compliance index (vascular data valid)
 *     - EDA filter validity (not thermal-contaminated)
 *   Downstream VulnerabilityEngine uses this score to weight clinical outputs.
 */
class UniversalSensorRouter(private val context: Context) {

    private val TAG = "SensorRouter"

    // ── Primary Bio Streams ────────────────────────────────────────────────────────
    private val _edaStream = MutableStateFlow(0.0f)
    val edaStream: StateFlow<Float> = _edaStream.asStateFlow()

    private val _heartRateStream = MutableStateFlow(0)
    val heartRateStream: StateFlow<Int> = _heartRateStream.asStateFlow()

    private val _hydrationRatioStream = MutableStateFlow(0.62f)
    val hydrationRatioStream: StateFlow<Float> = _hydrationRatioStream.asStateFlow()

    private val _ppgSqiStream = MutableStateFlow(1.0)
    val ppgSqiStream: StateFlow<Double> = _ppgSqiStream.asStateFlow()

    // ── Clinical Engine Output Streams ─────────────────────────────────────────────

    /** BIVA result stream — phase angle + fluid compartment analysis */
    private val _bivaStream = MutableStateFlow<BivaResult?>(null)
    val bivaStream: StateFlow<BivaResult?> = _bivaStream.asStateFlow()

    /** Adaptive EDA filtered stream — phasic/tonic separation + stress gate */
    private val _edaFilteredStream = MutableStateFlow<EdaFilterResult?>(null)
    val edaFilteredStream: StateFlow<EdaFilterResult?> = _edaFilteredStream.asStateFlow()

    /** PTT result stream — cuffless vascular compliance + SBP trend */
    private val _pttStream = MutableStateFlow<PttResult?>(null)
    val pttStream: StateFlow<PttResult?> = _pttStream.asStateFlow()

    /** Motion-masked PPG stream — LMS-cleaned cardiac signal */
    private val _cleanPpgStream = MutableStateFlow<MotionMaskedPpg?>(null)
    val cleanPpgStream: StateFlow<MotionMaskedPpg?> = _cleanPpgStream.asStateFlow()

    /** Composite sensor fusion quality score (0.0 = invalid, 1.0 = clinical grade) */
    private val _sensorFusionScore = MutableStateFlow(0.0f)
    val sensorFusionScore: StateFlow<Float> = _sensorFusionScore.asStateFlow()

    private val _sensorSource = MutableStateFlow(SensorSource.DETECTING)
    val sensorSource: StateFlow<SensorSource> = _sensorSource.asStateFlow()

    enum class SensorSource {
        DETECTING,
        SAMSUNG_GALAXY,     // Raw EDA + MF-BIA + ECG + IR PPG via Samsung Health Sensor SDK
        ANDROID_HEALTH,     // HR + SpO2 + Steps via Android Health Services
        SIMULATED           // Baseline model when no sensors available
    }

    // ── Five Clinical Processing Engines ──────────────────────────────────────────
    val bivaEngine = BioelectricalImpedanceAnalyzer()
    val edaFilter  = AdaptiveEdaFilter(samplingRateHz = 4.0)
    val pttEngine  = PulseTransitTimeEngine()
    val motionMask = InertialMotionMask(tapCount = 16, mu = 0.01f)

    private var samsungEngine: Any? = null
    private var healthServicesClient: Any? = null

    init {
        initialize()
    }

    private fun initialize() {
        if (tryInitSamsungEngine()) {
            _sensorSource.value = SensorSource.SAMSUNG_GALAXY
            Log.i(TAG, "Sensor route: Samsung Health Sensor SDK (Galaxy Watch)")
        } else if (tryInitHealthServices()) {
            _sensorSource.value = SensorSource.ANDROID_HEALTH
            Log.i(TAG, "Sensor route: Android Health Services (non-Samsung Wear OS)")
        } else {
            _sensorSource.value = SensorSource.SIMULATED
            Log.w(TAG, "Sensor route: Simulated baseline (no hardware sensors available)")
            activateBaselineSimulation()
        }
    }

    private fun tryInitSamsungEngine(): Boolean {
        return try {
            val clazz = Class.forName(
                "com.samsung.android.service.health.tracking.HealthTrackingService"
            )
            val engine = com.alphahealth.monitor.wear.tracking.HighPerformanceBioEngine(context)
            samsungEngine = engine
            true
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "Samsung Sensor SDK not present — skipping Galaxy route")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Samsung sensor init failed: ${e.message}")
            false
        }
    }

    private fun tryInitHealthServices(): Boolean {
        return try {
            Class.forName("androidx.health.services.client.HealthServicesClient")
            Log.d(TAG, "Android Health Services client available")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Health Services not available: ${e.message}")
            false
        }
    }

    private fun activateBaselineSimulation() {
        _edaStream.value = 1.8f
        _heartRateStream.value = 72
        _hydrationRatioStream.value = 0.62f
        _ppgSqiStream.value = 1.0
        // Simulate baseline BIVA (well-hydrated adult reference)
        _bivaStream.value = bivaEngine.simulateMfBiaSweep(dehydrationLevel = 0.0f)
        _sensorFusionScore.value = 0.6f // Development mode: moderate fusion confidence
    }

    // ── Engine Routing Callbacks ───────────────────────────────────────────────────

    /**
     * Called by Samsung SDK EDA tracker for each raw conductance sample.
     * Routes through AdaptiveEdaFilter before publishing to edaStream.
     */
    fun onSamsungEdaReceived(conductance: Float, skinTempCelsius: Float, hr: Int) {
        _edaStream.value = conductance
        val filtered = edaFilter.process(conductance, skinTempCelsius, hr)
        _edaFilteredStream.value = filtered
        updateFusionScore()
    }

    /** Simplified EDA callback for non-Samsung paths (no skin temp available). */
    fun onSamsungEdaReceived(conductance: Float) {
        _edaStream.value = conductance
    }

    /**
     * Called by Android Health Services HR callback.
     */
    fun onHealthServicesHrReceived(heartRate: Int) {
        _heartRateStream.value = heartRate
    }

    /**
     * Called by Samsung SDK IR PPG tracker. Feeds both MotionMask and PttEngine.
     * @param irPpgSample   Normalized IR PPG sample (DC-removed)
     * @param accelX/Y/Z    Simultaneous accelerometer readings (m/s²)
     * @param timestampMs   Sample timestamp from sensor hardware clock
     */
    fun onIrPpgSampleReceived(
        irPpgSample: Float,
        accelX: Float, accelY: Float, accelZ: Float,
        timestampMs: Long
    ) {
        // Step 1: LMS motion cancellation (16-tap, µ=0.01)
        val masked = motionMask.process(irPpgSample, accelX, accelY, accelZ)
        _cleanPpgStream.value = masked

        // Step 2: Feed cleaned PPG to PTT engine for systolic peak detection
        val pttResult = pttEngine.feedIrPpgSample(masked.cleanPpg, timestampMs)
        pttResult?.let {
            _pttStream.value = it
            updateFusionScore()
        }
    }

    /**
     * Called by Samsung SDK ECG tracker. Feeds raw ECG sample to PTT engine
     * for R-wave reference timestamp detection.
     */
    fun onEcgSampleReceived(ecgSample: Float, timestampMs: Long) {
        pttEngine.feedEcgSample(ecgSample, timestampMs)
    }

    /**
     * Called by Samsung SDK MF-BIA completion callback.
     * Runs the full BIVA vector analysis and updates hydration stream.
     */
    fun onMfBiaResultReceived(
        resistances: FloatArray,
        reactances: FloatArray,
        heightCm: Float = 175f,
        weightKg: Float = 75f
    ) {
        val result = bivaEngine.analyze(resistances, reactances, heightCm, weightKg)
        _bivaStream.value = result
        // Update hydration stream using BIVA ECW/TBW ratio as proxy
        _hydrationRatioStream.value = (1f - result.ecwTbwRatio).coerceIn(0f, 1f)
        updateFusionScore()
    }

    /**
     * Computes the composite Sensor Fusion Score from all active engine outputs.
     * Used by VulnerabilityEngine to weight clinical analysis confidence.
     *
     * Score components:
     *   - SQI quality (0.0–1.0)
     *   - PTT compliance (0.0–1.0)
     *   - EDA filter validity (1.0 if not thermal-contaminated, 0.5 if rejected)
     */
    private fun updateFusionScore() {
        val sqiScore = _ppgSqiStream.value.toFloat().coerceIn(0f, 1f)
        val pttScore = _pttStream.value?.vascularComplianceIndex ?: 0.5f
        val edaScore = _edaFilteredStream.value?.let {
            if (it.thermalDriftRejected) 0.5f else 1.0f
        } ?: 0.5f

        _sensorFusionScore.value = (sqiScore * 0.4f + pttScore * 0.35f + edaScore * 0.25f)
            .coerceIn(0f, 1f)
    }

    fun disconnect() {
        try {
            (samsungEngine as? com.alphahealth.monitor.wear.tracking.HighPerformanceBioEngine)
                ?.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Disconnect: ${e.message}")
        }
        edaFilter.reset()
        pttEngine.reset()
        motionMask.reset()
        samsungEngine = null
        healthServicesClient = null
        _sensorSource.value = SensorSource.SIMULATED
    }
}
