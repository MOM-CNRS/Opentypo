-- Migration V38: Resynchroniser la séquence de revinfo avec les données existantes
-- Corrige l'erreur "duplicate key value violates unique constraint revinfo_pkey"
-- quand la séquence génère une valeur déjà présente dans la table

DO $$
DECLARE
    seq_name TEXT;
    max_rev BIGINT;
BEGIN
    seq_name := pg_get_serial_sequence('revinfo', 'rev');
    IF seq_name IS NULL THEN
        seq_name := 'revinfo_rev_seq';
    END IF;
    
    SELECT COALESCE(MAX(rev), 0) INTO max_rev FROM revinfo;
    EXECUTE format('SELECT setval(%L, %s)', seq_name, max_rev);
    RAISE NOTICE 'Séquence revinfo synchronisée : prochaine valeur = %', max_rev + 1;
EXCEPTION
    WHEN undefined_table OR undefined_object OR invalid_parameter_value THEN
        RAISE NOTICE 'Séquence non trouvée ou erreur, ignore : %', seq_name;
END $$;
