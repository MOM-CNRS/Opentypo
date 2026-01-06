package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.UserDTO;
import fr.cnrs.opentypo.application.mapper.UserMapper;
import fr.cnrs.opentypo.domain.entity.Groupe;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.GroupeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion des utilisateurs
 * Pattern: Service Layer Pattern
 * 
 * Ce service encapsule la logique métier et utilise les DTOs pour la communication
 * avec la couche présentation.
 */
@Slf4j
@Service
@Transactional
public class UserService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Inject
    private GroupeRepository groupeRepository;

    @Inject
    private UserMapper userMapper;

    @Inject
    private UtilisateurService utilisateurService; // Pour le hashage des mots de passe

    /**
     * Récupère tous les utilisateurs sous forme de DTOs
     */
    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
        return userMapper.toDTOList(utilisateurs);
    }

    /**
     * Trouve un utilisateur par son ID
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return utilisateurRepository.findById(id)
                .map(userMapper::toDTO);
    }

    /**
     * Trouve un utilisateur par son email
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return utilisateurRepository.findByEmail(email.trim())
                .map(userMapper::toDTO);
    }

    /**
     * Crée un nouvel utilisateur
     */
    public UserDTO create(UserDTO userDTO) {
        if (userDTO == null) {
            throw new IllegalArgumentException("Le DTO utilisateur ne peut pas être null");
        }

        // Vérifier que l'email n'existe pas déjà
        if (utilisateurRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        // Récupérer le groupe
        Groupe groupe = groupeRepository.findById(userDTO.getGroupeId())
                .orElseThrow(() -> new IllegalArgumentException("Groupe introuvable"));

        // Créer l'entité
        Utilisateur utilisateur = userMapper.toEntity(userDTO, groupe);

        // Hasher le mot de passe si fourni
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            utilisateur.setPasswordHash(utilisateurService.encodePassword(userDTO.getPassword()));
        } else {
            throw new IllegalArgumentException("Le mot de passe est requis");
        }

        // Sauvegarder
        utilisateur = utilisateurRepository.save(utilisateur);
        log.info("Utilisateur créé avec succès: {}", utilisateur.getEmail());

        return userMapper.toDTO(utilisateur);
    }

    /**
     * Met à jour un utilisateur existant
     */
    public UserDTO update(Long id, UserDTO userDTO) {
        if (id == null || userDTO == null) {
            throw new IllegalArgumentException("L'ID et le DTO ne peuvent pas être null");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        // Vérifier que l'email n'est pas déjà utilisé par un autre utilisateur
        if (!utilisateur.getEmail().equals(userDTO.getEmail()) 
                && utilisateurRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        // Récupérer le groupe
        Groupe groupe = groupeRepository.findById(userDTO.getGroupeId())
                .orElseThrow(() -> new IllegalArgumentException("Groupe introuvable"));

        // Mettre à jour l'entité
        userMapper.updateEntity(utilisateur, userDTO, groupe);

        // Mettre à jour le mot de passe si fourni
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            utilisateur.setPasswordHash(utilisateurService.encodePassword(userDTO.getPassword()));
        }

        // Sauvegarder
        utilisateur = utilisateurRepository.save(utilisateur);
        log.info("Utilisateur mis à jour avec succès: {}", utilisateur.getEmail());

        return userMapper.toDTO(utilisateur);
    }

    /**
     * Supprime un utilisateur
     */
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID ne peut pas être null");
        }

        if (!utilisateurRepository.existsById(id)) {
            throw new IllegalArgumentException("Utilisateur introuvable");
        }

        utilisateurRepository.deleteById(id);
        log.info("Utilisateur supprimé avec succès: {}", id);
    }

    /**
     * Active ou désactive un utilisateur
     */
    public UserDTO toggleActive(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID ne peut pas être null");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        utilisateur.setActive(!utilisateur.getActive());
        utilisateur = utilisateurRepository.save(utilisateur);
        log.info("Statut de l'utilisateur {} modifié: {}", utilisateur.getEmail(), utilisateur.getActive());

        return userMapper.toDTO(utilisateur);
    }

    /**
     * Vérifie si un email existe déjà
     */
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return utilisateurRepository.existsByEmail(email.trim());
    }
}

