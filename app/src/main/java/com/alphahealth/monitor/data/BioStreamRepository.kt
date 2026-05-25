package com.alphahealth.monitor.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BioFrame
 *
 * A single 12-byte biometric packet decoded from the Galaxy Watch MessageClient
 * channel path /biometrics/ppg. Matches the ByteBuffer layout in NeuralPulseWearService:
 *   [0..3]  PPG conductance float
 *   [4..7]  EDA conductance float
 *   [8..11] Heart rate int
 */
data class BioFrame(
    val ppg: Float,
    val eda: Float,
    val heartRate: Int
)

/**
 * BioStreamRepository
 *
 * Singleton StateFlow sink for live bio-metric packets decoded by WatchDataReceiver
 * (WearableListenerService). Compose observes this via collectAsState() — any new
 * packet triggers recomposition of the watch widget and dashboard overlays.
 *
 * Thread safety: MutableStateFlow is thread-safe; WearableListenerService delivers
 * onMessageReceived on a background thread, so direct .value assignment is safe.
 */
object BioStreamRepository {

    private val _latest = MutableStateFlow<BioFrame?>(null)
    val latest: StateFlow<BioFrame?> = _latest.asStateFlow()

    fun emit(frame: BioFrame) {
        _latest.value = frame
    }

    fun reset() {
        _latest.value = null
    }
}
