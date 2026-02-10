package fr.cnrs.opentypo.common.util;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/**
 * Utilitaire pour générer des hash de mots de passe Argon2id.
 * Utilisez cette classe pour générer de nouveaux hash (migrations Flyway, utilisateurs initiaux).
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        String password = args.length > 0 ? args[0] : "admin";
        String hash = encoder.encode(password);

        System.out.println("========================================");
        System.out.println("Génération de hash Argon2id");
        System.out.println("========================================");
        System.out.println("Mot de passe: " + password);
        System.out.println("Hash Argon2id: " + hash);
        System.out.println("========================================");
        System.out.println();
        System.out.println("Pour une migration Flyway, mettez à jour la colonne password_hash");
        System.out.println("avec le hash ci-dessus (ex. UPDATE utilisateur SET password_hash = '...' WHERE email = 'admin';).");
    }
}

