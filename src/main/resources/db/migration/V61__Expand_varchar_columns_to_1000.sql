-- Porte toutes les colonnes character varying (VARCHAR) à VARCHAR(1000).
-- PostgreSQL : pas de VARCHAR2 ; stockage inchangé pour les valeurs déjà courtes.

DO $$
DECLARE
    col RECORD;
BEGIN
    FOR col IN
        SELECT c.table_schema,
               c.table_name,
               c.column_name,
               c.character_maximum_length
        FROM information_schema.columns c
        WHERE c.table_schema = current_schema()
          AND c.data_type = 'character varying'
          AND c.character_maximum_length IS NOT NULL
          AND c.character_maximum_length < 1000
        ORDER BY c.table_name, c.column_name
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I ALTER COLUMN %I TYPE VARCHAR(1000)',
            col.table_schema,
            col.table_name,
            col.column_name
        );
        RAISE NOTICE 'V61: %.% -> VARCHAR(1000) (était %)',
            col.table_name, col.column_name, col.character_maximum_length;
    END LOOP;
END $$;
