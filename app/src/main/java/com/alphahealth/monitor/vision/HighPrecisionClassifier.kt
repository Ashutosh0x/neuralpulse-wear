package com.alphahealth.monitor.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier.ImageClassifierOptions

/**
 * HighPrecisionClassifier
 *
 * MediaPipe Tasks Vision ImageClassifier wrapping the food_nutrition_v1.tflite model.
 * Uses GPU delegate for <2ms inference, confidence threshold 0.92, 3-frame temporal consensus.
 *
 * Exposes [lastConfidence] so FoodVisionEngine can include it in [FoodScanResult].
 */
class HighPrecisionClassifier(private val context: Context) {

    private val TAG = "HighPrecisionClassifier"
    private var classifier: ImageClassifier? = null
    private val consensusEngine = TemporalConsensusEngine(windowSize = 3, requiredAgreement = 2)

    // Tracks the confidence score of the most recent classification (before consensus)
    @Volatile
    private var _lastConfidence: Float = 0f
    fun lastConfidence(): Float = _lastConfidence

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("models/food_nutrition_v1.tflite")
                .setDelegate(Delegate.GPU)
                .build()

            val options = ImageClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setMaxResults(3)          // Top-3 for better consensus reasoning
                .setScoreThreshold(0.0f)   // Raw outputs; threshold applied below at 0.92
                .build()

            classifier = ImageClassifier.createFromOptions(context, options)
            Log.i(TAG, "MediaPipe INT8 food classifier initialized on GPU delegate.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build ImageClassifier: ${e.message}")
            // Fall back to CPU delegate
            tryFallbackCpu()
        }
    }

    private fun tryFallbackCpu() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("models/food_nutrition_v1.tflite")
                .setDelegate(Delegate.CPU)
                .build()
            val options = ImageClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setMaxResults(3)
                .setScoreThreshold(0.0f)
                .build()
            classifier = ImageClassifier.createFromOptions(context, options)
            Log.w(TAG, "Fallback: using CPU delegate for food classifier.")
        } catch (e: Exception) {
            Log.e(TAG, "CPU fallback also failed: ${e.message}")
        }
    }

    /**
     * Classify a camera frame.
     *
     * @param bitmap ARGB_8888 bitmap from CameraX ImageAnalysis
     * @return Consensus-confirmed raw label string, or null if consensus not yet reached.
     *
     * CONFIDENCE GATE: top-1 score must be >= 0.92 to enter the consensus window.
     * CONSENSUS: 2 of 3 consecutive qualifying frames must agree on the same label.
     *
     * This dual-gate prevents a single high-confidence spurious frame (e.g. reflection,
     * partial occlusion) from triggering a false positive result.
     */
    fun classifyFrameWithStrictFiltering(bitmap: Bitmap): String? {
        val currentClassifier = classifier ?: return null
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val results = currentClassifier.classify(mpImage)
            val topCategory = results
                ?.classificationResult()
                ?.classifications()
                ?.firstOrNull()
                ?.categories()
                ?.firstOrNull()

            if (topCategory != null && topCategory.score() >= 0.92f) {
                _lastConfidence = topCategory.score()
                consensusEngine.addPredictionAndCheckConsensus(topCategory.categoryName())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Classification error: ${e.message}")
            null
        }
    }

    fun resetConsensus() {
        consensusEngine.clearHistory()
    }
}
