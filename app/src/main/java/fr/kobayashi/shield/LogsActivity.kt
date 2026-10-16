/*
 * FICHIER : LogsActivity.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/LogsActivity.kt
 * LIGNES : 95 | RÔLE : Historique des appels et SMS filtrés, avec filtres et raison de chaque décision
 * DATE : 2026-10-16 | VERSION : 1.7.0 | ORIGINE : Refonte journal | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : activity_logs.xml, LogAdapter.kt, LogDao | LIEN : JournalVue.kt | NOTES : effacement confirmé par dialogue
 */
package fr.kobayashi.shield

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import fr.kobayashi.shield.databinding.ActivityLogsBinding
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.RuleDatabase
import kotlinx.coroutines.launch

class LogsActivity : BaseActivity() {

    private lateinit var binding: ActivityLogsBinding
    private lateinit var logAdapter: LogAdapter
    private var tous: List<LogEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        logAdapter = LogAdapter(lifecycleScope)
        binding.logsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@LogsActivity)
            adapter = logAdapter
            addItemDecoration(MaterialDividerItemDecoration(context, MaterialDividerItemDecoration.VERTICAL).apply {
                isLastItemDecorated = false
                dividerInsetStart = (70 * resources.displayMetrics.density).toInt()
            })
        }

        binding.btnRetour.setOnClickListener { finish() }
        binding.btnClearLogs.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Effacer l'historique ?")
                .setMessage("Les appels et SMS enregistrés seront supprimés de ce téléphone.")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Effacer") { _, _ ->
                    lifecycleScope.launch { RuleDatabase.getDatabase().logDao().clearAll() }
                }
                .show()
        }
        binding.filtres.setOnCheckedStateChangeListener { _, _ -> afficher() }

        lifecycleScope.launch {
            RuleDatabase.getDatabase().logDao().getAllLogs().collect { logs ->
                tous = logs
                afficher()
            }
        }
    }

    private fun afficher() {
        val types: Set<LogType>? = when (binding.filtres.checkedChipId) {
            R.id.fRejets -> setOf(LogType.CALL_BLOCKED)
            R.id.fMessagerie -> setOf(LogType.CALL_VOICEMAIL, LogType.CALL_SILENCED)
            R.id.fSonne -> setOf(LogType.CALL_INCOMING)
            R.id.fSms -> setOf(LogType.SMS_RECEIVED, LogType.SMS_BLOCKED)
            else -> null
        }
        val liste = if (types == null) tous else tous.filter { it.type in types }
        logAdapter.submitList(liste)
        val vide = liste.isEmpty()
        binding.emptyLogsText.visibility = if (vide) View.VISIBLE else View.GONE
        binding.carteListe.visibility = if (vide) View.GONE else View.VISIBLE

        val appels = tous.filter { it.type.name.startsWith("CALL") }
        val rejets = appels.count { it.type == LogType.CALL_BLOCKED }
        val messagerie = appels.count { it.type == LogType.CALL_VOICEMAIL || it.type == LogType.CALL_SILENCED }
        binding.tvResume.text = "${appels.size} appels : $rejets rejetés, $messagerie en messagerie"
    }
}
