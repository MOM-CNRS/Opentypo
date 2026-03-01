-- Migration Flyway: Suppression de la colonne publique et migration des statuts
-- Version: 25
-- Description: La visibilité est désormais gérée uniquement par la colonne statut
--   (PROPOSITION, PUBLIQUE, PRIVEE, REFUSED). On migre ACCEPTED/AUTOMATIC vers PUBLIQUE.

-- Étape 1: Migrer les statuts existants vers les nouvelles valeurs
UPDATE entity SET statut = 'PUBLIQUE' WHERE statut IN ('ACCEPTED', 'AUTOMATIC', 'PUBLIC');
UPDATE entity SET statut = 'PRIVEE' WHERE statut = 'PRIVET';

-- Étape 2: Supprimer la colonne publique
ALTER TABLE entity DROP COLUMN IF EXISTS publique;
