CREATE TABLE IF NOT EXISTS external_alignment (
    id BIGSERIAL PRIMARY KEY,
    source_type_id BIGINT NOT NULL,
    label VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    match_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_external_alignment_source_type
        FOREIGN KEY (source_type_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT ck_external_alignment_match_type
        CHECK (match_type IN ('ExactMatch', 'CloseMatch'))
);

CREATE INDEX IF NOT EXISTS idx_external_alignment_source_type
    ON external_alignment (source_type_id);
