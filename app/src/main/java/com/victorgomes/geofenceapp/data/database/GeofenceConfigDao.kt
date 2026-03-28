package com.victorgomes.geofenceapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceConfigDao {

    @Query("SELECT * FROM geofence_configs ORDER BY createdAt DESC")
    fun getAllConfigs(): Flow<List<GeofenceConfigEntity>>

    @Query("SELECT * FROM geofence_configs ORDER BY createdAt DESC")
    suspend fun getAllConfigsList(): List<GeofenceConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: GeofenceConfigEntity)

    @Query("DELETE FROM geofence_configs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE geofence_configs SET isActive = :isActive WHERE id = :id")
    suspend fun updateActive(id: String, isActive: Boolean)

    @Query("UPDATE geofence_configs SET name = :name, radiusMeters = :radiusMeters WHERE id = :id")
    suspend fun updateNameAndRadius(id: String, name: String, radiusMeters: Float)

    @Query("UPDATE geofence_configs SET latitude = :latitude, longitude = :longitude WHERE id = :id")
    suspend fun updatePosition(id: String, latitude: Double, longitude: Double)

    @Query("SELECT * FROM geofence_configs WHERE isActive = 1")
    suspend fun getActiveConfigs(): List<GeofenceConfigEntity>

    @Query("SELECT * FROM geofence_configs WHERE id = :id")
    suspend fun getById(id: String): GeofenceConfigEntity?

    @Query("UPDATE geofence_configs SET markerIcon = :icon WHERE id = :id")
    suspend fun updateMarkerIcon(id: String, icon: String)
}
