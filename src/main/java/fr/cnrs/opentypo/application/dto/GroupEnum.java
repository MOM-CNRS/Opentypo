package fr.cnrs.opentypo.application.dto;

import lombok.Getter;

@Getter
public enum GroupEnum {

    ADMINISTRATEUR_TECHNIQUE("Administrateur technique"),
    UTILISATEUR("Utilisateur");

    private final String label;

    GroupEnum(String label) {
        this.label = label;
    }
}