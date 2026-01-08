-- Migration Flyway: Ajout du type d'entité COLLECTION
-- Version: 6
-- Description: Insère le type d'entité COLLECTION dans la table entity_type

-- Insertion du type d'entité COLLECTION avec vérification d'existence pour éviter les doublons
INSERT INTO entity_type (code)
SELECT 'COLLECTION'
WHERE NOT EXISTS (SELECT 1 FROM entity_type WHERE code = 'COLLECTION');
