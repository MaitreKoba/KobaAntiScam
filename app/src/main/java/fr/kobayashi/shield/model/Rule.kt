package fr.kobayashi.shield.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.regex.Pattern

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val description: String? = null,
    val action: RuleAction = RuleAction.BLOCK,
    val ruleType: RuleType = RuleType.CALL,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    
    fun matches(number: String): Boolean {
        if (!isActive) return false
        
        return try {
            val regex = if (pattern.startsWith("regex:")) {
                Pattern.compile(pattern.substring(6))
            } else if (pattern.startsWith("prefix:")) {
                Pattern.compile("^${Pattern.quote(pattern.substring(7))}.*")
            } else {
                val cible = if (pattern.any { it.isLetter() }) pattern else Rules.normalize(pattern)
                Pattern.compile(".*${Pattern.quote(cible)}.*")
            }
            regex.matcher(number).matches()
        } catch (e: Exception) {
            false
        }
    }
}

enum class RuleAction {
    BLOCK,       // Bloquer l'appel/SMS
    SILENCE,     // Mettre en silencieux
    VOICEMAIL,   // Envoyer vers messagerie
    ALLOW        // Autoriser
}

enum class RuleType {
    CALL,    // Pour les appels
    SMS      // Pour les SMS
}
