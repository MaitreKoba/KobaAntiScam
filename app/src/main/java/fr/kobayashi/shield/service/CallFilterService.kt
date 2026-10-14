package fr.kobayashi.shield.service

import android.content.Context
import android.net.Uri
import android.telecom.CallScreeningService
import android.telecom.Call
import android.util.Log
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.RuleAction
import fr.kobayashi.shield.model.RuleDatabase
import fr.kobayashi.shield.model.RuleType
import fr.kobayashi.shield.model.Rules
import fr.kobayashi.shield.service.ContactsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * FICHIER : CallFilterService.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/service/CallFilterService.kt
 * LIGNES : ~140 | RÔLE : Service de filtrage d'appels entrants | DATE : 2026-10-14
 * VERSION : 1.0.0 | ORIGINE : Création selon MEGA_PROMPT | AUTEUR : Maitre Kobayashi (via agent)
 * STATUT : STABLE | DÉPENDANCES : CallScreeningService, Rules, RuleDatabase
 * LIEN : CallFilterService.kt | NOTES : Implémente l'ordre de priorité des règles
 */

class CallFilterService : CallScreeningService() {
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val tag = "CallFilterService"
    
    // Cache pour les timestamps des derniers appels (anti-spam)
    private val lastCallTimestamps = mutableMapOf<String, Long>()
    
    override fun onScreenCall(details: Call.Details) {
        val number = extractNumber(details.handle)
        Log.d(tag, "onScreenCall: $number, direction=${details.callDirection}")
        
        // Stocker le timestamp
        if (number.isNotEmpty()) {
            lastCallTimestamps[number] = System.currentTimeMillis()
        }
        
        coroutineScope.launch {
            val response = handleCall(details)
            respondToCall(details, response)
        }
    }
    
    private suspend fun handleCall(details: Call.Details): CallScreeningService.CallResponse {
        val number = extractNumber(details.handle)
        
        if (number.isEmpty()) {
            Log.w(tag, "Numéro vide, autoriser par défaut")
            return allowResponse()
        }
        
        val context = applicationContext
        val prefs = context.getSharedPreferences("shield", Context.MODE_PRIVATE)
        val systemEnabled = prefs.getBoolean("system_enabled", true)
        
        // Check if strict mode is enabled (block all except contacts)
        val strictMode = ContactsHelper.isStrictModeEnabled(context)
        
        // Récupérer les règles depuis la base de données
        val activeCallRules = withContext(Dispatchers.IO) {
            RuleDatabase.getDatabase().ruleDao().getActiveRulesByType(RuleType.CALL)
        }
        
        // Check if number is in contacts and not in neutral list
        val isContactAndNotNeutral = ContactsHelper.isContactAndNotNeutral(context, number)
        
        // In strict mode: block by default, allow only contacts (not in neutral list)
        if (strictMode && systemEnabled) {
            if (isContactAndNotNeutral) {
                Log.d(tag, "STRICT MODE: $number is in contacts and not neutral -> ALLOW")
                logCall(number, RuleAction.ALLOW)
                return allowResponse()
            } else {
                Log.d(tag, "STRICT MODE: $number is NOT in contacts or is neutral -> BLOCK")
                logCall(number, RuleAction.BLOCK)
                return blockResponse()
            }
        }
        
        // Récupérer l'allowlist depuis les préférences
        val allowlistSet = prefs.getStringSet("allowlist", mutableSetOf<String>()) ?: mutableSetOf()
        
        // Convertir les règles en allowlist
        val dbAllowlist = activeCallRules
            .filter { it.action == RuleAction.ALLOW }
            .map { it.pattern }
            .toSet()
        
        // Fusionner les allowlists
        val fullAllowlist = allowlistSet + dbAllowlist
        
        // Récupérer les règles de blocage
        val blockRules = activeCallRules
            .filter { it.action == RuleAction.BLOCK }
            .map { it.pattern }
        
        val silenceRules = activeCallRules
            .filter { it.action == RuleAction.SILENCE || it.action == RuleAction.VOICEMAIL }
            .map { it.pattern }
        
        // Appliquer l'ordre de priorité des règles (MEGA_PROMPT § Ordre des règles d'appel)
        val rulesAction = Rules.getAction(
            context = context,
            number = number,
            systemEnabled = systemEnabled,
            allowlist = fullAllowlist,
            lastCallTimestamps = lastCallTimestamps.toMap(),
            recentBlocked = setOf()
        )
        
        // Mapper Rules.CallAction vers RuleAction
        val action = when (rulesAction) {
            Rules.CallAction.ALLOW -> RuleAction.ALLOW
            Rules.CallAction.BLOCK -> RuleAction.BLOCK
            Rules.CallAction.SILENCE -> RuleAction.SILENCE
            Rules.CallAction.SILENCE_VOICEMAIL -> RuleAction.VOICEMAIL
        }
        
        // Vérifier aussi les règles de la base de données
        val dbRule = activeCallRules.find { it.matches(number) }
        val finalAction = dbRule?.action ?: action
        
        Log.d(tag, "Decision: $number -> $finalAction (rules=$rulesAction, db=$dbRule)")
        
        // Log dans la base de données
        logCall(number, finalAction)
        
        return when (finalAction) {
            RuleAction.ALLOW -> allowResponse()
            RuleAction.BLOCK -> blockResponse()
            RuleAction.SILENCE -> silenceResponse()
            RuleAction.VOICEMAIL -> voicemailResponse()
        }
    }
    
    private fun allowResponse(): CallScreeningService.CallResponse {
        Log.d(tag, "ALLOW: autoriser l'appel")
        return CallScreeningService.CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
    }
    
    private fun blockResponse(): CallScreeningService.CallResponse {
        Log.d(tag, "BLOCK: rejeter l'appel")
        return CallScreeningService.CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(true)
            .setSkipNotification(true)
            .build()
    }
    
    private fun silenceResponse(): CallScreeningService.CallResponse {
        Log.d(tag, "SILENCE: mettre en silencieux")
        return CallScreeningService.CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(true)
            .setSkipNotification(true)
            .build()
    }
    
    private fun voicemailResponse(): CallScreeningService.CallResponse {
        Log.d(tag, "VOICEMAIL: silencieux + messagerie")
        return CallScreeningService.CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
    }
    
    private fun logCall(number: String, action: RuleAction) {
        coroutineScope.launch {
            try {
                val logType = when (action) {
                    RuleAction.ALLOW -> LogType.CALL_INCOMING
                    RuleAction.BLOCK -> LogType.CALL_BLOCKED
                    RuleAction.SILENCE -> LogType.CALL_SILENCED
                    RuleAction.VOICEMAIL -> LogType.CALL_VOICEMAIL
                }
                
                RuleDatabase.getDatabase().logDao().insert(
                    LogEntry(
                        type = logType,
                        phoneNumber = number,
                        action = action,
                        message = "Appel $action"
                    )
                )
                Log.d(tag, "Logged: $number -> $action")
            } catch (e: Exception) {
                Log.e(tag, "Error logging call", e)
            }
        }
    }
    
    private fun extractNumber(handle: Uri?): String {
        if (handle == null) return ""
        val schemeSpecificPart = handle.schemeSpecificPart
        // Normaliser le numéro
        return Rules.normalize(schemeSpecificPart)
    }
}
