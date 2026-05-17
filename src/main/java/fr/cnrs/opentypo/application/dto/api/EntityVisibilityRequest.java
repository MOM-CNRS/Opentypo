package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de requête pour basculer la visibilité d'une entité entre publique et privée.
 */
@Schema(name = "EntityVisibilityRequest", description = "Rendre une entité publique ou privée (statuts PUBLIQUE / PRIVEE).")
public record EntityVisibilityRequest(
        @Schema(
                description = "true pour rendre l'entité publique (statut PUBLIQUE), "
                        + "false pour la rendre privée (statut PRIVEE).",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Le champ publique est obligatoire")
        Boolean publique) {
}
