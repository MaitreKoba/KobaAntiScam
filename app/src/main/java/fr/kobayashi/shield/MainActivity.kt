package fr.kobayashi.shield

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.kobayashi.shield.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupButtons()
        checkServices()
        updateSystemToggle()
    }
    
    private fun updateSystemToggle() {
        val prefs = getSharedPreferences("shield", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("system_enabled", true)
        binding.btnToggleSystem.text = if (isEnabled) getString(R.string.toggle_system_off) else getString(R.string.toggle_system_on)
        binding.statusText.text = if (isEnabled) getString(R.string.system_active) else getString(R.string.system_ready)
    }
    
    private fun setupButtons() {
        binding.btnRules.setOnClickListener {
            startActivity(Intent(this, RulesActivity::class.java))
        }
        
        binding.btnLogs.setOnClickListener {
            startActivity(Intent(this, LogsActivity::class.java))
        }
        
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        binding.btnUpdate.setOnClickListener {
            startActivity(Intent(this, UpdateActivity::class.java))
        }
        
        binding.btnToggleSystem.setOnClickListener {
            toggleSystem()
        }
    }
    
    private fun toggleSystem() {
        val prefs = getSharedPreferences("shield", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("system_enabled", true)
        prefs.edit().putBoolean("system_enabled", !isEnabled).apply()
        
        val newStatus = !isEnabled
        binding.btnToggleSystem.text = if (newStatus) getString(R.string.toggle_system_off) else getString(R.string.toggle_system_on)
        binding.statusText.text = if (newStatus) getString(R.string.system_active) else getString(R.string.system_inactive)
        
        android.widget.Toast.makeText(
            this,
            if (newStatus) "Système activé" else "Système désactivé",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun checkServices() {
        // Vérifier si les services sont activés
        val statusLines = mutableListOf<String>()
        
        // TODO: Vérifier le rôle de filtrage d'appels (nécessite API 29+)
        // Vérifier l'accès aux notifications
        val notificationListenerEnabled = isNotificationServiceEnabled()
        statusLines.add("Notifications: ${if (notificationListenerEnabled) "Activé" else "Non activé"}")
        
        binding.statusText.text = statusLines.joinToString(" | ")
    }
    
    private fun isNotificationServiceEnabled(): Boolean {
        val flat = java.lang.reflect.Array.newInstance(
            Class.forName("java.lang.String"),
            1
        ).apply {
            java.lang.reflect.Array.set(this, 0, "fr.kobayashi.shield.service.SmsNotificationListener")
        }
        
        return try {
            val cl = Class.forName("android.app.NotificationManager")
            val method = cl.getMethod("getEnabledNotificationListeners")
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            val listeners = method.invoke(nm) as? Set<String>
            listeners?.contains("fr.kobayashi.shield.service.SmsNotificationListener") ?: false
        } catch (e: Exception) {
            false
        }
    }
}
