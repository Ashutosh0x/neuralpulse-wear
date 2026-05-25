package com.alphahealth.monitor.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.alphahealth.monitor.shared.SyncProtocols
import java.nio.ByteBuffer

class WatchDataTransporter(
    context: Context,
    private val onTelemetryReceived: (eda: Double, heartRate: Int) -> Unit,
    private val onSqiReceived: ((sqi: Double) -> Unit)? = null
) : MessageClient.OnMessageReceivedListener {

    private val TAG = "AlphaWatchTransporter"

    init {
        try {
            // Register as MessageClient listener to capture raw binary payloads
            Wearable.getMessageClient(context).addListener(this)
            Log.d(TAG, "Wearable MessageClient listener bound for byte streams.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register Wearable MessageClient listener: ${e.message}")
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        if (path == SyncProtocols.PATH_WATCH_TELEMETRY) {
            try {
                val dataBytes = messageEvent.data ?: return
                
                // Wrap the 8-byte payload into ByteBuffer to extract primitives
                val buffer = ByteBuffer.wrap(dataBytes)
                val rawEda = buffer.float.toDouble()
                val rawHR = buffer.float.toInt()

                Log.i(TAG, "Binary telemetry packet received: Path=$path -> EDA=$rawEda uS, HR=$rawHR BPM")
                
                // Route directly to vulnerability analyzer
                onTelemetryReceived(rawEda, rawHR)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to decode binary telemetry payload: ${e.message}")
            }
        } else if (path == SyncProtocols.PATH_WATCH_SQI) {
            try {
                val dataBytes = messageEvent.data ?: return
                val buffer = ByteBuffer.wrap(dataBytes)
                val sqi = buffer.double

                Log.i(TAG, "Binary SQI packet received: Path=$path -> SQI=$sqi")
                onSqiReceived?.invoke(sqi)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to decode binary SQI payload: ${e.message}")
            }
        }
    }

    fun unregister(context: Context) {
        try {
            Wearable.getMessageClient(context).removeListener(this)
            Log.d(TAG, "Wearable MessageClient event listener unbound.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to unregister: ${e.message}")
        }
    }
}
