package fr.cnrs.opentypo.presentation.bean.util;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.primefaces.PrimeFaces;

/**
 * Utilitaire de validation pour les entités
 */
public final class EntityValidator {

    private EntityValidator() {
        // Classe utilitaire - pas d'instanciation
    }

    /**
     * Valide le code d'une entité
     * 
     * @param code Le code à valider
     * @param entityRepository Le repository pour vérifier l'unicité
     * @param formId L'ID du formulaire à mettre à jour en cas d'erreur
     * @return true si valide, false sinon
     */
    public static boolean validateCode(String code, EntityRepository entityRepository, String formId) {
        if (code == null || code.trim().isEmpty()) {
            addErrorMessage(EntityConstants.ERROR_CODE_REQUIRED, formId);
            return false;
        }
        
        String codeTrimmed = code.trim();
        
        if (codeTrimmed.length() > EntityConstants.MAX_CODE_LENGTH) {
            addErrorMessage(EntityConstants.ERROR_CODE_TOO_LONG, formId);
            return false;
        }
        
        if (entityRepository.existsByCode(codeTrimmed)) {
            addErrorMessage(EntityConstants.ERROR_CODE_ALREADY_EXISTS, formId);
            return false;
        }
        
        return true;
    }

    /**
     * Valide le label d'une entité
     * 
     * @param label Le label à valider
     * @param formId L'ID du formulaire à mettre à jour en cas d'erreur
     * @return true si valide, false sinon
     */
    public static boolean validateLabel(String label, String formId) {
        if (label == null || label.trim().isEmpty()) {
            addErrorMessage(EntityConstants.ERROR_LABEL_REQUIRED, formId);
            return false;
        }
        
        String labelTrimmed = label.trim();
        if (labelTrimmed.length() > EntityConstants.MAX_LABEL_LENGTH) {
            addErrorMessage(EntityConstants.ERROR_LABEL_TOO_LONG, formId);
            return false;
        }
        
        return true;
    }

    /**
     * Ajoute un message d'erreur
     */
    private static void addErrorMessage(String message, String formId) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", message));
        PrimeFaces.current().ajax().update(":growl, " + formId);
    }
}

