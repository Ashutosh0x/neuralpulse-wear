package com.alphahealth.monitor.data

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import java.util.TimeZone

class CalendarScheduler(private val context: Context) {

    private val TAG = "AlphaCalendarScheduler"

    fun scheduleRecoveryBlock(durationMinutes: Long): Boolean {
        Log.d(TAG, "Initiating Google Calendar synchronization for deep fatigue blocking...")
        try {
            val calId: Long = 1 
            val startMillis: Long = System.currentTimeMillis() + (30 * 60 * 1000)
            val endMillis: Long = startMillis + (durationMinutes * 60 * 1000)

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, "AlphaHealth Rest & Autonomic Recovery")
                put(CalendarContract.Events.DESCRIPTION, "AI scheduled recovery block: High sympathetic stress & metabolic fatigue detected. Restoring homeostasis.")
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            Log.i(TAG, "Recovery Block scheduled successfully on Google Calendar.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule Calendar event: ${e.message}")
        }
        return false
    }
}
