package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Optional;

/**
 * Service pour la gestion des utilisateurs.
 * Les mots de passe sont hashés avec Argon2id. Les hash BCrypt existants restent acceptés à la vérification.
 */
@Slf4j
@Service
public class UtilisateurService implements Serializable {

    @Inject
    private UtilisateurRepository utilisateurRepository;

    /** Encodeur principal : Argon2id (recommandé OWASP). */
    private final PasswordEncoder argon2Encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    /** Pour vérifier les anciens mots de passe hashés en BCrypt. */
    private final PasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();

    /**
     * Authentifie un utilisateur par son email et son mot de passe
     * 
     * @param email L'email de l'utilisateur
     * @param password Le mot de passe en clair
     * @return L'utilisateur authentifié ou Optional.empty() si l'authentification échoue
     */
    public Optional<Utilisateur> authenticate(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            log.warn("Tentative d'authentification avec des identifiants vides");
            return Optional.empty();
        }

        // Rechercher l'utilisateur par email
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByEmail(email.trim());
        
        if (utilisateurOpt.isEmpty()) {
            log.warn("Tentative d'authentification avec un email inexistant: {}", email);
            return Optional.empty();
        }

        Utilisateur utilisateur = utilisateurOpt.get();
        
        // Vérifier si le compte est actif
        if (utilisateur.getActive() == null || !utilisateur.getActive()) {
            log.warn("Tentative de connexion avec un compte désactivé: {}", email);
            return Optional.empty();
        }
        
        // Vérifier le mot de passe (Argon2id ou BCrypt pour les anciens hash)
        if (matchesPassword(password, utilisateur.getPasswordHash())) {
            log.info("Authentification réussie pour l'utilisateur: {}", email);
            return Optional.of(utilisateur);
        } else {
            log.warn("Échec d'authentification - mot de passe incorrect pour l'utilisateur: {}", email);
            return Optional.empty();
        }
    }

    /**
     * Trouve un utilisateur par son email
     * 
     * @param email L'email de l'utilisateur
     * @return L'utilisateur trouvé ou Optional.empty()
     */
    public Optional<Utilisateur> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return utilisateurRepository.findByEmail(email.trim());
    }

    /**
     * Vérifie si un utilisateur existe avec l'email donné
     * 
     * @param email L'email à vérifier
     * @return true si un utilisateur existe, false sinon
     */
    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return utilisateurRepository.existsByEmail(email.trim());
    }

    /**
     * Encode un mot de passe avec Argon2id.
     *
     * @param rawPassword Le mot de passe en clair
     * @return Le hash du mot de passe (format Argon2id)
     */
    public String encodePassword(String rawPassword) {
        return argon2Encoder.encode(rawPassword);
    }

    /**
     * Vérifie si un mot de passe correspond à un hash (Argon2id ou BCrypt).
     *
     * @param rawPassword Le mot de passe en clair
     * @param encodedPassword Le hash stocké (Argon2id ou BCrypt)
     * @return true si le mot de passe correspond, false sinon
     */
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$")) {
            return bcryptEncoder.matches(rawPassword, encodedPassword);
        }
        return argon2Encoder.matches(rawPassword, encodedPassword);
    }
}
