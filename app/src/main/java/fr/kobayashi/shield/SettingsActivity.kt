package fr.kobayashi.shield

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import fr.kobayashi.shield.databinding.ActivitySettingsBinding
import fr.kobayashi.shield.service.ContactsHelper
import java.io.File

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupButtons()
        updateStatus()
    }
    
    private fun setupButtons() {
        binding.btnRequestCallFilterRole.setOnClickListener {
            requestCallFilterRole()
        }
        
        binding.btnRequestNotificationAccess.setOnClickListener {
            requestNotificationAccess()
        }
        
        binding.btnDisableBatteryOptimization.setOnClickListener {
            disableBatteryOptimization()
        }
        
        binding.btnCheckUpdate.setOnClickListener {
            startActivity(Intent(this, UpdateActivity::class.java))
        }
        
        // Strict mode button
        binding.btnStrictMode.setOnClickListener {
            toggleStrictMode()
        }
        
        // Neutral list button
        binding.btnManageNeutralList.setOnClickListener {
            showNeutralListDialog()
        }
        
        // Voicemail message button
        binding.btnToggleCallback.setOnClickListener {
            toggleVoicemailSetting()
        }
        
        // Allowlist button
        binding.btnManageAllowlist.setOnClickListener {
            showAllowlistDialog()
        }
        
        // Export/Import buttons
        binding.btnExport.setOnClickListener {
            exportData()
        }
        
        binding.btnImport.setOnClickListener {
            importData()
        }
    }
    
    @Suppress("DEPRECATION")
    private fun requestCallFilterRole() {
        // TODO: Implémenter lorsque l'API est disponible
        // Nécessite Android 10+ (API 29) et le rôle CALL_SCREENING
        // Pour l'instant, afficher un message
        if (Build.VERSION.SDK_INT >= 29) {
            Toast.makeText(this, "Activer le rôle via les paramètres du téléphone", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Le filtrage d'appels nécessite Android 10+", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun requestNotificationAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
    
    private fun disableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= 23) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
    
    private fun toggleStrictMode() {
        val isEnabled = ContactsHelper.isStrictModeEnabled(this)
        ContactsHelper.setStrictModeEnabled(this, !isEnabled)
        updateStatus()
        Toast.makeText(
            this,
            if (!isEnabled) "Mode strict ACTIVÉ: bloquer tous sauf contacts" else "Mode strict désactivé",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun showNeutralListDialog() {
        val neutralList = ContactsHelper.getNeutralList(this).toMutableList()
        val items = arrayOf("Ajouter un numéro", "Retirer un numéro", "Voir la liste")
        
        AlertDialog.Builder(this)
            .setTitle("Liste neutre (bloqués même s'ils sont dans les contacts)")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showAddNeutralDialog()
                    1 -> showRemoveNeutralDialog()
                    2 -> showNeutralList()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    
    private fun showAddNeutralDialog() {
        val editText = android.widget.EditText(this)
        editText.hint = "Numéro à bloquer (ex: +33612345678)"
        
        AlertDialog.Builder(this)
            .setTitle("Ajouter à la liste neutre")
            .setView(editText)
            .setPositiveButton("Ajouter") { _, _ ->
                val number = editText.text.toString()
                if (number.isNotBlank()) {
                    ContactsHelper.addToNeutralList(this, number)
                    Toast.makeText(this, "Numéro ajouté à la liste neutre", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Numéro vide", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    
    private fun showRemoveNeutralDialog() {
        val neutralList = ContactsHelper.getNeutralList(this)
        if (neutralList.isEmpty()) {
            Toast.makeText(this, "Liste neutre vide", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Retirer de la liste neutre")
            .setItems(neutralList.toTypedArray()) { _, which ->
                val number = neutralList.elementAt(which)
                ContactsHelper.removeFromNeutralList(this, number)
                Toast.makeText(this, "Numéro retiré", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    
    private fun showNeutralList() {
        val neutralList = ContactsHelper.getNeutralList(this)
        if (neutralList.isEmpty()) {
            Toast.makeText(this, "Liste neutre vide", Toast.LENGTH_SHORT).show()
            return
        }
        
        val message = neutralList.joinToString("\n")
        AlertDialog.Builder(this)
            .setTitle("Liste neutre (${neutralList.size} numéros)")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showAllowlistDialog() {
        val items = arrayOf("Ajouter un contact", "Retirer un contact", "Voir la liste", "Ajouter tous les contacts")
        
        AlertDialog.Builder(this)
            .setTitle("Liste Blanche (contacts sans message vocal)")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showAddAllowlistDialog()
                    1 -> showRemoveAllowlistDialog()
                    2 -> showAllowlist()
                    3 -> addAllContactsToAllowlist()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    
    private fun showAddAllowlistDialog() {
        val editText = android.widget.EditText(this)
        editText.hint = "Numéro à ajouter (ex: +33612345678)"
        
        AlertDialog.Builder(this)
            .setTitle("Ajouter à la liste blanche")
            .setView(editText)
            .setPositiveButton("Ajouter") { _, _ ->
                val number = editText.text.toString()
                if (number.isNotBlank()) {
                    ContactsHelper.addToAllowlist(this, number)
                    Toast.makeText(this, "Numéro ajouté à la liste blanche", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Numéro vide", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    
    private fun showRemoveAllowlistDialog() {
        val allowlist = ContactsHelper.getAllowlist(this)
        if (allowlist.isEmpty()) {
            Toast.makeText(this, "Liste blanche vide", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Retirer de la liste blanche")
            .setItems(allowlist.toTypedArray()) { _, which ->
                val number = allowlist.elementAt(which)
                ContactsHelper.removeFromAllowlist(this, number)
                Toast.makeText(this, "Numéro retiré", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    
    private fun showAllowlist() {
        val allowlist = ContactsHelper.getAllowlist(this)
        if (allowlist.isEmpty()) {
            Toast.makeText(this, "Liste blanche vide", Toast.LENGTH_SHORT).show()
            return
        }
        
        val message = allowlist.joinToString("\n")
        AlertDialog.Builder(this)
            .setTitle("Liste blanche (${allowlist.size} contacts)")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun addAllContactsToAllowlist() {
        val allContacts = ContactsHelper.getAllContactNumbers(this)
        if (allContacts.isEmpty()) {
            Toast.makeText(this, "Aucun contact trouvé", Toast.LENGTH_SHORT).show()
            return
        }
        
        for (number in allContacts) {
            ContactsHelper.addToAllowlist(this, number)
        }
        
        Toast.makeText(this, "Tous les contacts ajoutés à la liste blanche (${allContacts.size})", Toast.LENGTH_SHORT).show()
    }
    
    private fun toggleVoicemailSetting() {
        val isEnabled = ContactsHelper.isVoicemailEnabled(this)
        ContactsHelper.setVoicemailEnabled(this, !isEnabled)
        updateStatus()
        Toast.makeText(
            this,
            if (!isEnabled) "Message vocal personnalisé ACTIVÉ" else "Message vocal personnalisé désactivé",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun updateStatus() {
        // Call Filter Role - TODO: Vérifier le rôle
        if (Build.VERSION.SDK_INT >= 29) {
            binding.btnRequestCallFilterRole.text = "Activer le rôle"
        } else {
            binding.btnRequestCallFilterRole.text = "Non disponible"
        }
        
        // Notification Access
        val hasNotificationAccess = isNotificationServiceEnabled()
        binding.btnRequestNotificationAccess.text = if (hasNotificationAccess) "Accès activé ✓" else "Activer l'accès"
        
        // Battery Optimization
        val isBatteryOptimized = isBatteryOptimized()
        binding.btnDisableBatteryOptimization.text = if (!isBatteryOptimized) "Optimisation désactivée ✓" else "Désactiver"
    }
    
    private fun exportData() {
        try {
            val allowlist = ContactsHelper.getAllowlist(this)
            val neutralList = ContactsHelper.getNeutralList(this)
            val strictMode = ContactsHelper.isStrictModeEnabled(this)
            val voicemailEnabled = ContactsHelper.isVoicemailEnabled(this)
            
            // Créer un JSON simple
            val json = StringBuilder().apply {
                append("{\n")
                append("  \"export_date\": ${System.currentTimeMillis()},\n")
                append("  \"allowlist\": [")
                append(allowlist.joinToString(", ") { "\"$it\"" })
                append("],\n")
                append("  \"neutral_list\": [")
                append(neutralList.joinToString(", ") { "\"$it\"" })
                append("],\n")
                append("  \"strict_mode\": $strictMode,\n")
                append("  \"voicemail_enabled\": $voicemailEnabled\n")
                append("}")
            }.toString()
            
            // Sauvegarder dans un fichier
            val file = File(getExternalFilesDir(null), "shield_backup_${System.currentTimeMillis()}.json")
            file.writeText(json)
            
            Toast.makeText(this, "Données exportées vers ${file.name}", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error exporting data", e)
            Toast.makeText(this, "Erreur d'export: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun importData() {
        // TODO: Implémenter l'import
        Toast.makeText(this, "Fonctionnalité d'import à venir", Toast.LENGTH_SHORT).show()
    }
    
    private fun isNotificationServiceEnabled(): Boolean {
        return try {
            val listenerName = "fr.kobayashi.shield.service.SmsNotificationListener"
            val cl = Class.forName("android.app.NotificationManager")
            val method = cl.getMethod("getEnabledNotificationListeners")
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            val listeners = method.invoke(nm) as? Set<String>
            listeners?.contains(listenerName) ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isBatteryOptimized(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val pkgName = packageName
            !powerManager.isIgnoringBatteryOptimizations(pkgName)
        } else {
            false
        }
    }
    
    companion object {
        private const val REQUEST_CALL_FILTER_ROLE = 1001
    }
}
