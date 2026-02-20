package fr.cnrs.opentypo.application.dto;

import lombok.Getter;

@Getter
public enum PermissionRoleEnum {

    GESTIONNAIRE_COLLECTION("Gestionnaire de collection"),
    GESTIONNAIRE_REFERENTIEL("Gestionnaire de référentiel"),
    REDACTEUR("Rédacteur"),
    VALIDEUR("Valideur"),
    RELECTEUR("Relecteur");

    private final String label;

    PermissionRoleEnum(String label) {
        this.label = label;
    }
}
