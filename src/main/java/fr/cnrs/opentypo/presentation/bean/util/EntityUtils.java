package fr.cnrs.opentypo.presentation.bean.util;

import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Label;

public class EntityUtils {


    public static String getLabelValueForLanguage(Entity entity, String codeLang) {
        if (entity == null || codeLang == null) {
            return "";
        }
        return entity.getLabels() != null
                ? entity.getLabels().stream()
                .filter(l -> l.getLangue() != null && codeLang.equalsIgnoreCase(l.getLangue().getCode()))
                .findFirst()
                .map(Label::getNom)
                .orElse("")
                : "";
    }

    public static String getDescriptionValueForLanguage(Entity entity, String codeLang) {
        if (entity == null || codeLang == null) {
            return "";
        }
        return entity.getDescriptions() != null
                ? entity.getDescriptions().stream()
                .filter(d -> d.getLangue() != null && codeLang.equalsIgnoreCase(d.getLangue().getCode()))
                .findFirst()
                .map(Description::getValeur)
                .orElse("")
                : "";
    }
}
