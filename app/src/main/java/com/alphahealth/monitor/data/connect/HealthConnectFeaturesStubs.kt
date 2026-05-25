package androidx.health.connect.client.features

interface HealthConnectFeatures {
    fun getFeatureStatus(feature: Int): Int

    companion object {
        const val FEATURE_PERSONAL_HEALTH_RECORD = 1001
        const val FEATURE_STATUS_AVAILABLE = 1
        const val FEATURE_STATUS_UNAVAILABLE = 0
    }
}
