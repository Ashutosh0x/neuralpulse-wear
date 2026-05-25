package com.alphahealth.monitor.wear.sensor

import android.util.Log
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * BioelectricalImpedanceAnalyzer (BIVA)
 *
 * Implements Multi-Frequency Bioelectrical Impedance Vector Analysis across
 * four standard clinical frequencies: 5 kHz, 50 kHz, 100 kHz, 250 kHz.
 *
 * Clinical principle:
 *   Low frequencies (5 kHz)  -> Current flows only through extracellular space
 *                               because intact cell membranes block ionic current.
 *   High frequencies (250 kHz) -> Current penetrates cell membranes and flows
 *                                 through both intracellular and extracellular water.
 *   Phase angle θ = arctan(Xc / R) — the primary BIVA marker. Validated across
 *   hundreds of clinical trials as a proxy for cellular hydration status and
 *   nutritional reserve.
 *
 * Thresholds (from published population-normative BIVA data):
 *   θ > 7.0° — Excellent cellular integrity (athletes, well-hydrated)
 *   θ 4.0–7.0° — Normal range
 *   θ < 4.0° — Elevated dehydration / reduced cell membrane integrity
 *   ECW/TBW > 0.60 — Clinically significant fluid overload or muscle oedema
 *
 * Reference: Kyle et al. "Body composition interpretation" (ESPEN, 2004);
 *            Norman et al. "Bioelectrical impedance vector analysis" (JPEN, 2012)
 *
 * Hardware: Samsung Health Sensor SDK MF_BIA measurement (Galaxy Watch 4+)
 *   SDK type: HealthTrackingEvent.BODY_COMPOSITION / MeasurementType.MF_BIA
 *   Result fields: resistance_at_each_frequency[], reactance_at_each_frequency[]
 */
class BioelectricalImpedanceAnalyzer {

    private val TAG = "BIVA"

    companion object {
        // Standard BIVA frequency sweep (Hz)
        val FREQUENCY_HZ = floatArrayOf(5_000f, 50_000f, 100_000f, 250_000f)

        // ECW/TBW ratio threshold for clinically significant fluid shift detection
        const val ECW_OVERLOAD_THRESHOLD = 0.60f

        // Phase angle dehydration threshold (degrees)
        const val PHASE_ANGLE_LOW_THRESHOLD = 4.0f
        const val PHASE_ANGLE_OPTIMAL_THRESHOLD = 7.0f
    }

    /**
     * Analyzes raw resistance and reactance vectors from a 4-frequency MF-BIA sweep.
     *
     * @param resistances  Array of 4 resistance values (Ohms) at [5k, 50k, 100k, 250k] Hz
     * @param reactances   Array of 4 reactance values (Ohms) at [5k, 50k, 100k, 250k] Hz
     * @param heightCm     Subject height in centimeters (required for TBW prediction equation)
     * @param weightKg     Subject weight in kilograms
     * @return BivaResult containing phase angle, fluid compartment estimates, and hydration status
     */
    fun analyze(
        resistances: FloatArray,
        reactances: FloatArray,
        heightCm: Float = 175f,   // Default: average adult height used if not configured
        weightKg: Float = 75f
    ): BivaResult {
        require(resistances.size == 4 && reactances.size == 4) {
            "MF-BIA requires exactly 4 frequency measurements"
        }

        // Phase angle at 50 kHz is the clinically standard BIVA reference frequency
        val r50  = resistances[1]
        val xc50 = reactances[1]
        val phaseAngleRad = atan((xc50 / r50).toDouble())
        val phaseAngleDeg = Math.toDegrees(phaseAngleRad).toFloat()

        // Body impedance index (BII) — height² / resistance at 50 kHz
        // Used in Lukaski (1986) and Deurenberg (1994) TBW prediction equations
        val bii = (heightCm * heightCm) / r50

        // Total Body Water estimation (Lukaski equation, validated for athletes)
        // TBW (L) = 0.377 × (H²/R50) + 0.14 × Weight - 0.08 × Gender - 2.9
        // Simplified (gender-neutral approximation):
        val tbwLitres = 0.377f * bii + 0.14f * weightKg - 2.9f

        // Extracellular Water from low-frequency (5 kHz) impedance
        // At 5 kHz, R reflects only extracellular impedance
        val ecwLitres = 0.306f * ((heightCm * heightCm) / resistances[0])

        // Intracellular Water = TBW - ECW
        val icwLitres = (tbwLitres - ecwLitres).coerceAtLeast(0f)

        val ecwTbwRatio = if (tbwLitres > 0f) ecwLitres / tbwLitres else 0f

        // Impedance vector magnitude |Z| at 50 kHz — used for BIVA ellipse plotting
        val impedanceMagnitude = sqrt(r50.pow(2) + xc50.pow(2))

        val hydrationStatus = when {
            ecwTbwRatio > ECW_OVERLOAD_THRESHOLD -> HydrationStatus.FLUID_OVERLOAD
            phaseAngleDeg < PHASE_ANGLE_LOW_THRESHOLD -> HydrationStatus.DEHYDRATED
            phaseAngleDeg > PHASE_ANGLE_OPTIMAL_THRESHOLD -> HydrationStatus.OPTIMAL
            else -> HydrationStatus.NORMAL
        }

        Log.i(TAG, "BIVA result: θ=${String.format("%.2f", phaseAngleDeg)}° " +
                "TBW=${String.format("%.1f", tbwLitres)}L " +
                "ECW=${String.format("%.1f", ecwLitres)}L " +
                "ICW=${String.format("%.1f", icwLitres)}L " +
                "ECW/TBW=${String.format("%.3f", ecwTbwRatio)} " +
                "Status=$hydrationStatus")

        return BivaResult(
            phaseAngleDeg = phaseAngleDeg,
            impedanceMagnitude = impedanceMagnitude,
            totalBodyWaterLitres = tbwLitres,
            extracellularWaterLitres = ecwLitres,
            intracellularWaterLitres = icwLitres,
            ecwTbwRatio = ecwTbwRatio,
            hydrationStatus = hydrationStatus
        )
    }

    /**
     * Generates a simulated MF-BIA sweep result for development/testing without
     * Samsung hardware. Values approximate a moderately dehydrated 75 kg adult.
     */
    fun simulateMfBiaSweep(dehydrationLevel: Float = 0.0f): BivaResult {
        // Dehydration increases R and decreases Xc (cell membrane capacitance drops)
        val r50Simulated  = 480f + (dehydrationLevel * 80f)   // Ohms
        val xc50Simulated = 62f  - (dehydrationLevel * 15f)   // Ohms
        return analyze(
            resistances = floatArrayOf(520f + dehydrationLevel * 90f, r50Simulated, 445f, 410f),
            reactances  = floatArrayOf(45f,  xc50Simulated, 68f, 55f)
        )
    }
}

/**
 * BIVA analysis result containing all fluid compartment measurements.
 */
data class BivaResult(
    val phaseAngleDeg: Float,           // θ = arctan(Xc/R) at 50 kHz — primary BIVA marker
    val impedanceMagnitude: Float,       // |Z| = sqrt(R² + Xc²) at 50 kHz
    val totalBodyWaterLitres: Float,     // TBW via Lukaski equation
    val extracellularWaterLitres: Float, // ECW from 5 kHz impedance
    val intracellularWaterLitres: Float, // ICW = TBW - ECW
    val ecwTbwRatio: Float,             // ECW/TBW — key overhydration marker (normal ~0.46)
    val hydrationStatus: HydrationStatus
)

enum class HydrationStatus {
    OPTIMAL,        // θ > 7.0° and ECW/TBW in normal range
    NORMAL,         // θ 4.0–7.0° — standard hydration
    DEHYDRATED,     // θ < 4.0° — reduced intracellular water
    FLUID_OVERLOAD  // ECW/TBW > 0.60 — clinically significant extracellular excess
}
