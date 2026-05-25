package com.alphahealth.monitor.wear.sensor.filter

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalQualityFilterTest {

    private val filter = SignalQualityFilter()

    @Test
    fun testFilterRawPPG_SizeConservation() {
        val rawInput = FloatArray(80) { i -> (i % 10).toFloat() }
        val filteredOutput = filter.filterRawPPG(rawInput)
        assertEquals(rawInput.size, filteredOutput.size)
    }

    @Test
    fun testCalculateSignalQualityIndex_ZeroVariance() {
        val flatline = FloatArray(75) { 1.5f }
        val sqi = filter.calculateSignalQualityIndex(flatline)
        assertEquals(0.0, sqi, 0.001)
    }

    @Test
    fun testCalculateSignalQualityIndex_ValidPhysiologicalRange() {
        // A normal-like distribution (or standard sinusoids with slight variations)
        // exhibits kurtosis in the target range (2.8 to 5.2)
        val data = FloatArray(75)
        // Let's generate a mock clean PPG-like signal (sine wave + small offset)
        for (i in 0 until 75) {
            data[i] = kotlin.math.sin(i * 0.4f) * 10f + 100f
        }
        val sqi = filter.calculateSignalQualityIndex(data)
        // For a pure sine wave, Kurtosis is ~1.5 (outside 2.8..5.2). It should return 0.0.
        assertEquals(0.0, sqi, 0.001)

        // Let's generate random normal distribution samples (which have kurtosis ~ 3.0)
        // We can approximate a normal distribution using Central Limit Theorem (sum of uniform variables)
        val normalData = FloatArray(75)
        val random = java.util.Random(42)
        for (i in 0 until 75) {
            var sum = 0f
            for (j in 0 until 12) {
                sum += random.nextFloat()
            }
            normalData[i] = sum - 6f // Mean 0, Variance 1
        }

        val normalSqi = filter.calculateSignalQualityIndex(normalData)
        // A standard normal distribution has kurtosis close to 3.0, which falls in 2.8..5.2.
        // It should return 1.0.
        assertEquals(1.0, normalSqi, 0.001)
    }

    @Test
    fun testCalculateSignalQualityIndex_OutliersRejection() {
        // High kurtosis (many standard deviations out) should be rejected
        val data = FloatArray(75) { 0f }
        data[0] = 100f // Huge single spike outlier (leptokurtic, high kurtosis)
        val sqi = filter.calculateSignalQualityIndex(data)
        assertEquals(0.0, sqi, 0.001)
    }
}
