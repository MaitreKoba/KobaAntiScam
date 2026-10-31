/*
 * FICHIER : MessagesVocaux.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/repondeur/MessagesVocaux.kt
 * LIGNES : 120 | RÔLE : Modèle d'un message vocal reçu par e-mail et stockage local (messages_vocaux.json + fichiers audio)
 * DATE : 2026-10-30 | VERSION : 1.11.0 | ORIGINE : Modification (réparation des messages OVH au chargement) | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : org.json (Android) | LIEN : Synchro.kt, MessagesActivity.kt
 * NOTES : tout reste dans le dossier privé de l'app. Un fichier JSON suffit pour quelques centaines de messages
 *         et évite une migration de la base Room existante. Le flux `etat` prévient l'écran à chaque changement.
 */
package fr.kobayashi.shield.repondeur

import android.content.Context
import fr.kobayashi.shield.model.Rules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class MessageVocal(
    val uid: Long,
    val date: Long,
    val numero: String?,
    val masque: Boolean,
    val sujet: String,
    val corps: String,
    val audio: String?,
    val transcription: String?,
    val categorie: Categorie,
    val lu: Boolean = false,
    val erreurTranscription: String? = null,
) {
    fun json(): JSONObject = JSONObject()
        .put("uid", uid).put("date", date).put("numero", numero ?: JSONObject.NULL).put("masque", masque)
        .put("sujet", sujet).put("corps", corps).put("audio", audio ?: JSONObject.NULL)
        .put("transcription", transcription ?: JSONObject.NULL).put("categorie", categorie.name).put("lu", lu)
        .put("erreur", erreurTranscription ?: JSONObject.NULL)

    companion object {
        fun depuis(o: JSONObject) = MessageVocal(
            uid = o.getLong("uid"),
            date = o.getLong("date"),
            numero = o.optString("numero").takeIf { !o.isNull("numero") && it.isNotBlank() },
            masque = o.optBoolean("masque"),
            sujet = o.optString("sujet"),
            corps = o.optString("corps"),
            audio = o.optString("audio").takeIf { !o.isNull("audio") && it.isNotBlank() },
            transcription = o.optString("transcription").takeIf { !o.isNull("transcription") && it.isNotBlank() },
            categorie = runCatching { Categorie.valueOf(o.optString("categorie")) }.getOrDefault(Categorie.AUTRE),
            lu = o.optBoolean("lu"),
            erreurTranscription = o.optString("erreur").takeIf { !o.isNull("erreur") && it.isNotBlank() },
        ).reparer()
    }

    /** Messages OVH enregistrés avant 1.11.0 : la ligne OVH était prise pour l'appelant et leur transcription ignorée */
    private fun reparer(): MessageVocal {
        if (!corps.contains("provenant du num", ignoreCase = true)) return this
        val n = Classement.numeroAppelant(listOf(sujet, corps))
        val t = transcription ?: Classement.transcriptionOvh(corps)
        val cat = when {
            n != null && Rules.isArcpPrefix(n) -> Categorie.DEMARCHAGE
            categorie == Categorie.SANS_TEXTE -> Classement.categorie(t)
            else -> categorie
        }
        return copy(numero = n, masque = n == null, transcription = t, categorie = cat, erreurTranscription = erreurTranscription.takeIf { t == null })
    }
}

object MessagesVocaux {
    private val verrou = Any()
    private val _etat = MutableStateFlow<List<MessageVocal>>(emptyList())
    val etat: StateFlow<List<MessageVocal>> = _etat
    private var charge = false

    private fun fichier(context: Context) = File(context.filesDir, "messages_vocaux.json")
    fun dossierAudio(context: Context) = File(context.filesDir, "vocaux").apply { mkdirs() }

    fun tous(context: Context): List<MessageVocal> = synchronized(verrou) {
        if (!charge) {
            val f = fichier(context)
            _etat.value = if (!f.exists()) emptyList() else runCatching {
                val a = JSONArray(f.readText())
                (0 until a.length()).map { MessageVocal.depuis(a.getJSONObject(it)) }
            }.getOrDefault(emptyList()).sortedByDescending { it.date }
            charge = true
        }
        _etat.value
    }

    private fun sauver(context: Context, liste: List<MessageVocal>) {
        val a = JSONArray()
        liste.forEach { a.put(it.json()) }
        val tmp = File(context.filesDir, "messages_vocaux.json.tmp")
        tmp.writeText(a.toString())
        tmp.renameTo(fichier(context))
        _etat.value = liste.sortedByDescending { it.date }
    }

    fun dernierUid(context: Context): Long = tous(context).maxOfOrNull { it.uid } ?: 0L

    fun ajouter(context: Context, nouveaux: List<MessageVocal>) = synchronized(verrou) {
        if (nouveaux.isEmpty()) return@synchronized
        val uids = nouveaux.map { it.uid }.toSet()
        sauver(context, tous(context).filter { it.uid !in uids } + nouveaux)
    }

    fun remplacer(context: Context, m: MessageVocal) = synchronized(verrou) {
        sauver(context, tous(context).map { if (it.uid == m.uid) m else it })
    }

    fun marquerLu(context: Context, uid: Long) = synchronized(verrou) {
        sauver(context, tous(context).map { if (it.uid == uid) it.copy(lu = true) else it })
    }

    fun supprimer(context: Context, uid: Long) = synchronized(verrou) {
        tous(context).firstOrNull { it.uid == uid }?.audio?.let { File(it).delete() }
        sauver(context, tous(context).filter { it.uid != uid })
    }

    fun nonLus(context: Context): Int = tous(context).count { !it.lu }
}
