/*
 * FICHIER : Transcription.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/repondeur/Transcription.kt
 * LIGNES : 50 | RÔLE : Transcription en texte d'un message vocal par l'API Whisper de Groq (offre gratuite)
 * DATE : 2026-10-16 | VERSION : 1.9.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE
 * DÉPENDANCES : okhttp | LIEN : Synchro.kt | NOTES : facultatif. Sans clé Groq, le message reste écoutable mais
 *        sans texte. La clé est saisie dans l'app (RepondeurConfig), jamais écrite dans le dépôt. L'audio part chez Groq.
 */
package fr.kobayashi.shield.repondeur

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object Transcription {
    private const val URL = "https://api.groq.com/openai/v1/audio/transcriptions"
    private const val MODELE = "whisper-large-v3-turbo"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    fun transcrire(audio: File, cle: String): String {
        val type = when (audio.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "ogg", "oga", "opus" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            else -> "audio/wav"
        }
        val corps = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", audio.name, audio.asRequestBody(type.toMediaType()))
            .addFormDataPart("model", MODELE)
            .addFormDataPart("language", "fr")
            .addFormDataPart("response_format", "text")
            .build()
        val requete = Request.Builder().url(URL).header("Authorization", "Bearer $cle").post(corps).build()
        client.newCall(requete).execute().use { r ->
            val texte = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("Groq ${r.code} : ${texte.take(160)}")
            return texte.trim()
        }
    }
}
