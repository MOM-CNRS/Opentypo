package fr.cnrs.opentypo.application.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload to create a new typology entity (metadata + one primary label).
 */
public record EntityCreateRequest(
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 50) String entityTypeCode,
        @NotBlank @Size(max = 255) String labelNom,
        @Size(max = 10) String labelLangCode,
        String statut,
        Long parentEntityId
) {
}
