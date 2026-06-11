-- Pages statiques éditables (contact, mentions légales, accessibilité), une ligne par page et par langue
CREATE TABLE IF NOT EXISTS site_page (
    id BIGSERIAL PRIMARY KEY,
    page_code VARCHAR(50) NOT NULL,
    langue_code VARCHAR(10) NOT NULL,
    titre VARCHAR(1000),
    contenu TEXT,
    CONSTRAINT fk_site_page_langue FOREIGN KEY (langue_code) REFERENCES langue(code) ON DELETE CASCADE,
    CONSTRAINT uq_site_page_code_langue UNIQUE (page_code, langue_code)
);

CREATE INDEX IF NOT EXISTS idx_site_page_code ON site_page (page_code);
CREATE INDEX IF NOT EXISTS idx_site_page_langue_code ON site_page (langue_code);

INSERT INTO site_page (page_code, langue_code, titre, contenu)
SELECT 'contact', 'fr', 'Contact',
       '<p>Pour toute question relative à <strong>OpenTypo</strong>, vous pouvez contacter l''équipe du projet via les coordonnées de votre établissement ou référentiel.</p><p>Cette page est éditable par les administrateurs de la plateforme.</p>'
WHERE NOT EXISTS (SELECT 1 FROM site_page WHERE page_code = 'contact' AND langue_code = 'fr');

INSERT INTO site_page (page_code, langue_code, titre, contenu)
SELECT 'contact', 'en', 'Contact',
       '<p>For any question about <strong>OpenTypo</strong>, please contact the project team through your institution or referential contact points.</p><p>This page can be edited by platform administrators.</p>'
WHERE NOT EXISTS (SELECT 1 FROM site_page WHERE page_code = 'contact' AND langue_code = 'en');

INSERT INTO site_page (page_code, langue_code, titre, contenu)
SELECT 'legal', 'fr', 'Mentions légales',
       '<p><strong>Éditeur</strong> : CNRS — OpenTypo</p><p><strong>Hébergement</strong> : à compléter par l''administrateur.</p><p><strong>Propriété intellectuelle</strong> : les contenus diffusés sur cette plateforme sont soumis aux règles applicables à la recherche publique.</p>'
WHERE NOT EXISTS (SELECT 1 FROM site_page WHERE page_code = 'legal' AND langue_code = 'fr');

INSERT INTO site_page (page_code, langue_code, titre, contenu)
SELECT 'legal', 'en', 'Legal notice',
       '<p><strong>Publisher</strong>: CNRS — OpenTypo</p><p><strong>Hosting</strong>: to be completed by the administrator.</p><p><strong>Intellectual property</strong>: content published on this platform is subject to applicable public research rules.</p>'
WHERE NOT EXISTS (SELECT 1 FROM site_page WHERE page_code = 'legal' AND langue_code = 'en');

INSERT INTO site_page (page_code, langue_code, titre, contenu)
SELECT 'accessibility', 'fr', 'Accessibilité',
       '<p>OpenTypo vise la conformité aux recommandations d''accessibilité numérique (RGAA).</p><p>Si vous rencontrez un obstacle à l''accès à un contenu ou à une fonctionnalité, merci de le signaler à l''équipe projet afin que des améliorations soient apportées.</p>'
WHERE NOT EXISTS (SELECT 1 FROM site_page WHERE page_code = 'accessibility' AND langue_code = 'fr');

INSERT INTO site_page (page_code, langue_code, titre, contenu)
SELECT 'accessibility', 'en', 'Accessibility',
       '<p>OpenTypo aims to comply with digital accessibility guidelines.</p><p>If you encounter any barrier when accessing content or features, please report it to the project team so improvements can be made.</p>'
WHERE NOT EXISTS (SELECT 1 FROM site_page WHERE page_code = 'accessibility' AND langue_code = 'en');
