package com.victorgomes.geofenceapp.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * Receives activity transition events from Play Services and exposes a static [isStill] flag
 * that [com.victorgomes.geofenceapp.service.GeofenceMonitorService] reads to adapt the
 * watchdog interval (Plan B).
 */
class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        for (event in result.transitionEvents) {
            if (event.activityType == DetectedActivity.STILL) {
                isStill = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
                Log.d(TAG, "Activity transition: still=$isStill")
            }
        }
    }

    companion object {
        private const val TAG = "ActivityTransitionRcvr"

        /**
         * True while the device is confirmed stationary. Written by [ActivityTransitionReceiver]
         * on the main thread; read by the watchdog handler — volatile is sufficient.
         */
        @Volatile
        var isStill = false
            private set
    }
}
