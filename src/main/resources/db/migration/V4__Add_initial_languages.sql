-- Migration Flyway: Ajout des langues initiales (Français et Anglais)
-- Version: 4
-- Description: Ajoute les langues Français et Anglais à la table langue

-- Insérer le Français si elle n'existe pas déjà
INSERT INTO langue (code, nom)
SELECT 'fr', 'Français'
WHERE NOT EXISTS (
    SELECT 1 FROM langue WHERE code = 'fr'
);

-- Insérer l'Anglais si elle n'existe pas déjà
INSERT INTO langue (code, nom)
SELECT 'en', 'Anglais'
WHERE NOT EXISTS (
    SELECT 1 FROM langue WHERE code = 'en'
);

