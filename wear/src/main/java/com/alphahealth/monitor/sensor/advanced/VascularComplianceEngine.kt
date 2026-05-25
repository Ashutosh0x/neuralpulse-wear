package com.alphahealth.monitor.sensor.advanced

class VascularComplianceEngine {

    /**
     * Correlates the temporal alignment between electrical heart events and physical blood flow.
     * @param ecgRWaveTimestamp The exact epoch time (ms) of the electrical cardiac depolarization.
     * @param ppgPulsePeakTimestamp The exact epoch time (ms) when the infrared sensor captured the pulse wave.
     * @return Calculated Pulse Transit Time in milliseconds.
     */
    fun calculatePulseTransitTime(ecgRWaveTimestamp: Long, ppgPulsePeakTimestamp: Long): Long {
        val pttDelta = ppgPulsePeakTimestamp - ecgRWaveTimestamp
        
        // Validation boundary: PTT values typically sit strictly between 150ms and 400ms
        return if (pttDelta in 150..400) {
            pttDelta
        } else {
            -1L // Flag as an invalid artifact frame (SQI Fail)
        }
    }
}
