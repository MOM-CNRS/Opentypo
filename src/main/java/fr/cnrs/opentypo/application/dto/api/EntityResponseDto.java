package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * JSON representation of an {@link fr.cnrs.opentypo.domain.entity.Entity} for REST APIs.
 */
@Schema(name = "EntityResponseDto", description = "Représentation JSON d'une entité typologique pour l'API REST.")
public record EntityResponseDto(
        @Schema(description = "Identifiant technique (clé primaire).", example = "42")
        Long id,
        @Schema(description = "Code métier unique.", example = "DECOCER")
        String code,
        @Schema(description = "Code du type d'entité (TYPE, SERIE, …).", example = "REFERENCE")
        String entityTypeCode,
        @Schema(description = "Statut : PROPOSITION, PUBLIQUE, PRIVEE, REFUSE.", example = "PUBLIQUE")
        String statut,
        @Schema(description = "Libellé principal (nom affiché).")
        String primaryLabel,
        @Schema(description = "Identifiant ARK éventuel.")
        String idArk,
        @Schema(description = "Ordre d'affichage dans les listes.")
        Integer displayOrder,
        @Schema(description = "Date de création.")
        LocalDateTime createDate,
        @Schema(description = "Identifiant du créateur (login), si renseigné.")
        String createBy
) {
}
