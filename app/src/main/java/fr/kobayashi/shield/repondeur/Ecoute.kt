/*
 * FICHIER : Ecoute.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/repondeur/Ecoute.kt
 * LIGNES : 145 | RÔLE : Écoute en continu de la boîte du répondeur (IMAP IDLE) : nouveau message vocal notifié
 *          en quelques secondes au lieu de la relève toutes les 15 min | DATE : 2026-10-31 | VERSION : 1.14.0
 * ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : BoiteMail.kt, Synchro.kt, Module.ECOUTE
 * LIEN : AndroidManifest.xml, DemarrageReceiver.kt | NOTES : service au premier plan (notification permanente imposée par
 *        Android). Ping NOOP toutes les 9 min, reconnexion avec attente croissante. La relève WorkManager reste en secours.
 */
package fr.kobayashi.shield.repondeur

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sun.mail.imap.IMAPFolder
import fr.kobayashi.shield.MessagesActivity
import fr.kobayashi.shield.R
import fr.kobayashi.shield.model.Module
import javax.mail.Folder
import javax.mail.event.MessageCountAdapter
import javax.mail.event.MessageCountEvent
import kotlin.concurrent.thread

class Ecoute : Service() {

    @Volatile private var actif = true
    @Volatile private var dossier: IMAPFolder? = null
    private var fil: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CANAL, "Répondeur à l'écoute", NotificationManager.IMPORTANCE_MIN))
        val ouvrir = PendingIntent.getActivity(
            this, 0, Intent(this, MessagesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_voicemail)
            .setContentTitle("Répondeur à l'écoute")
            .setContentText("Les messages vocaux arrivent en quelques secondes")
            .setOngoing(true)
            .setContentIntent(ouvrir)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID, n)
        }
        // Même processus que le filtrage d'appels : une erreur ici ne doit jamais faire tomber l'app
        fil = thread(name = "ecoute-repondeur") {
            runCatching { boucle() }.onFailure {
                Log.e(TAG, "Écoute arrêtée", it)
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        actif = false
        runCatching { dossier?.store?.close() }
        fil?.interrupt()
        super.onDestroy()
    }

    private fun boucle() {
        var attente = 5_000L
        while (actif) {
            val r = RepondeurConfig.lire(this)
            if (!r.complet || !Module.ECOUTE.actif(this)) {
                stopSelf()
                return
            }
            val store = BoiteMail.session(r, lectureMs = 12 * 60_000).getStore("imaps")
            try {
                store.connect(r.serveur, r.port, r.email, r.motDePasse)
                val inbox = store.getFolder("INBOX") as IMAPFolder
                inbox.open(Folder.READ_ONLY)
                dossier = inbox
                inbox.addMessageCountListener(object : MessageCountAdapter() {
                    override fun messagesAdded(e: MessageCountEvent) {
                        Log.d(TAG, "Écoute : ${e.messages.size} nouvel e-mail")
                        thread { Synchro.executer(applicationContext) }
                    }
                })
                // Rattrape ce qui est arrivé pendant une coupure
                Synchro.executer(applicationContext)
                attente = 5_000L
                Log.d(TAG, "Écoute : connecté, en attente de messages")
                val veille = thread(isDaemon = true) {
                    try {
                        while (actif && inbox.isOpen) {
                            Thread.sleep(9 * 60_000L)
                            inbox.doCommand { p -> p.simpleCommand("NOOP", null); null }
                        }
                    } catch (_: Exception) {
                    }
                }
                while (actif && inbox.isOpen) inbox.idle()
                veille.interrupt()
            } catch (e: Exception) {
                if (actif) Log.w(TAG, "Écoute interrompue : ${e.message}")
            } finally {
                dossier = null
                runCatching { store.close() }
            }
            if (!actif) return
            try {
                Thread.sleep(attente)
            } catch (_: InterruptedException) {
                return
            }
            attente = (attente * 2).coerceAtMost(5 * 60_000L)
        }
    }

    companion object {
        private const val TAG = "Shield"
        private const val CANAL = "repondeur_ecoute"
        private const val ID = 4242

        /** Lance ou arrête l'écoute selon la configuration ; sans effet si Android refuse le démarrage en arrière-plan */
        fun appliquer(context: Context) {
            val intent = Intent(context, Ecoute::class.java)
            if (RepondeurConfig.lire(context).complet && Module.ECOUTE.actif(context)) {
                runCatching { ContextCompat.startForegroundService(context, intent) }
                    .onFailure { Log.w(TAG, "Écoute non démarrée : ${it.message}") }
            } else {
                context.stopService(intent)
            }
        }
    }
}
