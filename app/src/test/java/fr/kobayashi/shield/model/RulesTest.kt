package fr.kobayashi.shield.model

import org.junit.Test
import org.junit.Assert.*

/**
 * FICHIER : RulesTest.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/test/java/fr/kobayashi/shield/model/RulesTest.kt
 * LIGNES : ~100 | RÔLE : Tests unitaires pour Rules | DATE : 2026-10-14
 * VERSION : 1.0.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi (via agent)
 * STATUT : STABLE | DÉPENDANCES : org.junit, Rules | LIEN : RulesTest.kt
 * NOTES : Tests de normalize, normalizePrefix, ARCEP, fenêtre de rappel
 */

class RulesTest {
    
    @Test
    fun testNormalizeFrenchNumber() {
        // Test avec numéro français avec indicatif
        assertEquals("+33123456789", Rules.normalize("+33123456789"))
        
        // Test avec numéro français sans +
        assertEquals("+33123456789", Rules.normalize("33123456789"))
        
        // Test avec numéro français commençant par 0
        assertEquals("+33123456789", Rules.normalize("0123456789"))
        
        // Test avec espaces
        assertEquals("+33123456789", Rules.normalize("01 23 45 67 89"))
        
        // Test avec points
        assertEquals("+33123456789", Rules.normalize("01.23.45.67.89"))
        
        // Test avec tirets
        assertEquals("+33123456789", Rules.normalize("01-23-45-67-89"))
        
        // Test avec parenthèses
        assertEquals("+33123456789", Rules.normalize("(+33) 1.23.45.67.89"))
    }
    
    @Test
    fun testNormalizeInternationalNumber() {
        // Test avec numéro international
        assertEquals("+12125551234", Rules.normalize("+1 212-555-1234"))
        assertEquals("+442071234567", Rules.normalize("+44-20-7123-4567"))
    }
    
    @Test
    fun testNormalizeEmpty() {
        assertEquals("", Rules.normalize(""))
        assertEquals("", Rules.normalize("   "))
    }
    
    @Test
    fun testNormalizePrefix() {
        // Test avec préfixe français
        assertEquals("+33162", Rules.normalizePrefix("162"))
        assertEquals("+33163", Rules.normalizePrefix("163"))
        
        // Test avec préfixe déjà normalisé
        assertEquals("+33162", Rules.normalizePrefix("+33162"))
        
        // Test avec préfixe avec 0
        assertEquals("+336", Rules.normalizePrefix("06"))
    }
    
    @Test
    fun testIsArcpPrefix() {
        // Préfixes ARCEP à rejeter
        val arcpPrefixes = listOf("162", "163", "270", "271", "377", "378", "424", "425", "568", "569", "948", "949")
        
        for (prefix in arcpPrefixes) {
            assertTrue("Préfixe $prefix devrait être ARCEP", Rules.isArcpPrefix("+33$prefix123456"))
            assertTrue("Préfixe $prefix devrait être ARCEP", Rules.isArcpPrefix("0$prefix123456"))
        }
        
        // Numéros non ARCEP
        assertFalse("0612345678 ne devrait pas être ARCEP", Rules.isArcpPrefix("+33612345678"))
        assertFalse("0712345678 ne devrait pas être ARCEP", Rules.isArcpPrefix("0712345678"))
        assertFalse("0123456789 ne devrait pas être ARCEP", Rules.isArcpPrefix("0123456789"))
    }
    
    @Test
    fun testIsRecallWithinWindow() {
        // Simuler un appel
        val number = "+33123456789"
        val timestamps = mutableMapOf<String, Long>()
        
        // Premier appel
        val firstCallTime = System.currentTimeMillis() - 60000 // 1 minute ago
        timestamps[number] = firstCallTime
        
        // Vérifier que le rappel sous 3 minutes est vrai
        assertTrue("Rappel sous 3 min devrait être vrai", 
            Rules.isRecallWithinWindow(number, timestamps))
        
        // Attendre plus de 3 minutes
        timestamps[number] = System.currentTimeMillis() - 190000 // 3 minutes et 10 secondes ago
        assertFalse("Rappel après 3 min devrait être faux", 
            Rules.isRecallWithinWindow(number, timestamps))
    }
    
    @Test
    fun testSlice() {
        // Test extraction de tranche de 8 chiffres
        assertEquals("12345678", Rules.getSlice("+33123456789"))
        assertEquals("12345678", Rules.getSlice("0123456789"))
        
        // Numéro avec moins de 8 chiffres
        assertEquals("12345", Rules.getSlice("012345"))
    }
    
    @Test
    fun testSliceCounter() {
        // Réinitialiser les compteurs pour le test
        // Note: En réalité, on ne peut pas accéder aux membres privés
        // mais on peut tester la logique via les méthodes publiques
        
        // Ce test vérifie que la logique de tranche fonctionne
        val slice1 = Rules.getSlice("+33123456789")
        val slice2 = Rules.getSlice("+33234567891")
        
        assertNotEquals("Les tranches devraient être différentes", slice1, slice2)
        
        // Même tranche pour des numéros qui partagent les 8 derniers chiffres
        val slice3 = Rules.getSlice("+33112345678")
        assertEquals("Les tranches devraient être les mêmes", slice1, slice3)
    }
}
