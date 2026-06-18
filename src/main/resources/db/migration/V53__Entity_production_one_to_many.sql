-- Production : passage de entity.production_id (1–1) à une liste reference-opentheso (code PRODUCTION, entity_id),
-- comme pour les aires de circulation.

-- 1) et 1bis) Migration legacy production_id → reference-opentheso (seulement si la colonne existe)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'entity'
          AND column_name = 'production_id'
    ) THEN
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

        DELETE FROM "reference-opentheso" ro
        USING entity e
        WHERE e.production_id IS NOT NULL
          AND ro.entity_id = e.id
          AND ro.id <> e.production_id
          AND ro.code = 'PRODUCTION';
    END IF;
END $$;

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
