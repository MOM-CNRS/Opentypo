package fr.cnrs.opentypo.converter;

import fr.cnrs.opentypo.entity.Groupe;
import fr.cnrs.opentypo.repository.GroupeRepository;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Optional;

/**
 * Converter JSF pour convertir entre Groupe (entité) et Long (ID)
 */
@Named("groupeConverter")
@FacesConverter(value = "groupeConverter", managed = true)
public class GroupeConverter implements Converter<Object> {

    @Inject
    private GroupeRepository groupeRepository;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Long id = Long.parseLong(value);
            Optional<Groupe> groupeOpt = groupeRepository.findById(id);
            return groupeOpt.orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Groupe) {
            return String.valueOf(((Groupe) value).getId());
        }
        if (value instanceof Long) {
            return String.valueOf(value);
        }
        return "";
    }
}

