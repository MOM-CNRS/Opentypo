-- Migration V34: Ajouter display_order à entity pour l'ordre personnalisé des collections (page d'accueil)
-- Ordre par défaut : alphabétique (display_order NULL = tri par label)

ALTER TABLE entity ADD COLUMN IF NOT EXISTS display_order INTEGER;

-- Table d'audit Envers
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'entity_aud') THEN
        ALTER TABLE entity_aud ADD COLUMN IF NOT EXISTS display_order INTEGER;
    END IF;
END $$;
