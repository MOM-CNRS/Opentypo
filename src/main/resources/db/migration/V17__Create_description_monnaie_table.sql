-- Migration V17: Création de la table description_monnaie pour les entités de la collection MONNAIE
CREATE TABLE IF NOT EXISTS description_monnaie (
    id BIGSERIAL PRIMARY KEY,
    entity_id BIGINT NOT NULL UNIQUE,
    droit TEXT,
    legende_droit TEXT,
    coins_monetaires_droit TEXT,
    revers TEXT,
    legende_revers TEXT,
    coins_monetaires_revers TEXT,
    CONSTRAINT fk_description_monnaie_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_description_monnaie_entity_id ON description_monnaie(entity_id);

-- Table d'audit pour Envers
CREATE TABLE IF NOT EXISTS description_monnaie_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    entity_id BIGINT,
    droit TEXT,
    legende_droit TEXT,
    coins_monetaires_droit TEXT,
    revers TEXT,
    legende_revers TEXT,
    coins_monetaires_revers TEXT,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_description_monnaie_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);
