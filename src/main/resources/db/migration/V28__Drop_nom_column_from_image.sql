-- Migration V28: Supprimer la colonne nom de la table image
-- La table image ne conserve que id, url et entity_id

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'image' AND column_name = 'nom'
    ) THEN
        ALTER TABLE image DROP COLUMN nom;
        RAISE NOTICE 'Colonne nom supprimée de la table image';
    END IF;
END $$;

-- Table d'audit image_aud
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'image_aud') THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'image_aud' AND column_name = 'nom'
        ) THEN
            ALTER TABLE image_aud DROP COLUMN nom;
            RAISE NOTICE 'Colonne nom supprimée de image_aud';
        END IF;
    END IF;
END $$;
