CREATE TABLE IF NOT EXISTS entity_auteur_scientifique (
    entity_id BIGINT NOT NULL,
    auteur_scientifique_id BIGINT NOT NULL,
    PRIMARY KEY (entity_id, auteur_scientifique_id),
    CONSTRAINT fk_entity_auteur_scientifique_entity
        FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT fk_entity_auteur_scientifique_author
        FOREIGN KEY (auteur_scientifique_id) REFERENCES auteur_scientifique(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_entity_auteur_scientifique_entity_id
    ON entity_auteur_scientifique (entity_id);

CREATE INDEX IF NOT EXISTS idx_entity_auteur_scientifique_author_id
    ON entity_auteur_scientifique (auteur_scientifique_id);
