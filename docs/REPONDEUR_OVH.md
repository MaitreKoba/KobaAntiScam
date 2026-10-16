<!--
FICHIER : REPONDEUR_OVH.md | CHEMIN : D:/Kobayashi/Developpement/shield/docs/REPONDEUR_OVH.md | LIGNES : 75
RÔLE : Guide de mise en place du répondeur OVH (ligne Découverte, renvois Free, écran Répondeur de l'app)
DATE : 2026-10-16 | VERSION : 1.9.0 | ORIGINE : Création | AUTEUR : Maitre Kobayashi | STATUT : STABLE
DÉPENDANCES : aucune | LIEN : README.md | NOTES : aucune donnée personnelle ni mot de passe dans ce fichier
-->

# Répondeur OVH

Le numéro principal ne sonne que pour les contacts, la liste blanche et les numéros déjà appelés. Les autres appels sont raccrochés par l'application et renvoyés par Free vers une ligne OVH, qui joue une annonce personnalisée et enregistre le message. L'application lit ensuite ces messages dans une boîte mail, les transcrit et les range par catégorie.

```
Inconnu appelle le 06
  -> l'app raccroche sans faire sonner
  -> Free renvoie (si occupé) vers la ligne OVH, gratuit
  -> OVH joue l'annonce MP3 et enregistre le message
  -> OVH envoie le message par e-mail
  -> l'app lit l'e-mail, transcrit, classe, notifie
```

## Coût

| Poste | Prix |
|---|---|
| Ligne OVH Découverte, sans engagement | 1,19 € TTC par mois |
| Renvoi d'appel Free vers un numéro français | inclus |
| Appels reçus, messages, e-mails | 0 € |
| Boîte mail OVH (MX Plan de l'hébergement) | incluse |
| Transcription Groq | offre gratuite |

Rien n'est facturé par appel tant que la ligne OVH n'appelle personne et n'envoie aucun SMS.

## 1. Chez OVH

Espace client, Télécom, VoIP, la ligne :

1. Messagerie vocale : annonce personnalisée, envoyer le fichier MP3.
2. Messagerie vocale : envoi des messages par e-mail vers la boîte dédiée, avec le fichier audio en pièce jointe.
3. Renvoi d'appel : renvoi immédiat vers la messagerie.
4. Ne configurer la ligne sur aucun téléphone ni application SIP, sinon les appels feraient sonner un appareil.

Si l'interface refuse le MP3, le convertir en WAV 8 kHz mono :

```bash
ffmpeg -i annonce.mp3 -ar 8000 -ac 1 annonce.wav
```

## 2. Dans l'application

Accueil, Répondeur, Configurer :

- adresse et mot de passe de la boîte (serveur `ssl0.ovh.net`, port 993) puis Tester la connexion ;
- numéro de la ligne OVH : il est ajouté à la liste blanche ;
- clé Groq facultative (console.groq.com, gratuite) pour lire les messages en texte ;
- Enregistrer. La boîte est vérifiée toutes les 15 minutes, et à chaque ouverture de l'écran.

Le mot de passe et la clé restent chiffrés sur le téléphone. La boîte est ouverte en lecture seule.

## 3. Renvois sur le 06 (Free)

L'écran Réglages du répondeur affiche les codes prêts à composer :

| Code | Effet |
|---|---|
| `**67*09XXXXXXXX#` | renvoi si occupé : les inconnus raccrochés par l'app |
| `**61*09XXXXXXXX#` | renvoi si pas de réponse |
| `**62*09XXXXXXXX#` | renvoi si éteint ou sans réseau |
| `*#67#` | vérifier le renvoi si occupé |
| `##002#` | annuler tous les renvois conditionnels |

Ne jamais activer le renvoi permanent `**21` : les contacts ne sonneraient plus.

## 4. Catégories

Le classement se fait sur les mots du message transcrit :

| Catégorie | Exemples de mots |
|---|---|
| Livraison | livreur, colis, commande, Uber, Chronopost, interphone |
| Travail | patron, planning, chantier, mission, contrat |
| Rendez-vous et démarches | médecin, rendez-vous, CAF, banque, préfecture |
| Démarchage | offre, prime, isolation, CPF, éligible, sans engagement |
| Autre | aucun mot reconnu |

Un numéro au préfixe ARCEP est toujours classé en démarchage. Le démarchage n'est pas notifié, sauf réglage contraire.

## 5. Test

1. Appeler le numéro OVH depuis un autre téléphone et laisser un message.
2. Vérifier que l'e-mail arrive dans la boîte.
3. Dans l'app, Répondeur, Actualiser : le message apparaît avec le numéro de l'appelant.
4. Si le numéro affiché est celui du 06 au lieu de l'appelant d'origine, ouvrir le menu du message, Voir l'e-mail, et signaler le format.
