package com.alphahealth.monitor.data

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import java.nio.ByteBuffer

/**
 * WatchDataReceiver
 *
 * WearableListenerService that receives 12-byte ByteBuffer biometric packets
 * from the :wear module's NeuralPulseWearService via MessageClient.
 *
 * Packet layout (matches NeuralPulseWearService.sendBioFrame):
 *   [0..3]  Float  — PPG raw conductance value
 *   [4..7]  Float  — EDA galvanic skin conductance (uS)
 *   [8..11] Int    — Heart rate (BPM)
 *
 * Channel path: /biometrics/ppg
 *   Selected because MessageClient guarantees ordered delivery — critical for
 *   real-time biometric streams where a late packet must not overwrite a newer one.
 *
 * Data flow:
 *   Galaxy Watch -> MessageClient -> onMessageReceived() -> BioStreamRepository.emit()
 *   -> StateFlow -> Compose collectAsState() -> WatchConnectionWidget recompose
 *
 * Registration in AndroidManifest.xml (required):
 *   <service
 *       android:name=".data.WatchDataReceiver"
 *       android:exported="true">
 *     <intent-filter>
 *       <action android:name="com.google.android.gms.wearable.MESSAGE_RECEIVED" />
 *       <data android:scheme="wear" android:host="*"
 *             android:pathPrefix="/biometrics" />
 *     </intent-filter>
 *   </service>
 */
class WatchDataReceiver : WearableListenerService() {

    private val TAG = "WatchDataReceiver"

    companion object {
        const val PATH_BIOMETRICS_PPG = "/biometrics/ppg"
    }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            PATH_BIOMETRICS_PPG -> decodeBioFrame(event.data)
            else -> Log.d(TAG, "Unhandled message path: ${event.path}")
        }
    }

    /**
     * Decodes the 12-byte ByteBuffer payload packed by NeuralPulseWearService.sendBioFrame().
     * Updates BioStreamRepository which Compose observes via collectAsState().
     */
    private fun decodeBioFrame(data: ByteArray) {
        if (data.size < 12) {
            Log.w(TAG, "Malformed packet: expected 12 bytes, received ${data.size}")
            return
        }
        try {
            val buffer = ByteBuffer.wrap(data)
            val ppg = buffer.float
            val eda = buffer.float
            val hr  = buffer.int

            Log.i(TAG, "BioFrame decoded: PPG=$ppg EDA=$eda HR=$hr BPM")

            // Emit to singleton StateFlow — triggers Compose recomposition
            BioStreamRepository.emit(BioFrame(ppg = ppg, eda = eda, heartRate = hr))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode BioFrame payload: ${e.message}")
        }
    }
}
