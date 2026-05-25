package com.alphahealth.monitor.wear.sensor.filter

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow
import kotlin.math.sqrt

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
        val data = FloatArray(75)
        // Let's generate a mock clean PPG-like sine wave (sine wave + offset)
        for (i in 0 until 75) {
            data[i] = kotlin.math.sin(i * 0.4f) * 10f + 100f
        }
        val sqi = filter.calculateSignalQualityIndex(data)
        // For a pure sine wave, Kurtosis is ~1.5 (outside 2.8..5.2). It should return 0.0.
        assertEquals(0.0, sqi, 0.001)

        // Generate normal-like distribution samples and verify their Kurtosis before assertions.
        // Bounded loop search over random states to guarantee we find a valid physiological Kurtosis [2.8..5.2].
        val normalData = FloatArray(75)
        val random = java.util.Random(10)
        var foundValidSignal = false
        var attempts = 0
        
        while (attempts < 500) {
            for (i in 0 until 75) {
                var sum = 0f
                for (j in 0 until 12) {
                    sum += random.nextFloat()
                }
                normalData[i] = sum - 6f // CLT approximation of Gaussian
            }
            
            // Check Kurtosis of generated data
            val mean = normalData.average()
            val variance = normalData.map { (it - mean).pow(2) }.average()
            val stdDev = sqrt(variance)
            if (stdDev > 0.0) {
                val kurtosis = normalData.map { ((it - mean) / stdDev).pow(4) }.average()
                if (kurtosis in 2.8..5.2) {
                    foundValidSignal = true
                    break
                }
            }
            attempts++
        }

        // Assert we successfully generated a signal with a valid Kurtosis
        org.junit.Assert.assertTrue(foundValidSignal)

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
