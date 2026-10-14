package fr.kobayashi.shield.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.telecom.Call
import android.telecom.TelecomManager
import android.util.Log
import java.io.IOException

/**
 * Service pour gérer les appels de rappel avec message vocal
 */
class CallbackService : Service() {
    
    private val tag = "CallbackService"
    private var mediaPlayer: MediaPlayer? = null
    private var call: Call? = null
    private var callbackPhoneNumber: String? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callbackPhoneNumber = intent?.getStringExtra("PHONE_NUMBER")
        
        if (callbackPhoneNumber.isNullOrBlank()) {
            Log.e(tag, "No phone number provided for callback")
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Initier l'appel
        startCallbackCall(callbackPhoneNumber!!)
        
        return START_STICKY
    }
    
    private fun startCallbackCall(phoneNumber: String) {
        try {
            // Utiliser un Intent pour démarrer l'appel
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = android.net.Uri.fromParts("tel", phoneNumber, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            // Démarrer l'appel
            callIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            startActivity(callIntent)
            
            // Écouter l'état de l'appel via un BroadcastReceiver
            // (On ne peut pas facilement intercepter l'état de l'appel lancé par Intent)
            // Donc on va jouer l'audio après un délai
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                playRejectionMessage()
            }, 3000) // Attendre 3 secondes que l'appel soit établi
            
            // Arrêter après 15 secondes
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                cleanup()
                stopSelf()
            }, 15000) // 15 secondes au total
            
        } catch (e: Exception) {
            Log.e(tag, "Error starting callback call", e)
            cleanup()
            stopSelf()
        }
    }
    
    private fun playRejectionMessage() {
        try {
            // Arrêter toute lecture précédente
            mediaPlayer?.release()
            mediaPlayer = null
            
            // Créer le MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                try {
                    // Charger l'audio depuis les ressources
                    val afd = assets.openFd("rejection_message.mp3")
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    
                    setOnPreparedListener {
                        Log.d(tag, "Audio prepared, starting playback")
                        start()
                        
                        // Arrêter l'appel après 10 secondes
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            stopPlayback()
                            disconnectCall()
                        }, 10000) // 10 secondes
                    }
                    
                    setOnErrorListener { mp, what, extra ->
                        Log.e(tag, "MediaPlayer error: what=$what, extra=$extra")
                        stopPlayback()
                        disconnectCall()
                        true
                    }
                    
                    prepareAsync()
                    
                } catch (e: IOException) {
                    Log.e(tag, "Error preparing audio", e)
                    stopPlayback()
                    disconnectCall()
                }
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error playing audio", e)
            disconnectCall()
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
    
    private fun disconnectCall() {
        try {
            call?.disconnect()
            call = null
        } catch (e: Exception) {
            Log.e(tag, "Error disconnecting call", e)
        }
    }
    
    private fun cleanup() {
        stopPlayback()
        disconnectCall()
    }
    
    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }
}
