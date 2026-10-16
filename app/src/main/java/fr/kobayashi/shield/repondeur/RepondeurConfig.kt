/*
 * FICHIER : RepondeurConfig.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/repondeur/RepondeurConfig.kt
 * LIGNES : 75 | RÔLE : Réglages du répondeur OVH : boîte mail IMAP, numéro de la ligne OVH, clé Groq facultative
 * DATE : 2026-10-16 | VERSION : 1.9.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : androidx.security:security-crypto | LIEN : RepondeurActivity.kt, Synchro.kt
 * NOTES : mot de passe et clé stockés chiffrés sur le téléphone (EncryptedSharedPreferences), jamais dans le dépôt.
 *         Si le Keystore du téléphone refuse le chiffrement, repli sur des prefs privées de l'app.
 */
package fr.kobayashi.shield.repondeur

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object RepondeurConfig {
    const val SERVEUR_DEFAUT = "ssl0.ovh.net"
    const val PORT_DEFAUT = 993

    data class Reglages(
        val email: String,
        val motDePasse: String,
        val serveur: String,
        val port: Int,
        val numeroOvh: String,
        val cleGroq: String,
        val transcription: Boolean,
        val notifierDemarchage: Boolean,
    ) {
        val complet: Boolean get() = email.isNotBlank() && motDePasse.isNotBlank() && serveur.isNotBlank()
    }

    private fun prefs(context: Context): SharedPreferences = try {
        val cle = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "repondeur_secret", cle,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w("Shield", "Chiffrement indisponible, prefs privées: ${e.message}")
        context.getSharedPreferences("repondeur_clair", Context.MODE_PRIVATE)
    }

    fun lire(context: Context): Reglages {
        val p = prefs(context)
        return Reglages(
            email = p.getString("email", "") ?: "",
            motDePasse = p.getString("mot_de_passe", "") ?: "",
            serveur = p.getString("serveur", SERVEUR_DEFAUT) ?: SERVEUR_DEFAUT,
            port = p.getInt("port", PORT_DEFAUT),
            numeroOvh = p.getString("numero_ovh", "") ?: "",
            cleGroq = p.getString("cle_groq", "") ?: "",
            transcription = p.getBoolean("transcription", true),
            notifierDemarchage = p.getBoolean("notifier_demarchage", false),
        )
    }

    fun ecrire(context: Context, r: Reglages) {
        prefs(context).edit()
            .putString("email", r.email.trim())
            .putString("mot_de_passe", r.motDePasse)
            .putString("serveur", r.serveur.trim().ifBlank { SERVEUR_DEFAUT })
            .putInt("port", r.port)
            .putString("numero_ovh", r.numeroOvh.trim())
            .putString("cle_groq", r.cleGroq.trim())
            .putBoolean("transcription", r.transcription)
            .putBoolean("notifier_demarchage", r.notifierDemarchage)
            .apply()
    }
}
