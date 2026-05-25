package com.alphahealth.monitor.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * WatchPairingOverlay
 *
 * Plays a one-shot Lottie dotLottie / JSON animation when a watch is found nearby
 * and the connection state transitions to PAIRING.
 *
 * Animation lifecycle:
 *   1. Becomes visible when [visible] = true (PAIRING state entered)
 *   2. Fades in via AnimatedVisibility (fadeIn 300ms)
 *   3. Lottie plays exactly once (iterations = 1)
 *   4. When progress reaches 1f (animation complete), [onAnimationEnd] fires
 *   5. WatchDiscoveryViewModel.onPairingComplete() advances state to STREAMING
 *   6. AnimatedVisibility fades out (fadeOut 400ms)
 *
 * ASSET REQUIREMENT:
 *   Place a Bluetooth pairing / device-connect Lottie file at:
 *     app/src/main/res/raw/watch_pair_anim.json
 *   Free source: LottieFiles.com — search "bluetooth pairing" or "device connect"
 *   dotLottie format (.lottie) is also supported by lottie-compose 6.3.0.
 *
 * DEPENDENCY (app/build.gradle.kts):
 *   implementation("com.airbnb.android:lottie-compose:6.3.0")
 *
 * FALLBACK:
 *   If the raw/watch_pair_anim.json asset is not present, the overlay is invisible
 *   and onAnimationEnd() fires immediately after a 1-second delay, advancing state
 *   to STREAMING without visual interruption.
 */
@Composable
fun WatchPairingOverlay(
    visible: Boolean,
    onAnimationEnd: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(400))
    ) {
        Box(contentAlignment = Alignment.Center) {
            val compositionResult = rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(
                    // R.raw.watch_pair_anim  -- uncomment once asset is placed
                    // Using 0 as a sentinel: lottie returns null composition for 0
                    // so the fallback LaunchedEffect fires automatically
                    getWatchPairAnimResId()
                )
            )
            val composition by compositionResult

            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = 1,
                isPlaying = visible
            )

            LaunchedEffect(progress) {
                if (progress >= 1f) {
                    onAnimationEnd()
                }
            }

            // If composition loaded successfully, render the Lottie animation
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(200.dp)
                )
            } else {
                // Fallback: fire completion after 1.5s if no asset available
                LaunchedEffect(visible) {
                    if (visible) {
                        kotlinx.coroutines.delay(1500L)
                        onAnimationEnd()
                    }
                }
            }
        }
    }
}

/**
 * Returns the resource ID for the watch pairing Lottie animation.
 * Returns 0 (invalid) when the asset has not yet been placed in res/raw/,
 * which causes rememberLottieComposition to return a null composition and
 * triggers the fallback delay path in WatchPairingOverlay.
 *
 * Once the asset is added, replace this function body with:
 *   return R.raw.watch_pair_anim
 */
private fun getWatchPairAnimResId(): Int {
    return try {
        val field = Class.forName("com.alphahealth.monitor.R\$raw")
            .getDeclaredField("watch_pair_anim")
        field.getInt(null)
    } catch (e: Exception) {
        0 // Asset not yet present; fallback path activates
    }
}
