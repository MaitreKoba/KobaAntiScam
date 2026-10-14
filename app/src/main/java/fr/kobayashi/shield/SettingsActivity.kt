package fr.kobayashi.shield

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.kobayashi.shield.databinding.ActivitySettingsBinding

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
    
    private fun isNotificationServiceEnabled(): Boolean {
        return try {
            val flat = java.lang.reflect.Array.newInstance(
                Class.forName("java.lang.String"),
                1
            ).apply {
                java.lang.reflect.Array.set(this, 0, "fr.kobayashi.shield.service.SmsNotificationListener")
            }
            
            val cl = Class.forName("android.app.NotificationManager")
            val method = cl.getMethod("getEnabledNotificationListeners")
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            val listeners = method.invoke(nm) as? Set<String>
            listeners?.contains("fr.kobayashi.shield.service.SmsNotificationListener") ?: false
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
