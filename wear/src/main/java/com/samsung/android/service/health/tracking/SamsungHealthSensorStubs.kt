package com.samsung.android.service.health.tracking

import android.content.Context
import com.samsung.android.service.health.tracking.data.HealthTrackerType

class HealthTrackingService(private val listener: ConnectionListener, private val context: Context) {

    fun connectService() {
        listener.onServiceConnected()
    }

    fun disconnectService() {
        listener.onServiceDisconnected()
    }

    fun getTracker(trackerType: HealthTrackerType): HealthTracker {
        return HealthTracker()
    }

    interface ConnectionListener {
        fun onServiceConnected()
        fun onServiceDisconnected()
        fun onConnectionFailed(exception: Exception?)
    }
}

class HealthTracker {
    fun setBatchProcessingGroup(ms: Int) {}
    fun setEventListener(listener: TrackerEventListener) {}
    fun startTracking() {}
    fun stopTracking() {}

    interface TrackerEventListener {
        fun onDataReceived(dataPoints: List<com.samsung.android.service.health.tracking.data.DataPoint>)
        fun onFlushCompleted()
        fun onError(error: TrackerError?)
    }

    class TrackerError {
        // Stub
    }
}
