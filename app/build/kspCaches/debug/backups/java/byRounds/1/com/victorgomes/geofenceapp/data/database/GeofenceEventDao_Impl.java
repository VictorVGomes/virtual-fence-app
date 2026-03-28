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
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GeofenceEventDao_Impl implements GeofenceEventDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GeofenceEventEntity> __insertionAdapterOfGeofenceEventEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  private final SharedSQLiteStatement __preparedStmtOfUpdateObs;

  public GeofenceEventDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGeofenceEventEntity = new EntityInsertionAdapter<GeofenceEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `geofence_events` (`id`,`timestamp`,`eventType`,`geofenceId`,`latitude`,`longitude`,`obs`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GeofenceEventEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, entity.getEventType());
        statement.bindString(4, entity.getGeofenceId());
        statement.bindDouble(5, entity.getLatitude());
        statement.bindDouble(6, entity.getLongitude());
        if (entity.getObs() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getObs());
        }
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM geofence_events";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateObs = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE geofence_events SET obs = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertEvent(final GeofenceEventEntity event,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGeofenceEventEntity.insert(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
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
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateObs(final long id, final String obs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateObs.acquire();
        int _argIndex = 1;
        if (obs == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, obs);
        }
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfUpdateObs.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GeofenceEventEntity>> getAllEvents() {
    final String _sql = "SELECT * FROM geofence_events ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"geofence_events"}, new Callable<List<GeofenceEventEntity>>() {
      @Override
      @NonNull
      public List<GeofenceEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfEventType = CursorUtil.getColumnIndexOrThrow(_cursor, "eventType");
          final int _cursorIndexOfGeofenceId = CursorUtil.getColumnIndexOrThrow(_cursor, "geofenceId");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfObs = CursorUtil.getColumnIndexOrThrow(_cursor, "obs");
          final List<GeofenceEventEntity> _result = new ArrayList<GeofenceEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GeofenceEventEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpEventType;
            _tmpEventType = _cursor.getString(_cursorIndexOfEventType);
            final String _tmpGeofenceId;
            _tmpGeofenceId = _cursor.getString(_cursorIndexOfGeofenceId);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpObs;
            if (_cursor.isNull(_cursorIndexOfObs)) {
              _tmpObs = null;
            } else {
              _tmpObs = _cursor.getString(_cursorIndexOfObs);
            }
            _item = new GeofenceEventEntity(_tmpId,_tmpTimestamp,_tmpEventType,_tmpGeofenceId,_tmpLatitude,_tmpLongitude,_tmpObs);
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
  public Object getCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM geofence_events";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object getAllEventsList(
      final Continuation<? super List<GeofenceEventEntity>> $completion) {
    final String _sql = "SELECT * FROM geofence_events ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<GeofenceEventEntity>>() {
      @Override
      @NonNull
      public List<GeofenceEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfEventType = CursorUtil.getColumnIndexOrThrow(_cursor, "eventType");
          final int _cursorIndexOfGeofenceId = CursorUtil.getColumnIndexOrThrow(_cursor, "geofenceId");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfObs = CursorUtil.getColumnIndexOrThrow(_cursor, "obs");
          final List<GeofenceEventEntity> _result = new ArrayList<GeofenceEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GeofenceEventEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpEventType;
            _tmpEventType = _cursor.getString(_cursorIndexOfEventType);
            final String _tmpGeofenceId;
            _tmpGeofenceId = _cursor.getString(_cursorIndexOfGeofenceId);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpObs;
            if (_cursor.isNull(_cursorIndexOfObs)) {
              _tmpObs = null;
            } else {
              _tmpObs = _cursor.getString(_cursorIndexOfObs);
            }
            _item = new GeofenceEventEntity(_tmpId,_tmpTimestamp,_tmpEventType,_tmpGeofenceId,_tmpLatitude,_tmpLongitude,_tmpObs);
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
  public Flow<List<FenceEventCount>> getEventCountsPerFence() {
    final String _sql = "SELECT geofenceId, COUNT(*) as count FROM geofence_events GROUP BY geofenceId";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"geofence_events"}, new Callable<List<FenceEventCount>>() {
      @Override
      @NonNull
      public List<FenceEventCount> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGeofenceId = 0;
          final int _cursorIndexOfCount = 1;
          final List<FenceEventCount> _result = new ArrayList<FenceEventCount>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FenceEventCount _item;
            final String _tmpGeofenceId;
            _tmpGeofenceId = _cursor.getString(_cursorIndexOfGeofenceId);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            _item = new FenceEventCount(_tmpGeofenceId,_tmpCount);
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
  public Object getLastEventForFence(final String fenceId,
      final Continuation<? super GeofenceEventEntity> $completion) {
    final String _sql = "SELECT * FROM geofence_events WHERE geofenceId = ? ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, fenceId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<GeofenceEventEntity>() {
      @Override
      @Nullable
      public GeofenceEventEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfEventType = CursorUtil.getColumnIndexOrThrow(_cursor, "eventType");
          final int _cursorIndexOfGeofenceId = CursorUtil.getColumnIndexOrThrow(_cursor, "geofenceId");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfObs = CursorUtil.getColumnIndexOrThrow(_cursor, "obs");
          final GeofenceEventEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpEventType;
            _tmpEventType = _cursor.getString(_cursorIndexOfEventType);
            final String _tmpGeofenceId;
            _tmpGeofenceId = _cursor.getString(_cursorIndexOfGeofenceId);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpObs;
            if (_cursor.isNull(_cursorIndexOfObs)) {
              _tmpObs = null;
            } else {
              _tmpObs = _cursor.getString(_cursorIndexOfObs);
            }
            _result = new GeofenceEventEntity(_tmpId,_tmpTimestamp,_tmpEventType,_tmpGeofenceId,_tmpLatitude,_tmpLongitude,_tmpObs);
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

  @Override
  public Flow<List<GeofenceEventEntity>> getLatestEventPerFence() {
    final String _sql = "SELECT * FROM geofence_events WHERE id IN (SELECT MAX(id) FROM geofence_events GROUP BY geofenceId)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"geofence_events"}, new Callable<List<GeofenceEventEntity>>() {
      @Override
      @NonNull
      public List<GeofenceEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfEventType = CursorUtil.getColumnIndexOrThrow(_cursor, "eventType");
          final int _cursorIndexOfGeofenceId = CursorUtil.getColumnIndexOrThrow(_cursor, "geofenceId");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfObs = CursorUtil.getColumnIndexOrThrow(_cursor, "obs");
          final List<GeofenceEventEntity> _result = new ArrayList<GeofenceEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GeofenceEventEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpEventType;
            _tmpEventType = _cursor.getString(_cursorIndexOfEventType);
            final String _tmpGeofenceId;
            _tmpGeofenceId = _cursor.getString(_cursorIndexOfGeofenceId);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpObs;
            if (_cursor.isNull(_cursorIndexOfObs)) {
              _tmpObs = null;
            } else {
              _tmpObs = _cursor.getString(_cursorIndexOfObs);
            }
            _item = new GeofenceEventEntity(_tmpId,_tmpTimestamp,_tmpEventType,_tmpGeofenceId,_tmpLatitude,_tmpLongitude,_tmpObs);
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
  public Object deleteByIds(final Collection<Long> ids,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM geofence_events WHERE id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : ids) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
