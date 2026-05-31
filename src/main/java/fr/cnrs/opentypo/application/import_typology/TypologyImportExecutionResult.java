package fr.cnrs.opentypo.application.import_typology;

/**
 * Résultat d'un import typologique (succès total, partiel ou échec sans ligne enregistrée).
 */
public record TypologyImportExecutionResult(
        boolean success,
        boolean partial,
        int importedCount,
        int plannedCount,
        Integer failedAtCsvRow,
        String message) {

    public static TypologyImportExecutionResult ok(int importedCount) {
        return new TypologyImportExecutionResult(true, false, importedCount, importedCount, null, null);
    }

    public static TypologyImportExecutionResult partial(
            int importedCount, int plannedCount, Integer failedAtCsvRow, String message) {
        return new TypologyImportExecutionResult(false, true, importedCount, plannedCount, failedAtCsvRow, message);
    }

    public static TypologyImportExecutionResult failure(String message) {
        return new TypologyImportExecutionResult(false, false, 0, 0, null, message);
    }
}
