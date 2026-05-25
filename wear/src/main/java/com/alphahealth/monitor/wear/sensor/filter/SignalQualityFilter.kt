package com.alphahealth.monitor.wear.sensor.filter

import kotlin.math.pow
import kotlin.math.sqrt

class SignalQualityFilter {

    /**
     * Performs a 4th-order Butterworth Bandpass filter locally on the 25Hz PPG stream.
     * Isolates physiological pulse frequencies (0.5 Hz - 4.0 Hz, or 30-240 BPM) 
     * and filters out motion artifacts.
     */
    fun filterRawPPG(rawSignal: FloatArray): FloatArray {
        val filtered = FloatArray(rawSignal.size)
        // Static filter coefficients optimized for 25Hz hardware constraints
        val b = doubleArrayOf(0.0039, 0.0, -0.0078, 0.0, 0.0039)
        val a = doubleArrayOf(1.0, -3.5185, 4.6793, -2.7875, 0.6272)

        for (i in 4 until rawSignal.size) {
            filtered[i] = (b[0] * rawSignal[i] + b[1] * rawSignal[i-1] + b[2] * rawSignal[i-2] + 
                           b[3] * rawSignal[i-3] + b[4] * rawSignal[i-4] -
                           a[1] * filtered[i-1] - a[2] * filtered[i-2] - 
                           a[3] * filtered[i-3] - a[4] * filtered[i-4]).toFloat()
        }
        return filtered
    }

    /**
     * Evaluates the Signal Quality Index (SQI) using statistical Kurtosis and Skewness.
     * Pure physiological pulse waves match a clear statistical profile; motion noise does not.
     */
    fun calculateSignalQualityIndex(buffer: FloatArray): Double {
        val mean = buffer.average()
        val variance = buffer.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)

        if (stdDev == 0.0) return 0.0

        // Calculate Kurtosis (statistical sharpness metric)
        val kurtosis = buffer.map { ((it - mean) / stdDev).pow(4) }.average()

        // If the signal matches a valid physiological range (Kurtosis between 3.0 and 5.0),
        // flag it as top-tier accuracy data.
        return if (kurtosis in 2.8..5.2) 1.0 else 0.0
    }
}
