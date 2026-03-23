-- Migration V37: Ajouter la colonne modified_by à revinfo pour tracer l'utilisateur
-- Cette colonne est remplie par OpentypoRevisionListener lors de chaque modification

ALTER TABLE revinfo ADD COLUMN IF NOT EXISTS modified_by VARCHAR(255);
