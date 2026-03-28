package com.victorgomes.geofenceapp.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "geofence_configs")
data class GeofenceConfigEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val markerIcon: String = "pin"
)
