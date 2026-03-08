package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
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

    private String saveTarget;

    /**
     * À appeler avant d'ouvrir le dialog (ex. action du bouton Valider).
     */
    public void prepareSave(String target) {
        this.saveTarget = target;
    }

    /**
     * Enregistre immédiatement : upload des images (fichiers physiques) puis sauvegarde en base.
     * Les images sont chargées sur le serveur et les URLs en base lors du clic sur Enregistrer.
     * Redirige après sauvegarde (PRG) pour éviter la resoumission du formulaire au rafraîchissement.
     */
    public String saveWithImagesImmediately() {
        entityUpdateBean.uploadPendingFilesAndMergeToEditingUrls();
        prepareSaveForSelectedEntity();
        performSave();
        return "/index.xhtml?faces-redirect=true";
    }

    /**
     * Prépare la sauvegarde en fonction du type de l'entité sélectionnée
     * (entityType.id: 1=REFERENTIEL, 6=COLLECTION, autre=GROUPE).
     * Upload les fichiers en attente (stockage différé) avant d'ouvrir le dialog.
     */
    public void prepareSaveForSelectedEntity() {
        entityUpdateBean.uploadPendingFilesAndMergeToEditingUrls();
        Entity e = applicationBean != null ? applicationBean.getSelectedEntity() : null;
        if (e == null || e.getEntityType() == null || e.getEntityType().getId() == null) {
            prepareSave(TARGET_GROUP);
            return;
        }
        long typeId = e.getEntityType().getId();
        if (typeId == 1) {
            prepareSave(TARGET_REFERENCE);
        } else if (typeId == 6) {
            prepareSave(TARGET_COLLECTION);
        } else {
            prepareSave(TARGET_GROUP);
        }
    }

    /**
     * Cibles d'update pour le dialog (formulaire, growl, panels, arbre).
     * Inclut :leftTreePanel pour les entités affichées dans l'arbre afin de rafraîchir
     * le code après sauvegarde.
     */
    public String getUpdateIds() {
        String base = ":growl, :contentPanels";
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
            case TARGET_COLLECTION:
            case TARGET_GROUP:
                entityUpdateBean.saveModification();
                break;
            case TARGET_CATEGORY:
                categoryBean.saveCategory(applicationBean);
                break;
            case TARGET_TYPE:
                typeBean.saveEditingType(applicationBean);
                break;
        }
    }

    /**
     * Exécute la sauvegarde puis redirige (PRG) pour éviter la resoumission au rafraîchissement.
     * Le paramètre scrollTop permet de déclencher le scroll en haut avec effet au chargement.
     */
    public String performSaveAndRedirect() {
        performSave();
        return "/index.xhtml?faces-redirect=true&scrollTop=1";
    }
}
