package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Référence thésaurus (OpenTheso / PACTOLS) liée à une entité.
 */
@Schema(name = "OpenThesoReferenceDto", description = "Valeur de thésaurus associée à une entité typologique.")
public record OpenThesoReferenceDto(
        @Schema(description = "Code du type de référence (ex. FORME, FABRICATION_FACONNAGE).", example = "FORME")
        String code,
        @Schema(description = "Libellé affiché.", example = "Assiette")
        String valeur,
        @Schema(description = "URL OpenTheso, si disponible.", nullable = true)
        String url,
        @Schema(description = "Identifiant concept OpenTheso.", nullable = true)
        String conceptId
) {
}
