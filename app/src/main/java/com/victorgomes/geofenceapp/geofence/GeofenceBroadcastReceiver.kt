package com.victorgomes.geofenceapp.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error: $errorMessage")
            return
        }

        val eventType = when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> return
        }
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        // For each triggered fence, enqueue a 30-second confirmation worker.
        // The worker samples GPS during that window and only logs the event if
        // the final position and ≥50 samples agree with the transition type.
        val detectedAt = System.currentTimeMillis()
        triggeringGeofences.forEach { geofence ->
            Log.d(TAG, "Transition detected: $eventType for ${geofence.requestId} — starting confirmation")
            val workData = workDataOf(
                TransitionConfirmationWorker.KEY_FENCE_ID  to geofence.requestId,
                TransitionConfirmationWorker.KEY_TRANSITION to eventType,
                TransitionConfirmationWorker.KEY_TIMESTAMP  to detectedAt
            )
            val request = OneTimeWorkRequestBuilder<TransitionConfirmationWorker>()
                .setInputData(workData)
                .build()
            // Unique name per fence+direction: if the same transition fires twice
            // before the first worker finishes (e.g. double re-registration), the
            // second enqueue is silently dropped instead of queuing a duplicate.
            WorkManager.getInstance(context).enqueueUniqueWork(
                "confirm_${geofence.requestId}_$eventType",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
