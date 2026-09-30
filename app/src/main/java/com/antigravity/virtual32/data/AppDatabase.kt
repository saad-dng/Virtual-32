package com.antigravity.virtual32.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Batch::class, AnswerEntity::class, CycleState::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun answerDao(): AnswerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, useInMemory: Boolean = false): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = if (useInMemory) {
                    Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries()
                } else {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "virtual32_database"
                    ).fallbackToDestructiveMigration() // safe for debug builds
                }
                
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
