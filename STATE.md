# STATE — Kobayashi AntiScam

/**
 * FICHIER : STATE.md | CHEMIN : D:/Kobayashi/Developpement/shield/STATE.md
 * LIGNES : ~250 | RÔLE : État du développement | DATE : 2026-10-14
 * VERSION : 1.0.0 | ORIGINE : Mise à jour | AUTEUR : Maitre Kobayashi (via agent)
 * STATUT : EN COURS | DÉPENDANCES : MEGA_PROMPT.md, AndroidManifest.xml, build.gradle.kts
 * LIEN : STATE.md | NOTES : État courant après renommage en Kobayashi AntiScam
 */

Dernière mise à jour : 2026-10-16

## 1.8.0 (2026-10-16) : étiquette Spam / Sûr / Inconnu et numéro toujours visible
- [x] JournalVue.etiquette (déduite du type d'entrée, pas de migration Room), item_log.xml (tvTag)
- [x] Émulateur : contact (Sûr + numéro), ARCEP (Spam), inconnu (Inconnu, « Pas dans tes contacts »)
- [ ] Passerelle OVH Découverte : renvois Free conditionnels **67/**61/**62 vers le numéro OVH, annonce MP3, messages par email, vérifier que le numéro d'origine est transmis
- [ ] Écran Contacts avec rôle par contact (Sonne, Messagerie, Bloqué), diagnostic contacts sur le Honor

## 1.7.0 (2026-10-16) : filtrage fiable, historique avec raisons, accueil sans emojis
- [x] Decision.kt (ordre des règles, raison par appel), CallFilterService réécrit, MP3 plus joué
- [x] Historique et accueil refaits, icônes Material vectorielles (res/drawable/ic_*.xml)
- [x] RulesTest.kt réparé : 9 tests OK
- [x] 7 scénarios validés sur émulateur Android 13 (AVD shieldtest, `adb emu gsm call`) : contact, contact au format +33, déjà appelé, ARCEP, inconnu, rappel sous 3 min, +225
- [x] Release v1.7.0 publiée
- [ ] Sur le Honor : mise à jour depuis l'app sans USB, vrai appel d'un inconnu, annonce MP3 envoyée dans l'espace abonné Free

## 1.6.1 (2026-10-16) : correctif de la mise à jour intégrée
- [x] Cause : plantage `FileProvider` (racines en double dans file_paths.xml) à la fin du téléchargement. La release v1.5.0 contenait en plus un APK compilé en 1.4.3 (versionCode 6).
- [x] UpdateActivity réécrite : dossier privé, contrôle paquet et versionCode, SemVer, RECEIVER_EXPORTED
- [x] Honor X7 passé en 1.6.0 puis 1.6.1 par adb
- Règle de release : vérifier `aapt dump badging` de l'APK (versionName = tag) avant `gh release create`

## 🎨 1.6.0 (2026-10-15) : refonte UI kobacraft.net
- [x] Thème Kobacraft clair et sombre, polices Fredoka et Nunito, fond dégradé
- [x] Écran Apparence (10 réglages) + BaseActivity
- [x] Accueil refait (carte d'état, tuiles, état des services)
- [x] `assembleDebug` OK (APK 1.6.0, versionCode 8)
- [ ] Installation et vérification visuelle sur le Honor X7 (non branché le 15/10)
- [x] `RulesTest.kt` réparé en 1.7.0


## 📊 Statut Global

- [x] Reconnaissance machine + chemins choisis
- [x] Paquet de reprise écrit (MEGA_PROMPT.md, CLAUDE.md, scripts, settings)
- [x] Chaîne de build installée (JDK 17, SDK Android 35, Gradle 8.9)
- [x] Squelette projet Android créé
- [x] **Filtrage appels/SMS implémenté** (selon MEGA_PROMPT)
- [x] **Deep link kobashield://allow?num=** implémenté
- [x] **Compteur anti-rotation** (slice de 8 chiffres) implémenté
- [x] **Écran de statistiques** implémenté
- [x] **Aide Honor** implémentée (boutons dans SettingsActivity)
- [x] **Tests unitaires JVM** créés (RulesTest.kt)
- [x] **Documentation** (README.md, CHANGELOG.md)
- [x] Build + compilation réussie (BUILD SUCCESSFUL)
- [x] **Détection USB fonctionnelle** (Honor X7: A78H9K2411623626)
- [x] **Dépôt GitHub créé** (MaitreKoba/KobaAntiScam - privé)
- [x] **Code poussé vers GitHub** (69 fichiers, commit initial)
- [x] **APK installé sur le téléphone** (fr.kobayashi.shield.debug, PID 23598)
- [x] **Application renommée** en Kobayashi AntiScam
- [x] **Rôle CALL_SCREENING attribué** (via adb)
- [x] **Accès aux notifications attribué** (via adb)
- [x] **Optimisation batterie désactivée** (via adb)
- [ ] 3 scénarios de test validés sur Honor X7

## 🎯 Prochaines étapes

### Validation des scénarios de test
Trois scénarios à valider selon MEGA_PROMPT § 4 :

#### Scénario 1: Numéro dans l'allowlist → Sonne
- **Test**: Appeler depuis un numéro dans l'allowlist
- **Attendu**: L'appel sonne normalement
- **Commande adb**: Simuler avec notification (adb ne simule pas un vrai appel)

#### Scénario 2: Numéro ARCEP → Rejet
- **Test**: Appeler depuis +33162xxxxxx (préfixe ARCEP)
- **Attendu**: Appel rejeté automatiquement
- **Vérification**: `adb logcat -s Shield` doit montrer REJECT

#### Scénario 3: Numéro inconnu → Silence + messagerie
- **Test**: Appeler depuis un numéro inconnu non dans l'allowlist
- **Attendu**: Appel silencieux, redirigé vers messagerie Free
- **Vérification**: `adb logcat -s Shield` doit montrer SILENCE

### Commandes utiles pour les tests
```bash
# Lancer l'app
adb shell am start -n fr.kobayashi.shield.debug/fr.kobayashi.shield.MainActivity

# Voir les logs de l'app
adb logcat -s SHIELD

# Vérifier les permissions
adb shell dumpsys package fr.kobayashi.shield.debug | grep -i permission

# Vérifier les rôles
adb shell dumpsys role | grep -A2 CALL_SCREENING

# Envoyer une fausse notification SMS (pour tester SmsNotificationListener)
adb shell cmd notification post -t "36105" shield_test "Code 1234"
```

### Actions manuelles requises
- [ ] Accepter un appel entrant réel depuis un autre téléphone (adb ne simule pas)
- [ ] Configurer la messagerie Free Mobile
- [ ] Enregistrer l'accueil vocal
- [ ] Créer les clés API (Groq, ntfy) pour le pipeline vocal

## ✅ Fait

### Connexion USB
- **Téléphone**: Honor X7 (CMA-LX1) détecté avec succès
- **Device ID**: A78H9K2411623626
- **Modèle**: HNCMA-Q
- **Statut**: device (prêt pour adb)

### Dépôt GitHub
- **URL**: https://github.com/MaitreKoba/KobaAntiScam
- **Type**: Privé
- **Branche**: main
- **Commit initial**: 477cac7 - "Initial commit - KobaAntiScam Android app"
- **Fichiers poussés**: 69 fichiers
- **Token utilisé**: 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 (à conserver pour les pushes futurs)
- **URL SSH**: git@github.com:MaitreKoba/KobaAntiScam.git
- **URL HTTPS**: https://github.com/MaitreKoba/KobaAntiScam.git

### Installation sur le téléphone
- **Package**: fr.kobayashi.shield.debug
- **APK**: app/build/outputs/apk/debug/app-debug.apk (16 Mo)
- **Process ID**: 23598 (en cours d'exécution)
- **Device**: Honor X7 (CMA-LX1, A78H9K2411623626)

### Permissions attribuées via adb
- ✅ android.permission.READ_PHONE_STATE
- ✅ android.permission.READ_CALL_LOG
- ✅ android.permission.RECEIVE_SMS
- ✅ android.permission.READ_SMS
- ✅ android.permission.POST_NOTIFICATIONS
- ✅ android.permission.REQUEST_INSTALL_PACKAGES

### Rôles système attribués
- ✅ android.app.role.CALL_SCREENING → fr.kobayashi.shield.debug
- ✅ NotificationListener → fr.kobayashi.shield.debug.SmsNotificationListener

### Optimisation batterie
- ✅ Désactivée pour fr.kobayashi.shield.debug

### Chaîne de build
- **JDK 17 Temurin**: `D:/Kobayashi/Developpement/Android/jdk17/jdk-17.0.20+8`
- **Android SDK**: `D:/Kobayashi/Developpement/Android/sdk`
  - Platforms: android-34, android-35
  - Platform-tools: adb, fastboot, etc.
  - Build-tools: 35.0.0
  - Cmdline-tools: latest
- **Gradle 8.9**: `D:/Kobayashi/Developpement/Android/gradle-8.9`

### Implémentation selon MEGA_PROMPT

#### 1. Deep Link ✅
- **Intent-filter** dans AndroidManifest.xml pour `kobashield://allow?num=...`
- **AllowActivity** créée pour gérer le deep link
- **Layout activity_allow.xml** créé
- **Logique**: Extraction du numéro, vérification, ajout à l'allowlist

#### 2. Compteur par tranche de 8 chiffres ✅
- **Slice.kt** créé avec:
  - Extraction de tranche de 8 chiffres (`getSliceFromNumber`)
  - Compteurs par tranche avec réinitialisation après 24h
  - Seuil configurable (`threshold`, par défaut 5)
  - Sauvegarde/gestion dans SharedPreferences
  - Méthodes: `isOverThresholdForSlice`, `isOverThresholdForNumber`, `incrementSlice`, `incrementNumber`
- **Intégration** dans Rules.kt pour l'anti-rotation

#### 3. Écran de statistiques ✅
- **StatsActivity** créée avec:
  - Affichage des compteurs par type (CALL_INCOMING, CALL_BLOCKED, CALL_SILENCED, CALL_VOICEMAIL, SMS_RECEIVED, SMS_BLOCKED, ERROR)
  - Calcul des totaux (appels, SMS)
  - StatsAdapter pour la liste
- **Layout activity_stats.xml** créé

#### 4. Aide Honor ✅
- **SettingsActivity** contient déjà:
  - Bouton "Activer le rôle" pour CALL_SCREENING (avec vérification API 29+)
  - Bouton "Activer l'accès" pour les notifications ( Intent vers ACTION_NOTIFICATION_LISTENER_SETTINGS)
  - Bouton "Désactiver" pour l'optimisation batterie (Intent vers ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
  - Vérification de l'état (isNotificationServiceEnabled, isBatteryOptimized)

#### 5. Tests unitaires JVM ✅
- **RulesTest.kt** créé avec 8 tests:
  - `testNormalizeFrenchNumber`: Normalisation des numéros français (06, +33, espaces, etc.)
  - `testNormalizeInternationalNumber`: Normalisation des numéros internationaux
  - `testNormalizeEmpty`: Gestion des entrées vides
  - `testNormalizePrefix`: Normalisation des préfixes
  - `testIsArcpPrefix`: Détection de tous les préfixes ARCEP (12 préfixes)
  - `testIsRecallWithinWindow`: Détection des rappels sous 3 minutes (180000 ms)
  - `testSlice`: Extraction de tranche de 8 chiffres
  - `testSliceCounter`: Logique du compteur anti-rotation

### Moteur de règles (Rules.kt) ✅
- **Normalisation**: `normalize()`, `normalizePrefix()`
- **Détection contacts**: `isContact(context, number)`
- **Allowlist**: `isInAllowlist(number, allowlist)`
- **Appels sortants**: `isCalledBefore(context, number)`
- **Préfixes ARCEP**: `isArcpPrefix(number)` avec liste complète
- **Rappel 3 min**: `isRecallWithinWindow(number, timestamps)`
- **Anti-rotation**: `incrementSliceCounter(number)` + vérification seuil
- **Ordre de priorité**: `getAction()` implémente exactement l'ordre du MEGA_PROMPT:
  1. Système désactivé → ALLOW
  2. Contact → ALLOW
  3. Allowlist → ALLOW
  4. Déjà appelé → ALLOW
  5. Préfixe ARCEP → BLOCK
  6. Rappel sous 3 min → ALLOW
  7. Autres → SILENCE_VOICEMAIL

### Services Android ✅

#### CallFilterService
- Étend `CallScreeningService`
- Implémente `onScreenCall(details: Call.Details)`
- Utilise `Rules.getAction()` pour la décision
- Utilise `respondToCall(details, response)`
- Gère 4 actions: ALLOW, BLOCK, SILENCE, VOICEMAIL
- Journalisation dans Room
- Cache des timestamps pour la détection de rappel

#### SmsNotificationListener
- Étend `NotificationListenerService`
- Surveille les paquets: com.android.mms, com.google.android.apps.messaging, com.android.messaging, com.honor.messaging, com.hihonor.messaging
- Extraction du numéro depuis les notifications
- Application des règles et annulation des notifications
- Journalisation

#### SmsReceiver
- `BroadcastReceiver` pour `android.provider.Telephony.SMS_RECEIVED`
- Interception des SMS entrants
- Vérification des règles
- `abortBroadcast()` pour les SMS bloqués
- Journalisation

### Base de données (Room) ✅

#### Modèles
- **Rule**: id, pattern, description, action, ruleType, isActive, createdAt
- **LogEntry**: id, timestamp, type, action, phoneNumber, message, details
- **LogType**: CALL_INCOMING, CALL_BLOCKED, CALL_SILENCED, CALL_VOICEMAIL, SMS_RECEIVED, SMS_BLOCKED, UPDATE_CHECK, ERROR
- **RuleType**: CALL, SMS
- **RuleAction**: BLOCK, SILENCE, VOICEMAIL, ALLOW

#### DAOs
- **RuleDao**: insert, update, delete, getAllRules(), getAllRulesList(), getRulesByType(), getActiveRulesByType(), getRuleById(), deleteById()
- **LogDao**: insert, delete, getAllLogs(), getAllLogsList(), getLogsByType(), clearAll(), count()

### Activités UI ✅
- **ShieldApp**: Application class, initialise Room
- **MainActivity**: Écran principal avec boutons vers toutes les fonctionnalités
- **RulesActivity**: Liste des règles avec adapter
- **LogsActivity**: Journal avec adapter
- **SettingsActivity**: Configuration avec vérification des permissions/roles
- **UpdateActivity**: Mise à jour via GitHub API
- **AllowActivity**: Deep link handler
- **StatsActivity**: Statistiques

### GitHub Update ✅
- **GitHubService**: Interface Retrofit pour `/repos/{owner}/{repo}/releases/latest`
- **UpdateActivity**: Vérification, téléchargement, installation
- **FileProvider**: Configuration dans AndroidManifest.xml
- **Permission**: REQUEST_INSTALL_PACKAGES

### Scripts ✅
- **scripts/dev.ps1**: PowerShell pour Windows
- **scripts/dev.sh**: Bash pour Git Bash
- Fonctionnalités: build, install, grant permissions, set roles, launch, logcat

### Configuration ✅
- **AndroidManifest.xml**:
  - Package: fr.kobayashi.shield
  - All permissions declared
  - Services, receivers, providers declared
  - Intent-filters for deep link
  - Debug suffix: .debug
- **build.gradle.kts**: compileSdk=35, minSdk=29, targetSdk=35
- **.claude/settings.json**: Commandes autorisées/bloquées
- **CLAUDE.md**: Instructions pour Claude Code
- **MEGA_PROMPT.md**: Spécifications complètes

### Documentation ✅
- **README.md**: Documentation utilisateur complète
- **CHANGELOG.md**: Historique des versions (SemVer)

## 📦 APK généré
- **Chemin**: `D:/Kobayashi/Developpement/shield/app/build/outputs/apk/debug/app-debug.apk`
- **Taille**: ~19,76 Mo
- **Package**: fr.kobayashi.shield.debug
- **Version**: 1.0.0
- **Build**: 2026-10-14
- **Statut**: ✅ Compilation réussie

## 🔄 En cours / Bloqué

### 📱 Tests sur appareil
- **Blocage**: adb ne détecte aucun appareil (`adb devices` retourne liste vide)
  - **Solution**: Attendre que l'utilisateur branche le Honor X7 avec débogage USB activé
  - **Instructions**:
    1. Brancher le téléphone en USB
    2. Activer "Options développeur" (7 taps sur "Numéro de build")
    3. Activer "Débogage USB" dans Options développeur
    4. Accepter l'invite RSA sur le téléphone
    5. Sélectionner "Transfert de fichiers" (MTP) ou PTP

### 🔍 Scénarios à valider
| # | Scénario | Méthode de test | Résultat attendu | Statut |
|---|----------|----------------|------------------|--------|
| 1 | Appel d'un contact | Appeler depuis un téléphone connu | Sonne normalement | ⏳ |
| 2 | Appel d'un numéro dans allowlist | Appeler depuis un numéro autorisé | Sonne normalement | ⏳ |
| 3 | Appel d'un numéro déjà appelé | Appeler après avoir appelé ce numéro | Sonne normalement | ⏳ |
| 4 | Appel d'un préfixe ARCEP | Appeler depuis +33162... | Rejeté | ⏳ |
| 5 | Rappel sous 3 min | Appeler, puis rappeler dans les 3 min | Sonne (2ème appel) | ⏳ |
| 6 | Appel d'un inconnu | Appeler depuis un numéro inconnu | Silence + messagerie | ⏳ |
| 7 | SMS d'un expéditeur bloqué | Notification factice via adb | Notification annulée | ⏳ |
| 8 | Deep link | Ouvrir kobashield://allow?num=+331234 | Ajout à allowlist | ⏳ |
| 9 | Mise à jour GitHub | Cliquer "Rechercher mise à jour" | Vérification + proposition | ⏳ |

### 🛠️ Configuration manuelle nécessaire
1. **Rôle CALL_SCREENING**: Paramètres > Apps > Accès spécial > Filtrage des appels > SHIELD
2. **Accès notifications**: Paramètres > Apps > Accès spécial > Accès aux notifications > SHIELD
3. **Optimisation batterie**: Paramètres > Batterie > Optimisation > SHIELD (désactiver)
4. **GitHub**: Configurer `OWNER`/`REPO` dans UpdateActivity

## 📝 Prochaines étapes

### 1️⃣ Immédiat (attendre utilisateur)
- [ ] Utilisateur branche le téléphone avec débogage USB activé
- [ ] Exécuter `./scripts/dev.ps1` pour installer l'APK
- [ ] Vérifier que l'app s'installe et se lance

### 2️⃣ Tests fonctionnels
- [ ] Valider les 9 scénarios de test ci-dessus
- [ ] Corriger les éventuels bugs de détection
- [ ] Ajuster les seuils si nécessaire

### 3️⃣ Migration package (optionnel)
- [ ] Migrer de `fr.kobayashi.shield` à `net.kobacraft.shield` (selon MEGA_PROMPT)
- [ ] Vérifier que tout fonctionne après la migration

### 4️⃣ Fonctionnalités futures
- [ ] Pipeline vocal (vm_filter.py) - hors scope Android
- [ ] Notifications ntfy - nécessite clés API
- [ ] Option payante (Asterisk, OVH) - à discuter
- [ ] Menu DTMF - dépend de la messagerie Free

### 5️⃣ Publication
- [ ] Créer dépôt GitHub (net.kobacraft/shield ou fr.kobayashi/shield)
- [ ] Configurer OWNER/REPO dans UpdateActivity
- [ ] Publier la release v1.0.0
- [ ] Créer les clés Groq/ntfy (hors dépôt)

## 🚨 Problèmes connus

### 1. Package name
- Actuellement: `fr.kobayashi.shield`
- Spécifié dans MEGA_PROMPT: `net.kobacraft.shield`
- **Impact**: Minime - peut être migré plus tard
- **Solution**: Renommer tous les packages et mettre à jour les références

### 2. Détection des contacts
- La méthode `isContact()` nécessite la permission `READ_CONTACTS`
- **Statut**: Permission déclarée dans AndroidManifest.xml
- **À tester**: Vérifier que la détection fonctionne sur Honor X7

### 3. Journal des appels
- `isCalledBefore()` lit le journal des appels Android
- **Permission**: `READ_CALL_LOG` déclarée
- **À tester**: Vérifier que la lecture fonctionne

### 4. Compteur Slice
- Actuellement en mémoire + SharedPreferences
- **Amélioration possible**: Persistance dans Room pour plus de fiabilité

### 5. SMS Receiver
- `abortBroadcast()` ne fonctionne que si l'app est l'app SMS par défaut
- **Solution**: Utiliser NotificationListenerService comme méthode principale
- **Statut**: Déjà implémenté, NotificationListener est la méthode par défaut

## 📞 Commandes utiles

```bash
# Build
cd D:/Kobayashi/Developpement/shield
set JAVA_HOME=D:/Kobayashi/Developpement/Android/jdk17/jdk-17.0.20+8
./gradlew.bat assembleDebug

# Installer
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk

# Vérifier appareil
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe devices

# Attribuer permissions
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell pm grant fr.kobayashi.shield.debug android.permission.READ_PHONE_STATE
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell pm grant fr.kobayashi.shield.debug android.permission.READ_CALL_LOG
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell pm grant fr.kobayashi.shield.debug android.permission.RECEIVE_SMS
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell pm grant fr.kobayashi.shield.debug android.permission.READ_SMS
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell pm grant fr.kobayashi.shield.debug android.permission.POST_NOTIFICATIONS
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell pm grant fr.kobayashi.shield.debug android.permission.READ_CONTACTS
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell pm grant fr.kobayashi.shield.debug android.permission.REQUEST_INSTALL_PACKAGES

# Attribuer rôles
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell cmd role add-role-holder fr.kobayashi.shield android.app.role.CALL_SCREENING
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell cmd notification allow_listener fr.kobayashi.shield

# Désactiver optimisation batterie
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell dumpsys deviceidle whitelist +fr.kobayashi.shield

# Lancer l'app
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell am start -n fr.kobayashi.shield.debug/.MainActivity

# Voir les logs
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe logcat -s fr/kobayashi/shield

# Tester notification SMS (debug)
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell cmd notification post -S SHIELD -t "Test SMS" -p "com.android.mms" -b "Code 1234"

# Tester deep link
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell am start -W -a android.intent.action.VIEW -d "kobashield://allow?num=+33123456789"
```

## 📊 Résumé technique

| Composant | Statut | Fichiers | Lignes | Couverture |
|-----------|--------|---------|--------|-----------|
| Moteur Rules | ✅ | Rules.kt | ~200 | Tests unitaires créés |
| CallFilterService | ✅ | CallFilterService.kt | ~140 | À tester |
| SmsNotificationListener | ✅ | SmsNotificationListener.kt | ~160 | À tester |
| SmsReceiver | ✅ | SmsReceiver.kt | ~90 | À tester |
| Slice (anti-rotation) | ✅ | Slice.kt | ~140 | Intégré |
| Tests unitaires | ✅ | RulesTest.kt | ~100 | 8 tests |
| Documentation | ✅ | README, CHANGELOG | ~140+60 | Complète |
| Scripts | ✅ | dev.ps1, dev.sh | ~70+50 | Prêts |

## ✅ Critères de "terminé" selon MEGA_PROMPT

Pour chaque fonctionnalité, le critère est:
- [ ] Compile ✅
- [ ] Interrupteur du module présent dans l'écran ✅
- [ ] Entrée dans le journal ✅
- [ ] Trois scénarios passés sur le Honor X7 ⏳ (en attente du téléphone)
- [ ] CHANGELOG.md et STATE.md à jour ✅

---

**Prochaine action attendue**: L'utilisateur branche le téléphone Honor X7 avec le débogage USB activé pour pouvoir exécuter `./scripts/dev.ps1` et installer l'APK.

**Dernière compilation**: 2026-10-14 - BUILD SUCCESSFUL ✅
