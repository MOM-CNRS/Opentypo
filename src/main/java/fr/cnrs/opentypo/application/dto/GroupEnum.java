package fr.cnrs.opentypo.application.dto;

import lombok.Getter;

@Getter
public enum GroupEnum {

    ADMINISTRATEUR_TECHNIQUE("Administrateur technique"),
    GESTIONNAIRE_REFERENTIELS("Gestionnaire de référentiels"),
    GESTIONNAIRE_COLLECTIONS("Gestionnaire de collections"),
    REDACTEUR("Rédacteur"),
    VALIDEUR("Valideur"),
    RELECTEUR("Relecteur");

    private final String label;

    GroupEnum(String label) {
        this.label = label;
    }
}