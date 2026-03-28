package com.victorgomes.geofenceapp.ui.debug

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.victorgomes.geofenceapp.data.database.GeofenceConfigEntity
import com.victorgomes.geofenceapp.data.repository.GeofenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DebugViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GeofenceRepository(application)

    // Persists mock position across config changes (not across process death)
    var mockLat: Double? = null
    var mockLon: Double? = null

    private val _fenceStatus = MutableLiveData<String>()
    val fenceStatus: LiveData<String> = _fenceStatus

    fun updateFenceStatus(lat: Double, lon: Double) {
        viewModelScope.launch {
            val fences = repository.allFenceConfigs.first()
            if (fences.isEmpty()) {
                _fenceStatus.postValue("No fences configured.")
                return@launch
            }
            val lines = fences.map { fence ->
                val dist = distanceBetween(lat, lon, fence.latitude, fence.longitude)
                val inside = dist <= fence.radiusMeters
                val symbol = if (inside) "✅" else "⛔"
                val distStr = if (dist >= 1000) "%.1f km".format(dist / 1000) else "%.0f m".format(dist)
                "$symbol  ${fence.name}  ($distStr from center)"
            }
            _fenceStatus.postValue(lines.joinToString("\n"))
        }
    }

    // Haversine — returns metres
    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0]
    }
}
