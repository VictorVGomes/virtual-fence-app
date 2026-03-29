package com.victorgomes.geofenceapp.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.victorgomes.geofenceapp.MainActivity
import com.victorgomes.geofenceapp.R

object NotificationHelper {

    private const val CHANNEL_EVENTS_ID = "geofence_events"
    private const val CHANNEL_MONITORING_ID = "geofence_monitoring"
    private const val EVENT_NOTIFICATION_ID = 2001
    const val MONITORING_NOTIFICATION_ID = 2002
    const val EXTRA_NAVIGATE_TO_LOG = "navigate_to_log"

    // Distinct request codes so the two PendingIntents are never considered the same
    // by the system. If they share a code, FLAG_UPDATE_CURRENT on one overwrites the
    // other's extras — e.g. tapping the monitoring notification would also navigate to
    // the log after a fence-event notification had been shown.
    private const val EVENT_PENDING_INTENT_RC      = 101
    private const val MONITORING_PENDING_INTENT_RC = 102

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            CHANNEL_EVENTS_ID,
            "Fence Crossings",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when you enter or exit the geofence"
            manager.createNotificationChannel(this)
        }

        NotificationChannel(
            CHANNEL_MONITORING_ID,
            "Active Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shown while geofence monitoring is active"
            manager.createNotificationChannel(this)
        }
    }

    fun showGeofenceNotification(context: Context, eventType: String, fenceName: String = "") {
        val isEnter = eventType == "ENTER"
        val title = if (isEnter) "Entered fence zone" else "Left fence zone"
        val message = if (fenceName.isNotEmpty()) {
            if (isEnter) "You entered \"$fenceName\"." else "You left \"$fenceName\"."
        } else {
            if (isEnter) "You have entered your monitored location." else "You have left your monitored location."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO_LOG, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, EVENT_PENDING_INTENT_RC, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS_ID)
            .setSmallIcon(R.drawable.ic_fence)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(EVENT_NOTIFICATION_ID, notification)
    }

    fun buildMonitoringNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, MONITORING_PENDING_INTENT_RC, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_MONITORING_ID)
            .setSmallIcon(R.drawable.ic_fence)
            .setContentTitle("Geofence Active")
            .setContentText("Monitoring your location fence in the background.")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
