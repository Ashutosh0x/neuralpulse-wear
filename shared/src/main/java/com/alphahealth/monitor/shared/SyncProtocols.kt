package com.alphahealth.monitor.shared

object SyncProtocols {
    // Wearable Data Layer endpoint paths
    const val PATH_WATCH_TELEMETRY = "/telemetry/biometrics"
    const val PATH_WATCH_SQI       = "/telemetry/sqi"
    const val PATH_PHONE_COMMANDS  = "/commands/vision_triggers"
    
    // Telemetry payload keys
    const val KEY_EDA_CONDUCTANCE  = "eda_conductance"
    const val KEY_HEART_RATE        = "heart_rate"
    const val KEY_BIA_HYDRATION    = "bia_hydration"
    
    // Mobile response alerts
    const val KEY_GLYCEMIC_WARNING = "glycemic_risk_alert"
    const val KEY_ENERGY_SCORE     = "energy_score"
}
