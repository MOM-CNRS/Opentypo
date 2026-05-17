package fr.cnrs.opentypo.application.dto.api;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * Filtre {@code statut} pour les endpoints API REST.
 * Valeurs : {@code PUBLIQUE}, {@code PROPOSITION}, {@code REFUSE} (ou {@code REFUSED}), {@code tous}.
 */
public enum EntityApiStatutFilter {

    /** Tous les statuts (aucun filtre en base). */
    TOUS(null),

    PUBLIQUE(EntityStatusEnum.PUBLIQUE.name()),
    PROPOSITION(EntityStatusEnum.PROPOSITION.name()),
    REFUSE(EntityStatusEnum.REFUSE.name());

    public static final String VALUE_TOUS = "tous";

    private final String statut;

    EntityApiStatutFilter(String statut) {
        this.statut = statut;
    }

    public boolean isFiltered() {
        return statut != null;
    }

    /** Statut unique pour JPQL ({@code null} = pas de filtre). */
    public String statutForQuery() {
        return statut;
    }

    public boolean matches(String entityStatut) {
        return statut == null || (entityStatut != null && statut.equals(entityStatut));
    }

    public static EntityApiStatutFilter parse(String value) {
        if (value == null || value.isBlank()) {
            return TOUS;
        }
        String trimmed = value.trim();
        if (VALUE_TOUS.equalsIgnoreCase(trimmed)) {
            return TOUS;
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if ("REFUSED".equals(normalized)) {
            normalized = REFUSE.name();
        }
        if (PUBLIQUE.name().equals(normalized)) {
            return PUBLIQUE;
        }
        if (PROPOSITION.name().equals(normalized)) {
            return PROPOSITION;
        }
        if (REFUSE.name().equals(normalized)) {
            return REFUSE;
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                ApiErrorMessages.invalidStatutFilter(trimmed));
    }

}
