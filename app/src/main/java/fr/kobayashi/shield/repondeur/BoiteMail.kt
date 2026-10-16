/*
 * FICHIER : BoiteMail.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/repondeur/BoiteMail.kt
 * LIGNES : 120 | RÔLE : Lecture IMAP (SSL) de la boîte qui reçoit les messages vocaux OVH : sujet, texte, date,
 *          pièce jointe audio enregistrée dans le dossier privé de l'app | DATE : 2026-10-16 | VERSION : 1.9.0
 * ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : com.sun.mail:android-mail
 * LIEN : Synchro.kt | NOTES : boîte ouverte en lecture seule, aucun e-mail n'est modifié ni supprimé sur le serveur.
 *        Seuls les e-mails d'UID supérieur au dernier traité sont lus.
 */
package fr.kobayashi.shield.repondeur

import android.content.Context
import com.sun.mail.imap.IMAPFolder
import java.io.File
import java.util.Properties
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.UIDFolder
import javax.mail.internet.MimeUtility

object BoiteMail {

    data class EmailBrut(val uid: Long, val date: Long, val expediteur: String, val sujet: String, val corps: String, val audio: File?)

    private val EXTENSIONS_AUDIO = listOf("wav", "mp3", "ogg", "oga", "opus", "m4a", "amr", "gsm")

    private fun session(r: RepondeurConfig.Reglages): Session = Session.getInstance(Properties().apply {
        put("mail.store.protocol", "imaps")
        put("mail.imaps.host", r.serveur)
        put("mail.imaps.port", r.port.toString())
        put("mail.imaps.ssl.enable", "true")
        put("mail.imaps.ssl.checkserveridentity", "true")
        put("mail.imaps.connectiontimeout", "15000")
        put("mail.imaps.timeout", "30000")
    })

    /** Vérifie la connexion et renvoie le nombre d'e-mails dans la boîte de réception */
    fun tester(r: RepondeurConfig.Reglages): Int {
        val store = session(r).getStore("imaps")
        store.connect(r.serveur, r.port, r.email, r.motDePasse)
        try {
            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            return inbox.messageCount.also { inbox.close(false) }
        } finally {
            store.close()
        }
    }

    fun nouveaux(context: Context, r: RepondeurConfig.Reglages, apresUid: Long, maximum: Int = 50): List<EmailBrut> {
        val store = session(r).getStore("imaps")
        store.connect(r.serveur, r.port, r.email, r.motDePasse)
        try {
            val inbox = store.getFolder("INBOX") as IMAPFolder
            inbox.open(Folder.READ_ONLY)
            val messages = inbox.getMessagesByUID(apresUid + 1, UIDFolder.LASTUID)
                .filterNotNull()
                .filter { inbox.getUID(it) > apresUid }
                .takeLast(maximum)
            val sortie = messages.map { m -> lire(context, inbox, m) }
            inbox.close(false)
            return sortie
        } finally {
            store.close()
        }
    }

    private fun lire(context: Context, dossier: IMAPFolder, m: Message): EmailBrut {
        val uid = dossier.getUID(m)
        val textes = StringBuilder()
        var audio: File? = null
        fun parcourir(p: Part) {
            val nom = p.fileName?.let { runCatching { MimeUtility.decodeText(it) }.getOrDefault(it) }
            val ext = nom?.substringAfterLast('.', "")?.lowercase().orEmpty()
            when {
                p.isMimeType("multipart/*") -> {
                    val mp = p.content as Multipart
                    for (i in 0 until mp.count) parcourir(mp.getBodyPart(i))
                }
                audio == null && (p.isMimeType("audio/*") || ext in EXTENSIONS_AUDIO) -> {
                    val extension = ext.ifBlank { if (p.isMimeType("audio/mpeg")) "mp3" else "wav" }
                    val f = File(MessagesVocaux.dossierAudio(context), "$uid.$extension")
                    p.inputStream.use { entree -> f.outputStream().use { entree.copyTo(it) } }
                    audio = f
                }
                p.isMimeType("text/plain") -> textes.appendLine(p.content.toString())
                p.isMimeType("text/html") && textes.isEmpty() ->
                    textes.appendLine(p.content.toString().replace(Regex("<[^>]+>"), " ").replace(Regex("&nbsp;"), " "))
            }
        }
        parcourir(m)
        return EmailBrut(
            uid = uid,
            date = (m.receivedDate ?: m.sentDate)?.time ?: System.currentTimeMillis(),
            expediteur = m.from?.firstOrNull()?.toString().orEmpty(),
            sujet = m.subject.orEmpty(),
            corps = textes.toString().replace(Regex("[ \\t]+"), " ").replace(Regex("\\n\\s*\\n+"), "\n").trim(),
            audio = audio,
        )
    }
}
