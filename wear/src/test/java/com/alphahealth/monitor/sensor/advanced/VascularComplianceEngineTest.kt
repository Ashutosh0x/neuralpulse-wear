package com.alphahealth.monitor.sensor.advanced

import org.junit.Assert.assertEquals
import org.junit.Test

class VascularComplianceEngineTest {

    private val engine = VascularComplianceEngine()

    @Test
    fun testCalculatePulseTransitTime_ValidRange() {
        val ecgTime = 1716612300000L
        val ppgTime = 1716612300250L // 250ms delta
        val ptt = engine.calculatePulseTransitTime(ecgTime, ppgTime)
        assertEquals(250L, ptt)
    }

    @Test
    fun testCalculatePulseTransitTime_TooShort_Rejected() {
        val ecgTime = 1716612300000L
        val ppgTime = 1716612300100L // 100ms delta (too fast)
        val ptt = engine.calculatePulseTransitTime(ecgTime, ppgTime)
        assertEquals(-1L, ptt)
    }

    @Test
    fun testCalculatePulseTransitTime_TooLong_Rejected() {
        val ecgTime = 1716612300000L
        val ppgTime = 1716612300500L // 500ms delta (too slow)
        val ptt = engine.calculatePulseTransitTime(ecgTime, ppgTime)
        assertEquals(-1L, ptt)
    }
}
