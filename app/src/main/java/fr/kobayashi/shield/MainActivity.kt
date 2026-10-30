/*
 * FICHIER : MainActivity.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/MainActivity.kt
 * LIGNES : 174 | RÔLE : Accueil : interrupteur du système, compteurs du jour, dernière activité avec raisons, tuiles,
 *          état des services (rôle de filtrage, accès aux notifications, batterie) | DATE : 2026-10-30 | VERSION : 1.11.0
 * ORIGINE : Message d'accueil qui suit l'interrupteur | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : activity_main.xml, item_tuile.xml, item_log.xml, JournalVue.kt, Apparence.kt
 * LIEN : LogsActivity.kt | NOTES : clé "system_enabled" des prefs "shield" inchangée
 */
package fr.kobayashi.shield

import android.app.role.RoleManager
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.PowerManager
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import com.google.android.material.divider.MaterialDivider
import fr.kobayashi.shield.databinding.ActivityMainBinding
import fr.kobayashi.shield.databinding.ItemLogBinding
import fr.kobayashi.shield.databinding.ItemTuileBinding
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.RuleDatabase
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tuile(binding.btnLogs, R.drawable.ic_history, 0xFF2F80ED.toInt(), "Historique", "Chaque appel et sa raison", LogsActivity::class.java)
        tuile(binding.btnRepondeur, R.drawable.ic_voicemail, 0xFF0F9D9A.toInt(), "Répondeur", "Messages vocaux OVH", MessagesActivity::class.java)
        tuile(binding.btnRules, R.drawable.ic_rule, 0xFFFF7E00.toInt(), getString(R.string.nav_rules), getString(R.string.tuile_regles), RulesActivity::class.java)
        tuile(binding.btnStats, R.drawable.ic_insights, 0xFF8E5CF7.toInt(), getString(R.string.nav_stats), getString(R.string.tuile_stats), StatsActivity::class.java)
        tuile(binding.btnSettings, R.drawable.ic_tune, 0xFF85633B.toInt(), getString(R.string.nav_settings), getString(R.string.tuile_reglages), SettingsActivity::class.java)
        tuile(binding.btnApparence, R.drawable.ic_palette, 0xFFEC5D9C.toInt(), getString(R.string.nav_apparence), getString(R.string.tuile_apparence), AppearanceActivity::class.java)
        tuile(binding.btnUpdate, R.drawable.ic_download, 0xFF2E9E5B.toInt(), getString(R.string.nav_update), getString(R.string.tuile_maj), UpdateActivity::class.java)

        binding.btnApparenceRapide.setOnClickListener { startActivity(Intent(this, AppearanceActivity::class.java)) }
        binding.btnToutVoir.setOnClickListener { startActivity(Intent(this, LogsActivity::class.java)) }
        binding.carteServices.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.switchSystem.setOnCheckedChangeListener { _, actif ->
            val prefs = getSharedPreferences("shield", MODE_PRIVATE)
            if (prefs.getBoolean("system_enabled", true) == actif) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("system_enabled", actif).apply()
            majEtat()
            Toast.makeText(this, if (actif) "Protection activée" else "Protection désactivée", Toast.LENGTH_SHORT).show()
        }
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()
        binding.texteVersion.text = "Kobayashi AntiScam $version"

        lifecycleScope.launch {
            RuleDatabase.getDatabase().logDao().getAllLogs().collect { majActivite(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        majEtat()
        majServices()
        majDisposition()
        val nonLus = fr.kobayashi.shield.repondeur.MessagesVocaux.nonLus(this)
        binding.btnRepondeur.sousTitre.text = if (nonLus > 0) "$nonLus message(s) non lu(s)" else "Messages vocaux OVH"
    }

    private fun tuile(t: ItemTuileBinding, icone: Int, couleur: Int, titre: String, sous: String, cible: Class<*>) {
        t.icone.setImageResource(icone)
        t.icone.imageTintList = ColorStateList.valueOf(couleur)
        t.icone.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(couleur, 0x2E))
        t.titre.text = titre
        t.sousTitre.text = sous
        t.root.setOnClickListener { startActivity(Intent(this, cible)) }
    }

    private fun majEtat() {
        val actif = getSharedPreferences("shield", MODE_PRIVATE).getBoolean("system_enabled", true)
        binding.switchSystem.isChecked = actif
        val salut = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Bonjour"
            in 12..17 -> "Bon après-midi"
            else -> "Bonsoir"
        }
        binding.tvSalut.text = if (actif) "$salut, ton téléphone est surveillé" else "$salut, la protection est en pause"
        binding.titreEtat.text = if (actif) "Protection active" else "Protection en pause"
        binding.statusText.text = getString(if (actif) R.string.etat_actif_desc else R.string.etat_inactif_desc)
        binding.iconeEtat.setImageResource(if (actif) R.drawable.ic_verified_user else R.drawable.ic_remove_moderator)
        binding.fondEtat.setBackgroundResource(if (actif) R.drawable.bg_hero else R.drawable.bg_hero_off)
    }

    private fun majActivite(logs: List<LogEntry>) {
        val aujourdhui = logs.filter { DateUtils.isToday(it.timestamp) }
        binding.nbRejets.text = aujourdhui.count { it.type == LogType.CALL_BLOCKED }.toString()
        binding.nbMessagerie.text = aujourdhui.count { it.type == LogType.CALL_VOICEMAIL || it.type == LogType.CALL_SILENCED }.toString()
        binding.nbSonne.text = aujourdhui.count { it.type == LogType.CALL_INCOMING }.toString()

        val recents = logs.filter { it.type != LogType.UPDATE_CHECK && it.type != LogType.ERROR }.take(4)
        binding.listeRecente.removeAllViews()
        binding.tvAucuneActivite.visibility = if (recents.isEmpty()) View.VISIBLE else View.GONE
        recents.forEachIndexed { i, log ->
            if (i > 0) binding.listeRecente.addView(MaterialDivider(this).apply {
                dividerInsetStart = (70 * resources.displayMetrics.density).toInt()
            })
            val ligne = ItemLogBinding.inflate(layoutInflater, binding.listeRecente, false)
            JournalVue.lier(ligne, log, lifecycleScope)
            binding.listeRecente.addView(ligne.root)
        }
    }

    private fun majDisposition() {
        val liste = Apparence.lire(this, "accueil") == "liste"
        binding.grilleTuiles.columnCount = if (liste) 1 else 2
        for (i in 0 until binding.grilleTuiles.childCount) {
            val v = binding.grilleTuiles.getChildAt(i)
            v.layoutParams = (v.layoutParams as GridLayout.LayoutParams).apply {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
            }
        }
        val voir = Apparence.lire(this, "services") == "oui"
        binding.titreServices.visibility = if (voir) View.VISIBLE else View.GONE
        binding.carteServices.visibility = if (voir) View.VISIBLE else View.GONE
    }

    private fun majServices() {
        val roleOk = getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        val notifOk = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val batterieOk = getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == true
        binding.listeServices.removeAllViews()
        listOf(
            Triple("Filtrage des appels", roleOk, if (roleOk) "Rôle attribué" else "Rôle à attribuer dans les réglages"),
            Triple("Lecture des notifications SMS", notifOk, if (notifOk) "Accès activé" else "Accès à activer"),
            Triple("Batterie", batterieOk, if (batterieOk) "Pas de mise en veille" else "Exclure de l'optimisation (Honor)"),
        ).forEach { (nom, ok, detail) -> binding.listeServices.addView(ligneService(nom, ok, detail)) }
    }

    private fun ligneService(nom: String, ok: Boolean, detail: String): View {
        val dp = resources.displayMetrics.density
        val couleur = ContextCompat.getColor(this, if (ok) R.color.succes else R.color.red)
        val icone = ImageView(this).apply {
            setImageResource(if (ok) R.drawable.ic_check_circle else R.drawable.ic_error)
            imageTintList = ColorStateList.valueOf(couleur)
            layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt())
        }
        val textes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = (14 * dp).toInt() }
            addView(TextView(context).apply { text = nom; textSize = 15f; setTypeface(typeface, android.graphics.Typeface.BOLD) })
            addView(TextView(context).apply { text = detail; textSize = 12f; setTextColor(ContextCompat.getColor(context, R.color.ink3)) })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
            addView(icone)
            addView(textes)
        }
    }
}
