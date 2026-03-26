-- Certaines bases peuvent avoir label.nom ou entity_metadata.code en bytea
-- (schémas anciens ou outils) : PostgreSQL n'a pas de fonction lower(bytea).

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns c
        WHERE c.table_schema = current_schema()
          AND c.table_name = 'label'
          AND c.column_name = 'nom'
          AND c.data_type = 'bytea'
    ) THEN
        ALTER TABLE label
            ALTER COLUMN nom TYPE VARCHAR(255) USING convert_from(nom, 'UTF8');
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns c
        WHERE c.table_schema = current_schema()
          AND c.table_name = 'entity_metadata'
          AND c.column_name = 'code'
          AND c.data_type = 'bytea'
    ) THEN
        ALTER TABLE entity_metadata
            ALTER COLUMN code TYPE VARCHAR(100) USING convert_from(code, 'UTF8');
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns c
        WHERE c.table_schema = current_schema()
          AND c.table_name = 'label_aud'
          AND c.column_name = 'nom'
          AND c.data_type = 'bytea'
    ) THEN
        ALTER TABLE label_aud
            ALTER COLUMN nom TYPE VARCHAR(255) USING convert_from(nom, 'UTF8');
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns c
        WHERE c.table_schema = current_schema()
          AND c.table_name = 'entity_metadata_aud'
          AND c.column_name = 'code'
          AND c.data_type = 'bytea'
    ) THEN
        ALTER TABLE entity_metadata_aud
            ALTER COLUMN code TYPE VARCHAR(100) USING convert_from(code, 'UTF8');
    END IF;
END $$;
