package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Rubrique « Caractéristiques physiques » : tous les champs sont exposés quel que soit le profil.
 */
@Schema(
        name = "EntityPhysicalCharacteristicsSectionDto",
        description = "Rubrique Caractéristiques physiques (tous champs, profils céramique et monnaie distincts).")
public record EntityPhysicalCharacteristicsSectionDto(
        @Schema(description = "Description de la pâte (description_pate).", nullable = true)
        String descriptionPate,
        @Schema(description = "Métrologie — référence thésaurus (caracteristique_physique).", nullable = true)
        OpenThesoReferenceDto metrologie,
        @Schema(description = "Métrologie — texte libre (caracteristique_physique_monnaie).", nullable = true)
        String metrologieMonnaie,
        @Schema(description = "Matériaux — référence thésaurus (caracteristique_physique).", nullable = true)
        OpenThesoReferenceDto materiaux,
        @Schema(description = "Matériaux — référence thésaurus (caracteristique_physique_monnaie).", nullable = true)
        OpenThesoReferenceDto materiauxMonnaie,
        @Schema(description = "Dimensions (caracteristique_physique).", nullable = true)
        OpenThesoReferenceDto dimensions,
        @Schema(description = "Technique (caracteristique_physique).", nullable = true)
        OpenThesoReferenceDto technique,
        @Schema(description = "Technique (caracteristique_physique_monnaie).", nullable = true)
        OpenThesoReferenceDto techniqueMonnaie,
        @Schema(description = "Fabrication / façonnage (références, code FABRICATION_FACONNAGE).")
        List<OpenThesoReferenceDto> fabricationsFaconnage,
        @Schema(description = "Cuisson / post-cuisson (références, code CUISSON_POST_CUISSON).")
        List<OpenThesoReferenceDto> cuissonsPostCuisson,
        @Schema(description = "Couleurs de pâte (références, code COULEUR_PATE).")
        List<OpenThesoReferenceDto> couleursPate,
        @Schema(description = "Natures de pâte (références, code NATURE_PATE).")
        List<OpenThesoReferenceDto> naturesPate,
        @Schema(description = "Inclusions (références, code INCLUSIONS).")
        List<OpenThesoReferenceDto> inclusionsPate,
        @Schema(description = "Dénomination (caracteristique_physique_monnaie).", nullable = true)
        OpenThesoReferenceDto denominationMonnaie,
        @Schema(description = "Valeur faciale (caracteristique_physique_monnaie).", nullable = true)
        OpenThesoReferenceDto valeurMonnaie
) {
}
