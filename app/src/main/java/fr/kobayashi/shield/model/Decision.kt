/*
 * FICHIER : Decision.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/model/Decision.kt
 * LIGNES : 250 | RÔLE : Décision pour un appel entrant (action + raison lisible pour le journal), dans l'ordre :
 *          système coupé, règle perso, contact, liste blanche, déjà appelé, démarchage ARCEP, rappel sous 3 min,
 *          pays d'arnaque au rappel, rotation de numéros, inconnu ou masqué | DATE : 2026-10-16 | VERSION : 1.9.0
 * ORIGINE : Refonte du filtrage | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : Rules.kt, ContactsHelper.kt, RuleDatabase | LIEN : CallFilterService.kt
 * NOTES : un inconnu n'est jamais rejeté sur simple soupçon : il part en messagerie pour qu'un vrai correspondant
 *         puisse laisser un message. Seuls ARCEP, pays d'arnaque au rappel, rotation et règles perso rejettent.
 */
package fr.kobayashi.shield.model

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import fr.kobayashi.shield.service.ContactsHelper

object Decision {
    private const val TAG = "Shield"
    private const val FENETRE_RAPPEL_MS = 3 * 60 * 1000L
    private const val JOUR_MS = 24 * 60 * 60 * 1000L
    const val SEUIL_TRANCHE_DEFAUT = 5

    enum class Action { SONNE, REJET, MESSAGERIE, SILENCE }

    data class Verdict(val action: Action, val raison: String, val nom: String? = null)

    private val PAYS_ARNAQUE_RAPPEL = mapOf(
        "225" to "Côte d'Ivoire", "223" to "Mali", "224" to "Guinée", "226" to "Burkina Faso",
        "229" to "Bénin", "231" to "Liberia", "232" to "Sierra Leone", "241" to "Gabon",
        "242" to "Congo", "243" to "RD Congo", "222" to "Mauritanie", "252" to "Somalie",
        "881" to "réseau satellite", "882" to "réseau international", "883" to "réseau international",
    )

    fun decider(context: Context, brut: String): Verdict {
        val prefs = context.getSharedPreferences("shield", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("system_enabled", true)) return Verdict(Action.SONNE, "Protection désactivée")

        val strict = ContactsHelper.isStrictModeEnabled(context)
        val inconnu = if (ContactsHelper.isVoicemailEnabled(context)) Action.MESSAGERIE else Action.SILENCE

        if (brut.isBlank()) {
            return if (strict) Verdict(Action.REJET, "Numéro masqué, mode strict")
            else Verdict(inconnu, "Numéro masqué")
        }
        val numero = Rules.normalize(brut)

        val regle = runCatching {
            RuleDatabase.getDatabase().ruleDao().getActiveRulesByType(RuleType.CALL)
        }.getOrDefault(emptyList()).firstOrNull { it.matches(numero) || it.matches(brut) }
        if (regle != null) {
            val libelle = regle.description?.takeIf { it.isNotBlank() } ?: regle.pattern
            val action = when (regle.action) {
                RuleAction.ALLOW -> Action.SONNE
                RuleAction.BLOCK -> Action.REJET
                RuleAction.SILENCE -> Action.SILENCE
                RuleAction.VOICEMAIL -> Action.MESSAGERIE
            }
            return Verdict(action, "Règle perso : $libelle")
        }

        val neutres = ContactsHelper.getNeutralList(context)
        val nomContact = nomContact(context, numero)
        if (nomContact != null && numero !in neutres) return Verdict(Action.SONNE, "Contact", nomContact)

        val blanche = ContactsHelper.getAllowlist(context) + (prefs.getStringSet("allowlist", emptySet()) ?: emptySet())
        if (blanche.any { correspond(numero, it) }) return Verdict(Action.SONNE, "Liste blanche")

        if (dejaAppele(context, numero)) return Verdict(Action.SONNE, "Numéro que tu as déjà appelé")

        if (Rules.isArcpPrefix(numero)) {
            noterAppelInconnu(context, numero)
            return Verdict(Action.REJET, "Démarchage : préfixe ARCEP ${Rules.affichage(numero).take(5)}")
        }

        if (rappelRecent(context, numero)) {
            noterAppelInconnu(context, numero)
            return Verdict(Action.SONNE, "A rappelé en moins de 3 min : sûrement une vraie personne")
        }

        val indicatif = PAYS_ARNAQUE_RAPPEL.keys.firstOrNull { numero.startsWith("+$it") }
        if (indicatif != null) {
            noterAppelInconnu(context, numero)
            return Verdict(Action.REJET, "Arnaque au rappel probable : +$indicatif ${PAYS_ARNAQUE_RAPPEL[indicatif]}")
        }

        val appelsTranche = noterAppelInconnu(context, numero)
        val seuil = context.getSharedPreferences("slice_prefs", Context.MODE_PRIVATE)
            .getInt("slice_threshold", SEUIL_TRANCHE_DEFAUT)
        if (appelsTranche > seuil) {
            return Verdict(Action.REJET, "Rotation de numéros : $appelsTranche numéros différents en ${tranche(numero)}xx en 24 h")
        }

        if (strict) return Verdict(Action.REJET, "Inconnu, mode strict")
        return if (numero.startsWith("+33")) Verdict(inconnu, "Inconnu")
        else Verdict(inconnu, "Inconnu, numéro étranger")
    }

    private fun correspond(numero: String, entree: String): Boolean {
        val e = Rules.normalize(entree)
        if (e.length < 4) return false
        return numero == e || (e.length < 12 && numero.startsWith(e))
    }

    private fun neufDerniers(numero: String) = numero.filter { it.isDigit() }.takeLast(9)

    /** PhoneLookup dépend du pays du téléphone : on essaie les formats +33 et 06, puis une comparaison sur les 9 derniers chiffres */
    fun nomContact(context: Context, numero: String): String? = try {
        val formats = listOfNotNull(numero, Rules.affichage(numero).replace(" ", "").takeIf { it != numero })
        formats.firstNotNullOfOrNull { f ->
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(f))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) ?: f else null
            }
        } ?: run {
            val cible = neufDerniers(numero)
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                null, null, null
            )?.use {
                var nom: String? = null
                while (nom == null && it.moveToNext()) {
                    val n = it.getString(0) ?: continue
                    if (cible.length == 9 && neufDerniers(n) == cible) nom = it.getString(1) ?: n
                }
                nom
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Recherche contact impossible: ${e.message}")
        null
    }

    private fun dejaAppele(context: Context, numero: String): Boolean = try {
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls._ID),
            "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.NUMBER} LIKE ?",
            arrayOf(CallLog.Calls.OUTGOING_TYPE.toString(), "%${neufDerniers(numero)}"), null
        )?.use { it.count > 0 } ?: false
    } catch (e: Exception) {
        Log.w(TAG, "Journal d'appels illisible: ${e.message}")
        false
    }

    private fun rappelRecent(context: Context, numero: String): Boolean {
        val dernier = context.getSharedPreferences("shield_rappels", Context.MODE_PRIVATE).getLong(numero, 0L)
        return dernier > 0 && System.currentTimeMillis() - dernier <= FENETRE_RAPPEL_MS
    }

    fun tranche(numero: String): String = Rules.affichage(numero).replace(" ", "").dropLast(2)

    /** Enregistre l'appel (fenêtre de rappel + tranche) et renvoie le nombre de numéros distincts de la tranche en 24 h */
    private fun noterAppelInconnu(context: Context, numero: String): Int {
        val maintenant = System.currentTimeMillis()
        val rappels = context.getSharedPreferences("shield_rappels", Context.MODE_PRIVATE)
        rappels.edit().apply {
            rappels.all.forEach { (k, v) -> if (v is Long && maintenant - v > JOUR_MS) remove(k) }
            putLong(numero, maintenant)
        }.apply()

        val tranches = context.getSharedPreferences("shield_tranches", Context.MODE_PRIVATE)
        val cle = tranche(numero)
        val entrees = (tranches.getString(cle, "") ?: "").split(',')
            .mapNotNull { e -> e.split('@').takeIf { it.size == 2 }?.let { it[0] to (it[1].toLongOrNull() ?: 0L) } }
            .filter { maintenant - it.second <= JOUR_MS && it.first != numero } + (numero to maintenant)
        tranches.edit().putString(cle, entrees.joinToString(",") { "${it.first}@${it.second}" }).apply()
        return entrees.size
    }
}
