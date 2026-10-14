# SHIELD Development Script
# PowerShell script pour compiler, installer et tester l'app sur le téléphone
# Nécessite: JDK 17, Android SDK, adb, téléphone branché avec débogage USB activé

param(
    [string]$BuildType = "Debug",
    [switch]$SkipBuild = $false,
    [switch]$SkipInstall = $false,
    [switch]$SkipGrant = $false,
    [switch]$SkipRun = $false,
    [switch]$Logcat = $false
)

# Configuration
$ProjectDir = Split-Path -Parent $PSScriptRoot
$AppPackage = "fr.kobayashi.shield.debug"  # debug build suffix
$ApkPath = Join-Path $ProjectDir "app\build\outputs\apk\debug\app-debug.apk"
$JavaHome = "D:\Kobayashi\Developpement\Android\jdk17\jdk-17.0.20+8"
$AdbPath = "D:\Kobayashi\Developpement\Android\sdk\platform-tools\adb.exe"

# Fonction pour afficher les messages
function Write-Header([string]$message) {
    Write-Host "`n$message" -ForegroundColor Cyan
}

function Write-Success([string]$message) {
    Write-Host "[OK] $message" -ForegroundColor Green
}

function Write-ErrorMsg([string]$message) {
    Write-Host "[ERROR] $message" -ForegroundColor Red
}

function Write-Info([string]$message) {
    Write-Host "[INFO] $message" -ForegroundColor Gray
}

# Fonction pour exécuter une commande avec vérification
function Invoke-CheckCommand([string]$command, [string]$errorMessage) {
    try {
        Invoke-Expression $command | Out-Null
        return $true
    } catch {
        Write-ErrorMsg $errorMessage
        Write-ErrorMsg "Command failed: $command"
        return $false
    }
}

# Vérification de l'environnement
Write-Header "Vérification de l'environnement..."

if (-not (Test-Path $JavaHome)) {
    Write-ErrorMsg "JDK 17 non trouvé: $JavaHome"
    exit 1
}

if (-not (Test-Path $AdbPath)) {
    Write-ErrorMsg "adb non trouvé: $AdbPath"
    exit 1
}

$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"

# Étape 1: Build
if (-not $SkipBuild) {
    Write-Header "Compilation de l'application..."
    
    Set-Location $ProjectDir
    $buildResult = Invoke-CheckCommand "& .\gradlew.bat assembleDebug" "La compilation a échoué"
    
    if (-not $buildResult) {
        Write-ErrorMsg "Erreur lors de la compilation"
        exit 1
    }
    
    if (-not (Test-Path $ApkPath)) {
        Write-ErrorMsg "APK non généré: $ApkPath"
        exit 1
    }
    
    Write-Success "Compilation réussie"
}

# Étape 2: Vérification du périphérique
Write-Header "Vérification du périphérique Android..."

$devices = & $AdbPath devices 2>&1
if ($devices -match "List of devices attached") {
    $deviceLines = $devices | Select-String -Pattern "^[^\s]+" | ForEach-Object { $_.Line.Trim() }
    
    if ($deviceLines.Count -eq 0 -or $deviceLines -match "^$") {
        Write-ErrorMsg "Aucun périphérique Android détecté. Vérifiez que:"
        Write-ErrorMsg "  1. Le téléphone est bien branché en USB"
        Write-ErrorMsg "  2. Le débogage USB est activé sur le téléphone"
        Write-ErrorMsg "  3. Vous avez accepté l'invite de débogage USB sur le téléphone"
        Write-ErrorMsg "  4. Le câble USB permet le transfert de données"
        exit 1
    }
    
    Write-Success "Périphérique détecté: $($deviceLines -join ', ')"
} else {
    Write-ErrorMsg "Erreur lors de la détection du périphérique"
    Write-ErrorMsg $devices
    exit 1
}

# Étape 3: Installation de l'APK
if (-not $SkipInstall) {
    Write-Header "Installation de l'APK..."
    
    $installResult = Invoke-CheckCommand "& $AdbPath install -r -t $ApkPath" "L'installation a échoué"
    
    if ($installResult) {
        Write-Success "APK installé avec succès"
    } else {
        Write-ErrorMsg "Échec de l'installation. Essayez de désinstaller manuellement depuis le téléphone."
        exit 1
    }
}

# Étape 4: Attribution des permissions
if (-not $SkipGrant) {
    Write-Header "Attribution des permissions..."
    
    $permissions = @(
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_CALL_LOG",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.REQUEST_INSTALL_PACKAGES"
    )
    
    foreach ($permission in $permissions) {
        $result = Invoke-CheckCommand "& $AdbPath shell pm grant $AppPackage $permission" "Erreur lors de l'attribution de $permission"
        if ($result) {
            Write-Success "Permission $permission attribuée"
        } else {
            Write-ErrorMsg "Échec de l'attribution de $permission"
        }
    }
    
    # Désactiver l'optimisation de la batterie
    Write-Info "Désactivation de l'optimisation de la batterie..."
    Invoke-CheckCommand "& $AdbPath shell dumpsys deviceidle force-idle" "Erreur lors de la désactivation de l'optimisation" | Out-Null
}

# Étape 5: Attribution des rôles (nécessite Android 10+)
Write-Header "Attribution des rôles système..."

# Rôle de filtrage d'appels
$roleResult = & $AdbPath shell cmd role add-role-holder fr.kobayashi.shield android.app.role.CALL_SCREENING 2>&1
if ($roleResult -match "Success") {
    Write-Success "Rôle CALL_SCREENING attribué"
} else {
    Write-ErrorMsg "Échec de l'attribution du rôle CALL_SCREENING"
    Write-ErrorMsg "Action manuelle nécessaire: Allez dans Paramètres > Apps > Accès spécial > Filtrage des appels > SHIELD"
}

# Accès aux notifications
$notificationResult = & $AdbPath shell cmd notification allow_listener fr.kobayashi.shield 2>&1
if ($notificationResult -match "Success") {
    Write-Success "Accès aux notifications attribué"
} else {
    Write-ErrorMsg "Échec de l'attribution de l'accès aux notifications"
    Write-ErrorMsg "Action manuelle nécessaire: Allez dans Paramètres > Apps > Accès spécial > Accès aux notifications > SHIELD"
}

# Étape 6: Lancement de l'application
if (-not $SkipRun) {
    Write-Header "Lancement de l'application..."
    
    $launchResult = & $AdbPath shell am start -n $AppPackage/.MainActivity 2>&1
    if ($launchResult -match "Starting") {
        Write-Success "Application lancée"
    } else {
        Write-ErrorMsg "Échec du lancement de l'application"
        Write-ErrorMsg $launchResult
    }
}

# Étape 7: Affichage des logs
if ($Logcat) {
    Write-Header "Affichage des logs (logcat) - Appuyez sur Ctrl+C pour arrêter..."
    Write-Info "Filtre: $AppPackage"
    
    & $AdbPath shell logcat -s $AppPackage.replace(".", "/") 2>&1
    
    # Si logcat est arrêté, afficher un message
    Write-Info "Logcat arrêté"
}

# Résumé
Write-Header ""
Write-Header "Installation terminée!"
Write-Info ""
Write-Info "Actions manuelles possibles si nécessaire:"
Write-Info "  - Activer le filtrage d'appels dans les paramètres du téléphone"
Write-Info "  - Activer l'accès aux notifications pour SHIELD"
Write-Info "  - Autoriser l'installation de sources inconnues pour les mises à jour"
Write-Info ""
Write-Info "Pour tester les notifications SMS (debug uniquement):"
Write-Info "  $AdbPath shell cmd notification post -S SHIELD -t 'Test SMS' -p 'com.android.mms' -b 'Code 1234'"
Write-Info ""
