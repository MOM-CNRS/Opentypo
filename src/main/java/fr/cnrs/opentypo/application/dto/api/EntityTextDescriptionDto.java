package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Description textuelle multilingue (table {@code description}, section présentation).
 */
@Schema(name = "EntityTextDescriptionDto", description = "Description HTML/texte par langue (présentation du type).")
public record EntityTextDescriptionDto(
        @Schema(description = "Code langue.", example = "fr")
        String langCode,
        @Schema(description = "Contenu de la description (peut contenir du HTML).")
        String valeur
) {
}
