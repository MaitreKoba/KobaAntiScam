# SHIELD - Anti-Scam pour Android

/**
 * FICHIER : README.md | CHEMIN : D:/Kobayashi/Developpement/shield/README.md
 * LIGNES : ~150 | RÔLE : Documentation principale | DATE : 2026-10-14
 * VERSION : 1.0.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi
 * STATUT : STABLE | DÉPENDANCES : aucun | LIEN : README.md
 * NOTES : Documentation utilisateur et technique
 */

## Description

SHIELD est une application Android de filtrage d'appels et SMS conçue pour protéger contre le démarchage téléphonique et les arnaques. Elle implémente un système de règles avancé pour bloquer, silencer ou rediriger les appels indésirables.

## Fonctionnalités

### 📞 Filtrage d'appels
- **Blocage des numéros ARCEP**: Rejette automatiquement les appels des préfixes de démarchage français
- **Allowlist**: Autorise les numéros de contacts et les numéros explicitement autorisés
- **Détection des rappels**: Autorise un numéro inconnu s'il rappelle dans les 3 minutes
- **Compteur anti-rotation**: Bloque les tranches de 8 chiffres qui dépassent un seuil configurable
- **Messagerie vocale**: Redirige les appels indésirables vers la messagerie

### 📱 Gestion des SMS
- **Notification Listener**: Intercepte et annule les notifications SMS des expéditeurs bloqués
- **Détection de liens**: Identifie les SMS contenant des liens potentiellement dangereux

### 📊 Statistiques et Journal
- **Journal des événements**: Historique complet des appels et SMS traités
- **Statistiques**: Compteurs d'appels autorisés, bloqués, silencieux, SMS filtrés
- **Export**: Possibilité d'exporter les logs

### ⚙️ Configuration
- **Activation/désactivation du système**: Désactivez temporairement le filtrage
- **Gestion de l'allowlist**: Ajoutez/supprimez des numéros via l'interface ou deep link
- **Seuils configurables**: Ajustez le seuil anti-rotation
- **Deep link**: `kobashield://allow?num=+33123456789` pour ajouter un numéro

### 🔄 Mise à jour
- **Vérification automatique**: Vérifie les mises à jour via GitHub Releases
- **Installation manuelle**: Télécharge et installe les nouvelles versions

## Prérequis

- **Android 9+** (API 28+, mais recommandé API 29+ pour le filtrage d'appels)
- **Permissions**: 
  - `READ_PHONE_STATE`
  - `READ_CALL_LOG`
  - `RECEIVE_SMS`
  - `READ_SMS`
  - `BIND_CALL_SCREENING_SERVICE` (pour le filtrage d'appels)
  - `BIND_NOTIFICATION_LISTENER_SERVICE` (pour l'interception SMS)
  - `POST_NOTIFICATIONS` (Android 13+)
  - `REQUEST_INSTALL_PACKAGES` (pour les mises à jour)
  - `READ_CONTACTS` (pour détecter les contacts)

- **Rôles système** (Android 10+):
  - Rôle `CALL_SCREENING` pour le filtrage d'appels
  - Accès aux notifications pour l'interception SMS

## Installation

1. **Télécharger l'APK**: Récupérez le dernier APK depuis les releases GitHub
2. **Installer**: Activez "Sources inconnues" et installez l'APK
3. **Configurer**:
   - Activez le débogage USB (pour le développement)
   - Attribuez les permissions via `adb shell pm grant`
   - Activez les rôles dans les paramètres du téléphone
4. **Lancer**: Ouvrez l'application et configurez vos règles

## Configuration du téléphone (Honor MagicOS)

### Filtrage d'appels
1. Allez dans **Paramètres > Apps > Accès spécial > Filtrage des appels**
2. Sélectionnez **SHIELD**
3. Activez le service

### Accès aux notifications
1. Allez dans **Paramètres > Apps > Accès spécial > Accès aux notifications**
2. Trouvez **SHIELD** dans la liste
3. Activez l'accès

### Optimisation de la batterie
1. Allez dans **Paramètres > Batterie > Optimisation de la batterie**
2. Trouvez **SHIELD**
3. Désactivez l'optimisation

## Deep Link

Pour ajouter un numéro à l'allowlist via un lien:
```
kobashield://allow?num=+33123456789
```

Cela ouvrira l'application et proposera d'ajouter le numéro à l'allowlist.

## Ordre de priorité des règles d'appel

Les règles sont évaluées dans cet ordre:

1. **Système désactivé** → Tout sonne
2. **Contact** → Sonne
3. **Allowlist** (numéro ou préfixe) → Sonne
4. **Numéro déjà appelé** par l'utilisateur → Sonne
5. **Préfixes ARCEP** → **Rejet**
6. **Même inconnu qui rappelle sous 3 minutes** → Sonne
7. **Autres inconnus et numéros masqués** → Silence + messagerie

## Préfixes ARCEP bloqués

Les préfixes suivants sont automatiquement rejetés:
- 162, 163 (Numéros surtaxés - services audiotex)
- 270, 271 (Numéros surtaxés)
- 377, 378 (Numéros surtaxés)
- 424, 425 (Numéros surtaxés)
- 568, 569 (Numéros surtaxés)
- 948, 949 (Numéros surtaxés)

## Compteur anti-rotation

Le système détecte les numéros qui tournent leurs chiffres pour contourner le blocage:
- Chaque tranche de **8 chiffres** est comptée
- Si une tranche dépasse **5 appels en 24h** (configurable), elle est bloquée
- Le compteur se réinitialise après 24h

## Structure du projet

```
shield/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/fr/kobayashi/shield/
│   │   │   │   ├── model/          # Modèles et bases de données
│   │   │   │   │   ├── Rule.kt       # Règles de filtrage
│   │   │   │   │   ├── Rules.kt      # Moteur de règles
│   │   │   │   │   ├── Slice.kt      # Anti-rotation
│   │   │   │   │   └── ...
│   │   │   │   ├── service/        # Services Android
│   │   │   │   │   ├── CallFilterService.kt
│   │   │   │   │   └── SmsNotificationListener.kt
│   │   │   │   ├── receiver/       # Broadcast Receivers
│   │   │   │   │   └── SmsReceiver.kt
│   │   │   │   └── ...            # Activités
│   │   └── test/                # Tests unitaires
│   │       └── java/fr/kobayashi/shield/model/
│   │           └── RulesTest.kt
├── scripts/
│   ├── dev.ps1              # Script PowerShell
│   └── dev.sh               # Script Bash
├── .claude/
│   └── settings.json        # Configuration Claude Code
├── CLAUDE.md
├── MEGA_PROMPT.md
├── STATE.md
├── README.md
└── CHANGELOG.md
```

## Développement

### Prérequis
- JDK 17
- Android SDK (API 35)
- Gradle 8.9
- adb (Android Debug Bridge)

### Build
```bash
# Windows
cd D:/Kobayashi/Developpement/shield
set JAVA_HOME=D:/Kobayashi/Developpement/Android/jdk17/jdk-17.0.20+8
./gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

### Installation
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Script de développement
```bash
# PowerShell
./scripts/dev.ps1

# Bash
./scripts/dev.sh
```

Le script:
- Compile l'application
- Installe l'APK
- Attribue les permissions
- Tente d'attribuer les rôles système
- Lance l'application
- Affiche les logs

### Tests
```bash
./gradlew testDebugUnitTest
```

## Contribution

1. Fork le dépôt
2. Crée une branche (`git checkout -b feature/nouvelle-fonctionnalité`)
3. Commit tes changements (`git commit -m 'Ajout: nouvelle fonctionnalité'`)
4. Push vers la branche (`git push origin feature/nouvelle-fonctionnalité`)
5. Ouvre une Pull Request

## Licence

Ce projet est sous licence propriétaire - Maitre Kobayashi. Toute utilisation commerciale nécessite une autorisation explicite.

## Confidentialité

- **Aucune télémétrie**: Aucune donnée n'est envoyée à des serveurs externes
- **Données locales**: Les règles, logs et configurations restent sur l'appareil
- **Pas de collection de données**: Aucune information personnelle n'est collectée

## Remerciements

- Android et le SDK Android
- Room pour la persistance
- Retrofit pour les appels API
- Kotlin pour le développement moderne

---

**Auteur**: Maitre Kobayashi  
**Version**: 1.0.0  
**Date**: 2026-10-14
