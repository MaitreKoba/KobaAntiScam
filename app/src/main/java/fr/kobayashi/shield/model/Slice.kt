package fr.kobayashi.shield.model

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * FICHIER : Slice.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/model/Slice.kt
 * LIGNES : ~100 | RÔLE : Module anti-rotation (tranche de 8 chiffres) | DATE : 2026-10-14
 * VERSION : 1.0.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi (via agent)
 * STATUT : STABLE | DÉPENDANCES : android.content.Context
 * LIEN : Slice.kt | NOTES : Compteur par tranche de 8 chiffres pour détecter la rotation de numéros
 */

object Slice {
    private const val TAG = "Slice"
    private const val PREFS_NAME = "slice_prefs"
    private const val KEY_THRESHOLD = "slice_threshold"
    private const val DEFAULT_THRESHOLD = 5
    
    // Compteurs en mémoire (resetés au démarrage de l'app)
    private val counters = ConcurrentHashMap<String, SliceCounter>()
    
    // Seuil configurable (nombre d'appels max par tranche en 24h)
    var threshold: Int = DEFAULT_THRESHOLD
    
    /**
     * Initialise le module Slice avec les préférences
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        threshold = prefs.getInt(KEY_THRESHOLD, DEFAULT_THRESHOLD)
        loadCounters(context)
    }
    
    /**
     * Sauvegarde le seuil dans les préférences
     */
    fun saveThreshold(context: Context, newThreshold: Int) {
        threshold = newThreshold
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THRESHOLD, newThreshold).apply()
    }
    
    /**
     * Extrait la tranche de 8 chiffres d'un numéro
     */
    fun getSliceFromNumber(number: String): String {
        val normalized = Rules.normalize(number)
        val digits = normalized.replace("[^\\d]".toRegex(), "")
        return if (digits.length >= 8) {
            digits.takeLast(8)
        } else {
            digits
        }
    }
    
    /**
     * Vérifie si une tranche a dépassé le seuil
     */
    fun isOverThresholdForSlice(slice: String): Boolean {
        val counter = counters[slice] ?: return false
        return counter.count >= threshold
    }
    
    /**
     * Vérifie si un numéro est dans une tranche sur le seuil
     */
    fun isOverThresholdForNumber(number: String): Boolean {
        val slice = getSliceFromNumber(number)
        return isOverThresholdForSlice(slice)
    }
    
    /**
     * Incrémente le compteur pour une tranche
     */
    fun incrementSlice(slice: String) {
        val now = System.currentTimeMillis()
        
        val counter = counters.getOrPut(slice) {
            SliceCounter()
        }
        
        // Réinitialiser si plus de 24h
        if (now - counter.lastReset > 86400000L) {
            counter.count = 0
            counter.lastReset = now
        }
        
        counter.count++
        android.util.Log.d(TAG, "Slice $slice count: ${counter.count}/$threshold")
    }
    
    /**
     * Incrémente le compteur pour un numéro
     */
    fun incrementNumber(number: String) {
        val slice = getSliceFromNumber(number)
        incrementSlice(slice)
    }
    
    /**
     * Réinitialise un compteur de tranche
     */
    fun resetSlice(slice: String) {
        counters[slice] = SliceCounter()
    }
    
    /**
     * Réinitialise tous les compteurs
     */
    fun resetAll() {
        counters.clear()
    }
    
    /**
     * Sauvegarde les compteurs dans les SharedPreferences
     */
    private fun saveCounters(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        for ((slice, counter) in counters) {
            editor.putInt("slice_$slice", counter.count)
            editor.putLong("slice_${slice}_reset", counter.lastReset)
        }
        
        editor.apply()
    }
    
    /**
     * Charge les compteurs depuis les SharedPreferences
     */
    private fun loadCounters(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val all = prefs.all
        
        for ((key, value) in all) {
            if (key.startsWith("slice_") && !key.endsWith("_reset")) {
                val slice = key.substring(6)
                val count = value as Int
                val resetKey = "slice_${slice}_reset"
                val lastReset = all[resetKey] as? Long ?: System.currentTimeMillis()
                
                counters[slice] = SliceCounter(count, lastReset)
            }
        }
    }
    
    /**
     * Récupère le compteur pour une tranche
     */
    fun getCounterForSlice(slice: String): Int {
        return counters[slice]?.count ?: 0
    }
    
    /**
     * Récupère toutes les tranches avec leurs compteurs
     */
    fun getAllSlices(): Map<String, Int> {
        return counters.mapValues { it.value.count }
    }
    
    /**
     * Data class pour stocker un compteur de tranche
     */
    data class SliceCounter(
        var count: Int = 0,
        var lastReset: Long = System.currentTimeMillis()
    )
}
