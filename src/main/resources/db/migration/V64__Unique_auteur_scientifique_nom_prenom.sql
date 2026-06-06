-- Fusionne les auteurs en double (nom/prénom identiques modulo casse et espaces),
-- puis impose l'unicité.

CREATE TEMP TABLE auteur_scientifique_dedup_map AS
SELECT a.id AS duplicate_id,
       canonical.keep_id AS keep_id
FROM auteur_scientifique a
JOIN (
    SELECT MIN(id) AS keep_id,
           LOWER(TRIM(nom)) AS norm_nom,
           LOWER(TRIM(prenom)) AS norm_prenom
    FROM auteur_scientifique
    GROUP BY LOWER(TRIM(nom)), LOWER(TRIM(prenom))
) canonical
  ON LOWER(TRIM(a.nom)) = canonical.norm_nom
 AND LOWER(TRIM(a.prenom)) = canonical.norm_prenom
WHERE a.id <> canonical.keep_id;

-- Repointe les liaisons entité → auteur conservé (id le plus petit)
UPDATE entity_auteur_scientifique eas
SET auteur_scientifique_id = m.keep_id
FROM auteur_scientifique_dedup_map m
WHERE eas.auteur_scientifique_id = m.duplicate_id
  AND NOT EXISTS (
      SELECT 1
      FROM entity_auteur_scientifique existing
      WHERE existing.entity_id = eas.entity_id
        AND existing.auteur_scientifique_id = m.keep_id
  );

-- Supprime les liaisons devenues redondantes
DELETE FROM entity_auteur_scientifique eas
USING auteur_scientifique_dedup_map m
WHERE eas.auteur_scientifique_id = m.duplicate_id;

-- Conserve un auteur actif si l'un des doublons l'était
UPDATE auteur_scientifique keep
SET active = TRUE
FROM auteur_scientifique_dedup_map m
JOIN auteur_scientifique dup ON dup.id = m.duplicate_id
WHERE keep.id = m.keep_id
  AND dup.active = TRUE
  AND keep.active = FALSE;

-- Supprime les doublons
DELETE FROM auteur_scientifique a
USING auteur_scientifique_dedup_map m
WHERE a.id = m.duplicate_id;

DROP TABLE auteur_scientifique_dedup_map;

-- Normalise les espaces en bordure
UPDATE auteur_scientifique
SET nom = TRIM(nom),
    prenom = TRIM(prenom);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auteur_scientifique_nom_prenom_ci
    ON auteur_scientifique (LOWER(TRIM(nom)), LOWER(TRIM(prenom)));
