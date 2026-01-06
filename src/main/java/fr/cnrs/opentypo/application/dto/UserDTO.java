package fr.cnrs.opentypo.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO pour représenter un utilisateur dans la couche présentation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String username;
    private String email;
    private String password; // Utilisé uniquement pour la création/modification
    private String firstName;
    private String lastName;
    private String groupeNom; // Nom du groupe au lieu de Role enum
    private Long groupeId; // ID du groupe
    private Boolean active;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private String createdBy;
}

