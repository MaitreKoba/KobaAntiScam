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
