package com.victorgomes.geofenceapp.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.victorgomes.geofenceapp.utils.NotificationHelper

/**
 * Foreground service that actively requests location updates. This serves two purposes:
 * 1. Keeps the app process alive with high priority so geofence broadcasts are delivered reliably.
 * 2. Keeps location "warm" — without an active location request from the app, Google Play Services
 *    geofencing can become unreliable when the device is idle or battery-optimised.
 */
class GeofenceMonitorService : Service() {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            // Updates are consumed by Google Play Services geofencing internally.
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NotificationHelper.MONITORING_NOTIFICATION_ID,
            NotificationHelper.buildMonitoringNotification(this)
        )

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            LOCATION_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MS / 2)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val LOCATION_INTERVAL_MS = 10_000L  // update every 10 s
    }
}
