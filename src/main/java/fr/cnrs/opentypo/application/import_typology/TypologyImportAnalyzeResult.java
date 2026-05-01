package fr.cnrs.opentypo.application.import_typology;

import java.util.List;

/**
 * Résultat de l'étape « analyse » (sans écriture en base).
 */
public record TypologyImportAnalyzeResult(
        boolean successful,
        List<String> blockingErrors,
        List<TypologyImportPreviewLine> previewLines,
        TypologyCsvParser.ParsedCsv parsedCsv
) {
}
