/*
 * FICHIER : Sauvegarde.kt | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/app/src/main/java/fr/kobayashi/shield/model/Sauvegarde.kt
 * LIGNES : 70 | RÔLE : Export et import des réglages en JSON : listes, modes, interrupteurs des modules, règles perso
 * DATE : 2026-10-31 | VERSION : 1.16.0 | ORIGINE : Création (l'import n'existait pas, l'export était inaccessible)
 * AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : ContactsHelper, Module, RuleDatabase, org.json
 * LIEN : SettingsActivity.kt | NOTES : n'exporte ni l'historique ni les messages ni les identifiants du répondeur.
 *        L'import fusionne : listes ajoutées, règles identiques (motif, action, type) ignorées.
 */
package fr.kobayashi.shield.model

import android.content.Context
import fr.kobayashi.shield.service.ContactsHelper
import org.json.JSONArray
import org.json.JSONObject

object Sauvegarde {
    private const val FORMAT = 1

    suspend fun versJson(context: Context): String {
        val prefs = context.getSharedPreferences("shield", Context.MODE_PRIVATE)
        val blanche = ContactsHelper.getAllowlist(context) + (prefs.getStringSet("allowlist", emptySet()) ?: emptySet())
        val modules = JSONObject().apply { Module.values().forEach { put(it.cle, it.actif(context)) } }
        val regles = JSONArray().apply {
            RuleDatabase.getDatabase().ruleDao().getAllRulesList().forEach { r ->
                put(JSONObject().put("motif", r.pattern).put("description", r.description ?: "")
                    .put("action", r.action.name).put("type", r.ruleType.name).put("active", r.isActive))
            }
        }
        return JSONObject()
            .put("application", "Kobayashi AntiScam")
            .put("format", FORMAT)
            .put("date", System.currentTimeMillis())
            .put("liste_blanche", JSONArray(blanche.sorted()))
            .put("liste_neutre", JSONArray(ContactsHelper.getNeutralList(context).sorted()))
            .put("mode_strict", ContactsHelper.isStrictModeEnabled(context))
            .put("inconnus_au_repondeur", ContactsHelper.isVoicemailEnabled(context))
            .put("modules", modules)
            .put("regles", regles)
            .toString(2)
    }

    /** Renvoie un résumé lisible ; lève une exception si le fichier n'est pas une sauvegarde de l'app */
    suspend fun depuisJson(context: Context, texte: String): String {
        val o = JSONObject(texte)
        require(o.optString("application") == "Kobayashi AntiScam") { "Ce fichier n'est pas une sauvegarde Kobayashi AntiScam" }
        val blanche = o.optJSONArray("liste_blanche") ?: JSONArray()
        for (i in 0 until blanche.length()) ContactsHelper.addToAllowlist(context, blanche.getString(i))
        val neutre = o.optJSONArray("liste_neutre") ?: JSONArray()
        for (i in 0 until neutre.length()) ContactsHelper.addToNeutralList(context, neutre.getString(i))
        if (o.has("mode_strict")) ContactsHelper.setStrictModeEnabled(context, o.getBoolean("mode_strict"))
        if (o.has("inconnus_au_repondeur")) ContactsHelper.setVoicemailEnabled(context, o.getBoolean("inconnus_au_repondeur"))
        o.optJSONObject("modules")?.let { m -> Module.values().filter { m.has(it.cle) }.forEach { it.regler(context, m.getBoolean(it.cle)) } }

        val dao = RuleDatabase.getDatabase().ruleDao()
        val existantes = dao.getAllRulesList().map { Triple(it.pattern, it.action, it.ruleType) }.toSet()
        var ajoutees = 0
        val regles = o.optJSONArray("regles") ?: JSONArray()
        for (i in 0 until regles.length()) {
            val r = regles.getJSONObject(i)
            val action = runCatching { RuleAction.valueOf(r.getString("action")) }.getOrNull() ?: continue
            val type = runCatching { RuleType.valueOf(r.getString("type")) }.getOrNull() ?: continue
            val motif = r.getString("motif")
            if (Triple(motif, action, type) in existantes) continue
            dao.insert(Rule(pattern = motif, description = r.optString("description").ifBlank { null },
                action = action, ruleType = type, isActive = r.optBoolean("active", true)))
            ajoutees++
        }
        return "${blanche.length()} numéro(s) en liste blanche, ${neutre.length()} en liste neutre, $ajoutees règle(s) ajoutée(s), modes et modules restaurés"
    }
}
