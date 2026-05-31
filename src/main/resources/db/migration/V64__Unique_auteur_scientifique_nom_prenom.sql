-- Unicité nom + prénom (comparaison insensible à la casse, espaces en bordure ignorés)
CREATE UNIQUE INDEX IF NOT EXISTS uk_auteur_scientifique_nom_prenom_ci
    ON auteur_scientifique (LOWER(TRIM(nom)), LOWER(TRIM(prenom)));
