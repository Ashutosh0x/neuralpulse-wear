package com.alphahealth.monitor.integration

import android.content.Context
import com.alphahealth.monitor.data.PredictiveRiskScore
import com.alphahealth.monitor.data.VulnerabilityEngine
import com.alphahealth.monitor.data.WatchDataTransporter
import com.alphahealth.monitor.shared.SyncProtocols
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import java.nio.ByteBuffer

class EndToEndTelemetryIntegrationTest {

    @Test
    fun testWatchToPhoneTelemetryIntegrationFlow() = runBlocking {
        // 1. Simulate packaging of telemetry on the Watch side (equivalent to HighPerformanceBioEngine)
        val testEda = 5.2f
        val testHeartRate = 95
        
        val buffer = ByteBuffer.allocate(8)
        buffer.putFloat(testEda)
        buffer.putFloat(testHeartRate.toFloat())
        val rawPayload = buffer.array()

        // 2. Mock Context and create a stubbed listener to receive data on the Phone side
        val mockContext = Mockito.mock(Context::class.java)
        
        var receivedEda = 0.0
        var receivedHeartRate = 0
        
        // Instantiate the vulnerability engine
        val vulnerabilityEngine = VulnerabilityEngine(mockContext)

        // Instantiate the transporter with callbacks that will feed directly into our analysis flow
        val transporter = WatchDataTransporter(
            context = mockContext,
            onTelemetryReceived = { eda, heartRate ->
                receivedEda = eda
                receivedHeartRate = heartRate
            }
        )

        // 3. Mock MessageEvent carrying the raw binary payload from the Watch
        val mockMessageEvent = Mockito.mock(MessageEvent::class.java)
        Mockito.`when`(mockMessageEvent.path).thenReturn(SyncProtocols.PATH_WATCH_TELEMETRY)
        Mockito.`when`(mockMessageEvent.data).thenReturn(rawPayload)

        // 4. Trigger message delivery manually on the phone's receiver
        transporter.onMessageReceived(mockMessageEvent)

        // Verify values were correctly decoded
        assertEquals(testEda.toDouble(), receivedEda, 0.001)
        assertEquals(testHeartRate, receivedHeartRate)

        // 5. Feed the decoded values into the VulnerabilityEngine to evaluate homeostatic recovery
        val scoreFlow = vulnerabilityEngine.analyzeVulnerability(
            liveEda = receivedEda.toFloat(),
            liveHydration = 0.60f,
            heartRateCurrent = receivedHeartRate,
            avgEnergyScore = 75,
            sleepApneaActive = false,
            isSignalDegraded = false,
            isNocturnal = false
        )

        val finalScore: PredictiveRiskScore = scoreFlow.first()

        // 6. Assertions on the integrated calculation
        assertNotNull(finalScore)
        assertEquals("Smart Watch", finalScore.resolvedDeviceSource)
        
        // Let's verify that the index calculation is accurate based on the stress markers:
        // base index = 15
        // receivedEda (5.2f) > 4.5f -> index + 25 = 40
        // liveHydration (0.60f) -> no change
        // heartRateCurrent (95) > 85 -> index + 15 = 55
        // expected finalIndex = 55
        assertEquals(55, finalScore.vulnerabilityIndex)
        
        // Autonomic Recovery Budget = 100 - vulnerabilityIndex = 45
        assertTrue(finalScore.recommendedMicroIntervention.contains("MODERATE STRAIN"))
    }
}
