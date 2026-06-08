package fr.cnrs.opentypo.presentation.bean.util;

import fr.cnrs.opentypo.application.service.EntityCodeUniquenessService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
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
     * Valide le format du code (obligatoire, longueur max).
     */
    public static boolean validateCodeFormat(String code) {
        if (code == null || code.trim().isEmpty()) {
            addErrorMessage(JsfMessages.get("validator.codeRequired"));
            return false;
        }
        if (code.trim().length() > EntityConstants.MAX_CODE_LENGTH) {
            addErrorMessage(JsfMessages.format("validator.codeTooLong", EntityConstants.MAX_CODE_LENGTH));
            return false;
        }
        return true;
    }

    /**
     * Valide le code d'une entité (unicité globale — référentiel, collection, etc.).
     */
    public static boolean validateCode(String code, EntityRepository entityRepository) {
        if (!validateCodeFormat(code)) {
            return false;
        }
        if (entityRepository.existsByCode(code.trim())) {
            addErrorMessage(JsfMessages.get("validator.codeAlreadyExists"));
            return false;
        }
        return true;
    }

    /**
     * Valide le code d'une entité en mode édition (unicité globale, exclut l'entité en cours).
     */
    public static boolean validateCodeForEdit(String code, Long excludeEntityId, EntityRepository entityRepository) {
        if (!validateCodeFormat(code)) {
            return false;
        }
        String codeTrimmed = code.trim();
        if (excludeEntityId != null
                && entityRepository.existsByCodeExcludingEntityId(codeTrimmed, excludeEntityId)) {
            addErrorMessage(JsfMessages.get("validator.codeAlreadyExists"));
            return false;
        }
        if (excludeEntityId == null && entityRepository.existsByCode(codeTrimmed)) {
            addErrorMessage(JsfMessages.get("validator.codeAlreadyExists"));
            return false;
        }
        return true;
    }

    public static boolean validateCategoryCodeForCreate(
            String code, Entity reference, EntityCodeUniquenessService uniquenessService) {
        if (!validateCodeFormat(code)) {
            return false;
        }
        if (reference != null && uniquenessService.isCategoryCodeTakenInReference(reference, code.trim(), null)) {
            addErrorMessage(JsfMessages.get("validator.categoryCodeExistsInReference"));
            return false;
        }
        return true;
    }

    public static boolean validateGroupCodeForCreate(
            String code, Entity reference, EntityCodeUniquenessService uniquenessService) {
        if (!validateCodeFormat(code)) {
            return false;
        }
        if (reference != null && uniquenessService.isGroupCodeTakenInReference(reference, code.trim(), null)) {
            addErrorMessage(JsfMessages.get("validator.groupCodeExistsInReference"));
            return false;
        }
        return true;
    }

    public static boolean validateSerieCodeForCreate(
            String code, Entity group, EntityCodeUniquenessService uniquenessService) {
        if (!validateCodeFormat(code)) {
            return false;
        }
        if (group != null && uniquenessService.isSerieCodeTakenInGroup(group, code.trim(), null)) {
            addErrorMessage(JsfMessages.get("validator.serieCodeExistsInGroup"));
            return false;
        }
        return true;
    }

    public static boolean validateTypeCodeForCreate(
            String code, Entity groupOrParent, EntityCodeUniquenessService uniquenessService) {
        if (!validateCodeFormat(code)) {
            return false;
        }
        return uniquenessService.resolveGroup(groupOrParent)
                .map(group -> {
                    if (uniquenessService.isTypeCodeTakenInGroup(group, code.trim(), null)) {
                        addErrorMessage(JsfMessages.get("validator.typeCodeExistsInGroup"));
                        return false;
                    }
                    return true;
                })
                .orElseGet(() -> {
                    addErrorMessage(JsfMessages.get("validator.codeUniqueGroup"));
                    return false;
                });
    }

    /**
     * Valide le label d'une entité
     */
    public static boolean validateLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            addErrorMessage(JsfMessages.get("validator.labelRequired"));
            return false;
        }

        String labelTrimmed = label.trim();
        if (labelTrimmed.length() > EntityConstants.MAX_LABEL_LENGTH) {
            addErrorMessage(JsfMessages.format("validator.labelTooLong", EntityConstants.MAX_LABEL_LENGTH));
            return false;
        }

        return true;
    }

    private static void addErrorMessage(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"), message));
        PrimeFaces.current().ajax().update(":growl");
    }
}
