package com.swingtrace.aicoaching.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AnalysisHistoryEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun analysisHistoryDao(): AnalysisHistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "swingtrace_database"
                )
                    .fallbackToDestructiveMigration() // スキーマ変更時に古いデータを破棄
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
