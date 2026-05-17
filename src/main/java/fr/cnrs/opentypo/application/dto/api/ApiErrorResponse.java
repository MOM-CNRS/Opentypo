package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON error body for {@code /api/**} REST endpoints.
 */
@Schema(name = "ApiErrorResponse", description = "Réponse d'erreur JSON standard de l'API REST.")
public record ApiErrorResponse(
        @Schema(description = "Code HTTP.", example = "400")
        int status,
        @Schema(description = "Message d'erreur principal.", example = "Échec de la validation")
        String error,
        @Schema(
                description = "Détails complémentaires (champs en échec, message métier, etc.).",
                nullable = true,
                example = "code : ne doit pas être vide")
        String details
) {
}
