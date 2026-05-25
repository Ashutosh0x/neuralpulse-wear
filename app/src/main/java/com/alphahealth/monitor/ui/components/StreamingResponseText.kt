package com.alphahealth.monitor.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate

private const val FADE_INTERVAL_MS = 120

/**
 * Displays streaming text with a smooth crossfade transition when the content updates.
 *
 * Uses a two-layer compositing technique:
 * 1. Layer 1 (base): Shows the current stable text at (1 - alpha) opacity
 * 2. Layer 2 (overlay): Shows new text fading in with BlendMode.Plus
 *
 * The sum of their alphas is always 1, ensuring a flicker-free crossfade without
 * intermediate darkening or color artifacts.
 *
 * Ported from Google AI Edge Gallery's BufferedFadingMarkdownText.
 *
 * @param text The full text to display (grows as tokens stream in)
 * @param inProgress Whether the AI is still generating tokens
 */
@Composable
fun StreamingResponseText(
    text: String,
    inProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    var text1 by remember { mutableStateOf(text) }
    var text2 by remember { mutableStateOf("") }
    val alpha2 = remember { Animatable(0f) }
    val currentText by rememberUpdatedState(text)
    var showOverlay by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        snapshotFlow { currentText }
            .conflate()
            .collect { newText ->
                if (newText == text1) return@collect

                // Set the new text onto the hidden overlay layer
                text2 = newText
                alpha2.snapTo(0f)

                // Smoothly fade the new layout in
                alpha2.animateTo(
                    1f,
                    animationSpec = tween(FADE_INTERVAL_MS, easing = LinearOutSlowInEasing),
                )

                // Swap the background text to match instantly
                text1 = newText

                // Wait a frame for the overlay to fully draw
                @Suppress("UNUSED_VARIABLE")
                val unused = awaitFrame()

                // Instantly hide overlay to prevent double-rendering
                alpha2.snapTo(0f)
            }
    }

    val previousInProgress = rememberUpdatedState(inProgress)
    LaunchedEffect(inProgress) {
        // Remove overlay once streaming completes
        if (previousInProgress.value && !inProgress) {
            delay(FADE_INTERVAL_MS.toLong() * 2)
            showOverlay = false
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    ) {
        // LAYER 1: Base text
        SelectionContainer {
            Text(
                text = text1,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer { alpha = 1f - alpha2.value },
            )
        }

        // LAYER 2: Overlay text (fading in)
        if (showOverlay) {
            Text(
                text = text2,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clearAndSetSemantics {}
                    .graphicsLayer {
                        alpha = alpha2.value
                        blendMode = BlendMode.Plus
                    },
            )
        }
    }
}
