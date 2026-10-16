/*
 * FICHIER : RepondeurActivity.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/RepondeurActivity.kt
 * LIGNES : 170 | RÔLE : Réglages du répondeur OVH : boîte mail (test de connexion), numéro OVH (liste blanche),
 *          transcription Groq, codes de renvoi conditionnel Free prêts à composer | DATE : 2026-10-16 | VERSION : 1.9.0
 * ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : activity_repondeur.xml, paquet repondeur
 * LIEN : MessagesActivity.kt | NOTES : les codes s'ouvrent dans le composeur, l'utilisateur appuie lui-même sur Appeler.
 *        Jamais de renvoi permanent (**21) : contacts et patrons doivent continuer à sonner.
 */
package fr.kobayashi.shield

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import fr.kobayashi.shield.databinding.ActivityRepondeurBinding
import fr.kobayashi.shield.model.Rules
import fr.kobayashi.shield.repondeur.BoiteMail
import fr.kobayashi.shield.repondeur.RepondeurConfig
import fr.kobayashi.shield.repondeur.Synchro
import fr.kobayashi.shield.service.ContactsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepondeurActivity : BaseActivity() {

    private lateinit var binding: ActivityRepondeurBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRepondeurBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val r = RepondeurConfig.lire(this)
        binding.champEmail.setText(r.email)
        binding.champMotDePasse.setText(r.motDePasse)
        binding.champServeur.setText(r.serveur)
        binding.champPort.setText(r.port.toString())
        binding.champNumeroOvh.setText(r.numeroOvh.takeIf { it.isNotBlank() }?.let { Rules.affichage(it) } ?: "")
        binding.champCleGroq.setText(r.cleGroq)
        binding.switchTranscription.isChecked = r.transcription
        binding.switchNotifDemarchage.isChecked = r.notifierDemarchage

        binding.btnRetour.setOnClickListener { finish() }
        binding.btnEnregistrer.setOnClickListener { enregistrer(); finish() }
        binding.btnTester.setOnClickListener { tester() }
        binding.btnCleGroq.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys")))
        }
        majRenvois(r.numeroOvh)
        binding.champNumeroOvh.doAfterTextChanged { majRenvois(it?.toString().orEmpty()) }
    }

    private fun saisie() = RepondeurConfig.Reglages(
        email = binding.champEmail.text.toString().trim(),
        motDePasse = binding.champMotDePasse.text.toString(),
        serveur = binding.champServeur.text.toString().trim().ifBlank { RepondeurConfig.SERVEUR_DEFAUT },
        port = binding.champPort.text.toString().toIntOrNull() ?: RepondeurConfig.PORT_DEFAUT,
        numeroOvh = binding.champNumeroOvh.text.toString().filter { it.isDigit() || it == '+' }
            .takeIf { it.isNotBlank() }?.let { Rules.normalize(it) } ?: "",
        cleGroq = binding.champCleGroq.text.toString().trim(),
        transcription = binding.switchTranscription.isChecked,
        notifierDemarchage = binding.switchNotifDemarchage.isChecked,
    )

    private fun enregistrer() {
        val ancien = RepondeurConfig.lire(this).numeroOvh
        val r = saisie()
        RepondeurConfig.ecrire(this, r)
        if (ancien.isNotBlank() && ancien != r.numeroOvh) ContactsHelper.removeFromAllowlist(this, ancien)
        if (r.numeroOvh.isNotBlank()) ContactsHelper.addToAllowlist(this, r.numeroOvh)
        Synchro.planifier(this)
        Toast.makeText(this, if (r.complet) "Répondeur enregistré, vérification toutes les 15 min" else "Enregistré, boîte mail incomplète", Toast.LENGTH_SHORT).show()
    }

    private fun tester() {
        val r = saisie()
        if (!r.complet) {
            Toast.makeText(this, "Adresse et mot de passe nécessaires", Toast.LENGTH_SHORT).show()
            return
        }
        binding.tvTest.visibility = View.VISIBLE
        binding.tvTest.setTextColor(ContextCompat.getColor(this, R.color.ink3))
        binding.tvTest.text = "Connexion en cours..."
        lifecycleScope.launch {
            val res = withContext(Dispatchers.IO) { runCatching { BoiteMail.tester(r) } }
            res.onSuccess { n ->
                binding.tvTest.setTextColor(ContextCompat.getColor(this@RepondeurActivity, R.color.succes))
                binding.tvTest.text = "Connexion réussie : $n e-mail(s) dans la boîte."
            }.onFailure { e ->
                binding.tvTest.setTextColor(ContextCompat.getColor(this@RepondeurActivity, R.color.red))
                binding.tvTest.text = "Échec : ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    private fun majRenvois(saisi: String) {
        val chiffres = saisi.filter { it.isDigit() || it == '+' }
        val national = if (chiffres.isBlank()) "" else Rules.affichage(chiffres).replace(" ", "")
        val liste = binding.listeRenvois
        while (liste.childCount > 1) liste.removeViewAt(1)
        if (national.length != 10) {
            binding.tvRenvois.text = "Saisis le numéro de ta ligne OVH : les codes de renvoi apparaîtront ici, prêts à composer depuis ton 06."
            return
        }
        binding.tvRenvois.text = "Seuls les appels que tu ne prends pas partent vers $national : inconnus raccrochés par l'app, absence de réponse, téléphone éteint. Tes contacts sonnent toujours. Touche Composer, puis Appeler. Gratuit chez Free."
        listOf(
            Triple("Si occupé (inconnus filtrés par l'app)", "**67*$national#", "Composer"),
            Triple("Si pas de réponse", "**61*$national#", "Composer"),
            Triple("Si éteint ou sans réseau", "**62*$national#", "Composer"),
            Triple("Vérifier le renvoi si occupé", "*#67#", "Vérifier"),
            Triple("Tout annuler (retour à la messagerie Free)", "##002#", "Annuler"),
        ).forEach { (titre, code, bouton) -> liste.addView(ligneCode(titre, code, bouton)) }
    }

    private fun ligneCode(titre: String, code: String, bouton: String): View {
        val dp = resources.displayMetrics.density
        val textes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(context).apply { text = titre; textSize = 14f; setTypeface(typeface, android.graphics.Typeface.BOLD) })
            addView(TextView(context).apply {
                text = code; textSize = 13f; typeface = android.graphics.Typeface.MONOSPACE
                setTextColor(ContextCompat.getColor(context, R.color.ink3))
            })
        }
        val b = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = bouton
            isAllCaps = false
            setOnClickListener { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(code)))) }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (10 * dp).toInt(), 0, 0)
            addView(textes)
            addView(b)
        }
    }
}
