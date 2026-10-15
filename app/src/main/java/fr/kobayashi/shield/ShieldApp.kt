package fr.kobayashi.shield

import android.app.Application
import android.content.Context
import fr.kobayashi.shield.model.RuleDatabase

class ShieldApp : Application() {
    
    init {
        instance = this
    }
    
    companion object {
        private var instance: ShieldApp? = null
        fun applicationContext(): Context {
            return instance!!
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Apparence.appliquerModeNuit(this)
        
        // Initialiser la base de données des règles
        RuleDatabase.init(this)
    }
}
