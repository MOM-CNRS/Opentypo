-- Migration V27: Migrer image_principale_url vers la table image, supprimer la colonne entity
-- 1. Ajouter la colonne nom à la table image (nom original du fichier)
-- 2. Migrer les données : pour chaque entity avec image_principale_url, créer une entrée dans image
-- 3. Supprimer la colonne image_principale_url de entity

-- 1. Ajouter nom à image
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'image' AND column_name = 'nom'
    ) THEN
        ALTER TABLE image ADD COLUMN nom VARCHAR(255);
        RAISE NOTICE 'Colonne nom ajoutée à la table image';
    END IF;
END $$;

-- 2. Migrer image_principale_url vers image
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'entity' AND column_name = 'image_principale_url'
    ) THEN
        INSERT INTO image (url, entity_id, nom)
        SELECT e.image_principale_url, e.id, NULL
        FROM entity e
        WHERE e.image_principale_url IS NOT NULL AND e.image_principale_url != '';
        RAISE NOTICE 'Données migrées de entity.image_principale_url vers image';
    END IF;
END $$;

-- 3. Supprimer image_principale_url de entity
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'entity' AND column_name = 'image_principale_url'
    ) THEN
        ALTER TABLE entity DROP COLUMN image_principale_url;
        RAISE NOTICE 'Colonne image_principale_url supprimée de entity';
    END IF;
END $$;

-- 4. Table d'audit image_aud
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'image_aud') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'image_aud' AND column_name = 'nom'
        ) THEN
            ALTER TABLE image_aud ADD COLUMN nom VARCHAR(255);
            RAISE NOTICE 'Colonne nom ajoutée à image_aud';
        END IF;
    END IF;
END $$;
