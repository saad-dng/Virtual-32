package com.antigravity.virtual32.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Batch::class, AnswerEntity::class, CycleState::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun answerDao(): AnswerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE batches ADD COLUMN pageCount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE batches ADD COLUMN photoPaths TEXT")
                db.execSQL("ALTER TABLE batches ADD COLUMN galleryUris TEXT")
                db.execSQL("ALTER TABLE batches ADD COLUMN warnings TEXT")
                db.execSQL("ALTER TABLE answers ADD COLUMN page INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context, useInMemory: Boolean = false): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = if (useInMemory) {
                    Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                        .addMigrations(MIGRATION_1_2)
                        .allowMainThreadQueries()
                } else {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "virtual32_database"
                    )
                        .addMigrations(MIGRATION_1_2)
                        .fallbackToDestructiveMigration() // safe for debug builds
                }
                
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
