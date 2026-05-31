-- Migration Flyway: Données par défaut (types d'entité, groupes, langues, administrateur)
-- Version: 62
-- Description: Types typologiques, groupes (GroupEnum), langues FR/EN, admin (admin / admin).
--              Aligné sur db/reference/default-reference-data.sql.

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

INSERT INTO groupe (nom)
SELECT 'Administrateur technique'
WHERE NOT EXISTS (SELECT 1 FROM groupe WHERE nom = 'Administrateur technique');

INSERT INTO groupe (nom)
SELECT 'Administrateur fonctionnel'
WHERE NOT EXISTS (SELECT 1 FROM groupe WHERE nom = 'Administrateur fonctionnel');

INSERT INTO groupe (nom)
SELECT 'Utilisateur'
WHERE NOT EXISTS (SELECT 1 FROM groupe WHERE nom = 'Utilisateur');

INSERT INTO langue (code, nom)
SELECT 'fr', 'Français'
WHERE NOT EXISTS (SELECT 1 FROM langue WHERE code = 'fr');

INSERT INTO langue (code, nom)
SELECT 'en', 'Anglais'
WHERE NOT EXISTS (SELECT 1 FROM langue WHERE code = 'en');

UPDATE langue SET nom = 'Français' WHERE code = 'fr' AND nom IS DISTINCT FROM 'Français';
UPDATE langue SET nom = 'Anglais' WHERE code = 'en' AND nom IS DISTINCT FROM 'Anglais';

INSERT INTO utilisateur (nom, prenom, email, password_hash, groupe_id, create_date, create_by, active)
SELECT
    'Administrateur',
    'Système',
    'admin',
    '$2a$10$hd6imws1NiYhSIe6JVeyqu8qS6Uz/gbsvKB0OOuQaN02mObbli1H.',
    g.id,
    CURRENT_TIMESTAMP,
    'SYSTEM',
    true
FROM groupe g
WHERE g.nom = 'Administrateur technique'
  AND NOT EXISTS (SELECT 1 FROM utilisateur u WHERE LOWER(u.email) = 'admin');
