-- Migration V19: Ajouter entity_id à image, supprimer image_id de entity
-- Inverse la relation : image pointe vers entity (plusieurs images par entité)

DO $$
BEGIN
    -- 1. Ajouter entity_id à la table image
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'image' AND column_name = 'entity_id'
    ) THEN
        ALTER TABLE image ADD COLUMN entity_id BIGINT;
        RAISE NOTICE 'Colonne entity_id ajoutée à la table image';
    ELSE
        RAISE NOTICE 'Colonne entity_id existe déjà dans la table image';
    END IF;
END $$;

-- 2. Migrer les données : copier entity.id vers image.entity_id pour les images liées
UPDATE image i
SET entity_id = e.id
FROM entity e
WHERE e.image_id = i.id;

-- 3. Contrainte de clé étrangère et index
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_image_entity'
    ) THEN
        ALTER TABLE image
        ADD CONSTRAINT fk_image_entity
        FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE;
        RAISE NOTICE 'Contrainte fk_image_entity ajoutée';
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_image_entity_id ON image(entity_id);

-- 4. Supprimer image_id de entity (la contrainte FK est supprimée automatiquement)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'entity' AND column_name = 'image_id'
    ) THEN
        ALTER TABLE entity DROP COLUMN image_id;
        RAISE NOTICE 'Colonne image_id supprimée de la table entity';
    ELSE
        RAISE NOTICE 'Colonne image_id n''existe pas dans la table entity';
    END IF;
END $$;

-- 5. Table d'audit image_aud
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'image_aud') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'image_aud' AND column_name = 'entity_id'
        ) THEN
            ALTER TABLE image_aud ADD COLUMN entity_id BIGINT;
            RAISE NOTICE 'Colonne entity_id ajoutée à la table image_aud';
        END IF;
    END IF;
END $$;
