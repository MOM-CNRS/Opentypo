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
    public static final String TARGET_COLLECTION = EntityConstants.ENTITY_TYPE_COLLECTION;
    public static final String TARGET_CATEGORY = EntityConstants.ENTITY_TYPE_CATEGORY;
    public static final String TARGET_GROUP = EntityConstants.ENTITY_TYPE_GROUP;
    public static final String TARGET_SERIE = EntityConstants.ENTITY_TYPE_SERIES;
    public static final String TARGET_TYPE = EntityConstants.ENTITY_TYPE_TYPE;

    @Inject
    private ApplicationBean applicationBean;
    @Inject
    private ReferenceBean referenceBean;
    @Inject
    private CollectionBean collectionBean;
    @Inject
    private CategoryBean categoryBean;
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
            case TARGET_COLLECTION: return "collectionEditForm";
            case TARGET_CATEGORY:  return "categoryEditForm";
            case TARGET_GROUP:     return "groupeEditForm";
            case TARGET_SERIE:     return "serieEditForm";
            case TARGET_TYPE:      return "typeEditForm";
            default:               return "growl";
        }
    }

    /**
     * Cibles d'update pour le dialog (formulaire, growl, panels, arbre).
     * Inclut :leftTreePanel pour les entités affichées dans l'arbre afin de rafraîchir
     * le code après sauvegarde.
     */
    public String getUpdateIds() {
        String base = ":" + getFormId() + ", :growl, :contentPanels";
        if (TARGET_REFERENCE.equals(saveTarget) || TARGET_COLLECTION.equals(saveTarget) || TARGET_CATEGORY.equals(saveTarget)
                || TARGET_GROUP.equals(saveTarget) || TARGET_SERIE.equals(saveTarget) || TARGET_TYPE.equals(saveTarget)) {
            return base + ", :leftTreePanel";
        }
        return base;
    }

    /**
     * Message explicatif affiché dans le dialog (entité concernée).
     */
    public String getMessageText() {
        Entity e = applicationBean != null ? applicationBean.getSelectedEntity() : null;
        String label = (e != null && applicationBean != null) ? applicationBean.getEntityLabel(e) : "";
        if (saveTarget == null) return "Les modifications seront enregistrées dans la base de données.";
        switch (saveTarget) {
            case TARGET_REFERENCE:
                return "Les modifications apportées au référentiel " + label + " seront enregistrées dans la base de données.";
            case TARGET_COLLECTION:
                return "Les modifications apportées à la collection " + label + " seront enregistrées dans la base de données.";
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
            case TARGET_COLLECTION:
                collectionBean.saveCollection(applicationBean);
                break;
            case TARGET_CATEGORY:
                categoryBean.saveCategory(applicationBean);
                break;
            case TARGET_TYPE:
                typeBean.saveEditingType(applicationBean);
                break;
        }
    }
}
