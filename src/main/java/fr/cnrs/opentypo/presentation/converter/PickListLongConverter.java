package fr.cnrs.opentypo.presentation.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 * Converter pour p:pickList avec DualListModel&lt;Long&gt;.
 * Évite les erreurs de conversion lors de la soumission du formulaire.
 */
@FacesConverter(value = "pickListLongConverter", managed = false)
public class PickListLongConverter implements Converter<Long> {

    @Override
    public Long getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Long value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
