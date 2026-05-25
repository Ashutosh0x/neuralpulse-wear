package com.alphahealth.monitor.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

/**
 * FoodScanResult — result from a confirmed 3-frame temporal consensus inference
 */
data class FoodScanResult(
    val foodItemName: String,
    val confidence: Float,
    val baselineCalories: Int,
    val macronutrients: Map<String, Float> // "Protein", "Carbs", "Fats" in grams per 100g serving
)

/**
 * FoodVisionEngine
 *
 * Orchestrates real-time food classification by:
 *   1. Passing every camera frame (as Bitmap) to HighPrecisionClassifier
 *   2. HighPrecisionClassifier runs MediaPipe ImageClassifier (food_nutrition_v1.tflite)
 *      with GPU delegate, threshold 0.92, and 3-frame temporal consensus
 *   3. When consensus is confirmed, the raw model label is mapped to a cleaned food name
 *      and accurate USDA/Open Food Facts macronutrient data via [lookupNutritionMetrics]
 *
 * Label mapping strategy:
 *   The food_nutrition_v1.tflite model (EfficientNet-Lite4 variant, iNaturalist + Food-101 trained)
 *   returns raw Food-101 class names such as "apple_pie", "baby_back_ribs", "pizza", "sushi" etc.
 *   These are normalized here to consumer-readable names with real macro data.
 *   Unknown classes fall back to a generic protein/carb/fat estimation from caloric density.
 */
class FoodVisionEngine(private val context: Context) {

    private val TAG = "AlphaFoodVision"
    private val highPrecisionClassifier = HighPrecisionClassifier(context)

    /**
     * Called per camera frame from ImageAnalysis analyzer.
     * Returns a [FoodScanResult] only when the 3-frame temporal consensus is confirmed.
     * Returns null for most frames (no consensus yet or confidence below 0.92).
     */
    fun scanFoodFrame(bitmap: Bitmap): FoodScanResult? {
        return try {
            val detectedLabel = highPrecisionClassifier.classifyFrameWithStrictFiltering(bitmap)
            if (detectedLabel != null) {
                lookupNutritionMetrics(detectedLabel, highPrecisionClassifier.lastConfidence())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Frame classification failed: ${e.message}")
            null
        }
    }

    /**
     * Maps raw model output labels to clean consumer names + USDA macronutrient data.
     *
     * All macro values are per 100g typical serving size.
     * Calorie formula: (protein × 4) + (carbs × 4) + (fat × 9) kcal
     *
     * Source: USDA FoodData Central / Open Food Facts averages
     */
    private fun lookupNutritionMetrics(rawLabel: String, confidence: Float): FoodScanResult {
        // Normalize: remove trailing numbers, underscores → spaces, lowercase
        val normalized = rawLabel
            .replace(Regex("_\\d+$"), "")
            .replace("_", " ")
            .trim()
            .lowercase()

        // Find best matching entry
        val entry = NUTRITION_DATABASE.entries
            .firstOrNull { (key, _) ->
                normalized.contains(key) || key.contains(normalized.split(" ").first())
            } ?: NUTRITION_DATABASE.entries.firstOrNull { (key, _) ->
                normalized.split(" ").any { word -> key.contains(word) && word.length > 3 }
            }

        return if (entry != null) {
            val (_, data) = entry
            val (displayName, protein, carbs, fat) = data
            val calories = (protein * 4 + carbs * 4 + fat * 9).toInt()
            FoodScanResult(
                foodItemName = displayName,
                confidence = confidence,
                baselineCalories = calories,
                macronutrients = mapOf("Protein" to protein, "Carbs" to carbs, "Fats" to fat)
            )
        } else {
            // Fallback: show the raw label cleaned up with generic macro profile
            val displayName = normalized.split(" ")
                .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            FoodScanResult(
                foodItemName = displayName,
                confidence = confidence,
                baselineCalories = 180,
                macronutrients = mapOf("Protein" to 8f, "Carbs" to 22f, "Fats" to 6f)
            )
        }
    }

    companion object {
        /**
         * Nutrition database: key = normalized label substring → (displayName, protein, carbs, fat)
         * All macros in grams per 100g serving. USDA/Open Food Facts sourced.
         */
        private val NUTRITION_DATABASE: Map<String, NutritionEntry> = mapOf(
            // Fruits
            "apple"               to NutritionEntry("Apple",               0.3f,  14.0f,  0.2f),
            "banana"              to NutritionEntry("Banana",              1.1f,  23.0f,  0.3f),
            "orange"              to NutritionEntry("Orange",              0.9f,  12.0f,  0.1f),
            "mango"               to NutritionEntry("Mango",               0.8f,  15.0f,  0.4f),
            "strawberr"           to NutritionEntry("Strawberries",        0.7f,   8.0f,  0.3f),
            "blueberr"            to NutritionEntry("Blueberries",         0.7f,  14.5f,  0.3f),
            "grapes"              to NutritionEntry("Grapes",              0.6f,  18.1f,  0.2f),
            "watermelon"          to NutritionEntry("Watermelon",          0.6f,   7.6f,  0.2f),
            "pineapple"           to NutritionEntry("Pineapple",           0.5f,  13.1f,  0.1f),
            "pear"                to NutritionEntry("Pear",                0.4f,  15.2f,  0.1f),
            "peach"               to NutritionEntry("Peach",               0.9f,   9.5f,  0.3f),
            "avocado"             to NutritionEntry("Avocado",             2.0f,   8.5f, 14.7f),
            "lemon"               to NutritionEntry("Lemon",               1.1f,   9.3f,  0.3f),
            "kiwi"                to NutritionEntry("Kiwi",                1.1f,  15.0f,  0.5f),
            "cherry"              to NutritionEntry("Cherries",            1.1f,  16.0f,  0.2f),
            "plum"                to NutritionEntry("Plum",                0.7f,  11.4f,  0.3f),

            // Vegetables
            "broccoli"            to NutritionEntry("Broccoli",            2.8f,   7.0f,  0.4f),
            "carrot"              to NutritionEntry("Carrots",             0.9f,  10.0f,  0.2f),
            "spinach"             to NutritionEntry("Spinach",             2.9f,   3.6f,  0.4f),
            "salad"               to NutritionEntry("Mixed Salad",         1.5f,   3.5f,  0.5f),
            "lettuce"             to NutritionEntry("Lettuce",             1.4f,   2.9f,  0.2f),
            "tomato"              to NutritionEntry("Tomato",              0.9f,   3.9f,  0.2f),
            "cucumber"            to NutritionEntry("Cucumber",            0.7f,   3.6f,  0.1f),
            "corn"                to NutritionEntry("Corn",                3.3f,  19.0f,  1.4f),
            "potato"              to NutritionEntry("Potato",              2.1f,  17.5f,  0.1f),
            "sweet potato"        to NutritionEntry("Sweet Potato",        1.6f,  20.7f,  0.1f),
            "mushroom"            to NutritionEntry("Mushrooms",           3.1f,   3.3f,  0.3f),
            "onion"               to NutritionEntry("Onion",               1.1f,   9.3f,  0.1f),
            "pepper"              to NutritionEntry("Bell Pepper",         1.0f,   6.0f,  0.3f),
            "zucchini"            to NutritionEntry("Zucchini",            1.2f,   3.1f,  0.3f),
            "cauliflower"         to NutritionEntry("Cauliflower",         1.9f,   5.0f,  0.3f),
            "celery"              to NutritionEntry("Celery",              0.7f,   3.0f,  0.2f),

            // Proteins
            "chicken"             to NutritionEntry("Grilled Chicken Breast", 31.0f,  0.0f,  3.6f),
            "grilled chicken"     to NutritionEntry("Grilled Chicken Breast", 31.0f,  0.0f,  3.6f),
            "salmon"              to NutritionEntry("Salmon Fillet",       22.0f,  0.0f, 13.0f),
            "steak"               to NutritionEntry("Beef Steak",          26.0f,  0.0f, 12.0f),
            "beef"                to NutritionEntry("Ground Beef",         17.2f,  0.0f, 20.0f),
            "pork"                to NutritionEntry("Pork Loin",           22.0f,  0.0f,  9.7f),
            "shrimp"              to NutritionEntry("Shrimp",              24.0f,  0.2f,  0.9f),
            "tuna"                to NutritionEntry("Tuna",                30.0f,  0.0f,  1.0f),
            "egg"                 to NutritionEntry("Eggs",                13.0f,  1.1f, 11.0f),
            "tofu"                to NutritionEntry("Tofu",                 8.0f,  2.0f,  4.0f),
            "turkey"              to NutritionEntry("Turkey Breast",       29.0f,  0.0f,  1.0f),
            "lamb"                to NutritionEntry("Lamb",                25.0f,  0.0f, 21.0f),
            "crab"                to NutritionEntry("Crab",                19.0f,  0.0f,  1.5f),
            "lobster"             to NutritionEntry("Lobster",             19.0f,  0.0f,  1.2f),

            // Grains and carbs
            "rice"                to NutritionEntry("White Rice",           2.7f,  28.0f,  0.3f),
            "pasta"               to NutritionEntry("Pasta",               5.0f,  25.0f,  0.9f),
            "bread"               to NutritionEntry("Whole Grain Bread",    8.0f,  43.0f,  3.5f),
            "pizza"               to NutritionEntry("Pizza Slice",         11.0f,  33.0f, 10.0f),
            "sandwich"            to NutritionEntry("Sandwich",            13.0f,  30.0f,  8.0f),
            "burger"              to NutritionEntry("Burger",              14.0f,  24.0f, 15.0f),
            "hot dog"             to NutritionEntry("Hot Dog",             11.0f,  19.0f, 15.0f),
            "waffle"              to NutritionEntry("Waffles",              7.0f,  38.0f, 11.0f),
            "pancake"             to NutritionEntry("Pancakes",             5.0f,  35.0f,  5.0f),
            "french toast"        to NutritionEntry("French Toast",         7.0f,  31.0f, 11.0f),
            "oatmeal"             to NutritionEntry("Oatmeal",              5.0f,  27.0f,  3.0f),
            "granola"             to NutritionEntry("Granola",              5.0f,  45.0f, 10.0f),
            "cereal"              to NutritionEntry("Cereal",               4.0f,  42.0f,  2.0f),

            // Dairy
            "cheese"              to NutritionEntry("Cheese",              25.0f,   1.3f, 33.0f),
            "yogurt"              to NutritionEntry("Greek Yogurt",        10.0f,   4.0f,  0.4f),
            "ice cream"           to NutritionEntry("Ice Cream",            3.5f,  24.0f, 11.0f),
            "milk"                to NutritionEntry("Milk",                 3.4f,   5.0f,  3.7f),
            "butter"              to NutritionEntry("Butter",               0.9f,   0.1f, 81.0f),

            // Snacks and sweets
            "chocolate"           to NutritionEntry("Dark Chocolate",       5.0f,  46.0f, 30.0f),
            "donut"               to NutritionEntry("Donut",                3.9f,  41.0f, 19.0f),
            "cake"                to NutritionEntry("Cake Slice",           3.9f,  52.0f, 14.0f),
            "cookie"              to NutritionEntry("Cookie",               3.5f,  64.0f, 23.0f),
            "chips"               to NutritionEntry("Potato Chips",         6.6f,  53.0f, 34.0f),
            "french fries"        to NutritionEntry("French Fries",         3.5f,  35.0f, 13.0f),
            "nachos"              to NutritionEntry("Nachos",               7.0f,  43.0f, 24.0f),
            "popcorn"             to NutritionEntry("Popcorn",              3.7f,  74.0f,  4.5f),
            "nuts"                to NutritionEntry("Mixed Nuts",          20.0f,  19.0f, 61.0f),
            "almond"              to NutritionEntry("Almonds",             21.0f,  22.0f, 49.0f),
            "peanut"              to NutritionEntry("Peanuts",             25.0f,  16.0f, 49.0f),
            "honey"               to NutritionEntry("Honey",               0.3f,  82.0f,  0.0f),

            // Prepared dishes
            "soup"                to NutritionEntry("Vegetable Soup",       2.5f,   9.0f,  1.2f),
            "sushi"               to NutritionEntry("Sushi Roll",           4.0f,  28.0f,  0.7f),
            "ramen"               to NutritionEntry("Ramen",               10.0f,  30.0f,  7.0f),
            "tacos"               to NutritionEntry("Tacos",               11.0f,  22.0f, 10.0f),
            "burrito"             to NutritionEntry("Burrito",             11.0f,  34.0f,  7.0f),
            "fried rice"          to NutritionEntry("Fried Rice",           5.0f,  28.0f,  6.0f),
            "curry"               to NutritionEntry("Chicken Curry",       16.0f,  12.0f, 14.0f),
            "hummus"              to NutritionEntry("Hummus",               8.0f,  14.0f,  9.0f),
            "guacamole"           to NutritionEntry("Guacamole",            2.0f,   8.6f, 15.0f),
            "spring roll"         to NutritionEntry("Spring Roll",          4.0f,  22.0f,  7.0f),

            // Drinks
            "juice"               to NutritionEntry("Orange Juice",         0.7f,  10.4f,  0.2f),
            "smoothie"            to NutritionEntry("Fruit Smoothie",       2.0f,  19.0f,  0.5f),
            "coffee"              to NutritionEntry("Black Coffee",         0.1f,   0.0f,  0.0f),
            "tea"                 to NutritionEntry("Green Tea",            0.0f,   0.0f,  0.0f)
        )
    }

    private data class NutritionEntry(
        val displayName: String,
        val protein: Float,
        val carbs: Float,
        val fat: Float
    )
}
