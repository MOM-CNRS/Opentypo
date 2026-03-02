-- Migration V26: Changer materiaux (TEXT) en materiaux_id (FK reference_opentheso) dans caracteristique_physique
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'caracteristique_physique'
        AND column_name = 'materiaux_id'
    ) THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'caracteristique_physique'
            AND column_name = 'materiaux'
            AND data_type = 'text'
        ) THEN
            ALTER TABLE caracteristique_physique RENAME COLUMN materiaux TO materiaux_old;
        END IF;

        ALTER TABLE caracteristique_physique ADD COLUMN materiaux_id BIGINT;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE table_name = 'caracteristique_physique'
            AND constraint_name = 'fk_caracteristique_physique_materiaux'
        ) THEN
            ALTER TABLE caracteristique_physique
            ADD CONSTRAINT fk_caracteristique_physique_materiaux
            FOREIGN KEY (materiaux_id) REFERENCES "reference-opentheso"(id) ON DELETE SET NULL;
        END IF;

        RAISE NOTICE 'Colonne materiaux_id ajoutée à caracteristique_physique';
    ELSE
        RAISE NOTICE 'Colonne materiaux_id existe déjà dans caracteristique_physique';
    END IF;
END $$;
