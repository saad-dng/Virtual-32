package com.antigravity.virtual32.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
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
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
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
public final class AnswerDao_Impl implements AnswerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Batch> __insertionAdapterOfBatch;

  private final EntityInsertionAdapter<AnswerEntity> __insertionAdapterOfAnswerEntity;

  private final EntityInsertionAdapter<CycleState> __insertionAdapterOfCycleState;

  private final EntityDeletionOrUpdateAdapter<CycleState> __updateAdapterOfCycleState;

  private final SharedSQLiteStatement __preparedStmtOfEditAnswerChoice;

  public AnswerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBatch = new EntityInsertionAdapter<Batch>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `batches` (`id`,`createdAt`,`source`,`photoPath`,`galleryUri`,`status`,`provider`,`model`,`promptName`,`promptHash`,`latencyMs`,`rawResponse`,`superseded`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Batch entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getCreatedAt());
        statement.bindString(3, entity.getSource());
        if (entity.getPhotoPath() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getPhotoPath());
        }
        if (entity.getGalleryUri() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getGalleryUri());
        }
        statement.bindString(6, entity.getStatus());
        statement.bindString(7, entity.getProvider());
        statement.bindString(8, entity.getModel());
        statement.bindString(9, entity.getPromptName());
        statement.bindString(10, entity.getPromptHash());
        statement.bindLong(11, entity.getLatencyMs());
        if (entity.getRawResponse() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getRawResponse());
        }
        final int _tmp = entity.getSuperseded() ? 1 : 0;
        statement.bindLong(13, _tmp);
      }
    };
    this.__insertionAdapterOfAnswerEntity = new EntityInsertionAdapter<AnswerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `answers` (`id`,`batchId`,`q`,`choice`,`conf`,`edited`,`note`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AnswerEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getBatchId());
        statement.bindLong(3, entity.getQ());
        statement.bindString(4, entity.getChoice());
        statement.bindString(5, entity.getConf());
        final int _tmp = entity.getEdited() ? 1 : 0;
        statement.bindLong(6, _tmp);
        if (entity.getNote() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getNote());
        }
      }
    };
    this.__insertionAdapterOfCycleState = new EntityInsertionAdapter<CycleState>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `cycle_state` (`id`,`activeBatchIds`,`cursor`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CycleState entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getActiveBatchIds());
        statement.bindLong(3, entity.getCursor());
      }
    };
    this.__updateAdapterOfCycleState = new EntityDeletionOrUpdateAdapter<CycleState>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `cycle_state` SET `id` = ?,`activeBatchIds` = ?,`cursor` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CycleState entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getActiveBatchIds());
        statement.bindLong(3, entity.getCursor());
        statement.bindLong(4, entity.getId());
      }
    };
    this.__preparedStmtOfEditAnswerChoice = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE answers SET choice = ?, edited = 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertBatch(final Batch batch, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfBatch.insertAndReturnId(batch);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAnswers(final List<AnswerEntity> answers,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAnswerEntity.insert(answers);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCycleState(final CycleState state,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCycleState.insert(state);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateCycleState(final CycleState state,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCycleState.handle(state);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object editAnswerChoice(final long answerId, final String newChoice,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfEditAnswerChoice.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, newChoice);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, answerId);
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
          __preparedStmtOfEditAnswerChoice.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getCycleState(final Continuation<? super CycleState> $completion) {
    final String _sql = "SELECT * FROM cycle_state WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CycleState>() {
      @Override
      @Nullable
      public CycleState call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfActiveBatchIds = CursorUtil.getColumnIndexOrThrow(_cursor, "activeBatchIds");
          final int _cursorIndexOfCursor = CursorUtil.getColumnIndexOrThrow(_cursor, "cursor");
          final CycleState _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpActiveBatchIds;
            _tmpActiveBatchIds = _cursor.getString(_cursorIndexOfActiveBatchIds);
            final int _tmpCursor;
            _tmpCursor = _cursor.getInt(_cursorIndexOfCursor);
            _result = new CycleState(_tmpId,_tmpActiveBatchIds,_tmpCursor);
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
  public Object getAnswersForBatches(final List<Long> batchIds,
      final Continuation<? super List<AnswerEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM answers WHERE batchId IN (");
    final int _inputSize = batchIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") ORDER BY q ASC");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (long _item : batchIds) {
      _statement.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AnswerEntity>>() {
      @Override
      @NonNull
      public List<AnswerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBatchId = CursorUtil.getColumnIndexOrThrow(_cursor, "batchId");
          final int _cursorIndexOfQ = CursorUtil.getColumnIndexOrThrow(_cursor, "q");
          final int _cursorIndexOfChoice = CursorUtil.getColumnIndexOrThrow(_cursor, "choice");
          final int _cursorIndexOfConf = CursorUtil.getColumnIndexOrThrow(_cursor, "conf");
          final int _cursorIndexOfEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "edited");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<AnswerEntity> _result = new ArrayList<AnswerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AnswerEntity _item_1;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpBatchId;
            _tmpBatchId = _cursor.getLong(_cursorIndexOfBatchId);
            final int _tmpQ;
            _tmpQ = _cursor.getInt(_cursorIndexOfQ);
            final String _tmpChoice;
            _tmpChoice = _cursor.getString(_cursorIndexOfChoice);
            final String _tmpConf;
            _tmpConf = _cursor.getString(_cursorIndexOfConf);
            final boolean _tmpEdited;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEdited);
            _tmpEdited = _tmp != 0;
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item_1 = new AnswerEntity(_tmpId,_tmpBatchId,_tmpQ,_tmpChoice,_tmpConf,_tmpEdited,_tmpNote);
            _result.add(_item_1);
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
  public Flow<List<AnswerEntity>> getAnswersForBatchesFlow(final List<Long> batchIds) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM answers WHERE batchId IN (");
    final int _inputSize = batchIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") ORDER BY q ASC");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (long _item : batchIds) {
      _statement.bindLong(_argIndex, _item);
      _argIndex++;
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"answers"}, new Callable<List<AnswerEntity>>() {
      @Override
      @NonNull
      public List<AnswerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBatchId = CursorUtil.getColumnIndexOrThrow(_cursor, "batchId");
          final int _cursorIndexOfQ = CursorUtil.getColumnIndexOrThrow(_cursor, "q");
          final int _cursorIndexOfChoice = CursorUtil.getColumnIndexOrThrow(_cursor, "choice");
          final int _cursorIndexOfConf = CursorUtil.getColumnIndexOrThrow(_cursor, "conf");
          final int _cursorIndexOfEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "edited");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<AnswerEntity> _result = new ArrayList<AnswerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AnswerEntity _item_1;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpBatchId;
            _tmpBatchId = _cursor.getLong(_cursorIndexOfBatchId);
            final int _tmpQ;
            _tmpQ = _cursor.getInt(_cursorIndexOfQ);
            final String _tmpChoice;
            _tmpChoice = _cursor.getString(_cursorIndexOfChoice);
            final String _tmpConf;
            _tmpConf = _cursor.getString(_cursorIndexOfConf);
            final boolean _tmpEdited;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEdited);
            _tmpEdited = _tmp != 0;
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item_1 = new AnswerEntity(_tmpId,_tmpBatchId,_tmpQ,_tmpChoice,_tmpConf,_tmpEdited,_tmpNote);
            _result.add(_item_1);
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
  public Object getLatest100BatchIds(final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT id FROM batches ORDER BY id DESC LIMIT 100";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
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
  public Object getRecentBatches(final Continuation<? super List<Batch>> $completion) {
    final String _sql = "SELECT * FROM batches ORDER BY id DESC LIMIT 10";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Batch>>() {
      @Override
      @NonNull
      public List<Batch> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfGalleryUri = CursorUtil.getColumnIndexOrThrow(_cursor, "galleryUri");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProvider = CursorUtil.getColumnIndexOrThrow(_cursor, "provider");
          final int _cursorIndexOfModel = CursorUtil.getColumnIndexOrThrow(_cursor, "model");
          final int _cursorIndexOfPromptName = CursorUtil.getColumnIndexOrThrow(_cursor, "promptName");
          final int _cursorIndexOfPromptHash = CursorUtil.getColumnIndexOrThrow(_cursor, "promptHash");
          final int _cursorIndexOfLatencyMs = CursorUtil.getColumnIndexOrThrow(_cursor, "latencyMs");
          final int _cursorIndexOfRawResponse = CursorUtil.getColumnIndexOrThrow(_cursor, "rawResponse");
          final int _cursorIndexOfSuperseded = CursorUtil.getColumnIndexOrThrow(_cursor, "superseded");
          final List<Batch> _result = new ArrayList<Batch>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Batch _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpPhotoPath;
            if (_cursor.isNull(_cursorIndexOfPhotoPath)) {
              _tmpPhotoPath = null;
            } else {
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            }
            final String _tmpGalleryUri;
            if (_cursor.isNull(_cursorIndexOfGalleryUri)) {
              _tmpGalleryUri = null;
            } else {
              _tmpGalleryUri = _cursor.getString(_cursorIndexOfGalleryUri);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpProvider;
            _tmpProvider = _cursor.getString(_cursorIndexOfProvider);
            final String _tmpModel;
            _tmpModel = _cursor.getString(_cursorIndexOfModel);
            final String _tmpPromptName;
            _tmpPromptName = _cursor.getString(_cursorIndexOfPromptName);
            final String _tmpPromptHash;
            _tmpPromptHash = _cursor.getString(_cursorIndexOfPromptHash);
            final long _tmpLatencyMs;
            _tmpLatencyMs = _cursor.getLong(_cursorIndexOfLatencyMs);
            final String _tmpRawResponse;
            if (_cursor.isNull(_cursorIndexOfRawResponse)) {
              _tmpRawResponse = null;
            } else {
              _tmpRawResponse = _cursor.getString(_cursorIndexOfRawResponse);
            }
            final boolean _tmpSuperseded;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSuperseded);
            _tmpSuperseded = _tmp != 0;
            _item = new Batch(_tmpId,_tmpCreatedAt,_tmpSource,_tmpPhotoPath,_tmpGalleryUri,_tmpStatus,_tmpProvider,_tmpModel,_tmpPromptName,_tmpPromptHash,_tmpLatencyMs,_tmpRawResponse,_tmpSuperseded);
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
  public Flow<List<Batch>> getRecentBatchesFlow() {
    final String _sql = "SELECT * FROM batches ORDER BY id DESC LIMIT 10";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"batches"}, new Callable<List<Batch>>() {
      @Override
      @NonNull
      public List<Batch> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfGalleryUri = CursorUtil.getColumnIndexOrThrow(_cursor, "galleryUri");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProvider = CursorUtil.getColumnIndexOrThrow(_cursor, "provider");
          final int _cursorIndexOfModel = CursorUtil.getColumnIndexOrThrow(_cursor, "model");
          final int _cursorIndexOfPromptName = CursorUtil.getColumnIndexOrThrow(_cursor, "promptName");
          final int _cursorIndexOfPromptHash = CursorUtil.getColumnIndexOrThrow(_cursor, "promptHash");
          final int _cursorIndexOfLatencyMs = CursorUtil.getColumnIndexOrThrow(_cursor, "latencyMs");
          final int _cursorIndexOfRawResponse = CursorUtil.getColumnIndexOrThrow(_cursor, "rawResponse");
          final int _cursorIndexOfSuperseded = CursorUtil.getColumnIndexOrThrow(_cursor, "superseded");
          final List<Batch> _result = new ArrayList<Batch>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Batch _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpPhotoPath;
            if (_cursor.isNull(_cursorIndexOfPhotoPath)) {
              _tmpPhotoPath = null;
            } else {
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            }
            final String _tmpGalleryUri;
            if (_cursor.isNull(_cursorIndexOfGalleryUri)) {
              _tmpGalleryUri = null;
            } else {
              _tmpGalleryUri = _cursor.getString(_cursorIndexOfGalleryUri);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpProvider;
            _tmpProvider = _cursor.getString(_cursorIndexOfProvider);
            final String _tmpModel;
            _tmpModel = _cursor.getString(_cursorIndexOfModel);
            final String _tmpPromptName;
            _tmpPromptName = _cursor.getString(_cursorIndexOfPromptName);
            final String _tmpPromptHash;
            _tmpPromptHash = _cursor.getString(_cursorIndexOfPromptHash);
            final long _tmpLatencyMs;
            _tmpLatencyMs = _cursor.getLong(_cursorIndexOfLatencyMs);
            final String _tmpRawResponse;
            if (_cursor.isNull(_cursorIndexOfRawResponse)) {
              _tmpRawResponse = null;
            } else {
              _tmpRawResponse = _cursor.getString(_cursorIndexOfRawResponse);
            }
            final boolean _tmpSuperseded;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSuperseded);
            _tmpSuperseded = _tmp != 0;
            _item = new Batch(_tmpId,_tmpCreatedAt,_tmpSource,_tmpPhotoPath,_tmpGalleryUri,_tmpStatus,_tmpProvider,_tmpModel,_tmpPromptName,_tmpPromptHash,_tmpLatencyMs,_tmpRawResponse,_tmpSuperseded);
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
  public Object markBatchesSuperseded(final List<Long> batchIds,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("UPDATE batches SET superseded = 1 WHERE id IN (");
        final int _inputSize = batchIds.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : batchIds) {
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

  @Override
  public Object deleteOldBatches(final List<Long> retentionIds,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM batches WHERE id NOT IN (");
        final int _inputSize = retentionIds.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : retentionIds) {
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
