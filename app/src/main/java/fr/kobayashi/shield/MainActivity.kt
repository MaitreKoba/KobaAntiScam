/*
 * FICHIER : MainActivity.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/MainActivity.kt
 * LIGNES : 130 | RÔLE : Accueil : interrupteur du système, tuiles de navigation, état des services (rôle de filtrage,
 *          accès aux notifications, batterie) | DATE : 2026-10-15 | VERSION : 1.6.0 | ORIGINE : Refonte UI
 * AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : activity_main.xml, item_tuile.xml, Apparence.kt
 * LIEN : SettingsActivity.kt | NOTES : clé "system_enabled" des prefs "shield" inchangée
 */
package fr.kobayashi.shield

import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import fr.kobayashi.shield.databinding.ActivityMainBinding
import fr.kobayashi.shield.databinding.ItemTuileBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tuile(binding.btnRules, "📋", getString(R.string.nav_rules), getString(R.string.tuile_regles), RulesActivity::class.java)
        tuile(binding.btnLogs, "📜", getString(R.string.nav_logs), getString(R.string.tuile_journal), LogsActivity::class.java)
        tuile(binding.btnStats, "📊", getString(R.string.nav_stats), getString(R.string.tuile_stats), StatsActivity::class.java)
        tuile(binding.btnSettings, "⚙️", getString(R.string.nav_settings), getString(R.string.tuile_reglages), SettingsActivity::class.java)
        tuile(binding.btnApparence, "🎨", getString(R.string.nav_apparence), getString(R.string.tuile_apparence), AppearanceActivity::class.java)
        tuile(binding.btnUpdate, "⬇️", getString(R.string.nav_update), getString(R.string.tuile_maj), UpdateActivity::class.java)

        binding.btnApparenceRapide.setOnClickListener { startActivity(Intent(this, AppearanceActivity::class.java)) }
        binding.carteServices.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.switchSystem.setOnCheckedChangeListener { _, actif ->
            val prefs = getSharedPreferences("shield", MODE_PRIVATE)
            if (prefs.getBoolean("system_enabled", true) == actif) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("system_enabled", actif).apply()
            majEtat()
            Toast.makeText(this, if (actif) "Système activé" else "Système désactivé", Toast.LENGTH_SHORT).show()
        }
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()
        binding.texteVersion.text = "Kobayashi AntiScam $version"
    }

    override fun onResume() {
        super.onResume()
        majEtat()
        majServices()
        majDisposition()
    }

    private fun tuile(t: ItemTuileBinding, icone: String, titre: String, sous: String, cible: Class<*>) {
        t.icone.text = icone
        t.titre.text = titre
        t.sousTitre.text = sous
        t.root.setOnClickListener { startActivity(Intent(this, cible)) }
    }

    private fun majEtat() {
        val actif = getSharedPreferences("shield", MODE_PRIVATE).getBoolean("system_enabled", true)
        binding.switchSystem.isChecked = actif
        binding.titreEtat.text = getString(if (actif) R.string.system_active else R.string.system_inactive)
        binding.statusText.text = getString(if (actif) R.string.etat_actif_desc else R.string.etat_inactif_desc)
        binding.iconeEtat.text = if (actif) "🛡️" else "💤"
        binding.fondEtat.setBackgroundResource(if (actif) R.drawable.bg_hero else R.drawable.bg_hero_off)
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
        val pastille = View(this).apply {
            background = ContextCompat.getDrawable(context, R.drawable.pastille)?.mutate()
            background.setTint(ContextCompat.getColor(context, if (ok) R.color.succes else R.color.red))
            layoutParams = LinearLayout.LayoutParams((10 * dp).toInt(), (10 * dp).toInt())
        }
        val textes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = (12 * dp).toInt() }
            addView(TextView(context).apply { text = nom; textSize = 15f; setTypeface(typeface, android.graphics.Typeface.BOLD) })
            addView(TextView(context).apply { text = detail; textSize = 12f; setTextColor(ContextCompat.getColor(context, R.color.ink3)) })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())
            addView(pastille)
            addView(textes)
        }
    }
}
