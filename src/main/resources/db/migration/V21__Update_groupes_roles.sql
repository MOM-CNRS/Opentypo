-- Migration V21: Mise à jour des rôles dans la table groupe
-- - Lecteur -> Relecteur
-- - Éditeur -> Rédacteur
-- - Administrateur Référentiel -> Gestionnaire de référentiels
-- - Ajout: Valideur, Gestionnaire de collections

-- 1. Modifier Lecteur en Relecteur
UPDATE groupe SET nom = 'Relecteur' WHERE nom = 'Lecteur';

-- 2. Modifier Éditeur en Rédacteur
UPDATE groupe SET nom = 'Rédacteur' WHERE nom = 'Éditeur';

-- 3. Modifier Administrateur Référentiel en Gestionnaire de référentiels
UPDATE groupe SET nom = 'Gestionnaire de référentiels' WHERE nom = 'Administrateur Référentiel';

-- 4. Ajouter le rôle Valideur
INSERT INTO groupe (nom)
SELECT 'Valideur'
WHERE NOT EXISTS (SELECT 1 FROM groupe WHERE nom = 'Valideur');

-- 5. Ajouter le rôle Gestionnaire de collections
INSERT INTO groupe (nom)
SELECT 'Gestionnaire de collections'
WHERE NOT EXISTS (SELECT 1 FROM groupe WHERE nom = 'Gestionnaire de collections');
