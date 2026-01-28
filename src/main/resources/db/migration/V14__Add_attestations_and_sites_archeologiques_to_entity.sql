-- Migration V14: Ajout des champs attestations et sites_archeologiques à la table entity
-- Description: Ajoute deux colonnes TEXT pour stocker des listes de valeurs concaténées par ";"

-- Ajouter la colonne attestations si elle n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'entity' 
        AND column_name = 'attestations'
    ) THEN
        ALTER TABLE entity ADD COLUMN attestations TEXT;
        
        RAISE NOTICE 'Colonne attestations ajoutée à la table entity';
    ELSE
        RAISE NOTICE 'Colonne attestations existe déjà dans la table entity';
    END IF;
END $$;

-- Ajouter la colonne sites_archeologiques si elle n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'entity' 
        AND column_name = 'sites_archeologiques'
    ) THEN
        ALTER TABLE entity ADD COLUMN sites_archeologiques TEXT;
        
        RAISE NOTICE 'Colonne sites_archeologiques ajoutée à la table entity';
    ELSE
        RAISE NOTICE 'Colonne sites_archeologiques existe déjà dans la table entity';
    END IF;
END $$;
