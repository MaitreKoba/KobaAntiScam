/*
 * FICHIER : RulesTest.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/test/java/fr/kobayashi/shield/model/RulesTest.kt
 * LIGNES : 140 | RÔLE : Tests unitaires de Rules et Decision | DATE : 2026-10-16
 * VERSION : 1.7.0 | ORIGINE : Création 1.0.0, réparé en 1.7.0 (ne compilait plus) | AUTEUR : Maitre Kobayashi
 * STATUT : STABLE | DÉPENDANCES : org.junit, Rules, Decision | LIEN : Rules.kt
 * NOTES : normalize, normalizePrefix, affichage, ARCEP, fenêtre de rappel, tranche anti-rotation
 */
package fr.kobayashi.shield.model

import org.junit.Test
import org.junit.Assert.*

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
            assertTrue("Préfixe $prefix devrait être ARCEP", Rules.isArcpPrefix("+33${prefix}123456"))
            assertTrue("Préfixe $prefix devrait être ARCEP", Rules.isArcpPrefix("0${prefix}123456"))
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
    fun testNormalizeIndicatif00() {
        assertEquals("+22507123456", Rules.normalize("00225 07 12 34 56"))
        assertEquals("+33695123456", Rules.normalize("0033 6 95 12 34 56"))
    }

    @Test
    fun testAffichage() {
        assertEquals("06 95 12 34 56", Rules.affichage("+33695123456"))
        assertEquals("09 48 12 34 56", Rules.affichage("0948123456"))
        assertEquals("+22507123456", Rules.affichage("+22507123456"))
    }

    @Test
    fun testTranche() {
        assertEquals("06951234", Decision.tranche("+33695123456"))
        assertEquals(Decision.tranche("+33695123456"), Decision.tranche("0695123499"))
        assertNotEquals(Decision.tranche("+33695123456"), Decision.tranche("+33695124456"))
    }
}
