package fr.cnrs.opentypo.application.dto;

import lombok.Getter;

@Getter
public enum GroupEnum {

    ADMINISTRATEUR_TECHNIQUE("Administrateur technique"),
    ADMINISTRATEUR_REFERENTIEL("Administrateur Référentiel"),
    EDITEUR("Éditeur"),
    LECTEUR("Lecteur"),;

    private final String label;

    GroupEnum(String label) {
        this.label = label;
    }
}