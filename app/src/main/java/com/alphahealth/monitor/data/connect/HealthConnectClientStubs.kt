package androidx.health.connect.client

import android.content.Context
import androidx.health.connect.client.features.HealthConnectFeatures
import androidx.health.connect.client.records.Record

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.FILE)
annotation class ExperimentalHealthConnectApi

interface HealthConnectClient {
    val features: HealthConnectFeatures

    suspend fun insertRecords(records: List<Record>)

    companion object {
        const val SDK_AVAILABLE = 1
        const val SDK_UNAVAILABLE = 0

        fun getSdkStatus(context: Context): Int {
            return SDK_AVAILABLE
        }

        fun getOrCreate(context: Context): HealthConnectClient {
            return object : HealthConnectClient {
                override val features: HealthConnectFeatures = object : HealthConnectFeatures {
                    override fun getFeatureStatus(feature: Int): Int {
                        return HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
                    }
                }

                override suspend fun insertRecords(records: List<Record>) {
                    // Stub implementation
                }
            }
        }
    }
}
