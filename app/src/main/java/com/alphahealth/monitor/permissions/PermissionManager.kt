package com.alphahealth.monitor.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACTIVITY_RECOGNITION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            add(Manifest.permission.BODY_SENSORS)
        }
    }

    /**
     * Checks if all standard runtime permissions are granted.
     */
    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Retrieves the lists of ungranted required permissions.
     */
    fun getUngrantedPermissions(): List<String> {
        return requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Provides a clear clinical rationale for each requested permission to aid user UX.
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> {
                "The camera is required for the local on-device computer vision engine to scan foods and extract dietary profiles."
            }
            Manifest.permission.BODY_SENSORS -> {
                "Body sensors access is required to read raw high-frequency biometric streams (such as EDA, PPG) directly from your wearable devices."
            }
            Manifest.permission.ACTIVITY_RECOGNITION -> {
                "Activity recognition is used to track motion contexts and gait kinematics, ensuring accurate calorie calculations."
            }
            else -> "Required for core homeostatic recovery and health metrics tracking."
        }
    }
}
