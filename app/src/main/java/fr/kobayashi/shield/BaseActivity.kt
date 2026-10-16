/*
 * FICHIER : BaseActivity.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/BaseActivity.kt
 * LIGNES : 45 | RÔLE : Activité de base : applique l'apparence choisie (surcouches, taille du texte, barres système)
 *          et se recrée quand elle change | DATE : 2026-10-15 | VERSION : 1.6.0 | ORIGINE : Refonte UI
 * AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : Apparence.kt | LIEN : themes.xml | NOTES : toutes les activités en héritent
 */
package fr.kobayashi.shield

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

open class BaseActivity : AppCompatActivity() {
    private var revisionAppliquee = -1

    override fun attachBaseContext(newBase: Context) {
        val echelle = Apparence.echelleTexte(newBase)
        if (echelle == 1f) return super.attachBaseContext(newBase)
        val conf = Configuration(newBase.resources.configuration).apply { fontScale *= echelle }
        super.attachBaseContext(newBase.createConfigurationContext(conf))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Apparence.appliquer(this)
        revisionAppliquee = Apparence.revision(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        // Barre d'état transparente : le contenu ne passe pas dessous
        view?.let { racine ->
            val haut = racine.paddingTop
            val bas = racine.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(racine) { v, insets ->
                val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updatePadding(top = haut + b.top, bottom = bas + b.bottom)
                insets
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (revisionAppliquee != -1 && revisionAppliquee != Apparence.revision(this)) recreate()
    }
}
