-- Migration V22: Simplification des groupes
-- - Conserver uniquement "Administrateur technique"
-- - Ajouter le groupe "Utilisateur"
-- - Réaffecter les utilisateurs des autres groupes vers "Utilisateur"
-- - Supprimer tous les groupes sauf "Administrateur technique" et "Utilisateur"

-- 1. Créer le groupe "Utilisateur" s'il n'existe pas
INSERT INTO groupe (nom)
SELECT 'Utilisateur'
WHERE NOT EXISTS (SELECT 1 FROM groupe WHERE nom = 'Utilisateur');

-- 2. Réaffecter vers "Utilisateur" tous les utilisateurs qui ne sont pas "Administrateur technique"
UPDATE utilisateur u
SET groupe_id = (SELECT g.id FROM groupe g WHERE g.nom = 'Utilisateur' LIMIT 1)
WHERE u.groupe_id IN (SELECT g.id FROM groupe g WHERE g.nom != 'Administrateur technique');

-- 3. Supprimer les groupes sauf "Administrateur technique" et "Utilisateur"
DELETE FROM groupe
WHERE nom NOT IN ('Administrateur technique', 'Utilisateur');
