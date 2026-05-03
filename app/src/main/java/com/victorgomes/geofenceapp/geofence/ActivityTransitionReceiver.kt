package com.victorgomes.geofenceapp.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.victorgomes.geofenceapp.data.repository.GeofenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives STILL_ENTER / STILL_EXIT from the Activity Recognition API.
 *
 * Two responsibilities:
 *  1. Keep the static [isStill] flag up-to-date so the watchdog in
 *     [com.victorgomes.geofenceapp.service.GeofenceMonitorService] can adapt its burst cadence.
 *  2. Re-register every active geofence with a responsiveness that matches the motion state —
 *     [GeofenceManager.RESPONSIVENESS_STILL] when stationary, [GeofenceManager.RESPONSIVENESS_NORMAL]
 *     when moving. Play Services treats same-ID registration as an in-place update, so no remove
 *     step is needed.
 */
class ActivityTransitionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return

        for (event in result.transitionEvents) {
            if (event.activityType != DetectedActivity.STILL) continue
            isStill = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
            Log.d(TAG, "Activity transition: still=$isStill")
        }

        val responsiveness = if (isStill) GeofenceManager.RESPONSIVENESS_STILL
                             else         GeofenceManager.RESPONSIVENESS_NORMAL
        val appContext = context.applicationContext

        scope.launch {
            val configs  = GeofenceRepository(appContext).getActiveConfigs()
            val manager  = GeofenceManager(appContext)
            for (config in configs) {
                manager.addGeofence(
                    id               = config.id,
                    latitude         = config.latitude,
                    longitude        = config.longitude,
                    radiusMeters     = config.radiusMeters,
                    responsivenessMs = responsiveness
                )
            }
            Log.d(TAG, "Re-registered ${configs.size} fence(s) at ${responsiveness}ms responsiveness")
        }
    }

    companion object {
        private const val TAG = "ActivityTransitionRcvr"

        /**
         * True while the device is confirmed stationary. Written on the main thread by this
         * receiver; read by the watchdog handler — volatile is sufficient.
         */
        @Volatile
        var isStill = false
            private set
    }
}
