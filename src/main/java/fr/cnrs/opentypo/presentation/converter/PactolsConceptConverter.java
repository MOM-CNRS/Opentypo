package fr.cnrs.opentypo.presentation.converter;

import fr.cnrs.opentypo.application.dto.pactols.PactolsConcept;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 * Converter pour PactolsConcept dans les autocompletes.
 */
@FacesConverter(value = "pactolsConceptConverter", managed = false)
public class PactolsConceptConverter implements Converter<PactolsConcept> {

    private static final String SEP = "|||";

    @Override
    public PactolsConcept getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) return null;
        String[] parts = value.split("\\|\\|\\|", -1);
        if (parts.length < 2) return null;
        String id = parts[0];
        String term = parts.length > 1 ? parts[1] : id;
        String uri = parts.length > 2 ? parts[2] : null;
        return new PactolsConcept(id, uri, term);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, PactolsConcept value) {
        if (value == null) return "";
        String id = value.getIdConcept() != null ? value.getIdConcept() : "";
        String term = value.getSelectedTerm() != null ? value.getSelectedTerm() : id;
        String uri = value.getUri() != null ? value.getUri() : "";
        return id + SEP + term + SEP + uri;
    }
}
