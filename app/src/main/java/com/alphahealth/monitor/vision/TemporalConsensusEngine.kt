package com.alphahealth.monitor.vision

class TemporalConsensusEngine(
    private val windowSize: Int = 3,
    private val requiredAgreement: Int = 2
) {
    private val predictionHistory = mutableListOf<String>()

    /**
     * Integrates a new prediction and checks if a consensus has been reached.
     * @param name The prediction item name.
     * @return The item name if a consensus is reached, null otherwise.
     */
    fun addPredictionAndCheckConsensus(name: String): String? {
        if (predictionHistory.size >= windowSize) {
            predictionHistory.removeAt(0)
        }
        predictionHistory.add(name)

        val frequencyMap = predictionHistory.groupingBy { it }.eachCount()
        val mostFrequent = frequencyMap.maxByOrNull { it.value }

        return if (mostFrequent != null && mostFrequent.value >= requiredAgreement) {
            mostFrequent.key
        } else {
            null
        }
    }

    /**
     * Resets the prediction history.
     */
    fun clearHistory() {
        predictionHistory.clear()
    }
}
