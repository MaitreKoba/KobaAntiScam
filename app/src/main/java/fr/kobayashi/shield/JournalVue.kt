/*
 * FICHIER : JournalVue.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/JournalVue.kt
 * LIGNES : 120 | RÔLE : Présentation d'une entrée du journal (icône, couleur, verdict, titre, raison, heure) et menu
 *          d'actions (autoriser, bloquer, appeler, copier) partagés par l'accueil et l'historique
 * DATE : 2026-10-16 | VERSION : 1.7.0 | ORIGINE : Refonte accueil et journal | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : item_log.xml, LogEntry.kt, ContactsHelper.kt | LIEN : LogAdapter.kt, MainActivity.kt
 * NOTES : les anciennes entrées sans raison affichent un texte générique
 */
package fr.kobayashi.shield

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.text.format.DateUtils
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.kobayashi.shield.databinding.ItemLogBinding
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.Rule
import fr.kobayashi.shield.model.RuleAction
import fr.kobayashi.shield.model.RuleDatabase
import fr.kobayashi.shield.model.RuleType
import fr.kobayashi.shield.model.Rules
import fr.kobayashi.shield.service.ContactsHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JournalVue {

    data class Style(val icone: Int, val couleur: Int, val verdict: String)

    fun style(log: LogEntry): Style = when (log.type) {
        LogType.CALL_INCOMING -> Style(R.drawable.ic_call, R.color.succes, "A sonné")
        LogType.CALL_BLOCKED -> Style(R.drawable.ic_phone_disabled, R.color.red, "Rejeté")
        LogType.CALL_VOICEMAIL -> Style(R.drawable.ic_voicemail, R.color.info, "Messagerie")
        LogType.CALL_SILENCED -> Style(R.drawable.ic_phone_callback, R.color.alerte, "Silencieux")
        LogType.SMS_RECEIVED -> Style(R.drawable.ic_sms, R.color.succes, "SMS")
        LogType.SMS_BLOCKED -> Style(R.drawable.ic_link_off, R.color.red, "Lien désarmé")
        LogType.UPDATE_CHECK -> Style(R.drawable.ic_download, R.color.info, "Mise à jour")
        LogType.ERROR -> Style(R.drawable.ic_error, R.color.red, "Erreur")
    }

    fun titre(log: LogEntry): String = when (log.type) {
        LogType.UPDATE_CHECK, LogType.ERROR -> log.message ?: log.displayText()
        LogType.SMS_RECEIVED, LogType.SMS_BLOCKED -> log.phoneNumber?.let { Rules.affichage(it) } ?: "Expéditeur inconnu"
        else -> {
            val numero = log.phoneNumber?.let { Rules.affichage(it) }
            when {
                log.message != null && log.details == "Contact" -> log.message
                numero != null -> numero
                else -> "Numéro masqué"
            }
        }
    }

    fun raison(log: LogEntry): String = log.details ?: when (log.type) {
        LogType.CALL_INCOMING -> "Appel autorisé"
        LogType.CALL_BLOCKED -> "Appel rejeté"
        LogType.CALL_VOICEMAIL -> "Envoyé en messagerie"
        LogType.CALL_SILENCED -> "Sonnerie coupée"
        LogType.SMS_RECEIVED, LogType.SMS_BLOCKED -> log.message ?: ""
        else -> ""
    }

    fun heure(log: LogEntry): String {
        val d = Date(log.timestamp)
        val h = SimpleDateFormat("HH:mm", Locale.FRANCE).format(d)
        return when {
            DateUtils.isToday(log.timestamp) -> h
            DateUtils.isToday(log.timestamp + DateUtils.DAY_IN_MILLIS) -> "Hier $h"
            else -> SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(d)
        }
    }

    fun lier(b: ItemLogBinding, log: LogEntry, scope: LifecycleCoroutineScope) {
        val ctx = b.root.context
        val s = style(log)
        val couleur = ContextCompat.getColor(ctx, s.couleur)
        b.icone.setImageResource(s.icone)
        b.icone.imageTintList = ColorStateList.valueOf(couleur)
        b.icone.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(couleur, 0x2E))
        b.tvTitre.text = titre(log)
        b.tvVerdict.text = s.verdict
        b.tvVerdict.setTextColor(couleur)
        b.tvVerdict.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(couleur, 0x24))
        val r = raison(log)
        b.tvRaison.text = r
        b.tvRaison.visibility = if (r.isBlank()) View.GONE else View.VISIBLE
        b.tvHeure.text = heure(log)
        val numero = log.phoneNumber
        if (numero != null && log.type in listOf(LogType.CALL_INCOMING, LogType.CALL_BLOCKED, LogType.CALL_VOICEMAIL, LogType.CALL_SILENCED)) {
            b.root.isClickable = true
            b.root.setOnClickListener { actions(ctx, numero, scope) }
        } else {
            b.root.isClickable = false
            b.root.setOnClickListener(null)
        }
    }

    private fun actions(ctx: Context, numero: String, scope: LifecycleCoroutineScope) {
        val lisible = Rules.affichage(numero)
        val choix = arrayOf("Autoriser ce numéro (il sonnera)", "Bloquer ce numéro", "Rappeler", "Copier le numéro")
        MaterialAlertDialogBuilder(ctx)
            .setTitle(lisible)
            .setItems(choix) { _, i ->
                when (i) {
                    0 -> {
                        ContactsHelper.addToAllowlist(ctx, Rules.normalize(numero))
                        Toast.makeText(ctx, "$lisible ajouté à la liste blanche", Toast.LENGTH_SHORT).show()
                    }
                    1 -> scope.launch {
                        RuleDatabase.getDatabase().ruleDao().insert(
                            Rule(pattern = Rules.normalize(numero), description = "Bloqué depuis l'historique ($lisible)",
                                action = RuleAction.BLOCK, ruleType = RuleType.CALL)
                        )
                        Toast.makeText(ctx, "$lisible sera rejeté", Toast.LENGTH_SHORT).show()
                    }
                    2 -> ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero")))
                    3 -> {
                        ctx.getSystemService(ClipboardManager::class.java)
                            ?.setPrimaryClip(ClipData.newPlainText("numéro", lisible))
                        Toast.makeText(ctx, "Numéro copié", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }
}
