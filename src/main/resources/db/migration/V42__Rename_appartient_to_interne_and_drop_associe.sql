-- Migration V42:
-- - Supprimer la colonne associe
-- - Renommer la colonne appartient en interne

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'entity_metadata' AND column_name = 'appartient'
    ) THEN
        ALTER TABLE entity_metadata RENAME COLUMN appartient TO interne;
    END IF;
END $$;

ALTER TABLE entity_metadata
    DROP COLUMN IF EXISTS associe;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'entity_metadata_aud') THEN
        BEGIN
            ALTER TABLE entity_metadata_aud RENAME COLUMN appartient TO interne;
        EXCEPTION
            WHEN undefined_column THEN
                -- Colonne absente, ignorer
                NULL;
        END;
        ALTER TABLE entity_metadata_aud DROP COLUMN IF EXISTS associe;
    END IF;
END $$;
