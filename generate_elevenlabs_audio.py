#!/usr/bin/env python3
"""
Script to generate audio with ElevenLabs API for Kobayashi AntiScam

Requirements:
- pip install requests
- Set ELEVENLABS_API_KEY environment variable

Usage:
    python generate_elevenlabs_audio.py
"""

import os
import requests
import json

# Configuration
VOICE_ID = "iCKVfVbyCo5AAswzTkkX"  # Ta voix ElevenLabs
TEXT = "Votre appel a été bloqué par Kobayashi AntiScam. Merci de formuler votre demande pour être recontacté."
OUTPUT_FILE = "app/src/main/res/raw/rejection_message.mp3"

# API Configuration
ELEVENLABS_API_URL = "https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"

# Headers
def get_headers(api_key):
    return {
        "xi-api-key": api_key,
        "Content-Type": "application/json"
    }

# Request body
def get_request_body(text):
    return {
        "text": text,
        "model_id": "eleven_multilingual_v2",  # ou "eleven_monolingual_v1" pour anglais
        "voice_settings": {
            "stability": 0.5,
            "similarity_boost": 0.5,
            "style": 0.0,
            "use_speaker_boost": True
        }
    }

def main():
    # Get API key from environment variable
    api_key = os.getenv("ELEVENLABS_API_KEY")
    
    if not api_key:
        print("❌ ELEVENLABS_API_KEY non trouvé !")
        print("Exporte ta clé API : export ELEVENLABS_API_KEY='ta_clé_ici'")
        return
    
    print(f"🔑 Utilisation de la voix : {VOICE_ID}")
    print(f"📝 Texte à synthétiser : {TEXT}")
    
    try:
        # Make the API request
        url = ELEVENLABS_API_URL.format(voice_id=VOICE_ID)
        headers = get_headers(api_key)
        data = get_request_body(TEXT)
        
        print("📡 Requête à ElevenLabs API...")
        response = requests.post(url, headers=headers, data=json.dumps(data), stream=True)
        
        if response.status_code != 200:
            print(f"❌ Erreur API : {response.status_code}")
            print(f"Réponse : {response.text}")
            return
        
        print("✅ Audio généré avec succès !")
        
        # Save the audio file
        with open(OUTPUT_FILE, "wb") as f:
            for chunk in response.iter_content(chunk_size=1024):
                if chunk:
                    f.write(chunk)
        
        print(f"💾 Fichier sauvegardé : {OUTPUT_FILE}")
        print("\n✨ Exécute ensuite :")
        print("   ./gradlew assembleDebug")
        print("   adb install -r app/build/outputs/apk/debug/app-debug.apk")
        
    except Exception as e:
        print(f"❌ Erreur : {e}")

if __name__ == "__main__":
    main()
