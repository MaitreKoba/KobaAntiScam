package fr.kobayashi.shield

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import fr.kobayashi.shield.databinding.ActivityUpdateBinding
import fr.kobayashi.shield.model.LogEntry
import fr.kobayashi.shield.model.LogType
import fr.kobayashi.shield.model.RuleDatabase
import fr.kobayashi.shield.network.GitHubService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class UpdateActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityUpdateBinding
    private lateinit var downloadManager: DownloadManager
    private var downloadId: Long = -1L
    private var latestRelease: GitHubService.Release? = null
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id == downloadId) {
                installUpdate()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        
        registerReceiver(
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        
        checkForUpdates()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadCompleteReceiver)
    }
    
    private fun checkForUpdates() {
        val githubRepo = "MaitreKoba/KobaAntiScam"
        
        binding.tvStatus.text = "Vérification en cours..."
        
        coroutineScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.github.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                
                val service = retrofit.create(GitHubService::class.java)
                val releases = service.getLatestRelease("MaitreKoba", "KobaAntiScam")
                
                withContext(Dispatchers.Main) {
                    if (releases != null && releases.tag_name != null) {
                        latestRelease = releases
                        val currentVersion = packageManager.getPackageInfo(packageName, 0)?.versionName ?: "unknown"
                        
                        binding.tvCurrentVersion.text = "Version actuelle: $currentVersion"
                        binding.tvLatestVersion.text = "Dernière version: ${releases.tag_name}"
                        
                        if (isNewerVersion(releases.tag_name, currentVersion)) {
                            binding.tvStatus.text = "Mise à jour disponible!"
                            binding.btnInstall.isEnabled = true
                        } else {
                            binding.tvStatus.text = "Aucune mise à jour disponible"
                            binding.btnInstall.isEnabled = false
                        }
                    } else {
                        binding.tvStatus.text = "Impossible de vérifier les mises à jour"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = "Erreur: ${e.message}"
                    RuleDatabase.getDatabase().logDao().insert(
                        LogEntry(
                            type = LogType.ERROR,
                            message = "Erreur vérification mise à jour: ${e.message}"
                        )
                    )
                }
            }
        }
    }
    
    private fun isNewerVersion(latest: String, current: String): Boolean {
        // Simple version comparison (for demo purposes)
        // TODO: Implement proper semantic version comparison
        return latest != current
    }
    
    private fun downloadUpdate() {
        latestRelease?.assets?.firstOrNull()?.let { asset ->
            val downloadUri = Uri.parse(asset.browser_download_url)
            
            val request = DownloadManager.Request(downloadUri).apply {
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "KobaAntiScan-${latestRelease?.tag_name}.apk"
                )
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setTitle("Téléchargement de Kobayashi AntiScan ${latestRelease?.tag_name}")
                setDescription("Mise à jour en cours...")
            }
            
            downloadId = downloadManager.enqueue(request)
            binding.tvStatus.text = "Téléchargement en cours..."
            Toast.makeText(this, "Téléchargement démarré", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "URL de téléchargement introuvable", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun installUpdate() {
        val fileName = "KobaAntiScan-${latestRelease?.tag_name}.apk"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            startActivity(installIntent)
            binding.tvStatus.text = "Installation en cours..."
            
            coroutineScope.launch {
                RuleDatabase.getDatabase().logDao().insert(
                    LogEntry(
                        type = LogType.UPDATE_CHECK,
                        message = "Installation de la mise à jour: ${latestRelease?.tag_name}"
                    )
                )
            }
        } else {
            Toast.makeText(this, "Fichier APK introuvable", Toast.LENGTH_SHORT).show()
        }
    }
}
