-- Migration Flyway: Ajout de la colonne entity_id à la table reference-opentheso
-- Version: 11
-- Description: Ajoute la colonne entity_id pour permettre de lier plusieurs références à une même entité

-- Ajouter la colonne entity_id si elle n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'reference-opentheso' AND column_name = 'entity_id'
    ) THEN
        ALTER TABLE "reference-opentheso" ADD COLUMN entity_id BIGINT;
        
        -- Ajouter une contrainte de clé étrangère
        ALTER TABLE "reference-opentheso" 
        ADD CONSTRAINT fk_reference_opentheso_entity 
        FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE;
        
        -- Créer un index pour améliorer les performances
        CREATE INDEX IF NOT EXISTS idx_reference_opentheso_entity_id ON "reference-opentheso"(entity_id);
        
        RAISE NOTICE 'Colonne entity_id ajoutée à la table reference-opentheso';
    ELSE
        RAISE NOTICE 'Colonne entity_id existe déjà dans la table reference-opentheso';
    END IF;
END $$;
