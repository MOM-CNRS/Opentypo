-- Migration V38: Resynchroniser la séquence de revinfo avec les données existantes
-- Corrige l'erreur "duplicate key value violates unique constraint revinfo_pkey"
-- quand la séquence génère une valeur déjà présente dans la table
--
-- Sur base vide, MAX(rev) est NULL → ne pas appeler setval(..., 0) : hors bornes pour PostgreSQL.

DO $$
DECLARE
    seq_name TEXT;
    max_rev BIGINT;
BEGIN
    seq_name := pg_get_serial_sequence('revinfo', 'rev');
    IF seq_name IS NULL THEN
        seq_name := 'public.revinfo_rev_seq';
    END IF;

    SELECT COALESCE(MAX(rev), 0) INTO max_rev FROM revinfo;

    IF max_rev < 1 THEN
        -- Table vide : prochain nextval() doit être 1
        EXECUTE format('SELECT setval(%L, 1, false)', seq_name);
        RAISE NOTICE 'Séquence revinfo synchronisée (base vide) : prochaine valeur = 1';
    ELSE
        EXECUTE format('SELECT setval(%L, %s, true)', seq_name, max_rev);
        RAISE NOTICE 'Séquence revinfo synchronisée : prochaine valeur = %', max_rev + 1;
    END IF;
EXCEPTION
    WHEN undefined_table OR undefined_object OR invalid_parameter_value THEN
        RAISE NOTICE 'Séquence non trouvée ou erreur, ignore : %', seq_name;
END $$;
