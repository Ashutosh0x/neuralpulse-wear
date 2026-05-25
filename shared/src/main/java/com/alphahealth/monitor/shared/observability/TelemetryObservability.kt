package com.alphahealth.monitor.shared.observability

import android.util.Log

object TelemetryObservability {

    private const val GLOBAL_TAG = "NeuralPulseTelemetry"

    /**
     * Dispatches runtime exceptions to error reporting services (Firebase Crashlytics, Sentry)
     * and fallback local log structures.
     */
    fun logException(tag: String, throwable: Throwable, contextInfo: String? = null) {
        val message = "[FATAL/ERROR] [Tag: $tag] ${contextInfo?.let { "Context: $it | " } ?: ""}Msg: ${throwable.message}"
        Log.e(GLOBAL_TAG, message, throwable)
        
        // Sentry production integration hook:
        // Sentry.captureException(throwable)
        
        // Crashlytics production integration hook:
        // FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    /**
     * Logs general operational telemetry status.
     */
    fun logEvent(tag: String, message: String) {
        Log.i(GLOBAL_TAG, "[INFO] [Tag: $tag] $message")
        
        // Analytics production integration hook:
        // FirebaseAnalytics.getInstance().logEvent(...)
    }

    /**
     * Logs non-fatal warning diagnostics.
     */
    fun logWarning(tag: String, message: String) {
        Log.w(GLOBAL_TAG, "[WARN] [Tag: $tag] $message")
    }
}
