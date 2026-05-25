package com.alphahealth.monitor.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

data class FoodScanResult(
    val foodItemName: String,
    val confidence: Float,
    val baselineCalories: Int,
    val macronutrients: Map<String, Float> // Protein, Carbs, Fats
)

class FoodVisionEngine(private val context: Context) {

    private val TAG = "AlphaFoodVision"
    private val highPrecisionClassifier = HighPrecisionClassifier(context)

    fun scanFoodFrame(bitmap: Bitmap): FoodScanResult? {
        try {
            val detectedFood = highPrecisionClassifier.classifyFrameWithStrictFiltering(bitmap)
            if (detectedFood != null) {
                return lookupNutritionMetrics(detectedFood, 0.95f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Frame classification failed: ${e.message}")
        }
        return null
    }

    private fun lookupNutritionMetrics(foodItem: String, confidence: Float): FoodScanResult {
        val macroMap = when (foodItem.lowercase().replace(" ", "_")) {
            "grilled_chicken_breast", "chicken" -> mapOf("Protein" to 31f, "Carbs" to 0f, "Fats" to 3.6f)
            "avocado_slice", "avocado" -> mapOf("Protein" to 2f, "Carbs" to 8.5f, "Fats" to 14.7f)
            "salmon_fillet", "salmon" -> mapOf("Protein" to 22f, "Carbs" to 0f, "Fats" to 13f)
            "pasta_carbonara", "pasta" -> mapOf("Protein" to 14f, "Carbs" to 58f, "Fats" to 19f)
            else -> mapOf("Protein" to 0f, "Carbs" to 0f, "Fats" to 0f)
        }
        
        val protein = macroMap["Protein"] ?: 0f
        val carbs = macroMap["Carbs"] ?: 0f
        val fats = macroMap["Fats"] ?: 0f
        val calories = (protein * 4 + carbs * 4 + fats * 9).toInt()
        
        val capitalizedName = foodItem.replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        return FoodScanResult(
            foodItemName = capitalizedName,
            confidence = confidence,
            baselineCalories = calories,
            macronutrients = macroMap
        )
    }
}
