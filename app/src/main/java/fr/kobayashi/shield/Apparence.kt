/*
 * FICHIER : Apparence.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/Apparence.kt
 * LIGNES : 110 | RÔLE : Réglages d'apparence (mode, accent, coins, cartes, fond, police, taille du texte, animations,
 *          disposition de l'accueil) et application des surcouches de thème | DATE : 2026-10-15 | VERSION : 1.6.0
 * ORIGINE : Refonte UI | AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : themes.xml, BaseActivity.kt
 * LIEN : AppearanceActivity.kt | NOTES : stockage local (SharedPreferences "apparence"), aucune donnée transmise
 */
package fr.kobayashi.shield

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object Apparence {
    private const val PREFS = "apparence"

    data class Option(val cle: String, val libelle: String, val style: Int = 0, val couleur: Int = 0)

    val MODES = listOf(Option("systeme", "Système"), Option("clair", "Clair"), Option("sombre", "Sombre"))
    val ACCENTS = listOf(
        Option("orange", "Kobacraft", R.style.Accent_Orange, 0xFFFF7E00.toInt()),
        Option("rouge", "Rouge", R.style.Accent_Rouge, 0xFFE8433A.toInt()),
        Option("ambre", "Ambre", R.style.Accent_Ambre, 0xFFF6AD37.toInt()),
        Option("dragon", "Dragon", R.style.Accent_Dragon, 0xFF3FAE6A.toInt()),
        Option("ocean", "Océan", R.style.Accent_Ocean, 0xFF2F80ED.toInt()),
        Option("lilas", "Lilas", R.style.Accent_Lilas, 0xFF8E5CF7.toInt()),
        Option("sakura", "Sakura", R.style.Accent_Sakura, 0xFFEC5D9C.toInt()),
    )
    val COINS = listOf(
        Option("carre", "Carrés", R.style.Coins_Carre), Option("doux", "Doux", R.style.Coins_Doux),
        Option("arrondi", "Arrondis", R.style.Coins_Arrondi), Option("bulle", "Bulles", R.style.Coins_Bulle),
    )
    val CARTES = listOf(
        Option("verre", "Verre dépoli", R.style.Cartes_Verre), Option("pleines", "Pleines", R.style.Cartes_Pleines),
        Option("teintees", "Teintées", R.style.Cartes_Teintees),
    )
    val FONDS = listOf(Option("degrade", "Dégradé", R.style.Fond_Degrade), Option("uni", "Uni", R.style.Fond_Uni))
    val POLICES = listOf(Option("kobacraft", "Kobacraft", R.style.Police_Kobacraft), Option("systeme", "Système", R.style.Police_Systeme))
    val ANIMATIONS = listOf(Option("normales", "Normales", R.style.Animations_Normales), Option("aucune", "Aucune", R.style.Animations_Aucune))
    val TAILLES = listOf(Option("90", "Petite"), Option("100", "Normale"), Option("115", "Grande"), Option("130", "Très grande"))
    val ACCUEIL = listOf(Option("grille", "Grille"), Option("liste", "Liste"))

    private val DEFAUTS = mapOf(
        "mode" to "systeme", "accent" to "orange", "coins" to "arrondi", "cartes" to "verre", "fond" to "degrade",
        "police" to "kobacraft", "animations" to "normales", "taille" to "100", "accueil" to "grille", "services" to "oui",
    )

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun lire(c: Context, cle: String): String = prefs(c).getString(cle, DEFAUTS[cle]) ?: DEFAUTS[cle].orEmpty()
    fun revision(c: Context): Int = prefs(c).getInt("revision", 0)

    fun ecrire(c: Context, cle: String, valeur: String) {
        val p = prefs(c)
        p.edit().putString(cle, valeur).putInt("revision", p.getInt("revision", 0) + 1).apply()
        if (cle == "mode") appliquerModeNuit(c)
    }

    fun reinitialiser(c: Context) {
        val rev = revision(c) + 1
        prefs(c).edit().clear().putInt("revision", rev).apply()
        appliquerModeNuit(c)
    }

    fun appliquerModeNuit(c: Context) {
        AppCompatDelegate.setDefaultNightMode(
            when (lire(c, "mode")) {
                "clair" -> AppCompatDelegate.MODE_NIGHT_NO
                "sombre" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun echelleTexte(c: Context): Float = (lire(c, "taille").toIntOrNull() ?: 100) / 100f

    /** Surcouches de thème, à appeler avant setContentView */
    fun appliquer(a: Activity) {
        fun style(liste: List<Option>, cle: String) = (liste.firstOrNull { it.cle == lire(a, cle) } ?: liste.first()).style
        listOf(
            style(ACCENTS, "accent"), style(COINS, "coins"), style(CARTES, "cartes"), style(FONDS, "fond"),
            style(POLICES, "police"), style(ANIMATIONS, "animations"),
        ).forEach { a.theme.applyStyle(it, true) }
    }
}
