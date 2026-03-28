package com.victorgomes.geofenceapp.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile GeofenceEventDao _geofenceEventDao;

  private volatile GeofenceConfigDao _geofenceConfigDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(4) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `geofence_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `eventType` TEXT NOT NULL, `geofenceId` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `obs` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `geofence_configs` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `radiusMeters` REAL NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `markerIcon` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'dbb33b4b826967cb5ea8bbaae6c2d1b5')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `geofence_events`");
        db.execSQL("DROP TABLE IF EXISTS `geofence_configs`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsGeofenceEvents = new HashMap<String, TableInfo.Column>(7);
        _columnsGeofenceEvents.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceEvents.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceEvents.put("eventType", new TableInfo.Column("eventType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceEvents.put("geofenceId", new TableInfo.Column("geofenceId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceEvents.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceEvents.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceEvents.put("obs", new TableInfo.Column("obs", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGeofenceEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGeofenceEvents = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGeofenceEvents = new TableInfo("geofence_events", _columnsGeofenceEvents, _foreignKeysGeofenceEvents, _indicesGeofenceEvents);
        final TableInfo _existingGeofenceEvents = TableInfo.read(db, "geofence_events");
        if (!_infoGeofenceEvents.equals(_existingGeofenceEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "geofence_events(com.victorgomes.geofenceapp.data.database.GeofenceEventEntity).\n"
                  + " Expected:\n" + _infoGeofenceEvents + "\n"
                  + " Found:\n" + _existingGeofenceEvents);
        }
        final HashMap<String, TableInfo.Column> _columnsGeofenceConfigs = new HashMap<String, TableInfo.Column>(8);
        _columnsGeofenceConfigs.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceConfigs.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceConfigs.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceConfigs.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceConfigs.put("radiusMeters", new TableInfo.Column("radiusMeters", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceConfigs.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceConfigs.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGeofenceConfigs.put("markerIcon", new TableInfo.Column("markerIcon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGeofenceConfigs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGeofenceConfigs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGeofenceConfigs = new TableInfo("geofence_configs", _columnsGeofenceConfigs, _foreignKeysGeofenceConfigs, _indicesGeofenceConfigs);
        final TableInfo _existingGeofenceConfigs = TableInfo.read(db, "geofence_configs");
        if (!_infoGeofenceConfigs.equals(_existingGeofenceConfigs)) {
          return new RoomOpenHelper.ValidationResult(false, "geofence_configs(com.victorgomes.geofenceapp.data.database.GeofenceConfigEntity).\n"
                  + " Expected:\n" + _infoGeofenceConfigs + "\n"
                  + " Found:\n" + _existingGeofenceConfigs);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "dbb33b4b826967cb5ea8bbaae6c2d1b5", "77f73bb467a33a1d1c2aa74134d13c54");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "geofence_events","geofence_configs");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `geofence_events`");
      _db.execSQL("DELETE FROM `geofence_configs`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(GeofenceEventDao.class, GeofenceEventDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GeofenceConfigDao.class, GeofenceConfigDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public GeofenceEventDao geofenceEventDao() {
    if (_geofenceEventDao != null) {
      return _geofenceEventDao;
    } else {
      synchronized(this) {
        if(_geofenceEventDao == null) {
          _geofenceEventDao = new GeofenceEventDao_Impl(this);
        }
        return _geofenceEventDao;
      }
    }
  }

  @Override
  public GeofenceConfigDao geofenceConfigDao() {
    if (_geofenceConfigDao != null) {
      return _geofenceConfigDao;
    } else {
      synchronized(this) {
        if(_geofenceConfigDao == null) {
          _geofenceConfigDao = new GeofenceConfigDao_Impl(this);
        }
        return _geofenceConfigDao;
      }
    }
  }
}
