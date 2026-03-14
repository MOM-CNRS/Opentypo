package fr.cnrs.opentypo.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO pour l'affichage des permissions d'un utilisateur sur une entité (profil).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionItem {

    /** Type d'entité (ex. Collection, Référentiel, Groupe). */
    private String entityTypeLabel;

    /** Label de l'entité (nom selon la langue). */
    private String entityLabel;

    /** Code de l'entité. */
    private String entityCode;

    /** Rôle (ex. Gestionnaire de collection, Rédacteur, Valideur, Relecteur). */
    private String role;

    /** ID de l'entité (pour lien éventuel). */
    private Long entityId;
}
