/*
 * FICHIER : ClassementTest.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/test/java/fr/kobayashi/shield/repondeur/ClassementTest.kt
 * LIGNES : 104 | RÔLE : Tests JVM du classement des messages vocaux et de l'extraction du numéro de l'appelant
 * DATE : 2026-10-30 | VERSION : 1.11.0 | ORIGINE : Ajout des tests du format OVH réel | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : junit 4, Classement.kt | LIEN : Classement.kt | NOTES : scénarios nominal, erreur et limite
 */
package fr.kobayashi.shield.repondeur

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassementTest {

    @Test
    fun livreurUberEats() {
        assertEquals(Categorie.LIVRAISON, Classement.categorie("Bonjour, c'est le livreur Uber Eats, votre commande est prête, je suis en bas de chez vous."))
    }

    @Test
    fun travailEtDemarches() {
        assertEquals(Categorie.TRAVAIL, Classement.categorie("Salut, c'est ton patron, rappelle-moi pour le planning du chantier demain."))
        assertEquals(Categorie.DEMARCHES, Classement.categorie("Le cabinet du docteur Martin pour confirmer votre rendez-vous de jeudi."))
    }

    @Test
    fun demarchageReconnuMaisPerdAEgalite() {
        assertEquals(Categorie.DEMARCHAGE, Classement.categorie("Félicitations, vous êtes éligible à une prime pour l'isolation de vos combles, offre sans engagement."))
        // Une vraie livraison qui cite « gratuit » reste une livraison
        assertEquals(Categorie.LIVRAISON, Classement.categorie("Votre colis est arrivé, livraison gratuite"))
    }

    @Test
    fun texteVideOuSansMotCle() {
        assertEquals(Categorie.SANS_TEXTE, Classement.categorie(null))
        assertEquals(Categorie.SANS_TEXTE, Classement.categorie("   "))
        assertEquals(Categorie.AUTRE, Classement.categorie("Coucou, rappelle-moi quand tu peux."))
    }

    @Test
    fun accentsEtMajuscules() {
        assertEquals(Categorie.DEMARCHES, Classement.categorie("RENDEZ-VOUS à la PRÉFECTURE"))
    }

    @Test
    fun numeroDansLeSujetIgnoreLaLigneOvh() {
        val sujet = "Nouveau message vocal de 06 12 34 56 78 pour la ligne 0912345678"
        assertEquals("+33612345678", Classement.numeroAppelant(listOf(sujet), listOf("0912345678")))
    }

    @Test
    fun numeroFormatsVaries() {
        assertEquals("+33612345678", Classement.numeroAppelant(listOf("Appelant : +33612345678")))
        assertEquals("+33612345678", Classement.numeroAppelant(listOf("Appelant : 0033 612345678")))
        assertEquals("+33148001122", Classement.numeroAppelant(listOf("de 01.48.00.11.22")))
        assertEquals("+2250707070707", Classement.numeroAppelant(listOf("Appel de +225 07 07 07 07 07")))
    }

    @Test
    fun numeroAbsentOuMasque() {
        val textes = listOf("Nouveau message vocal", "Appelant : anonyme. Durée 12 s, reçu le 16/10/2026 à 13:05")
        assertNull(Classement.numeroAppelant(textes, listOf("0912345678")))
        assertTrue(Classement.estMasque(textes))
    }

    private val corpsOvh = """
        Bonjour,
        Vous avez reçu un message vocal provenant du numéro 33682657801 vers votre numéro 0033972120256.
        Vous trouverez, ci-joint, le message de 5 secondes que votre correspondant vous a laissé.
        Voici la transcription de ce dernier :
        00:01.910 -> 00:03.330
        Bonjour, c'est le livreur Uber Eats, je suis en bas de chez vous.
        Attention : Cette transcription a été générée automatiquement par une intelligence artificielle.
        Service messagerie
    """.trimIndent()

    @Test
    fun emailOvhReelNumeroEtTranscription() {
        assertEquals("+33682657801", Classement.numeroAppelant(listOf("Message vocal du 33682657801", corpsOvh)))
        val t = Classement.transcriptionOvh(corpsOvh)
        assertEquals("Bonjour, c'est le livreur Uber Eats, je suis en bas de chez vous.", t)
        assertEquals(Categorie.LIVRAISON, Classement.categorie(t))
    }

    @Test
    fun emailOvhSansAppelantNeRetientPasLaLigne() {
        val corps = "Vous avez reçu un message vocal provenant du numéro anonyme vers votre numéro 0033972120256."
        assertNull(Classement.numeroAppelant(listOf("Message vocal", corps)))
    }

    @Test
    fun emailOvhSansTranscription() {
        val corps = "Vous avez reçu un message vocal provenant du numéro 33695626373 vers votre numéro 0033972120256.\n" +
            "Vous trouverez, sur votre boite vocale, le message de 4 secondes.\nService messagerie"
        assertNull(Classement.transcriptionOvh(corps))
        assertEquals("+33695626373", Classement.numeroAppelant(listOf(corps)))
    }

    @Test
    fun dateEtDureeNeSontPasDesNumeros() {
        assertNull(Classement.numeroAppelant(listOf("Reçu le 16/10/2026 à 13:05, durée 00:00:12, référence 20261016130500")))
    }
}
