package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.ReferenceService;
import fr.cnrs.opentypo.application.service.SerieService;
import fr.cnrs.opentypo.application.service.TypeService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.common.models.Language;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysique;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private transient UtilisateurRepository utilisateurRepository;

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

    @Inject
    private transient LoginBean loginBean;

    @Inject
    private CollectionBean collectionBean;


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
    @Named("treeBean")
    @Inject
    private TreeBean treeBean;


    // Getters pour compatibilité avec XHTML
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
        loadPublicCollections();
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
     * Si l'utilisateur est déconnecté, affiche uniquement les collections publiques
     * Trie par ordre alphabétique décroissant
     */
    public void loadPublicCollections() {
        collections = new ArrayList<>();
        try {
            // Charger les collections avec leurs labels (une seule collection à la fois avec JOIN FETCH)
            collections = entityRepository.findByEntityTypeCodeWithLabels(EntityConstants.ENTITY_TYPE_COLLECTION);
            
            // Initialiser les descriptions pour chaque collection (évite le problème MultipleBagFetchException)
            for (Entity collection : collections) {
                if (collection.getDescriptions() != null) {
                    // Forcer l'initialisation de la collection lazy
                    collection.getDescriptions().size();
                }
            }
            
            // Filtrer selon l'authentification et trier par ordre alphabétique décroissant (Z à A)
            boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
            collections = collections.stream()
                .filter(c -> isAuthenticated || (c.getPublique() != null && c.getPublique()))
                .sorted((c1, c2) -> {
                    String nom1 = getCollectionNameForLanguage(c1);
                    String nom2 = getCollectionNameForLanguage(c2);
                    return nom1.compareToIgnoreCase(nom2);
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des collections depuis la base de données", e);
            collections = new ArrayList<>();
        }
    }
    
    /**
     * Récupère le nom d'une collection selon la langue sélectionnée dans SearchBean
     * Par défaut, utilise le français ("fr")
     * Si aucun label n'est trouvé pour la langue, retourne le nom par défaut de l'entité
     */
    public String getCollectionNameForLanguage(Entity collection) {
        if (collection == null) {
            return "";
        }
        
        final String langCode = (searchBean != null && searchBean.getLangSelected() != null && !searchBean.getLangSelected().isEmpty()) 
            ? searchBean.getLangSelected() 
            : "fr"; // Par défaut français
        
        // Chercher le label dans la langue sélectionnée
        if (collection.getLabels() != null && !collection.getLabels().isEmpty()) {
            return collection.getLabels().stream()
                .filter(label -> label.getLangue() != null && langCode.equals(label.getLangue().getCode()))
                .map(Label::getNom)
                .findFirst()
                .orElse(collection.getNom()); // Fallback sur le nom par défaut
        }
        
        return collection.getNom();
    }
    
    /**
     * Récupère la description d'une collection selon la langue sélectionnée dans SearchBean
     * Par défaut, utilise le français ("fr")
     * Si aucune description n'est trouvée pour la langue, retourne le commentaire par défaut de l'entité
     */
    public String getCollectionDescriptionForLanguage(Entity collection) {
        if (collection == null) {
            return "";
        }
        
        final String langCode = (searchBean != null && searchBean.getLangSelected() != null && !searchBean.getLangSelected().isEmpty()) 
            ? searchBean.getLangSelected() 
            : "fr"; // Par défaut français
        
        // Chercher la description dans la langue sélectionnée
        if (collection.getDescriptions() != null && !collection.getDescriptions().isEmpty()) {
            return collection.getDescriptions().stream()
                .filter(desc -> desc.getLangue() != null && langCode.equals(desc.getLangue().getCode()))
                .map(Description::getValeur)
                .findFirst()
                .orElse(collection.getCommentaire() != null ? collection.getCommentaire() : ""); // Fallback sur le commentaire par défaut
        }
        
        return collection.getCommentaire() != null ? collection.getCommentaire() : "";
    }
    
    /**
     * Méthode appelée lorsque la langue change dans la barre de recherche
     * Recharge les collections avec le nouveau tri selon la langue sélectionnée
     */
    public void onLanguageChange() {
        // Recharger les collections pour appliquer le nouveau tri selon la langue
        loadPublicCollections();
        
        // Mettre à jour le label et la description de la collection sélectionnée si elle existe
        if (selectedCollection != null) {
            collectionBean.updateCollectionLanguage();
        }
    }

    public void showSelectedPanel(Entity entity) {
        switch (entity.getEntityType().getCode()) {
            case EntityConstants.ENTITY_TYPE_COLLECTION:
                collectionBean.showCollectionDetail(entity);
                break;
            case EntityConstants.ENTITY_TYPE_REFERENCE:
                showReferenceDetail(entity);
                break;
        }
    }

    /**
     * Navigation depuis le fil d'Ariane : affiche le détail correspondant au type d'entité.
     */
    public void navigateToBreadcrumbElement(Entity entity) {
        if (entity == null || entity.getEntityType() == null) {
            return;
        }
        String code = entity.getEntityType().getCode();
        if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(code)) {
            collectionBean.showCollectionDetail(entity);
        } else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(code) || "REFERENTIEL".equals(code)) {
            showReferenceDetail(entity);
        } else if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(code) || "CATEGORIE".equals(code)) {
            showCategoryDetail(entity);
        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(code) || "GROUPE".equals(code)) {
            showGroupe(entity);
        } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(code) || "SERIE".equals(code)) {
            showSerie(entity);
        } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(code)) {
            showType(entity);
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
                                    ReferenceOpenthesoEnum.CATEGORIE.name().equals(parent.getEntityType().getCode()))) {
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

        // Déplier l'arbre jusqu'au groupe et le sélectionner
        if (treeBeanProvider != null) {
            try {
                treeBeanProvider.get().expandPathAndSelectEntity(selectedGroup);
            } catch (Exception ex) {
                log.debug("Impossible de déplier/sélectionner le nœud groupe dans l'arbre", ex);
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
        refreshReferenceCategoriesList();

        // Sélectionner le nœud correspondant dans l'arbre et charger les catégories
        TreeBean treeBean = treeBeanProvider.get();
        if (treeBean != null) {
            treeBean.selectReferenceNode(reference);
            // Charger les catégories dans l'arbre
            treeBean.loadCategoriesForReference(reference);
        }
    }

    public void refreshCollectionReferencesList() {
        this.collectionReferences = referenceService.loadReferencesByCollection(selectedCollection);
    }

    public void refreshReferenceCategoriesList() {
        referenceCategories = categoryService.loadCategoriesByReference(selectedReference);
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
                    // Forcer le chargement de la liste des aires de circulation
                    if (this.selectedSerie.getAiresCirculation() != null) {
                        this.selectedSerie.getAiresCirculation().forEach(ref -> {
                            if (ref != null && ReferenceOpenthesoEnum.AIRE_CIRCULATION.name().equals(ref.getCode())) {
                                ref.getValeur(); // Force le chargement
                            }
                        });
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

        // Déplier l'arbre jusqu'à la série et la sélectionner
        if (treeBeanProvider != null) {
            try {
                treeBeanProvider.get().expandPathAndSelectEntity(selectedSerie);
            } catch (Exception ex) {
                log.debug("Impossible de déplier/sélectionner le nœud série dans l'arbre", ex);
            }
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
                    // Forcer le chargement de la liste des aires de circulation
                    if (this.selectedType.getAiresCirculation() != null) {
                        this.selectedType.getAiresCirculation().forEach(ref -> {
                            if (ref != null && ReferenceOpenthesoEnum.AIRE_CIRCULATION.name().equals(ref.getCode())) {
                                ref.getValeur(); // Force le chargement
                            }
                        });
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
                                    ReferenceOpenthesoEnum.CATEGORIE.name().equals(parent.getEntityType().getCode()))) {
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

        // Déplier l'arbre jusqu'au type et le sélectionner
        if (treeBeanProvider != null) {
            try {
                treeBeanProvider.get().expandPathAndSelectEntity(selectedType);
            } catch (Exception ex) {
                log.debug("Impossible de déplier/sélectionner le nœud type dans l'arbre", ex);
            }
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

        // Déplier l'arbre jusqu'à la catégorie et la sélectionner (même code que dans l'arbre)
        if (treeBeanProvider != null) {
            try {
                treeBeanProvider.get().expandPathAndSelectEntity(category);
            } catch (Exception ex) {
                log.debug("Impossible de déplier/sélectionner le nœud catégorie dans l'arbre", ex);
            }
        }
    }

    public void refreshCategoryGroupsList() {
        if (selectedCategory != null) {
            categoryGroups = groupService.loadCategoryGroups(selectedCategory);
        }
    }

    /**
     * Supprime récursivement une entité et toutes ses entités enfants
     * @param entity L'entité à supprimer
     */
    public void deleteEntityRecursively(Entity entity) {
        if (entity == null || entity.getId() == null) {
            return;
        }

        try {
            // Trouver tous les enfants de cette entité
            List<Entity> children = entityRelationRepository.findChildrenByParent(entity);
            
            // Supprimer récursivement tous les enfants
            for (Entity child : children) {
                deleteEntityRecursively(child);
            }
            
            // Supprimer toutes les relations où cette entité est parent
            List<EntityRelation> parentRelations = entityRelationRepository.findByParent(entity);
            if (parentRelations != null && !parentRelations.isEmpty()) {
                entityRelationRepository.deleteAll(parentRelations);
            }
            
            // Supprimer toutes les relations où cette entité est enfant
            List<EntityRelation> childRelations = entityRelationRepository.findByChild(entity);
            if (childRelations != null && !childRelations.isEmpty()) {
                entityRelationRepository.deleteAll(childRelations);
            }
            
            // Supprimer l'entité elle-même
            entityRepository.delete(entity);
            
            log.info("Entité supprimée avec succès: {} (ID: {})", entity.getCode(), entity.getId());
        } catch (Exception e) {
            log.error("Erreur lors de la suppression récursive de l'entité: {}", entity.getCode(), e);
            throw e; // Propager l'erreur pour que la transaction soit annulée
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
    
    /**
     * Édite une collection (affiche les détails pour édition)
     */
    public void editCollection(Entity collection) {
        if (collection == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucune collection sélectionnée."));
            }
            return;
        }
        
        // Afficher les détails de la collection pour édition
        collectionBean.showCollectionDetail(collection);
        
        // Activer le mode édition
        collectionBean.startEditingCollection();
        
        log.debug("Mode édition activé pour la collection: {}", collection.getCode());
    }

    public String getEntityLabel(Entity entitySelected) {

        String codeLang = searchBean.getLangSelected();
        return entitySelected.getLabels().stream()
                .filter(label -> codeLang.equalsIgnoreCase(label.getLangue().getCode()))
                .findFirst()
                .map(Label::getNom)
                .orElse("Non renseigné");
    }

    public String getEntityDescription(Entity entitySelected) {

        String codeLang = searchBean.getLangSelected();
        return entitySelected.getDescriptions().stream()
                .filter(description -> codeLang.equalsIgnoreCase(description.getLangue().getCode()))
                .findFirst()
                .map(Description::getValeur)
                .orElse("Non renseigné");
    }
    
    /**
     * Supprime une collection et toutes ses entités rattachées
     */
    @Transactional
    public void deleteCollection(Entity collection) {
        if (collection == null || collection.getId() == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucune collection sélectionnée."));
            }
            return;
        }

        try {
            String collectionCode = collection.getCode();
            String collectionName = collection.getNom();
            Long collectionId = collection.getId();
            
            // Supprimer récursivement la collection et toutes ses entités enfants
            deleteEntityRecursively(collection);
            
            // Réinitialiser la sélection si c'était la collection sélectionnée
            if (selectedCollection != null && selectedCollection.getId().equals(collectionId)) {
                selectedCollection = null;
                selectedEntityLabel = "";
                collectionReferences = new ArrayList<>();
            }
            
            // Recharger les collections
            loadPublicCollections();
            
            // Mettre à jour l'arbre
            if (treeBeanProvider != null) {
                TreeBean treeBean = treeBeanProvider.get();
                if (treeBean != null) {
                    treeBean.initializeTreeWithCollection();
                }
            }
            
            // Afficher un message de succès
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "La collection '" + collectionName + "' et toutes ses entités rattachées ont été supprimées avec succès."));
            }
            
            // Afficher le panel des collections
            panelState.showCollections();
            
            log.info("Collection supprimée avec succès: {} (ID: {})", collectionCode, collectionId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la collection", e);
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors de la suppression : " + e.getMessage()));
            }
        }
    }
}

