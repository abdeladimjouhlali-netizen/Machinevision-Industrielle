# MySQL local

Le service Windows `MySQL80` fonctionne sans Docker. Pour préparer la base :

1. Ouvre MySQL Workbench et connecte-toi à l’instance locale avec un compte administrateur MySQL.
2. Exécute les requêtes de création de la base et de l’utilisateur dans la section MySQL du fichier principal `README.md`.
3. Ouvre `init.sql` dans Workbench et exécute-le pour créer `equipment` et `predictions`.
4. Utilise le même nom d’utilisateur et mot de passe dans les variables `DB_USER` et `DB_PASSWORD` avant de lancer Spring Boot.

## Ajouter une machine réelle

Place les PDF officiels dans `documents/`, par exemple `documents/fiches_techniques/` et `documents/manuels_officiels/`. Enregistre uniquement les informations vérifiées pour la marque et le modèle :

```sql
INSERT INTO equipment (
    asset_tag, category, manufacturer, model_name,
    technical_sheet_filename, manual_filename
) VALUES (
    'PMP-001', 'pompe', 'Fabricant réel', 'Modèle réel',
    'fiches_techniques/PMP-001.pdf',
    'manuels_officiels/PMP-001.pdf'
);
```

Les catégories autorisées sont `pompe`, `compresseur` et `convoyeur`. Ne publie pas de numéro de série confidentiel dans un dépôt public.
