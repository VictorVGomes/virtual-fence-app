package com.victorgomes.geofenceapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceEventDao {

    @Query("SELECT * FROM geofence_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<GeofenceEventEntity>>

    @Insert
    suspend fun insertEvent(event: GeofenceEventEntity)

    @Query("DELETE FROM geofence_events")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM geofence_events")
    suspend fun getCount(): Int

    @Query("SELECT * FROM geofence_events ORDER BY timestamp DESC")
    suspend fun getAllEventsList(): List<GeofenceEventEntity>

    @Query("UPDATE geofence_events SET obs = :obs WHERE id = :id")
    suspend fun updateObs(id: Long, obs: String?)

    @Query("SELECT geofenceId, COUNT(*) as count FROM geofence_events GROUP BY geofenceId")
    fun getEventCountsPerFence(): Flow<List<FenceEventCount>>

    @Query("SELECT * FROM geofence_events WHERE geofenceId = :fenceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastEventForFence(fenceId: String): GeofenceEventEntity?

    /** Returns the single most-recent event for every fence that has at least one event. */
    @Query("SELECT * FROM geofence_events WHERE id IN (SELECT MAX(id) FROM geofence_events GROUP BY geofenceId)")
    fun getLatestEventPerFence(): Flow<List<GeofenceEventEntity>>

    @Query("DELETE FROM geofence_events WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: Collection<Long>)
}
