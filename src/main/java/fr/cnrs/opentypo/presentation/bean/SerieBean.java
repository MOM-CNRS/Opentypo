package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.DescriptionItem;
import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.NameItem;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.application.service.EntityAuthorityService;
import fr.cnrs.opentypo.application.service.EntityCodeUniquenessService;
import fr.cnrs.opentypo.presentation.bean.candidats.Candidat;
import fr.cnrs.opentypo.presentation.bean.candidats.CandidatBean;
import fr.cnrs.opentypo.presentation.bean.candidats.converter.CandidatConverter;
import fr.cnrs.opentypo.domain.entity.Description;
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
import fr.cnrs.opentypo.presentation.bean.util.EntityValidator;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
@SessionScoped
@Named(value = "serieBean")
@Slf4j
public class SerieBean implements Serializable {

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private LoginBean loginBean;

    @Inject
    private Provider<TreeBean> treeBeanProvider;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private SearchBean searchBean;

    @Inject
    private ApplicationBean applicationBean;

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private CandidatBean candidatBean;

    @Inject
    private EntityEditModeBean entityEditModeBean;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private EntityAuthorityService entityAuthorityService;

    @Autowired
    private EntityCodeUniquenessService entityCodeUniquenessService;

    private String serieCode;
    private String serieLabel;
    private String serieDescription;

    private List<NameItem> serieNames = new ArrayList<>();
    private List<DescriptionItem> serieDescriptions = new ArrayList<>();
    private String serieDialogCode;
    private String newNameValue;
    private String newNameLangueCode;
    private String newDescriptionValue;
    private String newDescriptionLangueCode;
    private List<Langue> availableLanguages;


    public void resetSerieForm() {
        serieCode = null;
        serieLabel = null;
        serieDescription = null;
    }

    public void resetSerieDialogForm() {
        serieDialogCode = null;
        serieNames = new ArrayList<>();
        serieDescriptions = new ArrayList<>();
        newNameValue = null;
        newNameLangueCode = searchBean.getLangSelected();
        newDescriptionValue = null;
        newDescriptionLangueCode = searchBean.getLangSelected();
    }

    /** Active le mode édition in-place pour la série sélectionnée (comme GroupBean.startEditingGroupe). */
    public void startEditingSerie(ApplicationBean appBean) {
        if (appBean == null || appBean.getSelectedEntity() == null) return;
        Entity entity = appBean.getSelectedEntity();
        if (entity.getEntityType() == null || !EntityConstants.ENTITY_TYPE_SERIES.equals(entity.getEntityType().getCode())) return;
        Candidat candidat = new CandidatConverter().convertEntityToCandidat(entity);
        candidatBean.visualiserCandidat(candidat);
        entityEditModeBean.startEditing();
    }

    /** Annule le mode édition et rafraîchit l'entité. */
    public void cancelEditingSerie(ApplicationBean appBean) {
        entityEditModeBean.cancelEditing();
        if (appBean != null && appBean.getSelectedEntity() != null && appBean.getSelectedEntity().getId() != null) {
            appBean.setSelectedEntity(entityRepository.findById(appBean.getSelectedEntity().getId()).orElse(appBean.getSelectedEntity()));
        }
    }

    /** Enregistre les modifications et sort du mode édition si succès. */
    public void saveEditingSerie(ApplicationBean appBean) {
        if (candidatBean.performEnregistrerModifications()) {
            cancelEditingSerie(appBean);
        }
    }

    public void prepareCreateSerie() {
        resetSerieDialogForm();
    }

    private void loadAvailableLanguages() {
        if (availableLanguages == null) {
            try {
                availableLanguages = langueRepository.findAllByOrderByNomAsc();
            } catch (Exception e) {
                log.error("Erreur lors du chargement des langues", e);
                availableLanguages = new ArrayList<>();
            }
        }
    }

    public boolean isLangueAlreadyUsedInSerieNames(String code, NameItem exclude) {
        if (serieNames == null || code == null) return false;
        return serieNames.stream()
                .filter(i -> i != exclude && i.getLangueCode() != null)
                .anyMatch(i -> i.getLangueCode().equals(code));
    }

    public List<Langue> getAvailableLanguagesForNewSerieName() {
        loadAvailableLanguages();
        if (availableLanguages == null) return new ArrayList<>();
        return availableLanguages.stream()
                .filter(l -> !isLangueAlreadyUsedInSerieNames(l.getCode(), null))
                .collect(java.util.stream.Collectors.toList());
    }

    public void addSerieNameFromInput() {
        if (newNameValue == null || newNameValue.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.fieldNameRequired")));
            return;
        }
        if (newNameLangueCode == null || newNameLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.languageRequired")));
            return;
        }
        if (isLangueAlreadyUsedInSerieNames(newNameLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.languageAlreadyUsedName")));
            return;
        }
        if (serieNames == null) serieNames = new ArrayList<>();
        Langue langue = langueRepository.findByCode(newNameLangueCode);
        serieNames.add(new NameItem(newNameValue.trim(), newNameLangueCode, langue));
        newNameValue = null;
        newNameLangueCode = null;
    }

    public void removeSerieName(NameItem item) {
        if (serieNames != null) serieNames.remove(item);
    }

    public boolean isLangueAlreadyUsedInSerieDescriptions(String code, DescriptionItem exclude) {
        if (serieDescriptions == null || code == null) return false;
        return serieDescriptions.stream()
                .filter(i -> i != exclude && i.getLangueCode() != null)
                .anyMatch(i -> i.getLangueCode().equals(code));
    }

    public List<Langue> getAvailableLanguagesForNewSerieDescription() {
        loadAvailableLanguages();
        if (availableLanguages == null) return new ArrayList<>();
        return availableLanguages.stream()
                .filter(l -> !isLangueAlreadyUsedInSerieDescriptions(l.getCode(), null))
                .collect(java.util.stream.Collectors.toList());
    }

    public void addSerieDescriptionFromInput() {
        if (newDescriptionValue == null || newDescriptionValue.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.descriptionRequired")));
            return;
        }
        if (newDescriptionLangueCode == null || newDescriptionLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.languageRequired")));
            return;
        }
        if (isLangueAlreadyUsedInSerieDescriptions(newDescriptionLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.languageAlreadyUsedDescription")));
            return;
        }
        if (serieDescriptions == null) serieDescriptions = new ArrayList<>();
        Langue langue = langueRepository.findByCode(newDescriptionLangueCode);
        serieDescriptions.add(new DescriptionItem(newDescriptionValue.trim(), newDescriptionLangueCode, langue));
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
    }

    public void removeSerieDescription(DescriptionItem item) {
        if (serieDescriptions != null) serieDescriptions.remove(item);
    }

    @Transactional
    public void createSerieFromDialog() {

        if (!EntityValidator.validateSerieCodeForCreate(
                serieDialogCode, applicationBean.getSelectedGroup(), entityCodeUniquenessService)) {
            return;
        }

        for (NameItem item : serieNames) {
            if (item.getNom() == null || item.getNom().trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        JsfMessages.get("common.growl.error"), JsfMessages.get("common.validation.nameValueRequired")));
                PrimeFaces.current().ajax().update(":serieDialogForm, :growl");
                return;
            }
        }

        try {
            EntityType serieType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_SERIES)
                    .orElse(entityTypeRepository.findByCode("SERIE")
                            .orElseThrow(() -> new IllegalStateException(
                                    JsfMessages.get("entity.type.missing.series")));

            Entity newSerie = new Entity();
            newSerie.setCode(serieDialogCode.trim());
            newSerie.setEntityType(serieType);
            newSerie.setCreateDate(LocalDateTime.now());

            List<Label> labels = new ArrayList<>();
            if (!CollectionUtils.isEmpty(serieNames)) {
                labels = serieNames.stream()
                        .filter(element -> element != null && element.getLangueCode() != null && StringUtils.hasText(element.getNom()))
                        .map(element -> {
                            Label label = null;
                            Langue l = langueRepository.findByCode(element.getLangueCode());
                            if (l != null) {
                                label = new Label();
                                label.setNom(element.getNom().trim());
                                label.setLangue(l);
                                label.setEntity(newSerie);
                            }
                            return label;
                        })
                        .filter(Objects::nonNull)
                        .toList();
            }
            newSerie.setLabels(labels);

            List<Description> descriptions = new ArrayList<>();
            List<DescriptionItem> descList = serieDescriptions != null ? serieDescriptions : new ArrayList<>();
            for (DescriptionItem di : descList) {
                if (di != null && di.getLangueCode() != null && StringUtils.hasText(di.getValeur())) {
                    Langue l = langueRepository.findByCode(di.getLangueCode());
                    if (l != null) {
                        Description desc = new Description();
                        desc.setValeur(di.getValeur().trim());
                        desc.setLangue(l);
                        desc.setEntity(newSerie);
                        descriptions.add(desc);
                    }
                }
            }
            newSerie.setDescriptions(descriptions);

            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                newSerie.setCreateBy(currentUser.getEmail());
                List<Utilisateur> auteurs = new ArrayList<>();
                auteurs.add(currentUser);
                newSerie.setAuteurs(auteurs);
            }

            newSerie.setStatut(EntityStatusEnum.PROPOSITION.name());

            Entity savedSerie = entityRepository.save(newSerie);

            if (!entityRelationRepository.existsByParentAndChild(
                    applicationBean.getSelectedGroup().getId(), savedSerie.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(applicationBean.getSelectedGroup());
                relation.setChild(savedSerie);
                entityRelationRepository.save(relation);
            }

            applicationBean.refreshGroupSeriesList();
            TreeBean tb = treeBeanProvider.get();
            if (tb != null) {
                tb.addEntityToTree(savedSerie, applicationBean.getSelectedGroup());
            }

            String labelPrincipal = serieNames.isEmpty() ? savedSerie.getCode() : serieNames.get(0).getNom();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, JsfMessages.get("common.growl.success"),
                    JsfMessages.format("entity.create.success.series", labelPrincipal)));

            resetSerieDialogForm();
            PrimeFaces.current().executeScript("PF('serieDialog').hide();");
            PrimeFaces.current().ajax().update(":growl :serieDialogForm :contentPanels :leftTreePanel");
        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création de la série", e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"), e.getMessage()));
            PrimeFaces.current().ajax().update(":serieDialogForm, :growl");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la série", e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"),
                    JsfMessages.format("common.error.create.series", e.getMessage()));
            PrimeFaces.current().ajax().update(":serieDialogForm, :growl");
        }
    }

    public void createSerie() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Validation des champs obligatoires
        if (!EntityValidator.validateSerieCodeForCreate(
                serieCode, applicationBean.getSelectedGroup(), entityCodeUniquenessService)) {
            return;
        }

        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateLabel(serieLabel)) {
            return;
        }
        
        // Vérifier qu'un groupe est sélectionné
        if (applicationBean == null || applicationBean.getSelectedGroup() == null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            JsfMessages.get("common.growl.error"),
                            JsfMessages.get("entity.parent.required.series")));
            PrimeFaces.current().ajax().update(":growl, :serieForm");
            return;
        }

        String codeTrimmed = serieCode.trim();
        String labelTrimmed = serieLabel.trim();
        String descriptionTrimmed = (serieDescription != null && !serieDescription.trim().isEmpty())
                ? serieDescription.trim() : null;

        try {
            // Récupérer le type d'entité SERIES
            // Essayer d'abord avec "SERIES" puis "SERIE" pour compatibilité
            EntityType serieType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_SERIES)
                    .orElse(entityTypeRepository.findByCode("SERIE")
                            .orElseThrow(() -> new IllegalStateException(
                                    JsfMessages.get("entity.type.missing.series")));

            // Créer la nouvelle entité série
            Entity newSerie = new Entity();
            newSerie.setCode(codeTrimmed);
            newSerie.setCommentaire(descriptionTrimmed);
            newSerie.setEntityType(serieType);
            newSerie.setStatut(EntityStatusEnum.PROPOSITION.name());
            newSerie.setCreateDate(LocalDateTime.now());

            CollectionBean.createCollectionLabel(labelTrimmed, newSerie, langueRepository, searchBean, loginBean);

            // Sauvegarder la série
            Entity savedSerie = entityRepository.save(newSerie);

            // Créer la relation entre le groupe (parent) et la série (child)
            if (!entityRelationRepository.existsByParentAndChild(
                    applicationBean.getSelectedGroup().getId(), savedSerie.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(applicationBean.getSelectedGroup());
                relation.setChild(savedSerie);
                entityRelationRepository.save(relation);
            }

            // Recharger la liste des séries
            applicationBean.refreshGroupSeriesList();

            // Ajouter la série à l'arbre
            TreeBean treeBean = treeBeanProvider.get();
            if (treeBean != null) {
                treeBean.addEntityToTree(savedSerie, applicationBean.getSelectedGroup());
            }

            // Message de succès
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            JsfMessages.get("common.growl.success"),
                            JsfMessages.format("entity.create.success.series", labelTrimmed)));

            resetSerieForm();

            // Mettre à jour les composants : growl, formulaire, arbre, et conteneur des séries
            PrimeFaces.current().ajax().update(":growl, :serieForm, :seriesContainer");

        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création de la série", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            JsfMessages.get("common.growl.error"),
                            e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :serieForm");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la série", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            JsfMessages.get("common.growl.error"),
                            JsfMessages.format("common.error.create.series", e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :serieForm");
        }
    }

    /**
     * Supprime la série sélectionnée et toutes ses entités enfants (types) de manière récursive.
     */
    @Transactional
    public void deleteSerie(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null || applicationBean.getSelectedEntity().getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"), JsfMessages.get("entity.none.series")));
            return;
        }
        if (!canDeleteSerie(applicationBean)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"),
                            JsfMessages.get("entity.permission.delete.series")));
            return;
        }
        try {
            Entity serie = applicationBean.getSelectedEntity();
            String serieCode = serie.getCode();
            Long serieId = serie.getId();
            Entity parentGroup = applicationBean.getSelectedGroup();

            applicationBean.deleteEntityRecursively(serie);

            applicationBean.setSelectedEntity(parentGroup);
            applicationBean.setChilds(parentGroup != null ? new ArrayList<>() : new ArrayList<>());
            if (!applicationBean.getBreadCrumbElements().isEmpty()) {
                applicationBean.getBreadCrumbElements().remove(applicationBean.getBreadCrumbElements().size() - 1);
            }
            if (parentGroup != null) {
                applicationBean.refreshChilds();
                applicationBean.getPanelState().showGroupe();
            } else {
                applicationBean.getPanelState().showCollections();
            }
            TreeBean tb = treeBeanProvider != null ? treeBeanProvider.get() : null;
            if (tb != null) {
                tb.initializeTreeWithCollection();
            }
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, JsfMessages.get("common.growl.success"),
                            JsfMessages.format("entity.delete.success.series", serieCode)));
            log.info("Série supprimée avec succès: {} (ID: {})", serieCode, serieId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la série", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"),
                            JsfMessages.format("common.error.delete", e.getMessage()));
        }
    }

    public boolean canEditSerie(ApplicationBean applicationBean) {
        if (!loginBean.isAuthenticated() || loginBean.getCurrentUser() == null
                || applicationBean.getSelectedEntity() == null) {
            return false;
        }
        return entityAuthorityService.canUpdate(loginBean.getCurrentUser(), applicationBean.getSelectedEntity());
    }

    /**
     * Indique si l'utilisateur connecté peut créer une série (bouton Créer une nouvelle série).
     * Visible si : administrateur technique, gestionnaire de la collection, gestionnaire du référentiel,
     * ou rédacteur du groupe. Utilisé quand selectedEntity est un groupe.
     */
    public boolean canCreateSerie(ApplicationBean applicationBean) {
        if (!loginBean.isAuthenticated() || loginBean.getCurrentUser() == null
                || applicationBean.getSelectedEntity() == null) {
            return false;
        }
        return entityAuthorityService.canCreate(
                loginBean.getCurrentUser(),
                EntityConstants.ENTITY_TYPE_SERIES,
                applicationBean.getSelectedEntity().getId());
    }

    public boolean canDeleteSerie(ApplicationBean applicationBean) {
        if (!loginBean.isAuthenticated() || loginBean.getCurrentUser() == null
                || applicationBean.getSelectedEntity() == null) {
            return false;
        }
        return entityAuthorityService.canDelete(loginBean.getCurrentUser(), applicationBean.getSelectedEntity());
    }

    /**
     * Indique si l'utilisateur connecté peut publier ou refuser une proposition (série).
     * Visible si : série en statut PROPOSITION, et l'un des rôles : administrateur technique,
     * gestionnaire de la collection, gestionnaire du référentiel, ou valideur du groupe contenant la série.
     */
    public boolean canPublishOrRefusePropositionSerie(ApplicationBean applicationBean) {
        if (!loginBean.isAuthenticated() || applicationBean.getSelectedEntity() == null
                || !EntityStatusEnum.PROPOSITION.name().equals(applicationBean.getSelectedEntity().getStatut())) {
            return false;
        }
        if (applicationBean.getSelectedEntity().getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_SERIES.equals(applicationBean.getSelectedEntity().getEntityType().getCode())) {
            return false;
        }
        if (loginBean.isAdminTechniqueOrFonctionnel()) {
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

        if (applicationBean.getSelectedReference() != null && applicationBean.getSelectedReference().getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, applicationBean.getSelectedReference().getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }

        return applicationBean.getSelectedGroup() != null && applicationBean.getSelectedGroup().getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, applicationBean.getSelectedGroup().getId(),
                PermissionRoleEnum.VALIDEUR.getLabel());
    }
}
