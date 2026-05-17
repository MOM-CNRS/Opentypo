package fr.cnrs.opentypo.application.dto.api;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload to create a new typology entity (metadata + one primary label).
 */
@Schema(
        name = "EntityCreateRequest",
        description = "Corps de requête pour créer une entité typologique (métadonnées + libellé principal).")
public record EntityCreateRequest(
        @Schema(
                description = "Code métier unique de l'entité.",
                example = "DECOCER",
                maxLength = EntityConstants.MAX_CODE_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Le code est obligatoire")
        @Size(max = 100, message = "Le code ne doit pas dépasser {max} caractères")
        String code,
        @Schema(
                description = "Code du type d'entité.",
                example = "TYPE",
                allowableValues = {
                        "COLLECTION",
                        "REFERENTIEL",
                        "CATEGORIE",
                        "GROUPE",
                        "SERIE",
                        "TYPE"
                },
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Le type d'entité est obligatoire")
        @Size(max = 50, message = "Le type d'entité ne doit pas dépasser {max} caractères")
        String entityTypeCode,
        @Schema(
                description = "Libellé principal (nom affiché).",
                example = "Céramique décorée",
                maxLength = EntityConstants.MAX_LABEL_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 255, message = "Le libellé ne doit pas dépasser {max} caractères")
        String labelNom,
        @Schema(
                description = "Code langue du libellé principal. Défaut : fr.",
                example = "fr",
                maxLength = 10)
        @Size(max = 10, message = "Le code langue ne doit pas dépasser {max} caractères")
        String labelLangCode,
        @Schema(
                description = "Statut initial. Défaut : PUBLIQUE si omis.",
                example = "PUBLIQUE",
                allowableValues = {"PROPOSITION", "IN_VALIDATION", "PUBLIQUE", "PRIVEE", "REFUSE"},
                implementation = EntityStatusEnum.class)
        String statut,
        @Schema(
                description = "Identifiant technique du parent dans l'arbre typologique (optionnel).",
                example = "12",
                nullable = true)
        Long parentEntityId
) {
}
