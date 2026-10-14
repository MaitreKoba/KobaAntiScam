package fr.kobayashi.shield.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntry): Long
    
    @Delete
    suspend fun delete(log: LogEntry)
    
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1000")
    fun getAllLogs(): Flow<List<LogEntry>>
    
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1000")
    suspend fun getAllLogsList(): List<LogEntry>
    
    @Query("SELECT * FROM logs WHERE type = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: LogType): Flow<List<LogEntry>>
    
    @Query("DELETE FROM logs")
    suspend fun clearAll()
    
    @Query("SELECT COUNT(*) FROM logs")
    suspend fun count(): Int
}
