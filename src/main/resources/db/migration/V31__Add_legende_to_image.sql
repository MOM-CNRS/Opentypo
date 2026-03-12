-- Migration V31: Ajouter la colonne legende à la table image
-- Permet d'associer une légende à chaque image (URL ou upload local)

ALTER TABLE image ADD COLUMN IF NOT EXISTS legende VARCHAR(500);

-- Table d'audit image_aud (Hibernate Envers)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'image_aud') THEN
        ALTER TABLE image_aud ADD COLUMN IF NOT EXISTS legende VARCHAR(500);
    END IF;
END $$;
