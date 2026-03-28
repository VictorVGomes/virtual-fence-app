package com.victorgomes.geofenceapp.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geofence_events")
data class GeofenceEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val eventType: String,   // "ENTER" or "EXIT"
    val geofenceId: String,
    val latitude: Double,
    val longitude: Double,
    val obs: String? = null
)
