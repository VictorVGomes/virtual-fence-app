package com.victorgomes.geofenceapp.geofence

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.victorgomes.geofenceapp.data.repository.GeofenceRepository
import com.victorgomes.geofenceapp.service.GeofenceMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        CoroutineScope(Dispatchers.IO).launch {
            val repository = GeofenceRepository(context)
            val activeConfigs = repository.getActiveConfigs()
            if (activeConfigs.isEmpty()) return@launch

            val manager = GeofenceManager(context)
            activeConfigs.forEach { config ->
                manager.addGeofence(
                    id = config.id,
                    latitude = config.latitude,
                    longitude = config.longitude,
                    radiusMeters = config.radiusMeters
                )
            }
            context.startForegroundService(Intent(context, GeofenceMonitorService::class.java))
        }
    }
}
