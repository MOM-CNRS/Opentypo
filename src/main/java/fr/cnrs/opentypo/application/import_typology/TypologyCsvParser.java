package fr.cnrs.opentypo.application.import_typology;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parseur CSV minimal (RFC 4180 simplifié : guillemets ; séparateur {@code ,} ou {@code ;} détecté sur la ligne d'en-tête).
 */
@Slf4j
public final class TypologyCsvParser {

    private TypologyCsvParser() {
    }

    /**
     * @param content texte CSV complet avec une ligne d'en-tête
     */
    public static ParsedCsv parse(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }
        String bomStripped = content.startsWith("\uFEFF") ? content.substring(1) : content;
        String normalized = bomStripped.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        if (lines.length < 2) {
            throw new IllegalArgumentException("Le fichier doit contenir un en-tête et au moins une ligne de données.");
        }
        char delimiter = detectDelimiter(lines[0]);
        List<String> headers = splitCsvLine(lines[0], delimiter);
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                continue;
            }
            List<String> cells = splitCsvLine(line, delimiter);
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                String key = normalizeHeader(headers.get(c));
                if (key.isEmpty()) {
                    continue;
                }
                String value = c < cells.size() ? cells.get(c) : "";
                row.put(key, value == null ? "" : value.strip());
            }
            rows.add(row);
        }
        return new ParsedCsv(headers.stream().map(TypologyCsvParser::normalizeHeader).filter(s -> !s.isEmpty()).toList(), rows);
    }

    /**
     * En-tête normalisé pour lookup stable (évite BOM Excel / espaces invisibles qui cassent {@code code_categorie} au 2e import).
     */
    private static String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.strip().toLowerCase(Locale.ROOT);
        while (s.startsWith("\uFEFF")) {
            s = s.substring(1).strip().toLowerCase(Locale.ROOT);
        }
        return s.strip();
    }

    /**
     * Export Excel (FR) et équivalents utilisent souvent {@code ;}. On choisit le séparateur qui produit le plus de colonnes cohérentes sur l'en-tête.
     */
    private static char detectDelimiter(String headerLine) {
        if (headerLine == null || headerLine.isBlank()) {
            return ',';
        }
        List<String> bySemi = splitCsvLine(headerLine, ';');
        List<String> byComma = splitCsvLine(headerLine, ',');
        if (bySemi.size() > byComma.size()) {
            return ';';
        }
        return ',';
    }

    /**
     * Découpe une ligne CSV en respectant les guillemets.
     */
    static List<String> splitCsvLine(String line, char delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == delimiter) {
                    result.add(cell.toString());
                    cell.setLength(0);
                } else {
                    cell.append(ch);
                }
            }
        }
        result.add(cell.toString());
        return result;
    }

    public record ParsedCsv(List<String> headers, List<Map<String, String>> rows) {
        /**
         * Noms d'en-tête normalisés (tels qu'utilisés en clé dans chaque ligne) : sert à l'import partiel
         * (seules les colonnes listées ici sont prises en compte à la mise à jour).
         */
        public Set<String> headerKeySet() {
            return Set.copyOf(headers);
        }
    }
}
