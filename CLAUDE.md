# SHIELD — instructions pour l'agent (Claude Code / opencode)

Ce dossier est un **handoff complet**. Lis d'abord :

1. `MEGA_PROMPT.md` — contexte, chemins, spec, règles de sécurité, ce qui a déjà été fait.
2. `STATE.md` — état courant et dernière action.

## Mode autonome via USB

Tu pilotes un vrai téléphone (Honor, MagicOS) via `adb`. Boucle autorisée :

**compiler → installer → accorder les permissions → lire le journal (logcat) → corriger.**

- **Maximum 5 itérations par erreur**, puis tu t'arrêtes et tu fais un rapport à l'humain.
- Script tout-en-un : `scripts\dev.ps1` (PowerShell) ou `scripts\dev.sh` (bash / Git Bash).

## Règles de sécurité (résumé — détails dans MEGA_PROMPT.md § 0)

- **INTERDIT** : passer un appel sortant, composer un numéro, envoyer un SMS réel.
  (pas de `am start` vers `tel:`/`ACTION_CALL`, pas de `service call phone`, pas de `sendsms`).
- Autorisé : fausses notifications locales de test (`adb shell cmd notification post`).
- Actions interdites sans confirmation : `adb uninstall`, `adb reboot`, `adb shell rm`,
  `pm clear`, `settings put`, `git push --force`, `rm -rf`.
- Un appel entrant réel se teste uniquement en demandant à l'humain d'appeler.

## À la fin de chaque session

Mettre à jour `STATE.md` (fait / en cours / bloqué / prochaines étapes).
