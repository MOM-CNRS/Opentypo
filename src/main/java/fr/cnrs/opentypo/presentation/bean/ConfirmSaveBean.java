package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

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
    @Autowired
    private EntityUpdateBean entityUpdateBean;
    @Inject
    private EntityEditModeBean entityEditModeBean;

    private String saveTarget;
    private String dialogMessage;

    /**
     * À appeler avant d'ouvrir le dialog (ex. action du bouton Valider).
     */
    public void prepareSave(String target) {
        this.saveTarget = target;
        refreshDialogMessage();
    }

    /**
     * Enregistre immédiatement : upload des images (fichiers physiques) puis sauvegarde en base.
     * Les images sont chargées sur le serveur et les URLs en base lors du clic sur Enregistrer.
     * Redirige après sauvegarde (PRG) pour éviter la resoumission du formulaire au rafraîchissement.
     */
    public String saveWithImagesImmediately() {
        entityUpdateBean.uploadPendingFilesAndMergeToEditingUrls();
        prepareSaveForSelectedEntity();
        if (!performSave()) {
            return null;
        }
        return "/index.xhtml?faces-redirect=true";
    }

    /**
     * Prépare la sauvegarde en fonction du type de l'entité sélectionnée.
     * Upload les fichiers en attente (stockage différé) avant d'ouvrir le dialog.
     */
    public void prepareSaveForSelectedEntity() {
        entityUpdateBean.uploadPendingFilesAndMergeToEditingUrls();
        Entity e = applicationBean != null ? applicationBean.getSelectedEntity() : null;
        if (e == null || e.getEntityType() == null || e.getEntityType().getCode() == null) {
            prepareSave(TARGET_GROUP);
            return;
        }
        String typeCode = e.getEntityType().getCode();
        if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(typeCode)) {
            prepareSave(TARGET_REFERENCE);
        } else if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(typeCode)) {
            prepareSave(TARGET_COLLECTION);
        } else if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(typeCode)) {
            prepareSave(TARGET_CATEGORY);
        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(typeCode)) {
            prepareSave(TARGET_GROUP);
        } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(typeCode)) {
            prepareSave(TARGET_SERIE);
        } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(typeCode)) {
            prepareSave(TARGET_TYPE);
        } else {
            prepareSave(TARGET_GROUP);
        }
    }

    /**
     * Composants à traiter lors de la confirmation : le formulaire d'édition actif + le bouton.
     * Sans le formulaire, performSave() persiste l'état du bean avant les modifications saisies.
     */
    public String getModifierFormProcessIds() {
        String formId = switch (saveTarget != null ? saveTarget : "") {
            case TARGET_REFERENCE -> ":referenceEditForm";
            case TARGET_COLLECTION -> ":collectionEditForm";
            case TARGET_CATEGORY -> ":categoryEditForm";
            case TARGET_GROUP -> ":groupeEditForm";
            case TARGET_SERIE -> ":serieEditForm";
            case TARGET_TYPE -> ":typeEditForm";
            default -> null;
        };
        return formId != null ? formId + " @this" : "@this";
    }

    /**
     * Cibles d'update pour le dialog (formulaire, growl, panels, arbre).
     * Inclut :leftTreePanel pour les entités affichées dans l'arbre afin de rafraîchir
     * le code après sauvegarde.
     */
    public String getUpdateIds() {
        String base = ":growl :contentPanels :searchSectionWrapper";
        if (TARGET_REFERENCE.equals(saveTarget) || TARGET_COLLECTION.equals(saveTarget) || TARGET_CATEGORY.equals(saveTarget)
                || TARGET_GROUP.equals(saveTarget) || TARGET_SERIE.equals(saveTarget) || TARGET_TYPE.equals(saveTarget)) {
            return base + " :leftTreePanel";
        }
        return base;
    }

    /**
     * IDs à rafraîchir avant d'afficher le dialog de confirmation.
     */
    public String getDialogContentUpdateId() {
        return ":confirmSaveDialogContent";
    }

    /**
     * Message explicatif affiché dans le dialog (entité concernée).
     */
    public String getMessageText() {
        if (dialogMessage == null) {
            refreshDialogMessage();
        }
        return dialogMessage;
    }

    private void refreshDialogMessage() {
        Entity e = applicationBean != null ? applicationBean.getSelectedEntity() : null;
        String label = (e != null && applicationBean != null) ? applicationBean.getEntityLabelHtml(e) : "";
        if (saveTarget == null) {
            dialogMessage = JsfMessages.get("confirmSave.message.default");
            return;
        }
        String key = switch (saveTarget) {
            case TARGET_REFERENCE -> "confirmSave.message.reference";
            case TARGET_COLLECTION -> "confirmSave.message.collection";
            case TARGET_CATEGORY -> "confirmSave.message.category";
            case TARGET_GROUP -> "confirmSave.message.group";
            case TARGET_SERIE -> "confirmSave.message.series";
            case TARGET_TYPE -> "confirmSave.message.type";
            default -> "confirmSave.message.default";
        };
        dialogMessage = JsfMessages.format(key, label);
    }

    /**
     * Exécute la sauvegarde selon le type préparé (appelé par le bouton "Oui, enregistrer" du dialog).
     * @return true si la sauvegarde a réussi
     */
    public boolean performSave() {
        if (saveTarget == null) {
            return false;
        }
        entityUpdateBean.uploadPendingFilesAndMergeToEditingUrls();
        boolean saved = switch (saveTarget) {
            case TARGET_REFERENCE, TARGET_COLLECTION, TARGET_CATEGORY, TARGET_GROUP, TARGET_SERIE, TARGET_TYPE ->
                    entityUpdateBean.saveModification();
            default -> false;
        };
        if (saved) {
            exitEditMode();
        }
        return saved;
    }

    /** Quitte le mode édition catalogue (formulaire modifier). */
    private void exitEditMode() {
        if (entityUpdateBean != null) {
            entityUpdateBean.setEditingEntity(false);
        }
        if (entityEditModeBean != null) {
            entityEditModeBean.cancelEditing();
        }
    }
}
