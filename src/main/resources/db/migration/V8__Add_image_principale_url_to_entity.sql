-- Migration Flyway: Ajout de la colonne image_principale_url à la table entity
-- Version: 8
-- Description: Ajoute la colonne image_principale_url pour stocker l'URL de l'image principale chargée dans IIIF

-- Ajouter la colonne image_principale_url si elle n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'entity' AND column_name = 'image_principale_url'
    ) THEN
        ALTER TABLE entity ADD COLUMN image_principale_url VARCHAR(500);
    END IF;
END $$;
