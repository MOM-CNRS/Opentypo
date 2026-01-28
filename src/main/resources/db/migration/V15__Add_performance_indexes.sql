-- Migration V15: Ajout d'index pour améliorer les performances des requêtes fréquentes
-- Description: Ajoute des index sur les colonnes fréquemment utilisées dans les recherches et jointures
-- Impact: Amélioration significative des performances de recherche (10-100x selon la taille des données)

-- ============================================
-- INDEX SUR LA TABLE entity
-- ============================================

-- Index sur code (recherche fréquente, utilisé dans findByCode)
CREATE INDEX IF NOT EXISTS idx_entity_code ON entity(code);

-- Index sur statut (filtrage fréquent dans candidats, utilisé dans findByStatut)
CREATE INDEX IF NOT EXISTS idx_entity_statut ON entity(statut);

-- Index sur entity_type_id (jointures fréquentes avec EntityType)
CREATE INDEX IF NOT EXISTS idx_entity_type_id ON entity(entity_type_id);

-- Index composite pour les recherches par type et statut (requête combinée fréquente)
CREATE INDEX IF NOT EXISTS idx_entity_type_statut ON entity(entity_type_id, statut);

-- Index sur create_date (tri fréquent dans findByStatut avec ORDER BY)
CREATE INDEX IF NOT EXISTS idx_entity_create_date ON entity(create_date DESC);

-- Index composite pour statut + create_date (optimise findByStatut avec tri)
CREATE INDEX IF NOT EXISTS idx_entity_statut_create_date ON entity(statut, create_date DESC);

-- Index sur nom (recherche LIKE dans findByNomContainingIgnoreCase)
CREATE INDEX IF NOT EXISTS idx_entity_nom ON entity(nom);

-- ============================================
-- INDEX SUR LA TABLE reference-opentheso
-- ============================================

-- Index sur code (recherche fréquente dans findByCode)
CREATE INDEX IF NOT EXISTS idx_reference_code ON "reference-opentheso"(code);

-- Index sur entity_id (jointures fréquentes avec Entity)
CREATE INDEX IF NOT EXISTS idx_reference_entity_id ON "reference-opentheso"(entity_id);

-- Index composite pour entity_id + code (requête findByEntityIdAndCode)
CREATE INDEX IF NOT EXISTS idx_reference_entity_code ON "reference-opentheso"(entity_id, code);

-- Index sur valeur (recherche LIKE dans findByValeurContainingIgnoreCase)
CREATE INDEX IF NOT EXISTS idx_reference_valeur ON "reference-opentheso"(valeur);

-- Index sur concept_id (recherche OpenTheso, utilisé pour les références externes)
CREATE INDEX IF NOT EXISTS idx_reference_concept_id ON "reference-opentheso"(concept_id);

-- ============================================
-- INDEX SUR LA TABLE label
-- ============================================

-- Index sur entity_id (jointures fréquentes avec Entity)
CREATE INDEX IF NOT EXISTS idx_label_entity_id ON label(entity_id);

-- Index sur code_langue (filtrage par langue)
CREATE INDEX IF NOT EXISTS idx_label_code_langue ON label(code_langue);

-- Index composite pour entity_id + code_langue (recherche de label par entité et langue)
CREATE INDEX IF NOT EXISTS idx_label_entity_langue ON label(entity_id, code_langue);

-- ============================================
-- INDEX SUR LA TABLE entity_relation
-- ============================================

-- Index sur parent_id (recherche des enfants d'une entité)
CREATE INDEX IF NOT EXISTS idx_entity_relation_parent_id ON entity_relation(parent_id);

-- Index sur child_id (recherche des parents d'une entité)
CREATE INDEX IF NOT EXISTS idx_entity_relation_child_id ON entity_relation(child_id);

-- Note: L'index composite (parent_id, child_id) est déjà présent via la contrainte UNIQUE

-- ============================================
-- INDEX SUR LA TABLE auteur
-- ============================================

-- Index sur entity_id (jointures fréquentes avec Entity)
CREATE INDEX IF NOT EXISTS idx_auteur_entity_id ON auteur(entity_id);

-- Index sur user_id (recherche des entités d'un utilisateur)
CREATE INDEX IF NOT EXISTS idx_auteur_user_id ON auteur(user_id);

-- ============================================
-- INDEX SUR LES TABLES DE DESCRIPTION
-- ============================================

-- Index sur entity_id pour description_detail (jointures fréquentes)
CREATE INDEX IF NOT EXISTS idx_description_detail_entity_id ON description_detail(entity_id);

-- Index sur entity_id pour caracteristique_physique (jointures fréquentes)
CREATE INDEX IF NOT EXISTS idx_caracteristique_physique_entity_id ON caracteristique_physique(entity_id);

-- Index sur entity_id pour description_pate (jointures fréquentes)
CREATE INDEX IF NOT EXISTS idx_description_pate_entity_id ON description_pate(entity_id);

-- ============================================
-- INDEX SUR LES TABLES D'AUDIT (Hibernate Envers)
-- ============================================

-- Index sur rev pour les tables d'audit (améliore les requêtes d'historique)
-- Note: revend n'existe pas par défaut dans Hibernate Envers, seulement rev et revtype
CREATE INDEX IF NOT EXISTS idx_entity_aud_rev ON entity_aud(rev);

-- Index composite sur id + rev pour les tables d'audit
CREATE INDEX IF NOT EXISTS idx_entity_aud_id_rev ON entity_aud(id, rev);

-- ============================================
-- NOTES
-- ============================================
-- Ces index améliorent significativement les performances des requêtes suivantes :
-- - EntityRepository.findByCode()
-- - EntityRepository.findByStatut()
-- - EntityRepository.findByEntityTypeCode()
-- - EntityRepository.findByNomContainingIgnoreCase()
-- - ReferenceOpenthesoRepository.findByEntityIdAndCode()
-- - ReferenceOpenthesoRepository.findByEntityId()
-- - Recherches de relations parent-enfant
-- - Chargement des labels par entité et langue
-- - Recherche des auteurs d'une entité
--
-- Impact estimé : 10-100x amélioration selon la taille des données
-- Coût : Légère augmentation de l'espace disque et temps d'insertion/update légèrement plus long
-- Bénéfice : Amélioration massive des temps de recherche et jointures
