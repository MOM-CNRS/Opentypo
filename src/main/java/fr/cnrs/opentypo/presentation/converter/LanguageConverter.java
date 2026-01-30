package fr.cnrs.opentypo.presentation.converter;

import fr.cnrs.opentypo.common.models.Language;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Converter JSF pour convertir entre Language (objet) et String (code) pour le select de langue.
 * Utilise LangueRepository (singleton) au lieu d'ApplicationBean (session) pour éviter ScopeNotActiveException au démarrage.
 */
@Named("languageConverter")
@FacesConverter(value = "languageConverter", managed = true)
public class LanguageConverter implements Converter<Object> {

    @Inject
    private LangueRepository langueRepository;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty() || langueRepository == null) {
            return null;
        }
        Langue langue = langueRepository.findByCode(value.trim());
        if (langue == null) {
            return null;
        }
        return new Language(
                langue.getCode() != null ? langue.getCode().hashCode() : 0,
                langue.getCode(),
                langue.getNom(),
                langue.getCode()
        );
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Language lang) {
            return lang.getCode() != null ? lang.getCode() : "";
        }
        return "";
    }
}
