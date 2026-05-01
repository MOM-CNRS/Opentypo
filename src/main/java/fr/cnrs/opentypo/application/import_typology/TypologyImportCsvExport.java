package fr.cnrs.opentypo.application.import_typology;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Export CSV des lignes en erreur (même ordre de colonnes que le fichier analysé + colonnes de diagnostic).
 */
public final class TypologyImportCsvExport {

    private static final String COL_ERREURS = "erreurs_detectees";
    private static final String COL_AVERTISSEMENTS = "avertissements_detectes";

    private TypologyImportCsvExport() {
    }

    public static byte[] buildUtf8(TypologyCsvParser.ParsedCsv parsed, List<TypologyImportPreviewLine> errorLines) {
        StringBuilder sb = new StringBuilder();
        List<String> headers = parsed.headers();
        List<Map<String, String>> rows = parsed.rows();

        boolean first = true;
        for (String h : headers) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(escapeCsvField(h));
        }
        sb.append(',').append(escapeCsvField(COL_ERREURS)).append(',').append(escapeCsvField(COL_AVERTISSEMENTS)).append('\n');

        for (TypologyImportPreviewLine line : errorLines) {
            int idx = line.csvRowNumber() - 2;
            if (idx < 0 || idx >= rows.size()) {
                continue;
            }
            Map<String, String> row = rows.get(idx);
            first = true;
            for (String h : headers) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                String v = row.get(h);
                sb.append(escapeCsvField(v != null ? v : ""));
            }
            sb.append(',').append(escapeCsvField(joinMessages(line.errors())));
            sb.append(',').append(escapeCsvField(joinMessages(line.warnings())));
            sb.append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String joinMessages(List<String> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            return "";
        }
        return String.join(" | ", msgs);
    }

    static String escapeCsvField(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean mustQuote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!mustQuote) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
