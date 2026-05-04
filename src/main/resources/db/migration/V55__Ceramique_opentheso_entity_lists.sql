-- Céramique : fonction/usage, fabrication/façonnage, couleur/nature/inclusion/cuisson de pâte
-- passent en listes reference-opentheso sur entity (entity_id + code), comme la production.
-- Suppression des FK description_detail.fonction_id, caracteristique_physique.fabrication_id,
-- description_pate.couleur_id, nature_id, inclusion_id, cuisson_id.

-- ========== 1) fonction_id (description_detail) → FONCTION_USAGE ==========
UPDATE "reference-opentheso" ro
SET entity_id = dd.entity_id
FROM description_detail dd
WHERE dd.fonction_id = ro.id
  AND (ro.entity_id IS DISTINCT FROM dd.entity_id);

UPDATE "reference-opentheso" ro
SET code = 'FONCTION_USAGE'
FROM description_detail dd
WHERE dd.fonction_id = ro.id
  AND (ro.code IS DISTINCT FROM 'FONCTION_USAGE');

DELETE FROM "reference-opentheso" ro
USING description_detail dd
WHERE dd.fonction_id IS NOT NULL
  AND ro.entity_id = dd.entity_id
  AND ro.code = 'FONCTION_USAGE'
  AND ro.id <> dd.fonction_id;

DO $$
DECLARE r record;
BEGIN
    FOR r IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'description_detail'
          AND c.contype = 'f'
          AND pg_get_constraintdef(c.oid) ILIKE '%fonction_id%'
    LOOP
        EXECUTE format('ALTER TABLE description_detail DROP CONSTRAINT IF EXISTS %I', r.conname);
    END LOOP;
END $$;

ALTER TABLE description_detail DROP COLUMN IF EXISTS fonction_id;
ALTER TABLE description_detail_aud DROP COLUMN IF EXISTS fonction_id;

-- ========== 2) fabrication_id (caracteristique_physique) → FABRICATION_FACONNAGE ==========
UPDATE "reference-opentheso" ro
SET entity_id = cp.entity_id
FROM caracteristique_physique cp
WHERE cp.fabrication_id = ro.id
  AND (ro.entity_id IS DISTINCT FROM cp.entity_id);

UPDATE "reference-opentheso" ro
SET code = 'FABRICATION_FACONNAGE'
FROM caracteristique_physique cp
WHERE cp.fabrication_id = ro.id
  AND (ro.code IS DISTINCT FROM 'FABRICATION_FACONNAGE');

DELETE FROM "reference-opentheso" ro
USING caracteristique_physique cp
WHERE cp.fabrication_id IS NOT NULL
  AND ro.entity_id = cp.entity_id
  AND ro.code = 'FABRICATION_FACONNAGE'
  AND ro.id <> cp.fabrication_id;

DO $$
DECLARE r record;
BEGIN
    FOR r IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'caracteristique_physique'
          AND c.contype = 'f'
          AND pg_get_constraintdef(c.oid) ILIKE '%fabrication_id%'
    LOOP
        EXECUTE format('ALTER TABLE caracteristique_physique DROP CONSTRAINT IF EXISTS %I', r.conname);
    END LOOP;
END $$;

ALTER TABLE caracteristique_physique DROP COLUMN IF EXISTS fabrication_id;
ALTER TABLE caracteristique_physique_aud DROP COLUMN IF EXISTS fabrication_id;

-- ========== 3) description_pate : couleur, nature, inclusion, cuisson ==========
UPDATE "reference-opentheso" ro
SET entity_id = dp.entity_id
FROM description_pate dp
WHERE dp.couleur_id = ro.id
  AND (ro.entity_id IS DISTINCT FROM dp.entity_id);

UPDATE "reference-opentheso" ro
SET code = 'COULEUR_PATE'
FROM description_pate dp
WHERE dp.couleur_id = ro.id
  AND (ro.code IS DISTINCT FROM 'COULEUR_PATE');

DELETE FROM "reference-opentheso" ro
USING description_pate dp
WHERE dp.couleur_id IS NOT NULL
  AND ro.entity_id = dp.entity_id
  AND ro.code = 'COULEUR_PATE'
  AND ro.id <> dp.couleur_id;

UPDATE "reference-opentheso" ro
SET entity_id = dp.entity_id
FROM description_pate dp
WHERE dp.nature_id = ro.id
  AND (ro.entity_id IS DISTINCT FROM dp.entity_id);

UPDATE "reference-opentheso" ro
SET code = 'NATURE_PATE'
FROM description_pate dp
WHERE dp.nature_id = ro.id
  AND (ro.code IS DISTINCT FROM 'NATURE_PATE');

DELETE FROM "reference-opentheso" ro
USING description_pate dp
WHERE dp.nature_id IS NOT NULL
  AND ro.entity_id = dp.entity_id
  AND ro.code = 'NATURE_PATE'
  AND ro.id <> dp.nature_id;

UPDATE "reference-opentheso" ro
SET entity_id = dp.entity_id
FROM description_pate dp
WHERE dp.inclusion_id = ro.id
  AND (ro.entity_id IS DISTINCT FROM dp.entity_id);

UPDATE "reference-opentheso" ro
SET code = 'INCLUSIONS'
FROM description_pate dp
WHERE dp.inclusion_id = ro.id
  AND (ro.code IS DISTINCT FROM 'INCLUSIONS');

DELETE FROM "reference-opentheso" ro
USING description_pate dp
WHERE dp.inclusion_id IS NOT NULL
  AND ro.entity_id = dp.entity_id
  AND ro.code = 'INCLUSIONS'
  AND ro.id <> dp.inclusion_id;

UPDATE "reference-opentheso" ro
SET entity_id = dp.entity_id
FROM description_pate dp
WHERE dp.cuisson_id = ro.id
  AND (ro.entity_id IS DISTINCT FROM dp.entity_id);

UPDATE "reference-opentheso" ro
SET code = 'CUISSON_POST_CUISSON'
FROM description_pate dp
WHERE dp.cuisson_id = ro.id
  AND (ro.code IS DISTINCT FROM 'CUISSON_POST_CUISSON');

DELETE FROM "reference-opentheso" ro
USING description_pate dp
WHERE dp.cuisson_id IS NOT NULL
  AND ro.entity_id = dp.entity_id
  AND ro.code = 'CUISSON_POST_CUISSON'
  AND ro.id <> dp.cuisson_id;

DO $$
DECLARE r record;
BEGIN
    FOR r IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'description_pate'
          AND c.contype = 'f'
          AND (
              pg_get_constraintdef(c.oid) ILIKE '%couleur_id%'
              OR pg_get_constraintdef(c.oid) ILIKE '%nature_id%'
              OR pg_get_constraintdef(c.oid) ILIKE '%inclusion_id%'
              OR pg_get_constraintdef(c.oid) ILIKE '%cuisson_id%'
          )
    LOOP
        EXECUTE format('ALTER TABLE description_pate DROP CONSTRAINT IF EXISTS %I', r.conname);
    END LOOP;
END $$;

ALTER TABLE description_pate DROP COLUMN IF EXISTS couleur_id;
ALTER TABLE description_pate DROP COLUMN IF EXISTS nature_id;
ALTER TABLE description_pate DROP COLUMN IF EXISTS inclusion_id;
ALTER TABLE description_pate DROP COLUMN IF EXISTS cuisson_id;

ALTER TABLE description_pate_aud DROP COLUMN IF EXISTS couleur_id;
ALTER TABLE description_pate_aud DROP COLUMN IF EXISTS nature_id;
ALTER TABLE description_pate_aud DROP COLUMN IF EXISTS inclusion_id;
ALTER TABLE description_pate_aud DROP COLUMN IF EXISTS cuisson_id;
