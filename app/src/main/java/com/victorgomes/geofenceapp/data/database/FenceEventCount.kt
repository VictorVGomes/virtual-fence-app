package com.victorgomes.geofenceapp.data.database

import androidx.room.ColumnInfo

data class FenceEventCount(
    @ColumnInfo(name = "geofenceId") val geofenceId: String,
    @ColumnInfo(name = "count") val count: Int
)
