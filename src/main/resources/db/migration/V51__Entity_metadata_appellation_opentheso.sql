-- Appellation usuelle : référence OpenTheso (FK) au lieu du texte libre.

ALTER TABLE entity_metadata
    ADD COLUMN IF NOT EXISTS appellation_opentheso_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_entity_metadata_appellation_opentheso'
    ) THEN
        ALTER TABLE entity_metadata
            ADD CONSTRAINT fk_entity_metadata_appellation_opentheso
            FOREIGN KEY (appellation_opentheso_id) REFERENCES "reference-opentheso"(id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_entity_metadata_appellation_opentheso_id
    ON entity_metadata (appellation_opentheso_id);

-- Migrer les libellés existants vers reference-opentheso
INSERT INTO "reference-opentheso" (code, valeur, entity_id)
SELECT 'APPELLATION_USUELLE', TRIM(em.appellation), em.entity_id
FROM entity_metadata em
WHERE em.appellation IS NOT NULL
  AND TRIM(em.appellation) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM "reference-opentheso" ro
      WHERE ro.entity_id = em.entity_id
        AND ro.code = 'APPELLATION_USUELLE'
  );

UPDATE entity_metadata em
SET appellation_opentheso_id = ro.id
FROM "reference-opentheso" ro
WHERE ro.entity_id = em.entity_id
  AND ro.code = 'APPELLATION_USUELLE'
  AND em.appellation IS NOT NULL
  AND TRIM(em.appellation) <> ''
  AND em.appellation_opentheso_id IS NULL;

ALTER TABLE entity_metadata DROP COLUMN IF EXISTS appellation;

-- Audit Envers
ALTER TABLE entity_metadata_aud ADD COLUMN IF NOT EXISTS appellation_opentheso_id BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'entity_metadata_aud' AND column_name = 'appellation'
    ) THEN
        ALTER TABLE entity_metadata_aud DROP COLUMN appellation;
    END IF;
END $$;
