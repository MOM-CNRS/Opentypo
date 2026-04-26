ALTER TABLE entity_metadata
    ADD COLUMN IF NOT EXISTS denomination_instrumentum TEXT;

ALTER TABLE entity_metadata_aud
    ADD COLUMN IF NOT EXISTS denomination_instrumentum TEXT;
