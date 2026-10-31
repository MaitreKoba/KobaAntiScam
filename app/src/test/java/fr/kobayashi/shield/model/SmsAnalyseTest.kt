/*
 * FICHIER : SmsAnalyseTest.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/test/java/fr/kobayashi/shield/model/SmsAnalyseTest.kt
 * LIGNES : 45 | RÔLE : Tests JVM de l'analyse des SMS : liens, désarmement, score de risque
 * DATE : 2026-10-31 | VERSION : 1.12.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : junit 4, SmsAnalyse.kt | LIEN : SmsAnalyse.kt | NOTES : scénarios nominal, erreur et limite
 */
package fr.kobayashi.shield.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsAnalyseTest {

    @Test
    fun arnaqueAuColisInterceptee() {
        val sms = "Colis en attente, frais de douane impayés : https://bit.ly/x1 urgent"
        assertEquals(listOf("https://bit.ly/x1"), SmsAnalyse.liens(sms))
        assertEquals("Colis en attente, frais de douane impayés : hxxps://bit[.]ly/x1 urgent", SmsAnalyse.desarmer(sms))
        assertTrue(SmsAnalyse.score(sms, expediteurConnu = false) >= SmsAnalyse.SEUIL)
    }

    @Test
    fun sansLienRienAIntercepter() {
        val sms = "Ton code de connexion est 123456, urgent, compte bloqué"
        assertTrue(SmsAnalyse.liens(sms).isEmpty())
        assertEquals(0, SmsAnalyse.score(sms, expediteurConnu = false))
        assertEquals(sms, SmsAnalyse.desarmer(sms))
    }

    @Test
    fun lienDunContactNeDeclenchePas() {
        val sms = "Regarde ça www.leboncoin.fr/annonce/123"
        assertFalse(SmsAnalyse.score(sms, expediteurConnu = true) >= SmsAnalyse.SEUIL)
        assertTrue(SmsAnalyse.score(sms, expediteurConnu = false) < SmsAnalyse.score("ameli.fr-remboursement.xyz/carte-vitale expire", false))
    }

    @Test
    fun domaineNuEtAdresseIp() {
        assertEquals(listOf("ameli-remboursement.xyz/carte"), SmsAnalyse.liens("Votre carte vitale expire : ameli-remboursement.xyz/carte."))
        assertTrue(SmsAnalyse.score("Paiement refusé http://185.23.11.4/login", false) >= 70)
        assertTrue(SmsAnalyse.liens("Version 1.11.0, appelle le 06.95.12.34.56").isEmpty())
    }
}
