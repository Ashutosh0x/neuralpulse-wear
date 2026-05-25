package com.alphahealth.monitor.wear.surfaces

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alphahealth.monitor.wear.presentation.WatchDashboardActivity

class OngoingActivityHelper(private val context: Context) {

    private val TAG = "AlphaOngoingActivity"
    private val CHANNEL_ID = "alpha_live_tracking_channel"
    private val NOTIFICATION_ID = 4096

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "NeuralPulse Live Updates"
            val descriptionText = "Continuous on-wrist biometric sensor telemetry feeds"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Publishes a Wear OS 7 Live Update widget layout.
     * Integrates with the watch face status bar and ongoing activity overlays.
     */
    fun startLiveTrackingNotification(edaValue: Float, pulseRate: Int) {
        Log.d(TAG, "Registering Wear OS 7 Live Updates telemetry package...")

        val activityIntent = Intent(context, WatchDashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Compile Wear OS Ongoing Activity notification tags
        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("NeuralPulse Tracking")
            .setContentText("Stress: ${String.format("%.2f", edaValue)} uS | HR: $pulseRate BPM")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle())
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.i(TAG, "Wear OS 7 Live Activity badge synchronized successfully.")
    }

    fun cancelTrackingNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "Wear OS 7 Live Activity badge cancelled.")
    }
}
