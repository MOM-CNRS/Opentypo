package fr.cnrs.opentypo.application.dto;

import lombok.Getter;

@Getter
public enum LangueEnum {

    ARABE("ar", "Arabe"),
    ALLEMAND("de", "Allemand"),
    GREC("el", "Grec moderne"),
    ANGLAIS("en", "Anglais"),
    ESPAGNOL("es", "Espagnol"),
    FRANCAIS("fr", "Français"),
    HEBREU("he", "Hébreu"),
    ITALIEN("it", "Italien"),
    NEERLANDAIS("nl", "Néerlandais"),
    RUSSE("ru", "Russe"),;

    private final String code;
    private final String label;


    LangueEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String getLabelByCode(String code) {
        for (LangueEnum status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status.label;
            }
        }
        return null;
    }
}