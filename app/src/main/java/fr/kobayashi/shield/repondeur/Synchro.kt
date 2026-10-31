/*
 * FICHIER : Synchro.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/repondeur/Synchro.kt
 * LIGNES : 198 | RÔLE : Récupère les nouveaux messages vocaux (IMAP), les transcrit, les classe, les enregistre et
 *          notifie (transcription Groq, sinon texte de l'e-mail OVH) ; tâche WorkManager toutes les 15 min
 * DATE : 2026-10-30 | VERSION : 1.12.0 | ORIGINE : Modification (autorisation réelle des notifications, dernière relève) | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : BoiteMail.kt, Transcription.kt, Classement.kt, MessagesVocaux.kt, work-runtime-ktx
 * LIEN : MessagesActivity.kt, ShieldApp.kt | NOTES : le démarchage n'est pas notifié sauf réglage contraire
 */
package fr.kobayashi.shield.repondeur

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.kobayashi.shield.MessagesActivity
import fr.kobayashi.shield.Notifs
import fr.kobayashi.shield.R
import fr.kobayashi.shield.model.Decision
import fr.kobayashi.shield.model.Rules
import java.util.concurrent.TimeUnit

object Synchro {
    private const val TAG = "Shield"
    private const val CANAL = "repondeur"
    private const val TRAVAIL = "synchro_repondeur"
    private const val CANAL_APPELS = "appels_repondeur"
    private const val PREFS = "repondeur_releve"
    private val RELANCES_S = listOf(45L, 90L, 180L, 360L)

    data class Resultat(val nouveaux: Int, val erreur: String? = null)

    /** Heure et résultat de la dernière relève, pour le diagnostic */
    fun derniereReleve(context: Context): Pair<Long, String> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getLong("date", 0L) to p.getString("resultat", "Jamais relevée").orEmpty()
    }

    @Synchronized
    fun executer(context: Context): Resultat = relever(context).also { res ->
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("date", System.currentTimeMillis())
            .putString("resultat", res.erreur ?: "${res.nouveaux} nouveau(x) message(s)")
            .apply()
    }

    private fun relever(context: Context): Resultat {
        val r = RepondeurConfig.lire(context)
        if (!r.complet) return Resultat(0, "Répondeur non configuré")
        val emails = try {
            BoiteMail.nouveaux(context, r, MessagesVocaux.dernierUid(context))
        } catch (e: Exception) {
            Log.w(TAG, "Lecture de la boîte impossible: ${e.message}")
            return Resultat(0, e.message ?: e.javaClass.simpleName)
        }
        val messages = emails.map { e -> traiter(context, r, e) }
        MessagesVocaux.ajouter(context, messages)
        messages.filter { it.categorie != Categorie.DEMARCHAGE || r.notifierDemarchage }.forEach { notifier(context, it) }
        Log.d(TAG, "Répondeur : ${messages.size} nouveau(x) message(s)")
        return Resultat(messages.size)
    }

    private fun traiter(context: Context, r: RepondeurConfig.Reglages, e: BoiteMail.EmailBrut): MessageVocal {
        val textes = listOf(e.sujet, e.corps)
        val numero = Classement.numeroAppelant(textes, listOf(r.numeroOvh))
        var transcription: String? = null
        var erreur: String? = null
        if (r.transcription && r.cleGroq.isNotBlank() && e.audio != null) {
            try {
                transcription = Transcription.transcrire(e.audio, r.cleGroq)
            } catch (ex: Exception) {
                erreur = ex.message ?: ex.javaClass.simpleName
                Log.w(TAG, "Transcription impossible: $erreur")
            }
        }
        if (transcription == null) transcription = Classement.transcriptionOvh(e.corps)
        if (transcription != null) erreur = null
        val categorie = if (numero != null && Rules.isArcpPrefix(numero)) Categorie.DEMARCHAGE else Classement.categorie(transcription)
        return MessageVocal(
            uid = e.uid, date = e.date, numero = numero, masque = numero == null && Classement.estMasque(textes),
            sujet = e.sujet, corps = e.corps, audio = e.audio?.absolutePath, transcription = transcription,
            categorie = categorie, erreurTranscription = erreur,
        )
    }

    /** Relance la transcription d'un message déjà reçu (clé ajoutée après coup, erreur réseau) */
    fun retranscrire(context: Context, m: MessageVocal): MessageVocal {
        val r = RepondeurConfig.lire(context)
        val audio = m.audio?.let { java.io.File(it) }?.takeIf { it.exists() } ?: return m
        if (r.cleGroq.isBlank()) return m.copy(erreurTranscription = "Aucune clé Groq dans les réglages du répondeur")
        val maj = try {
            val t = Transcription.transcrire(audio, r.cleGroq)
            val cat = if (m.numero != null && Rules.isArcpPrefix(m.numero)) Categorie.DEMARCHAGE else Classement.categorie(t)
            m.copy(transcription = t, categorie = cat, erreurTranscription = null)
        } catch (e: Exception) {
            m.copy(erreurTranscription = e.message ?: e.javaClass.simpleName)
        }
        MessagesVocaux.remplacer(context, maj)
        return maj
    }

    fun titre(context: Context, m: MessageVocal): String = when {
        m.numero != null -> Decision.nomContact(context, m.numero) ?: Rules.affichage(m.numero)
        m.masque -> "Numéro masqué"
        else -> "Numéro inconnu"
    }

    private fun notifier(context: Context, m: MessageVocal) {
        if (!Notifs.autorisees(context)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CANAL, "Messages du répondeur", NotificationManager.IMPORTANCE_DEFAULT))
        val ouvrir = PendingIntent.getActivity(
            context, m.uid.toInt(), Intent(context, MessagesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Sans clé Groq, le corps de l'e-mail OVH porte leur propre transcription
        val texte = m.transcription?.takeIf { it.isNotBlank() } ?: m.corps.takeIf { it.isNotBlank() }
            ?: "Nouveau message vocal, touche pour l'écouter"
        val n = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_voicemail)
            .setContentTitle("${m.categorie.libelle} : ${titre(context, m)}")
            .setContentText(texte.take(200))
            .setStyle(NotificationCompat.BigTextStyle().bigText(texte.take(1500)))
            .setContentIntent(ouvrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(CANAL, m.uid.toInt(), n)
    }

    /** Appel renvoyé au répondeur : prévient tout de suite, puis relève la boîte sans attendre la tâche des 15 min */
    fun appelAuRepondeur(context: Context, brut: String) {
        relancer(context)
        if (!Notifs.autorisees(context)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CANAL_APPELS, "Appels envoyés au répondeur", NotificationManager.IMPORTANCE_HIGH))
        val id = (System.currentTimeMillis() / 1000).toInt()
        val drapeaux = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val ouvrir = PendingIntent.getActivity(
            context, id, Intent(context, MessagesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), drapeaux
        )
        val b = NotificationCompat.Builder(context, CANAL_APPELS)
            .setSmallIcon(R.drawable.ic_voicemail)
            .setContentTitle("Au répondeur : ${if (brut.isBlank()) "numéro masqué" else Rules.affichage(brut)}")
            .setContentText("Inconnu renvoyé vers ton répondeur. Son message s'affichera dès réception.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(ouvrir)
            .setAutoCancel(true)
        if (brut.isNotBlank()) {
            val rappeler = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(brut))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            b.addAction(0, "Rappeler", PendingIntent.getActivity(context, id + 1, rappeler, drapeaux))
        }
        NotificationManagerCompat.from(context).notify(CANAL_APPELS, id, b.build())
    }

    private fun relancer(context: Context) {
        if (!RepondeurConfig.lire(context).complet) return
        val wm = WorkManager.getInstance(context)
        val reseau = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        RELANCES_S.forEachIndexed { i, s ->
            wm.enqueueUniqueWork(
                "${TRAVAIL}_relance_$i", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<Travail>().setInitialDelay(s, TimeUnit.SECONDS).setConstraints(reseau).build()
            )
        }
    }

    fun planifier(context: Context) {
        val wm = WorkManager.getInstance(context)
        if (!RepondeurConfig.lire(context).complet) {
            wm.cancelUniqueWork(TRAVAIL)
            return
        }
        val requete = PeriodicWorkRequestBuilder<Travail>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(TRAVAIL, ExistingPeriodicWorkPolicy.KEEP, requete)
    }

    class Travail(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val res = executer(applicationContext)
            return if (res.erreur == null || res.erreur == "Répondeur non configuré") Result.success() else Result.retry()
        }
    }
}
