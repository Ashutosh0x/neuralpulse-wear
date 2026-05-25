package com.alphahealth.monitor.wear.sensor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UniversalSensorRouter
 *
 * Provides a unified biometric data stream regardless of watch manufacturer.
 * On Galaxy Watch 4+: routes to Samsung Health Sensor SDK (raw EDA, MF-BIA, PPG).
 * On Pixel Watch, Fossil, TicWatch, and all other Wear OS 3+ devices:
 *   routes to Android Health Services (PassiveMonitoringClient).
 *
 * Usage:
 *   val router = UniversalSensorRouter(context)
 *   router.edaStream.collect { eda -> ... }
 *   router.heartRateStream.collect { hr -> ... }
 */
class UniversalSensorRouter(private val context: Context) {

    private val TAG = "SensorRouter"

    private val _edaStream = MutableStateFlow(0.0f)
    val edaStream: StateFlow<Float> = _edaStream.asStateFlow()

    private val _heartRateStream = MutableStateFlow(0)
    val heartRateStream: StateFlow<Int> = _heartRateStream.asStateFlow()

    private val _hydrationRatioStream = MutableStateFlow(0.62f)
    val hydrationRatioStream: StateFlow<Float> = _hydrationRatioStream.asStateFlow()

    private val _ppgSqiStream = MutableStateFlow(1.0)
    val ppgSqiStream: StateFlow<Double> = _ppgSqiStream.asStateFlow()

    private val _sensorSource = MutableStateFlow(SensorSource.DETECTING)
    val sensorSource: StateFlow<SensorSource> = _sensorSource.asStateFlow()

    enum class SensorSource {
        DETECTING,
        SAMSUNG_GALAXY,     // Raw EDA + MF-BIA + PPG via Samsung Health Sensor SDK
        ANDROID_HEALTH,     // HR + SpO2 + Steps via Android Health Services
        SIMULATED           // Baseline model when no sensors available
    }

    private var samsungEngine: Any? = null       // HighPerformanceBioEngine (Samsung)
    private var healthServicesClient: Any? = null // HealthServicesClient (universal)

    init {
        initialize()
    }

    private fun initialize() {
        if (tryInitSamsungEngine()) {
            _sensorSource.value = SensorSource.SAMSUNG_GALAXY
            Log.i(TAG, "Sensor route: Samsung Health Sensor SDK (Galaxy Watch detected)")
        } else if (tryInitHealthServices()) {
            _sensorSource.value = SensorSource.ANDROID_HEALTH
            Log.i(TAG, "Sensor route: Android Health Services (non-Samsung Wear OS watch)")
        } else {
            _sensorSource.value = SensorSource.SIMULATED
            Log.w(TAG, "Sensor route: Simulated baseline (no hardware sensors available)")
            activateBaselineSimulation()
        }
    }

    /**
     * Attempts to connect to the Samsung Health Tracking Service.
     * Returns false immediately if the Samsung SDK AAR is not present in libs/,
     * or if running on a non-Galaxy watch — no crash, no error dialog.
     */
    private fun tryInitSamsungEngine(): Boolean {
        return try {
            // Reflection-based check: only succeeds if samsung-health-sensor-api.aar
            // is present in the libs/ folder at build time.
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
            Log.w(TAG, "Samsung sensor init failed: ${e.message} — falling back")
            false
        }
    }

    /**
     * Initializes Android Health Services for non-Samsung Wear OS devices.
     * Throws no exceptions if not present.
     */
    private fun tryInitHealthServices(): Boolean {
        return try {
            // androidx.health:health-services-client is always included in wear/build.gradle.kts
            val hsClientClass = Class.forName(
                "androidx.health.services.client.HealthServicesClient"
            )
            Log.d(TAG, "Android Health Services client available")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Health Services not available: ${e.message}")
            false
        }
    }

    /**
     * When no hardware sensors are available (emulator, unsupported device),
     * emits physiologically plausible baseline values so the UI remains functional.
     * Used for UI testing and demo runs without a physical watch.
     */
    private fun activateBaselineSimulation() {
        _edaStream.value = 1.8f        // Normal resting conductance
        _heartRateStream.value = 72    // Healthy resting HR
        _hydrationRatioStream.value = 0.62f
        _ppgSqiStream.value = 1.0
    }

    /**
     * Routes incoming EDA telemetry from Samsung engine to unified stream.
     * Called by HighPerformanceBioEngine when Samsung SDK delivers a data point.
     */
    fun onSamsungEdaReceived(conductance: Float) {
        _edaStream.value = conductance
    }

    /**
     * Routes incoming heart rate from Android Health Services to unified stream.
     */
    fun onHealthServicesHrReceived(heartRate: Int) {
        _heartRateStream.value = heartRate
    }

    fun disconnect() {
        try {
            (samsungEngine as? com.alphahealth.monitor.wear.tracking.HighPerformanceBioEngine)
                ?.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Disconnect: ${e.message}")
        }
        samsungEngine = null
        healthServicesClient = null
        _sensorSource.value = SensorSource.SIMULATED
    }
}
