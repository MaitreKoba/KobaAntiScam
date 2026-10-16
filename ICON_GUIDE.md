# Guide pour personnaliser l'icône de l'application

/**
 * FICHIER : ICON_GUIDE.md | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/ICON_GUIDE.md
 * LIGNES : ~100 | RÔLE : Guide de personnalisation d'icône | DATE : 2026-10-14
 * VERSION : 1.0.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi
 * STATUT : STABLE | DÉPENDANCES : Aucun | LIEN : ICON_GUIDE.md
 * NOTES : Instructions pour remplacer l'icône par défaut
 */

## 🎨 Remplacement de l'icône de l'application

### Structure des fichiers d'icône

L'application utilise actuellement des icônes vectorielles (XML) dans :
```
app/src/main/res/mipmap-*/ic_launcher.xml
app/src/main/res/mipmap-*/ic_launcher_round.xml
app/src/main/res/drawable/ic_launcher.xml
```

Pour utiliser une image personnalisée (comme l'image que tu as fournie), tu dois :

### Étape 1 : Préparer tes images d'icône

Crée les fichiers PNG pour chaque densité d'écran :

| Densité | Taille | Dossier | Fichier |
|---------|-------|---------|--------|
| mdpi | 48×48px | `res/mipmap-mdpi/` | `ic_launcher.png` |
| hdpi | 72×72px | `res/mipmap-hdpi/` | `ic_launcher.png` |
| xhdpi | 96×96px | `res/mipmap-xhdpi/` | `ic_launcher.png` |
| xxhdpi | 144×144px | `res/mipmap-xxhdpi/` | `ic_launcher.png` |
| xxxhdpi | 192×192px | `res/mipmap-xxxhdpi/` | `ic_launcher.png` |

Et les versions rondes pour Android 7.1+ :
```
res/mipmap-mdpi/ic_launcher_round.png
res/mipmap-hdpi/ic_launcher_round.png
res/mipmap-xhdpi/ic_launcher_round.png
res/mipmap-xxhdpi/ic_launcher_round.png
res/mipmap-xxxhdpi/ic_launcher_round.png
```

### Étape 2 : Supprimer les anciens fichiers XML

Supprime les fichiers XML actuels :
```bash
rm app/src/main/res/mipmap-*/ic_launcher.xml
rm app/src/main/res/mipmap-*/ic_launcher_round.xml
rm app/src/main/res/drawable/ic_launcher.xml
```

### Étape 3 : Convertir ton image en icônes Android

#### Option A : Utiliser Android Studio
1. Ouvre le projet dans Android Studio
2. Fais un clic droit sur `app/src/main/res/`
3. Sélectionne **New → Image Asset**
4. Choisis ton image source
5. Configure :
   - Name: `ic_launcher`
   - Type: Launcher Icons
   - Padding: 0%
   - Shape: None (ou Square)
6. Clique sur **Next** puis **Finish**

Android Studio générera automatiquement toutes les tailles nécessaires.

#### Option B : Utiliser un générateur en ligne
- [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/)
- [Icon Kitchen](https://icon.kitchen/)
- [App Icon Generator](https://appicon.co/)

Télécharge le pack d'icônes et place les fichiers dans les dossiers appropriés.

### Étape 4 : Renommer les fichiers

Assure-toi que les fichiers sont nommés exactement :
- `ic_launcher.png` (dans chaque dossier mipmap-*)
- `ic_launcher_round.png` (dans chaque dossier mipmap-*)

### Étape 5 : Mettre à jour AndroidManifest.xml

Ton AndroidManifest.xml doit avoir :
```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    ...>
```

C'est déjà configuré ainsi dans ton projet.

### Étape 6 : Recompiler et tester

```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 📁 Où placer tes images

Copie tes fichiers PNG générés dans :
```
app/src/main/res/mipmap-mdpi/ic_launcher.png
app/src/main/res/mipmap-mdpi/ic_launcher_round.png
app/src/main/res/mipmap-hdpi/ic_launcher.png
app/src/main/res/mipmap-hdpi/ic_launcher_round.png
app/src/main/res/mipmap-xhdpi/ic_launcher.png
app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
app/src/main/res/mipmap-xxhdpi/ic_launcher.png
app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
```

## 🎨 Spécifications de l'icône

- **Format** : PNG
- **Fond** : Transparent de préférence
- **Couleurs** : Utilise des couleurs contrastées pour une bonne visibilité
- **Style** : Adapté à une application de sécurité/anti-arnaque

## 📌 Image fournie par l'utilisateur

Tu as fourni une image d'une fille anime avec :
- Cheveux longs orange/rose
- Cornes
- Yeux bleus
- Tenue avec cravate

Pour utiliser cette image comme icône :
1. Recadre l'image pour qu'elle soit carrée
2. Supprime l'arrière-plan si nécessaire
3. Utilise un outil de conversion pour générer les 5 tailles
4. Place les fichiers dans les dossiers mipmap-*

## ✅ Vérification

Après avoir remplacé les icônes :
1. L'icône de l'app dans le launcher doit être la nouvelle image
2. L'icône dans les paramètres doit être la nouvelle image
3. L'icône dans le sélectionneur d'apps doit être la nouvelle image

Si l'icône ne change pas, fais :
```bash
adb shell pm clear fr.kobayashi.shield.debug
adb install -r -d app/build/outputs/apk/debug/app-debug.apk
```

## 🔄 Pour revenir aux icônes par défaut

Si tu veux revenir aux icônes par défaut, supprime simplement tes fichiers PNG et rétablis les fichiers XML originaux.
