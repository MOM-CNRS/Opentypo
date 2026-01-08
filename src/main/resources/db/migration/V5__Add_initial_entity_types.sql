-- Migration Flyway: Insertion des types d'entité par défaut
-- Version: 5
-- Description: Insère les types d'entité REFERENTIEL, CATEGORIE, GROUPE, SERIE, TYPE dans la table entity_type

-- Insertion des types d'entité avec vérification d'existence pour éviter les doublons
INSERT INTO entity_type (code)
SELECT 'REFERENTIEL'
WHERE NOT EXISTS (SELECT 1 FROM entity_type WHERE code = 'REFERENTIEL');

INSERT INTO entity_type (code)
SELECT 'CATEGORIE'
WHERE NOT EXISTS (SELECT 1 FROM entity_type WHERE code = 'CATEGORIE');

INSERT INTO entity_type (code)
SELECT 'GROUPE'
WHERE NOT EXISTS (SELECT 1 FROM entity_type WHERE code = 'GROUPE');

INSERT INTO entity_type (code)
SELECT 'SERIE'
WHERE NOT EXISTS (SELECT 1 FROM entity_type WHERE code = 'SERIE');

INSERT INTO entity_type (code)
SELECT 'TYPE'
WHERE NOT EXISTS (SELECT 1 FROM entity_type WHERE code = 'TYPE');

