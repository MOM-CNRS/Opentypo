ALTER TABLE entity_metadata
    ADD COLUMN IF NOT EXISTS relation_imitation TEXT;

ALTER TABLE entity_metadata_aud
    ADD COLUMN IF NOT EXISTS relation_imitation TEXT;
