CREATE TABLE IF NOT EXISTS internal_alignment (
    source_type_id BIGINT NOT NULL,
    target_type_id BIGINT NOT NULL,
    match_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (source_type_id, target_type_id),
    CONSTRAINT fk_internal_alignment_source_type
        FOREIGN KEY (source_type_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT fk_internal_alignment_target_type
        FOREIGN KEY (target_type_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT ck_internal_alignment_no_self
        CHECK (source_type_id <> target_type_id),
    CONSTRAINT ck_internal_alignment_match_type
        CHECK (match_type IN ('ExactMatch', 'CloseMatch'))
);

CREATE INDEX IF NOT EXISTS idx_internal_alignment_source_type
    ON internal_alignment (source_type_id);

CREATE INDEX IF NOT EXISTS idx_internal_alignment_target_type
    ON internal_alignment (target_type_id);
