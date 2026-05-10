-- Paramétrage ARK par référentiel (NAAN, épaule, URL de résolution optionnelle).
-- Si NULL, l'application utilise opentypo.ark.* (application.yaml).

ALTER TABLE parametrage ADD COLUMN IF NOT EXISTS ark_naan VARCHAR(32);
ALTER TABLE parametrage ADD COLUMN IF NOT EXISTS ark_shoulder VARCHAR(128);
ALTER TABLE parametrage ADD COLUMN IF NOT EXISTS ark_resolver_base VARCHAR(512);
