package fr.cnrs.opentypo.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilitaire pour générer des hash de mots de passe BCrypt
 * Utilisez cette classe pour générer de nouveaux hash si nécessaire
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Générer un hash pour le mot de passe "admin"
        String password = "admin";
        String hash = encoder.encode(password);
        
        System.out.println("========================================");
        System.out.println("Génération de hash BCrypt");
        System.out.println("========================================");
        System.out.println("Mot de passe: " + password);
        System.out.println("Hash BCrypt: " + hash);
        System.out.println("========================================");
        System.out.println();
        System.out.println("Pour mettre à jour le script Flyway, remplacez le hash dans:");
        System.out.println("src/main/resources/db/migration/V1__Initial_groups_and_admin_user.sql");
        System.out.println("ligne 25, avec le nouveau hash ci-dessus.");
    }
}

