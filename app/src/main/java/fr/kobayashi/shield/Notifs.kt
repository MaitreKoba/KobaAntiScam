/*
 * FICHIER : Notifs.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/Notifs.kt
 * LIGNES : 89 | RÔLE : Notifications communes : autorisation réelle (réglage système + permission Android 13+),
 *          appels bloqués, SMS suspects avec lien désarmé et signalement 33700, notification de test
 * DATE : 2026-10-31 | VERSION : 1.13.0 | ORIGINE : Titre paramétrable (appels silencieux) | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : NotificationCompat, Rules.kt | LIEN : CallFilterService.kt, SmsNotificationListener.kt, Synchro.kt
 * NOTES : POST_NOTIFICATIONS n'existe qu'à partir d'Android 13 ; avant, seul le réglage système compte.
 */
package fr.kobayashi.shield

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import fr.kobayashi.shield.model.Rules

object Notifs {
    private const val CANAL_BLOQUES = "appels_bloques"
    private const val CANAL_SMS = "sms_suspects"
    private const val DRAPEAUX = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun autorisees(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun canal(context: Context, id: String, nom: String, importance: Int) =
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(id, nom, importance))

    private fun ouvrir(context: Context, cible: Class<*>, code: Int) =
        PendingIntent.getActivity(context, code, Intent(context, cible).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), DRAPEAUX)

    private fun id() = (System.nanoTime() and 0x7FFFFFFF).toInt()

    /** Appel rejeté (démarchage ARCEP, arnaque au rappel, rotation, règle) : discret, sans son */
    fun appelBloque(context: Context, brut: String, raison: String, titre: String = "Appel bloqué") {
        if (!autorisees(context)) return
        canal(context, CANAL_BLOQUES, "Appels filtrés", NotificationManager.IMPORTANCE_LOW)
        val n = id()
        val numero = if (brut.isBlank()) "Numéro masqué" else Rules.affichage(brut)
        NotificationManagerCompat.from(context).notify(CANAL_BLOQUES, n, NotificationCompat.Builder(context, CANAL_BLOQUES)
            .setSmallIcon(R.drawable.ic_remove_moderator)
            .setContentTitle("$titre : $numero")
            .setContentText(raison)
            .setContentIntent(ouvrir(context, LogsActivity::class.java, n))
            .setGroup(CANAL_BLOQUES)
            .setAutoCancel(true)
            .build())
    }

    /** SMS suspect : remplace la notification d'origine, lien désarmé, score et signalement au 33700 */
    fun smsSuspect(context: Context, expediteur: String, texteDesarme: String, score: Int, texteOriginal: String) {
        if (!autorisees(context)) return
        canal(context, CANAL_SMS, "SMS suspects", NotificationManager.IMPORTANCE_DEFAULT)
        val n = id()
        val signaler = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:33700")).putExtra("sms_body", texteOriginal)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        NotificationManagerCompat.from(context).notify(CANAL_SMS, n, NotificationCompat.Builder(context, CANAL_SMS)
            .setSmallIcon(R.drawable.ic_remove_moderator)
            .setContentTitle("SMS suspect ($score/100) : $expediteur")
            .setContentText(texteDesarme)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$texteDesarme\n\nLien désarmé : ne l'ouvre pas. Le SMS reste lisible dans Messages."))
            .setContentIntent(ouvrir(context, LogsActivity::class.java, n))
            .addAction(0, "Signaler au 33700", PendingIntent.getActivity(context, n + 1, signaler, DRAPEAUX))
            .setAutoCancel(true)
            .build())
    }

    /** Vérifie depuis Réglages que les notifications arrivent bien jusqu'à l'écran */
    fun test(context: Context): Boolean {
        if (!autorisees(context)) return false
        canal(context, CANAL_BLOQUES, "Appels filtrés", NotificationManager.IMPORTANCE_LOW)
        NotificationManagerCompat.from(context).notify(CANAL_BLOQUES, id(), NotificationCompat.Builder(context, CANAL_BLOQUES)
            .setSmallIcon(R.drawable.ic_verified_user)
            .setContentTitle("Test Kobayashi AntiScam")
            .setContentText("Les notifications fonctionnent.")
            .setAutoCancel(true)
            .build())
        return true
    }
}
