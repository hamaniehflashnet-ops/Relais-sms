# SMS Pro — Guide simple pour obtenir ton APK

Aucune installation, aucune commande. Juste un compte gratuit et 4 étapes.

## Étape 1 — Créer un compte GitHub (5 min)
1. Va sur https://github.com/signup
2. Choisis un nom d'utilisateur, ton email, un mot de passe
3. Valide ton compte (email de confirmation)

## Étape 2 — Créer un nouveau dépôt (dépôt = dossier de projet)
1. Une fois connecté, clique sur le bouton vert **"New"** (ou le "+" en haut à droite → "New repository")
2. Nomme-le par exemple `sms-pro-app`
3. Laisse-le en **Public** (le mode gratuit d'Actions marche mieux en public)
4. Clique **"Create repository"**

## Étape 3 — Déposer le code
1. Sur la page du dépôt créé, clique sur **"uploading an existing file"**
2. Dézippe le fichier `sms-pro-app.zip` que je t'ai donné sur ton ordinateur
3. Glisse-dépose **tout le contenu** du dossier dézippé (pas le dossier lui-même, son contenu) dans la zone d'upload GitHub
4. En bas de page, clique **"Commit changes"**

## Étape 4 — Récupérer l'APK
1. En haut du dépôt, clique sur l'onglet **"Actions"**
2. Tu vois une compilation qui tourne automatiquement (ça prend 3-5 minutes) — un rond jaune qui devient vert ✅
3. Clique dessus, puis descends jusqu'à **"Artifacts"**
4. Clique sur **"sms-pro-apk"** → ça télécharge un fichier zip contenant ton APK
5. Dézippe-le : voici ton `app-debug.apk`, prêt à installer sur un téléphone Android !

## Installer l'APK sur un téléphone
1. Envoie-toi le fichier `app-debug.apk` (par email, Drive, ou câble USB)
2. Sur le téléphone, ouvre le fichier → Android va demander d'autoriser "Sources inconnues" → accepte
3. L'app s'installe

## ⚠️ Important à savoir
- Cette version est un **debug APK** : elle fonctionne parfaitement pour tester, mais pour publier sur le Play Store il faudra la "signer" (étape technique en plus, on la fera plus tard).
- L'app actuelle contient : un écran de connexion, un tableau de bord avec les 6 modules (Contacts, Groupes, Messages, Campagnes, Statistiques, Paramètres) et le moteur d'envoi SMS en arrière-plan. Les modules sont pour l'instant des écrans "vides" — on les remplira un par un ensuite.
- L'app va essayer de contacter un backend à une adresse fictive (`api.smspro.exemple.com`) — normal, tant qu'on n'a pas construit le vrai serveur, la passerelle ne trouvera rien à envoyer. Ça n'empêche pas l'app de s'installer et de s'ouvrir.

## Et après ?
Une fois que tu as vérifié que l'app s'installe et s'ouvre bien, dis-le moi et on avance sur la prochaine brique (le backend, ou remplir un des 6 modules).
