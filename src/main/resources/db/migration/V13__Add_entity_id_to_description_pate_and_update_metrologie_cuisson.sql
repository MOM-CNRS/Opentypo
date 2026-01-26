-- Migration pour ajouter entity_id à description_pate et adapter métrologie/cuisson
-- Version: 13
-- Description: Ajoute entity_id à description_pate et change métrologie/cuisson en référence_id

-- Ajouter entity_id à description_pate si elle n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'description_pate' AND column_name = 'entity_id'
    ) THEN
        ALTER TABLE description_pate ADD COLUMN entity_id BIGINT;
        
        -- Ajouter une contrainte de clé étrangère
        ALTER TABLE description_pate 
        ADD CONSTRAINT fk_description_pate_entity 
        FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE;
        
        -- Créer un index pour améliorer les performances
        CREATE INDEX IF NOT EXISTS idx_description_pate_entity_id ON description_pate(entity_id);
        
        -- Ajouter contrainte unique pour garantir une seule description_pate par entité
        ALTER TABLE description_pate
        ADD CONSTRAINT uk_description_pate_entity_id UNIQUE (entity_id);
        
        RAISE NOTICE 'Colonne entity_id ajoutée à la table description_pate';
    ELSE
        RAISE NOTICE 'Colonne entity_id existe déjà dans la table description_pate';
    END IF;
END $$;

-- Changer métrologie de TEXT à référence_id dans caracteristique_physique
DO $$
BEGIN
    -- Vérifier si la colonne metrologie_id existe déjà
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'caracteristique_physique' 
        AND column_name = 'metrologie_id'
    ) THEN
        -- Vérifier si la colonne metrologie existe et est de type TEXT
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'caracteristique_physique' 
            AND column_name = 'metrologie' 
            AND data_type = 'text'
        ) THEN
            -- Renommer l'ancienne colonne
            ALTER TABLE caracteristique_physique RENAME COLUMN metrologie TO metrologie_old;
        END IF;
        
        -- Créer la nouvelle colonne metrologie_id
        ALTER TABLE caracteristique_physique ADD COLUMN metrologie_id BIGINT;
        
        -- Ajouter une contrainte de clé étrangère si elle n'existe pas déjà
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE table_name = 'caracteristique_physique'
            AND constraint_name = 'fk_caracteristique_physique_metrologie'
        ) THEN
            ALTER TABLE caracteristique_physique 
            ADD CONSTRAINT fk_caracteristique_physique_metrologie 
            FOREIGN KEY (metrologie_id) REFERENCES "reference-opentheso"(id) ON DELETE SET NULL;
        END IF;
        
        RAISE NOTICE 'Colonne metrologie_id ajoutée à caracteristique_physique';
    ELSE
        RAISE NOTICE 'Colonne metrologie_id existe déjà dans caracteristique_physique';
    END IF;
END $$;

-- Changer cuisson de TEXT à référence_id dans description_pate
DO $$
BEGIN
    -- Vérifier si la colonne cuisson_id existe déjà
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'description_pate' 
        AND column_name = 'cuisson_id'
    ) THEN
        -- Vérifier si la colonne cuisson existe et est de type TEXT/varchar
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'description_pate' 
            AND column_name = 'cuisson'
        ) THEN
            -- Renommer l'ancienne colonne
            ALTER TABLE description_pate RENAME COLUMN cuisson TO cuisson_old;
        END IF;
        
        -- Créer la nouvelle colonne cuisson_id
        ALTER TABLE description_pate ADD COLUMN cuisson_id BIGINT;
        
        -- Ajouter une contrainte de clé étrangère si elle n'existe pas déjà
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE table_name = 'description_pate'
            AND constraint_name = 'fk_description_pate_cuisson'
        ) THEN
            ALTER TABLE description_pate 
            ADD CONSTRAINT fk_description_pate_cuisson 
            FOREIGN KEY (cuisson_id) REFERENCES "reference-opentheso"(id) ON DELETE SET NULL;
        END IF;
        
        RAISE NOTICE 'Colonne cuisson_id ajoutée à description_pate';
    ELSE
        RAISE NOTICE 'Colonne cuisson_id existe déjà dans description_pate';
    END IF;
    
    -- Modifier la colonne description pour permettre NULL (car elle peut être vide lors de la création)
    -- Vérifier d'abord si la contrainte NOT NULL existe
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'description_pate' 
        AND column_name = 'description'
        AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE description_pate ALTER COLUMN description DROP NOT NULL;
        RAISE NOTICE 'Contrainte NOT NULL supprimée de la colonne description dans description_pate';
    END IF;
END $$;
