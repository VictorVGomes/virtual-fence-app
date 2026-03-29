package com.victorgomes.geofenceapp.geofence

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.HandlerThread
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.victorgomes.geofenceapp.data.database.GeofenceConfigEntity
import com.victorgomes.geofenceapp.data.database.GeofenceEventEntity
import com.victorgomes.geofenceapp.data.repository.GeofenceRepository
import com.victorgomes.geofenceapp.utils.NotificationHelper
import com.victorgomes.geofenceapp.utils.PersonalizationPrefs
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class TransitionConfirmationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_FENCE_ID = "fence_id"
        const val KEY_TRANSITION = "transition"

        // Collect samples for up to 45 s at 3 s intervals (≈15 samples max).
        private const val SAMPLE_INTERVAL_MS = 3_000L
        private const val CONFIRMATION_WINDOW_MS = 45_000L

        // Minimum number of accurate, confirming samples required to log an event.
        // Kept small (3) so a real crossing is confirmed quickly without being blocked
        // by a strict count requirement.
        private const val MIN_CONFIRMING_SAMPLES = 3

        private const val TAG = "ConfirmationWorker"
    }

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val fenceId    = inputData.getString(KEY_FENCE_ID)   ?: return Result.failure()
        val transition = inputData.getString(KEY_TRANSITION) ?: return Result.failure()
        val isEnter    = transition == "ENTER"

        val repository = GeofenceRepository(applicationContext)
        val fence = repository.getFenceById(fenceId) ?: run {
            Log.w(TAG, "Fence $fenceId not found — discarding $transition")
            return Result.failure()
        }

        // ── Fast-fail: state-consistency check ───────────────────────────────
        // Valid transitions follow the state machine:  (none) → ENTER → EXIT → ENTER → …
        //   • ENTER is only valid when the last event was EXIT or there is no event yet.
        //   • EXIT is only valid when the last event was ENTER.
        // Anything outside this sequence is discarded immediately — it means either a
        // duplicate trigger (re-registration ghost) or an EXIT fired for a fence the
        // user was never recorded as entering.
        val lastEvent = repository.getLastEventForFence(fenceId)
        val lastType  = lastEvent?.eventType   // null → no events yet

        val stateViolation = when (transition) {
            "ENTER" -> lastType == "ENTER"          // already inside
            "EXIT"  -> lastType != "ENTER"          // not inside (null = never entered, or last was EXIT)
            else    -> true
        }
        if (stateViolation) {
            Log.d(TAG, "$transition discarded — state violation for ${fence.name} (last=$lastType)")
            return Result.success()
        }

        // ── Collect location samples ──────────────────────────────────────────
        val fusedClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, SAMPLE_INTERVAL_MS
        ).setMinUpdateIntervalMillis(SAMPLE_INTERVAL_MS / 2).build()

        // Deliver callbacks on a background thread so GPS fixes don't wake the main looper.
        val handlerThread = HandlerThread("ConfirmationWorkerLooper").also { it.start() }
        val locationFlow = callbackFlow<Location> {
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { trySend(it) }
                }
            }
            fusedClient.requestLocationUpdates(locationRequest, callback, handlerThread.looper)
            awaitClose {
                fusedClient.removeLocationUpdates(callback)
                handlerThread.quit()
            }
        }

        val samples = mutableListOf<Location>()
        withTimeoutOrNull(CONFIRMATION_WINDOW_MS) {
            locationFlow.collect { samples.add(it) }
        }

        Log.d(TAG, "Collected ${samples.size} samples for $transition on fence ${fence.name}")

        if (samples.isEmpty()) {
            Log.w(TAG, "No samples collected — discarding $transition on ${fence.name}")
            return Result.success()
        }

        // ── Accuracy-aware confirmation ───────────────────────────────────────
        // Only use samples whose horizontal accuracy is within 1.5× the fence
        // radius. Samples with worse accuracy than this cannot reliably determine
        // inside vs outside, but the 1.5× factor avoids throwing away every
        // sample when the fence is small (e.g. 100 m fence, 120 m accuracy fix).
        val usableSamples = samples.filter { it.accuracy <= fence.radiusMeters * 1.5f }
        Log.d(TAG, "${usableSamples.size}/${samples.size} samples within accuracy threshold")

        if (usableSamples.isEmpty()) {
            Log.d(TAG, "$transition discarded — no samples within accuracy threshold")
            return Result.success()
        }

        // The most recent usable sample must confirm the transition direction.
        val lastInside = isInsideFence(usableSamples.last().latitude, usableSamples.last().longitude, fence)
        if (lastInside != isEnter) {
            Log.d(TAG, "$transition discarded — final position does not confirm (inside=$lastInside)")
            return Result.success()
        }

        // At least MIN_CONFIRMING_SAMPLES usable samples must agree on the direction.
        // This rejects spurious events caused by a momentary jitter fix: if the user
        // is actually stationary on the other side, the majority of the 45-second
        // window will contradict the transition and the event will be discarded.
        val confirming = usableSamples.filter { isInsideFence(it.latitude, it.longitude, fence) == isEnter }
        if (confirming.size < MIN_CONFIRMING_SAMPLES) {
            Log.d(TAG, "$transition discarded — only ${confirming.size} confirming samples (need $MIN_CONFIRMING_SAMPLES)")
            return Result.success()
        }

        // Use the first sample of the final confirming run as the event timestamp/location.
        var runStart = 0; var inRun = false
        for (i in usableSamples.indices) {
            val ok = isInsideFence(usableSamples[i].latitude, usableSamples[i].longitude, fence) == isEnter
            if (ok && !inRun) { runStart = i; inRun = true } else if (!ok) { inRun = false }
        }
        val eventSample = usableSamples[runStart]

        Log.d(TAG, "$transition confirmed (${confirming.size}/${usableSamples.size} usable samples) for fence ${fence.name}")

        // ── Persist and notify ────────────────────────────────────────────────
        repository.insertEvent(
            GeofenceEventEntity(
                timestamp  = eventSample.time,
                eventType  = transition,
                geofenceId = fenceId,
                latitude   = eventSample.latitude,
                longitude  = eventSample.longitude
            )
        )
        if (PersonalizationPrefs.isEventNotificationsEnabled(applicationContext)) {
            NotificationHelper.showGeofenceNotification(applicationContext, transition, fence.name)
        }
        return Result.success()
    }

    /** Haversine distance — returns true if (lat, lon) is within the fence radius. */
    private fun isInsideFence(lat: Double, lon: Double, fence: GeofenceConfigEntity): Boolean {
        val R    = 6_371_000.0
        val dLat = Math.toRadians(lat - fence.latitude)
        val dLon = Math.toRadians(lon - fence.longitude)
        val a    = sin(dLat / 2).pow(2) +
                   cos(Math.toRadians(fence.latitude)) * cos(Math.toRadians(lat)) * sin(dLon / 2).pow(2)
        return R * 2 * asin(sqrt(a)) <= fence.radiusMeters
    }
}
