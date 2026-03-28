package com.victorgomes.geofenceapp.ui.fences

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.victorgomes.geofenceapp.data.database.FenceEventCount
import com.victorgomes.geofenceapp.data.database.GeofenceConfigEntity
import com.victorgomes.geofenceapp.data.repository.GeofenceRepository
import com.victorgomes.geofenceapp.geofence.GeofenceManager
import com.victorgomes.geofenceapp.service.GeofenceMonitorService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FencesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GeofenceRepository(application)
    private val geofenceManager = GeofenceManager(application)

    val fences = repository.allFenceConfigs.asLiveData()

    val eventCounts: LiveData<Map<String, Int>> = repository.eventCountsPerFence
        .map { list -> list.associate { it.geofenceId to it.count } }
        .asLiveData()

    /**
     * Maps fenceId → the timestamp of the ENTER event that started the current stay.
     * A fence is considered "currently inside" when its most recent event is ENTER.
     * Null / absent means the user is outside (last event was EXIT or no events yet).
     */
    val insideStatus: LiveData<Map<String, Long>> = repository.latestEventPerFence
        .map { events ->
            events
                .filter { it.eventType == "ENTER" }
                .associate { it.geofenceId to it.timestamp }
        }
        .asLiveData()

    @SuppressLint("MissingPermission")
    fun toggleFence(config: GeofenceConfigEntity, activate: Boolean) {
        viewModelScope.launch {
            if (activate) {
                geofenceManager.addGeofence(
                    id = config.id,
                    latitude = config.latitude,
                    longitude = config.longitude,
                    radiusMeters = config.radiusMeters,
                    onSuccess = {
                        viewModelScope.launch {
                            repository.updateFenceActive(config.id, true)
                            updateServiceState()
                        }
                    },
                    onFailure = {}
                )
            } else {
                geofenceManager.removeGeofence(
                    id = config.id,
                    onSuccess = {
                        viewModelScope.launch {
                            repository.updateFenceActive(config.id, false)
                            updateServiceState()
                        }
                    }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun editFence(config: GeofenceConfigEntity, newName: String, newRadius: Float, newIcon: String) {
        viewModelScope.launch {
            repository.updateFenceNameAndRadius(config.id, newName, newRadius)
            repository.updateFenceMarkerIcon(config.id, newIcon)
            if (config.isActive) {
                geofenceManager.removeGeofence(config.id)
                geofenceManager.addGeofence(
                    id = config.id,
                    latitude = config.latitude,
                    longitude = config.longitude,
                    radiusMeters = newRadius
                )
            }
        }
    }

    fun deleteFence(config: GeofenceConfigEntity) {
        viewModelScope.launch {
            geofenceManager.removeGeofence(id = config.id)
            repository.deleteFenceConfig(config.id)
            updateServiceState()
        }
    }

    private suspend fun updateServiceState() {
        val hasActive = repository.getActiveConfigs().isNotEmpty()
        val intent = Intent(getApplication(), GeofenceMonitorService::class.java)
        if (hasActive) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().stopService(intent)
        }
    }
}
