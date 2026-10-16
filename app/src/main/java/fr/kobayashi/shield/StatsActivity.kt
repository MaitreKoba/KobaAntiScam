package fr.kobayashi.shield

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.kobayashi.shield.databinding.ActivityStatsBinding
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.RuleDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * FICHIER : StatsActivity.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/StatsActivity.kt
 * LIGNES : ~100 | RÔLE : Écran de statistiques | DATE : 2026-10-14
 * VERSION : 1.0.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi (via agent)
 * STATUT : STABLE | DÉPENDANCES : ActivityStatsBinding, RuleDatabase
 * LIEN : StatsActivity.kt | NOTES : Affiche les statistiques d'appels et SMS filtrés
 */

class StatsActivity : BaseActivity() {
    
    private lateinit var binding: ActivityStatsBinding
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        loadStats()
    }
    
    private fun loadStats() {
        coroutineScope.launch {
            val logDao = RuleDatabase.getDatabase().logDao()
            
            // Récupérer tous les logs
            val allLogs = logDao.getAllLogsList()
            
            // Compter par type
            val stats = mutableMapOf<String, Int>()
            
            // Initialiser tous les types
            for (type in LogType.values()) {
                stats[type.name] = 0
            }
            
            // Compter
            for (log in allLogs) {
                val key = log.type.name
                stats[key] = (stats[key] ?: 0) + 1
            }
            
            // Préparer les données pour l'UI
            val displayStats = mutableListOf<StatItem>()
            
            // Appels
            displayStats.add(StatItem("Appels autorisés", stats[LogType.CALL_INCOMING.name] ?: 0, android.graphics.Color.parseColor("#4CAF50")))
            displayStats.add(StatItem("Appels bloqués", stats[LogType.CALL_BLOCKED.name] ?: 0, android.graphics.Color.parseColor("#F44336")))
            displayStats.add(StatItem("Appels silencieux", stats[LogType.CALL_SILENCED.name] ?: 0, android.graphics.Color.parseColor("#FF9800")))
            displayStats.add(StatItem("Appels vers messagerie", stats[LogType.CALL_VOICEMAIL.name] ?: 0, android.graphics.Color.parseColor("#2196F3")))
            
            // SMS
            displayStats.add(StatItem("SMS reçus", stats[LogType.SMS_RECEIVED.name] ?: 0, android.graphics.Color.parseColor("#4CAF50")))
            displayStats.add(StatItem("SMS bloqués", stats[LogType.SMS_BLOCKED.name] ?: 0, android.graphics.Color.parseColor("#F44336")))
            
            // Autres
            displayStats.add(StatItem("Erreurs", stats[LogType.ERROR.name] ?: 0, android.graphics.Color.parseColor("#9C27B0")))
            
            // Total
            val totalCalls = (stats[LogType.CALL_INCOMING.name] ?: 0) + (stats[LogType.CALL_BLOCKED.name] ?: 0) + 
                     (stats[LogType.CALL_SILENCED.name] ?: 0) + (stats[LogType.CALL_VOICEMAIL.name] ?: 0)
            val totalSms = (stats[LogType.SMS_RECEIVED.name] ?: 0) + (stats[LogType.SMS_BLOCKED.name] ?: 0)
            
            displayStats.add(StatItem("Total appels", totalCalls, android.graphics.Color.parseColor("#607D8B")))
            displayStats.add(StatItem("Total SMS", totalSms, android.graphics.Color.parseColor("#607D8B")))
            
            withContext(Dispatchers.Main) {
                updateUI(displayStats)
            }
        }
    }
    
    private fun updateUI(stats: List<StatItem>) {
        val adapter = StatsAdapter(this, stats)
        binding.statsList.adapter = adapter
        
        // Calculer le total général
        val total = stats.sumOf { it.count }
        binding.totalText.text = "Total: $total événements"
    }
    
    data class StatItem(
        val label: String,
        val count: Int,
        val color: Int
    )
}

/**
 * Adapter pour la liste de statistiques
 */
class StatsAdapter(private val context: StatsActivity, private val items: List<StatsActivity.StatItem>) : 
    android.widget.BaseAdapter() {
    
    override fun getCount(): Int = items.size
    override fun getItem(position: Int): StatsActivity.StatItem = items[position]
    override fun getItemId(position: Int): Long = position.toLong()
    
    override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup?): android.view.View {
        val view = convertView ?: android.view.LayoutInflater.from(context).inflate(
            android.R.layout.simple_list_item_2, parent, false
        )
        
        val item = items[position]
        val text1 = view.findViewById<android.widget.TextView>(android.R.id.text1)
        val text2 = view.findViewById<android.widget.TextView>(android.R.id.text2)
        
        text1.text = item.label
        text2.text = item.count.toString()
        text2.setTextColor(item.color)
        
        return view
    }
}
