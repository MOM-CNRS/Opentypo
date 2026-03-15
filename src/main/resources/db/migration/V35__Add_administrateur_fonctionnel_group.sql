-- Migration V35: Ajout du groupe "Administrateur fonctionnel"
-- Même comportement que "Administrateur technique" pour le moment

INSERT INTO groupe (nom)
SELECT 'Administrateur fonctionnel'
WHERE NOT EXISTS (SELECT 1 FROM groupe WHERE nom = 'Administrateur fonctionnel');
