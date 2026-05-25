package com.alphahealth.monitor.data.connect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.MedicalRecord // New Android 16 Client API
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthConnectFhirOrchestrator(private val context: Context) {

    private val healthConnectClient by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    /**
     * Commits pristine, Butterworth-validated telemetry logs straight into the local 
     * Android 16 Health Connect data layer using standard FHIR JSON format blocks.
     */
    suspend fun exportValidatedTelemetryToFhir(fhirJsonPayload: String) = withContext(Dispatchers.IO) {
        if (healthConnectClient == null) return@withContext

        try {
            val observationRecord = MedicalRecord.Builder()
                .setFhirVersion("R4")
                .setFhirResourceCategory(MedicalRecord.CATEGORY_LABORATORY)
                .setPayload(fhirJsonPayload) // Encrypted raw observation data standard
                .build()

            healthConnectClient?.insertRecords(listOf(observationRecord))
            Log.i("AlphaConnect", "Successfully exported verified health telemetry into system FHIR framework.")
        } catch (e: Exception) {
            Log.e("AlphaConnect", "System rejection on FHIR data entry pipeline: ${e.message}")
        }
    }
}
