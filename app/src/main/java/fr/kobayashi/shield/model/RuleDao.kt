package fr.kobayashi.shield.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: Rule): Long
    
    @Update
    suspend fun update(rule: Rule)
    
    @Delete
    suspend fun delete(rule: Rule)
    
    @Query("SELECT * FROM rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<Rule>>
    
    @Query("SELECT * FROM rules ORDER BY createdAt DESC")
    suspend fun getAllRulesList(): List<Rule>
    
    @Query("SELECT * FROM rules WHERE ruleType = :type ORDER BY createdAt DESC")
    fun getRulesByType(type: RuleType): Flow<List<Rule>>
    
    @Query("SELECT * FROM rules WHERE isActive = 1 AND ruleType = :type")
    fun getActiveRulesByType(type: RuleType): List<Rule>
    
    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Long): Rule?
    
    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
