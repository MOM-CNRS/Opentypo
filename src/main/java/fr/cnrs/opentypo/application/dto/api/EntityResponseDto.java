package fr.cnrs.opentypo.application.dto.api;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JSON representation of an {@link fr.cnrs.opentypo.domain.entity.Entity} for REST APIs.
 */
@Schema(name = "EntityResponseDto", description = "Représentation JSON d'une entité typologique pour l'API REST.")
public record EntityResponseDto(
        @Schema(description = "Identifiant technique (clé primaire).", example = "42")
        Long id,
        @Schema(description = "Code métier unique.", example = "DECOCER")
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
                })
        String entityTypeCode,
        @Schema(
                description = "Statut de visibilité / workflow.",
                example = "PUBLIQUE",
                allowableValues = {"PROPOSITION", "IN_VALIDATION", "PUBLIQUE", "PRIVEE", "REFUSE"},
                implementation = EntityStatusEnum.class)
        String statut,
        @Schema(description = "Libellé principal (premier libellé disponible).", example = "Céramique décorée")
        String primaryLabel,
        @Schema(description = "Identifiant ARK éventuel.", example = "ark:/12345/x5abc", nullable = true)
        String idArk,
        @Schema(description = "Ordre d'affichage dans les listes.", example = "1", nullable = true)
        Integer displayOrder,
        @Schema(description = "Date de création.", example = "2024-06-15T10:30:00")
        LocalDateTime createDate,
        @Schema(description = "Login du créateur, si renseigné.", example = "jdoe", nullable = true)
        String createBy,
        @Schema(description = "Appellation usuelle (métadonnées).", nullable = true)
        String appellation,
        @Schema(description = "Bibliographie (texte libre).", nullable = true)
        String bibliographie,
        @Schema(
                description = "Clés d'items Zotero (JSON tableau de chaînes).",
                example = "[\"ABCDE123\"]",
                nullable = true)
        String zoteroItemKeys,
        @Schema(description = "Commentaire des métadonnées.", nullable = true)
        String metadataCommentaire,
        @Schema(description = "Commentaire de datation.", nullable = true)
        String commentaireDatation,
        @Schema(description = "Terminus post quem (année de début de datation).", example = "-500", nullable = true)
        Integer tpq,
        @Schema(description = "Terminus ante quem (année de fin de datation).", example = "-400", nullable = true)
        Integer taq,
        @Schema(description = "URL de l'image principale (première image).", nullable = true)
        String primaryImageUrl,
        @Schema(description = "Tous les libellés multilingues de l'entité.")
        List<EntityLabelDto> labels,
        @Schema(description = "Identifiants des parents directs dans l'arbre typologique.")
        List<Long> parentEntityIds,
        @Schema(description = "Identifiants des enfants directs dans l'arbre typologique.")
        List<Long> childEntityIds,
        @Schema(
                description = "Descriptions multilingues de présentation (table description). "
                        + "Renseigné sur les réponses détail (GET par id ou code).",
                nullable = true)
        List<EntityTextDescriptionDto> presentationDescriptions,
        @Schema(
                description = "Rubrique « Description » (forme, décors, monnaie, instrumentum…). "
                        + "Renseigné sur les réponses détail.",
                nullable = true)
        EntityDescriptionSectionDto description,
        @Schema(
                description = "Rubrique « Caractéristiques physiques ». Renseigné sur les réponses détail.",
                nullable = true)
        EntityPhysicalCharacteristicsSectionDto physicalCharacteristics
) {
}
