-- Migration Flyway: Ajout de la colonne active à la table utilisateur
-- Version: 2
-- Description: Ajoute la colonne active et met à jour les valeurs existantes

-- Étape 1: Ajouter la colonne active si elle n'existe pas déjà (sans contrainte NOT NULL d'abord)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'utilisateur' AND column_name = 'active'
    ) THEN
        ALTER TABLE utilisateur ADD COLUMN active BOOLEAN;
    END IF;
END $$;

-- Étape 2: Mettre à jour tous les utilisateurs existants pour définir active = true par défaut
UPDATE utilisateur SET active = true WHERE active IS NULL;

-- Étape 3: Maintenant que toutes les valeurs sont définies, ajouter la contrainte NOT NULL et la valeur par défaut
DO $$
BEGIN
    -- Vérifier si la colonne existe et si elle a déjà la contrainte NOT NULL
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'utilisateur' 
        AND column_name = 'active'
        AND is_nullable = 'YES'
    ) THEN
        -- Définir la valeur par défaut d'abord
        ALTER TABLE utilisateur ALTER COLUMN active SET DEFAULT true;
        
        -- Ensuite, définir la contrainte NOT NULL
        ALTER TABLE utilisateur ALTER COLUMN active SET NOT NULL;
    END IF;
END $$;

