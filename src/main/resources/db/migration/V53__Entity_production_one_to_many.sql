-- Production : passage de entity.production_id (1–1) à une liste reference-opentheso (code PRODUCTION, entity_id),
-- comme pour les aires de circulation.

-- 1) Associer la ligne référencée par production_id à l’entité si besoin
UPDATE "reference-opentheso" ro
SET entity_id = e.id
FROM entity e
WHERE e.production_id = ro.id
  AND (ro.entity_id IS DISTINCT FROM e.id);

UPDATE "reference-opentheso" ro
SET code = 'PRODUCTION'
FROM entity e
WHERE e.production_id = ro.id
  AND (ro.code IS DISTINCT FROM 'PRODUCTION');

-- 1bis) Éviter les doublons : la migration ne crée pas de lignes, mais la base peut déjà contenir
-- plusieurs lignes PRODUCTION pour la même entité (ex. entity_id renseigné + ancienne FK production_id).
-- On conserve uniquement la ligne officielle entity.production_id ; les autres PRODUCTION liées à la même
-- entité sont supprimées.
DELETE FROM "reference-opentheso" ro
USING entity e
WHERE e.production_id IS NOT NULL
  AND ro.entity_id = e.id
  AND ro.id <> e.production_id
  AND ro.code = 'PRODUCTION';

-- 2) Supprimer la FK entity.production_id puis la colonne
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
          AND pg_get_constraintdef(c.oid) ILIKE '%production_id%'
    LOOP
        EXECUTE format('ALTER TABLE entity DROP CONSTRAINT IF EXISTS %I', r.conname);
    END LOOP;
END $$;

ALTER TABLE entity DROP COLUMN IF EXISTS production_id;

-- Envers : même colonne si présente
ALTER TABLE entity_aud DROP COLUMN IF EXISTS production_id;
