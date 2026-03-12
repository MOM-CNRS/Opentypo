package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.application.service.CollectionService;
import fr.cnrs.opentypo.application.service.ReferenceService;
import fr.cnrs.opentypo.application.service.TypeService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.CandidatBean;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatReferenceTreeService;
import fr.cnrs.opentypo.presentation.bean.util.EntityUtils;
import fr.cnrs.opentypo.presentation.bean.util.EntityValidator;
import io.micrometer.common.util.StringUtils;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@SessionScoped
@Named(value = "typeBean")
@Slf4j
public class TypeBean implements Serializable {

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private LoginBean loginBean;

    @Autowired
    private Provider<TreeBean> treeBeanProvider;
    
    @Autowired
    private Provider<ApplicationBean> applicationBeanProvider;

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Autowired
    private TypeService typeService;

    @Autowired
    private LangueRepository langueRepository;

    @Autowired
    private SearchBean searchBean;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private CandidatReferenceTreeService candidatReferenceTreeService;

    @Autowired
    private CandidatBean candidatBean;

    @Autowired
    private EntityEditModeBean entityEditModeBean;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    private String typeCode;
    private String typeLabel;
    private String typeDescription;
    private String typeDialogCode;
    private boolean editingType = false;
    private String editingTypeCode;
    private String editingLabelLangueCode;
    private String editingTypeLabel;
    private String editingDescriptionLangueCode;
    private String editingTypeDescription;
    private String editingTypeCommentaire;
    private String editingTypeStatut;
    private Integer editingTypeTpq;
    private Integer editingTypeTaq;

    /** ID du parent sélectionné pour le déplacement du type. */
    private Long moveTypeSelectedParentId;

    /** IDs pour le déplacement par drag-and-drop depuis l'arbre. */
    private Long moveTypeIdForDrag;
    private Long moveTypeNewParentIdForDrag;

    /** Message et contexte pour le dialogue de confirmation de déplacement. */
    private String confirmMoveMessage;
    private boolean confirmMoveFromDialog;


    public void resetTypeForm() {
        typeCode = null;
        typeLabel = null;
        typeDescription = null;
    }

    public void resetTypeDialogForm() {
        typeDialogCode = null;
    }

    public void prepareCreateType() {
        resetTypeDialogForm();
    }

    @Transactional
    public void createTypeFromDialog() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ApplicationBean applicationBean = applicationBeanProvider.get();

        Entity parent = null;
        if (applicationBean.getSelectedEntity() != null
                && applicationBean.getSelectedEntity().getEntityType() != null) {
            String typeCode = applicationBean.getSelectedEntity().getEntityType().getCode();
            if (EntityConstants.ENTITY_TYPE_GROUP.equals(typeCode) || EntityConstants.ENTITY_TYPE_SERIES.equals(typeCode)) {
                parent = applicationBean.getSelectedEntity();
            }
        }
        if (parent == null) {
            parent = applicationBean.getSelectedGroup();
        }

        if (parent == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Aucun groupe ou série n'est sélectionné. Veuillez sélectionner un groupe ou une série avant de créer un type."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        if (StringUtils.isEmpty(typeDialogCode)) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Le code du type ne doit pas être vide."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        // Validation : unicité du code (EntityValidator.validateCode vérifie aussi vide et longueur)
        if (!EntityValidator.validateCode(typeDialogCode, entityRepository)) {
            return;
        }

        String codeTrimmed = typeDialogCode.trim();

        try {
            EntityType typeEntityType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_TYPE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Le type d'entité 'TYPE' n'existe pas dans la base de données."));

            Entity newType = new Entity();
            newType.setCode(codeTrimmed);
            newType.setEntityType(typeEntityType);
            newType.setCreateDate(LocalDateTime.now());
            newType.setStatut(EntityStatusEnum.PROPOSITION.name());

            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                newType.setCreateBy(currentUser.getEmail());
                List<Utilisateur> auteurs = new ArrayList<>();
                auteurs.add(currentUser);
                newType.setAuteurs(auteurs);
            }

            Entity savedType = entityRepository.save(newType);

            if (!entityRelationRepository.existsByParentAndChild(parent.getId(), savedType.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(parent);
                relation.setChild(savedType);
                entityRelationRepository.save(relation);
            }

            applicationBean.refreshGroupTypesList();
            TreeBean treeBean = treeBeanProvider.get();
            if (treeBean != null) {
                treeBean.addEntityToTree(savedType, parent);
            }

            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    "Le type '" + codeTrimmed + "' a été créé avec succès."));

            resetTypeDialogForm();
            PrimeFaces.current().executeScript("PF('typeDialog').hide();");
            PrimeFaces.current().ajax().update(":growl, :typeDialogForm, :typesContent, :centerContent");
        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création du type", e);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage()));
            PrimeFaces.current().ajax().update(":typeDialogForm, :growl");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création du type", e);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Une erreur est survenue lors de la création du type : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":typeDialogForm, :growl");
        }
    }

    /** Prépare le dialog de déplacement du type : charge les parents possibles et réinitialise la sélection. */
    public void prepareMoveTypeDialog() {
        moveTypeSelectedParentId = null;
    }

    /**
     * Liste des groupes et séries pouvant être parents, groupés par collection (tout le projet).
     * Exclut le parent actuel. Utilisé pour le dialogue de déplacement avec recherche.
     */
    public List<SelectItem> getMoveTypeParentOptions() {
        List<SelectItem> result = new ArrayList<>();
        ApplicationBean appBean = applicationBeanProvider.get();
        Entity type = appBean != null ? appBean.getSelectedEntity() : null;
        Entity currentParent = getMoveTypeCurrentParent();
        if (type == null || appBean == null) return result;

        if (appBean.getCollections() == null || appBean.getCollections().isEmpty()) {
            appBean.loadAllCollections();
        }
        List<Entity> collections = appBean.getCollections();
        if (collections == null) return result;

        for (Entity collection : collections) {
            if (collection == null || !appBean.isEntityVisibleForCurrentUser(collection)) continue;
            List<Entity> references = referenceService.loadReferencesByCollection(collection);
            if (references == null || references.isEmpty()) continue;

            List<SelectItem> groupItems = new ArrayList<>();
            for (Entity reference : references) {
                if (reference == null || !appBean.isEntityVisibleForCurrentUser(reference)) continue;
                List<Entity> parents = typeService.getPossibleParentsForReference(reference);
                for (Entity parent : parents) {
                    if (parent == null || parent.getId() == null) continue;
                    if (currentParent != null && parent.getId().equals(currentParent.getId())) continue;
                    groupItems.add(new SelectItem(parent.getId(), getParentOptionLabel(parent)));
                }
            }
            if (!groupItems.isEmpty()) {
                String collectionLabel = appBean.getEntityLabel(collection);
                if (collectionLabel == null) collectionLabel = collection.getCode();
                SelectItemGroup group = new SelectItemGroup("📁 " + collectionLabel);
                group.setSelectItems(groupItems.toArray(new SelectItem[0]));
                result.add(group);
            }
        }
        return result;
    }

    private Entity getMoveTypeCurrentParent() {
        ApplicationBean appBean = applicationBeanProvider.get();
        Entity type = appBean != null ? appBean.getSelectedEntity() : null;
        if (type == null) return null;
        List<Entity> parents = entityRelationRepository.findParentsByChild(type);
        return (parents != null && !parents.isEmpty()) ? parents.get(0) : null;
    }

    /** Libellé du parent actuel pour affichage dans le dialog. */
    public String getMoveTypeCurrentParentLabel() {
        Entity parent = getMoveTypeCurrentParent();
        return parent != null ? getParentOptionLabel(parent) : "—";
    }

    /** Prépare le message pour le dialogue de confirmation (depuis le dialog de déplacement). */
    public void prepareConfirmMoveFromDialog() {
        ApplicationBean appBean = applicationBeanProvider.get();
        Entity type = appBean != null ? appBean.getSelectedEntity() : null;
        Entity newParent = moveTypeSelectedParentId != null ? entityRepository.findById(moveTypeSelectedParentId).orElse(null) : null;
        confirmMoveFromDialog = true;
        if (type != null && newParent != null) {
            confirmMoveMessage = "Voulez-vous déplacer le type \"" + (type.getCode() != null ? type.getCode() : type.getNom()) + "\" vers "
                    + getParentOptionLabel(newParent) + " ?";
        } else {
            confirmMoveMessage = "Voulez-vous déplacer ce type vers le nouveau parent sélectionné ?";
        }
    }

    /** Prépare le message pour le dialogue de confirmation (depuis le drag-and-drop). */
    public void prepareConfirmMoveFromDrag() {
        Entity type = moveTypeIdForDrag != null ? entityRepository.findById(moveTypeIdForDrag).orElse(null) : null;
        Entity newParent = moveTypeNewParentIdForDrag != null ? entityRepository.findById(moveTypeNewParentIdForDrag).orElse(null) : null;
        confirmMoveFromDialog = false;
        if (type != null && newParent != null) {
            confirmMoveMessage = "Voulez-vous déplacer le type \"" + (type.getCode() != null ? type.getCode() : type.getNom()) + "\" vers "
                    + getParentOptionLabel(newParent) + " ?";
        } else {
            confirmMoveMessage = "Voulez-vous déplacer ce type vers ce parent ?";
        }
    }

    /** Exécute le déplacement après confirmation (dialog ou drag). */
    public void performConfirmMove() {
        if (confirmMoveFromDialog) {
            changeTypeParentFromDialog();
        } else {
            moveTypeByDragAndDrop();
        }
    }

    /** Libellé formaté pour une option parent (Groupe ou Série). */
    public String getParentOptionLabel(Entity parent) {
        if (parent == null) return "";

        if (parent.getEntityType() != null) {
            if (EntityConstants.ENTITY_TYPE_GROUP.equals(parent.getEntityType().getCode())) {
                return "Groupe : " + parent.getCode();
            }

            if (EntityConstants.ENTITY_TYPE_SERIES.equals(parent.getEntityType().getCode())) {
                return "Série : " + parent.getCode();
            }
        }
        return parent.getCode();
    }

    /** Exécute le déplacement du type vers le nouveau parent. */
    public void changeTypeParentFromDialog() {

        Entity type = applicationBeanProvider.get().getSelectedEntity();
        if (type == null || moveTypeSelectedParentId == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Veuillez sélectionner un nouveau parent."));
            PrimeFaces.current().ajax().update(":moveTypeDialogForm :growl");
            return;
        }

        Entity newParent = entityRepository.findById(moveTypeSelectedParentId).orElse(null);
        if (newParent == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Parent introuvable."));
            PrimeFaces.current().ajax().update(":moveTypeDialogForm :growl");
            return;
        }

        // Récupérer l'ancien parent avant le déplacement pour rafraîchir l'arbre
        Entity oldParent = null;
        List<Entity> oldParents = entityRelationRepository.findParentsByChild(type);
        if (oldParents != null && !oldParents.isEmpty()) {
            oldParent = oldParents.get(0);
        }

        typeService.changeTypeParent(type, newParent);

        ApplicationBean appBean = applicationBeanProvider.get();
        appBean.refreshChilds();
        appBean.setBeadCrumbElements(appBean.buildBreadcrumbFromSelectedEntity());

        TreeBean treeBean = treeBeanProvider.get();
        Entity newParentCollection = collectionService.findCollectionIdByEntityId(newParent.getId());
        Entity currentCollection = appBean.getSelectedCollection();

        if (treeBean != null) {
            // Recharger l'ancien parent pour retirer le type de l'arbre à l'ancien emplacement
            if (oldParent != null) {
                treeBean.reloadChildrenForEntity(oldParent);
            }
            // Si le nouveau parent est dans une autre collection, basculer la vue vers cette collection
            if (newParentCollection != null && currentCollection != null
                    && !newParentCollection.getId().equals(currentCollection.getId())) {
                CollectionBean collBean = appBean.getCollectionBean();
                if (collBean != null) {
                    collBean.showCollectionDetail(appBean, newParentCollection);
                }
            }
            // Recharger le nouveau parent (même collection) ou l'arbre a déjà été réinitialisé (autre collection)
            if (newParentCollection != null && currentCollection != null
                    && newParentCollection.getId().equals(currentCollection.getId())) {
                treeBean.reloadChildrenForEntity(newParent);
            }
            treeBean.expandPathAndSelectEntity(type);
        }

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                "Le type a été déplacé vers " + getParentOptionLabel(newParent) + "."));
        PrimeFaces.current().executeScript("PF('moveTypeDialog').hide();");
        PrimeFaces.current().executeScript("setTimeout(function(){ if(typeof applyTreeSelectionHighlight==='function') applyTreeSelectionHighlight(); if(typeof hideLoading==='function') hideLoading(); }, 400);");
    }

    /**
     * Déplace un type vers un nouveau parent (appelé depuis le drag-and-drop dans l'arbre).
     * Lit moveTypeIdForDrag et moveTypeNewParentIdForDrag depuis le formulaire,
     * puis délègue à changeTypeParentFromDialog.
     */
    public void moveTypeByDragAndDrop() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (moveTypeIdForDrag == null || moveTypeNewParentIdForDrag == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Paramètres de déplacement invalides."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        Entity type = entityRepository.findById(moveTypeIdForDrag).orElse(null);
        Entity newParent = entityRepository.findById(moveTypeNewParentIdForDrag).orElse(null);
        if (type == null || newParent == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Type ou parent introuvable."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        if (type.getEntityType() == null || !EntityConstants.ENTITY_TYPE_TYPE.equals(type.getEntityType().getCode())) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "L'élément déplacé n'est pas un type."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        if (newParent.getEntityType() == null
                || (!EntityConstants.ENTITY_TYPE_GROUP.equals(newParent.getEntityType().getCode())
                && !EntityConstants.ENTITY_TYPE_SERIES.equals(newParent.getEntityType().getCode()))) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le parent cible doit être un groupe ou une série."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        List<Entity> oldParents = entityRelationRepository.findParentsByChild(type);
        Entity oldParent = (oldParents != null && !oldParents.isEmpty()) ? oldParents.get(0) : null;
        if (oldParent != null && oldParent.getId().equals(newParent.getId())) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "Le type est déjà à cet emplacement."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        if (!canMoveType(type)) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Vous n'avez pas les droits pour déplacer ce type."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        ApplicationBean appBean = applicationBeanProvider.get();
        appBean.setSelectedEntity(type);
        moveTypeSelectedParentId = newParent.getId();
        changeTypeParentFromDialog();
    }

    public void createType() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Validation des champs obligatoires
        if (!EntityValidator.validateCode(typeCode, entityRepository) || !EntityValidator.validateLabel(typeLabel)) {
            return;
        }

        ApplicationBean applicationBean = applicationBeanProvider.get();
        
        // Vérifier qu'un groupe est sélectionné
        if (applicationBean == null || applicationBean.getSelectedGroup() == null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Aucun groupe n'est sélectionné. Veuillez sélectionner un groupe avant de créer un type."));
            PrimeFaces.current().ajax().update(":growl, :typeForm");
            return;
        }

        String codeTrimmed = typeCode.trim();
        String labelTrimmed = typeLabel.trim();
        String descriptionTrimmed = (typeDescription != null && !typeDescription.trim().isEmpty())
                ? typeDescription.trim() : null;

        try {
            // Récupérer le type d'entité TYPE
            EntityType typeEntityType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_TYPE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Le type d'entité 'TYPE' n'existe pas dans la base de données."));

            // Créer la nouvelle entité type
            Entity newType = new Entity();
            newType.setCode(codeTrimmed);
            newType.setCommentaire(descriptionTrimmed);
            newType.setEntityType(typeEntityType);
            newType.setStatut(EntityStatusEnum.PROPOSITION.name());
            newType.setCreateDate(LocalDateTime.now());

            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                newType.setCreateBy(currentUser.getEmail());
                List<Utilisateur> auteurs = new ArrayList<>();
                auteurs.add(currentUser);
                newType.setAuteurs(auteurs);
            }

            // Sauvegarder le type
            Entity savedType = entityRepository.save(newType);

            // Créer la relation entre le groupe (parent) et le type (child)
            if (!entityRelationRepository.existsByParentAndChild(
                    applicationBean.getSelectedGroup().getId(), savedType.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(applicationBean.getSelectedGroup());
                relation.setChild(savedType);
                entityRelationRepository.save(relation);
            }

            // Recharger la liste des types
            applicationBean.refreshGroupTypesList();

            // Ajouter le type à l'arbre
            TreeBean treeBean = treeBeanProvider.get();
            if (treeBean != null) {
                treeBean.addEntityToTree(savedType, applicationBean.getSelectedGroup());
            }

            // Message de succès
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Succès",
                            "Le type '" + labelTrimmed + "' a été créé avec succès."));

            resetTypeForm();

            // Mettre à jour les composants : growl, formulaire, arbre, et conteneur des types
            PrimeFaces.current().ajax().update(":growl, :typeForm, :typesContainer");

        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création du type", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :typeForm");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création du type", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Une erreur est survenue lors de la création du type : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :typeForm");
        }
    }

    /** Annule le mode édition et rafraîchit l'entité (utilisé par modifier Retour). */
    public void cancelEditingType(ApplicationBean appBean) {
        entityEditModeBean.cancelEditing();
        if (appBean != null && appBean.getSelectedEntity() != null && appBean.getSelectedEntity().getId() != null) {
            appBean.setSelectedEntity(entityRepository.findById(appBean.getSelectedEntity().getId()).orElse(appBean.getSelectedEntity()));
        }
    }

    /** Annule le mode édition sans refresh (utilisé par deleteType). */
    public void cancelEditingType() {
        entityEditModeBean.cancelEditing();
        editingType = false;
        editingTypeCode = null;
        editingLabelLangueCode = null;
        editingDescriptionLangueCode = null;
        editingTypeLabel = null;
        editingTypeDescription = null;
        editingTypeCommentaire = null;
        editingTypeStatut = null;
        editingTypeTpq = null;
        editingTypeTaq = null;
    }

    /** Enregistre les modifications et sort du mode édition si succès. */
    public void saveEditingType(ApplicationBean appBean) {
        if (candidatBean.performEnregistrerModifications()) {
            cancelEditingType(appBean);
        }
    }

    @Transactional
    public void saveType(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null) {
            return;
        }
        Entity typeToUpdate = entityRepository.findById(applicationBean.getSelectedEntity().getId()).orElse(null);
        if (typeToUpdate == null) {
            return;
        }

        String newCode = editingTypeCode != null ? editingTypeCode.trim() : null;
        if (newCode != null && !newCode.isEmpty() && !Objects.equals(newCode, typeToUpdate.getCode())) {
            typeToUpdate.setCode(newCode);
        }

        String labelLangueCode = editingLabelLangueCode != null ? editingLabelLangueCode : "fr";
        Langue labelLangue = langueRepository.findByCode(labelLangueCode);
        String newLabel = editingTypeLabel != null ? editingTypeLabel.trim() : "";
        String currentLabelValue = EntityUtils.getLabelValueForLanguage(typeToUpdate, labelLangueCode);
        if (labelLangue != null && !Objects.equals(newLabel, currentLabelValue)) {
            Optional<Label> labelOpt = typeToUpdate.getLabels() != null
                    ? typeToUpdate.getLabels().stream()
                    .filter(l -> l.getLangue() != null && labelLangueCode.equalsIgnoreCase(l.getLangue().getCode()))
                    .findFirst()
                    : Optional.empty();
            if (labelOpt.isPresent()) {
                labelOpt.get().setNom(newLabel);
            } else {
                Label newLabelEntity = new Label();
                newLabelEntity.setNom(newLabel);
                newLabelEntity.setEntity(typeToUpdate);
                newLabelEntity.setLangue(labelLangue);
                if (typeToUpdate.getLabels() == null) {
                    typeToUpdate.setLabels(new ArrayList<>());
                }
                typeToUpdate.getLabels().add(newLabelEntity);
            }
        }

        String descLangueCode = editingDescriptionLangueCode != null ? editingDescriptionLangueCode : "fr";
        Langue descLangue = langueRepository.findByCode(descLangueCode);
        String newDesc = editingTypeDescription != null ? editingTypeDescription.trim() : "";
        String currentDescValue = EntityUtils.getDescriptionValueForLanguage(typeToUpdate, descLangueCode);
        if (descLangue != null && !Objects.equals(newDesc, currentDescValue)) {
            Optional<Description> descOpt = typeToUpdate.getDescriptions() != null
                    ? typeToUpdate.getDescriptions().stream()
                    .filter(d -> d.getLangue() != null && descLangueCode.equalsIgnoreCase(d.getLangue().getCode()))
                    .findFirst()
                    : Optional.empty();
            if (descOpt.isPresent()) {
                descOpt.get().setValeur(newDesc);
            } else {
                Description newDescEntity = new Description();
                newDescEntity.setValeur(newDesc);
                newDescEntity.setEntity(typeToUpdate);
                newDescEntity.setLangue(descLangue);
                if (typeToUpdate.getDescriptions() == null) {
                    typeToUpdate.setDescriptions(new ArrayList<>());
                }
                typeToUpdate.getDescriptions().add(newDescEntity);
            }
        }

        if (editingTypeCommentaire != null) {
            typeToUpdate.setMetadataCommentaire(editingTypeCommentaire.trim().isEmpty() ? null : editingTypeCommentaire.trim());
        }
        if (editingTypeStatut != null) {
            typeToUpdate.setStatut(editingTypeStatut.trim().isEmpty() ? null : editingTypeStatut.trim());
        }
        if (editingTypeTpq != null) {
            typeToUpdate.setTpq(editingTypeTpq);
        }
        if (editingTypeTaq != null) {
            typeToUpdate.setTaq(editingTypeTaq);
        }

        Entity saved = entityRepository.save(typeToUpdate);
        applicationBean.setSelectedEntity(saved);
        int idx = applicationBean.getBeadCrumbElements().size() - 1;
        if (idx >= 0) {
            applicationBean.getBeadCrumbElements().set(idx, saved);
        }
        TreeBean tb = treeBeanProvider != null ? treeBeanProvider.get() : null;
        if (tb != null) {
            tb.expandPathAndSelectEntity(saved);
        }
        cancelEditingType();
        applicationBean.refreshGroupTypesList();
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Les modifications du type ont été enregistrées."));
    }

    /**
     * Supprime le type sélectionné (et ses éventuels enfants) de manière récursive.
     */
    @Transactional
    public void deleteType(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null || applicationBean.getSelectedEntity().getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Aucun type sélectionné."));
            return;
        }
        try {
            Entity type = applicationBean.getSelectedEntity();
            String typeCode = type.getCode();
            Long typeId = type.getId();
            Entity parentSerie = applicationBean.getSelectedSerie();

            applicationBean.deleteEntityRecursively(type);

            applicationBean.setSelectedEntity(parentSerie);
            applicationBean.setChilds(new ArrayList<>());
            if (!applicationBean.getBeadCrumbElements().isEmpty()) {
                applicationBean.getBeadCrumbElements().removeLast();
            }
            if (parentSerie != null) {
                applicationBean.refreshChilds();
                applicationBean.getPanelState().showSerie();
            } else {
                applicationBean.getPanelState().showCollections();
            }
            TreeBean tb = treeBeanProvider != null ? treeBeanProvider.get() : null;
            if (tb != null) {
                tb.initializeTreeWithCollection();
            }
            cancelEditingType();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                            "Le type '" + typeCode + "' et toutes les entités rattachées ont été supprimés."));
            log.info("Type supprimé avec succès: {} (ID: {})", typeCode, typeId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du type", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                            "Une erreur est survenue lors de la suppression : " + e.getMessage()));
        }
    }

    public String getCollectionLabel(Long entityId) {
        Entity entity = collectionService.findCollectionIdByEntityId(entityId);
        return candidatReferenceTreeService.getCollectionLabel(entity, searchBean.getLangSelected());
    }


    /**
     * Indique si l'utilisateur connecté peut créer un type (bouton Créer un nouveau type).
     * Visible si : administrateur technique, gestionnaire de la collection, gestionnaire du référentiel,
     * ou rédacteur du groupe contenant l'entité.
     * Utilisé quand selectedEntity est une série ou un groupe.
     */
    public boolean canCreateType(ApplicationBean applicationBean) {
        if (!loginBean.isAuthenticated() || applicationBean.getSelectedEntity() == null) {
            return false;
        }
        if (loginBean.isAdminTechnique()) {
            return true;
        }
        Long userId = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
        if (userId == null) return false;

        Entity collection = applicationBean.getSelectedCollection();
        if (collection != null && collection.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, collection.getId(),
                PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())) {
            return true;
        }
        Entity reference = applicationBean.getSelectedReference();
        if (reference != null && reference.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, reference.getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        Entity group = applicationBean.getSelectedGroup();
        return group != null && group.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, group.getId(), PermissionRoleEnum.REDACTEUR.getLabel());
    }

    /**
     * Indique si l'utilisateur connecté peut déplacer un type (drag-and-drop dans l'arbre).
     * Restreint à : administrateur technique, gestionnaire de la collection, ou gestionnaire du référentiel.
     */
    public boolean canMoveType(Entity type) {
        if (type == null || type.getId() == null) return false;
        if (type.getEntityType() == null || !EntityConstants.ENTITY_TYPE_TYPE.equals(type.getEntityType().getCode())) {
            return false;
        }
        if (!loginBean.isAuthenticated()) return false;
        if (loginBean.isAdminTechnique()) return true;
        Long userId = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
        if (userId == null) return false;

        Entity collection = collectionService.findCollectionIdByEntityId(type.getId());
        if (collection != null && collection.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, collection.getId(),
                PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())) {
            return true;
        }
        Entity reference = typeService.findReferenceAncestor(type);
        if (reference != null && reference.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, reference.getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        return false;
    }

    /**
     * Indique si l'utilisateur connecté peut modifier le type (bouton Modifier sur un type).
     * Visible si : administrateur technique, gestionnaire de la collection, gestionnaire du référentiel,
     * rédacteur ou valideur du groupe contenant le type.
     */
    public boolean canEditType(ApplicationBean applicationBean) {
        if (!loginBean.isAuthenticated() || applicationBean.getSelectedEntity() == null) {
            return false;
        }
        if (loginBean.isAdminTechnique()) {
            return true;
        }
        Long userId = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
        if (userId == null) return false;

        Entity collection = applicationBean.getSelectedCollection();
        if (collection != null && collection.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, collection.getId(),
                PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())) {
            return true;
        }
        Entity reference = applicationBean.getSelectedReference();
        if (reference != null && reference.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, reference.getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        Entity group = applicationBean.getSelectedGroup();
        if (group != null && group.getId() != null) {
            if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, group.getId(), PermissionRoleEnum.REDACTEUR.getLabel())) {
                return true;
            }
            return userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, group.getId(), PermissionRoleEnum.VALIDEUR.getLabel());
        }
        return false;
    }


    /**
     * Indique si l'utilisateur connecté peut publier ou refuser une proposition (type).
     * Visible si : type en statut PROPOSITION, et l'un des rôles : administrateur technique,
     * gestionnaire de la collection, gestionnaire du référentiel, ou valideur du groupe contenant le type.
     */
    public boolean canPublishOrRefusePropositionType(ApplicationBean applicationBean) {
        if (!loginBean.isAuthenticated() || applicationBean.getSelectedEntity() == null
                || !EntityStatusEnum.PROPOSITION.name().equals(applicationBean.getSelectedEntity().getStatut())) {
            return false;
        }
        if (applicationBean.getSelectedEntity().getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_TYPE.equals(applicationBean.getSelectedEntity().getEntityType().getCode())) {
            return false;
        }
        if (loginBean.isAdminTechnique()) {
            return true;
        }
        Long userId = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
        if (userId == null) return false;

        Entity collection = applicationBean.getSelectedCollection();
        if (collection != null && collection.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, collection.getId(),
                PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())) {
            return true;
        }
        Entity reference = applicationBean.getSelectedReference();
        if (reference != null && reference.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, reference.getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        Entity group = applicationBean.getSelectedGroup();
        return group != null && group.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, group.getId(),
                PermissionRoleEnum.VALIDEUR.getLabel());
    }
}
