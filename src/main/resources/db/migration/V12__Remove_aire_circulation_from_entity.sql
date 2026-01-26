-- Migration pour supprimer la colonne aire_circulation de la table entity
-- Cette colonne est remplacée par une relation @OneToMany via entity_id dans reference-opentheso

DO $$
BEGIN
    -- Vérifier si la colonne existe avant de la supprimer
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'entity' AND column_name = 'aire_circulation'
    ) THEN
        -- Supprimer la contrainte de clé étrangère si elle existe
        IF EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE table_name = 'entity' 
            AND constraint_name LIKE '%aire_circulation%'
        ) THEN
            ALTER TABLE entity DROP CONSTRAINT IF EXISTS fk_entity_aire_circulation;
        END IF;
        
        -- Supprimer la colonne
        ALTER TABLE entity DROP COLUMN aire_circulation;
        
        RAISE NOTICE 'Colonne aire_circulation supprimée de la table entity';
    ELSE
        RAISE NOTICE 'Colonne aire_circulation n''existe pas dans la table entity';
    END IF;
END $$;
