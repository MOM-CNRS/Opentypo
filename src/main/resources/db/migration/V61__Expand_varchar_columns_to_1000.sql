-- Porte les colonnes VARCHAR métier (< 1000) à VARCHAR(1000).
-- Les tables d'audit Envers (*_aud) sont exclues : Hibernate les aligne via ddl-auto.
-- Sur une base neuve (schéma Hibernate déjà en 1000), cette migration est un no-op.

DO $$
DECLARE
    col RECORD;
    remaining INTEGER;
BEGIN
    SELECT COUNT(*) INTO remaining
    FROM information_schema.columns c
    WHERE c.table_schema = current_schema()
      AND c.data_type = 'character varying'
      AND c.character_maximum_length IS NOT NULL
      AND c.character_maximum_length < 1000
      AND c.table_name NOT LIKE '%\_aud' ESCAPE '\'
      AND c.table_name <> 'flyway_schema_history';

    IF remaining = 0 THEN
        RAISE NOTICE 'V61: aucune colonne VARCHAR à élargir — migration ignorée';
        RETURN;
    END IF;

    RAISE NOTICE 'V61: % colonne(s) VARCHAR à élargir', remaining;

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
          AND c.table_name NOT LIKE '%\_aud' ESCAPE '\'
          AND c.table_name <> 'flyway_schema_history'
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
