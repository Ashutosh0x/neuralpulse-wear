package com.alphahealth.monitor.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * WatchDiscoveryViewModel
 *
 * Implements the full auto-discovery pipeline from the NeuralPulse Interactive Guide:
 *
 *   Step 1 SCANNING  -> NodeClient.connectedNodes() confirms Bluetooth link present
 *   Step 2 FOUND     -> CapabilityClient.getCapability(FILTER_REACHABLE) matches
 *                       "neuralpulse_wear_capability" declared in :wear res/values/wear.xml
 *   Step 3 PAIRING   -> State set to PAIRING; Lottie animation plays one-shot on UI
 *   Step 4 STREAMING -> MessageClient /biometrics/ppg telemetry active; BioStreamRepository
 *                       receives live BioFrame packets every 3 seconds
 *
 * Poll interval: 3 seconds — matches the Galaxy Watch FIFO batch cycle configured in
 * UniversalSensorRouter and HighPerformanceBioEngine.
 *
 * API reference:
 *   NodeClient     -> discover connected devices on the Wearable network
 *   CapabilityClient -> FILTER_REACHABLE checks both connected AND nearby nodes
 *   MessageClient  -> ordered, urgent delivery for real-time biometric streams
 */
class WatchDiscoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "WatchDiscovery"

    private val capabilityClient: CapabilityClient =
        Wearable.getCapabilityClient(application)

    private val _connectionState = MutableStateFlow(WatchConnectionState.SCANNING)
    val connectionState: StateFlow<WatchConnectionState> = _connectionState.asStateFlow()

    companion object {
        // Capability string declared in wear/src/main/res/values/wear_capabilities.xml
        // Must match exactly — case-sensitive
        private const val WEAR_CAPABILITY = "neuralpulse_wear_capability"

        // Polling interval matching the Galaxy Watch FIFO 3-second batch cycle
        private const val POLL_INTERVAL_MS = 3_000L
    }

    init {
        startDiscovery()
    }

    /**
     * Starts the auto-discovery polling loop.
     *
     * Every 3 seconds the phone queries CapabilityClient for FILTER_REACHABLE nodes
     * that have the :wear app installed with the matching capability declaration.
     * When found, the state machine advances SCANNING -> FOUND -> PAIRING -> STREAMING.
     *
     * FILTER_REACHABLE is preferred over FILTER_ALL because it returns only nodes
     * that are Bluetooth-connected and immediately reachable — not just paired.
     */
    fun startDiscovery() {
        viewModelScope.launch {
            _connectionState.value = WatchConnectionState.SCANNING
            Log.i(TAG, "Auto-discovery started: polling CapabilityClient every ${POLL_INTERVAL_MS}ms")

            while (_connectionState.value != WatchConnectionState.STREAMING) {
                try {
                    val result = capabilityClient
                        .getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                        .await()

                    if (result.nodes.isNotEmpty()) {
                        val watchNode = result.nodes.first()
                        Log.i(TAG, "Watch found: nodeId=${watchNode.id} name=${watchNode.displayName}")

                        _connectionState.value = WatchConnectionState.FOUND
                        delay(500L) // Brief hold on FOUND state so UI renders the chip

                        _connectionState.value = WatchConnectionState.PAIRING
                        // PAIRING state is held until onPairingComplete() is called by
                        // the Lottie animation's onAnimationEnd callback (progress == 1f)
                        break
                    } else {
                        Log.d(TAG, "No reachable watch nodes with capability — retrying in ${POLL_INTERVAL_MS}ms")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "CapabilityClient query failed: ${e.message} — retrying")
                    _connectionState.value = WatchConnectionState.OFFLINE
                }

                delay(POLL_INTERVAL_MS)
                if (_connectionState.value == WatchConnectionState.OFFLINE) {
                    _connectionState.value = WatchConnectionState.SCANNING
                }
            }
        }
    }

    /**
     * Called by WatchPairingOverlay when the Lottie animation completes (progress == 1f).
     * Advances the state from PAIRING -> STREAMING, which triggers the Compose overlay
     * to switch from the animation to live telemetry readouts.
     */
    fun onPairingComplete() {
        Log.i(TAG, "Pairing animation complete -> transitioning to STREAMING")
        _connectionState.value = WatchConnectionState.STREAMING
    }

    /**
     * Manually resets discovery — useful for reconnection after watch goes out of range.
     */
    fun resetDiscovery() {
        BioStreamRepository.reset()
        startDiscovery()
    }
}
