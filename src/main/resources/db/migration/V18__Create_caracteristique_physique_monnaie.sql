-- Migration V18: Caractéristiques physiques pour la collection MONNAIE
CREATE TABLE IF NOT EXISTS caracteristique_physique_monnaie (
    id BIGSERIAL PRIMARY KEY,
    entity_id BIGINT NOT NULL UNIQUE,
    materiau_id BIGINT,
    denomination_id BIGINT,
    metrologie TEXT,
    valeur_id BIGINT,
    technique_id BIGINT,
    fabrication_id BIGINT,
    CONSTRAINT fk_cpm_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT fk_cpm_materiau FOREIGN KEY (materiau_id) REFERENCES reference_opentheso(id),
    CONSTRAINT fk_cpm_denomination FOREIGN KEY (denomination_id) REFERENCES reference_opentheso(id),
    CONSTRAINT fk_cpm_valeur FOREIGN KEY (valeur_id) REFERENCES reference_opentheso(id),
    CONSTRAINT fk_cpm_technique FOREIGN KEY (technique_id) REFERENCES reference_opentheso(id),
    CONSTRAINT fk_cpm_fabrication FOREIGN KEY (fabrication_id) REFERENCES reference_opentheso(id)
);

CREATE INDEX IF NOT EXISTS idx_cpm_entity_id ON caracteristique_physique_monnaie(entity_id);

-- Table d'audit Envers
CREATE TABLE IF NOT EXISTS caracteristique_physique_monnaie_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    entity_id BIGINT,
    materiau_id BIGINT,
    denomination_id BIGINT,
    metrologie TEXT,
    valeur_id BIGINT,
    technique_id BIGINT,
    fabrication_id BIGINT,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_cpm_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);
