# CHANGELOG - Kobayashi AntiScan

/**
 * FICHIER : CHANGELOG.md | CHEMIN : D:/Kobayashi/Developpement/shield/CHANGELOG.md
 * LIGNES : ~100 | RÔLE : Historique des versions | DATE : 2026-10-14
 * VERSION : 1.0.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi
 * STATUT : STABLE | DÉPENDANCES : aucun | LIEN : CHANGELOG.md
 * NOTES : Suivi des changements par version (SemVer strict)
 */

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr-FR/1.0.0/).

## [1.6.0] - 2026-10-15
### Ajouté
- Interface refaite avec la charte de kobacraft.net : palette crème, pêche, orange et rouge, cartes en verre dépoli, polices Fredoka et Nunito embarquées, mode sombre repris du site.
- Écran Apparence (tuile et bouton 🎨 de l'accueil) : mode clair, sombre ou système ; 7 couleurs principales ; taille du texte ; coins carrés, doux, arrondis ou bulles ; cartes verre, pleines ou teintées ; fond dégradé ou uni ; police Kobacraft ou système ; animations ; accueil en grille ou en liste ; carte d'état des services affichée ou masquée ; retour au thème Kobacraft.
- Accueil : carte d'état en dégradé avec interrupteur du système, tuiles (Règles, Journal, Statistiques, Réglages, Apparence, Mise à jour), état réel des services (rôle de filtrage, accès aux notifications, optimisation batterie).
- `BaseActivity` : toutes les activités appliquent l'apparence choisie et se recréent quand elle change.

### Modifié
- Couleurs codées en dur retirées de `activity_allow.xml`, `activity_stats.xml`, `item_log.xml`, `item_rule.xml`.
- Détection de l'accès aux notifications via `NotificationManagerCompat` (l'ancienne réflexion comparait un nom de classe au lieu du paquet).

### Connu
- `RulesTest.kt` ne compile pas (antérieur à 1.6.0) : `getSlice` privé dans `Rules`, `prefix123456` inexistant.

## [Unreleased]
### ✨ Modifié
- Application renommée de **SHIELD AntiScam** à **Kobayashi AntiScan**
- Nom affiché mis à jour dans toutes les activités et ressources

## [1.0.0] - 2026-10-14

### ✨ Ajouté

#### Architecture de base
- **Projet Android Kotlin** avec Gradle et Room
- **Structure de dossiers** complète selon les standards Android
- **Configuration build.gradle** avec dépendances (Room, Retrofit, Material)

#### Moteur de règles
- **Rules.kt**: Implémentation complète de la logique de filtrage
  - Normalisation des numéros (`normalize`, `normalizePrefix`)
  - Détection des préfixes ARCEP (162, 163, 270, 271, 377, 378, 424, 425, 568, 569, 948, 949)
  - Détection des contacts via ContentProvider
  - Vérification de l'allowlist
  - Vérification des appels sortants (journal)
  - Détection des rappels sous 3 minutes
  - Compteur anti-rotation par tranche de 8 chiffres

#### Services Android
- **CallFilterService**: `CallScreeningService` pour le filtrage d'appels
  - Implémente `onScreenCall` avec `respondToCall`
  - Gestion des 4 actions: ALLOW, BLOCK, SILENCE, VOICEMAIL
  - Intégration avec le moteur Rules
  - Journalisation dans Room

- **SmsNotificationListener**: `NotificationListenerService` pour intercepter les notifications SMS
  - Surveillance des paquets SMS (com.android.mms, Messages, etc.)
  - Extraction du numéro depuis les notifications
  - Application des règles et annulation des notifications

- **SmsReceiver**: `BroadcastReceiver` pour `SMS_RECEIVED`
  - Interception des SMS entrants
  - Journalisation et filtrage basé sur les règles

#### Base de données (Room)
- **Rule.kt**: Modèle de règle avec pattern, action, type, statut
- **RuleDao.kt**: Accès aux règles (CRUD, requêtes par type)
- **LogEntry.kt**: Modèle de log avec type, numéro, action, message
- **LogDao.kt**: Accès aux logs
- **RuleDatabase.kt**: Configuration Room avec initialisation
- **Slice.kt**: Module anti-rotation avec compteurs par tranche

#### Interface utilisateur
- **MainActivity**: Écran principal avec boutons vers:
  - Règles
  - Journal
  - Paramètres
  - Mise à jour
  - Statistiques

- **RulesActivity**: Liste des règles avec:
  - Ajout/édition/suppression
  - Filtrage par type (CALL, SMS)
  - Activation/désactivation

- **LogsActivity**: Journal des événements avec:
  - Liste des appels et SMS
  - Filtrage par type
  - Details de chaque entrée

- **SettingsActivity**: Configuration avec:
  - Activation/désactivation du système
  - Boutons pour attribuer les rôles système
  - Boutons pour désactiver l'optimisation batterie
  - Accès à la mise à jour

- **UpdateActivity**: Mise à jour via GitHub Releases
  - Vérification de la dernière version
  - Téléchargement de l'APK
  - Installation via FileProvider

- **AllowActivity**: Gestion du deep link `kobashield://allow?num=...`
  - Extraction du numéro depuis l'URL
  - Ajout à l'allowlist
  - Confirmation visuelle

- **StatsActivity**: Statistiques avec:
  - Compteurs par type (appels autorisés, bloqués, silencieux, messagerie)
  - Compteurs SMS
  - Total général

#### Deep Link
- **kobashield://allow?num=+33123456789**: Ouvre AllowActivity pour ajouter un numéro
  - Intent-filter dans AndroidManifest.xml
  - Gestion de l'URI et extraction du numéro

#### Mise à jour GitHub
- **GitHubService.kt**: API Retrofit vers `api.github.com/repos/{owner}/{repo}/releases/latest`
- **UpdateActivity**:
  - Vérification de la version
  - Téléchargement de l'APK
  - Installation via Intent
- **FileProvider**: Pour partager l'APK téléchargé

#### Tests unitaires
- **RulesTest.kt**: Tests JVM pour le moteur de règles
  - `testNormalizeFrenchNumber`: Normalisation des numéros français
  - `testNormalizeInternationalNumber`: Normalisation des numéros internationaux
  - `testNormalizeEmpty`: Gestion des entrées vides
  - `testNormalizePrefix`: Normalisation des préfixes
  - `testIsArcpPrefix`: Détection des préfixes ARCEP
  - `testIsRecallWithinWindow`: Détection des rappels sous 3 minutes
  - `testSlice`: Extraction de tranche de 8 chiffres
  - `testSliceCounter`: Logique du compteur anti-rotation

#### Scripts de développement
- **dev.ps1**: Script PowerShell pour Windows
  - Build, installation, permissions, rôles, lancement, logs
- **dev.sh**: Script Bash pour Git Bash
  - Même fonctionnalités que dev.ps1

#### Configuration
- **AndroidManifest.xml**:
  - Déclaration de tous les services, receivers, activités
  - Intent-filters pour le deep link
  - Permissions nécessaires
  - FileProvider pour les mises à jour

- **.claude/settings.json**: Configuration pour Claude Code
  - Commandes autorisées
  - Commandes bloquées
  - Limites d'itérations

#### Documentation
- **MEGA_PROMPT.md**: Spécifications détaillées du projet
- **CLAUDE.md**: Instructions pour Claude Code
- **STATE.md**: État courant du développement
- **README.md**: Documentation utilisateur
- **CHANGELOG.md**: Historique des versions

### 📝 Notes de version

- **Première version stable**
- **Compatibilité**: Android 9+ (API 28+), recommandé Android 10+ (API 29+)
- **Package**: fr.kobayashi.shield (debug: fr.kobayashi.shield.debug)
- **Build**: JDK 17, Gradle 8.9, Android SDK 35, AGP 8.7.0

---

## [Non publié]

### À venir

- Migration vers le package `net.kobacraft.shield` (conforme au MEGA_PROMPT)
- Intégration avec vm_filter.py (pipeline vocal)
- Notifications ntfy pour les transcriptions
- Option payante avec Asterisk et numéro OVH
- Menu DTMF (si compatible avec la messagerie)
- Tests sur appareil réel (Honor X7)
- Publication sur GitHub Releases

---

## Template pour les futures versions

```
## [X.Y.Z] - YYYY-MM-DD

### ✨ Ajouté
- Nouvelle fonctionnalité 1
- Nouvelle fonctionnalité 2

### 🔧 Changé
- Modification 1
- Modification 2

### 🐛 Corrigé
- Correction de bug 1
- Correction de bug 2

### 🗑️ Supprimé
- Fonctionnalité supprimée 1
```

---

**Format de version**: SemVer (MAJOR.MINOR.PATCH)  
**Auteur**: Maitre Kobayashi  
**Dernière mise à jour**: 2026-10-14
