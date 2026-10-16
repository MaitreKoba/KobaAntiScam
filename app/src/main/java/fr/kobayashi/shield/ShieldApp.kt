/*
 * FICHIER : ShieldApp.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/ShieldApp.kt
 * LIGNES : 38 | RÔLE : Application : mode nuit, base des règles, planification de la synchro du répondeur
 * DATE : 2026-10-16 | VERSION : 1.9.0 | ORIGINE : Création, synchro du répondeur en 1.9.0 | AUTEUR : Maitre Kobayashi
 * STATUT : STABLE | DÉPENDANCES : RuleDatabase, Apparence, repondeur/Synchro | LIEN : AndroidManifest.xml | NOTES : aucune
 */
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
        fr.kobayashi.shield.repondeur.Synchro.planifier(this)
    }
}
