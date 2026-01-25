-- Migration Flyway: Ajout des colonnes OpenTheso à la table reference-opentheso
-- Version: 9
-- Description: Ajoute les colonnes thesaurus_id, concept_id, collection_id et url pour stocker les informations OpenTheso

-- Ajouter la colonne thesaurus_id si elle n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'reference-opentheso' AND column_name = 'thesaurus_id'
    ) THEN
        ALTER TABLE "reference-opentheso" ADD COLUMN thesaurus_id VARCHAR(255);
    END IF;
END $$;

-- Ajouter la colonne concept_id si elle n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'reference-opentheso' AND column_name = 'concept_id'
    ) THEN
        ALTER TABLE "reference-opentheso" ADD COLUMN concept_id VARCHAR(255);
    END IF;
END $$;

-- Ajouter la colonne collection_id si elle n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'reference-opentheso' AND column_name = 'collection_id'
    ) THEN
        ALTER TABLE "reference-opentheso" ADD COLUMN collection_id VARCHAR(255);
    END IF;
END $$;

-- Ajouter la colonne url si elle n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'reference-opentheso' AND column_name = 'url'
    ) THEN
        ALTER TABLE "reference-opentheso" ADD COLUMN url VARCHAR(500);
    END IF;
END $$;
