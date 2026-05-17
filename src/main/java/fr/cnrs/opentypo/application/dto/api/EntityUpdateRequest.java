package fr.cnrs.opentypo.application.dto.api;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update: only non-null fields are applied.
 */
@Schema(
        name = "EntityUpdateRequest",
        description = "Mise à jour partielle (RFC 5789) : seuls les champs non null sont appliqués.")
@Getter
@Setter
public class EntityUpdateRequest {

    @Schema(
            description = "Nouveau statut.",
            example = "PUBLIQUE",
            allowableValues = {"PROPOSITION", "IN_VALIDATION", "PUBLIQUE", "PRIVEE", "REFUSE"},
            implementation = EntityStatusEnum.class,
            nullable = true)
    private String statut;

    @Schema(
            description = "Nouveau code métier (doit rester unique).",
            example = "DECOCER_V2",
            maxLength = EntityConstants.MAX_CODE_LENGTH,
            nullable = true)
    private String code;

    @Schema(description = "Identifiant ARK pérenne.", example = "ark:/12345/x5abc", nullable = true)
    private String idArk;

    @Schema(description = "Ordre d'affichage dans les listes.", example = "10", nullable = true)
    private Integer displayOrder;

    @Schema(description = "Appellation usuelle (métadonnées).", nullable = true)
    private String appellation;

    @Schema(description = "Bibliographie (texte libre).", nullable = true)
    private String bibliographie;

    @Schema(
            description = "Clés d'items Zotero (JSON tableau de chaînes).",
            example = "[\"ABCDE123\",\"FGHIJ456\"]",
            nullable = true)
    private String zoteroItemKeys;

    @Schema(description = "Commentaire des métadonnées (entity_metadata.commentaire).", nullable = true)
    private String metadataCommentaire;
}
