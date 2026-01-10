package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.common.models.Language;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
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
    
    // Référentiels de la collection sélectionnée
    private List<Entity> collectionReferences;

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
        
        // Sélectionner le nœud correspondant dans l'arbre
        TreeBean treeBean = treeBeanProvider.get();
        if (treeBean != null) {
            treeBean.selectReferenceNode(reference);
        }
    }

    public void showCategory() {
        panelState.showCategory();
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
        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateCode(
                categoryCode, entityRepository, ":categoryForm")) {
            return;
        }
        
        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateLabel(
                categoryLabel, ":categoryForm")) {
            return;
        }
        
        // TODO: Implémenter la logique de sauvegarde de la catégorie
        
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "La catégorie '" + categoryLabel + "' a été créée avec succès."));
        
        resetCategoryForm();
        PrimeFaces.current().ajax().update(":growl, :categoryForm, :contentPanels");
    }
}

