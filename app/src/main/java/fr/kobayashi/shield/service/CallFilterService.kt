/*
 * FICHIER : CallFilterService.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/service/CallFilterService.kt
 * LIGNES : 97 | RÔLE : Filtrage des appels entrants : applique le verdict de Decision et l'inscrit au journal avec sa raison ;
 *          appel envoyé au répondeur : alerte immédiate et relève rapide de la boîte (Synchro.appelAuRepondeur)
 * DATE : 2026-10-30 | VERSION : 1.10.0 | ORIGINE : Modification (alerte immédiate au répondeur)
 * AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : CallScreeningService, Decision.kt, RuleDatabase
 * LIEN : LogsActivity.kt | NOTES : plus de lecture du MP3 au rejet : il sortait du haut-parleur du téléphone, pas dans l'appel
 */
package fr.kobayashi.shield.service

import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import fr.kobayashi.shield.model.Decision
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.RuleAction
import fr.kobayashi.shield.model.RuleDatabase
import fr.kobayashi.shield.model.Rules
import fr.kobayashi.shield.repondeur.Synchro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallFilterService : CallScreeningService() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val tag = "Shield"

    override fun onScreenCall(details: Call.Details) {
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(details, reponse(Decision.Action.SONNE))
            return
        }
        val brut = extractNumber(details.handle)

        coroutineScope.launch {
            val verdict = try {
                Decision.decider(applicationContext, brut)
            } catch (e: Exception) {
                Log.e(tag, "Erreur de décision, l'appel sonne", e)
                Decision.Verdict(Decision.Action.SONNE, "Erreur interne, appel laissé passer")
            }
            Log.d(tag, "Appel ${brut.ifBlank { "masqué" }} -> ${verdict.action} (${verdict.raison})")
            respondToCall(details, reponse(verdict.action))
            journaliser(brut, verdict)
            if (verdict.action == Decision.Action.MESSAGERIE) {
                runCatching { Synchro.appelAuRepondeur(applicationContext, brut) }
                    .onFailure { Log.e(tag, "Alerte répondeur impossible", it) }
            }
        }
    }

    private fun reponse(action: Decision.Action): CallResponse = when (action) {
        Decision.Action.SONNE -> CallResponse.Builder()
            .setDisallowCall(false).setRejectCall(false).setSilenceCall(false)
            .setSkipCallLog(false).setSkipNotification(false).build()
        Decision.Action.REJET -> CallResponse.Builder()
            .setDisallowCall(true).setRejectCall(true)
            .setSkipCallLog(false).setSkipNotification(true).build()
        Decision.Action.MESSAGERIE -> CallResponse.Builder()
            .setDisallowCall(true).setRejectCall(false)
            .setSkipCallLog(false).setSkipNotification(false).build()
        Decision.Action.SILENCE -> CallResponse.Builder()
            .setDisallowCall(false).setRejectCall(false).setSilenceCall(true)
            .setSkipCallLog(false).setSkipNotification(false).build()
    }

    private suspend fun journaliser(brut: String, verdict: Decision.Verdict) {
        val (type, action) = when (verdict.action) {
            Decision.Action.SONNE -> LogType.CALL_INCOMING to RuleAction.ALLOW
            Decision.Action.REJET -> LogType.CALL_BLOCKED to RuleAction.BLOCK
            Decision.Action.MESSAGERIE -> LogType.CALL_VOICEMAIL to RuleAction.VOICEMAIL
            Decision.Action.SILENCE -> LogType.CALL_SILENCED to RuleAction.SILENCE
        }
        try {
            RuleDatabase.getDatabase().logDao().insert(
                LogEntry(
                    type = type,
                    action = action,
                    phoneNumber = brut.ifBlank { null },
                    message = verdict.nom,
                    details = verdict.raison
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Journal indisponible", e)
        }
    }

    private fun extractNumber(handle: Uri?): String {
        val partie = handle?.schemeSpecificPart ?: return ""
        if (partie.none { it.isDigit() }) return ""
        return Rules.normalize(partie)
    }
}
