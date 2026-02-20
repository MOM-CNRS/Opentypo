package fr.cnrs.opentypo.presentation.converter;

import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Optional;

/**
 * Converter JSF pour convertir entre Utilisateur (entité) et Long (ID)
 */
@Named("utilisateurConverter")
@FacesConverter(value = "utilisateurConverter", managed = true)
public class UtilisateurConverter implements Converter<Object> {

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Long id = Long.parseLong(value);
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(id);
            return utilisateurOpt.orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Utilisateur) {
            return String.valueOf(((Utilisateur) value).getId());
        }
        if (value instanceof Long) {
            return String.valueOf(value);
        }
        return "";
    }
}
