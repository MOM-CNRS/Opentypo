-- Migration V30: Ajouter display_order à entity_relation pour l'ordre personnalisé des types
-- Ordre par défaut : alphabétique croissant (display_order NULL = tri par code)
ALTER TABLE entity_relation ADD COLUMN IF NOT EXISTS display_order INTEGER;

-- Table d'audit Envers si elle existe
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'entity_relation_aud') THEN
        ALTER TABLE entity_relation_aud ADD COLUMN IF NOT EXISTS display_order INTEGER;
    END IF;
END $$;
