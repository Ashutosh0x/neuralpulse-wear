package com.samsung.android.health.data

import android.content.Context
import com.samsung.android.health.data.error.HealthConnectionErrorResult

class HealthDataStore private constructor(context: Context) {

    fun connectService(listener: ConnectionListener) {
        listener.onConnected()
    }

    fun getPermissionManager(): PermissionManager {
        return PermissionManager()
    }

    interface ConnectionListener {
        fun onConnected()
        fun onConnectionFailed(errorResult: HealthConnectionErrorResult)
        fun onDisconnected()
    }

    companion object {
        fun getStore(context: Context): HealthDataStore {
            return HealthDataStore(context)
        }
    }
}

class PermissionManager {
    fun getPermissionStatus(dataTypes: Set<String>): PermissionStatus {
        return PermissionStatus()
    }
}

class PermissionStatus {
    fun isAllGranted(): Boolean = true
}
