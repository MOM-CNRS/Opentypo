-- Migration V29: Ajouter appartient et associe à entity_metadata
-- 1. Colonne appartient (VARCHAR 255)
ALTER TABLE entity_metadata ADD COLUMN IF NOT EXISTS appartient VARCHAR(255);

-- 2. Colonne associe (VARCHAR 255)
ALTER TABLE entity_metadata ADD COLUMN IF NOT EXISTS associe VARCHAR(255);

-- 3. Mise à jour table d'audit si Envers l'utilise
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'entity_metadata_aud') THEN
        ALTER TABLE entity_metadata_aud ADD COLUMN IF NOT EXISTS appartient VARCHAR(255);
        ALTER TABLE entity_metadata_aud ADD COLUMN IF NOT EXISTS associe VARCHAR(255);
    END IF;
END $$;
