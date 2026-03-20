-- Table site_presentation : contenu de la présentation du site par langue (page d'accueil)
-- Les langues disponibles sont celles présentes dans la table langue
CREATE TABLE IF NOT EXISTS site_presentation (
    id BIGSERIAL PRIMARY KEY,
    langue_code VARCHAR(10) NOT NULL,
    titre VARCHAR(255),
    description TEXT,
    CONSTRAINT fk_site_presentation_langue FOREIGN KEY (langue_code) REFERENCES langue(code) ON DELETE CASCADE,
    CONSTRAINT uq_site_presentation_langue UNIQUE (langue_code)
);

CREATE INDEX IF NOT EXISTS idx_site_presentation_langue_code ON site_presentation(langue_code);

-- Contenu par défaut en français
INSERT INTO site_presentation (langue_code, titre, description)
SELECT 'fr',
       'À propos d''Opentypo',
       'Opentypo est une plateforme de recherche et de gestion de typologies archéologiques développée par le CNRS. Cette application permet aux chercheurs de consulter, gérer et enrichir une base de données complète de types archéologiques, facilitant ainsi la recherche scientifique et la documentation du patrimoine archéologique.'
WHERE NOT EXISTS (SELECT 1 FROM site_presentation WHERE langue_code = 'fr');
