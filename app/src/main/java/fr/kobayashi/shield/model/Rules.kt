package fr.kobayashi.shield.model

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import java.util.regex.Pattern

/**
 * FICHIER : Rules.kt | CHEMIN : /D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/model/Rules.kt
 * LIGNES : ~200 | RÔLE : Moteur de règles de filtrage d'appels et SMS | DATE : 2026-10-14
 * VERSION : 1.7.0 | ORIGINE : Création, affichage() et indicatif 00 en 1.7.0 | AUTEUR : Maitre Kobayashi (via agent)
 * STATUT : STABLE | DÉPENDANCES : android.content.Context | LIEN : Rules.kt
 * NOTES : Implémente l'ordre de priorité des règles selon MEGA_PROMPT.md
 */

object Rules {
    private const val TAG = "Rules"
    
    // Préfixes ARCEP à rejeter (démarchage)
    private val ARCEP_PREFIXES = listOf(
        "162", "163", "270", "271", "377", "378", 
        "424", "425", "568", "569", "948", "949"
    )
    
    // Temps de rappel pour les inconnus (3 minutes = 180 secondes)
    private const val RECALL_WINDOW_MS = 180000L
    
    // Map pour le compteur anti-rotation (tranche de 8 chiffres -> compteur)
    private val sliceCounters = mutableMapOf<String, SliceCounter>()
    
    // Seuil anti-rotation (configurable, par défaut 5 appels en 24h)
    var sliceThreshold: Int = 5
    
    /**
     * Normalise un numéro de téléphone pour comparaison
     * - Supprime les espaces, +, (), -, etc.
     * - Conserve uniquement les chiffres
     * - Ajoute +33 si le numéro commence par 0 (France)
     */
    fun normalize(number: String): String {
        if (number.isEmpty()) return ""
        
        // Supprimer tous les caractères non-chiffres
        val digitsOnly = number.replace("[^\\d]".toRegex(), "")
        if (digitsOnly.isEmpty()) return ""

        if (digitsOnly.startsWith("00") && digitsOnly.length > 4) {
            return "+${digitsOnly.substring(2)}"
        }

        // Si commence par 0 (France), ajouter +33
        if (digitsOnly.startsWith("0") && digitsOnly.length > 1) {
            return "+33${digitsOnly.substring(1)}"
        }
        
        // Si commence déjà par +33, garder tel quel
        if (digitsOnly.startsWith("33") && digitsOnly.length > 2) {
            return "+$digitsOnly"
        }
        
        // Si commence par 33 sans +, ajouter +
        if (digitsOnly.startsWith("33") && digitsOnly.length > 2) {
            return "+$digitsOnly"
        }
        
        return if (digitsOnly.startsWith("+")) digitsOnly else "+$digitsOnly"
    }
    
    /** Format lisible : +33695123456 devient 06 95 12 34 56, les autres numéros restent en international */
    fun affichage(number: String): String {
        val n = normalize(number)
        if (n.startsWith("+33") && n.length == 12) {
            return ("0" + n.substring(3)).chunked(2).joinToString(" ")
        }
        return n
    }

    /**
     * Normalise un préfixe pour comparaison avec les numéros
     * Ajoute +33 si nécessaire pour correspondre au format normalisé
     */
    fun normalizePrefix(prefix: String): String {
        val brut = prefix.trim()
        val chiffres = brut.filter { it.isDigit() }
        if (chiffres.isEmpty()) return ""
        if (!brut.startsWith("+") && !chiffres.startsWith("0") && !chiffres.startsWith("33")) {
            return "+33$chiffres"
        }
        return normalize(brut)
    }
    
    /**
     * Vérifie si un numéro est un contact
     */
    fun isContact(context: Context, number: String): Boolean {
        val normalizedNumber = normalize(number)
        
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            val selectionArgs = arrayOf("%${normalizedNumber.takeLast(10)}%") // Cherche les 10 derniers chiffres
            
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            val isContact = cursor?.count ?: 0 > 0
            cursor?.close()
            
            Log.d(TAG, "isContact($number) = $isContact")
            return isContact
        } catch (e: Exception) {
            Log.e(TAG, "Error checking contact", e)
            return false
        }
    }
    
    /**
     * Vérifie si un numéro est dans l'allowlist
     */
    fun isInAllowlist(number: String, allowlist: Set<String>): Boolean {
        val normalized = normalize(number)
        return allowlist.any { allowed -> 
            val normalizedAllowed = normalize(allowed)
            normalized == normalizedAllowed || 
            normalized.endsWith(normalizedAllowed.takeLast(8)) || // Correspondance sur les 8 derniers chiffres
            normalizedAllowed.startsWith(normalized.take(8)) // Préfixe
        }
    }
    
    /**
     * Vérifie si un numéro a déjà été appelé par l'utilisateur (appel sortant)
     */
    fun isCalledBefore(context: Context, number: String): Boolean {
        val normalized = normalize(number)
        
        try {
            val cursor = context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(android.provider.CallLog.Calls.NUMBER),
                "${android.provider.CallLog.Calls.TYPE} = ? AND ${android.provider.CallLog.Calls.NUMBER} LIKE ?",
                arrayOf(
                    android.provider.CallLog.Calls.OUTGOING_TYPE.toString(),
                    "%${normalized.takeLast(10)}%"
                ),
                "${android.provider.CallLog.Calls.DATE} DESC"
            )
            
            val hasCalled = cursor?.count ?: 0 > 0
            cursor?.close()
            
            Log.d(TAG, "isCalledBefore($number) = $hasCalled")
            return hasCalled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking call log", e)
            return false
        }
    }
    
    /**
     * Vérifie si un numéro est un préfixe ARCEP
     */
    fun isArcpPrefix(number: String): Boolean {
        val normalized = normalize(number)
        
        // Extraire le préfixe après +33
        val prefix = if (normalized.startsWith("+33") && normalized.length > 3) {
            normalized.substring(3)
        } else if (normalized.startsWith("33") && normalized.length > 2) {
            normalized.substring(2)
        } else if (normalized.startsWith("0") && normalized.length > 1) {
            normalized.substring(1)
        } else {
            normalized
        }
        
        return ARCEP_PREFIXES.any { arcep -> prefix.startsWith(arcep) }
    }
    
    /**
     * Vérifie si c'est un rappel sous 3 minutes (même inconnu)
     */
    fun isRecallWithinWindow(number: String, lastCallTimestamps: Map<String, Long>): Boolean {
        val normalized = normalize(number)
        val now = System.currentTimeMillis()
        
        val lastTimestamp = lastCallTimestamps[normalized] ?: return false
        val timeSinceLastCall = now - lastTimestamp
        
        Log.d(TAG, "isRecallWithinWindow($number): last=$lastTimestamp, now=$now, diff=$timeSinceLastCall ms")
        
        return timeSinceLastCall <= RECALL_WINDOW_MS
    }
    
    /**
     * Vérifie si la tranche de 8 chiffres a dépassé le seuil
     */
    fun isSliceOverThreshold(number: String): Boolean {
        val slice = getSlice(normalize(number))
        val counter = sliceCounters[slice] ?: return false
        return counter.count >= sliceThreshold
    }
    
    /**
     * Incrémente le compteur pour une tranche
     */
    fun incrementSliceCounter(number: String) {
        val slice = getSlice(normalize(number))
        val now = System.currentTimeMillis()
        
        val counter = sliceCounters.getOrPut(slice) {
            SliceCounter()
        }
        
        // Réinitialiser si plus de 24h
        if (now - counter.lastReset > 86400000L) {
            counter.count = 0
            counter.lastReset = now
        }
        
        counter.count++
        Log.d(TAG, "Slice $slice count: ${counter.count}/$sliceThreshold")
    }
    
    /**
     * Extrait la tranche de 8 chiffres (anti-rotation)
     */
    private fun getSlice(normalized: String): String {
        // Prendre les 8 derniers chiffres
        val digits = normalized.replace("[^\\d]".toRegex(), "")
        return if (digits.length >= 8) {
            digits.takeLast(8)
        } else {
            digits
        }
    }
    
    /**
     * Récupère l'action pour un numéro selon l'ordre de priorité
     */
    fun getAction(
        context: Context,
        number: String,
        systemEnabled: Boolean,
        allowlist: Set<String>,
        lastCallTimestamps: Map<String, Long>,
        recentBlocked: Set<String>
    ): CallAction {
        val normalized = normalize(number)
        
        // 1. Système désactivé → tout sonne
        if (!systemEnabled) {
            Log.d(TAG, "Système désactivé, autoriser: $normalized")
            return CallAction.ALLOW
        }
        
        // 2. Contact → sonne
        if (isContact(context, number)) {
            Log.d(TAG, "Contact trouvé, autoriser: $normalized")
            return CallAction.ALLOW
        }
        
        // 3. Allowlist → sonne
        if (isInAllowlist(normalized, allowlist)) {
            Log.d(TAG, "Dans allowlist, autoriser: $normalized")
            return CallAction.ALLOW
        }
        
        // 4. Numéro déjà appelé par l'utilisateur → sonne
        if (isCalledBefore(context, number)) {
            Log.d(TAG, "Déjà appelé, autoriser: $normalized")
            return CallAction.ALLOW
        }
        
        // 5. Préfixes ARCEP → rejet
        if (isArcpPrefix(number)) {
            Log.d(TAG, "Préfixe ARCEP, rejeter: $normalized")
            incrementSliceCounter(number)
            return CallAction.BLOCK
        }
        
        // 6. Même inconnu qui rappelle sous 3 minutes → sonne
        if (isRecallWithinWindow(number, lastCallTimestamps)) {
            Log.d(TAG, "Rappel sous 3 min, autoriser: $normalized")
            return CallAction.ALLOW
        }
        
        // 7. Autres inconnus et numéros masqués → silence + messagerie
        Log.d(TAG, "Inconnu, silence+voicemail: $normalized")
        incrementSliceCounter(number)
        return CallAction.SILENCE_VOICEMAIL
    }
    
    /**
     * Actions pour les appels
     */
    enum class CallAction {
        ALLOW,           // Autoriser (sonne normalement)
        BLOCK,           // Rejeter
        SILENCE,         // Mettre en silencieux
        SILENCE_VOICEMAIL // Mettre en silencieux + envoyer vers messagerie
    }
    
    /**
     * Compteur pour une tranche de 8 chiffres
     */
    private data class SliceCounter(
        var count: Int = 0,
        var lastReset: Long = System.currentTimeMillis()
    )
}
