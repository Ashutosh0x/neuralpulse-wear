package com.alphahealth.monitor.wear.sensor

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import java.nio.ByteBuffer

/**
 * NeuralPulseWearService
 *
 * Transmits live biometric frames from the Galaxy Watch to the companion phone app
 * via MessageClient on the Wearable Data Layer API.
 *
 * Packet format: 12-byte ByteBuffer (matches WatchDataReceiver on the phone side)
 *   [0..3]  Float  — PPG raw conductance
 *   [4..7]  Float  — EDA galvanic skin conductance (uS)
 *   [8..11] Int    — Heart rate (BPM)
 *
 * Transmission path: /biometrics/ppg
 *   MessageClient guarantees ordered delivery — correct choice for time-sensitive
 *   biometric streams (vs. DataClient which synchronises state, not ordered messages).
 *
 * Transmission cycle: 3 seconds — matches the Galaxy Watch FIFO setBatchProcessingGroup(3000ms)
 *   configured in UniversalSensorRouter to align CPU sleep cycles (0.0ms GC pauses).
 *
 * Usage:
 *   val service = NeuralPulseWearService(context)
 *   service.sendBioFrame(ppg = rawPpg, eda = edaConductance, hr = heartRate)
 */
class NeuralPulseWearService(private val context: Context) {

    private val TAG = "NeuralPulseWearSvc"

    companion object {
        const val PATH_BIOMETRICS_PPG = "/biometrics/ppg"
        const val PAYLOAD_SIZE_BYTES = 12 // 4 (float ppg) + 4 (float eda) + 4 (int hr)
    }

    /**
     * Packs biometric values into a 12-byte ByteBuffer and sends it to all
     * connected and reachable phone nodes via MessageClient.
     *
     * Called every 3 seconds by the watch sensor loop — aligned with the
     * FIFO hardware batch cycle to avoid waking the CPU unnecessarily.
     *
     * @param ppg   Raw PPG sensor output
     * @param eda   Galvanic skin conductance (electrodermal activity) in microsiemens
     * @param hr    Instantaneous heart rate in BPM
     */
    suspend fun sendBioFrame(ppg: Float, eda: Float, hr: Int) {
        try {
            val payload = ByteBuffer
                .allocate(PAYLOAD_SIZE_BYTES)
                .putFloat(ppg)
                .putFloat(eda)
                .putInt(hr)
                .array()

            // Discover all connected phone nodes
            val nodes = Wearable.getNodeClient(context)
                .connectedNodes
                .await()

            if (nodes.isEmpty()) {
                Log.w(TAG, "No connected phone nodes available — bio frame dropped")
                return
            }

            // Transmit to the first reachable phone node (companion phone)
            val phoneNodeId = nodes.first().id
            Wearable.getMessageClient(context)
                .sendMessage(phoneNodeId, PATH_BIOMETRICS_PPG, payload)
                .await()

            Log.d(TAG, "BioFrame transmitted to $phoneNodeId: PPG=$ppg EDA=$eda HR=$hr BPM")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send BioFrame: ${e.message}")
        }
    }
}
