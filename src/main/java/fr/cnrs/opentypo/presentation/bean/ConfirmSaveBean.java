package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Bean partagé pour la boîte de dialogue générique de confirmation de sauvegarde.
 * Préparer avec prepareSave(type), puis afficher le dialog ; le message est calculé selon le type.
 * performSave() exécute la sauvegarde via le bon bean métier.
 */
@Getter
@Setter
@SessionScoped
@Named("confirmSaveBean")
public class ConfirmSaveBean implements Serializable {

    public static final String TARGET_REFERENCE = EntityConstants.ENTITY_TYPE_REFERENCE;
    public static final String TARGET_CATEGORY = EntityConstants.ENTITY_TYPE_CATEGORY;
    public static final String TARGET_GROUP = EntityConstants.ENTITY_TYPE_GROUP;
    public static final String TARGET_SERIE = EntityConstants.ENTITY_TYPE_SERIES;
    public static final String TARGET_TYPE = EntityConstants.ENTITY_TYPE_TYPE;

    @Inject
    private ApplicationBean applicationBean;
    @Inject
    private ReferenceBean referenceBean;
    @Inject
    private CategoryBean categoryBean;
    @Inject
    private GroupBean groupBean;
    @Inject
    private SerieBean serieBean;
    @Inject
    private TypeBean typeBean;

    private String saveTarget;

    /**
     * À appeler avant d'ouvrir le dialog (ex. action du bouton Valider).
     */
    public void prepareSave(String target) {
        this.saveTarget = target;
    }

    /**
     * Id du formulaire à traiter selon le type (pour process/update dans le dialog).
     * Ne doit jamais être vide pour éviter l'expression ":" invalide en JSF.
     */
    public String getFormId() {
        if (saveTarget == null) return "growl";
        switch (saveTarget) {
            case TARGET_REFERENCE: return "referenceEditForm";
            case TARGET_CATEGORY:  return "categoryEditForm";
            case TARGET_GROUP:     return "groupeEditForm";
            case TARGET_SERIE:     return "serieEditForm";
            case TARGET_TYPE:      return "typeEditForm";
            default:               return "growl";
        }
    }

    /**
     * Message explicatif affiché dans le dialog (entité concernée).
     */
    public String getMessageText() {
        Entity e = applicationBean != null ? applicationBean.getSelectedEntity() : null;
        String label = (e != null && e.getCode() != null) ? e.getCode() : "";
        if (saveTarget == null) return "Les modifications seront enregistrées dans la base de données.";
        switch (saveTarget) {
            case TARGET_REFERENCE:
                return "Les modifications apportées à la description et à la référence bibliographique seront enregistrées dans la base de données.";
            case TARGET_CATEGORY:
                return "Les modifications apportées à la catégorie " + label + " seront enregistrées dans la base de données.";
            case TARGET_GROUP:
                return "Les modifications apportées au groupe " + label + " seront enregistrées dans la base de données.";
            case TARGET_SERIE:
                return "Les modifications apportées à la série " + label + " seront enregistrées dans la base de données.";
            case TARGET_TYPE:
                return "Les modifications apportées au type " + label + " seront enregistrées dans la base de données.";
            default:
                return "Les modifications seront enregistrées dans la base de données.";
        }
    }

    /**
     * Exécute la sauvegarde selon le type préparé (appelé par le bouton "Oui, enregistrer" du dialog).
     */
    public void performSave() {
        if (saveTarget == null) return;
        switch (saveTarget) {
            case TARGET_REFERENCE:
                referenceBean.saveReference(applicationBean);
                break;
            case TARGET_CATEGORY:
                categoryBean.saveCategory(applicationBean);
                break;
            case TARGET_GROUP:
                groupBean.saveGroup(applicationBean);
                break;
            case TARGET_SERIE:
                serieBean.saveSerie(applicationBean);
                break;
            case TARGET_TYPE:
                typeBean.saveType(applicationBean);
                break;
            default:
                break;
        }
    }
}
