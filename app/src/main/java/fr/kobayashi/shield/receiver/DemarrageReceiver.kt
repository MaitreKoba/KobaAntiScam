/*
 * FICHIER : DemarrageReceiver.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/receiver/DemarrageReceiver.kt
 * LIGNES : 20 | RÔLE : Après redémarrage du téléphone ou mise à jour de l'app, relance la relève et l'écoute du répondeur
 * DATE : 2026-10-31 | VERSION : 1.14.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : Synchro.kt, Ecoute.kt | LIEN : AndroidManifest.xml | NOTES : aucune
 */
package fr.kobayashi.shield.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.kobayashi.shield.repondeur.Synchro

class DemarrageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Synchro.planifier(context)
        }
    }
}
