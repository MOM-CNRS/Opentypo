ALTER TABLE entity_metadata
    ADD COLUMN IF NOT EXISTS corpus_lies TEXT;

ALTER TABLE entity_metadata_aud
    ADD COLUMN IF NOT EXISTS corpus_lies TEXT;
