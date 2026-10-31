/*
 * FICHIER : SettingsActivity.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/SettingsActivity.kt
 * LIGNES : 456 | RÔLE : Réglages en sections : à corriger, accès, répondeur, modules, inconnus, listes, sauvegarde, application
 * DATE : 2026-10-31 | VERSION : 1.16.0 | ORIGINE : Refonte de l'écran, import et export réels (sélecteur de fichiers)
 * AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : activity_settings.xml, Module, Sauvegarde, Ecoute
 * LIEN : MainActivity.kt | NOTES : interface construite par le code pour rester homogène ; la liste neutre est retirée
 *        (sans effet : Android ne présente pas les appels des contacts à l'app).
 */
package fr.kobayashi.shield

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.text.format.DateUtils
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import fr.kobayashi.shield.databinding.ActivitySettingsBinding
import fr.kobayashi.shield.model.Decision
import fr.kobayashi.shield.model.Module
import fr.kobayashi.shield.model.Rules
import fr.kobayashi.shield.model.Sauvegarde
import fr.kobayashi.shield.repondeur.Ecoute
import fr.kobayashi.shield.repondeur.RepondeurConfig
import fr.kobayashi.shield.repondeur.Synchro
import fr.kobayashi.shield.service.ContactsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val dp by lazy { resources.displayMetrics.density }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnRetour.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        construire()
    }

    private val demandeRole = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Toast.makeText(this, if (roleFiltrage()) "Filtrage des appels activé" else "Rôle refusé : l'app ne verra pas les appels", Toast.LENGTH_LONG).show()
        construire()
    }

    private val choixExport = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val ok = runCatching {
                val json = Sauvegarde.versJson(applicationContext)
                withContext(Dispatchers.IO) { contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } }
            }
            Toast.makeText(this@SettingsActivity, ok.fold({ "Sauvegarde enregistrée" }, { "Export impossible : ${it.message}" }), Toast.LENGTH_LONG).show()
        }
    }

    private val choixImport = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val res = runCatching {
                val texte = withContext(Dispatchers.IO) { contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }
                    ?: error("Fichier illisible")
                Sauvegarde.depuisJson(applicationContext, texte)
            }
            AlertDialog.Builder(this@SettingsActivity)
                .setTitle(if (res.isSuccess) "Import terminé" else "Import impossible")
                .setMessage(res.fold({ it }, { it.message ?: it.javaClass.simpleName }))
                .setPositiveButton("OK", null)
                .show()
            Ecoute.appliquer(applicationContext)
            construire()
        }
    }

    private fun roleFiltrage(): Boolean = getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

    // ---- Construction de l'écran ----

    private fun construire() {
        val zone = binding.contenuReglages
        zone.removeAllViews()
        val role = roleFiltrage()
        val alertes = Notifs.autorisees(this)
        val sms = isNotificationServiceEnabled()
        val batterie = !isBatteryOptimized()

        val manques = listOfNotNull(
            if (!role) Pair("Filtrage des appels inactif : l'app ne voit aucun appel") { requestCallFilterRole() } else null,
            if (!alertes) Pair("Notifications bloquées : aucune alerte ne s'affichera") { ouvrirNotifs() } else null,
            if (!sms) Pair("Lecture des SMS inactive : pas d'anti-arnaque SMS") { requestNotificationAccess() } else null,
        )
        if (manques.isNotEmpty()) {
            section(zone, "À corriger")
            val c = carte(zone)
            manques.forEach { (texte, action) -> ligne(c, texte, "Touche pour corriger", R.color.red, action) }
        }

        section(zone, "Accès")
        carte(zone).let { c ->
            ligne(c, "Filtrage des appels", if (role) "Actif" else "Inactif, touche pour l'activer", etat(role)) { requestCallFilterRole() }
            ligne(c, "Notifications", if (alertes) "Autorisées" else "Bloquées, touche pour les autoriser", etat(alertes)) { ouvrirNotifs() }
            ligne(c, "Lecture des SMS", if (sms) "Active" else "Inactive, touche pour l'activer", etat(sms)) { requestNotificationAccess() }
            ligne(c, "Batterie", if (batterie) "Pas de mise en veille" else "Optimisée : Honor peut couper l'app", etat(batterie)) { disableBatteryOptimization() }
            ligne(c, "Lancement automatique (Honor)", "Autorise l'app à tourner en arrière-plan", null) { ouvrirLancementHonor() }
        }

        section(zone, "Répondeur")
        carte(zone).let { c ->
            val r = RepondeurConfig.lire(this)
            ligne(c, "Boîte mail, clé Groq, renvois", if (r.complet) "Configuré : ${r.email}" else "Non configuré", etat(r.complet)) {
                startActivity(Intent(this, RepondeurActivity::class.java))
            }
        }

        Module.values().groupBy { it.section }.forEach { (nom, modules) ->
            section(zone, nom)
            val c = carte(zone)
            modules.forEach { m ->
                interrupteur(c, m.titre, m.description, m.actif(this)) { actif ->
                    m.regler(this, actif)
                    if (m == Module.ECOUTE) Ecoute.appliquer(this)
                }
            }
        }

        section(zone, "Inconnus")
        carte(zone).let { c ->
            interrupteur(c, "Envoyer au répondeur", "Coupé : ils ne sonnent pas mais restent affichés (sonnerie coupée).",
                ContactsHelper.isVoicemailEnabled(this)) { ContactsHelper.setVoicemailEnabled(this, it) }
            interrupteur(c, "Mode strict", "Rejette tous les numéros hors contacts et liste blanche, sans répondeur.",
                ContactsHelper.isStrictModeEnabled(this)) { ContactsHelper.setStrictModeEnabled(this, it) }
        }

        section(zone, "Listes")
        carte(zone).let { c ->
            ligne(c, "Liste blanche", "${ContactsHelper.getAllowlist(this).size} numéro(s) qui sonnent toujours", null) { showAllowlistDialog() }
            ligne(c, "Règles perso", "Autoriser, bloquer ou envoyer au répondeur un numéro ou un préfixe", null) {
                startActivity(Intent(this, RulesActivity::class.java))
            }
        }

        section(zone, "Sauvegarde")
        carte(zone).let { c ->
            ligne(c, "Exporter", "Listes, modes, modules et règles dans un fichier JSON", null) {
                choixExport.launch("kobayashi-antiscam-" + SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date()) + ".json")
            }
            ligne(c, "Importer", "Restaurer depuis un fichier exporté (fusion, sans doublon)", null) {
                choixImport.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            }
        }

        section(zone, "Application")
        carte(zone).let { c ->
            ligne(c, "Mise à jour", "Vérifier et installer la dernière version", null) { startActivity(Intent(this, UpdateActivity::class.java)) }
            ligne(c, "Diagnostic et tests", "État complet, tester une notification, relever, simuler un appel", null) { afficherDiagnostic() }
            ligne(c, "Crédits et à propos", "Auteur, services, bibliothèques, vie privée", null) { afficherCredits() }
        }
    }

    private fun etat(ok: Boolean) = if (ok) R.color.succes else R.color.red

    private fun section(zone: LinearLayout, titre: String) = zone.addView(TextView(this).apply {
        text = titre
        textSize = 13f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(ContextCompat.getColor(context, R.color.ink3))
        setPadding((4 * dp).toInt(), (18 * dp).toInt(), 0, (6 * dp).toInt())
    })

    private fun carte(zone: LinearLayout): LinearLayout {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, (4 * dp).toInt(), 0, (4 * dp).toInt()) }
        zone.addView(MaterialCardView(this).apply { radius = 16 * dp; cardElevation = 0f; addView(col) })
        return col
    }

    private fun textes(titre: String, sous: String, couleurSous: Int?) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(context).apply { text = titre; textSize = 15f; setTypeface(typeface, Typeface.BOLD) })
        addView(TextView(context).apply {
            text = sous
            textSize = 12.5f
            setTextColor(ContextCompat.getColor(context, couleurSous ?: R.color.ink3))
        })
    }

    private fun ligne(c: LinearLayout, titre: String, sous: String, couleurSous: Int?, action: () -> Unit) {
        val rangee = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt())
            isClickable = true
            setOnClickListener { action() }
        }
        rangee.addView(textes(titre, sous, couleurSous), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        rangee.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_chevron_right)
            imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ink3))
        })
        c.addView(rangee)
    }

    private fun interrupteur(c: LinearLayout, titre: String, sous: String, valeur: Boolean, changer: (Boolean) -> Unit) {
        val rangee = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (10 * dp).toInt(), (12 * dp).toInt(), (10 * dp).toInt())
        }
        rangee.addView(textes(titre, sous, null), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val bascule = MaterialSwitch(this).apply { isChecked = valeur; setOnCheckedChangeListener { _, v -> changer(v) } }
        rangee.addView(bascule)
        rangee.setOnClickListener { bascule.toggle() }
        c.addView(rangee)
    }

    private fun ouvrirNotifs() =
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))

    /** Gestion du lancement des apps Honor ; à défaut, la fiche de l'app dans les réglages Android */
    private fun ouvrirLancementHonor() {
        val candidats = listOf(
            ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        )
        val ouvert = candidats.any { runCatching { startActivity(Intent().setComponent(it)) }.isSuccess }
        if (!ouvert) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Batterie, puis Lancement des applications : gestion manuelle, tout autoriser", Toast.LENGTH_LONG).show()
        }
    }

    private fun afficherDiagnostic() {
        val ok = { b: Boolean -> if (b) "OK" else "À CORRIGER" }
        val r = RepondeurConfig.lire(this)
        val (date, resultat) = Synchro.derniereReleve(this)
        val lignes = listOf(
            "Version" to runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty(),
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Protection" to if (getSharedPreferences("shield", MODE_PRIVATE).getBoolean("system_enabled", true)) "active" else "EN PAUSE",
            "Notifications" to ok(Notifs.autorisees(this)),
            "Filtrage des appels" to ok(getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true),
            "Lecture des SMS (notifications)" to ok(isNotificationServiceEnabled()),
            "Batterie (Honor)" to ok(getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == true),
            "Répondeur" to if (r.complet) "configuré" else "NON CONFIGURÉ",
            "Numéro OVH" to r.numeroOvh.ifBlank { "absent" },
            "Clé Groq" to if (r.cleGroq.isBlank()) "absente (texte OVH utilisé)" else "présente",
            "Dernière relève" to if (date == 0L) resultat else "${DateUtils.getRelativeTimeSpanString(date)} : $resultat",
        )
        AlertDialog.Builder(this)
            .setTitle("Diagnostic")
            .setMessage(lignes.joinToString("\n") { "${it.first} : ${it.second}" })
            .setPositiveButton("Tester une notification") { _, _ ->
                if (Notifs.test(this)) Toast.makeText(this, "Notification envoyée : regarde le volet", Toast.LENGTH_SHORT).show()
                else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }
            .setNeutralButton("Relever maintenant") { _, _ ->
                lifecycleScope.launch {
                    val res = withContext(Dispatchers.IO) { Synchro.executer(applicationContext) }
                    Toast.makeText(this@SettingsActivity, res.erreur ?: "${res.nouveaux} nouveau(x) message(s)", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Simuler un appel") { _, _ -> simulerAppel() }
            .show()
    }

    /** Verdict qu'aurait un appel entrant, sans appel réel ni enregistrement ; affiche l'alerte correspondante */
    private fun simulerAppel() {
        val champ = EditText(this).apply { hint = "ex. 0948112233 ou 0612345678"; inputType = InputType.TYPE_CLASS_PHONE }
        AlertDialog.Builder(this)
            .setTitle("Simuler un appel entrant")
            .setView(champ)
            .setPositiveButton("Simuler") { _, _ ->
                val brut = Rules.normalize(champ.text.toString())
                lifecycleScope.launch {
                    val v = withContext(Dispatchers.IO) { Decision.decider(applicationContext, brut, simulation = true) }
                    when (v.action) {
                        Decision.Action.REJET -> Notifs.appelBloque(applicationContext, brut, "Simulation : ${v.raison}")
                        else -> Notifs.test(applicationContext)
                    }
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle(if (brut.isBlank()) "Numéro masqué" else Rules.affichage(brut))
                        .setMessage("Verdict : ${v.action}\nRaison : ${v.raison}" + (v.nom?.let { "\nContact : $it" } ?: ""))
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun afficherCredits() {
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()
        AlertDialog.Builder(this)
            .setTitle("Kobayashi AntiScam $version")
            .setMessage(
                "Conception et développement : Maitre Kobayashi\n" +
                "Code source : github.com/MaitreKoba/KobaAntiScam\n\n" +
                "Services utilisés\n" +
                "• OVHcloud : ligne VoIP, répondeur et transcription des messages\n" +
                "• Free Mobile : renvois d'appel conditionnels\n" +
                "• Groq (Whisper) : transcription facultative\n\n" +
                "Bibliothèques\n" +
                "• AndroidX, Material Components, Room, WorkManager\n" +
                "• Retrofit, OkHttp, Gson, JavaMail Android (IMAP), Security Crypto\n\n" +
                "Vie privée : historique, messages et clés restent sur ce téléphone. Aucune télémétrie."
            )
            .setPositiveButton("Fermer", null)
            .show()
    }

    private fun requestCallFilterRole() {
        val rm = getSystemService(RoleManager::class.java)
        when {
            rm == null || !rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) ->
                Toast.makeText(this, "Ce téléphone ne propose pas le filtrage d'appels aux apps", Toast.LENGTH_LONG).show()
            rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ->
                Toast.makeText(this, "Le filtrage des appels est déjà actif", Toast.LENGTH_SHORT).show()
            else -> demandeRole.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
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

    // L'ancienne version passait par une méthode cachée d'Android et répondait toujours non
    private fun isNotificationServiceEnabled(): Boolean =
        androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    
    private fun isBatteryOptimized(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val pkgName = packageName
            !powerManager.isIgnoringBatteryOptimizations(pkgName)
        } else {
            false
        }
    }
}
