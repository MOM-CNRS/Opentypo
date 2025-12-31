package fr.cnrs.opentypo.service;

import fr.cnrs.opentypo.entity.Utilisateur;
import fr.cnrs.opentypo.repository.UtilisateurRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Optional;

/**
 * Service pour la gestion des utilisateurs
 */
@Slf4j
@Service
public class UtilisateurService implements Serializable {

    @Inject
    private UtilisateurRepository utilisateurRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
        
        // Vérifier le mot de passe avec BCrypt
        if (passwordEncoder.matches(password, utilisateur.getPasswordHash())) {
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
     * Encode un mot de passe avec BCrypt
     * 
     * @param rawPassword Le mot de passe en clair
     * @return Le hash du mot de passe
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Vérifie si un mot de passe correspond à un hash
     * 
     * @param rawPassword Le mot de passe en clair
     * @param encodedPassword Le hash du mot de passe
     * @return true si le mot de passe correspond, false sinon
     */
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
