package fr.kobayashi.shield.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.telecom.Call
import android.util.Log
import java.io.IOException

/**
 * Service pour jouer un message vocal personnalisé comme messagerie
 * Quand un appel est bloqué, ce service joue le MP3 pour indiquer à l'appelant
 * que l'appel a été bloqué
 */
class VoicemailService : Service() {
    
    private val tag = "VoicemailService"
    private var mediaPlayer: MediaPlayer? = null
    private var call: Call? = null
    private var targetPhoneNumber: String? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetPhoneNumber = intent?.getStringExtra("PHONE_NUMBER")
        
        if (targetPhoneNumber.isNullOrBlank()) {
            Log.e(tag, "No phone number provided")
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Jouer le message vocal après un court délai
        playVoicemailMessage()
        
        return START_STICKY
    }
    
    private fun playVoicemailMessage() {
        try {
            // Libérer tout MediaPlayer précédent
            mediaPlayer?.release()
            mediaPlayer = null
            
            // Créer le MediaPlayer pour jouer le MP3
            mediaPlayer = MediaPlayer().apply {
                try {
                    // Charger l'audio depuis les assets
                    val afd = assets.openFd("rejection_message.mp3")
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    
                    setOnPreparedListener {
                        Log.d(tag, "Voicemail message prepared, starting playback")
                        start()
                        
                        // Arrêter après la lecture (le MP3 fait ~10 secondes)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            stopPlayback()
                            stopSelf()
                        }, 12000) // 12 secondes pour être sûr
                    }
                    
                    setOnErrorListener { _, what, extra ->
                        Log.e(tag, "MediaPlayer error: what=$what, extra=$extra")
                        stopPlayback()
                        stopSelf()
                        true
                    }
                    
                    setOnCompletionListener {
                        Log.d(tag, "Voicemail message completed")
                        stopPlayback()
                        stopSelf()
                    }
                    
                    prepareAsync()
                    
                } catch (e: IOException) {
                    Log.e(tag, "Error preparing voicemail audio", e)
                    stopPlayback()
                    stopSelf()
                }
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error playing voicemail", e)
            stopSelf()
        }
    }
    
    private fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(tag, "Error stopping playback", e)
        }
    }
    
    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }
}
