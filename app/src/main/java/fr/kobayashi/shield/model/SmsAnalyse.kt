/*
 * FICHIER : SmsAnalyse.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/model/SmsAnalyse.kt
 * LIGNES : 47 | RÔLE : Analyse d'un SMS : liens présents, lien désarmé (hxxps, [.]), score de risque sur 100
 * DATE : 2026-10-31 | VERSION : 1.12.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : Classement.sansAccents | LIEN : SmsNotificationListener.kt, SmsAnalyseTest.kt
 * NOTES : fonctions pures, testables en JVM. Sans lien, le score est 0 : seuls les SMS avec lien sont interceptés.
 */
package fr.kobayashi.shield.model

import fr.kobayashi.shield.repondeur.Classement

object SmsAnalyse {
    const val SEUIL = 50

    private val LIEN = Regex(
        """(?i)\b(?:https?://[^\s<>"]+|www\.[^\s<>"]+|[a-z0-9-]+(?:\.[a-z0-9-]+)*\.(?:com|fr|net|org|info|io|ly|link|xyz|top|site|online|shop|app|me|co|cc|ru|cn|tk|gd|gy|vip|live|icu)(?:/[^\s<>"]*)?)"""
    )
    private val RACCOURCIS = listOf(
        "bit.ly", "tinyurl", "t.co/", "goo.gl", "cutt.ly", "is.gd", "rb.gy", "shorturl", "tiny.cc", "ow.ly", "s.id/", "urlz.fr", "t.ly",
    )
    private val ADRESSE_IP = Regex("""\d{1,3}(?:\.\d{1,3}){3}""")
    private val MOTS = listOf(
        "colis", "livraison", "douane", "frais", "compte", "bloque", "suspendu", "urgent", "amende", "antai", "cpf",
        "carte vitale", "ameli", "rembourse", "mot de passe", "identifiant", "confirmer", "verifier", "mise a jour",
        "paiement", "facture", "impaye", "gagne", "felicitations", "cadeau", "cliquez", "chronopost", "colissimo",
        "dhl", "la poste", "banque", "expire",
    )

    fun liens(texte: String): List<String> = LIEN.findAll(texte).map { it.value.trimEnd('.', ',', ')', '!', '?', ';') }.toList()

    /** Rend chaque lien non cliquable : https://bit.ly/x devient hxxps://bit[.]ly/x */
    fun desarmer(texte: String): String =
        LIEN.replace(texte) { it.value.replace(Regex("(?i)http"), "hxxp").replace(".", "[.]") }

    fun score(texte: String, expediteurConnu: Boolean): Int {
        val l = liens(texte)
        if (l.isEmpty()) return 0
        var s = 40
        if (l.any { u -> RACCOURCIS.any { u.contains(it, ignoreCase = true) } }) s += 25
        if (l.any { it.startsWith("http://", ignoreCase = true) }) s += 10
        if (l.any { ADRESSE_IP.containsMatchIn(it) }) s += 20
        val t = Classement.sansAccents(texte)
        s += (MOTS.count { t.contains(it) } * 8).coerceAtMost(30)
        if (expediteurConnu) s -= 50
        return s.coerceIn(0, 100)
    }
}
