# 🎤 Intégration ElevenLabs - Message Vocal Personnalisé

## ✨ Fonctionnalité : Message vocal avec ta voix pour les appels bloqués

Quand un appel est bloqué, l'application **rappelle automatiquement** l'appelant et **lit un message vocal** avec **TA voix ElevenLabs** !

**Message par défaut :**
> "Votre appel a été bloqué par Kobayashi AntiScam. Merci de formuler votre demande pour être recontacté."

---

## 🔧 Configuration

### Étape 1 : Générer l'audio avec ElevenLabs

1. **Installe Python** (si pas déjà installé)
2. **Installe les dépendances** :
   ```bash
   pip install requests
   ```

3. **Exécute le script de génération** :
   ```bash
   # Sur Windows (avec Git Bash)
   export ELEVENLABS_API_KEY="ta_clé_api_ici"
   python generate_elevenlabs_audio.py
   ```
   
   **Ou sur Windows CMD** :
   ```cmd
   set ELEVENLABS_API_KEY=ta_clé_api_ici
   python generate_elevenlabs_audio.py
   ```

4. **Résultat** : Le fichier `app/src/main/res/raw/rejection_message.mp3` sera créé.

---

### Étape 2 : Obtenir ta clé API ElevenLabs

1. Va sur [https://elevenlabs.io](https://elevenlabs.io)
2. Connecte-toi
3. Va dans **API Keys** (ou **Profile → API Keys**)
4. Copie ta clé API

---

### Étape 3 : Personnaliser le message

Modifie le script `generate_elevenlabs_audio.py` :
```python
# Change ces variables
VOICE_ID = "iCKVfVbyCo5AAswzTkkX"  # Ta voix
TEXT = "Votre appel a été bloqué par Kobayashi AntiScam. Merci de formuler votre demande pour être recontacté."
```

---

## 📊 Voices disponibles

Tu peux utiliser n'importe quelle voix ElevenLabs. Quelques exemples :
- **iCKVfVbyCo5AAswzTkkX** : Ta voix (déjà configurée)
- **21m00Tcm4TlvD9q8WJ3b** : Rachel (Anglais)
- **AZnzlk1XvdvUeBnXmlP** : Adam (Anglais)

**Pour trouver ton Voice ID** :
1. Va dans **Voices** sur ElevenLabs
2. Sélectionne ta voix
3. Copie l'ID dans l'URL ou les détails

---

## ⚙️ Activer/Désactiver le message vocal

Dans l'application **Kobayashi AntiScam** :
1. Va dans **Réglages**
2. Appuie sur **"Activer le message vocal"**
3. Le message vocal sera **activé/désactivé**

---

## ⚠️ Notes importantes

1. **Coût** : Les appels de rappel sont **payants** selon ton forfait téléphonique
2. **Permissions** : L'application a besoin de `CALL_PHONE` pour passer les appels
3. **Délai** : Il y a un délai de 2-3 secondes avant que le message ne soit lu
4. **Opérateurs** : Certains opérateurs peuvent bloquer les appels automatiques

---

## 🛠️ Dépannage

### Problème : L'audio ne se joue pas
- **Solution 1** : Vérifie que le fichier `rejection_message.mp3` existe dans `app/src/main/res/raw/`
- **Solution 2** : Vérifie que le message vocal est activé dans les Réglages
- **Solution 3** : Vérifie que tu as la permission `CALL_PHONE` activée

### Problème : Erreur API ElevenLabs
- **Solution** : Vérifie que ta clé API est correcte
- **Solution** : Vérifie que tu as des crédits sur ton compte ElevenLabs
- **Solution** : Vérifie que ton Voice ID est correct

---

## 🎉 Résultat

Quand un numéro est bloqué :
1. ✅ L'appel est **rejeté**
2. ✅ L'application **rappelle** l'appelant
3. ✅ **Ta voix** dit : "Votre appel a été bloqué par Kobayashi AntiScam..."
4. ✅ L'appel se **raccroche** automatiquement après 10 secondes

**L'appelant entend VRAIMENT ta voix ElevenLabs !** 🎤
