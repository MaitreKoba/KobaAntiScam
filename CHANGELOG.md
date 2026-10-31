# CHANGELOG - Kobayashi AntiScan

/**
 * FICHIER : CHANGELOG.md | CHEMIN : D:/Kobayashi/Developpement/KobayashiAntiScam/CHANGELOG.md
 * LIGNES : ~100 | RÔLE : Historique des versions | DATE : 2026-10-14
 * VERSION : 1.0.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi
 * STATUT : STABLE | DÉPENDANCES : aucun | LIEN : CHANGELOG.md
 * NOTES : Suivi des changements par version (SemVer strict)
 */

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr-FR/1.0.0/).

## [1.13.1] - 2026-10-31
### Corrigé
- Réglages : le bouton « Activer le rôle » ne faisait rien (code jamais écrit). Il ouvre maintenant la demande Android du rôle de filtrage d'appels ; sans ce rôle, l'app ne voit aucun appel (ni filtre ni alerte). Le bouton affiche « Filtrage actif » quand c'est le cas.
- Réglages : l'état est rafraîchi au retour sur l'écran.

### Ajouté
- Modules : alertes rouges cliquables en tête si le filtrage d'appels, les notifications ou la lecture des SMS sont inactifs.
- Modules : « Filtrer uniquement les suspects » (coupé par défaut). Activé, les inconnus sonnent et seuls les suspects sont filtrés ; coupé, seul le répertoire sonne.

## [1.13.0] - 2026-10-31
### Ajouté
- Réglages, section « Modules » : un interrupteur par règle d'appel (contacts, numéros déjà appelés, rappel sous 3 min, rejet du démarchage, arnaque au rappel, anti-rotation), pour le SMS anti-arnaque et pour chaque type d'alerte (tentatives d'appel, appels bloqués, messages du répondeur). Tout est actif par défaut.
- Alerte « Appel silencieux » quand un inconnu est mis en sonnerie coupée : chaque appel qui ne sonne pas est signalé.

### Notes
- Couper « Contacts » et « Numéros déjà appelés » permet de tester le répondeur avec son autre téléphone sans masquer le numéro (le « #31# » est refusé par l'option anti-appels masqués de l'opérateur ou du téléphone).

## [1.12.1] - 2026-10-31
### Corrigé
- Historique : les SMS ordinaires (proches compris) n'y sont plus enregistrés, avec leur raison technique « Package: … ». Seuls les SMS suspects et ceux bloqués par une règle y figurent.
- Historique : expéditeur court ou nommé (36105, AMELI) affiché tel quel au lieu de « +36105 ».
- Historique : un SMS suspect montre son score et son texte désarmé ; le toucher propose « Signaler au 33700 » et « Copier le texte ».

## [1.12.0] - 2026-10-31
### Corrigé
- Plantage de l'app à chaque notification de SMS : l'expression régulière d'extraction du numéro (`[\s+()-"]`) est invalide sur Android. Le plantage arrêtait tout le processus, filtrage d'appels et répondeur compris. Le traitement des SMS ne peut plus faire tomber l'app.
- Aucune notification sur Android 12 et moins : la permission POST_NOTIFICATIONS (Android 13+) était vérifiée partout et répondait toujours « refusée ». Vérification réelle : réglage système, plus la permission sur Android 13+, demandée au démarrage.
- Réglages : « Accès aux notifications » toujours vu comme inactif (méthode cachée d'Android).

### Ajouté
- SMS anti-arnaque : SMS avec lien d'un expéditeur inconnu, notification remplacée par une alerte « SMS suspect (score/100) » avec lien désarmé (hxxps, [.]) et bouton « Signaler au 33700 », inscrit à l'historique.
- Notification discrète « Appel bloqué » à chaque appel rejeté, avec la raison.
- Réglages, « Diagnostic et tests » : état de chaque service, dernière relève de la boîte, tester une notification, relever maintenant, simuler un appel (verdict sans enregistrement).
- Accueil : ligne « Notifications » dans l'état des services.
- 4 tests de l'analyse des SMS (25 au total).

## [1.11.0] - 2026-10-30
### Corrigé
- Répondeur : le numéro affiché était celui de la ligne OVH (09 72 12 02 56) au lieu de l'appelant. Le format réel des e-mails OVH (« provenant du numéro 33… ») est maintenant reconnu ; un appel masqué n'est plus confondu avec la ligne.
- Répondeur : la transcription fournie par OVH est utilisée quand il n'y a pas de clé Groq (liste, notification, classement par catégorie). Les messages déjà reçus sont réparés au chargement.
- Réglages : l'écran ne défilait pas, le mode strict, la liste neutre, le choix messagerie ou sonnerie coupée, la liste blanche et l'export étaient inaccessibles.
- Accueil et Apparence : le contenu passait sous la barre d'état en défilant.
- Accueil : le message « ton téléphone est surveillé » restait affiché protection en pause.

### Ajouté
- Bouton « Réglages : boîte mail, clé Groq, renvois » avec libellé dans l'écran Répondeur (l'icône seule passait inaperçue), et le même accès depuis Réglages.
- Crédits et à propos (Réglages) : auteur, services, bibliothèques, vie privée.
- 3 tests sur le format OVH réel (21 tests au total).

## [1.10.0] - 2026-10-30
### Ajouté
- Alerte immédiate quand un inconnu est envoyé au répondeur : notification « Au répondeur : numéro » avec bouton Rappeler, sans attendre l'e-mail.
- Relève rapide de la boîte mail 45 s, 90 s, 3 min et 6 min après l'appel : le message (texte et catégorie) arrive en une à deux minutes au lieu de 15.

## [1.9.2] - 2026-10-30
### Modifié
- Notification d'un nouveau message vocal : sans transcription Groq, elle affiche le texte de l'e-mail OVH (option « Transcription et message joint »), au lieu de « Nouveau message vocal ». Texte déplié jusqu'à 1500 caractères.

## [1.9.1] - 2026-10-30
### Modifié
- Écran Répondeur : les boutons Vérifier et « Tout annuler » (`##002#`) sont toujours affichés, même sans numéro OVH saisi, pour contrôler ou supprimer des renvois composés à la main.
- Ajout de la vérification du renvoi si pas de réponse (`*#61#`) et si éteint (`*#62#`).

## [1.9.0] - 2026-10-16
### Ajouté
- Écran Répondeur : messages vocaux de la ligne OVH lus dans une boîte mail (IMAP SSL, lecture seule), numéro de l'appelant extrait de l'e-mail, écoute de l'audio, transcription facultative par Groq Whisper, classement automatique (Livraison, Travail, Rendez-vous et démarches, Démarchage, Autre), étiquette Sûr, Inconnu ou Spam, actions rappeler, autoriser, marquer comme spam, voir l'e-mail.
- Synchronisation toutes les 15 minutes (WorkManager) et notification des nouveaux messages, sauf le démarchage.
- Réglages du répondeur : boîte mail avec test de connexion, numéro OVH ajouté à la liste blanche, clé Groq, codes de renvoi Free conditionnels prêts à composer (`**67`, `**61`, `**62`, vérification, annulation).
- Mot de passe et clé stockés chiffrés (EncryptedSharedPreferences).
- Tuile Répondeur sur l'accueil avec le nombre de messages non lus.
- Guide `docs/REPONDEUR_OVH.md`. Tests `ClassementTest` (9 tests).

## [1.8.0] - 2026-10-16
### Ajouté
- Historique et Dernière activité : étiquette de confiance à côté du verdict. Spam (appel rejeté : démarchage ARCEP, arnaque au rappel, rotation, numéro bloqué), Sûr (contact, liste blanche, numéro déjà appelé, rappel), Inconnu (messagerie ou silencieux), Non vérifié (protection coupée).
- Le numéro de l'appelant est toujours visible, y compris pour un contact (nom en titre, numéro sur la ligne du dessous).
- Action « Marquer comme spam (rejeté) » au lieu de « Bloquer ce numéro ».

## [1.7.0] - 2026-10-16
### Ajouté
- Moteur de décision `Decision.kt` : chaque appel reçoit une action et une raison lisible, enregistrées dans l'historique. Ordre : protection coupée, règle perso, contact (avec son nom), liste blanche, numéro déjà appelé, démarchage ARCEP (rejet), rappel en moins de 3 min (sonne), pays connus pour l'arnaque au rappel (rejet), rotation de numéros (plus de 5 numéros différents d'une même tranche en 24 h, rejet), inconnu ou masqué (messagerie).
- Historique refait : icône et couleur par verdict, nom du contact ou numéro au format 06 95 12 34 56, raison de la décision, heure relative, filtres (Rejetés, Messagerie, Ont sonné, SMS). Touche un appel pour l'autoriser, le bloquer, le rappeler ou copier le numéro.
- Accueil refait : icônes vectorielles Material au lieu des emojis, compteurs du jour dans la carte d'état, bloc « Dernière activité » avec les raisons, tuiles colorées avec chevron, état des services avec icônes.

### Corrigé
- La règle « rappel sous 3 min » était toujours vraie (l'heure était enregistrée avant la décision) : tous les inconnus sonnaient.
- Contacts mal reconnus (recherche sur `3695...` alors que le contact est enregistré en `06 95...`) : recherche par `PhoneLookup`.
- Numéro masqué : était autorisé par défaut, part maintenant en messagerie.
- Mode messagerie (activé par défaut) : rejetait tous les non-contacts et jouait le MP3 sur le haut-parleur du téléphone. Les inconnus partent maintenant en messagerie Free sans rejet, et le MP3 n'est plus joué.
- `normalize` : indicatif `00` (00225... devient +225...), chaîne sans chiffre vide. `normalizePrefix("162")` donne `+33162`.
- Tranche anti-rotation : prenait les 8 derniers chiffres (le numéro lui-même) au lieu des 8 premiers ; compteurs persistants.
- `RulesTest.kt` compile et passe (9 tests).

## [1.6.1] - 2026-10-16
### Corrigé
- Mise à jour intégrée : l'app plantait à la fin du téléchargement (`FileProvider` : « Failed to find configured root »), car deux racines de `file_paths.xml` portaient le même nom `external_files`. Résultat : rien ne s'installait et l'app restait sur l'ancienne version.
- L'APK est maintenant téléchargé dans le dossier privé de l'app (`Android/data/.../files/Download/KobaAntiScan-update.apk`), écrasé à chaque fois : fini les doublons `-1`, `-2` dans Téléchargements et l'installation d'un ancien fichier.
- Avant installation, l'APK est contrôlé : paquet identique, `versionCode` strictement supérieur. Un APK de release mal étiqueté (cas de la v1.5.0, compilée en 1.4.3) est refusé avec un message clair au lieu d'une fausse réussite.
- Comparaison de versions SemVer (l'ancienne proposait une « mise à jour » dès que les chaînes différaient, y compris vers une version plus ancienne).
- Receiver `DOWNLOAD_COMPLETE` enregistré avec `RECEIVER_EXPORTED` (obligatoire avec targetSdk 34+), statut du téléchargement vérifié, renvoi vers l'autorisation « sources inconnues » si elle manque.

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
