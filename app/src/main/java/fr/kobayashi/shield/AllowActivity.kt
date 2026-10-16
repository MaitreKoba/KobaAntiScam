package fr.kobayashi.shield

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.kobayashi.shield.databinding.ActivityAllowBinding
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.Rule
import fr.kobayashi.shield.model.RuleAction
import fr.kobayashi.shield.model.RuleDatabase
import fr.kobayashi.shield.model.RuleType
import fr.kobayashi.shield.model.Rules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FICHIER : AllowActivity.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/AllowActivity.kt
 * LIGNES : ~80 | RÔLE : Gestion du deep link kobashield://allow?num=... | DATE : 2026-10-14
 * VERSION : 1.0.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi (via agent)
 * STATUT : STABLE | DÉPENDANCES : ActivityAllowBinding, Rules, RuleDatabase
 * LIEN : AllowActivity.kt | NOTES : Ajoute un numéro à l'allowlist via deep link
 */

class AllowActivity : BaseActivity() {
    
    private lateinit var binding: ActivityAllowBinding
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        // Analyser l'URI du deep link
        handleDeepLink(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        
        if (data == null) {
            binding.statusText.text = "Aucun numéro fourni"
            binding.btnConfirm.isEnabled = false
            return
        }
        
        // Extraire le numéro depuis kobashield://allow?num=...
        val number = data.getQueryParameter("num")
        
        if (number.isNullOrEmpty()) {
            binding.statusText.text = "Numéro manquant dans l'URL"
            binding.btnConfirm.isEnabled = false
            return
        }
        
        val normalizedNumber = Rules.normalize(number)
        binding.numberText.text = "Numéro: $normalizedNumber"
        binding.btnConfirm.isEnabled = true
        
        binding.btnConfirm.setOnClickListener {
            addToAllowlist(normalizedNumber)
        }
    }
    
    private fun addToAllowlist(number: String) {
        coroutineScope.launch {
            try {
                val ruleDao = RuleDatabase.getDatabase().ruleDao()
                
                // Vérifier si la règle existe déjà
                val existingRules = ruleDao.getAllRulesList()
                val exists = existingRules.any { rule -> rule.pattern == number }
                
                if (!exists) {
                    // Ajouter à l'allowlist
                    val rule = Rule(
                        pattern = number,
                        description = "Ajouté via deep link",
                        action = RuleAction.ALLOW,
                        ruleType = RuleType.CALL,
                        isActive = true
                    )
                    ruleDao.insert(rule)
                    
                    // Log
                    val logDao = RuleDatabase.getDatabase().logDao()
                    logDao.insert(
                        LogEntry(
                            type = LogType.UPDATE_CHECK,
                            message = "Numéro ajouté à l'allowlist via deep link: $number"
                        )
                    )
                    
                    runOnUiThread {
                        binding.statusText.text = "Numéro ajouté à la liste blanche"
                        binding.statusText.setTextColor(
                            android.graphics.Color.parseColor("#4CAF50")
                        )
                    }
                } else {
                    runOnUiThread {
                        binding.statusText.text = "Numéro déjà dans l'allowlist"
                        binding.statusText.setTextColor(
                            android.graphics.Color.parseColor("#FF9800")
                        )
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.statusText.text = "Erreur: ${e.message}"
                    binding.statusText.setTextColor(
                        android.graphics.Color.parseColor("#F44336")
                    )
                }
            }
        }
    }
}
