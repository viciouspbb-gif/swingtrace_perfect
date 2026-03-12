package com.swingtrace.aicoaching.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisHistoryDao {
    
    @Query("SELECT * FROM analysis_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getHistoryByUser(userId: String): Flow<List<AnalysisHistoryEntity>>
    
    @Query("SELECT * FROM analysis_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): AnalysisHistoryEntity?
    
    @Insert
    suspend fun insert(history: AnalysisHistoryEntity): Long
    
    @Delete
    suspend fun delete(history: AnalysisHistoryEntity)
    
    @Query("DELETE FROM analysis_history WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)
}
