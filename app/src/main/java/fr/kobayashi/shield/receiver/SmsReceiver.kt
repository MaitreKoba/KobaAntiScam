package fr.kobayashi.shield.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
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
 * Broadcast Receiver pour intercepter les SMS entrants.
 * 
 * Nécessite :
 * - Permission RECEIVE_SMS dans le manifest
 * - L'app doit être l'app SMS par défaut pour abortBroadcast()
 *   (restriction Android 4.4+)
 */
class SmsReceiver : BroadcastReceiver() {
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val tag = "SmsReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        
        Log.d(tag, "SMS received broadcast")
        
        coroutineScope.launch {
            handleSms(context, intent)
        }
    }
    
    private suspend fun handleSms(context: Context, intent: Intent) {
        try {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            Log.d(tag, "Found ${smsMessages.size} SMS parts")
            
            for (sms in smsMessages) {
                val originatingAddress = sms.originatingAddress ?: ""
                val messageBody = sms.messageBody ?: ""
                
                Log.d(tag, "SMS from: $originatingAddress, body: $messageBody")
                
                // Log le SMS reçu
                val logDao = RuleDatabase.getDatabase().logDao()
                logDao.insert(
                    LogEntry(
                        type = LogType.SMS_RECEIVED,
                        phoneNumber = originatingAddress,
                        message = messageBody
                    )
                )
                
                // Vérifier les règles
                val rules = RuleDatabase.getDatabase().ruleDao().getActiveRulesByType(RuleType.SMS)
                val matchingRule = rules.find { it.matches(originatingAddress) }
                
                if (matchingRule != null && matchingRule.isActive) {
                    Log.d(tag, "Matched rule for SMS: ${matchingRule.pattern} -> ${matchingRule.action}")
                    
                    logDao.insert(
                        LogEntry(
                            type = LogType.SMS_BLOCKED,
                            phoneNumber = originatingAddress,
                            action = matchingRule.action,
                            message = "SMS bloqué par règle: ${matchingRule.pattern}"
                        )
                    )
                    
                    when (matchingRule.action) {
                        RuleAction.BLOCK, RuleAction.SILENCE, RuleAction.VOICEMAIL -> {
                            // Bloquer le SMS
                            Log.d(tag, "Aborting broadcast for blocked SMS")
                            abortBroadcast()
                            return
                        }
                        RuleAction.ALLOW -> {
                            Log.d(tag, "SMS allowed, continuing broadcast")
                            // Ne rien faire, laisser passer
                        }
                    }
                } else {
                    Log.d(tag, "No matching rule for SMS from $originatingAddress")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error handling SMS", e)
        }
    }
}
