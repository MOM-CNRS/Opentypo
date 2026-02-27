package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bean partagé pour la boîte de dialogue générique de confirmation de suppression.
 * Préparer avec prepareDelete(type), puis afficher le dialog ; le contenu (titre, message, liste)
 * est calculé selon le type. performDelete() exécute la suppression via le bon bean métier.
 */
@Getter
@Setter
@SessionScoped
@Named("confirmDeleteBean")
public class ConfirmDeleteBean implements Serializable {

    public static final String TARGET_COLLECTION = EntityConstants.ENTITY_TYPE_COLLECTION;
    public static final String TARGET_REFERENCE = EntityConstants.ENTITY_TYPE_REFERENCE;
    public static final String TARGET_CATEGORY = EntityConstants.ENTITY_TYPE_CATEGORY;
    public static final String TARGET_GROUP = EntityConstants.ENTITY_TYPE_GROUP;
    public static final String TARGET_SERIE = EntityConstants.ENTITY_TYPE_SERIES;
    public static final String TARGET_TYPE = EntityConstants.ENTITY_TYPE_TYPE;

    @Inject
    private ApplicationBean applicationBean;
    @Inject
    private EntityRepository entityRepository;
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

    @Inject
    private CollectionBean collectionBean;

    private String deleteTarget;
    /** ID de l'entité à supprimer (pour suppression depuis la liste des référentiels dans une collection). */
    private Long deleteTargetEntityId;

    /**
     * À appeler avant d'ouvrir le dialog (ex. action du bouton Supprimer).
     */
    public void prepareDelete(String target) {
        this.deleteTarget = target;
        this.deleteTargetEntityId = null;
    }

    /**
     * Prépare la suppression d'un référentiel par son ID (depuis la liste collection).
     */
    public void prepareDeleteReference(Long referenceId) {
        this.deleteTarget = TARGET_REFERENCE;
        this.deleteTargetEntityId = referenceId;
    }

    public String getDialogTitle() {
        if (deleteTarget == null) return "Confirmer la suppression";
        switch (deleteTarget) {
            case TARGET_COLLECTION: return "Confirmer la suppression";
            case TARGET_REFERENCE:   return "Confirmer la suppression";
            case TARGET_CATEGORY:   return "Confirmer la suppression";
            case TARGET_GROUP:      return "Confirmer la suppression du groupe";
            case TARGET_SERIE:     return "Confirmer la suppression de la série";
            case TARGET_TYPE:      return "Confirmer la suppression du type";
            default:                return "Confirmer la suppression";
        }
    }

    public String getEntityLabel() {
        Entity e = getEntityToDelete();
        if (e == null) return "";
        if (TARGET_COLLECTION.equals(deleteTarget) || TARGET_REFERENCE.equals(deleteTarget)) {
            return e.getNom() != null ? e.getNom() : e.getCode();
        }
        return e.getCode() != null ? e.getCode() : e.getNom();
    }

    private Entity getEntityToDelete() {
        if (TARGET_REFERENCE.equals(deleteTarget) && deleteTargetEntityId != null && entityRepository != null) {
            return entityRepository.findById(deleteTargetEntityId).orElse(null);
        }
        return applicationBean != null ? applicationBean.getSelectedEntity() : null;
    }

    /** Libellé du type d'entité (la collection, le référentiel, …). */
    public String getEntityTypeLabel() {
        if (deleteTarget == null) return "l'élément";
        switch (deleteTarget) {
            case TARGET_COLLECTION: return "la collection";
            case TARGET_REFERENCE:  return "le référentiel";
            case TARGET_CATEGORY:   return "la catégorie";
            case TARGET_GROUP:      return "le groupe";
            case TARGET_SERIE:     return "la série";
            case TARGET_TYPE:      return "le type";
            default:                return "l'élément";
        }
    }

    /** True si le message doit afficher une liste à puces (détails des entités rattachées). */
    public boolean isHasDetailsList() {
        return !TARGET_TYPE.equals(deleteTarget);
    }

    /** Liste des lignes pour la liste à puces (entités qui seront supprimées). */
    public List<String> getDetailsList() {
        if (deleteTarget == null) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        switch (deleteTarget) {
            case TARGET_COLLECTION:
                list.add("Tous les référentiels");
                list.add("Toutes les catégories");
                list.add("Tous les groupes");
                list.add("Toutes les séries");
                list.add("Tous les types");
                break;
            case TARGET_REFERENCE:
                list.add("Toutes les catégories");
                list.add("Tous les groupes");
                list.add("Toutes les séries");
                list.add("Tous les types");
                break;
            case TARGET_CATEGORY:
                list.add("Tous les groupes");
                list.add("Toutes les séries");
                list.add("Tous les types");
                break;
            case TARGET_GROUP:
                list.add("Toutes les séries du groupe");
                list.add("Tous les types de ces séries");
                break;
            case TARGET_SERIE:
                list.add("Tous les types de cette série");
                break;
            case TARGET_TYPE:
                break;
            default:
                break;
        }
        return list;
    }

    public String getWarningText() {
        return "Cette action est définitive et irréversible. Êtes-vous sûr de vouloir continuer ?";
    }

    /**
     * Exécute la suppression selon le type préparé (appelé par le bouton "Oui, supprimer" du dialog).
     */
    public void performDelete() {
        if (deleteTarget == null) return;
        switch (deleteTarget) {
            case TARGET_COLLECTION:
                collectionBean.deleteCollection(applicationBean);
                break;
            case TARGET_REFERENCE:
                if (deleteTargetEntityId != null) {
                    referenceBean.deleteReferenceById(applicationBean, deleteTargetEntityId);
                    deleteTargetEntityId = null;
                } else {
                    referenceBean.deleteReference(applicationBean);
                }
                break;
            case TARGET_CATEGORY:
                categoryBean.deleteReference(applicationBean);
                break;
            case TARGET_GROUP:
                groupBean.deleteGroup(applicationBean);
                break;
            case TARGET_SERIE:
                serieBean.deleteSerie(applicationBean);
                break;
            case TARGET_TYPE:
                typeBean.deleteType(applicationBean);
                break;
            default:
                break;
        }
    }
}
