package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Langue disponible pour les libellés.
 */
@Schema(name = "LangueDto", description = "Langue supportée pour les libellés multilingues.")
public record LangueDto(
        @Schema(description = "Code langue ISO.", example = "fr")
        String code,
        @Schema(description = "Nom affiché de la langue.", example = "Français")
        String nom
) {
}
