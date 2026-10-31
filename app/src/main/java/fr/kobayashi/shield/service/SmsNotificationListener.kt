/*
 * FICHIER : SmsNotificationListener.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/service/SmsNotificationListener.kt
 * LIGNES : 214 | RÔLE : Notifications des SMS : règles perso, puis anti-arnaque (lien désarmé, score, signalement 33700)
 * DATE : 2026-10-31 | VERSION : 1.12.0 | ORIGINE : Anti-arnaque, plantage à chaque SMS corrigé (regex) | AUTEUR : Maitre Kobayashi
 * STATUT : STABLE | DÉPENDANCES : SmsAnalyse.kt, Notifs.kt, Decision.kt, RuleDatabase | LIEN : LogsActivity.kt
 * NOTES : une app non définie par défaut ne bloque pas le SMS : elle masque sa notification et en poste une autre.
 *         Le SMS reste lisible dans Messages. Build debug : com.android.shell accepté pour les tests adb.
 */
package fr.kobayashi.shield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.ContactsContract
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import fr.kobayashi.shield.Notifs
import fr.kobayashi.shield.model.Decision
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.RuleAction
import fr.kobayashi.shield.model.RuleDatabase
import fr.kobayashi.shield.model.RuleType
import fr.kobayashi.shield.model.Rules
import fr.kobayashi.shield.model.SmsAnalyse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service pour intercepter les notifications SMS.
 * 
 * À activer manuellement dans les paramètres du téléphone :
 * Paramètres > Apps > Accès spécial > Accès aux notifications > Kobayashi AntiScan
 */
class SmsNotificationListener : NotificationListenerService() {
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val tag = "Shield"
    
    // Paquets des applications SMS connues
    private val smsPackages = listOf(
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.android.messaging",
        "com.honor.messaging",
        "com.hihonor.messaging"
    )
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(tag, "SmsNotificationListener created")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(tag, "Notification posted: ${sbn.packageName}")
        coroutineScope.launch {
            // Une exception ici tuait tout le processus, filtrage d'appels compris
            runCatching { handleNotification(sbn) }.onFailure { Log.e(tag, "Notification SMS non traitée", it) }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(tag, "Notification removed: ${sbn.packageName}")
    }
    
    private suspend fun handleNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        val debug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!smsPackages.contains(packageName) && !(debug && packageName == "com.android.shell")) {
            Log.d(tag, "Ignoring notification from non-SMS package: $packageName")
            return
        }
        
        Log.d(tag, "Processing SMS notification from: $packageName")
        
        val notification: Notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (!getSharedPreferences("shield", MODE_PRIVATE).getBoolean("system_enabled", true)) return
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (text.isBlank()) return

        val number = extractPhoneNumber(title)
        if (antiArnaque(sbn, title, text, number)) return
        if (number == null) {
            Log.d(tag, "No phone number extracted from notification")
            return
        }
        
        Log.d(tag, "Extracted phone number: $number")
        
        // Log la réception du SMS
        val logDao = RuleDatabase.getDatabase().logDao()
        logDao.insert(
            LogEntry(
                type = LogType.SMS_RECEIVED,
                phoneNumber = number,
                message = text,
                details = "Package: $packageName"
            )
        )
        
        // Vérifier les règles
        val rules = RuleDatabase.getDatabase().ruleDao().getActiveRulesByType(RuleType.SMS)
        val matchingRule = rules.find { it.matches(number) }
        
        if (matchingRule == null) {
            Log.d(tag, "No matching rule for SMS from $number")
            return
        }
        
        Log.d(tag, "Matched rule: ${matchingRule.pattern} -> ${matchingRule.action}")
        
        // Appliquer l'action
        when (matchingRule.action) {
            RuleAction.BLOCK, RuleAction.SILENCE -> {
                logDao.insert(
                    LogEntry(
                        type = LogType.SMS_BLOCKED,
                        phoneNumber = number,
                        action = matchingRule.action,
                        message = "SMS bloqué par règle: ${matchingRule.pattern}"
                    )
                )
                // Supprimer la notification
                cancelNotification(sbn.key)
                Log.d(tag, "SMS blocked and notification cancelled")
            }
            RuleAction.VOICEMAIL -> {
                // Pour les SMS, VOICEMAIL = bloquer
                logDao.insert(
                    LogEntry(
                        type = LogType.SMS_BLOCKED,
                        phoneNumber = number,
                        action = matchingRule.action,
                        message = "SMS envoyé vers messagerie par règle: ${matchingRule.pattern}"
                    )
                )
                cancelNotification(sbn.key)
                Log.d(tag, "SMS voicemail action - blocked")
            }
            RuleAction.ALLOW -> {
                Log.d(tag, "SMS allowed by rule")
                // Ne rien faire
            }
        }
    }
    
    /** SMS avec lien d'un expéditeur inconnu : notification remplacée par une alerte désarmée. true si intercepté */
    private suspend fun antiArnaque(sbn: StatusBarNotification, expediteur: String, texte: String, numero: String?): Boolean {
        val connu = if (numero != null) Decision.nomContact(this, numero) != null else contactNomme(expediteur)
        val score = SmsAnalyse.score(texte, connu)
        if (score < SmsAnalyse.SEUIL) return false
        cancelNotification(sbn.key)
        val desarme = SmsAnalyse.desarmer(texte)
        Notifs.smsSuspect(this, expediteur.ifBlank { "Expéditeur inconnu" }, desarme, score, texte)
        RuleDatabase.getDatabase().logDao().insert(
            LogEntry(
                type = LogType.SMS_BLOCKED,
                phoneNumber = numero ?: expediteur.ifBlank { null },
                action = RuleAction.BLOCK,
                message = desarme,
                details = "SMS suspect : lien désarmé, risque $score/100"
            )
        )
        Log.d(tag, "SMS suspect de $expediteur, risque $score/100")
        return true
    }

    /** Expéditeur affiché par son nom : vrai s'il correspond à un contact (et non à un nom d'expéditeur imité) */
    private fun contactNomme(nom: String): Boolean = nom.isNotBlank() && runCatching {
        contentResolver.query(
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(nom)),
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null
        )?.use { c ->
            generateSequence { if (c.moveToNext()) c.getString(0) else null }.any { it.equals(nom, ignoreCase = true) }
        } ?: false
    }.getOrDefault(false)

    private fun extractPhoneNumber(text: String?): String? {
        if (text == null) return null
        
        // Chercher un numéro de téléphone dans le texte
        // Supprimer les espaces et caractères spéciaux
        // Tiret en fin de classe : « )-" » était lu comme une plage invalide et faisait planter l'app sur Android
        val cleaned = text.replace("[\\s+()\"-]".toRegex(), "")

        // Pattern pour les numéros de téléphone (international ou local)
        val phonePattern = "(?:\\+?\\d{1,3})?[1-9]\\d{6,14}".toRegex()
        return phonePattern.find(cleaned)?.value?.let { Rules.normalize(it) }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sms_filter_channel",
                "SMS Filter",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d(tag, "Notification channel created")
        }
    }
}
