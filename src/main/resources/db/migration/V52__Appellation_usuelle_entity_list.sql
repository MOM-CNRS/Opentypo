-- Appellation usuelle : plusieurs références OpenTheso liées à l'entité (code APPELLATION_USUELLE).
-- Suppression du lien FK entity_metadata → reference-opentheso (les lignes ont déjà entity_id depuis V51).

ALTER TABLE entity_metadata DROP CONSTRAINT IF EXISTS fk_entity_metadata_appellation_opentheso;

DROP INDEX IF EXISTS idx_entity_metadata_appellation_opentheso_id;

ALTER TABLE entity_metadata DROP COLUMN IF EXISTS appellation_opentheso_id;

ALTER TABLE entity_metadata_aud DROP COLUMN IF EXISTS appellation_opentheso_id;
