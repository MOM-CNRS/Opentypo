-- Migration V10: Ajout du champ ateliers à la table entity
-- Description: Ajoute une colonne ateliers de type TEXT pour stocker une liste d'ateliers concaténés par ";"

-- Vérifier si la colonne n'existe pas déjà (idempotence)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'entity' 
        AND column_name = 'ateliers'
    ) THEN
        ALTER TABLE entity ADD COLUMN ateliers TEXT;
        
        RAISE NOTICE 'Colonne ateliers ajoutée à la table entity';
    ELSE
        RAISE NOTICE 'Colonne ateliers existe déjà dans la table entity';
    END IF;
END $$;
