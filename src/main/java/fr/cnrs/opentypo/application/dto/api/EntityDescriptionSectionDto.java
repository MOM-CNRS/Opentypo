package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Rubrique « Description » : tous les champs sont exposés quel que soit le profil de collection.
 */
@Schema(
        name = "EntityDescriptionSectionDto",
        description = "Rubrique Description (tous champs, indépendamment du profil céramique / monnaie / instrumentum).")
public record EntityDescriptionSectionDto(
        @Schema(description = "Forme (référence thésaurus, table caracteristique_physique).", nullable = true)
        OpenThesoReferenceDto forme,
        @Schema(description = "Décors (description_detail).", nullable = true)
        String decors,
        @Schema(description = "Marques / estampilles (description_detail).", nullable = true)
        String marques,
        @Schema(description = "Métrologie texte libre (description_detail).", nullable = true)
        String metrologieDetail,
        @Schema(description = "Fonctions / usages (références entity, code FONCTION_USAGE).")
        List<OpenThesoReferenceDto> fonctionsUsage,
        @Schema(description = "Catégorie fonctionnelle (référence sur l'entité).", nullable = true)
        OpenThesoReferenceDto categorieFonctionnelle,
        @Schema(description = "Relation(s) d'imitation (métadonnées, texte libre).", nullable = true)
        String relationImitation,
        @Schema(description = "Dénomination instrumentum (métadonnées, texte libre).", nullable = true)
        String denominationInstrumentum,
        @Schema(description = "Face droit (description_monnaie).", nullable = true)
        String droit,
        @Schema(description = "Légende du droit (description_monnaie).", nullable = true)
        String legendeDroit,
        @Schema(description = "Revers (description_monnaie).", nullable = true)
        String revers,
        @Schema(description = "Légende du revers (description_monnaie).", nullable = true)
        String legendeRevers
) {
}
