-- Période : passage de entity.periode_id (1–1) à une liste reference-opentheso (code PERIODE, entity_id),
-- comme pour la production.

-- 1) Associer la ligne référencée par periode_id à l’entité si besoin
UPDATE "reference-opentheso" ro
SET entity_id = e.id
FROM entity e
WHERE e.periode_id = ro.id
  AND (ro.entity_id IS DISTINCT FROM e.id);

UPDATE "reference-opentheso" ro
SET code = 'PERIODE'
FROM entity e
WHERE e.periode_id = ro.id
  AND (ro.code IS DISTINCT FROM 'PERIODE');

-- 1bis) Éviter les doublons : la migration ne crée pas de lignes, mais la base peut déjà contenir
-- plusieurs lignes PERIODE pour la même entité (ex. entity_id renseigné + ancienne FK periode_id).
-- On conserve uniquement la ligne officielle entity.periode_id ; les autres PERIODE liées à la même
-- entité sont supprimées (orphelinRemoval côté appli ferait pareil).
DELETE FROM "reference-opentheso" ro
USING entity e
WHERE e.periode_id IS NOT NULL
  AND ro.entity_id = e.id
  AND ro.id <> e.periode_id
  AND ro.code = 'PERIODE';

-- 2) Supprimer la FK entity.periode_id puis la colonne
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'entity'
          AND c.contype = 'f'
          AND pg_get_constraintdef(c.oid) ILIKE '%periode_id%'
    LOOP
        EXECUTE format('ALTER TABLE entity DROP CONSTRAINT IF EXISTS %I', r.conname);
    END LOOP;
END $$;

ALTER TABLE entity DROP COLUMN IF EXISTS periode_id;

-- Envers : même colonne si présente
ALTER TABLE entity_aud DROP COLUMN IF EXISTS periode_id;
