package fr.cnrs.opentypo.presentation.bean.util;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

/**
 * Validateur pour le champ de recherche : longueur minimale 2 caractères.
 * Affiche uniquement le message : "La longueur est inférieure à la valeur minimale autorisée, \"2\"."
 */
@FacesValidator("searchTermMinLengthValidator")
public class SearchTermMinLengthValidator implements Validator<Object> {

    private static final int MIN_LENGTH = 2;
    private static final String MESSAGE = "La longueur est inférieure à la valeur minimale autorisée, \"2\".";

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (value == null) {
            return;
        }
        String str = value.toString();
        if (str.length() < MIN_LENGTH) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", MESSAGE);
            throw new ValidatorException(msg);
        }
    }
}
