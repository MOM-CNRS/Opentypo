-- Migration Flyway: Insertion des données initiales
-- Version: 1
-- Description: Insère les groupes Administrateur, Éditeur, Lecteur et l'utilisateur admin par défaut
-- Note: Les tables sont créées par Hibernate à partir des entités JPA

-- Création des groupes d'utilisateurs
INSERT INTO groupe (nom)
SELECT 'Administrateur'
WHERE NOT EXISTS (SELECT 1 FROM groupe WHERE nom = 'Administrateur');

INSERT INTO groupe (nom)
SELECT 'Éditeur'
WHERE NOT EXISTS (SELECT 1 FROM groupe WHERE nom = 'Éditeur');

INSERT INTO groupe (nom)
SELECT 'Lecteur'
WHERE NOT EXISTS (SELECT 1 FROM groupe WHERE nom = 'Lecteur');

-- Création de l'utilisateur administrateur par défaut (login: admin, password: admin)
INSERT INTO utilisateur (nom, prenom, email, password_hash, groupe_id, create_date, create_by)
SELECT 
    'Administrateur' as nom,
    'Système' as prenom,
    'admin' as email,
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' as password_hash,
    g.id as groupe_id,
    CURRENT_TIMESTAMP as create_date,
    'SYSTEM' as create_by
FROM groupe g
WHERE g.nom = 'Administrateur'
AND NOT EXISTS (SELECT 1 FROM utilisateur u WHERE u.email = 'admin');

