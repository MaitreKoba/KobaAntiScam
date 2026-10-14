package fr.kobayashi.shield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.RuleAction
import fr.kobayashi.shield.model.RuleDatabase
import fr.kobayashi.shield.model.RuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service pour intercepter les notifications SMS.
 * 
 * À activer manuellement dans les paramètres du téléphone :
 * Paramètres > Apps > Accès spécial > Accès aux notifications > SHIELD
 */
class SmsNotificationListener : NotificationListenerService() {
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val tag = "SmsNotificationListener"
    
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
            handleNotification(sbn)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(tag, "Notification removed: ${sbn.packageName}")
    }
    
    private suspend fun handleNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // Vérifier si c'est une notification d'une app SMS
        if (!smsPackages.contains(packageName)) {
            Log.d(tag, "Ignoring notification from non-SMS package: $packageName")
            return
        }
        
        Log.d(tag, "Processing SMS notification from: $packageName")
        
        val notification: Notification = sbn.notification
        val title = notification.extras.getString(Notification.EXTRA_TITLE, "")
        val text = notification.extras.getString(Notification.EXTRA_TEXT, "")
        
        // Extraire le numéro de téléphone depuis le titre ou le text
        val number = extractPhoneNumber(title) ?: extractPhoneNumber(text)
        
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
    
    private fun extractPhoneNumber(text: String?): String? {
        if (text == null) return null
        
        // Chercher un numéro de téléphone dans le texte
        // Supprimer les espaces et caractères spéciaux
        val cleaned = text.replace("[\\s+()-\"]".toRegex(), "")
        
        // Pattern pour les numéros de téléphone (international ou local)
        val phonePattern = "(?:\\+?\\d{1,3})?[1-9]\\d{6,14}".toRegex()
        return phonePattern.find(cleaned)?.value
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
