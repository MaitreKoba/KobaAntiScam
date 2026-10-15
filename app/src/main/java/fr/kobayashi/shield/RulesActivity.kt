package fr.kobayashi.shield

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import fr.kobayashi.shield.databinding.ActivityRulesBinding
import fr.kobayashi.shield.databinding.DialogRuleEditBinding
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.Rule
import fr.kobayashi.shield.model.RuleAction
import fr.kobayashi.shield.model.RuleDatabase
import fr.kobayashi.shield.model.RuleType
import kotlinx.coroutines.launch

class RulesActivity : BaseActivity() {
    
    private lateinit var binding: ActivityRulesBinding
    private lateinit var ruleAdapter: RuleAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRulesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupFab()
        loadRules()
    }
    
    private fun setupRecyclerView() {
        ruleAdapter = RuleAdapter(
            onEdit = { rule -> editRule(rule) },
            onDelete = { rule -> deleteRule(rule) }
        )
        
        binding.rulesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@RulesActivity)
            adapter = ruleAdapter
        }
    }
    
    private fun setupFab() {
        binding.addRuleBtn.setOnClickListener {
            addNewRule()
        }
    }
    
    private fun loadRules() {
        lifecycleScope.launch {
            RuleDatabase.getDatabase().ruleDao().getAllRules()
                .collect { rules ->
                    ruleAdapter.submitList(rules)
                    binding.emptyRulesText.visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE
                }
        }
    }
    
    private fun addNewRule() {
        val dialogBinding = DialogRuleEditBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Nouvelle règle")
            .setView(dialogBinding.root)
            .setPositiveButton("Enregistrer") { _, _ ->
                saveRule(dialogBinding)
            }
            .setNegativeButton("Annuler") { _, _ -> }
            .show()
    }
    
    private fun editRule(rule: Rule) {
        val dialogBinding = DialogRuleEditBinding.inflate(layoutInflater)
        
        // Remplir les champs
        dialogBinding.etPattern.setText(rule.pattern)
        dialogBinding.etDescription.setText(rule.description ?: "")
        dialogBinding.rbCall.isChecked = rule.ruleType == RuleType.CALL
        dialogBinding.rbSms.isChecked = rule.ruleType == RuleType.SMS
        
        when (rule.action) {
            RuleAction.BLOCK -> dialogBinding.rbBlock.isChecked = true
            RuleAction.SILENCE -> dialogBinding.rbSilence.isChecked = true
            RuleAction.VOICEMAIL -> dialogBinding.rbVoicemail.isChecked = true
            RuleAction.ALLOW -> dialogBinding.rbAllow.isChecked = true
        }
        
        dialogBinding.cbActive.isChecked = rule.isActive
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Modifier la règle")
            .setView(dialogBinding.root)
            .setPositiveButton("Enregistrer") { _, _ ->
                saveRule(dialogBinding, rule.id)
            }
            .setNegativeButton("Annuler") { _, _ -> }
            .show()
    }
    
    private fun saveRule(dialogBinding: DialogRuleEditBinding, ruleId: Long? = null) {
        val pattern = dialogBinding.etPattern.text.toString().trim()
        if (pattern.isEmpty()) {
            Toast.makeText(this, "Le motif est obligatoire", Toast.LENGTH_SHORT).show()
            return
        }
        
        val description = dialogBinding.etDescription.text.toString().trim()
        val ruleType = if (dialogBinding.rbCall.isChecked) RuleType.CALL else RuleType.SMS
        val action = when {
            dialogBinding.rbBlock.isChecked -> RuleAction.BLOCK
            dialogBinding.rbSilence.isChecked -> RuleAction.SILENCE
            dialogBinding.rbVoicemail.isChecked -> RuleAction.VOICEMAIL
            else -> RuleAction.ALLOW
        }
        val isActive = dialogBinding.cbActive.isChecked
        
        val rule = Rule(
            id = ruleId ?: 0,
            pattern = pattern,
            description = if (description.isEmpty()) null else description,
            action = action,
            ruleType = ruleType,
            isActive = isActive
        )
        
        lifecycleScope.launch {
            if (ruleId == null) {
                RuleDatabase.getDatabase().ruleDao().insert(rule)
            } else {
                RuleDatabase.getDatabase().ruleDao().update(rule)
            }
            
            // Log
            RuleDatabase.getDatabase().logDao().insert(
                LogEntry(
                    type = LogType.CALL_INCOMING, // TODO: change to rule type
                    message = "Règle ${if (ruleId == null) "ajoutée" else "modifiée"}: $pattern"
                )
            )
        }
    }
    
    private fun deleteRule(rule: Rule) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Supprimer la règle")
            .setMessage("Voulez-vous supprimer la règle: ${rule.pattern}?")
            .setPositiveButton("Supprimer") { _, _ ->
                lifecycleScope.launch {
                    RuleDatabase.getDatabase().ruleDao().delete(rule)
                    
                    RuleDatabase.getDatabase().logDao().insert(
                        LogEntry(
                            type = LogType.ERROR, // TODO: change
                            message = "Règle supprimée: ${rule.pattern}"
                        )
                    )
                }
            }
            .setNegativeButton("Annuler") { _, _ -> }
            .show()
    }
}
