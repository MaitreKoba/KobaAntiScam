# SHIELD — MEGA PROMPT v2.0

Ce fichier est le prompt de reprise complet. Il décrit **ce qui a déjà été fait**,
**les chemins exacts**, **la chaîne de build**, **la spec de l'app** et **les règles de
sécurité**. Lis-le en entier avant d'agir, puis lis `STATE.md`.

---

## 0. RÈGLES ABSOLUES (sécurité — non négociables)

L'agent pilote un **vrai téléphone via adb**. Il ne doit JAMAIS :

- passer un appel sortant, composer un numéro, ni déclencher un appel
  (interdit : `am start` vers `tel:` / `android.intent.action.CALL`,
  `service call phone`, `cmd phone`, toute commande `sendsms`) ;
- envoyer un SMS vers un vrai numéro.

Autorisé uniquement :

- une **fausse notification locale** de test : `adb shell cmd notification post ...`
  (c'est un simple affichage à l'écran, aucun envoi réseau, aucun numéro contacté).

Actions interdites sans confirmation explicite de l'humain :
`adb uninstall`, `adb reboot`, `adb shell rm`, `pm clear`, `settings put`,
`git push --force`, `rm -rf`.

Un **appel entrant réel** ne peut pas être simulé par adb : il se teste uniquement
en demandant à l'humain d'appeler depuis un autre téléphone.

---

## 1. CONTEXTE MACHINE (état constaté le 2026-10-14)

- Windows 11, shell **PowerShell 5.1** (l'outil bash lance PowerShell).
- Présents : `winget`, `choco`, `node`/`npm`, `python` (C:\Python312), Git Bash
  (`C:\Program Files\Git\bin\bash.exe`).
- **ABSENTS** : JDK 17 (seulement Java 8 `C:\Program Files\Java\jre1.8.0_491`),
  SDK Android, `adb`, Android Studio, Gradle, 7-Zip.
- Aucune archive `shield-0.2.0.zip` trouvée sur C:, D:, E:, S:, ni dossier `shield/`.
- Le téléphone Honor **n'était pas détecté** en adb/MTP au branchement
  (seuls des disques USB WPD étaient listés : `KobaJeux`, `KobaCrucial`, `KobaPerso`).

### Ce qui a déjà été fait (reconnaissance uniquement — RIEN d'installé)

1. Recherche du projet `shield` sur tous les disques : introuvable (donc app à créer de zéro).
2. Paquets winget vérifiés et disponibles :
   - `EclipseAdoptium.Temurin.17.JDK` (17.0.20.101)
   - `Google.PlatformTools` (adb, 37.0.1)
   - `Google.AndroidStudio` (dispo mais lourd — non retenu)
3. URLs téléchargeables vérifiées (HTTP 200) :
   - Android cmdline-tools (le plus récent) :
     `https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip`
     (143 040 480 octets)
   - Autre cmdline-tools : `commandlinetools-win-11076708_latest.zip` (153 583 359 octets)
   - Gradle : `https://services.gradle.org/distributions/gradle-8.9-bin.zip` (136 114 148 octets)
     et `gradle-8.11.1-bin.zip`
4. Tentative `winget install EclipseAdoptium.Temurin.17.JDK` : **interrompue** (timeout,
   probablement invite UAC). Vérifié : **Temurin n'est PAS installé**. À refaire, en
   acceptant l'invite UAC à l'écran, ou en installant le JDK 17 en scope utilisateur.

---

## 2. CHEMINS CHOISIS (à respecter)

| Rôle | Chemin |
|------|--------|
| Projet | `D:\Kobayashi\Developpement\KobayashiAntiScam` |
| ANDROID_HOME (SDK) | `D:\Kobayashi\Developpement\Android\sdk` |
| Gradle | `D:\Kobayashi\Developpement\Android\gradle-8.9` |
| JDK 17 | `C:\Program Files\Eclipse Adoptium\jdk-17.x` (ou Temurin user-scope) |
| Package Android | `fr.kobayashi.shield` |

Chemins déjà créés :
`D:\Kobayashi\Developpement\KobayashiAntiScam`, `...\KobayashiAntiScam\scripts`, `...\KobayashiAntiScam\.claude`.

---

## 3. INSTALLATION DE LA CHAÎNE DE BUILD (PowerShell, admin pour le JDK/SDK)

```powershell
# 1) JDK 17 (accepter l'invite UAC)
winget install --id EclipseAdoptium.Temurin.17.JDK --exact --accept-package-agreements --accept-source-agreements
# si le scope machine échoue, tenter un JDK en scope utilisateur (archive .zip décompressée)

# 2) adb / platform-tools
winget install --id Google.PlatformTools --exact --accept-package-agreements --accept-source-agreements

# 3) Android cmdline-tools (SDK)
$dl = "D:\Kobayashi\Developpement\Android"
New-Item -ItemType Directory -Force -Path $dl, "$dl\sdk\cmdline-tools" | Out-Null
Invoke-WebRequest "https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip" -OutFile "$dl\clt.zip"
Expand-Archive "$dl\clt.zip" -DestinationPath "$dl\_clt_tmp" -Force
Move-Item "$dl\_clt_tmp\cmdline-tools" "$dl\sdk\cmdline-tools\latest" -Force
Remove-Item "$dl\_clt_tmp","$dl\clt.zip" -Recurse -Force

# 4) Gradle
Invoke-WebRequest "https://services.gradle.org/distributions/gradle-8.9-bin.zip" -OutFile "$dl\gradle.zip"
Expand-Archive "$dl\gradle.zip" -DestinationPath $dl -Force
Remove-Item "$dl\gradle.zip" -Force
```

Variables d'environnement (persistantes, scope utilisateur) :

```powershell
[Environment]::SetEnvironmentVariable("ANDROID_HOME","D:\Kobayashi\Developpement\Android\sdk","User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT","D:\Kobayashi\Developpement\Android\sdk","User")
$env:ANDROID_HOME = "D:\Kobayashi\Developpement\Android\sdk"
$env:JAVA_HOME   = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101"
$env:Path += ";$env:JAVA_HOME\bin;D:\Kobayashi\Developpement\Android\gradle-8.9\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin"
```

Licences + composants SDK :

```powershell
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --licenses   # tout accepter (y)
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Vérifications :

```powershell
java -version                     # doit afficher 17.x
adb version
adb devices                       # doit lister le Honor (voir section 7)
```

---

## 4. SPEC DE L'APPLICATION « SHIELD »

App Android **Kotlin**, Gradle, package `fr.kobayashi.shield`.

- `minSdk 29`, `targetSdk 35`, `compileSdk 35`.
- AGP 8.7.x + Gradle 8.9 (compatibles), Kotlin 2.x.
- UI simple : écran **Règles**, écran **Journal**, écran **Réglages**, bouton **Mise à jour**.
- Aucune dépendance lourde ; Material Components + AndroidX de base.

### 4.1 Filtrage des appels — `CallScreeningService`

- Service `fr.kobayashi.shield.filter.CallFilterService` étendant
  `android.telecom.CallScreeningService`, déclaré avec
  `android:permission="android.permission.BIND_SCREENING_SERVICE"` et l'intent-filter
  `android.telecom.CallScreeningService`.
- Dans `onScreenCall(Call.Details)` : évaluer le numéro contre les règles, puis
  répondre via `CallResponse` (`setDisallowCall`, `setRejectCall`, `setSilenceCall`,
  `setSkipCallLog`, `setSkipNotification`).
- Rôle requis : `android.app.role.CALL_SCREENING`
  (`RoleManager.ROLE_CALL_SCREENING`).

### 4.2 Garde SMS — `NotificationListenerService` (+ `RECEIVE_SMS`)

- Service `fr.kobayashi.shield.filter.SmsNotificationListener` étendant
  `NotificationListenerService`, déclaré avec
  `android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"`
  et l'intent-filter `android.service.notification.NotificationListenerService`.
- Objectif : détecter les notifications de la messagerie (paquet `com.android.mms`,
  `com.google.android.apps.messaging`, messagerie Honor) et les **annuler/silencier**
  quand le numéro/expéditeur correspond à une règle.
- Complément : `BroadcastReceiver` pour `android.provider.Telephony.SMS_RECEIVED`
  (permission runtime `RECEIVE_SMS`) avec `abortBroadcast()` — à ne considérer que si
  l'app est **app SMS par défaut** (restriction Android 4.4+). Sinon s'appuyer sur le
  listener de notifications.
- Permission runtime : `RECEIVE_SMS`, `READ_PHONE_STATE`, `POST_NOTIFICATIONS` (Android 13+).

### 4.3 Moteur de règles

- Règles stockées en local (Room ou JSON dans `filesDir`).
- Champs : `pattern` (numéro exact, préfixe, regex), `action` ∈ {silence, reject,
  voicemail/renvoyer], block}, `type` ∈ {appel, sms}, `actif`.
- 3 scénarios de test à valider (voir section 6) :
  1. numéro bloqué → appel silencieux/rejeté sans notification ;
  2. numéro autorisé → appel sonne normalement ;
  3. SMS d'un expéditeur bloqué → notification annulée.

---

## 5. BUILD + INSTALLATION (boucle adb)

Depuis `D:\Kobayashi\Developpement\KobayashiAntiScam` :

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Le script tout-en-un est `scripts\dev.ps1` (équivalent bash : `scripts\dev.sh`).
Il enchaîne : build, install, `pm grant` des permissions, attribution du rôle de
filtrage d'appels, activation du listener de notifications, exclusion de l'optimisation
batterie, lancement de l'app, puis affichage des erreurs `logcat`.

En cas d'échec de `cmd role add-role-holder` ou `cmd notification allow_listener`
(courant sur MagicOS), le script affiche l'erreur et renvoie vers les **boutons de
l'app** (écran Réglages) pour l'attribution manuelle.

Boucle autonome : compiler → installer → lire `logcat` → corriger. **Maximum 5 itérations
par erreur**, puis arrêt et rapport à l'humain.

---

## 6. TEST DE LA GARDE SMS (sans envoyer de vrai SMS)

Dans le **build debug uniquement**, prévoir un paquet de test accepté par :

```powershell
adb shell cmd notification post -t "SMS test" SHIELD_TEST "Code 1234"
# syntaxe selon version : vérifier `adb shell cmd notification help post`
```

L'app (en debug) doit réagir à cette notification comme à un SMS. **Ne jamais** utiliser
`am start` vers une app SMS ni une commande d'envoi réel.

---

## 7. BOUTON « RECHERCHER UNE MISE À JOUR » (GitHub Releases)

But : depuis l'app, vérifier s'il existe une version plus récente sur un dépôt GitHub et
proposer de l'installer.

- Endpoint : `https://api.github.com/repos/<OWNER>/<REPO>/releases/latest` (JSON).
- Comparer `tag_name` (ex. `v0.2.0`) à `BuildConfig.VERSION_NAME` / `VERSION_CODE`.
- Si plus récent : télécharger l'asset `.apk` (via `assets[].browser_download_url`)
  dans le cache, l'exposer avec un `FileProvider`, puis lancer
  `Intent(ACTION_VIEW, fileUri).setDataAndType(uri, "application/vnd.android.package-archive")`.
- Permission `REQUEST_INSTALL_PACKAGES` dans le manifeste ; l'utilisateur doit autoriser
  « installer des applications inconnues » **une fois**.
- **Android n'autorise PAS la mise à jour silencieuse** d'une app normale : le système
  affiche toujours l'écran d'installation et l'utilisateur confirme. Le bouton
  « cherche + propose », il n'installe pas seul.
- Token GitHub seulement côté publication des releases (ordinateur), jamais embarqué
  dans l'APK. Limite API non authentifiée : 60 req/h (suffisant pour un bouton manuel).
- Config à renseigner plus tard par l'humain : `OWNER`/`REPO` (il a un compte GitHub).

---

## 8. CE QUE L'IA NE PEUT PAS FAIRE SEULE (à demander à l'humain, précisément)

1. **Débogage USB** : sur le Honor, activer « Options développeur » → « Débogage USB »,
   brancher le câble, accepter l'invite « Autoriser le débogage USB ? ». MagicOS exige
   souvent aussi d'autoriser l'installation via USB (réglage spécifique).
2. **Appel entrant réel** : appeler depuis un autre téléphone (adb ne simule pas un appel
   cellulaire réel).
3. **Messagerie Free** : configurer la messagerie et enregistrer l'accueil vocal.
4. **Clés** : créer les clés Groq, le topic ntfy, etc. — jamais dans le dépôt.
5. **Repo GitHub** : fournir `OWNER`/`REPO` (et un token pour publier les releases).

---

## 9. PREMIER MESSAGE À COLLER DANS L'AGENT

> Lis `CLAUDE.md`, `MEGA_PROMPT.md` et `STATE.md`.
> Le Honor est branché en USB (ou : je viens de le brancher).
> Installe la chaîne de build (section 3), crée l'app décrite en section 4,
> puis exécute `scripts\dev.ps1`. Corrige les erreurs (5 itérations max par erreur),
> installe l'app, valide les trois scénarios de règles, ajoute le bouton de mise à jour
> GitHub, puis donne-moi la liste exacte des actions physiques qu'il me reste à faire.
> Mets `STATE.md` à jour. INTERDIT : passer un appel ou envoyer un SMS.

---

## 10. RAPPEL SÉCURITÉ FINAL

Le mode autonome a accès au téléphone via adb : ne l'active que sur ce PC, avec un câble
maîtrisé, et **désactive le débogage USB à la fin**.
