-- Migration Flyway: Ajout de la colonne publique à la table entity
-- Version: 3
-- Description: Ajoute la colonne publique et met à jour les valeurs existantes

-- Étape 1: Ajouter la colonne publique si elle n'existe pas déjà (sans contrainte NOT NULL d'abord)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'entity' AND column_name = 'publique'
    ) THEN
        ALTER TABLE entity ADD COLUMN publique BOOLEAN;
    END IF;
END $$;

-- Étape 2: Mettre à jour toutes les entités existantes pour définir publique = true par défaut
UPDATE entity SET publique = true WHERE publique IS NULL;

-- Étape 3: Maintenant que toutes les valeurs sont définies, ajouter la contrainte NOT NULL et la valeur par défaut
DO $$
BEGIN
    -- Vérifier si la colonne existe et si elle a déjà la contrainte NOT NULL
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'entity' 
        AND column_name = 'publique'
        AND is_nullable = 'YES'
    ) THEN
        -- Définir la valeur par défaut d'abord
        ALTER TABLE entity ALTER COLUMN publique SET DEFAULT true;
        
        -- Ensuite, définir la contrainte NOT NULL
        ALTER TABLE entity ALTER COLUMN publique SET NOT NULL;
    END IF;
END $$;

