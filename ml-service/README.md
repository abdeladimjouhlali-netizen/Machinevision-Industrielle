# Service de reconnaissance TensorFlow

Ce service Python charge le modèle Keras exporté depuis Colab et expose :

- `GET /health` pour vérifier que le modèle est chargé ;
- `POST /predict` avec un champ image multipart pour obtenir une catégorie et un score.

## Réentraîner sur les photos locales

Range les images dans `data/images/<categorie>/` avec un dossier par catégorie. Puis, depuis la racine du projet, lance :

```powershell
python ml-service/train_local.py
```

Le script réserve une image par catégorie pour une validation indicative, entraîne la tête de classification avec augmentation, et écrit `models/modele_machines_local.keras` ainsi qu'un rapport JSON. Il conserve `modele_demo_machines.keras`. Le service charge par défaut le nouveau modèle au prochain redémarrage.

Le jeu actuel est très petit (5 compresseurs, 9 convoyeurs et 4 pompes). Le score sur trois images de validation n'est pas une mesure fiable des performances sur de nouvelles machines. Ajoute beaucoup plus de photos réelles, prises sous des angles et dans des conditions variés, avant toute utilisation opérationnelle.

## Exporter depuis Colab

Après le réentraînement, sauvegarde le modèle au format `.keras` dans Google Drive :

```python
modele.save(projet / "modele_demo_machines.keras")
```

Télécharge ensuite ce fichier depuis Drive sur l’ordinateur et place-le ici :

```text
ml-service/models/modele_demo_machines.keras
```

Vérifie aussi que `class_names.json` a le même ordre que `entrainement.class_names` dans Colab. Les noms par défaut sont rangés par ordre alphabétique : `compresseur`, `convoyeur`, `pompe`.

Le modèle d’exercice est très petit et ne doit pas être utilisé pour les décisions d’exploitation ou de sécurité.
