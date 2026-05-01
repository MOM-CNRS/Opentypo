package fr.cnrs.opentypo.application.import_typology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Une ligne du fichier après classification et validation logique (aperçu étape 2).
 */
public record TypologyImportPreviewLine(
        int csvRowNumber,
        TypologyImportKind kind,
        String targetCode,
        boolean createOrUpdateCreate,
        boolean lineOk,
        List<String> errors,
        List<String> warnings
) {

    public TypologyImportPreviewLine {
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static TypologyImportPreviewLine blocked(int csvRowNumber, TypologyImportKind kind, String targetCode,
                                                     String error) {
        List<String> errs = new ArrayList<>();
        errs.add(error);
        return new TypologyImportPreviewLine(csvRowNumber, kind, targetCode, true, false, errs, Collections.emptyList());
    }
}
