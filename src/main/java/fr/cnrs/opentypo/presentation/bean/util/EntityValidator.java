package fr.cnrs.opentypo.presentation.bean.util;

import fr.cnrs.opentypo.application.service.EntityCodeUniquenessService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
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
     * Valide le format du code (obligatoire, longueur max).
     */
    public static boolean validateCodeFormat(String code) {
        if (code == null || code.trim().isEmpty()) {
            addErrorMessage(EntityConstants.ERROR_CODE_REQUIRED);
            return false;
        }
        if (code.trim().length() > EntityConstants.MAX_CODE_LENGTH) {
            addErrorMessage(EntityConstants.ERROR_CODE_TOO_LONG);
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
            addErrorMessage(EntityConstants.ERROR_CODE_ALREADY_EXISTS);
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
            addErrorMessage(EntityConstants.ERROR_CODE_ALREADY_EXISTS);
            return false;
        }
        if (excludeEntityId == null && entityRepository.existsByCode(codeTrimmed)) {
            addErrorMessage(EntityConstants.ERROR_CODE_ALREADY_EXISTS);
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
            addErrorMessage(EntityConstants.ERROR_CATEGORY_CODE_EXISTS_IN_REFERENCE);
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
            addErrorMessage(EntityConstants.ERROR_GROUP_CODE_EXISTS_IN_REFERENCE);
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
            addErrorMessage(EntityConstants.ERROR_SERIE_CODE_EXISTS_IN_GROUP);
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
                        addErrorMessage(EntityConstants.ERROR_TYPE_CODE_EXISTS_IN_GROUP);
                        return false;
                    }
                    return true;
                })
                .orElseGet(() -> {
                    addErrorMessage("Impossible de déterminer le groupe pour valider l'unicité du code.");
                    return false;
                });
    }

    /**
     * Valide le label d'une entité
     */
    public static boolean validateLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            addErrorMessage(EntityConstants.ERROR_LABEL_REQUIRED);
            return false;
        }

        String labelTrimmed = label.trim();
        if (labelTrimmed.length() > EntityConstants.MAX_LABEL_LENGTH) {
            addErrorMessage(EntityConstants.ERROR_LABEL_TOO_LONG);
            return false;
        }

        return true;
    }

    private static void addErrorMessage(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", message));
        PrimeFaces.current().ajax().update(":growl");
    }
}
