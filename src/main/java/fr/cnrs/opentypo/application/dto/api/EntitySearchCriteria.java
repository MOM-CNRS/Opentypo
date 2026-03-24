package fr.cnrs.opentypo.application.dto.api;

/**
 * Optional filters for {@code GET /api/v1/entities}. All fields are optional; combined with AND.
 * <ul>
 *   <li>{@code typeCode} — entity type (e.g. TYPE, SERIE, GROUPE)</li>
 *   <li>{@code statut} — e.g. PUBLIQUE, PRIVEE</li>
 *   <li>{@code code} — exact match on metadata code</li>
 *   <li>{@code codeContains} — case-insensitive substring on metadata code</li>
 *   <li>{@code idArk} — exact match</li>
 *   <li>{@code q} — text search: metadata code or label name (see {@code labelLang})</li>
 *   <li>{@code labelLang} — when {@code q} is set, restricts label search to this language code (e.g. fr); if null, any language</li>
 * </ul>
 */
public record EntitySearchCriteria(
        String typeCode,
        String statut,
        String code,
        String codeContains,
        String idArk,
        String q,
        String labelLang
) {
    /** Normalizes blank strings to null so JPQL optional clauses work. */
    public static EntitySearchCriteria normalize(
            String typeCode,
            String statut,
            String code,
            String codeContains,
            String idArk,
            String q,
            String labelLang) {
        return new EntitySearchCriteria(
                blankToNull(typeCode),
                blankToNull(statut),
                blankToNull(code),
                blankToNull(codeContains),
                blankToNull(idArk),
                blankToNull(q),
                blankToNull(labelLang));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
