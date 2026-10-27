# MachineVision - reconnaissance de machines industrielles

Prototype local qui estime le type d'une machine à partir d'une photo : pompe, compresseur ou convoyeur. React/Vite affiche l'interface, Spring Boot reçoit l'image, FastAPI/TensorFlow produit la prédiction et MySQL enregistre les résultats et les équipements.

La prédiction reconnaît une catégorie générale. Elle ne détermine pas à elle seule la marque, le modèle ou le numéro d'actif. La fiche de démonstration fournie est clairement provisoire et ne remplace pas un manuel constructeur.

## Architecture

| Composant | Port local | Fonction |
|---|---:|---|
| Frontend React/Vite | 5173 | Interface employé et envoi de la photo |
| API Spring Boot | 8080 | Orchestration, équipements et historique |
| Service ML Python/FastAPI | 8000 | Modèle TensorFlow/Keras et prédiction |
| MySQL | 3306 | Équipements et résultats |

## Prérequis

- Node.js et npm
- Python 3.12
- JDK 21 et Maven
- MySQL Server 8

Docker est optionnel. Le projet peut fonctionner directement sous Windows avec MySQL installé localement.

## Installation locale sous Windows

### 1. Préparer MySQL

Dans MySQL Workbench, connecte-toi avec un compte administrateur et exécute :

```sql
CREATE DATABASE IF NOT EXISTS machines
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'machines_dev'@'localhost'
  IDENTIFIED BY 'CHOISIR_UN_MOT_DE_PASSE_LOCAL';
GRANT ALL PRIVILEGES ON machines.* TO 'machines_dev'@'localhost';
```

Puis ouvre et exécute `database/init.sql`. Pour charger les trois actifs de démonstration et leurs fiches provisoires (pompe, compresseur et convoyeur), exécute aussi `database/seed_demo.sql`. Le script peut être relancé sans créer de doublons.

### 2. Démarrer le service ML

Dans PowerShell :

```powershell
cd ml-service
python -m venv .venv
& .\.venv\Scripts\python.exe -m pip install -r requirements.txt
& .\.venv\Scripts\python.exe -m uvicorn app:app --host 0.0.0.0 --port 8000
```

Le modèle nécessaire au lancement se trouve dans `ml-service/models/modele_machines_local.keras`. Vérifie `http://localhost:8000/health` : le statut doit être `ready`.

### 3. Démarrer Spring Boot

Dans une autre fenêtre PowerShell, définis les mêmes identifiants que dans MySQL :

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/machines?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER = "machines_dev"
$env:DB_PASSWORD = "LE_MOT_DE_PASSE_LOCAL"
$env:ML_BASE_URL = "http://localhost:8000"
cd backend
mvn spring-boot:run
```

Si Maven ou Java ne sont pas dans le `PATH`, configure JDK 21 et lance Maven depuis IntelliJ IDEA.

### 4. Démarrer React

Dans une troisième fenêtre PowerShell :

```powershell
cd frontend
npm.cmd install
npm.cmd run dev -- --host 0.0.0.0
```

Ouvre `http://localhost:5173`. Garde les trois fenêtres de service et MySQL ouverts pendant l'utilisation.

## Utiliser l'application sur un téléphone

Connecte le téléphone et le PC au même Wi-Fi. Sur le PC, exécute `ipconfig` et repère l'adresse IPv4 de l'adaptateur Wi-Fi. Sur le téléphone, ouvre `http://ADRESSE_IPV4_DU_PC:5173`, par exemple `http://192.168.0.118:5173` si cette adresse est celle affichée sur ton réseau.

Le PC doit rester allumé et les quatre services doivent continuer à fonctionner. Si la page ne s'ouvre pas, autorise Node.js/Vite sur le réseau privé dans le pare-feu Windows. Cette méthode sert uniquement sur le réseau local, pas depuis Internet.

### Accès privé depuis un autre Wi-Fi et installation sur le téléphone

Pour accéder à l'application hors du Wi-Fi local sans l'exposer à tout Internet, utilise Tailscale sur le PC et le téléphone :

1. Installe Tailscale sur les deux appareils et connecte-les au même compte personnel.
2. Garde MySQL, le service ML, Spring Boot et Vite démarrés sur le PC. Le PC doit rester allumé et ne pas se mettre en veille.
3. Dans PowerShell, exécute `tailscale status` et relève le nom DNS complet de ce PC, qui se termine par `.ts.net`.
4. Redémarre Vite en autorisant uniquement ce nom d'hôte :

```powershell
$env:MACHINEVISION_ALLOWED_HOST = "NOM-DNS-DU-PC.ts.net"
cd frontend
npm.cmd run dev -- --host 0.0.0.0
```

5. Dans PowerShell, publie le port Vite uniquement sur le réseau Tailscale et récupère l'adresse HTTPS affichée :

```powershell
tailscale serve --bg 5173
```

Tailscale Serve réserve cette adresse aux appareils du même réseau privé. N'utilise pas Tailscale Funnel pour ce projet. Si Tailscale demande d'activer les certificats HTTPS, suis sa page de configuration.

6. Sur le téléphone, connecte aussi Tailscale, ouvre l'adresse `https://...ts.net` obtenue, puis installe MachineVision depuis le menu du navigateur : **Installer l'application** sur Android ou **Partager > Sur l'écran d'accueil** sur iPhone.

L'application est préparée avec un manifeste PWA et une icône ; elle s'ouvre en plein écran, mais l'analyse et les PDF nécessitent que le PC et ses services restent disponibles. La connexion Tailscale doit rester active sur le téléphone pour accéder à l'adresse privée.

## Ajouter les photos et les fiches

Pour réentraîner le modèle, range des images dont tu as le droit d'utiliser sous `data/images/pompe/`, `data/images/compresseur/` et `data/images/convoyeur/`, puis lance depuis la racine :

```powershell
python ml-service/train_local.py
```

Les trois PDF `DEMO-*.pdf` sont des fiches descriptives provisoires créées à partir des photos. Ils ne sont pas des fiches constructeur ni des manuels d'utilisation ou de sécurité. Pour relier une vraie documentation, utilise la marque et le modèle lus sur la plaque. Place les documents vérifiés dans `documents/fiches_techniques/` ou `documents/manuels_officiels/`, puis enregistre leur chemin dans la table `equipment`.

## Préparer une publication GitHub

- Ne publie jamais `.env` ni un mot de passe. Le fichier `.env` local est ignoré par Git ; partage uniquement des valeurs fictives dans `.env.example`.
- Les dépendances, environnements Python et sorties de compilation sont ignorés.
- Les photos locales d'entraînement et les images de référence ne sont pas ajoutées automatiquement : vérifie leur licence avant de les publier.
- Vérifie les droits de redistribution des PDF officiels avant de pousser un dépôt public.
