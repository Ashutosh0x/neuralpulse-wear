package com.samsung.android.service.health.tracking.data

enum class HealthTrackerType {
    EDA, MF_BIA, PPG
}

class DataPoint {
    enum class Key {
        EDA_STATUS, ICW, ECW, PPG_GREEN
    }

    fun getValue(key: Key): Any? {
        return when (key) {
            Key.EDA_STATUS -> 1.8f
            Key.ICW -> 20.0f
            Key.ECW -> 12.0f
            Key.PPG_GREEN -> 2048
        }
    }
}
