-- Table d'audit Hibernate Envers pour l'entité ReferenceOpentheso (@Table(name = "reference-opentheso")).
-- Sans cette table, toute persistance de lignes reference-opentheso (ex. import, période/production)
-- provoque : ERROR: relation "reference-opentheso_aud" does not exist

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'reference-opentheso_aud'
    ) THEN
        CREATE TABLE "reference-opentheso_aud" (
            id BIGINT NOT NULL,
            rev INTEGER NOT NULL,
            revtype SMALLINT,
            code VARCHAR(255),
            valeur VARCHAR(500),
            thesaurus_id VARCHAR(255),
            concept_id VARCHAR(255),
            collection_id VARCHAR(255),
            url VARCHAR(500),
            entity_id BIGINT,
            PRIMARY KEY (id, rev),
            CONSTRAINT fk_reference_opentheso_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
        );
        RAISE NOTICE 'Table reference-opentheso_aud créée';
    ELSE
        RAISE NOTICE 'Table reference-opentheso_aud existe déjà';
    END IF;
END $$;
