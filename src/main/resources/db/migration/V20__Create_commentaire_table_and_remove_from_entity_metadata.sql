-- Migration V20: Créer la table commentaire et supprimer le champ commentaire de entity_metadata
-- Chaque entité peut avoir un ou plusieurs commentaires, chaque commentaire est créé par un utilisateur

-- 1. Créer la table commentaire
CREATE TABLE IF NOT EXISTS commentaire (
    id BIGSERIAL PRIMARY KEY,
    entity_id BIGINT NOT NULL,
    utilisateur_id BIGINT,
    contenu TEXT NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_commentaire_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT fk_commentaire_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_commentaire_entity_id ON commentaire(entity_id);
CREATE INDEX IF NOT EXISTS idx_commentaire_utilisateur_id ON commentaire(utilisateur_id);
CREATE INDEX IF NOT EXISTS idx_commentaire_date_creation ON commentaire(date_creation);

-- 2. Migrer les données existantes de entity_metadata vers la nouvelle table (seulement si la colonne existe)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'entity_metadata'
          AND column_name = 'commentaire'
    ) THEN
        INSERT INTO commentaire (entity_id, utilisateur_id, contenu, date_creation)
        SELECT
            em.entity_id,
            (SELECT u.id FROM utilisateur u
             JOIN groupe g ON u.groupe_id = g.id
             WHERE g.nom = 'Administrateur'
             LIMIT 1),
            em.commentaire,
            CURRENT_TIMESTAMP
        FROM entity_metadata em
        WHERE em.commentaire IS NOT NULL
          AND TRIM(em.commentaire) != '';
    END IF;
END $$;

-- 3. Supprimer la colonne commentaire de entity_metadata
ALTER TABLE entity_metadata DROP COLUMN IF EXISTS commentaire;

-- 4. Mise à jour de la table d'audit Hibernate Envers
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'entity_metadata_aud') THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'entity_metadata_aud' AND column_name = 'commentaire'
        ) THEN
            ALTER TABLE entity_metadata_aud DROP COLUMN commentaire;
            RAISE NOTICE 'Colonne commentaire supprimée de entity_metadata_aud';
        END IF;
    END IF;
END $$;