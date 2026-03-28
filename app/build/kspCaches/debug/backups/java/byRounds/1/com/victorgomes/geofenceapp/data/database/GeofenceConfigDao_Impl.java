package com.victorgomes.geofenceapp.data.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GeofenceConfigDao_Impl implements GeofenceConfigDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GeofenceConfigEntity> __insertionAdapterOfGeofenceConfigEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateActive;

  private final SharedSQLiteStatement __preparedStmtOfUpdateNameAndRadius;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePosition;

  private final SharedSQLiteStatement __preparedStmtOfUpdateMarkerIcon;

  public GeofenceConfigDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGeofenceConfigEntity = new EntityInsertionAdapter<GeofenceConfigEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `geofence_configs` (`id`,`name`,`latitude`,`longitude`,`radiusMeters`,`isActive`,`createdAt`,`markerIcon`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GeofenceConfigEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindDouble(3, entity.getLatitude());
        statement.bindDouble(4, entity.getLongitude());
        statement.bindDouble(5, entity.getRadiusMeters());
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getCreatedAt());
        statement.bindString(8, entity.getMarkerIcon());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM geofence_configs WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateActive = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE geofence_configs SET isActive = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateNameAndRadius = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE geofence_configs SET name = ?, radiusMeters = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdatePosition = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE geofence_configs SET latitude = ?, longitude = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateMarkerIcon = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE geofence_configs SET markerIcon = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdate(final GeofenceConfigEntity config,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGeofenceConfigEntity.insert(config);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateActive(final String id, final boolean isActive,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateActive.acquire();
        int _argIndex = 1;
        final int _tmp = isActive ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateActive.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateNameAndRadius(final String id, final String name, final float radiusMeters,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateNameAndRadius.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, name);
        _argIndex = 2;
        _stmt.bindDouble(_argIndex, radiusMeters);
        _argIndex = 3;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateNameAndRadius.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePosition(final String id, final double latitude, final double longitude,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePosition.acquire();
        int _argIndex = 1;
        _stmt.bindDouble(_argIndex, latitude);
        _argIndex = 2;
        _stmt.bindDouble(_argIndex, longitude);
        _argIndex = 3;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdatePosition.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMarkerIcon(final String id, final String icon,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateMarkerIcon.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, icon);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateMarkerIcon.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GeofenceConfigEntity>> getAllConfigs() {
    final String _sql = "SELECT * FROM geofence_configs ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"geofence_configs"}, new Callable<List<GeofenceConfigEntity>>() {
      @Override
      @NonNull
      public List<GeofenceConfigEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfRadiusMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "radiusMeters");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfMarkerIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "markerIcon");
          final List<GeofenceConfigEntity> _result = new ArrayList<GeofenceConfigEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GeofenceConfigEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final float _tmpRadiusMeters;
            _tmpRadiusMeters = _cursor.getFloat(_cursorIndexOfRadiusMeters);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpMarkerIcon;
            _tmpMarkerIcon = _cursor.getString(_cursorIndexOfMarkerIcon);
            _item = new GeofenceConfigEntity(_tmpId,_tmpName,_tmpLatitude,_tmpLongitude,_tmpRadiusMeters,_tmpIsActive,_tmpCreatedAt,_tmpMarkerIcon);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllConfigsList(
      final Continuation<? super List<GeofenceConfigEntity>> $completion) {
    final String _sql = "SELECT * FROM geofence_configs ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<GeofenceConfigEntity>>() {
      @Override
      @NonNull
      public List<GeofenceConfigEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfRadiusMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "radiusMeters");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfMarkerIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "markerIcon");
          final List<GeofenceConfigEntity> _result = new ArrayList<GeofenceConfigEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GeofenceConfigEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final float _tmpRadiusMeters;
            _tmpRadiusMeters = _cursor.getFloat(_cursorIndexOfRadiusMeters);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpMarkerIcon;
            _tmpMarkerIcon = _cursor.getString(_cursorIndexOfMarkerIcon);
            _item = new GeofenceConfigEntity(_tmpId,_tmpName,_tmpLatitude,_tmpLongitude,_tmpRadiusMeters,_tmpIsActive,_tmpCreatedAt,_tmpMarkerIcon);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getActiveConfigs(
      final Continuation<? super List<GeofenceConfigEntity>> $completion) {
    final String _sql = "SELECT * FROM geofence_configs WHERE isActive = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<GeofenceConfigEntity>>() {
      @Override
      @NonNull
      public List<GeofenceConfigEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfRadiusMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "radiusMeters");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfMarkerIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "markerIcon");
          final List<GeofenceConfigEntity> _result = new ArrayList<GeofenceConfigEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GeofenceConfigEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final float _tmpRadiusMeters;
            _tmpRadiusMeters = _cursor.getFloat(_cursorIndexOfRadiusMeters);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpMarkerIcon;
            _tmpMarkerIcon = _cursor.getString(_cursorIndexOfMarkerIcon);
            _item = new GeofenceConfigEntity(_tmpId,_tmpName,_tmpLatitude,_tmpLongitude,_tmpRadiusMeters,_tmpIsActive,_tmpCreatedAt,_tmpMarkerIcon);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final String id,
      final Continuation<? super GeofenceConfigEntity> $completion) {
    final String _sql = "SELECT * FROM geofence_configs WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<GeofenceConfigEntity>() {
      @Override
      @Nullable
      public GeofenceConfigEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfRadiusMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "radiusMeters");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfMarkerIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "markerIcon");
          final GeofenceConfigEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final float _tmpRadiusMeters;
            _tmpRadiusMeters = _cursor.getFloat(_cursorIndexOfRadiusMeters);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpMarkerIcon;
            _tmpMarkerIcon = _cursor.getString(_cursorIndexOfMarkerIcon);
            _result = new GeofenceConfigEntity(_tmpId,_tmpName,_tmpLatitude,_tmpLongitude,_tmpRadiusMeters,_tmpIsActive,_tmpCreatedAt,_tmpMarkerIcon);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
