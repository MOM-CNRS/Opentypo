package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Type d'entité typologique (COLLECTION, TYPE, …).
 */
@Schema(name = "EntityTypeDto", description = "Type d'entité de la hiérarchie typologique.")
public record EntityTypeDto(
        @Schema(description = "Identifiant technique.", example = "6")
        Long id,
        @Schema(
                description = "Code du type.",
                example = "TYPE",
                allowableValues = {
                        "COLLECTION",
                        "REFERENTIEL",
                        "CATEGORIE",
                        "GROUPE",
                        "SERIE",
                        "TYPE"
                })
        String code
) {
}
