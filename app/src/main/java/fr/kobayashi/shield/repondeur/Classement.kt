/*
 * FICHIER : Classement.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/repondeur/Classement.kt
 * LIGNES : 110 | RÔLE : Fonctions pures du répondeur : catégorie d'un message d'après les mots prononcés,
 *          extraction du numéro de l'appelant dans l'e-mail d'OVH | DATE : 2026-10-16 | VERSION : 1.9.0
 * ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : Rules.kt
 * LIEN : ClassementTest.kt, Synchro.kt
 * NOTES : sans dépendance Android pour rester testable en JVM. Le format exact de l'e-mail OVH n'est pas connu
 *         avant le premier message réel : l'extraction cherche tout numéro plausible et ignore celui de la ligne OVH.
 */
package fr.kobayashi.shield.repondeur

import fr.kobayashi.shield.model.Rules
import java.text.Normalizer

enum class Categorie(val libelle: String) {
    LIVRAISON("Livraison"),
    TRAVAIL("Travail"),
    DEMARCHES("Rendez-vous et démarches"),
    DEMARCHAGE("Démarchage"),
    AUTRE("Autre"),
    SANS_TEXTE("Sans transcription"),
}

object Classement {

    private val MOTS = mapOf(
        Categorie.LIVRAISON to listOf(
            "livr", "colis", "commande", "uber", "deliveroo", "just eat", "amazon", "chronopost", "colissimo",
            "dpd", "ups", "fedex", "gls", "point relais", "relais", "facteur", "la poste", "interphone", "digicode",
            "portail", "en bas de chez", "devant chez", "boite aux lettres", "restaurant", "repas",
        ),
        Categorie.TRAVAIL to listOf(
            "travail", "boulot", "chantier", "mission", "planning", "patron", "collegue", "reunion", "client",
            "horaire", "entreprise", "agence", "interim", "contrat", "embauche", "entretien", "equipe", "manager",
        ),
        Categorie.DEMARCHES to listOf(
            "rendez-vous", "rendez vous", "rdv", "medecin", "docteur", "cabinet", "pharmacie", "hopital", "clinique",
            "caf", "impots", "prefecture", "mairie", "banque", "assurance", "dossier", "ameli", "secu",
            "france travail", "pole emploi", "avocat", "notaire", "ecole", "garage", "proprietaire", "loyer",
        ),
        Categorie.DEMARCHAGE to listOf(
            "offre", "promotion", "gratuit", "economie", "panneaux solaires", "panneau solaire", "isolation",
            "pompe a chaleur", "renovation energetique", "cpf", "compte personnel de formation", "sondage", "enquete",
            "credit", "vous avez ete selectionne", "felicitations", "frais de douane", "carte vitale",
            "compte bloque", "conseiller", "mutuelle sante", "prime", "eligible", "sans engagement",
        ),
    )

    private val MASQUE = Regex("anonyme|masque|private|unknown|inconnu", RegexOption.IGNORE_CASE)
    private val NUMERO_FR = Regex("""(?<!\d)(?:\+33\s?|0033\s?|0)[1-9](?:[\s.\-]?\d{2}){4}(?!\d)""")
    private val NUMERO_INTER = Regex("""(?<![\d+])\+(?!33)\d[\d\s.\-]{7,16}\d""")

    fun sansAccents(texte: String): String =
        Normalizer.normalize(texte.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    /** Catégorie la plus citée ; à égalité, le démarchage perd face à une vraie raison d'appeler */
    fun categorie(transcription: String?): Categorie {
        if (transcription.isNullOrBlank()) return Categorie.SANS_TEXTE
        val t = sansAccents(transcription)
        val scores = MOTS.mapValues { (_, mots) -> mots.count { t.contains(it) } }
        val max = scores.values.maxOrNull() ?: 0
        if (max == 0) return Categorie.AUTRE
        val gagnants = scores.filterValues { it == max }.keys
        return listOf(Categorie.LIVRAISON, Categorie.TRAVAIL, Categorie.DEMARCHES, Categorie.DEMARCHAGE)
            .first { it in gagnants }
    }

    /** Numéro de l'appelant au format +33..., ou null si masqué ou introuvable. `exclus` : numéros à ignorer (ligne OVH, 06 95) */
    fun numeroAppelant(textes: List<String>, exclus: Collection<String> = emptyList()): String? {
        val ignores = exclus.filter { it.isNotBlank() }.map { Rules.normalize(it) }.toSet()
        for (texte in textes) {
            val candidats = (NUMERO_INTER.findAll(texte) + NUMERO_FR.findAll(texte))
                .sortedBy { it.range.first }
                .map { Rules.normalize(it.value) }
                .filter { it.length >= 11 && it !in ignores }
            candidats.firstOrNull()?.let { return it }
        }
        return null
    }

    fun estMasque(textes: List<String>): Boolean = textes.any { MASQUE.containsMatchIn(sansAccents(it)) }
}
