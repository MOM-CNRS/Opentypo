package fr.cnrs.opentypo.application.dto;

import java.util.Arrays;
import java.util.Optional;

/**
 * Identifiants des pages statiques éditables (contact, mentions légales, accessibilité).
 */
public enum SitePageCode {

    CONTACT("contact"),
    LEGAL("legal"),
    ACCESSIBILITY("accessibility");

    private final String code;

    SitePageCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Optional<SitePageCode> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(p -> p.code.equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
