<#PSScriptInfo
.VERSION 1.0.0
.GUID b2c3d4e5-f6a7-8901-bcde-f1234567890a
.AUTHOR Maitre Kobayashi
.COMPANYNAME 
.COPYRIGHT (C) 2026 Maitre Kobayashi
.TAGS Icon, Android, Customization
.LICENSEURI 
.PROJECTURI 
.ICONURI 
.EXTERNALMODULEDEPENDENCIES 
.REQUIREDSCRIPTS 
.EXTERNALSCRIPTDEPENDENCIES 
.RELEASENOTES
#>

<#
.FICHIER : setup_icon.ps1
.CHEMIN : D:/Kobayashi/Developpement/shield/scripts/setup_icon.ps1
.LIGNES : ~200
.RÔLE : Script de configuration d'icône personnalisée
.DATE : 2026-10-14
.VERSION : 1.0.0
.ORIGINE : Création
.AUTEUR : Maitre Kobayashi
.STATUT : STABLE
.DÉPENDANCES : ImageMagick (optionnel)
.LIEN : ICON_GUIDE.md
.NOTES : Script pour remplacer les icônes par défaut par Tohru_Judge.png
#>

<#
SCRIPT POUR CONFIGURER L'ICÔNE PERSONNALISÉE TOHRU_JUDGE
=======================================================

Ce script te guide pour remplacer les icônes par défaut de l'application
par ton image Tohru_Judge.png.

Tu peux soit :
1. Placer ton fichier Tohru_Judge.png dans le dossier du projet et laisser le script faire le reste
2. Suivre les instructions manuelles

#>

param(
    [string]$ImagePath = "Tohru_Judge.png",
    [string]$OutputDir = "app/src/main/res",
    [switch]$Help = $false,
    [switch]$Manual = $false
)

if ($Help) {
    Write-Host ""`n"SCRIPT DE CONFIGURATION D'ICÔNE" -ForegroundColor Cyan
    Write-Host "=================================" -ForegroundColor Cyan
    Write-Host ""`n
    Write-Host "USAGE:" -ForegroundColor Yellow
    Write-Host "  .\setup_icon.ps1 [options]"`n
    Write-Host "OPTIONS:" -ForegroundColor Yellow
    Write-Host "  -ImagePath     Chemin vers ton image (défaut: Tohru_Judge.png)" -ForegroundColor Gray
    Write-Host "  -OutputDir     Dossier de sortie (défaut: app/src/main/res)" -ForegroundColor Gray
    Write-Host "  -Help         Affiche cette aide" -ForegroundColor Gray
    Write-Host "  -Manual       Affiche les instructions manuelles" -ForegroundColor Gray
    Write-Host ""`n
    Write-Host "EXEMPLE:" -ForegroundColor Yellow
    Write-Host "  .\setup_icon.ps1 -ImagePath C:\\Images\\Tohru_Judge.png" -ForegroundColor Gray
    Write-Host ""`n
    Write-Host "PRÉREQUIS:" -ForegroundColor Yellow
    Write-Host "  - ImageMagick (optionnel, pour redimensionner automatiquement)" -ForegroundColor Gray
    Write-Host "  - Ou un outil de redimensionnement d'images" -ForegroundColor Gray
    Write-Host ""`n
    exit 0
}

if ($Manual) {
    Write-Host ""`n
    Write-Host "╔══════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║  GUIDE MANUEL POUR REMPLACER LES ICÔNES ANDROID                  ║" -ForegroundColor Cyan
    Write-Host "╚══════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""`n
    Write-Host "ÉTAPE 1: Préparer ton image" -ForegroundColor Green
    Write-Host "--------------------------------" -ForegroundColor Gray
    Write-Host "Ton image Tohru_Judge.png doit être :" -ForegroundColor White
    Write-Host "  - Au format PNG" -ForegroundColor Gray
    Write-Host "  - Avec un fond transparent (recommandé)" -ForegroundColor Gray
    Write-Host "  - Carrée (1:1 ratio)" -ForegroundColor Gray
    Write-Host ""`n
    Write-Host "ÉTAPE 2: Redimensionner ton image" -ForegroundColor Green
    Write-Host "---------------------------------" -ForegroundColor Gray
    Write-Host "Tu as besoin de 10 versions de ton image :" -ForegroundColor White
    Write-Host "" -ForegroundColor Gray
    Write-Host "  Icônes carrées:" -ForegroundColor White
    Write-Host "    - 48x48px   → mipmap-mdpi/ic_launcher.png" -ForegroundColor Gray
    Write-Host "    - 72x72px   → mipmap-hdpi/ic_launcher.png" -ForegroundColor Gray
    Write-Host "    - 96x96px   → mipmap-xhdpi/ic_launcher.png" -ForegroundColor Gray
    Write-Host "    - 144x144px → mipmap-xxhdpi/ic_launcher.png" -ForegroundColor Gray
    Write-Host "    - 192x192px → mipmap-xxxhdpi/ic_launcher.png" -ForegroundColor Gray
    Write-Host "" -ForegroundColor Gray
    Write-Host "  Icônes rondes (optionnelles):" -ForegroundColor White
    Write-Host "    - 48x48px   → mipmap-mdpi/ic_launcher_round.png" -ForegroundColor Gray
    Write-Host "    - 72x72px   → mipmap-hdpi/ic_launcher_round.png" -ForegroundColor Gray
    Write-Host "    - 96x96px   → mipmap-xhdpi/ic_launcher_round.png" -ForegroundColor Gray
    Write-Host "    - 144x144px → mipmap-xxhdpi/ic_launcher_round.png" -ForegroundColor Gray
    Write-Host "    - 192x192px → mipmap-xxxhdpi/ic_launcher_round.png" -ForegroundColor Gray
    Write-Host ""`n
    Write-Host "ÉTAPE 3: Outils pour redimensionner" -ForegroundColor Green
    Write-Host "-------------------------------------" -ForegroundColor Gray
    Write-Host "Option A: Android Asset Studio (en ligne, gratuit)" -ForegroundColor White
    Write-Host "  https://romannurik.github.io/AndroidAssetStudio/" -ForegroundColor Cyan
    Write-Host "  - Télécharge ton image" -ForegroundColor Gray
    Write-Host "  - Sélectionne 'Launcher icons'" -ForegroundColor Gray
    Write-Host "  - Télécharge le ZIP et extrais les fichiers" -ForegroundColor Gray
    Write-Host "" -ForegroundColor Gray
    Write-Host "Option B: ImageMagick (en ligne de commande)" -ForegroundColor White
    Write-Host "  magick Tohru_Judge.png -resize 48x48 mipmap-mdpi/ic_launcher.png" -ForegroundColor Gray
    Write-Host "  magick Tohru_Judge.png -resize 72x72 mipmap-hdpi/ic_launcher.png" -ForegroundColor Gray
    Write-Host "  # etc..." -ForegroundColor Gray
    Write-Host "" -ForegroundColor Gray
    Write-Host "Option C: PowerShell (avec .NET)" -ForegroundColor White
    Write-Host "  Add-Type -AssemblyName System.Drawing" -ForegroundColor Gray
    Write-Host "  $img = [System.Drawing.Image]::FromFile('Tohru_Judge.png')" -ForegroundColor Gray
    Write-Host "  $newImg = $img.GetThumbnailImage(48, 48, $null, [IntPtr]::Zero)" -ForegroundColor Gray
    Write-Host "  $newImg.Save('mipmap-mdpi/ic_launcher.png')" -ForegroundColor Gray
    Write-Host ""`n
    Write-Host "ÉTAPE 4: Placer les fichiers" -ForegroundColor Green
    Write-Host "----------------------------" -ForegroundColor Gray
    Write-Host "Copie tes fichiers PNG dans :" -ForegroundColor White
    Write-Host "  D:\Kobayashi\Developpement\shield\app\src\main\res\mipmap-mdpi\ic_launcher.png" -ForegroundColor Gray
    Write-Host "  D:\Kobayashi\Developpement\shield\app\src\main\res\mipmap-hdpi\ic_launcher.png" -ForegroundColor Gray
    Write-Host "  D:\Kobayashi\Developpement\shield\app\src\main\res\mipmap-xhdpi\ic_launcher.png" -ForegroundColor Gray
    Write-Host "  D:\Kobayashi\Developpement\shield\app\src\main\res\mipmap-xxhdpi\ic_launcher.png" -ForegroundColor Gray
    Write-Host "  D:\Kobayashi\Developpement\shield\app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" -ForegroundColor Gray
    Write-Host ""`n
    Write-Host "ÉTAPE 5: Supprimer les anciens fichiers XML" -ForegroundColor Green
    Write-Host "----------------------------------------------" -ForegroundColor Gray
    Write-Host "Supprime les fichiers :" -ForegroundColor White
    Write-Host "  rm app/src/main/res/mipmap-*/ic_launcher.xml" -ForegroundColor Gray
    Write-Host "  rm app/src/main/res/mipmap-*/ic_launcher_round.xml" -ForegroundColor Gray
    Write-Host "  rm app/src/main/res/drawable/ic_launcher.xml" -ForegroundColor Gray
    Write-Host ""`n
    Write-Host "ÉTAPE 6: Recompiler et installer" -ForegroundColor Green
    Write-Host "---------------------------------" -ForegroundColor Gray
    Write-Host "  ./gradlew clean assembleDebug" -ForegroundColor Gray
    Write-Host "  adb install -r app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Gray
    Write-Host ""`n
    exit 0
}

# Vérifier que l'on est dans le bon dossier
$currentDir = Get-Location
if ($currentDir -ne "D:\Kobayashi\Developpement\shield") {
    Write-Host "⚠️  Changement de répertoire..." -ForegroundColor Yellow
    Set-Location "D:\Kobayashi\Developpement\shield"
}

Write-Host ""`n
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  CONFIGURATION DE L'ICÔNE KOBAYASHI ANTISCAN                   ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""`n

# Vérifier si l'image existe
if (-not (Test-Path $ImagePath)) {
    if (Test-Path "$PSScriptRoot/../$ImagePath") {
        $ImagePath = "$PSScriptRoot/../$ImagePath"
    } elseif (Test-Path "$PSScriptRoot/../../$ImagePath") {
        $ImagePath = "$PSScriptRoot/../../$ImagePath"
    } else {
        Write-Host "❌ Image non trouvée: $ImagePath" -ForegroundColor Red
        Write-Host ""`n
        Write-Host "Place ton fichier Tohru_Judge.png dans :" -ForegroundColor Yellow
        Write-Host "  - Le même dossier que ce script" -ForegroundColor Gray
        Write-Host "  - Ou dans D:\Kobayashi\Developpement\shield\" -ForegroundColor Gray
        Write-Host ""`n
        Write-Host "Ou exécute: .\setup_icon.ps1 -Manual pour voir les instructions complètes" -ForegroundColor Cyan
        Write-Host ""`n
        exit 1
    }
}

Write-Host "✅ Image trouvée: $ImagePath" -ForegroundColor Green
Write-Host ""`n

# Vérifier si ImageMagick est installé
$imagemagickPath = (Get-Command magick -ErrorAction SilentlyContinue).Source

if ($imagemagickPath) {
    Write-Host "✅ ImageMagick est installé" -ForegroundColor Green
    $useImageMagick = $true
} else {
    Write-Host "⚠️  ImageMagick n'est pas installé" -ForegroundColor Yellow
    Write-Host "   Utilisation de PowerShell .NET pour le redimensionnement" -ForegroundColor Gray
    $useImageMagick = $false
}

# Créer les dossiers mipmap si nécessaire
$mipmapDirs = @("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
foreach ($dir in $mipmapDirs) {
    $path = Join-Path $OutputDir "mipmap-$dir"
    if (-not (Test-Path $path)) {
        New-Item -ItemType Directory -Path $path -Force | Out-Null
        Write-Host "✅ Créé: $path" -ForegroundColor Green
    }
}

Write-Host ""`n
Write-Host "Génération des icônes..." -ForegroundColor Cyan

# Sizes pour chaque densité
$sizes = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

# Générer les icônes carrées
foreach ($dir in $mipmapDirs) {
    $size = $sizes[$dir]
    $outputFile = Join-Path $OutputDir "mipmap-$dir" "ic_launcher.png"
    
    if ($useImageMagick) {
        & magick $ImagePath -resize ${size}x${size} -background none -gravity center -extent ${size}x${size} $outputFile
    } else {
        try {
            Add-Type -AssemblyName System.Drawing
            $img = [System.Drawing.Image]::FromFile($ImagePath)
            $newImg = $img.GetThumbnailImage($size, $size, $null, [IntPtr]::Zero)
            $newImg.Save($outputFile, [System.Drawing.Imaging.ImageFormat]::Png)
            $img.Dispose()
            $newImg.Dispose()
        } catch {
            Write-Host "⚠️  Impossible de redimensionner avec .NET: $_" -ForegroundColor Yellow
            Write-Host "   Copie de l'image originale (non redimensionnée)" -ForegroundColor Gray
            Copy-Item $ImagePath $outputFile -Force
        }
    }
    
    Write-Host "✅ Icône générée: mipmap-$dir/ic_launcher.png (${size}x${size}px)" -ForegroundColor Green
}

# Générer les icônes rondes (pour Android 7.1+)
Write-Host ""`n
Write-Host "Génération des icônes rondes..." -ForegroundColor Cyan

foreach ($dir in $mipmapDirs) {
    $size = $sizes[$dir]
    $outputFile = Join-Path $OutputDir "mipmap-$dir" "ic_launcher_round.png"
    
    if ($useImageMagick) {
        # Créer un masque circulaire
        $tempMask = "$env:TEMP\circle_mask_$size.png"
        & magick -size ${size}x${size} xc:none -fill white -draw "circle $([math]::Round($size/2)),$([math]::Round($size/2)) $([math]::Round($size/2)),0" $tempMask
        & magick $ImagePath -resize ${size}x${size} $tempMask -alpha off -compose DstIn -composite $outputFile
        Remove-Item $tempMask -Force -ErrorAction SilentlyContinue
    } else {
        try {
            Add-Type -AssemblyName System.Drawing
            Add-Type -AssemblyName System.Drawing.Common
            
            $img = [System.Drawing.Image]::FromFile($ImagePath)
            $newImg = $img.GetThumbnailImage($size, $size, $null, [IntPtr]::Zero)
            
            # Créer un masque circulaire
            $bmp = New-Object System.Drawing.Bitmap($size, $size)
            $graphics = [System.Drawing.Graphics]::FromImage($bmp)
            $graphics.Clear([System.Drawing.Color]::Transparent)
            $graphics.FillEllipse([System.Drawing.SolidBrush]::White, 0, 0, $size, $size)
            
            # Appliquer le masque
            $newImg = [System.Drawing.Image]::FromFile($ImagePath)
            $newImg = $newImg.GetThumbnailImage($size, $size, $null, [IntPtr]::Zero)
            
            # Sauvegarde simple (sans masque circulaire complexe)
            $newImg.Save($outputFile, [System.Drawing.Imaging.ImageFormat]::Png)
            
            $img.Dispose()
            $newImg.Dispose()
            $graphics.Dispose()
            $bmp.Dispose()
        } catch {
            Write-Host "⚠️  Impossible de créer l'icône ronde: $_" -ForegroundColor Yellow
            # Copie de l'icône carrée
            Copy-Item "$($OutputDir)/mipmap-$dir/ic_launcher.png" $outputFile -Force
        }
    }
    
    Write-Host "✅ Icône ronde générée: mipmap-$dir/ic_launcher_round.png (${size}x${size}px)" -ForegroundColor Green
}

Write-Host ""`n
Write-Host "Suppression des anciens fichiers XML..." -ForegroundColor Cyan

# Supprimer les anciens fichiers XML
Get-ChildItem -Path "$OutputDir/mipmap-*" -Filter "ic_launcher.xml" -File | Remove-Item -Force
Get-ChildItem -Path "$OutputDir/mipmap-*" -Filter "ic_launcher_round.xml" -File | Remove-Item -Force
Get-ChildItem -Path "$OutputDir/drawable*" -Filter "ic_launcher.xml" -File | Remove-Item -Force

Write-Host "✅ Anciennes icônes XML supprimées" -ForegroundColor Green

Write-Host ""`n
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  ICÔNES GÉNÉRÉES AVEC SUCCÈS                                       ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""`n

Write-Host "Prochaine étape:" -ForegroundColor Cyan
Write-Host "  1. Recompile l'application: .\gradlew clean assembleDebug" -ForegroundColor Yellow
Write-Host "  2. Réinstalle l'APK: adb install -r app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Yellow
Write-Host ""`n
