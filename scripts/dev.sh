#!/bin/bash
# SHIELD Development Script
# Bash script pour compiler, installer et tester l'app sur le téléphone
# Nécessite: JDK 17, Android SDK, adb, téléphone branché avec débogage USB activé

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_PACKAGE="fr.kobayashi.shield.debug"  # debug build suffix
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
JAVA_HOME="D:/Kobayashi/Developpement/Android/jdk17/jdk-17.0.20+8"
ADB_PATH="D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe"

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

# Fonction pour afficher les messages
header() {
    echo -e "${CYAN}\n$1${NC}"
}

success() {
    echo -e "${GREEN}[OK] $1${NC}"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

info() {
    echo -e "${GRAY}[INFO] $1${NC}"
}

# Vérification de l'environnement
header "Vérification de l'environnement..."

if [ ! -d "$JAVA_HOME" ]; then
    error "JDK 17 non trouvé: $JAVA_HOME"
    exit 1
fi

if [ ! -f "$ADB_PATH" ]; then
    error "adb non trouvé: $ADB_PATH"
    exit 1
fi

export JAVA_HOME="$JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

# Étape 1: Build
if [ "$SKIP_BUILD" != "true" ]; then
    header "Compilation de l'application..."
    
    cd "$PROJECT_DIR"
    if ! ./gradlew.bat assembleDebug; then
        error "La compilation a échoué"
        exit 1
    fi
    
    if [ ! -f "$APK_PATH" ]; then
        error "APK non généré: $APK_PATH"
        exit 1
    fi
    
    success "Compilation réussie"
fi

# Étape 2: Vérification du périphérique
header "Vérification du périphérique Android..."

DEVICES=$("$ADB_PATH" devices 2>&1)
if echo "$DEVICES" | grep -q "List of devices attached"; then
    DEVICE_COUNT=$(echo "$DEVICES" | grep -v "List of devices attached" | grep -v "^$" | wc -l)
    
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        error "Aucun périphérique Android détecté. Vérifiez que:"
        error "  1. Le téléphone est bien branché en USB"
        error "  2. Le débogage USB est activé sur le téléphone"
        error "  3. Vous avez accepté l'invite de débogage USB sur le téléphone"
        error "  4. Le câble USB permet le transfert de données"
        exit 1
    fi
    
    success "Périphérique détecté"
else
    error "Erreur lors de la détection du périphérique"
    error "$DEVICES"
    exit 1
fi

# Étape 3: Installation de l'APK
if [ "$SKIP_INSTALL" != "true" ]; then
    header "Installation de l'APK..."
    
    if "$ADB_PATH" install -r -t "$APK_PATH"; then
        success "APK installé avec succès"
    else
        error "Échec de l'installation. Essayez de désinstaller manuellement depuis le téléphone."
        exit 1
    fi
fi

# Étape 4: Attribution des permissions
if [ "$SKIP_GRANT" != "true" ]; then
    header "Attribution des permissions..."
    
    PERMISSIONS=(
        "android.permission.READ_PHONE_STATE"
        "android.permission.READ_CALL_LOG"
        "android.permission.RECEIVE_SMS"
        "android.permission.READ_SMS"
        "android.permission.POST_NOTIFICATIONS"
        "android.permission.REQUEST_INSTALL_PACKAGES"
    )
    
    for permission in "${PERMISSIONS[@]}"; do
        if "$ADB_PATH" shell pm grant "$APP_PACKAGE" "$permission" > /dev/null 2>&1; then
            success "Permission $permission attribuée"
        else
            error "Échec de l'attribution de $permission"
        fi
    done
    
    # Désactiver l'optimisation de la batterie
    info "Désactivation de l'optimisation de la batterie..."
    "$ADB_PATH" shell dumpsys deviceidle force-idle > /dev/null 2>&1
fi

# Étape 5: Attribution des rôles
header "Attribution des rôles système..."

# Rôle de filtrage d'appels
ROLE_RESULT=$("$ADB_PATH" shell cmd role add-role-holder fr.kobayashi.shield android.app.role.CALL_SCREENING 2>&1)
if echo "$ROLE_RESULT" | grep -qi "Success"; then
    success "Rôle CALL_SCREENING attribué"
else
    error "Échec de l'attribution du rôle CALL_SCREENING"
    error "Action manuelle nécessaire: Allez dans Paramètres > Apps > Accès spécial > Filtrage des appels > SHIELD"
fi

# Accès aux notifications
NOTIFICATION_RESULT=$("$ADB_PATH" shell cmd notification allow_listener fr.kobayashi.shield 2>&1)
if echo "$NOTIFICATION_RESULT" | grep -qi "Success"; then
    success "Accès aux notifications attribué"
else
    error "Échec de l'attribution de l'accès aux notifications"
    error "Action manuelle nécessaire: Allez dans Paramètres > Apps > Accès spécial > Accès aux notifications > SHIELD"
fi

# Étape 6: Lancement de l'application
if [ "$SKIP_RUN" != "true" ]; then
    header "Lancement de l'application..."
    
    LAUNCH_RESULT=$("$ADB_PATH" shell am start -n "$APP_PACKAGE/.MainActivity" 2>&1)
    if echo "$LAUNCH_RESULT" | grep -q "Starting"; then
        success "Application lancée"
    else
        error "Échec du lancement de l'application"
        error "$LAUNCH_RESULT"
    fi
fi

# Étape 7: Affichage des logs
if [ "$LOGCAT" = "true" ]; then
    header "Affichage des logs (logcat) - Appuyez sur Ctrl+C pour arrêter..."
    info "Filtre: $APP_PACKAGE"
    
    "$ADB_PATH" shell logcat -s "${APP_PACKAGE//./\/}"
    
    info "Logcat arrêté"
fi

# Résumé
header ""
header "Installation terminée!"
info ""
info "Actions manuelles possibles si nécessaire:"
info "  - Activer le filtrage d'appels dans les paramètres du téléphone"
info "  - Activer l'accès aux notifications pour SHIELD"
info "  - Autoriser l'installation de sources inconnues pour les mises à jour"
info ""
info "Pour tester les notifications SMS (debug uniquement):"
info "  $ADB_PATH shell cmd notification post -S SHIELD -t 'Test SMS' -p 'com.android.mms' -b 'Code 1234'"
