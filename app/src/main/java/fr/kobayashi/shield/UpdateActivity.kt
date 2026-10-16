/*
 * FICHIER : UpdateActivity.kt | CHEMIN : D:/Kobayashi/Developpement/shield/app/src/main/java/fr/kobayashi/shield/UpdateActivity.kt
 * LIGNES : 260 | RÔLE : Mise à jour depuis la dernière release GitHub (vérification, téléchargement, contrôle de l'APK, installation)
 *          | DATE : 2026-10-16 | VERSION : 1.6.1 | ORIGINE : Correctif mise à jour
 * AUTEUR : Maitre Kobayashi | STATUT : STABLE | DÉPENDANCES : GitHubService.kt, file_paths.xml, REQUEST_INSTALL_PACKAGES
 * LIEN : activity_update.xml | NOTES : APK téléchargé dans le dossier privé de l'app, contrôlé (paquet, versionCode) avant installation
 */
package fr.kobayashi.shield

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import fr.kobayashi.shield.databinding.ActivityUpdateBinding
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.RuleDatabase
import fr.kobayashi.shield.network.GitHubService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class UpdateActivity : BaseActivity() {

    private lateinit var binding: ActivityUpdateBinding
    private lateinit var downloadManager: DownloadManager
    private var downloadId: Long = -1L
    private var latestRelease: GitHubService.Release? = null
    private var apkEnAttente: File? = null
    private val tag = "UpdateActivity"

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id == downloadId) {
                onDownloadTermine(id)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager

        binding.btnInstall.setOnClickListener {
            downloadUpdate()
        }

        ContextCompat.registerReceiver(
            this,
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )

        checkForUpdates()
    }

    override fun onResume() {
        super.onResume()
        val apk = apkEnAttente
        if (apk != null && packageManager.canRequestPackageInstalls()) {
            apkEnAttente = null
            lancerInstallation(apk)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadCompleteReceiver)
    }

    private fun versionActuelle(): Pair<String, Long> {
        val info = packageManager.getPackageInfo(packageName, 0)
        return (info.versionName ?: "inconnue") to PackageInfoCompat.getLongVersionCode(info)
    }

    private fun checkForUpdates() {
        binding.tvStatus.text = "Vérification en cours..."
        binding.btnInstall.isEnabled = false

        coroutineScope.launch {
            try {
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("User-Agent", "Kobayashi AntiScan")
                            .addHeader("Accept", "application/vnd.github+json")
                            .build()
                        chain.proceed(request)
                    }
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.github.com/")
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(GitHubService::class.java)
                val release = service.getLatestRelease("MaitreKoba", "KobaAntiScam")

                withContext(Dispatchers.Main) {
                    val tagName = release.tag_name
                    if (tagName != null) {
                        latestRelease = release
                        val (currentVersion, _) = versionActuelle()

                        binding.tvCurrentVersion.text = "Version actuelle: $currentVersion"
                        binding.tvLatestVersion.text = "Dernière version: $tagName"

                        if (isNewerVersion(tagName, currentVersion)) {
                            binding.tvStatus.text = "Mise à jour disponible!"
                            binding.btnInstall.isEnabled = true
                        } else {
                            binding.tvStatus.text = "Aucune mise à jour disponible"
                        }
                    } else {
                        binding.tvStatus.text = "Impossible de vérifier les mises à jour"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = "Erreur: ${e.message}"
                    journaliser(LogType.ERROR, "Erreur vérification mise à jour: ${e.message}")
                }
            }
        }
    }

    private fun parseVersion(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V")
            .substringBefore('-').substringBefore('+')
            .split('.')
            .map { it.toIntOrNull() ?: 0 }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val a = parseVersion(latest)
        val b = parseVersion(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun fichierApk(): File =
        File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "KobaAntiScan-update.apk")

    private fun downloadUpdate() {
        val release = latestRelease
        if (release == null) {
            binding.tvStatus.text = "Erreur: release introuvable"
            return
        }

        val asset = release.assets.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
        val url = asset?.browser_download_url
        if (url == null) {
            binding.tvStatus.text = "Aucun APK dans la release ${release.tag_name}"
            return
        }

        val cible = fichierApk()
        cible.parentFile?.mkdirs()
        if (cible.exists()) cible.delete()

        Log.d(tag, "Téléchargement de ${asset.name} depuis $url vers ${cible.path}")
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setDestinationUri(Uri.fromFile(cible))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setTitle("Kobayashi AntiScan ${release.tag_name}")
            setDescription("Mise à jour en cours...")
        }

        downloadId = downloadManager.enqueue(request)
        binding.btnInstall.isEnabled = false
        binding.tvStatus.text = "Téléchargement en cours..."
    }

    private fun onDownloadTermine(id: Long) {
        val curseur = downloadManager.query(DownloadManager.Query().setFilterById(id))
        val (statut, raison) = curseur.use {
            if (!it.moveToFirst()) return@use DownloadManager.STATUS_FAILED to -1
            it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) to
                it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
        }
        if (statut != DownloadManager.STATUS_SUCCESSFUL) {
            echec("Téléchargement échoué (code $raison)")
            return
        }

        val apk = fichierApk()
        val info = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageArchiveInfo(apk.path, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(apk.path, 0)
        }
        if (info == null) {
            echec("APK téléchargé illisible")
            return
        }
        val (versionCourante, codeCourant) = versionActuelle()
        val codeApk = PackageInfoCompat.getLongVersionCode(info)
        if (info.packageName != packageName) {
            echec("APK pour ${info.packageName}, attendu $packageName")
            return
        }
        if (codeApk <= codeCourant) {
            echec("L'APK de la release est en ${info.versionName} (code $codeApk), pas plus récent que $versionCourante (code $codeCourant)")
            return
        }

        if (!packageManager.canRequestPackageInstalls()) {
            apkEnAttente = apk
            binding.tvStatus.text = "Autorise l'installation depuis cette app, puis reviens"
            startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
            )
            return
        }
        lancerInstallation(apk)
    }

    private fun lancerInstallation(apk: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", apk)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(installIntent)
            binding.tvStatus.text = "Installation en cours..."
            journaliser(LogType.UPDATE_CHECK, "Installation de la mise à jour: ${latestRelease?.tag_name}")
        } catch (e: Exception) {
            echec("Lancement de l'installation impossible: ${e.message}")
        }
    }

    private fun echec(message: String) {
        Log.w(tag, message)
        binding.tvStatus.text = message
        binding.btnInstall.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        journaliser(LogType.ERROR, "Mise à jour: $message")
    }

    private fun journaliser(type: LogType, message: String) {
        coroutineScope.launch {
            try {
                RuleDatabase.getDatabase().logDao().insert(LogEntry(type = type, message = message))
            } catch (e: Exception) {
                Log.w(tag, "Journal indisponible: ${e.message}")
            }
        }
    }
}
