package com.alphahealth.monitor.data

import android.content.Context
import android.util.Log

data class GeminiAppFunctionResponse(
    val functionName: String,
    val vocalBreakdown: String,
    val recommendationText: String,
    val statusSuccess: Boolean
)

class GeminiAppFunctions(private val context: Context) {

    private val TAG = "AlphaGeminiAgent"

    fun checkPhysicalRecoveryStatus(vulnerabilityIndex: Int): GeminiAppFunctionResponse {
        Log.d(TAG, "Gemini AppFunctions API triggered.")

        val vocalSummary = when {
            vulnerabilityIndex >= 70 -> {
                "Your physical systems are under elevated strain today. Your stress readings are high and hydration is low. I recommend taking a break, drinking some water, and deferring intense workloads."
            }
            vulnerabilityIndex >= 40 -> {
                "You are under mild strain. Yesterday's low sleep score is affecting recovery. I have adjusted your calendar and recommend a light walk."
            }
            else -> {
                "Your biometrics look fantastic today. Homeostasis is maintained. You are fully charged for deep work."
            }
        }

        val actionText = if (vulnerabilityIndex >= 40) {
            "Schedule rest block and adjust evening workout target."
        } else {
            "Maintain current training targets."
        }

        return GeminiAppFunctionResponse(
            functionName = "checkPhysicalRecoveryStatus",
            vocalBreakdown = vocalSummary,
            recommendationText = actionText,
            statusSuccess = true
        )
    }
}
