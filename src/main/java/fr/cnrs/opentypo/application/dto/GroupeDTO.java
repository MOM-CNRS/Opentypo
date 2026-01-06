package fr.cnrs.opentypo.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO pour représenter un groupe d'utilisateurs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupeDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String nom;
}

