-- Migration Flyway: Mise à jour des groupes Administrateur
-- Version: 7
-- Description: 
--   - Modifie le nom du groupe GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel() en "Administrateur technique"
--   - Ajoute un nouveau groupe "Administrateur Référentiel"

-- Modifier le nom du groupe "Administrateur" en "Administrateur technique"
UPDATE groupe 
SET nom = 'Administrateur technique'
WHERE nom = 'Administrateur';

-- Ajouter le nouveau groupe "Administrateur Référentiel"
INSERT INTO groupe (nom)
SELECT 'Administrateur Référentiel'
WHERE NOT EXISTS (SELECT 1 FROM groupe WHERE nom = 'Administrateur Référentiel');
