-- Migration V16: Ajout du champ corpus_externe à entity_metadata
-- Description: Ajoute une colonne TEXT pour stocker le corpus externe

ALTER TABLE entity_metadata ADD COLUMN IF NOT EXISTS corpus_externe TEXT;

-- Table d'audit Hibernate Envers
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'entity_metadata_aud') THEN
        ALTER TABLE entity_metadata_aud ADD COLUMN IF NOT EXISTS corpus_externe TEXT;
    END IF;
END $$;
