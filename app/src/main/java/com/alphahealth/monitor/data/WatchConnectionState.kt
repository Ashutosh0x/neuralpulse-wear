package com.alphahealth.monitor.data

/**
 * WatchConnectionState
 *
 * Represents the full lifecycle of the NeuralPulse Wear OS auto-discovery pipeline:
 *
 *   OFFLINE   -> No Bluetooth or Wearable Data Layer available
 *   SCANNING  -> NodeClient.connectedNodes() polling every 3s
 *   FOUND     -> CapabilityClient FILTER_REACHABLE matched neuralpulse_wear_capability
 *   PAIRING   -> Lottie watch-to-phone arc animation playing (one-shot, 1 iteration)
 *   STREAMING -> MessageClient /biometrics/ppg live telemetry active
 */
enum class WatchConnectionState {
    OFFLINE,
    SCANNING,
    FOUND,
    PAIRING,
    STREAMING;

    val displayLabel: String get() = when (this) {
        OFFLINE   -> "Watch Offline"
        SCANNING  -> "Scanning for watch"
        FOUND     -> "Watch found nearby"
        PAIRING   -> "Pairing animation"
        STREAMING -> "Live telemetry streaming"
    }

    val isActive: Boolean get() = this == STREAMING
}
