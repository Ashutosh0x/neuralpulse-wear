package com.alphahealth.monitor.wear.sensor

import android.util.Log
import kotlin.math.abs
import kotlin.math.sign

/**
 * PulseTransitTimeEngine
 *
 * Measures Pulse Transit Time (PTT) — the millisecond delay between:
 *   t1: Electrical R-wave peak from the ECG electrode on the watch chassis
 *   t2: Mechanical pulse wave arrival at the capillary bed, detected by IR PPG sensor
 *
 *   PTT = t2 - t1 (milliseconds)
 *
 * Clinical significance:
 *   PTT is inversely related to pulse wave velocity (PWV). PWV rises when arterial walls
 *   stiffen (arteriosclerosis, hypertension, high cardiovascular load), shortening PTT.
 *   Healthy resting PTT range: 100–400 ms.
 *   Decreasing PTT over training sessions = increased vascular stiffness.
 *   Increasing PTT = vasodilation / recovery / reduced afterload.
 *
 * No inflatable cuff required — PTT provides a continuous, cuffless, relative blood
 * pressure trend that correlates with intra-arterial systolic BP changes at r = 0.85
 * in multiple clinical validation studies.
 *
 * References:
 *   Mukkamala et al. (2015) "Towards ubiquitous blood pressure monitoring via
 *   pulse transit time" IEEE Trans Biomed Eng.
 *   Sharma et al. (2017) "Cuffless and continuous blood pressure" sensors review.
 *
 * Algorithm:
 *   R-wave detection: Pan-Tompkins simplified (threshold-adaptive gradient max)
 *   PPG peak detection: Sign-change gradient peak finder on IR channel (940 nm)
 *   PTT averaging: Running median over the last 5 beats (noise-robust estimator)
 *
 * Hardware requirements:
 *   Samsung Health Sensor SDK — On-Demand ECG (requires user consent)
 *   Samsung Health Sensor SDK — Continuous IR PPG (Green/IR multi-wavelength)
 */
class PulseTransitTimeEngine {

    private val TAG = "PttEngine"

    companion object {
        const val PTT_MIN_VALID_MS = 80L    // Below 80ms = likely sensor artifact
        const val PTT_MAX_VALID_MS = 500L   // Above 500ms = motion/detection failure
        const val MEDIAN_WINDOW = 5         // Running median over last 5 PTT beats
        const val SBP_SLOPE = -0.85f        // dBP/dPTT: estimated -0.85 mmHg/ms (population mean)
        const val SBP_REFERENCE_PTT = 250f  // Reference PTT at calibration SBP
        const val SBP_REFERENCE_VALUE = 120f// Assumed calibration systolic BP (mmHg)
    }

    // Sliding window for median PTT estimation
    private val pttHistory = ArrayDeque<Long>()

    // ECG R-wave detection state (Pan-Tompkins simplified)
    private var ecgPrevSample = 0f
    private var ecgGradPrev = 0f
    private var ecgPeakThreshold = 0.4f      // Adaptive threshold (fraction of recent peak)
    private var ecgRecentPeakAmplitude = 1f
    private var lastRWaveTimestampMs = -1L
    private var ecgRefractoryMs = 250L       // Minimum physiological RR interval at 240 BPM

    // IR PPG peak detection state
    private var ppgPrevSample = 0f
    private var ppgGradPrev = 0f
    private var lastPpgPeakTimestampMs = -1L
    private var ppgRefractoryMs = 250L

    /**
     * Feeds a single ECG sample. Detects R-waves using adaptive threshold gradient.
     *
     * @param ecgSample   Normalized ECG amplitude (mV, range ~-1.5 to +1.5)
     * @param timestampMs System.currentTimeMillis() at sample capture
     */
    fun feedEcgSample(ecgSample: Float, timestampMs: Long) {
        val gradient = ecgSample - ecgPrevSample
        ecgPrevSample = ecgSample

        // R-wave: large positive gradient followed by negative gradient (peak crossing)
        val isRWaveCandidate = ecgGradPrev > 0f &&
                gradient < 0f &&
                ecgPrevSample > ecgPeakThreshold &&
                (lastRWaveTimestampMs < 0 || timestampMs - lastRWaveTimestampMs > ecgRefractoryMs)

        if (isRWaveCandidate) {
            lastRWaveTimestampMs = timestampMs
            // Adaptive threshold: 70% of recent peak amplitude (standard Pan-Tompkins)
            ecgRecentPeakAmplitude = ecgPrevSample
            ecgPeakThreshold = ecgRecentPeakAmplitude * 0.70f
            Log.v(TAG, "R-wave detected at ${timestampMs}ms amplitude=${ecgPrevSample}")
        }
        ecgGradPrev = gradient
    }

    /**
     * Feeds a single IR PPG sample (940 nm infrared channel).
     * Detects the systolic peak — the mechanical pulse wave arrival time.
     *
     * @param irPpgSample  Normalized IR PPG amplitude (arbitrary sensor units, DC-removed)
     * @param timestampMs  System.currentTimeMillis() at sample capture
     * @return PttResult if a valid beat-pair PTT was computed, null otherwise
     */
    fun feedIrPpgSample(irPpgSample: Float, timestampMs: Long): PttResult? {
        val gradient = irPpgSample - ppgPrevSample
        ppgPrevSample = irPpgSample

        // PPG systolic peak: gradient sign change from positive to negative
        val isPpgPeak = ppgGradPrev > 0f &&
                gradient <= 0f &&
                (lastPpgPeakTimestampMs < 0 || timestampMs - lastPpgPeakTimestampMs > ppgRefractoryMs)

        ppgGradPrev = gradient

        if (isPpgPeak) {
            lastPpgPeakTimestampMs = timestampMs

            // Only compute PTT if we have a valid R-wave reference within 600ms
            if (lastRWaveTimestampMs > 0L &&
                (timestampMs - lastRWaveTimestampMs) in PTT_MIN_VALID_MS..PTT_MAX_VALID_MS
            ) {
                val rawPtt = timestampMs - lastRWaveTimestampMs
                return computeResult(rawPtt, timestampMs)
            }
        }
        return null
    }

    private fun computeResult(rawPttMs: Long, timestampMs: Long): PttResult {
        // Update running median window
        pttHistory.add(rawPttMs)
        if (pttHistory.size > MEDIAN_WINDOW) pttHistory.removeAt(0)

        // Running median PTT (more robust than mean against outlier beats)
        val sortedPtt = pttHistory.sorted()
        val medianPttMs = sortedPtt[sortedPtt.size / 2].toFloat()

        // Vascular compliance index: higher PTT = more compliant arteries = lower stiffness
        // Normalized to 0.0 (stiff) – 1.0 (highly compliant) using population bounds
        val vascularComplianceIndex = ((medianPttMs - PTT_MIN_VALID_MS) /
                (PTT_MAX_VALID_MS - PTT_MIN_VALID_MS).toFloat()).coerceIn(0f, 1f)

        // Relative SBP trend estimate (not absolute — requires calibration session)
        // dSBP = SBP_SLOPE * (PTT - PTT_ref) + SBP_ref
        val estimatedSbpTrend = SBP_REFERENCE_VALUE + SBP_SLOPE * (medianPttMs - SBP_REFERENCE_PTT)

        val stiffnessCategory = when {
            medianPttMs < 150f -> VascularStiffness.ELEVATED    // Short PTT = stiff arteries
            medianPttMs < 280f -> VascularStiffness.NORMAL
            else               -> VascularStiffness.COMPLIANT   // Long PTT = flexible arteries
        }

        Log.i(TAG, "PTT: raw=${rawPttMs}ms median=${medianPttMs}ms " +
                "compliance=${String.format("%.2f", vascularComplianceIndex)} " +
                "eSBP=${String.format("%.1f", estimatedSbpTrend)}mmHg $stiffnessCategory")

        return PttResult(
            rawPttMs = rawPttMs,
            medianPttMs = medianPttMs.toLong(),
            vascularComplianceIndex = vascularComplianceIndex,
            estimatedSbpTrendMmhg = estimatedSbpTrend,
            stiffnessCategory = stiffnessCategory,
            timestampMs = timestampMs
        )
    }

    fun reset() {
        pttHistory.clear()
        lastRWaveTimestampMs = -1L
        lastPpgPeakTimestampMs = -1L
        ecgPeakThreshold = 0.4f
        ecgGradPrev = 0f; ecgPrevSample = 0f
        ppgGradPrev = 0f; ppgPrevSample = 0f
    }
}

data class PttResult(
    val rawPttMs: Long,                    // Single-beat PTT measurement
    val medianPttMs: Long,                 // 5-beat running median (noise-robust)
    val vascularComplianceIndex: Float,    // 0.0 (stiff) to 1.0 (compliant)
    val estimatedSbpTrendMmhg: Float,      // Relative SBP trend estimate (not absolute)
    val stiffnessCategory: VascularStiffness,
    val timestampMs: Long
)

enum class VascularStiffness {
    ELEVATED,   // PTT < 150ms — arterial stiffness, high cardiovascular load
    NORMAL,     // PTT 150–280ms — healthy vascular response
    COMPLIANT   // PTT > 280ms — vasodilation, good recovery state
}
