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

-- Création de l'utilisateur administrateur par défaut
-- Identifiants : email = admin, mot de passe = admin
-- Hash BCrypt (l'application accepte aussi Argon2id ; pour un hash Argon2id, exécuter
-- fr.cnrs.opentypo.common.util.PasswordHashGenerator avec l'arg "admin" puis mettre à jour la colonne)
INSERT INTO utilisateur (nom, prenom, email, password_hash, groupe_id, create_date, create_by, active)
SELECT
    'Administrateur',
    'Système',
    'admin',
     '$2a$10$hd6imws1NiYhSIe6JVeyqu8qS6Uz/gbsvKB0OOuQaN02mObbli1H.',
    g.id,
    CURRENT_TIMESTAMP,
    'SYSTEM',
    true
FROM groupe g
WHERE g.nom = 'Administrateur'
  AND NOT EXISTS (SELECT 1 FROM utilisateur u WHERE u.email = 'admin');
