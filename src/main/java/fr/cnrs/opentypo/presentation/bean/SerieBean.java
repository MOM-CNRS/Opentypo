package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.DescriptionItem;
import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.NameItem;
import fr.cnrs.opentypo.common.constant.EntityConstants;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


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
        newNameLangueCode = null;
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
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
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Le nom est requis."));
            return;
        }
        if (newNameLangueCode == null || newNameLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La langue est requise."));
            return;
        }
        if (isLangueAlreadyUsedInSerieNames(newNameLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Cette langue est déjà utilisée pour un autre nom."));
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
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La description est requise."));
            return;
        }
        if (newDescriptionLangueCode == null || newDescriptionLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La langue est requise."));
            return;
        }
        if (isLangueAlreadyUsedInSerieDescriptions(newDescriptionLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Cette langue est déjà utilisée pour une autre description."));
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
        FacesContext facesContext = FacesContext.getCurrentInstance();

        if (applicationBean == null || applicationBean.getSelectedGroup() == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Aucun groupe n'est sélectionné. Veuillez sélectionner un groupe avant de créer une série."));
            PrimeFaces.current().ajax().update(":serieDialogForm, :growl");
            return;
        }

        if (!EntityValidator.validateCode(serieDialogCode, entityRepository, ":serieDialogForm")) {
            return;
        }

        if (serieNames == null || serieNames.isEmpty()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Au moins un nom est requis."));
            PrimeFaces.current().ajax().update(":serieDialogForm, :growl");
            return;
        }

        for (NameItem item : serieNames) {
            if (item.getNom() == null || item.getNom().trim().isEmpty()) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Tous les noms doivent avoir une valeur."));
                PrimeFaces.current().ajax().update(":serieDialogForm, :growl");
                return;
            }
        }

        String codeTrimmed = serieDialogCode.trim();

        try {
            EntityType serieType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_SERIES)
                    .orElse(entityTypeRepository.findByCode("SERIE")
                            .orElseThrow(() -> new IllegalStateException(
                                    "Le type d'entité 'SERIES' ou 'SERIE' n'existe pas dans la base de données.")));

            Entity newSerie = new Entity();
            newSerie.setCode(codeTrimmed);
            newSerie.setEntityType(serieType);
            newSerie.setPublique(true);
            newSerie.setCreateDate(LocalDateTime.now());

            List<Label> labels = new ArrayList<>();
            for (NameItem ni : serieNames) {
                if (ni != null && ni.getLangueCode() != null && StringUtils.hasText(ni.getNom())) {
                    Langue l = langueRepository.findByCode(ni.getLangueCode());
                    if (l != null) {
                        Label label = new Label();
                        label.setNom(ni.getNom().trim());
                        label.setLangue(l);
                        label.setEntity(newSerie);
                        labels.add(label);
                    }
                }
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

            String labelPrincipal = serieNames.get(0).getNom();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    "La série '" + labelPrincipal + "' a été créée avec succès."));

            resetSerieDialogForm();
            PrimeFaces.current().executeScript("PF('serieDialog').hide();");
            PrimeFaces.current().ajax().update(":growl, :serieDialogForm, :seriesContent, :centerContent");
        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création de la série", e);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage()));
            PrimeFaces.current().ajax().update(":serieDialogForm, :growl");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la série", e);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Une erreur est survenue lors de la création de la série : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":serieDialogForm, :growl");
        }
    }

    public void createSerie() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Validation des champs obligatoires
        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateCode(
                serieCode, entityRepository, ":serieForm")) {
            return;
        }

        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateLabel(
                serieLabel, ":serieForm")) {
            return;
        }
        
        // Vérifier qu'un groupe est sélectionné
        if (applicationBean == null || applicationBean.getSelectedGroup() == null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Aucun groupe n'est sélectionné. Veuillez sélectionner un groupe avant de créer une série."));
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
                                    "Le type d'entité 'SERIES' ou 'SERIE' n'existe pas dans la base de données.")));

            // Créer la nouvelle entité série
            Entity newSerie = new Entity();
            newSerie.setCode(codeTrimmed);
            newSerie.setCommentaire(descriptionTrimmed);
            newSerie.setEntityType(serieType);
            newSerie.setPublique(true);
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
                            "Succès",
                            "La série '" + labelTrimmed + "' a été créée avec succès."));

            resetSerieForm();

            // Mettre à jour les composants : growl, formulaire, arbre, et conteneur des séries
            PrimeFaces.current().ajax().update(":growl, :serieForm, :seriesContainer");

        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création de la série", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :serieForm");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la série", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Une erreur est survenue lors de la création de la série : " + e.getMessage()));
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
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Aucune série sélectionnée."));
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
            if (applicationBean.getBeadCrumbElements().size() > 0) {
                applicationBean.getBeadCrumbElements().remove(applicationBean.getBeadCrumbElements().size() - 1);
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
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                            "La série '" + serieCode + "' et toutes les entités rattachées ont été supprimées."));
            log.info("Série supprimée avec succès: {} (ID: {})", serieCode, serieId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la série", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                            "Une erreur est survenue lors de la suppression : " + e.getMessage()));
        }
    }
}
