/*
 * FICHIER : Modules.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/model/Modules.kt
 * LIGNES : 32 | RÔLE : Interrupteurs des modules (règles d'appel, SMS, notifications) affichés dans Réglages
 * DATE : 2026-10-31 | VERSION : 1.13.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : prefs "shield" | LIEN : Decision.kt, CallFilterService.kt, SmsNotificationListener.kt, SettingsActivity.kt
 * NOTES : tous actifs par défaut, comportement identique aux versions précédentes tant qu'on n'y touche pas.
 */
package fr.kobayashi.shield.model

import android.content.Context

enum class Module(val cle: String, val section: String, val titre: String, val description: String) {
    CONTACTS("mod_contacts", "Appels", "Contacts", "Tes contacts sonnent toujours. À couper pour tester avec ton autre téléphone."),
    DEJA_APPELE("mod_deja_appele", "Appels", "Numéros déjà appelés", "Un numéro que tu as déjà appelé sonne. À couper aussi pour tester."),
    RAPPEL("mod_rappel", "Appels", "Rappel sous 3 minutes", "Un inconnu qui rappelle aussitôt sonne (livreur, urgence)."),
    ARCEP("mod_arcep", "Appels", "Rejet du démarchage", "Préfixes ARCEP réservés au démarchage (0162, 0163, 0270, 0271, 0377, 0378, 0424, 0425, 0568, 0569, 0948, 0949)."),
    PAYS("mod_pays", "Appels", "Rejet de l'arnaque au rappel", "Indicatifs étrangers connus pour les appels en absence surtaxés."),
    ROTATION("mod_rotation", "Appels", "Anti-rotation de numéros", "Trop de numéros différents d'une même tranche en 24 h : rejet."),
    SMS("mod_sms", "SMS", "SMS anti-arnaque", "SMS avec lien d'un inconnu : alerte, lien désarmé, signalement 33700."),
    NOTIF_TENTATIVES("notif_tentatives", "Notifications", "Tentatives d'appel", "Alerte immédiate quand un appel ne sonne pas (répondeur ou sonnerie coupée)."),
    NOTIF_BLOQUES("notif_bloques", "Notifications", "Appels bloqués", "Alerte discrète à chaque appel rejeté, avec la raison."),
    NOTIF_MESSAGES("notif_messages", "Notifications", "Messages du répondeur", "Alerte avec le texte de chaque message vocal reçu.");

    fun actif(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(cle, true)

    fun regler(context: Context, actif: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(cle, actif).apply()

    private companion object {
        const val PREFS = "shield"
    }
}
