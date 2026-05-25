package com.alphahealth.monitor.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier.ImageClassifierOptions

class HighPrecisionClassifier(private val context: Context) {

    private val TAG = "HighPrecisionClassifier"
    private var classifier: ImageClassifier? = null
    private val consensusEngine = TemporalConsensusEngine(windowSize = 3, requiredAgreement = 2)

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath("models/food_nutrition_v1.tflite")
                .setDelegate(Delegate.GPU)

            val options = ImageClassifierOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setMaxResults(1)
                .setScoreThreshold(0.0f) // Keep raw outputs, threshold is checked at 0.92f
                .build()

            classifier = ImageClassifier.createFromOptions(context, options)
            Log.d(TAG, "MediaPipe image classifier initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build ImageClassifier: ${e.message}")
        }
    }

    fun classifyFrameWithStrictFiltering(bitmap: Bitmap): String? {
        val currentClassifier = classifier ?: return null
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val results = currentClassifier.classify(mpImage)

            val topCategory = results?.classificationResult()?.classifications()?.firstOrNull()?.categories()?.firstOrNull()
            if (topCategory != null && topCategory.score() >= 0.92f) {
                consensusEngine.addPredictionAndCheckConsensus(topCategory.categoryName())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Classification error: ${e.message}")
            null
        }
    }
}
