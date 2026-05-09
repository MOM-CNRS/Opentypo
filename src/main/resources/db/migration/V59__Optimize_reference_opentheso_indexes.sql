-- Optimisation non destructive de la table reference-opentheso
-- NOTE:
--  - Pas de suppression de lignes ici (risque FK vers caracteristique_physique, etc.)
--  - Pas de contrainte UNIQUE ici (peut échouer tant que des doublons existent)

-- 1) Index d'accès (entity + code), utile pour les chargements de listes d'une entité.
CREATE INDEX IF NOT EXISTS idx_reference_opentheso_entity_code
    ON "reference-opentheso"(entity_id, code);

-- 2) Index de lookup OpenTheso (code + concept_id), utile pour résolutions / comparaisons.
CREATE INDEX IF NOT EXISTS idx_reference_opentheso_code_concept
    ON "reference-opentheso"(code, concept_id);

-- 3) Index de recherche par libellé dans un code.
CREATE INDEX IF NOT EXISTS idx_reference_opentheso_code_valeur
    ON "reference-opentheso"(code, valeur);

-- 4) Index d'aide pour analyse de doublons / filtres normalisés.
CREATE INDEX IF NOT EXISTS idx_reference_opentheso_entity_code_valeur_norm
    ON "reference-opentheso"(entity_id, code, lower(trim(valeur)));

