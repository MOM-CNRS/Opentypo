-- Certaines bases (contrainte Hibernate) n'ont pas ON DELETE CASCADE sur auteur_scientifique_id.
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_class rt ON rt.oid = c.confrelid
        WHERE t.relname = 'entity_auteur_scientifique'
          AND rt.relname = 'auteur_scientifique'
          AND c.contype = 'f'
    LOOP
        EXECUTE format('ALTER TABLE entity_auteur_scientifique DROP CONSTRAINT IF EXISTS %I', r.conname);
    END LOOP;
END $$;

ALTER TABLE entity_auteur_scientifique
    ADD CONSTRAINT fk_entity_auteur_scientifique_author
        FOREIGN KEY (auteur_scientifique_id) REFERENCES auteur_scientifique(id) ON DELETE CASCADE;
