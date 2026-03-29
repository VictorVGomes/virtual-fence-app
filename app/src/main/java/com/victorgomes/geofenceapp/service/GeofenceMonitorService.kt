package com.victorgomes.geofenceapp.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.victorgomes.geofenceapp.data.repository.GeofenceRepository
import com.victorgomes.geofenceapp.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps Play Services geofencing responsive.
 *
 * Battery strategy:
 *
 * A — Passive-first location: the primary request uses PRIORITY_PASSIVE, which piggybacks on
 *     fixes already requested by other apps and costs the radio nothing on its own. A watchdog
 *     fires a short active burst (PRIORITY_BALANCED) only when no passive fix has arrived for
 *     [PASSIVE_STALE_MS], ensuring geofencing stays warm even on quiet devices.
 *
 * E — Background looper: GPS callbacks are delivered on a dedicated HandlerThread instead of
 *     the main looper, avoiding unnecessary UI-thread wake-ups on every fix.
 *
 * Note — Plan B (activity-aware watchdog cadence) requires the separate
 *     play-services-activity-recognition artifact. ActivityTransitionReceiver is already
 *     written and ready; add the dependency and wire it up here when needed.
 */
class GeofenceMonitorService : Service() {

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    // Dedicated background thread for GPS callbacks (Plan E).
    private val locationThread = HandlerThread("GeofenceLocationThread")

    // ElapsedRealtime timestamp of the last fix received from the passive request.
    private var lastFixElapsed = 0L

    // Updates lastFixElapsed on every GPS delivery; no other work needed here.
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            lastFixElapsed = SystemClock.elapsedRealtime()
        }
    }

    // Watchdog: if passive mode has gone quiet, fire a brief active burst to warm up
    // geofencing, then revert to passive (Plan A).
    private val watchdogHandler = Handler(Looper.getMainLooper())
    private val watchdogRunnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            val age = SystemClock.elapsedRealtime() - lastFixElapsed
            if (lastFixElapsed == 0L || age > PASSIVE_STALE_MS) {
                Log.d(TAG, "No passive fix for ${age / 1000}s — active burst for ${ACTIVE_BURST_MS / 1000}s")
                requestUpdates(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                watchdogHandler.postDelayed(
                    { requestUpdates(Priority.PRIORITY_PASSIVE) },
                    ACTIVE_BURST_MS
                )
            }
            watchdogHandler.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NotificationHelper.MONITORING_NOTIFICATION_ID,
            NotificationHelper.buildMonitoringNotification(this)
        )

        // Guard against double-start on re-delivery of START_STICKY.
        if (!locationThread.isAlive) locationThread.start()

        // Begin passive location monitoring.
        requestUpdates(Priority.PRIORITY_PASSIVE)
        // Seed lastFixElapsed so the first watchdog cycle doesn't immediately burst.
        lastFixElapsed = SystemClock.elapsedRealtime()
        watchdogHandler.removeCallbacks(watchdogRunnable)
        watchdogHandler.postDelayed(watchdogRunnable, WATCHDOG_INTERVAL_MS)

        // START_STICKY gap fix: when the OS restarts a killed service, verify that active
        // fences still exist — stop immediately if they don't.
        CoroutineScope(Dispatchers.IO).launch {
            if (GeofenceRepository(applicationContext).getActiveConfigs().isEmpty()) {
                Log.d(TAG, "No active fences on (re)start — stopping service")
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        watchdogHandler.removeCallbacksAndMessages(null)
        locationThread.quit()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun requestUpdates(priority: Int) {
        fusedClient.removeLocationUpdates(locationCallback)
        val request = LocationRequest.Builder(priority, PASSIVE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(PASSIVE_INTERVAL_MS / 2)
            .build()
        fusedClient.requestLocationUpdates(request, locationCallback, locationThread.looper)
    }

    companion object {
        private const val TAG = "GeofenceMonitorService"

        // Interval hint passed to the passive request (the OS may use a different cadence).
        private const val PASSIVE_INTERVAL_MS = 10_000L

        // Trigger an active burst if no passive fix arrives within this window.
        private const val PASSIVE_STALE_MS    = 5 * 60 * 1000L   // 5 min

        // Duration of the active burst before returning to passive.
        private const val ACTIVE_BURST_MS     = 20_000L           // 20 s

        // How often the watchdog checks for a stale fix.
        private const val WATCHDOG_INTERVAL_MS = 3 * 60 * 1000L  // 3 min
    }
}
