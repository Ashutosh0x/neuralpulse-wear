package com.alphahealth.monitor.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alphahealth.monitor.data.BioStreamRepository
import com.alphahealth.monitor.data.WatchConnectionState
import com.alphahealth.monitor.data.WatchDiscoveryViewModel

/**
 * WatchConnectionWidget
 *
 * The complete 3D watch widget implementing the interactive guide's Box layer stack:
 *
 *   Layer 1 (background): WatchScene3D — 3D rotating Galaxy Watch model (SceneView/Filament)
 *                          with live HR waveform projected onto the watch_face_screen sub-mesh
 *                          via WatchFaceTextureEngine (512x512 ARGB bitmap).
 *   Layer 2 (middle):     WatchPairingOverlay — Lottie animation plays once on FOUND -> PAIRING.
 *   Layer 3 (foreground): WatchDataOverlay — Compose connection state chip + live biometrics.
 *
 * Auto-connection flow (no user tap required):
 *   WatchDiscoveryViewModel polls CapabilityClient every 3 seconds.
 *   On FILTER_REACHABLE match: SCANNING -> FOUND -> PAIRING -> (Lottie ends) -> STREAMING.
 *   StateFlow updates propagate to all three layers via collectAsState().
 *
 * Live telemetry:
 *   WatchDataReceiver (WearableListenerService) decodes 12-byte ByteBuffer packets
 *   on path /biometrics/ppg and emits BioFrame to BioStreamRepository.
 *   collectAsState() here triggers recomposition of all layers automatically.
 *
 * Watch Face Texture:
 *   WatchFaceTextureEngine renders a live waveform bitmap on each new BioFrame.
 *   The bitmap is assigned to SceneView's watch_face_screen material node at runtime,
 *   projecting real ECG-style data directly onto the 3D model's wrist surface.
 *   HR history buffer retains the last 30 readings (3-second cadence = 90 seconds).
 */
@Composable
fun WatchConnectionWidget(
    viewModel: WatchDiscoveryViewModel = viewModel()
) {
    val state by viewModel.connectionState.collectAsState()
    val bioFrame by BioStreamRepository.latest.collectAsState()

    // Rolling HR history buffer — last 30 readings drive the waveform texture
    // Implemented as a remembered snapshot list that updates on each BioFrame
    val hrHistory = remember { mutableStateListOf<Int>() }
    LaunchedEffect(bioFrame) {
        bioFrame?.let { frame ->
            hrHistory.add(frame.heartRate)
            if (hrHistory.size > 30) hrHistory.removeAt(0)
        }
    }

    // Watch face texture — re-rendered on every new BioFrame (< 0.5ms per render)
    val watchFaceTexture = rememberWatchFaceTexture(
        bioFrame = bioFrame,
        hrHistory = hrHistory.toList()
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // === LAYER 1: 3D Watch Model (SceneView + Filament) ===
            // WatchFaceTextureEngine bitmap is passed to WatchScene3D.
            // When the GLB asset is present, SceneView assigns this bitmap
            // to the watch_face_screen material node at runtime.
            WatchScene3D(
                connectionState = state,
                heartRate = bioFrame?.heartRate ?: 0,
                bioFrame = bioFrame,
                watchFaceTexture = watchFaceTexture,
                modifier = Modifier.fillMaxSize()
            )

            // === LAYER 2: Lottie Pairing Animation ===
            // Plays exactly once when the watch is discovered (PAIRING state).
            // Centered over the 3D scene. Fades in/out via AnimatedVisibility.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WatchPairingOverlay(
                    visible = state == WatchConnectionState.PAIRING,
                    onAnimationEnd = { viewModel.onPairingComplete() }
                )
            }

            // === LAYER 3: Compose Overlay — State Chip + Live Readouts ===
            // Sits at the bottom of the card. AnimatedContent swaps the state chip
            // text and icon as the connection state machine advances.
            WatchDataOverlay(
                state = state,
                bioFrame = bioFrame,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
