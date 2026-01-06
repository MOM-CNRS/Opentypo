package fr.cnrs.opentypo.application.mapper;

import fr.cnrs.opentypo.application.dto.GroupeDTO;
import fr.cnrs.opentypo.domain.entity.Groupe;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir entre Entity Groupe et DTO GroupeDTO
 */
@Component
public class GroupeMapper {

    /**
     * Convertit une entité Groupe en DTO GroupeDTO
     */
    public GroupeDTO toDTO(Groupe groupe) {
        if (groupe == null) {
            return null;
        }
        
        return GroupeDTO.builder()
                .id(groupe.getId())
                .nom(groupe.getNom())
                .build();
    }

    /**
     * Convertit une liste d'entités Groupe en liste de DTOs
     */
    public List<GroupeDTO> toDTOList(List<Groupe> groupes) {
        if (groupes == null) {
            return List.of();
        }
        return groupes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

