package com.victorgomes.geofenceapp.data.repository

import android.content.Context
import com.victorgomes.geofenceapp.data.database.AppDatabase
import com.victorgomes.geofenceapp.data.database.FenceEventCount
import com.victorgomes.geofenceapp.data.database.GeofenceConfigEntity
import com.victorgomes.geofenceapp.data.database.GeofenceEventEntity
import kotlinx.coroutines.flow.Flow

class GeofenceRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val eventDao = db.geofenceEventDao()
    private val configDao = db.geofenceConfigDao()

    // --- Events ---
    val allEvents: Flow<List<GeofenceEventEntity>> = eventDao.getAllEvents()
    val eventCountsPerFence: Flow<List<FenceEventCount>> = eventDao.getEventCountsPerFence()
    val latestEventPerFence: Flow<List<GeofenceEventEntity>> = eventDao.getLatestEventPerFence()
    suspend fun insertEvent(event: GeofenceEventEntity) = eventDao.insertEvent(event)
    suspend fun clearAllEvents() = eventDao.clearAll()
    suspend fun getAllEventsList(): List<GeofenceEventEntity> = eventDao.getAllEventsList()
    suspend fun updateEventObs(id: Long, obs: String?) = eventDao.updateObs(id, obs)
    suspend fun deleteEvents(ids: Collection<Long>) = eventDao.deleteByIds(ids)
    suspend fun getLastEventForFence(fenceId: String) = eventDao.getLastEventForFence(fenceId)

    // --- Fence configs ---
    val allFenceConfigs: Flow<List<GeofenceConfigEntity>> = configDao.getAllConfigs()
    suspend fun getAllFenceConfigsList(): List<GeofenceConfigEntity> = configDao.getAllConfigsList()
    suspend fun insertFenceConfig(config: GeofenceConfigEntity) = configDao.insertOrUpdate(config)
    suspend fun deleteFenceConfig(id: String) = configDao.deleteById(id)
    suspend fun updateFenceActive(id: String, isActive: Boolean) = configDao.updateActive(id, isActive)
    suspend fun updateFenceNameAndRadius(id: String, name: String, radiusMeters: Float) = configDao.updateNameAndRadius(id, name, radiusMeters)
    suspend fun updateFencePosition(id: String, latitude: Double, longitude: Double) = configDao.updatePosition(id, latitude, longitude)
    suspend fun getActiveConfigs(): List<GeofenceConfigEntity> = configDao.getActiveConfigs()
    suspend fun getFenceById(id: String): GeofenceConfigEntity? = configDao.getById(id)
    suspend fun updateFenceMarkerIcon(id: String, icon: String) = configDao.updateMarkerIcon(id, icon)
}
