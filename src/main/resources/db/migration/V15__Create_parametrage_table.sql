-- Table parametrage : paramétrage OpenTheso par collection (entity)
CREATE TABLE IF NOT EXISTS parametrage (
    id BIGSERIAL PRIMARY KEY,
    entity_id BIGINT NOT NULL,
    base_url VARCHAR(500),
    id_theso VARCHAR(100),
    id_groupe VARCHAR(100),
    id_langue VARCHAR(20),
    CONSTRAINT fk_parametrage_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT uq_parametrage_entity UNIQUE (entity_id)
);

CREATE INDEX IF NOT EXISTS idx_parametrage_entity_id ON parametrage(entity_id);
