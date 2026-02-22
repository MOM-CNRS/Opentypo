package fr.cnrs.opentypo.application.dto;

import fr.cnrs.opentypo.domain.entity.Entity;

import java.util.List;

/**
 * DTO regroupant une série et la liste de ses types, pour l'affichage unifié série → types.
 */
public record SerieWithTypes(Entity serie, List<Entity> types) {
    /** Nombre de types (pour EL / Facelets). */
    public int typesCount() {
        return types != null ? types.size() : 0;
    }
}
