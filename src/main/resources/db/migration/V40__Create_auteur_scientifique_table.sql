CREATE TABLE IF NOT EXISTS auteur_scientifique (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(120) NOT NULL,
    prenom VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_auteur_scientifique_nom_prenom ON auteur_scientifique (nom, prenom);
