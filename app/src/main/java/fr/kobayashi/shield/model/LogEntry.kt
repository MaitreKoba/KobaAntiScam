package fr.kobayashi.shield.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType,
    val action: RuleAction? = null,
    val phoneNumber: String? = null,
    val message: String? = null,
    val details: String? = null
) {
    
    fun formattedTimestamp(): String {
        // Format: HH:mm:ss dd/MM/yyyy
        val time = java.util.Date(timestamp)
        return java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy", java.util.Locale.getDefault()).format(time)
    }
    
    fun displayText(): String {
        return when (type) {
            LogType.CALL_INCOMING -> {
                val actionText = action?.name ?: "UNKNOWN"
                "Appel de $phoneNumber - Action: $actionText"
            }
            LogType.CALL_BLOCKED -> {
                "Appel bloqué: $phoneNumber"
            }
            LogType.CALL_SILENCED -> {
                "Appel silencieux: $phoneNumber"
            }
            LogType.CALL_VOICEMAIL -> {
                "Appel vers messagerie: $phoneNumber"
            }
            LogType.SMS_RECEIVED -> {
                "SMS reçu de $phoneNumber: $message"
            }
            LogType.SMS_BLOCKED -> {
                "SMS bloqué de $phoneNumber"
            }
            LogType.UPDATE_CHECK -> {
                "Vérification mise à jour: $message"
            }
            LogType.ERROR -> {
                "Erreur: $message"
            }
        }
    }
}

enum class LogType {
    CALL_INCOMING,
    CALL_BLOCKED,
    CALL_SILENCED,
    CALL_VOICEMAIL,
    SMS_RECEIVED,
    SMS_BLOCKED,
    UPDATE_CHECK,
    ERROR
}
