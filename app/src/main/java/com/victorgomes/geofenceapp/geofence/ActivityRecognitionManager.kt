package com.victorgomes.geofenceapp.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.victorgomes.geofenceapp.utils.PermissionHelper

object ActivityRecognitionManager {

    private const val TAG          = "ActivityRecognitionMgr"
    private const val REQUEST_CODE = 1002

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (!PermissionHelper.hasActivityRecognitionPermission(context)) return

        val transitions = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(
                ActivityTransitionRequest(transitions),
                pendingIntent(context)
            )
            .addOnSuccessListener { Log.d(TAG, "Transition updates registered") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to register transitions: ${e.message}") }
    }

    fun stop(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        ActivityRecognition.getClient(context)
            .removeActivityTransitionUpdates(pendingIntent(context))
            .addOnFailureListener { e -> Log.e(TAG, "Failed to remove transitions: ${e.message}") }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
