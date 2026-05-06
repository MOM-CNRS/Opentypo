package fr.cnrs.opentypo.application.dto.api;

import lombok.Getter;
import lombok.Setter;

/**
 * Partial update: only non-null fields are applied.
 */
@Getter
@Setter
public class EntityUpdateRequest {
    private String statut;
    private String code;
    private String idArk;
    private Integer displayOrder;
    private String appellation;
    private String bibliographie;
    /** JSON tableau de clés d'items Zotero, ex. {@code ["ABCDE123","FGHIJ456"]}. */
    private String zoteroItemKeys;
    private String metadataCommentaire;
}
