package com.victorgomes.geofenceapp.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(
        id: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            // The confirmation worker waits 45 s before logging an event, so a
            // 30-second delivery delay is imperceptible end-to-end but meaningfully
            // reduces OS wake-ups from the geofencing subsystem.
            .setNotificationResponsiveness(30_000)
            .build()

        val request = GeofencingRequest.Builder()
            // No initial trigger — we don't want to start a confirmation job just
            // because the device was already inside a fence at registration time.
            .setInitialTrigger(0)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Geofence added: $id")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to add geofence $id: ${e.message}")
                onFailure(e)
            }
    }

    fun removeGeofence(
        id: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        geofencingClient.removeGeofences(listOf(id))
            .addOnSuccessListener {
                Log.d(TAG, "Geofence removed: $id")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofence $id: ${e.message}")
                onFailure(e)
            }
    }

    fun removeAllGeofences(onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    companion object {
        private const val TAG = "GeofenceManager"
        private const val REQUEST_CODE = 1001
    }
}
