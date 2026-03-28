package com.victorgomes.geofenceapp.ui.map

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.victorgomes.geofenceapp.data.database.GeofenceConfigEntity
import com.victorgomes.geofenceapp.data.repository.GeofenceRepository
import com.victorgomes.geofenceapp.geofence.GeofenceManager
import com.victorgomes.geofenceapp.service.GeofenceMonitorService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GeofenceRepository(application)
    val geofenceManager = GeofenceManager(application)

    val fenceConfigs = repository.allFenceConfigs.asLiveData()

    /** fenceId → ENTER timestamp for fences whose last logged event is ENTER. */
    val enterTimestamps: LiveData<Map<String, Long>> = repository.latestEventPerFence
        .map { events ->
            events.filter { it.eventType == "ENTER" }.associate { it.geofenceId to it.timestamp }
        }
        .asLiveData()

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    @SuppressLint("MissingPermission")
    fun moveFence(id: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val config = repository.getFenceById(id) ?: return@launch
            repository.updateFencePosition(id, latitude, longitude)
            if (config.isActive) {
                geofenceManager.removeGeofence(id)
                geofenceManager.addGeofence(
                    id = id,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = config.radiusMeters
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun createFence(name: String, latitude: Double, longitude: Double, radiusMeters: Float, markerIcon: String = "pin") {
        val config = GeofenceConfigEntity(
            name = name,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            isActive = true,
            markerIcon = markerIcon
        )
        geofenceManager.addGeofence(
            id = config.id,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            onSuccess = {
                viewModelScope.launch {
                    repository.insertFenceConfig(config)
                    startServiceIfNeeded()
                    _statusMessage.postValue("Fence \"$name\" created and active.")
                }
            },
            onFailure = { e ->
                _statusMessage.postValue("Failed to activate fence: ${e.message}")
            }
        )
    }

    private suspend fun startServiceIfNeeded() {
        val hasActive = repository.getActiveConfigs().isNotEmpty()
        val intent = Intent(getApplication(), GeofenceMonitorService::class.java)
        if (hasActive) {
            getApplication<Application>().startForegroundService(intent)
        }
    }
}
