/*
 * FICHIER : AppearanceActivity.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/AppearanceActivity.kt
 * LIGNES : 110 | RÔLE : Personnalisation de l'interface : mode, couleur, coins, cartes, fond, police, taille du texte,
 *          animations, disposition de l'accueil, état des services | DATE : 2026-10-15 | VERSION : 1.6.0 | ORIGINE : Création
 * AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : Apparence.kt, activity_appearance.xml | LIEN : themes.xml
 * NOTES : chaque choix est enregistré puis l'écran se recrée pour l'aperçu
 */
package fr.kobayashi.shield

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.kobayashi.shield.databinding.ActivityAppearanceBinding

class AppearanceActivity : BaseActivity() {

    private lateinit var binding: ActivityAppearanceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppearanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetour.setOnClickListener { finish() }
        binding.btnReinitialiser.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.apparence_reinitialiser)
                .setMessage(R.string.apparence_reinitialiser_q)
                .setPositiveButton(R.string.oui) { _, _ -> Apparence.reinitialiser(this); recreate() }
                .setNegativeButton(R.string.annuler, null)
                .show()
        }

        section("Mode", "Clair, sombre ou comme le téléphone", "mode", Apparence.MODES)
        section("Couleur principale", "Boutons, carte d'état, interrupteurs", "accent", Apparence.ACCENTS)
        section("Taille du texte", "S'ajoute au réglage du téléphone", "taille", Apparence.TAILLES)
        section("Coins", "Arrondi des cartes et des boutons", "coins", Apparence.COINS)
        section("Cartes", "Verre dépoli comme sur kobacraft.net, pleines ou teintées", "cartes", Apparence.CARTES)
        section("Fond", "Dégradé crème et pêche, ou couleur unie", "fond", Apparence.FONDS)
        section("Police", "Fredoka et Nunito (kobacraft.net) ou police du téléphone", "police", Apparence.POLICES)
        section("Animations", "Transitions entre les écrans", "animations", Apparence.ANIMATIONS)
        section("Accueil", "Raccourcis en grille ou en liste", "accueil", Apparence.ACCUEIL)
        section("État des services", "Afficher la carte sur l'accueil", "services",
            listOf(Apparence.Option("oui", "Afficher"), Apparence.Option("non", "Masquer")))
    }

    private fun section(titre: String, aide: String, cle: String, options: List<Apparence.Option>) {
        val dp = resources.displayMetrics.density
        val actuel = Apparence.lire(this, cle)
        val groupe = ChipGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = (8 * dp).toInt() }
        }
        options.forEach { o ->
            groupe.addView(Chip(this).apply {
                text = o.libelle
                isCheckable = true
                isChecked = o.cle == actuel
                if (o.couleur != 0) {
                    chipIconTint = ColorStateList.valueOf(o.couleur)
                    chipIcon = getDrawable(R.drawable.pastille)
                    isChipIconVisible = true
                }
                setOnClickListener {
                    if (Apparence.lire(context, cle) == o.cle) return@setOnClickListener
                    Apparence.ecrire(context, cle, o.cle)
                    if (cle != "mode") recreate()
                }
            })
        }
        val carte = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = (12 * dp).toInt() }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
                addView(TextView(context).apply {
                    text = titre
                    setTextAppearance(R.style.Texte_Shield_SousTitre)
                    textSize = 17f
                })
                addView(TextView(context).apply {
                    text = aide
                    textSize = 12.5f
                    setTextColor(getColor(R.color.ink3))
                })
                addView(groupe)
            })
        }
        binding.sections.addView(carte)
    }
}
