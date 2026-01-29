package fr.cnrs.opentypo.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO représentant une révision d'audit d'une entité
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityRevisionDTO implements Serializable {

    private Long entityId;
    private Long revisionNumber;
    private String revisionType; // ADD, MOD, DEL
    private LocalDateTime revisionDate;
    private String modifiedBy;
    
    // Données de l'entité à cette révision
    private Map<String, Object> entityData;
    
    // Données de l'entité à la révision précédente (pour comparaison)
    private Map<String, Object> previousEntityData;
    
    /**
     * Retourne le libellé du type de révision
     */
    public String getRevisionTypeLabel() {
        if (revisionType == null) {
            return "Inconnu";
        }
        switch (revisionType) {
            case "0": // ADD
                return "Création";
            case "1": // MOD
                return "Modification";
            case "2": // DEL
                return "Suppression";
            default:
                return revisionType;
        }
    }
    
    /**
     * Retourne l'icône selon le type de révision
     */
    public String getRevisionTypeIcon() {
        if (revisionType == null) {
            return "pi pi-circle";
        }
        switch (revisionType) {
            case "0": // ADD
                return "pi pi-plus-circle";
            case "1": // MOD
                return "pi pi-pencil";
            case "2": // DEL
                return "pi pi-trash";
            default:
                return "pi pi-circle";
        }
    }
    
    /**
     * Retourne la classe CSS selon le type de révision
     */
    public String getRevisionTypeClass() {
        if (revisionType == null) {
            return "";
        }
        switch (revisionType) {
            case "0": // ADD
                return "revision-add";
            case "1": // MOD
                return "revision-modify";
            case "2": // DEL
                return "revision-delete";
            default:
                return "";
        }
    }
}
