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
        String flagCode = codeFlag != null ? codeFlag : code;
        return "/resources/img/flag/" + flagCode + ".png";
    }

}
