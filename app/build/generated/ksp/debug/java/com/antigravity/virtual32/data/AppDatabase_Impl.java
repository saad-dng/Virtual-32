package com.antigravity.virtual32.data;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile AnswerDao _answerDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `batches` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` INTEGER NOT NULL, `source` TEXT NOT NULL, `photoPath` TEXT, `galleryUri` TEXT, `status` TEXT NOT NULL, `provider` TEXT NOT NULL, `model` TEXT NOT NULL, `promptName` TEXT NOT NULL, `promptHash` TEXT NOT NULL, `latencyMs` INTEGER NOT NULL, `rawResponse` TEXT, `superseded` INTEGER NOT NULL, `pageCount` INTEGER NOT NULL, `photoPaths` TEXT, `galleryUris` TEXT, `warnings` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `answers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `batchId` INTEGER NOT NULL, `q` INTEGER NOT NULL, `choice` TEXT NOT NULL, `conf` TEXT NOT NULL, `edited` INTEGER NOT NULL, `note` TEXT, `page` INTEGER NOT NULL, FOREIGN KEY(`batchId`) REFERENCES `batches`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_answers_batchId` ON `answers` (`batchId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cycle_state` (`id` INTEGER NOT NULL, `activeBatchIds` TEXT NOT NULL, `cursor` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fd6c17930bafe8931dbef9a04a3850c3')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `batches`");
        db.execSQL("DROP TABLE IF EXISTS `answers`");
        db.execSQL("DROP TABLE IF EXISTS `cycle_state`");
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
        db.execSQL("PRAGMA foreign_keys = ON");
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
        final HashMap<String, TableInfo.Column> _columnsBatches = new HashMap<String, TableInfo.Column>(17);
        _columnsBatches.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("source", new TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("photoPath", new TableInfo.Column("photoPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("galleryUri", new TableInfo.Column("galleryUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("provider", new TableInfo.Column("provider", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("model", new TableInfo.Column("model", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("promptName", new TableInfo.Column("promptName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("promptHash", new TableInfo.Column("promptHash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("latencyMs", new TableInfo.Column("latencyMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("rawResponse", new TableInfo.Column("rawResponse", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("superseded", new TableInfo.Column("superseded", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("pageCount", new TableInfo.Column("pageCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("photoPaths", new TableInfo.Column("photoPaths", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("galleryUris", new TableInfo.Column("galleryUris", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBatches.put("warnings", new TableInfo.Column("warnings", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBatches = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBatches = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBatches = new TableInfo("batches", _columnsBatches, _foreignKeysBatches, _indicesBatches);
        final TableInfo _existingBatches = TableInfo.read(db, "batches");
        if (!_infoBatches.equals(_existingBatches)) {
          return new RoomOpenHelper.ValidationResult(false, "batches(com.antigravity.virtual32.data.Batch).\n"
                  + " Expected:\n" + _infoBatches + "\n"
                  + " Found:\n" + _existingBatches);
        }
        final HashMap<String, TableInfo.Column> _columnsAnswers = new HashMap<String, TableInfo.Column>(8);
        _columnsAnswers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnswers.put("batchId", new TableInfo.Column("batchId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnswers.put("q", new TableInfo.Column("q", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnswers.put("choice", new TableInfo.Column("choice", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnswers.put("conf", new TableInfo.Column("conf", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnswers.put("edited", new TableInfo.Column("edited", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnswers.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnswers.put("page", new TableInfo.Column("page", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAnswers = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysAnswers.add(new TableInfo.ForeignKey("batches", "CASCADE", "NO ACTION", Arrays.asList("batchId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesAnswers = new HashSet<TableInfo.Index>(1);
        _indicesAnswers.add(new TableInfo.Index("index_answers_batchId", false, Arrays.asList("batchId"), Arrays.asList("ASC")));
        final TableInfo _infoAnswers = new TableInfo("answers", _columnsAnswers, _foreignKeysAnswers, _indicesAnswers);
        final TableInfo _existingAnswers = TableInfo.read(db, "answers");
        if (!_infoAnswers.equals(_existingAnswers)) {
          return new RoomOpenHelper.ValidationResult(false, "answers(com.antigravity.virtual32.data.AnswerEntity).\n"
                  + " Expected:\n" + _infoAnswers + "\n"
                  + " Found:\n" + _existingAnswers);
        }
        final HashMap<String, TableInfo.Column> _columnsCycleState = new HashMap<String, TableInfo.Column>(3);
        _columnsCycleState.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCycleState.put("activeBatchIds", new TableInfo.Column("activeBatchIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCycleState.put("cursor", new TableInfo.Column("cursor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCycleState = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCycleState = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCycleState = new TableInfo("cycle_state", _columnsCycleState, _foreignKeysCycleState, _indicesCycleState);
        final TableInfo _existingCycleState = TableInfo.read(db, "cycle_state");
        if (!_infoCycleState.equals(_existingCycleState)) {
          return new RoomOpenHelper.ValidationResult(false, "cycle_state(com.antigravity.virtual32.data.CycleState).\n"
                  + " Expected:\n" + _infoCycleState + "\n"
                  + " Found:\n" + _existingCycleState);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "fd6c17930bafe8931dbef9a04a3850c3", "0cda2f8a9918fd8341345b6208e75174");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "batches","answers","cycle_state");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `batches`");
      _db.execSQL("DELETE FROM `answers`");
      _db.execSQL("DELETE FROM `cycle_state`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
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
    _typeConvertersMap.put(AnswerDao.class, AnswerDao_Impl.getRequiredConverters());
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
  public AnswerDao answerDao() {
    if (_answerDao != null) {
      return _answerDao;
    } else {
      synchronized(this) {
        if(_answerDao == null) {
          _answerDao = new AnswerDao_Impl(this);
        }
        return _answerDao;
      }
    }
  }
}
