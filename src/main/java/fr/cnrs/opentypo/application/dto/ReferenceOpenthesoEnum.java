package fr.cnrs.opentypo.application.dto;

import lombok.Getter;

@Getter
public enum ReferenceOpenthesoEnum {

    PRODUCTION,
    PERIODE,
    CATEGORIE,
    CATEGORIE_FONCTIONNELLE,
    AIRE_CIRCULATION,
    FONCTION_USAGE,
    METROLOGIE,
    FABRICATION_FACONNAGE,
    COULEUR_PATE,
    NATURE_PATE,
    INCLUSIONS,
    CUISSON_POST_CUISSON,
    MATERIAU,
    DENOMINATION,
    VALEUR,
    TECHNIQUE,
    FABRICATION;


    public static ReferenceOpenthesoEnum fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("La valeur ne peut pas être null");
        }

        return ReferenceOpenthesoEnum.valueOf(value.toUpperCase());
    }
}