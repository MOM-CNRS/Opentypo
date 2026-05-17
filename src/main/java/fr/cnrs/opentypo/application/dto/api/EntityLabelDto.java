package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Libellé multilingue d'une entité typologique.
 */
@Schema(name = "EntityLabelDto", description = "Libellé d'une entité dans une langue donnée.")
public record EntityLabelDto(
        @Schema(description = "Code langue ISO (ex. fr, en).", example = "fr")
        String langCode,
        @Schema(description = "Texte du libellé.", example = "Céramique décorée")
        String nom
) {
}
