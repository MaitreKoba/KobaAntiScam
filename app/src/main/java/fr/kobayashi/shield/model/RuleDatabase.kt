package fr.kobayashi.shield.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Rule::class, LogEntry::class], version = 1, exportSchema = false)
abstract class RuleDatabase : RoomDatabase() {
    
    abstract fun ruleDao(): RuleDao
    abstract fun logDao(): LogDao
    
    companion object {
        @Volatile
        private var INSTANCE: RuleDatabase? = null
        
        fun init(context: Context) {
            if (INSTANCE == null) {
                synchronized(RuleDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            RuleDatabase::class.java,
                            "shield-db"
                        ).build()
                    }
                }
            }
        }
        
        fun getDatabase(): RuleDatabase {
            return INSTANCE ?: throw IllegalStateException("Database not initialized")
        }
    }
}
