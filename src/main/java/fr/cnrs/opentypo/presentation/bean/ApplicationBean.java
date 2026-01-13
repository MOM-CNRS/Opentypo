package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.ReferenceService;
import fr.cnrs.opentypo.application.service.SerieService;
import fr.cnrs.opentypo.application.service.TypeService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.common.models.Language;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysique;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.presentation.bean.util.PanelStateManager;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
@SessionScoped
@Named("applicationBean")
public class ApplicationBean implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Inject
    private transient Provider<SearchBean> searchBeanProvider;

    @Inject
    private transient LangueRepository langueRepository;

    @Inject
    private transient EntityRepository entityRepository;

    @Inject
    private transient EntityTypeRepository entityTypeRepository;

    @Inject
    private transient EntityRelationRepository entityRelationRepository;

    @Inject
    private transient Provider<TreeBean> treeBeanProvider;

    @Inject
    private transient ReferenceService referenceService;

    @Inject
    private transient CategoryService categoryService;

    @Inject
    private transient GroupService groupService;

    @Inject
    private transient SerieService serieService;

    @Inject
    private transient TypeService typeService;

    @Inject
    private transient SearchBean searchBean;

    private final PanelStateManager panelState = new PanelStateManager();

    private List<Language> languages;

    private List<Entity> beadCrumbElements;
    
    private List<Entity> references;
    private List<Entity> collections;
    
    // Collection actuellement sélectionnée
    private Entity selectedCollection;
    
    // Référentiel actuellement sélectionné
    private Entity selectedReference;
    
    // Catégorie actuellement sélectionnée
    private Entity selectedCategory;
    
    // Groupe actuellement sélectionné
    private Entity selectedGroup;
    
    // Série actuellement sélectionnée
    private Entity selectedSerie;
    
    // Type actuellement sélectionné
    private Entity selectedType;
    
    // Référentiels de la collection sélectionnée
    private List<Entity> collectionReferences;
    
    // Catégories du référentiel sélectionné
    private List<Entity> referenceCategories;
    
    // Groupes de la catégorie sélectionnée
    private List<Entity> categoryGroups;
    
    // Séries du groupe sélectionné
    private List<Entity> groupSeries;
    
    // Types du groupe sélectionné
    private List<Entity> groupTypes;
    
    // Types de la série sélectionnée
    private List<Entity> serieTypes;

    // Titre de l'écran
    private String selectedEntityLabel;


    // Getters pour compatibilité avec XHTML
    public boolean isShowBreadCrumbPanel() { return panelState.isShowBreadCrumb(); }
    public boolean isShowCollectionsPanel() { return panelState.isShowCollections(); }
    public boolean isShowReferencesPanel() { return panelState.isShowReferencesPanel(); }
    public boolean isShowReferencePanel() { return panelState.isShowReferencePanel(); }
    public boolean isShowCategoryPanel() { return panelState.isShowCategoryPanel(); }
    public boolean isShowGroupePanel() { return panelState.isShowGroupePanel(); }
    public boolean isShowSeriePanel() { return panelState.isShowSeriePanel(); }
    public boolean isShowTypePanel() { return panelState.isShowTypePanel(); }
    public boolean isShowTreePanel() { return panelState.isShowTreePanel(); }

    @PostConstruct
    public void initialization() {
        this.selectedEntityLabel = "";
        checkSessionExpiration();
        loadLanguages();
        loadCollections();
    }

    /**
     * Vérifie si la session ou la vue a expiré
     */
    private void checkSessionExpiration() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String sessionExpired = facesContext.getExternalContext()
                .getRequestParameterMap().get(ViewConstants.PARAM_SESSION_EXPIRED);
            String viewExpired = facesContext.getExternalContext()
                .getRequestParameterMap().get(ViewConstants.PARAM_VIEW_EXPIRED);
            
            if (ViewConstants.PARAM_TRUE.equals(sessionExpired) 
                || ViewConstants.PARAM_TRUE.equals(viewExpired)) {
                panelState.showCollections();
            }
        } else {
            panelState.showCollections();
        }
    }

    /**
     * Charge les langues depuis la base de données
     */
    private void loadLanguages() {
        languages = new ArrayList<>();
        try {
            List<Langue> languesFromDb = langueRepository.findAllByOrderByNomAsc();
            int id = 1;
            for (Langue langue : languesFromDb) {
                languages.add(new Language(
                    id++,
                    langue.getCode(),
                    langue.getNom(),
                    langue.getCode()
                ));
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des langues depuis la base de données", e);
            // Valeurs par défaut en cas d'erreur
            languages.add(new Language(1, "fr", "Français", "fr"));
            languages.add(new Language(2, "en", "Anglais", "en"));
        }
    }
    
    /**
     * Charge les référentiels depuis la base de données
     */
    public void loadreferences() {
        references = new ArrayList<>();
        try {
            references = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_REFERENCE);
            references = references.stream()
                .filter(r -> r.getPublique() != null && r.getPublique())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des référentiels depuis la base de données", e);
            references = new ArrayList<>();
        }
    }

    /**
     * Charge les collections depuis la base de données
     */
    public void loadCollections() {
        collections = new ArrayList<>();
        try {
            collections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION);
            collections = collections.stream()
                .filter(c -> c.getPublique() != null && c.getPublique())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des collections depuis la base de données", e);
            collections = new ArrayList<>();
        }
    }

    public void showSelectedPanel(Entity entity) {
        switch (entity.getEntityType().getCode()) {
            case EntityConstants.ENTITY_TYPE_COLLECTION:
                showCollectionDetail(entity);
                break;
            case EntityConstants.ENTITY_TYPE_REFERENCE:
                showReferenceDetail(entity);
                break;
        }
    }

    public boolean isShowDetail() {
        return panelState.isShowDetail();
    }

    public void showCollections() {
        this.selectedEntityLabel = "";
        this.selectedCollection = null; // Réinitialiser la collection sélectionnée
        this.beadCrumbElements = new ArrayList<>(); // Réinitialiser le breadcrumb
        searchBean.setCollectionSelected(null);
        panelState.showCollections();
    }
    
    public void showCollectionDetail() {
        panelState.showCollectionDetail();
    }

    public void showCategory() {
        panelState.showCategory();
    }

    public void showGroupe() {
        panelState.showGroupe();
    }
    
    /**
     * Affiche les détails d'un groupe spécifique
     */
    public void showGroupe(Entity group) {
        // Recharger le groupe depuis la base de données pour avoir toutes les données complètes
        if (group != null && group.getId() != null) {
            try {
                this.selectedGroup = entityRepository.findById(group.getId())
                    .orElse(group); // Fallback sur le groupe passé en paramètre si non trouvé
            } catch (Exception e) {
                log.error("Erreur lors du rechargement du groupe depuis la base de données", e);
                this.selectedGroup = group; // Fallback sur le groupe passé en paramètre
            }
        } else {
            this.selectedGroup = group;
        }
        
        panelState.showGroupe();
        
        // Charger les séries du groupe depuis la table entity_relation
        // Les séries sont des entités de type "SERIE" rattachées au groupe via entity_relation
        groupSeries = serieService.loadGroupSeries(selectedGroup);
        
        // Charger les types du groupe depuis la table entity_relation
        // Les types sont des entités de type "TYPE" rattachées au groupe via entity_relation
        groupTypes = typeService.loadGroupTypes(selectedGroup);
        
        // Trouver la catégorie parente de ce groupe pour le breadcrumb
        if (selectedCategory == null && selectedGroup != null) {
            try {
                List<Entity> parents = entityRelationRepository.findParentsByChild(selectedGroup);
                if (parents != null && !parents.isEmpty()) {
                    // Trouver la catégorie parente
                    for (Entity parent : parents) {
                        if (parent.getEntityType() != null &&
                            (EntityConstants.ENTITY_TYPE_CATEGORY.equals(parent.getEntityType().getCode()) ||
                             "CATEGORIE".equals(parent.getEntityType().getCode()))) {
                            this.selectedCategory = parent;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la recherche de la catégorie parente du groupe", e);
            }
        }
        
        // Si on a la catégorie, trouver le référentiel parent
        if (selectedCategory != null && selectedReference == null) {
            try {
                List<Entity> parents = entityRelationRepository.findParentsByChild(selectedCategory);
                if (parents != null && !parents.isEmpty()) {
                    // Trouver le référentiel parent
                    for (Entity parent : parents) {
                        if (parent.getEntityType() != null &&
                            (EntityConstants.ENTITY_TYPE_REFERENCE.equals(parent.getEntityType().getCode()) ||
                             "REFERENTIEL".equals(parent.getEntityType().getCode()))) {
                            this.selectedReference = parent;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la recherche du référentiel parent de la catégorie", e);
            }
        }
        
        // Si on a le référentiel, trouver la collection parente
        if (selectedReference != null && selectedCollection == null) {
            try {
                List<Entity> parents = entityRelationRepository.findParentsByChild(selectedReference);
                if (parents != null && !parents.isEmpty()) {
                    // Trouver la collection parente
                    for (Entity parent : parents) {
                        if (parent.getEntityType() != null &&
                            EntityConstants.ENTITY_TYPE_COLLECTION.equals(parent.getEntityType().getCode())) {
                            this.selectedCollection = parent;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la recherche de la collection parente du référentiel", e);
            }
        }
        
        // Mettre à jour le breadcrumb
        beadCrumbElements = new ArrayList<>();
        if (selectedCollection != null) {
            beadCrumbElements.add(selectedCollection);
        }
        if (selectedReference != null) {
            beadCrumbElements.add(selectedReference);
        }
        if (selectedCategory != null) {
            beadCrumbElements.add(selectedCategory);
        }
        if (selectedGroup != null) {
            beadCrumbElements.add(selectedGroup);
        }
    }

    public void showSerie() {
        panelState.showSerie();
    }
    
    /**
     * Affiche les détails d'une série spécifique
     */
    public void showSerie(Entity serie) {
        // Recharger la série depuis la base de données pour avoir toutes les données complètes
        if (serie != null && serie.getId() != null) {
            try {
                this.selectedSerie = entityRepository.findById(serie.getId())
                    .orElse(serie); // Fallback sur la série passée en paramètre si non trouvée
                
                if (this.selectedSerie != null) {
                    // Force loading of lazy relations if needed
                    if (this.selectedSerie.getProduction() != null) {
                        this.selectedSerie.getProduction().getValeur();
                    }
                    if (this.selectedSerie.getAireCirculation() != null) {
                        this.selectedSerie.getAireCirculation().getValeur();
                    }
                    if (this.selectedSerie.getCategorieFonctionnelle() != null) {
                        this.selectedSerie.getCategorieFonctionnelle().getValeur();
                    }
                    if (this.selectedSerie.getCaracteristiquePhysique() != null) {
                        CaracteristiquePhysique cp = this.selectedSerie.getCaracteristiquePhysique();
                        if (cp.getForme() != null) {
                            cp.getForme().getValeur();
                        }
                        if (cp.getDimensions() != null) {
                            cp.getDimensions().getValeur();
                        }
                        if (cp.getTechnique() != null) {
                            cp.getTechnique().getValeur();
                        }
                        if (cp.getFabrication() != null) {
                            cp.getFabrication().getValeur();
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors du rechargement de la série depuis la base de données", e);
                this.selectedSerie = serie; // Fallback sur la série passée en paramètre
            }
        } else {
            this.selectedSerie = serie;
        }
        
        panelState.showSerie();
        
        // Charger les types de la série depuis la table entity_relation
        // Les types sont des entités de type "TYPE" rattachées à la série via entity_relation
        serieTypes = typeService.loadSerieTypes(selectedSerie);
        
        // Trouver le groupe parent de cette série pour le breadcrumb
        if (selectedGroup == null && selectedSerie != null) {
            try {
                List<Entity> parents = entityRelationRepository.findParentsByChild(selectedSerie);
                if (parents != null && !parents.isEmpty()) {
                    // Trouver le parent qui est un groupe
                    for (Entity parent : parents) {
                        if (parent != null && parent.getEntityType() != null &&
                            (EntityConstants.ENTITY_TYPE_GROUP.equals(parent.getEntityType().getCode()))) {
                            selectedGroup = parent;
                            // Trouver la catégorie parente du groupe
                            if (selectedCategory == null) {
                                List<Entity> groupParents = entityRelationRepository.findParentsByChild(selectedGroup);
                                if (groupParents != null && !groupParents.isEmpty()) {
                                    for (Entity groupParent : groupParents) {
                                        if (groupParent != null && groupParent.getEntityType() != null &&
                                            (EntityConstants.ENTITY_TYPE_CATEGORY.equals(groupParent.getEntityType().getCode()))) {
                                            selectedCategory = groupParent;
                                            // Trouver la référence parente de la catégorie
                                            if (selectedReference == null) {
                                                List<Entity> categoryParents = entityRelationRepository.findParentsByChild(selectedCategory);
                                                if (categoryParents != null && !categoryParents.isEmpty()) {
                                                    for (Entity categoryParent : categoryParents) {
                                                        if (categoryParent != null && categoryParent.getEntityType() != null &&
                                                            (EntityConstants.ENTITY_TYPE_REFERENCE.equals(categoryParent.getEntityType().getCode()))) {
                                                            selectedReference = categoryParent;
                                                            // Trouver la collection parente de la référence
                                                            if (selectedCollection == null) {
                                                                List<Entity> referenceParents = entityRelationRepository.findParentsByChild(selectedReference);
                                                                if (referenceParents != null && !referenceParents.isEmpty()) {
                                                                    for (Entity referenceParent : referenceParents) {
                                                                        if (referenceParent != null && referenceParent.getEntityType() != null &&
                                                                            (EntityConstants.ENTITY_TYPE_COLLECTION.equals(referenceParent.getEntityType().getCode()))) {
                                                                            selectedCollection = referenceParent;
                                                                            break;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la recherche du groupe parent de la série", e);
            }
        }
        
        // Construire le breadcrumb : série -> groupe -> catégorie -> référence -> collection
        beadCrumbElements = new ArrayList<>();
        if (selectedCollection != null) {
            beadCrumbElements.add(selectedCollection);
        }
        if (selectedReference != null) {
            beadCrumbElements.add(selectedReference);
        }
        if (selectedCategory != null) {
            beadCrumbElements.add(selectedCategory);
        }
        if (selectedGroup != null) {
            beadCrumbElements.add(selectedGroup);
        }
        if (selectedSerie != null) {
            beadCrumbElements.add(selectedSerie);
        }
    }

    public void showType() {
        panelState.showType();
    }
    
    /**
     * Affiche les détails d'un type spécifique
     */
    public void showType(Entity type) {
        // Recharger le type depuis la base de données pour avoir toutes les données complètes
        if (type != null && type.getId() != null) {
            try {
                this.selectedType = entityRepository.findById(type.getId())
                    .orElse(type); // Fallback sur le type passé en paramètre si non trouvé
                
                // Initialiser les relations lazy si nécessaire
                if (this.selectedType != null) {
                    // Accéder aux relations pour forcer leur chargement (si elles existent)
                    if (this.selectedType.getProduction() != null) {
                        this.selectedType.getProduction().getValeur(); // Force le chargement
                    }
                    if (this.selectedType.getAireCirculation() != null) {
                        this.selectedType.getAireCirculation().getValeur(); // Force le chargement
                    }
                    if (this.selectedType.getCategorieFonctionnelle() != null) {
                        this.selectedType.getCategorieFonctionnelle().getValeur(); // Force le chargement
                    }
                    if (this.selectedType.getCaracteristiquePhysique() != null) {
                        CaracteristiquePhysique cp = this.selectedType.getCaracteristiquePhysique();
                        if (cp.getForme() != null) {
                            cp.getForme().getValeur(); // Force le chargement
                        }
                        if (cp.getDimensions() != null) {
                            cp.getDimensions().getValeur(); // Force le chargement
                        }
                        if (cp.getTechnique() != null) {
                            cp.getTechnique().getValeur(); // Force le chargement
                        }
                        if (cp.getFabrication() != null) {
                            cp.getFabrication().getValeur(); // Force le chargement
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors du rechargement du type depuis la base de données", e);
                this.selectedType = type; // Fallback sur le type passé en paramètre
            }
        } else {
            this.selectedType = type;
        }
        
        panelState.showType();
        
        // Trouver le groupe parent de ce type pour le breadcrumb
        if (selectedGroup == null && selectedType != null) {
            try {
                List<Entity> parents = entityRelationRepository.findParentsByChild(selectedType);
                if (parents != null && !parents.isEmpty()) {
                    // Chercher d'abord un groupe parent
                    for (Entity parent : parents) {
                        if (parent.getEntityType() != null &&
                            (EntityConstants.ENTITY_TYPE_GROUP.equals(parent.getEntityType().getCode()) ||
                             "GROUPE".equals(parent.getEntityType().getCode()))) {
                            this.selectedGroup = parent;
                            break;
                        }
                    }
                    // Si pas de groupe trouvé, chercher une série parente
                    if (this.selectedGroup == null) {
                        for (Entity parent : parents) {
                            if (parent.getEntityType() != null &&
                                (EntityConstants.ENTITY_TYPE_SERIES.equals(parent.getEntityType().getCode()) ||
                                 "SERIE".equals(parent.getEntityType().getCode()))) {
                                this.selectedSerie = parent;
                                // Chercher le groupe parent de la série
                                List<Entity> serieParents = entityRelationRepository.findParentsByChild(this.selectedSerie);
                                if (serieParents != null && !serieParents.isEmpty()) {
                                    for (Entity serieParent : serieParents) {
                                        if (serieParent.getEntityType() != null &&
                                            (EntityConstants.ENTITY_TYPE_GROUP.equals(serieParent.getEntityType().getCode()) ||
                                             "GROUPE".equals(serieParent.getEntityType().getCode()))) {
                                            this.selectedGroup = serieParent;
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la recherche du groupe parent du type", e);
            }
        }
        
        // Trouver la catégorie parente si nécessaire
        if (selectedCategory == null && selectedGroup != null) {
            try {
                List<Entity> parents = entityRelationRepository.findParentsByChild(selectedGroup);
                if (parents != null && !parents.isEmpty()) {
                    for (Entity parent : parents) {
                        if (parent.getEntityType() != null &&
                            (EntityConstants.ENTITY_TYPE_CATEGORY.equals(parent.getEntityType().getCode()) ||
                             "CATEGORIE".equals(parent.getEntityType().getCode()))) {
                            this.selectedCategory = parent;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la recherche de la catégorie parente du groupe", e);
            }
        }
        
        // Trouver le référentiel parent si nécessaire
        if (selectedReference == null && selectedCategory != null) {
            try {
                List<Entity> parents = entityRelationRepository.findParentsByChild(selectedCategory);
                if (parents != null && !parents.isEmpty()) {
                    for (Entity parent : parents) {
                        if (parent.getEntityType() != null &&
                            (EntityConstants.ENTITY_TYPE_REFERENCE.equals(parent.getEntityType().getCode()) ||
                             "REFERENTIEL".equals(parent.getEntityType().getCode()))) {
                            this.selectedReference = parent;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la recherche du référentiel parent de la catégorie", e);
            }
        }
        
        // Trouver la collection parente si nécessaire
        if (selectedCollection == null && selectedReference != null) {
            try {
                List<Entity> parents = entityRelationRepository.findParentsByChild(selectedReference);
                if (parents != null && !parents.isEmpty()) {
                    for (Entity parent : parents) {
                        if (parent.getEntityType() != null &&
                            EntityConstants.ENTITY_TYPE_COLLECTION.equals(parent.getEntityType().getCode())) {
                            this.selectedCollection = parent;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la recherche de la collection parente du référentiel", e);
            }
        }
        
        // Mettre à jour le breadcrumb : collection -> référence -> catégorie -> groupe -> série -> type
        beadCrumbElements = new ArrayList<>();
        if (selectedCollection != null) {
            beadCrumbElements.add(selectedCollection);
        }
        if (selectedReference != null) {
            beadCrumbElements.add(selectedReference);
        }
        if (selectedCategory != null) {
            beadCrumbElements.add(selectedCategory);
        }
        if (selectedGroup != null) {
            beadCrumbElements.add(selectedGroup);
        }
        if (selectedSerie != null) {
            beadCrumbElements.add(selectedSerie);
        }
        if (selectedType != null) {
            beadCrumbElements.add(selectedType);
        }
    }

    /**
     * Affiche les détails d'une collection spécifique
     */
    public void showCollectionDetail(Entity collection) {
        this.selectedCollection = collection;
        this.selectedEntityLabel = selectedCollection.getNom();

        refreshCollectionReferencesList();

        panelState.showCollectionDetail();
        SearchBean searchBean = searchBeanProvider.get();
        if (searchBean != null) {
            searchBean.setCollectionSelected(collection.getCode());
        }

        // Initialiser le breadcrumb avec la collection
        beadCrumbElements = new ArrayList<>();
        beadCrumbElements.add(collection);

        // Initialiser l'arbre avec les référentiels de la collection
        TreeBean treeBean = treeBeanProvider.get();
        if (treeBean != null) {
            treeBean.initializeTreeWithCollection();
        }
    }

    /**
     * Affiche les détails d'un référentiel spécifique
     */
    public void showReferenceDetail(Entity reference) {
        this.selectedReference = reference;
        panelState.showReference();

        beadCrumbElements = new ArrayList<>();
        beadCrumbElements.add(selectedCollection);
        beadCrumbElements.add(reference);

        // Charger les catégories du référentiel
        refreshReferenceCategoriesList();

        // Sélectionner le nœud correspondant dans l'arbre et charger les catégories
        TreeBean treeBean = treeBeanProvider.get();
        if (treeBean != null) {
            treeBean.selectReferenceNode(reference);
            // Charger les catégories dans l'arbre
            treeBean.loadCategoriesForReference(reference);
        }
    }

    /**
     * Affiche les détails d'une catégorie spécifique
     */
    public void showCategoryDetail(Entity category) {
        this.selectedCategory = category;
        panelState.showCategory();

        // Charger les groupes de la catégorie depuis la base de données
        categoryGroups = groupService.loadCategoryGroups(selectedCategory);

        // Trouver le référentiel parent de cette catégorie pour le breadcrumb
        if (selectedReference == null) {
            try {
                List<Entity> parents = entityRelationRepository.findParentsByChild(category);
                if (parents != null && !parents.isEmpty()) {
                    // Trouver le référentiel parent
                    for (Entity parent : parents) {
                        if (parent.getEntityType() != null &&
                                (EntityConstants.ENTITY_TYPE_REFERENCE.equals(parent.getEntityType().getCode()) ||
                                        "REFERENTIEL".equals(parent.getEntityType().getCode()))) {
                            this.selectedReference = parent;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la recherche du référentiel parent de la catégorie", e);
            }
        }

        // Mettre à jour le breadcrumb
        beadCrumbElements = new ArrayList<>();
        if (selectedCollection != null) {
            beadCrumbElements.add(selectedCollection);
        }
        if (selectedReference != null) {
            beadCrumbElements.add(selectedReference);
        }
        beadCrumbElements.add(category);
    }

    public void refreshCollectionReferencesList() {
        this.collectionReferences = referenceService.loadReferencesByCollection(selectedCollection);
    }

    public void refreshReferenceCategoriesList() {
        referenceCategories = categoryService.loadCategoriesByReference(selectedReference);
    }

    public void refreshCategoryGroupsList() {
        if (selectedCategory != null) {
            categoryGroups = groupService.loadCategoryGroups(selectedCategory);
        }
    }

    /**
     * Recharge la liste des séries du groupe sélectionné depuis la table entity_relation
     */
    public void refreshGroupSeriesList() {
        if (selectedGroup != null) {
            // Recharger depuis entity_relation les séries (entités de type SERIE) rattachées au groupe
            groupSeries = serieService.loadGroupSeries(selectedGroup);
        }
    }

    /**
     * Recharge la liste des types du groupe sélectionné depuis la table entity_relation
     */
    public void refreshGroupTypesList() {
        if (selectedGroup != null) {
            // Recharger depuis entity_relation les types (entités de type TYPE) rattachées au groupe
            groupTypes = typeService.loadGroupTypes(selectedGroup);
        }
    }
}

