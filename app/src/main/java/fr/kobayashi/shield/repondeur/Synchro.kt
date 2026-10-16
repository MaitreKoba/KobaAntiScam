/*
 * FICHIER : Synchro.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/repondeur/Synchro.kt
 * LIGNES : 140 | RÔLE : Récupère les nouveaux messages vocaux (IMAP), les transcrit, les classe, les enregistre et
 *          notifie ; tâche périodique WorkManager toutes les 15 min quand le réseau est disponible
 * DATE : 2026-10-16 | VERSION : 1.9.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : BoiteMail.kt, Transcription.kt, Classement.kt, MessagesVocaux.kt, work-runtime-ktx
 * LIEN : MessagesActivity.kt, ShieldApp.kt | NOTES : le démarchage n'est pas notifié sauf réglage contraire
 */
package fr.kobayashi.shield.repondeur

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.kobayashi.shield.MessagesActivity
import fr.kobayashi.shield.R
import fr.kobayashi.shield.model.Decision
import fr.kobayashi.shield.model.Rules
import java.util.concurrent.TimeUnit

object Synchro {
    private const val TAG = "Shield"
    private const val CANAL = "repondeur"
    private const val TRAVAIL = "synchro_repondeur"

    data class Resultat(val nouveaux: Int, val erreur: String? = null)

    @Synchronized
    fun executer(context: Context): Resultat {
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
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CANAL, "Messages du répondeur", NotificationManager.IMPORTANCE_DEFAULT))
        val ouvrir = PendingIntent.getActivity(
            context, m.uid.toInt(), Intent(context, MessagesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val texte = m.transcription?.take(200) ?: "Nouveau message vocal, touche pour l'écouter"
        val n = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_voicemail)
            .setContentTitle("${m.categorie.libelle} : ${titre(context, m)}")
            .setContentText(texte)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texte))
            .setContentIntent(ouvrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(CANAL, m.uid.toInt(), n)
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
