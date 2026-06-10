package fr.cnrs.opentypo.common.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Language {

    private Integer id;
    private String code;
    private String value;
    private String codeFlag;


    public String getValue() {
        if(value == null || value.isEmpty()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    /**
     * Retourne le chemin du drapeau pour cette langue
     */
    public String getFlagPath() {
        String flagCode = resolveFlagResourceCode(codeFlag != null ? codeFlag : code);
        return "/resources/img/flags/" + flagCode + ".svg";
    }

    /** Codes langue → code pays pour l'emoji drapeau (en → gb pour 🇬🇧). */
    private static final java.util.Map<String, String> LANG_TO_COUNTRY = java.util.Map.of(
            "en", "gb"   // Anglais → drapeau britannique 🇬🇧
    );

    /**
     * Code ressource drapeau (fr, gb, …) pour un code langue typologie (fr, en, …).
     */
    public static String resolveFlagResourceCode(String langCode) {
        if (langCode == null || langCode.isBlank()) {
            return "fr";
        }
        return LANG_TO_COUNTRY.getOrDefault(langCode.toLowerCase(), langCode.toLowerCase());
    }

    /**
     * Retourne l'emoji drapeau du pays pour le code langue (ex. fr → 🇫🇷, en → 🇬🇧).
     * Même principe que les icônes 📁 📖 du select Collection.
     */
    public String getFlagEmoji() {
        String countryCode = resolveFlagResourceCode(code);
        if (countryCode == null || countryCode.length() != 2) {
            return "";
        }
        countryCode = countryCode.toUpperCase();
        int first = countryCode.charAt(0) - 'A' + 0x1F1E6;
        int second = countryCode.charAt(1) - 'A' + 0x1F1E6;
        if (first < 0x1F1E6 || first > 0x1F1FF || second < 0x1F1E6 || second > 0x1F1FF) {
            return "";
        }
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }

}
