-- Migration V42:
-- - Supprimer la colonne associe
-- - Renommer la colonne appartient en interne

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'entity_metadata' AND column_name = 'appartient'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'entity_metadata' AND column_name = 'interne'
    ) THEN
        ALTER TABLE entity_metadata RENAME COLUMN appartient TO interne;
    END IF;
END $$;

ALTER TABLE entity_metadata
    DROP COLUMN IF EXISTS associe;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'entity_metadata_aud') THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'entity_metadata_aud' AND column_name = 'appartient'
        ) AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'entity_metadata_aud' AND column_name = 'interne'
        ) THEN
            ALTER TABLE entity_metadata_aud RENAME COLUMN appartient TO interne;
        END IF;
        ALTER TABLE entity_metadata_aud DROP COLUMN IF EXISTS associe;
    END IF;
END $$;
