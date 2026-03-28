package com.victorgomes.geofenceapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GeofenceEventEntity::class, GeofenceConfigEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun geofenceEventDao(): GeofenceEventDao
    abstract fun geofenceConfigDao(): GeofenceConfigDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `geofence_configs` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `radiusMeters` REAL NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE geofence_events ADD COLUMN obs TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE geofence_configs ADD COLUMN markerIcon TEXT NOT NULL DEFAULT 'pin'")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "geofence_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
