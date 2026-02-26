-- Migration V24: Ajouter id_ark à entity et commentaire à entity_metadata

-- 1. Ajouter id_ark (VARCHAR) à la table entity
ALTER TABLE entity ADD COLUMN IF NOT EXISTS id_ark VARCHAR(255);

-- 2. Ajouter commentaire (TEXT) à entity_metadata
ALTER TABLE entity_metadata ADD COLUMN IF NOT EXISTS commentaire TEXT;

-- 3. Mise à jour table d'audit Hibernate Envers pour entity
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'entity_aud') THEN
        ALTER TABLE entity_aud ADD COLUMN IF NOT EXISTS id_ark VARCHAR(255);
    END IF;
END $$;

-- 4. Mise à jour table d'audit pour entity_metadata
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'entity_metadata_aud') THEN
        ALTER TABLE entity_metadata_aud ADD COLUMN IF NOT EXISTS commentaire TEXT;
    END IF;
END $$;
