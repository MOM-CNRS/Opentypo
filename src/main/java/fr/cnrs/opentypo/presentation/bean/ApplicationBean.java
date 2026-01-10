package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.common.models.Language;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.presentation.bean.util.PanelStateManager;
import jakarta.annotation.PostConstruct;
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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
@SessionScoped
@Named("applicationBean")
public class ApplicationBean implements Serializable {

    @Inject
    private Provider<SearchBean> searchBeanProvider;

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private Provider<TreeBean> treeBeanProvider;

    @Inject
    private SearchBean searchBean;

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
    
    // Référentiels de la collection sélectionnée
    private List<Entity> collectionReferences;
    
    // Catégories du référentiel sélectionné
    private List<Entity> referenceCategories;

    // Titre de l'écran
    private String selectedEntityLabel;
    
    // Propriétés pour le formulaire de création de catégorie
    private String categoryCode;
    private String categoryLabel;
    private String categoryDescription;


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
    
    /**
     * Affiche les détails d'une collection spécifique
     */
    public void showCollectionDetail(Entity collection) {
        this.selectedCollection = collection;
        this.selectedEntityLabel = selectedCollection.getNom();
        loadCollectionReferences();
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
     * Charge les référentiels rattachés à la collection sélectionnée
     */
    public void loadCollectionReferences() {
        collectionReferences = new ArrayList<>();
        if (selectedCollection != null) {
            try {
                collectionReferences = entityRelationRepository.findChildrenByParentAndType(
                    selectedCollection, 
                    EntityConstants.ENTITY_TYPE_REFERENCE
                );
            } catch (Exception e) {
                log.error("Erreur lors du chargement des référentiels de la collection", e);
                collectionReferences = new ArrayList<>();
            }
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
        loadReferenceCategories();
        
        // Sélectionner le nœud correspondant dans l'arbre et charger les catégories
        TreeBean treeBean = treeBeanProvider.get();
        if (treeBean != null) {
            treeBean.selectReferenceNode(reference);
            // Charger les catégories dans l'arbre
            treeBean.loadCategoriesForReference(reference);
        }
    }
    
    /**
     * Charge les catégories rattachées au référentiel sélectionné
     */
    public void loadReferenceCategories() {
        referenceCategories = new ArrayList<>();
        if (selectedReference != null) {
            try {
                // Essayer d'abord avec "CATEGORY" puis "CATEGORIE" pour compatibilité
                referenceCategories = entityRelationRepository.findChildrenByParentAndType(
                    selectedReference, 
                    EntityConstants.ENTITY_TYPE_CATEGORY
                );
                
                // Si aucune catégorie trouvée avec "CATEGORY", essayer avec "CATEGORIE"
                if (referenceCategories.isEmpty()) {
                    referenceCategories = entityRelationRepository.findChildrenByParentAndType(
                        selectedReference, 
                        "CATEGORIE"
                    );
                }
            } catch (Exception e) {
                log.error("Erreur lors du chargement des catégories du référentiel", e);
                referenceCategories = new ArrayList<>();
            }
        }
    }

    public void showCategory() {
        panelState.showCategory();
    }
    
    /**
     * Affiche les détails d'une catégorie spécifique
     */
    public void showCategoryDetail(Entity category) {
        this.selectedCategory = category;
        panelState.showCategory();
        
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

    public void showGroupe() {
        panelState.showGroupe();
    }

    public void showSerie() {
        panelState.showSerie();
    }

    public void showType() {
        panelState.showType();
    }
    
    public void resetCategoryForm() {
        categoryCode = null;
        categoryLabel = null;
        categoryDescription = null;
    }
    
    public void createCategory() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        // Validation des champs obligatoires
        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateCode(
                categoryCode, entityRepository, ":categoryForm")) {
            return;
        }
        
        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateLabel(
                categoryLabel, ":categoryForm")) {
            return;
        }
        
        // Vérifier qu'un référentiel est sélectionné
        if (selectedReference == null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucun référentiel n'est sélectionné. Veuillez sélectionner un référentiel avant de créer une catégorie."));
            PrimeFaces.current().ajax().update(":growl, :categoryForm");
            return;
        }
        
        String codeTrimmed = categoryCode.trim();
        String labelTrimmed = categoryLabel.trim();
        String descriptionTrimmed = (categoryDescription != null && !categoryDescription.trim().isEmpty()) 
            ? categoryDescription.trim() : null;
        
        try {
            // Récupérer le type d'entité CATEGORY
            // Essayer d'abord avec "CATEGORY" puis "CATEGORIE" pour compatibilité
            EntityType categoryType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_CATEGORY)
                .orElse(entityTypeRepository.findByCode("CATEGORIE")
                    .orElseThrow(() -> new IllegalStateException(
                        "Le type d'entité 'CATEGORY' ou 'CATEGORIE' n'existe pas dans la base de données.")));
            
            // Créer la nouvelle entité catégorie
            Entity newCategory = new Entity();
            newCategory.setCode(codeTrimmed);
            newCategory.setNom(labelTrimmed);
            newCategory.setCommentaire(descriptionTrimmed);
            newCategory.setEntityType(categoryType);
            newCategory.setPublique(true);
            newCategory.setCreateDate(LocalDateTime.now());
            
            // Sauvegarder la catégorie
            Entity savedCategory = entityRepository.save(newCategory);
            
            // Créer la relation entre le référentiel (parent) et la catégorie (child)
            if (!entityRelationRepository.existsByParentAndChild(selectedReference.getId(), savedCategory.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(selectedReference);
                relation.setChild(savedCategory);
                entityRelationRepository.save(relation);
            }
            
            // Recharger la liste des catégories
            loadReferenceCategories();
            
            // Ajouter la catégorie à l'arbre
            TreeBean treeBean = treeBeanProvider.get();
            if (treeBean != null) {
                treeBean.addCategoryToTree(savedCategory, selectedReference);
            }
            
            // Message de succès
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "La catégorie '" + labelTrimmed + "' a été créée avec succès."));
            
            resetCategoryForm();
            
            // Mettre à jour les composants : growl, formulaire, arbre, et conteneur des catégories
            PrimeFaces.current().ajax().update(":growl, :categoryForm, :treeWidget, :categoriesContainer");
            
        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création de la catégorie", e);
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :categoryForm");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la catégorie", e);
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors de la création de la catégorie : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :categoryForm");
        }
    }
}

