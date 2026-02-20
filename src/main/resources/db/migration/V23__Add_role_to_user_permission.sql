-- Migration V23: Ajout de la colonne role dans user_permission
-- Valeurs possibles : Gestionnaire de collection, Gestionnaire de référentiel, Rédacteur, Valideur, Relecteur

ALTER TABLE user_permission
ADD COLUMN IF NOT EXISTS role VARCHAR(100);
