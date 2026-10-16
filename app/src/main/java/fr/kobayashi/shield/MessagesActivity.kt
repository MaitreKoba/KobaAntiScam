/*
 * FICHIER : MessagesActivity.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/MessagesActivity.kt
 * LIGNES : 250 | RÔLE : Écran Répondeur : messages vocaux reçus par e-mail (ligne OVH), filtrés par catégorie,
 *          écoute, transcription, rappel, liste blanche, spam | DATE : 2026-10-16 | VERSION : 1.9.0 | ORIGINE : Création
 * AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : activity_messages.xml, item_message.xml, paquet repondeur
 * LIEN : RepondeurActivity.kt | NOTES : « Supprimer » retire le message de l'app seulement, l'e-mail reste sur le serveur
 */
package fr.kobayashi.shield

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import fr.kobayashi.shield.databinding.ActivityMessagesBinding
import fr.kobayashi.shield.databinding.ItemMessageBinding
import fr.kobayashi.shield.model.Decision
import fr.kobayashi.shield.model.Rule
import fr.kobayashi.shield.model.RuleAction
import fr.kobayashi.shield.model.RuleDatabase
import fr.kobayashi.shield.model.RuleType
import fr.kobayashi.shield.model.Rules
import fr.kobayashi.shield.repondeur.Categorie
import fr.kobayashi.shield.repondeur.MessageVocal
import fr.kobayashi.shield.repondeur.MessagesVocaux
import fr.kobayashi.shield.repondeur.RepondeurConfig
import fr.kobayashi.shield.repondeur.Synchro
import fr.kobayashi.shield.service.ContactsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MessagesActivity : BaseActivity() {

    private lateinit var binding: ActivityMessagesBinding
    private val adaptateur = Adaptateur()
    private var lecteur: MediaPlayer? = null
    private var enLecture: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.liste.layoutManager = LinearLayoutManager(this)
        binding.liste.adapter = adaptateur
        binding.liste.addItemDecoration(MaterialDividerItemDecoration(this, MaterialDividerItemDecoration.VERTICAL).apply {
            isLastItemDecorated = false
            dividerInsetStart = (70 * resources.displayMetrics.density).toInt()
        })
        binding.btnRetour.setOnClickListener { finish() }
        binding.btnReglages.setOnClickListener { startActivity(Intent(this, RepondeurActivity::class.java)) }
        binding.btnConfigurer.setOnClickListener { startActivity(Intent(this, RepondeurActivity::class.java)) }
        binding.btnActualiser.setOnClickListener { actualiser(true) }
        binding.filtres.setOnCheckedStateChangeListener { _, _ -> afficher(MessagesVocaux.etat.value) }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) { MessagesVocaux.tous(applicationContext) }
            MessagesVocaux.etat.collect { afficher(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        val configure = RepondeurConfig.lire(this).complet
        binding.carteConfig.visibility = if (configure) View.GONE else View.VISIBLE
        if (configure) actualiser(false)
    }

    override fun onStop() {
        super.onStop()
        arreterLecture()
    }

    private fun actualiser(manuel: Boolean) {
        if (!RepondeurConfig.lire(this).complet) {
            if (manuel) startActivity(Intent(this, RepondeurActivity::class.java))
            return
        }
        binding.chargement.visibility = View.VISIBLE
        lifecycleScope.launch {
            val res = withContext(Dispatchers.IO) { Synchro.executer(applicationContext) }
            binding.chargement.visibility = View.GONE
            when {
                res.erreur != null -> Toast.makeText(this@MessagesActivity, "Boîte mail injoignable : ${res.erreur}", Toast.LENGTH_LONG).show()
                manuel -> Toast.makeText(this@MessagesActivity,
                    if (res.nouveaux == 0) "Aucun nouveau message" else "${res.nouveaux} nouveau(x) message(s)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun afficher(tous: List<MessageVocal>) {
        val cat: Set<Categorie>? = when (binding.filtres.checkedChipId) {
            R.id.fLivraison -> setOf(Categorie.LIVRAISON)
            R.id.fTravail -> setOf(Categorie.TRAVAIL)
            R.id.fDemarches -> setOf(Categorie.DEMARCHES)
            R.id.fAutre -> setOf(Categorie.AUTRE, Categorie.SANS_TEXTE)
            R.id.fDemarchage -> setOf(Categorie.DEMARCHAGE)
            else -> null
        }
        val liste = if (cat == null) tous else tous.filter { it.categorie in cat }
        adaptateur.submitList(liste)
        val vide = liste.isEmpty()
        binding.vide.visibility = if (vide) View.VISIBLE else View.GONE
        binding.carteListe.visibility = if (vide) View.GONE else View.VISIBLE
        val nonLus = tous.count { !it.lu }
        binding.tvResume.text = "${tous.size} message(s)" + if (nonLus > 0) ", $nonLus non lu(s)" else ""
    }

    private fun couleurCategorie(c: Categorie): Int = when (c) {
        Categorie.LIVRAISON -> 0xFFFF7E00.toInt()
        Categorie.TRAVAIL -> 0xFF2F80ED.toInt()
        Categorie.DEMARCHES -> 0xFF8E5CF7.toInt()
        Categorie.DEMARCHAGE -> ContextCompat.getColor(this, R.color.red)
        Categorie.AUTRE, Categorie.SANS_TEXTE -> ContextCompat.getColor(this, R.color.ink3)
    }

    /** Sûr : contact ou liste blanche. Spam : préfixe ARCEP ou démarchage reconnu. Sinon Inconnu. */
    private fun confiance(m: MessageVocal): Pair<String, Int> {
        val n = m.numero
        val blanche = ContactsHelper.getAllowlist(this) +
            (getSharedPreferences("shield", MODE_PRIVATE).getStringSet("allowlist", emptySet()) ?: emptySet())
        return when {
            n != null && (Decision.nomContact(this, n) != null || blanche.any { Rules.normalize(it) == n }) ->
                "Sûr" to ContextCompat.getColor(this, R.color.succes)
            m.categorie == Categorie.DEMARCHAGE || (n != null && Rules.isArcpPrefix(n)) ->
                "Spam" to ContextCompat.getColor(this, R.color.red)
            else -> "Inconnu" to ContextCompat.getColor(this, R.color.ink3)
        }
    }

    private fun texte(m: MessageVocal): String = when {
        m.transcription != null -> m.transcription
        m.audio == null -> "Aucun fichier audio dans l'e-mail. Touche les trois points, puis « Voir l'e-mail »."
        m.erreurTranscription != null -> "Transcription impossible : ${m.erreurTranscription}"
        RepondeurConfig.lire(this).cleGroq.isBlank() -> "Pas de texte : ajoute une clé Groq gratuite dans les réglages du répondeur, ou écoute le message."
        else -> "Pas encore transcrit."
    }

    private fun lier(b: ItemMessageBinding, m: MessageVocal) {
        val couleur = couleurCategorie(m.categorie)
        b.icone.imageTintList = ColorStateList.valueOf(couleur)
        b.icone.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(couleur, 0x2E))
        b.pointNonLu.visibility = if (m.lu) View.GONE else View.VISIBLE
        b.tvTitre.text = Synchro.titre(this, m)
        b.tvHeure.text = JournalVue.heure(m.date)
        b.tvCategorie.text = m.categorie.libelle
        b.tvCategorie.setTextColor(couleur)
        b.tvCategorie.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(couleur, 0x24))
        val (tag, cTag) = confiance(m)
        b.tvTag.text = tag
        b.tvTag.setTextColor(cTag)
        b.tvTag.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(cTag, 0x24))
        val numero = m.numero?.let { Rules.affichage(it) }
        b.tvNumero.text = if (numero != null && numero != b.tvTitre.text) numero else ""
        b.tvTexte.text = texte(m)
        b.tvTexte.maxLines = 4
        b.tvTexte.setOnClickListener {
            b.tvTexte.maxLines = if (b.tvTexte.maxLines == 4) Int.MAX_VALUE else 4
            marquerLu(m)
        }
        val joue = enLecture == m.uid
        b.btnEcouter.isEnabled = m.audio != null
        b.btnEcouter.text = if (joue) "Pause" else "Écouter"
        b.btnEcouter.setIconResource(if (joue) R.drawable.ic_pause else R.drawable.ic_play_arrow)
        b.btnEcouter.setOnClickListener { basculerLecture(m) }
        b.btnRappeler.isEnabled = m.numero != null
        b.btnRappeler.setOnClickListener { m.numero?.let { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))) } }
        b.btnPlus.setOnClickListener { menu(m) }
    }

    private fun marquerLu(m: MessageVocal) {
        if (!m.lu) lifecycleScope.launch(Dispatchers.IO) { MessagesVocaux.marquerLu(applicationContext, m.uid) }
    }

    private fun basculerLecture(m: MessageVocal) {
        if (enLecture == m.uid) {
            arreterLecture()
            return
        }
        arreterLecture()
        val f = m.audio?.let { File(it) }?.takeIf { it.exists() } ?: run {
            Toast.makeText(this, "Fichier audio introuvable", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            lecteur = MediaPlayer().apply {
                setDataSource(f.absolutePath)
                setOnCompletionListener { arreterLecture() }
                prepare()
                start()
            }
            enLecture = m.uid
            marquerLu(m)
        } catch (e: Exception) {
            Toast.makeText(this, "Lecture impossible : ${e.message}", Toast.LENGTH_LONG).show()
            arreterLecture()
        }
        adaptateur.notifyDataSetChanged()
    }

    private fun arreterLecture() {
        lecteur?.runCatching { stop(); release() }
        lecteur = null
        if (enLecture != null) {
            enLecture = null
            adaptateur.notifyDataSetChanged()
        }
    }

    private fun menu(m: MessageVocal) {
        val actions = mutableListOf<Pair<String, () -> Unit>>()
        m.numero?.let { n ->
            val lisible = Rules.affichage(n)
            actions += "Copier le numéro" to {
                getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("numéro", lisible))
                Toast.makeText(this, "Numéro copié", Toast.LENGTH_SHORT).show()
            }
            actions += "Autoriser ce numéro (il sonnera)" to {
                ContactsHelper.addToAllowlist(this, n)
                Toast.makeText(this, "$lisible ajouté à la liste blanche", Toast.LENGTH_SHORT).show()
                adaptateur.notifyDataSetChanged()
            }
            actions += "Marquer comme spam (rejeté)" to {
                lifecycleScope.launch {
                    RuleDatabase.getDatabase().ruleDao().insert(Rule(pattern = n, description = "Spam signalé depuis le répondeur ($lisible)",
                        action = RuleAction.BLOCK, ruleType = RuleType.CALL))
                    Toast.makeText(this@MessagesActivity, "$lisible sera rejeté", Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (m.audio != null) actions += "Relancer la transcription" to {
            binding.chargement.visibility = View.VISIBLE
            lifecycleScope.launch {
                val maj = withContext(Dispatchers.IO) { Synchro.retranscrire(applicationContext, m) }
                binding.chargement.visibility = View.GONE
                maj.erreurTranscription?.let { Toast.makeText(this@MessagesActivity, it, Toast.LENGTH_LONG).show() }
            }
        }
        actions += "Voir l'e-mail" to {
            MaterialAlertDialogBuilder(this).setTitle(m.sujet.ifBlank { "Sans sujet" })
                .setMessage(m.corps.ifBlank { "(e-mail sans texte)" }).setPositiveButton("Fermer", null).show()
        }
        actions += "Supprimer de l'app" to {
            if (enLecture == m.uid) arreterLecture()
            lifecycleScope.launch(Dispatchers.IO) { MessagesVocaux.supprimer(applicationContext, m.uid) }
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(Synchro.titre(this, m))
            .setItems(actions.map { it.first }.toTypedArray()) { _, i -> actions[i].second() }
            .show()
    }

    private inner class Adaptateur : ListAdapter<MessageVocal, Adaptateur.Ligne>(object : DiffUtil.ItemCallback<MessageVocal>() {
        override fun areItemsTheSame(a: MessageVocal, b: MessageVocal) = a.uid == b.uid
        override fun areContentsTheSame(a: MessageVocal, b: MessageVocal) = a == b
    }) {
        inner class Ligne(val b: ItemMessageBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Ligne(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: Ligne, position: Int) = lier(holder.b, getItem(position))
    }
}
