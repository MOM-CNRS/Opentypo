package fr.cnrs.opentypo.application.mapper;

import fr.cnrs.opentypo.application.dto.UserDTO;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.domain.entity.Groupe;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir entre Entity Utilisateur et DTO UserDTO
 * Pattern: Mapper Pattern
 */
@Component
public class UserMapper {

    /**
     * Convertit une entité Utilisateur en DTO UserDTO
     */
    public UserDTO toDTO(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        
        return UserDTO.builder()
                .id(utilisateur.getId())
                .email(utilisateur.getEmail())
                .firstName(utilisateur.getPrenom())
                .lastName(utilisateur.getNom())
                .username(utilisateur.getPrenom() + " " + utilisateur.getNom())
                .groupeId(utilisateur.getGroupe() != null ? utilisateur.getGroupe().getId() : null)
                .groupeNom(utilisateur.getGroupe() != null ? utilisateur.getGroupe().getNom() : null)
                .active(utilisateur.getActive())
                .dateCreation(utilisateur.getCreateDate())
                .createdBy(utilisateur.getCreateBy())
                .build();
    }

    /**
     * Convertit une liste d'entités Utilisateur en liste de DTOs
     */
    public List<UserDTO> toDTOList(List<Utilisateur> utilisateurs) {
        if (utilisateurs == null) {
            return List.of();
        }
        return utilisateurs.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convertit un DTO UserDTO en entité Utilisateur (pour création)
     * Note: Le mot de passe doit être hashé avant d'appeler cette méthode
     */
    public Utilisateur toEntity(UserDTO userDTO, Groupe groupe) {
        if (userDTO == null) {
            return null;
        }
        
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(userDTO.getId());
        utilisateur.setEmail(userDTO.getEmail());
        utilisateur.setPrenom(userDTO.getFirstName());
        utilisateur.setNom(userDTO.getLastName());
        utilisateur.setGroupe(groupe);
        utilisateur.setActive(userDTO.getActive() != null ? userDTO.getActive() : true);
        
        // Le passwordHash doit être défini séparément après le hashage
        // utilisateur.setPasswordHash(...);
        
        return utilisateur;
    }

    /**
     * Met à jour une entité Utilisateur existante avec les données du DTO
     */
    public void updateEntity(Utilisateur utilisateur, UserDTO userDTO, Groupe groupe) {
        if (utilisateur == null || userDTO == null) {
            return;
        }
        
        utilisateur.setEmail(userDTO.getEmail());
        utilisateur.setPrenom(userDTO.getFirstName());
        utilisateur.setNom(userDTO.getLastName());
        utilisateur.setGroupe(groupe);
        if (userDTO.getActive() != null) {
            utilisateur.setActive(userDTO.getActive());
        }
    }
}

