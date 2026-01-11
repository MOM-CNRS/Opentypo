package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.ReferenceService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.common.models.Language;
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
    
    // Référentiels de la collection sélectionnée
    private List<Entity> collectionReferences;
    
    // Catégories du référentiel sélectionné
    private List<Entity> referenceCategories;
    
    // Groupes de la catégorie sélectionnée
    private List<Entity> categoryGroups;

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

    public void showType() {
        panelState.showType();
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
}

