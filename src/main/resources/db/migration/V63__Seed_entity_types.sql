-- Migration Flyway: Types d'entité typologiques manquants
-- Version: 63
-- Description: COLLECTION, REFERENTIEL, CATEGORIE, GROUPE, SERIE, TYPE (idempotent).
--              Utile si V62 a été appliquée sans types ou si la table entity_type est vide.

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

INSERT INTO entity_type (code)
SELECT 'COLLECTION'
WHERE NOT EXISTS (SELECT 1 FROM entity_type WHERE code = 'COLLECTION');
