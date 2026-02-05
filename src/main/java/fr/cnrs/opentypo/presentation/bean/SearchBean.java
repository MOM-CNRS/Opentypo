package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import jakarta.faces.model.SelectItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Getter
@Setter
@SessionScoped
@Named("searchBean")
public class SearchBean implements Serializable {

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private Provider<ApplicationBean> applicationBeanProvider;

    @Inject
    private Provider<CollectionBean> collectionBeanProvider;

    @Inject
    private Provider<TreeBean> treeBeanProvider;

    private String searchSelected;
    private String collectionSelected;
    private String langSelected = "fr"; // Français sélectionné par défaut
    private String searchTerm; // Terme de recherche
    
    // Nouveaux filtres de recherche
    private String typeFilter = ""; // Type d'entité (code de EntityType ou "" pour tous)
    private String statutFilter = ""; // Statut : "", "public", "prive"
    private String etatFilter = ""; // État : "", "publie", "proposition"
    private String searchTypeFilter = "CONTAINS"; // Type de recherche : "STARTS_WITH", "CONTAINS", "EXACT"
    
    private List<Entity> references;
    private List<Entity> collections;
    private List<Entity> searchResults = new ArrayList<>(); // Résultats de la recherche

    @PostConstruct
    public void init() {
        loadReferences();
        loadCollections();
    }
    
    /**
     * Gère le changement de sélection de collection
     */
    public void onCollectionChange() {
        ApplicationBean appBean = applicationBeanProvider.get();
        TreeBean treeBean = treeBeanProvider.get();
        if (appBean == null || treeBean == null) {
            return;
        }
        
        if (collectionSelected == null || collectionSelected.isEmpty()) {
            // "Toutes les collections" est sélectionné - recharger les collections selon l'état de connexion
            appBean.loadPublicCollections();
            appBean.showCollections();
        } else {
            Entity selectedEntity = null;
            
            // Parser la valeur sélectionnée pour déterminer si c'est une collection ou une référence
            if (collectionSelected.startsWith("COL:")) {
                // C'est une collection
                String collectionCode = collectionSelected.substring(4);
                selectedEntity = collections.stream()
                        .filter(c -> c != null && c.getCode() != null && c.getCode().equals(collectionCode))
                        .findFirst()
                        .orElse(null);

                if (selectedEntity != null) {
                    Optional<Label> label = selectedEntity.getLabels().stream()
                            .filter(element -> element.getLangue().getCode().equalsIgnoreCase(langSelected))
                            .findFirst();
                    label.ifPresent(value -> appBean.setSelectedEntityLabel(value.getNom().toUpperCase()));

                    collectionBeanProvider.get().showCollectionDetail(selectedEntity);
                    // Initialiser l'arbre avec la collection comme racine
                    treeBean.initializeTreeWithEntity(selectedEntity);
                }
            } else if (collectionSelected.startsWith("REF:")) {
                // C'est une référence : format "REF:collectionCode:referenceCode"
                String[] parts = collectionSelected.split(":", 3);
                if (parts.length == 3) {
                    String collectionCode = parts[1];
                    String referenceCode = parts[2];
                    
                    // Trouver la collection parente
                    Entity parentCollection = collections.stream()
                            .filter(c -> c != null && c.getCode() != null && c.getCode().equals(collectionCode))
                            .findFirst()
                            .orElse(null);
                    
                    // S'assurer que les références sont chargées
                    if (references == null || references.isEmpty()) {
                        loadReferences();
                    }
                    // Trouver la référence et afficher ses détails
                    selectedEntity = references.stream()
                            .filter(r -> r != null && r.getCode() != null && r.getCode().equals(referenceCode))
                            .findFirst()
                            .orElse(null);
                    
                    if (selectedEntity != null) {
                        appBean.showReferenceDetail(selectedEntity);
                        appBean.setSelectedEntityLabel(getTitle(parentCollection, selectedEntity));
                        
                        // Toujours construire l'arbre en partant de la collection parente
                        if (parentCollection != null) {
                            // Initialiser l'arbre avec la collection comme racine
                            treeBean.initializeTreeWithEntity(parentCollection);
                        } else {
                            // Si on ne trouve pas la collection, essayer de la trouver via les relations
                            try {
                                List<Entity> parents = entityRelationRepository.findParentsByChild(selectedEntity);
                                if (parents != null && !parents.isEmpty()) {
                                    for (Entity parent : parents) {
                                        if (parent != null && parent.getEntityType() != null &&
                                            EntityConstants.ENTITY_TYPE_COLLECTION.equals(parent.getEntityType().getCode())) {
                                            treeBean.initializeTreeWithEntity(parent);
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("Erreur lors de la recherche de la collection parente", e);
                            }
                        }
                    }
                }
            } else {
                // Format ancien (compatibilité) - traiter comme un code de collection
                selectedEntity = collections.stream()
                        .filter(c -> c != null && c.getCode() != null && c.getCode().equals(collectionSelected))
                        .findFirst()
                        .orElse(null);
                
                if (selectedEntity != null) {
                    collectionBeanProvider.get().showCollectionDetail(selectedEntity);
                    // Initialiser l'arbre avec la collection comme racine
                    treeBean.initializeTreeWithEntity(selectedEntity);
                }
            }
        }
    }

    private String getTitle(Entity parentCollection, Entity selectedEntity) {
        Optional<Label> labelReferent = selectedEntity.getLabels().stream()
                .filter(element -> element.getLangue().getCode().equalsIgnoreCase(langSelected))
                .findFirst();

        Optional<Label> labelCollection = parentCollection.getLabels().stream()
                .filter(element -> element.getLangue().getCode().equalsIgnoreCase(langSelected))
                .findFirst();

        String title = "";
        if (labelCollection.isPresent()) {
            title = labelCollection.get().getNom().toUpperCase();
        }
        if (labelReferent.isPresent()) {
            title = title + " - " + labelReferent.get().getNom();
        }
        return title;
    }
    
    /**
     * Charge les référentiels depuis la base de données
     */
    public void loadReferences() {
        // Chargement brut depuis la base ; le filtrage par droits est appliqué plus tard
        // (dans applySearch et dans les méthodes qui exposent les données à la vue).
        references = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_REFERENCE);
    }

    /**
     * Charge les collections depuis la base de données
     */
    public void loadCollections() {
        // Chargement brut depuis la base ; le filtrage par droits est appliqué plus tard
        // (dans applySearch et dans les méthodes qui exposent les données à la vue).
        collections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION);
    }

    /**
     * Retourne une liste plate de SelectItem permettant de sélectionner les collections (par nom) et leurs références
     * Les collections sont sélectionnables directement
     * Les références sont listées avec un préfixe visuel pour indiquer leur collection parente
     */
    public List<SelectItem> getHierarchicalCollectionItems() {
        List<SelectItem> items = new ArrayList<>();
        
        // Ajouter l'option "Toutes les collections"
        items.add(new SelectItem("", "Toutes les collections"));

        ApplicationBean appBean = applicationBeanProvider != null ? applicationBeanProvider.get() : null;
        
        try {
            // S'assurer que les collections sont chargées
            if (collections == null || collections.isEmpty()) {
                loadCollections();
            }
            
            // Pour chaque collection
            for (Entity collection : collections) {
                if (collection == null || collection.getCode() == null) {
                    continue;
                }

                // Ne proposer que les collections visibles pour l'utilisateur
                if (appBean != null && !appBean.isEntityVisibleForCurrentUser(collection)) {
                    continue;
                }
                
                // Ajouter la collection comme item sélectionnable (avec son code)
                String collectionValue = "COL:" + collection.getCode();

                Optional<Label> label = collection.getLabels().stream()
                        .filter(element -> element.getLangue().getCode().equalsIgnoreCase(langSelected))
                        .findFirst();
                String collectionDisplayCode = label.isPresent() ? label.get().getNom() : collection.getCode();
                // Tronquer le code si trop long (max 40 caractères)
                if (collectionDisplayCode.length() > 40) {
                    collectionDisplayCode = collectionDisplayCode.substring(0, 37) + "...";
                }
                // Collection avec style distinctif (pas d'indentation)
                items.add(new SelectItem(collectionValue, "📁 " + collectionDisplayCode));
                
                // Récupérer les références rattachées à cette collection
                List<Entity> collectionReferences = entityRelationRepository.findChildrenByParentAndType(
                    collection, EntityConstants.ENTITY_TYPE_REFERENCE);
                
                // Filtrer selon les droits de l'utilisateur (publique / groupe / user_permission / REFUSED)
                if (appBean != null) {
                    collectionReferences = collectionReferences.stream()
                            .filter(r -> r != null && appBean.isEntityVisibleForCurrentUser(r))
                            .toList();
                }
                
                // Ajouter les références comme items sélectionnables avec indentation visuelle importante
                for (Entity reference : collectionReferences) {
                    if (reference != null && reference.getCode() != null) {
                        // Utiliser un préfixe pour identifier que c'est une référence
                        String value = "REF:" + collection.getCode() + ":" + reference.getCode();
                        // Utiliser le code de la référence avec indentation visuelle importante et icône
                        String displayCode = reference.getCode();
                        // Tronquer le code si trop long (max 40 caractères car on a plus d'espace avec l'indentation)
                        if (displayCode.length() > 40) {
                            displayCode = displayCode.substring(0, 37) + "...";
                        }
                        // Indentation importante avec caractères non-breaking space pour un décalage visible
                        items.add(new SelectItem(value, "\u00A0\u00A0\u00A0\u00A0📖 " + displayCode));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la création de la liste hiérarchique", e);
        }
        
        return items;
    }

    /**
     * Récupère récursivement toutes les entités d'une collection (référentiels, catégories, groupes, séries, types)
     */
    private Set<Entity> getAllEntitiesInCollection(Entity collection) {
        Set<Entity> allEntities = new HashSet<>();
        Set<Long> processedIds = new HashSet<>(); // Pour éviter les boucles infinies
        getAllEntitiesInCollectionRecursive(collection, allEntities, processedIds);
        return allEntities;
    }
    
    /**
     * Méthode récursive helper pour éviter les boucles infinies
     */
    private void getAllEntitiesInCollectionRecursive(Entity entity, Set<Entity> allEntities, Set<Long> processedIds) {
        if (entity == null || entity.getId() == null) {
            return;
        }
        
        // Éviter les boucles infinies
        if (processedIds.contains(entity.getId())) {
            return;
        }
        
        processedIds.add(entity.getId());
        allEntities.add(entity);
        
        try {
            // Récupérer tous les enfants directs
            List<Entity> children = entityRelationRepository.findChildrenByParent(entity);
            
            // Pour chaque enfant, récursivement récupérer ses descendants
            for (Entity child : children) {
                if (child != null && child.getId() != null) {
                    getAllEntitiesInCollectionRecursive(child, allEntities, processedIds);
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération récursive des entités pour l'entité: {}", entity.getCode(), e);
        }
    }

    /**
     * Valide que le terme de recherche a au moins 2 caractères
     */
    public boolean isValidSearchTerm() {
        return searchTerm != null && searchTerm.trim().length() >= 2;
    }

    /**
     * Effectue la recherche dans les entités
     */
    public void applySearch() {
        try {
            // Initialiser la liste des résultats
            searchResults = new ArrayList<>();
            
            // Vérifier que le terme de recherche n'est pas vide et a au moins 2 caractères
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                log.warn("Terme de recherche vide");
                return;
            }
            
            String trimmedSearchTerm = searchTerm.trim();
            
            // Validation : minimum 2 caractères
            if (trimmedSearchTerm.length() < 2) {
                log.warn("Terme de recherche trop court (minimum 2 caractères requis)");
                return;
            }
            
            log.debug("Début de la recherche avec le terme: '{}', type: '{}', statut: '{}', état: '{}', typeFilter: '{}'", 
                     trimmedSearchTerm, searchTypeFilter, statutFilter, etatFilter, typeFilter);
            
            // Effectuer la recherche selon le type de recherche (code + labels selon langue)
            List<Entity> allMatchingEntities;
            
            switch (searchTypeFilter != null ? searchTypeFilter : "CONTAINS") {
                case "STARTS_WITH":
                    allMatchingEntities = entityRepository.searchByCodeOrLabelStartsWith(trimmedSearchTerm, langSelected);
                    break;
                case "EXACT":
                    allMatchingEntities = entityRepository.searchByCodeOrLabelExact(trimmedSearchTerm, langSelected);
                    break;
                case "CONTAINS":
                default:
                    allMatchingEntities = entityRepository.searchByCodeOrLabelContains(trimmedSearchTerm, langSelected);
                    break;
            }
            
            log.debug("Nombre d'entités trouvées par la requête: {}", allMatchingEntities != null ? allMatchingEntities.size() : 0);
            
            // Filtrer selon les critères
            List<Entity> filtered = new ArrayList<>();
            if (allMatchingEntities != null) {
                filtered = allMatchingEntities.stream()
                    .filter(e -> e != null)
                    // Filtre global d'autorisation (publique / groupe / user_permission / REFUSED)
                    .filter(e -> applicationBeanProvider.get() == null || applicationBeanProvider.get().isEntityVisibleForCurrentUser(e))
                    // Filtre par type d'entité
                    .filter(e -> {
                        if (typeFilter == null || typeFilter.isEmpty()) {
                            return true; // Tous les types
                        }
                        return e.getEntityType() != null && typeFilter.equals(e.getEntityType().getCode());
                    })
                    // Filtre par statut (public/privé)
                    .filter(e -> {
                        if (statutFilter == null || statutFilter.isEmpty()) {
                            return true; // Tous
                        }
                        if ("public".equals(statutFilter)) {
                            return e.getPublique() != null && e.getPublique();
                        }
                        if ("prive".equals(statutFilter)) {
                            return e.getPublique() == null || !e.getPublique();
                        }
                        return true;
                    })
                    // Filtre par état (publié/proposition)
                    .filter(e -> {
                        if (etatFilter == null || etatFilter.isEmpty()) {
                            return true; // Tous
                        }
                        if ("publie".equals(etatFilter)) {
                            // Publié = ACCEPTED ou AUTOMATIC
                            String statut = e.getStatut();
                            return statut != null && ("ACCEPTED".equals(statut) || "AUTOMATIC".equals(statut));
                        }
                        if ("proposition".equals(etatFilter)) {
                            // Proposition = PROPOSITION
                            return "PROPOSITION".equals(e.getStatut());
                        }
                        return true;
                    })
                    // Filtre par collection si sélectionnée
                    .filter(e -> {
                        if (collectionSelected == null || collectionSelected.isEmpty()) {
                            return true; // Toutes les collections
                        }
                        // Si une collection est sélectionnée, vérifier si l'entité appartient à cette collection
                        try {
                            if (collections == null) {
                                loadCollections();
                            }
                            
                            final String collectionCode;
                            if (collectionSelected.startsWith("COL:")) {
                                collectionCode = collectionSelected.substring(4);
                            } else if (collectionSelected.startsWith("REF:")) {
                                String[] parts = collectionSelected.split(":", 3);
                                if (parts.length == 3) {
                                    collectionCode = parts[1];
                                } else {
                                    collectionCode = null;
                                }
                            } else {
                                collectionCode = collectionSelected;
                            }
                            
                            if (collectionCode != null) {
                                final String finalCollectionCode = collectionCode;
                                Entity selectedCollection = collections.stream()
                                    .filter(c -> c != null && c.getCode() != null && c.getCode().equals(finalCollectionCode))
                                    .findFirst()
                                    .orElse(null);
                                
                                if (selectedCollection != null) {
                                    Set<Entity> collectionEntities = getAllEntitiesInCollection(selectedCollection);
                                    return collectionEntities.contains(e);
                                }
                            }
                            return false;
                        } catch (Exception ex) {
                            log.error("Erreur lors du filtrage par collection", ex);
                            return true; // En cas d'erreur, inclure l'entité
                        }
                    })
                    .collect(Collectors.toList());
            }
            
            // Trier par ordre alphabétique décroissant (Z à A)
            filtered.sort(Comparator.comparing((Entity e) -> 
                e.getNom() != null ? e.getNom().toLowerCase() : "").reversed());
            
            searchResults = filtered;
            
            log.info("Recherche effectuée : {} résultats trouvés", searchResults.size());
            
        } catch (Exception e) {
            log.error("Erreur lors de la recherche", e);
            searchResults = new ArrayList<>();
            // Ne pas propager l'exception pour éviter de casser l'interface
        }
    }
    
    /**
     * Vérifie s'il y a des résultats de recherche
     */
    public boolean hasSearchResults() {
        return searchResults != null && !searchResults.isEmpty();
    }
    
    /**
     * Retourne le nombre de résultats
     */
    public int getSearchResultsCount() {
        return searchResults != null ? searchResults.size() : 0;
    }
    
    /**
     * Affiche les détails d'une entité depuis les résultats de recherche
     */
    public void showEntityDetail(Entity entity) {
        if (entity == null || entity.getEntityType() == null) {
            return;
        }
        
        ApplicationBean appBean = applicationBeanProvider.get();
        TreeBean treeBean = treeBeanProvider.get();
        if (appBean == null) {
            return;
        }
        
        String entityTypeCode = entity.getEntityType().getCode();
        
        // Appeler la méthode appropriée selon le type d'entité
        if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(entityTypeCode)) {
            collectionBeanProvider.get().showCollectionDetail(entity);
            // Initialiser l'arbre avec la collection comme racine
            if (treeBean != null) {
                treeBean.initializeTreeWithEntity(entity);
            }
        } else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entityTypeCode) || 
                   "REFERENTIEL".equals(entityTypeCode)) {
            // Pour une référence, trouver la collection parente et construire l'arbre à partir de celle-ci
            Entity parentCollection = null;
            try {
                // Chercher la collection parente via les relations
                List<Entity> parents = entityRelationRepository.findParentsByChild(entity);
                if (parents != null && !parents.isEmpty()) {
                    for (Entity parent : parents) {
                        if (parent != null && parent.getEntityType() != null &&
                            EntityConstants.ENTITY_TYPE_COLLECTION.equals(parent.getEntityType().getCode())) {
                            parentCollection = parent;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la recherche de la collection parente pour la référence : {}", 
                         entity.getCode(), e);
            }
            
            // Afficher les détails de la référence
            appBean.showReferenceDetail(entity);
            
            // Construire l'arbre en partant de la collection parente
            if (treeBean != null && parentCollection != null) {
                treeBean.initializeTreeWithEntity(parentCollection);
            } else if (treeBean != null) {
                // Si on ne trouve pas la collection parente, essayer de trouver via les collections chargées
                if (collections == null || collections.isEmpty()) {
                    loadCollections();
                }
                // Chercher dans les collections chargées si la référence est liée
                for (Entity collection : collections) {
                    if (collection != null) {
                        try {
                            List<Entity> refs = entityRelationRepository.findChildrenByParentAndType(
                                collection, EntityConstants.ENTITY_TYPE_REFERENCE);
                            if (refs != null && refs.stream().anyMatch(r -> 
                                r != null && r.getCode() != null && r.getCode().equals(entity.getCode()))) {
                                treeBean.initializeTreeWithEntity(collection);
                                break;
                            }
                        } catch (Exception e) {
                            log.debug("Erreur lors de la vérification de la relation collection-référence", e);
                        }
                    }
                }
            }
        } else if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(entityTypeCode) || 
                   "CATEGORIE".equals(entityTypeCode)) {
            appBean.showCategoryDetail(entity);
        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(entityTypeCode) || 
                   "GROUPE".equals(entityTypeCode)) {
            appBean.showGroupe(entity);
        } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(entityTypeCode) || 
                   "SERIE".equals(entityTypeCode)) {
            appBean.showSerie(entity);
        } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(entityTypeCode)) {
            appBean.showType(entity);
        }
    }
    
    /**
     * Retourne l'icône appropriée selon le type d'entité
     */
    public String getEntityIcon(Entity entity) {
        if (entity == null || entity.getEntityType() == null) {
            return "pi pi-circle";
        }
        
        String entityTypeCode = entity.getEntityType().getCode();
        
        if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(entityTypeCode)) {
            return "pi pi-folder";
        } else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entityTypeCode) || 
                   "REFERENTIEL".equals(entityTypeCode)) {
            return "pi pi-book";
        } else if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(entityTypeCode) || 
                   "CATEGORIE".equals(entityTypeCode)) {
            return "pi pi-tags";
        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(entityTypeCode) || 
                   "GROUPE".equals(entityTypeCode)) {
            return "pi pi-users";
        } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(entityTypeCode) || 
                   "SERIE".equals(entityTypeCode)) {
            return "pi pi-list";
        } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(entityTypeCode)) {
            return "pi pi-tag";
        }
        
        return "pi pi-circle";
    }
    
    /**
     * Retourne le label du type d'entité
     */
    public String getEntityTypeLabel(Entity entity) {
        if (entity == null || entity.getEntityType() == null) {
            return "Entité";
        }
        
        String entityTypeCode = entity.getEntityType().getCode();
        
        if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(entityTypeCode)) {
            return "Collection";
        } else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entityTypeCode) || 
                   "REFERENTIEL".equals(entityTypeCode)) {
            return "Référentiel";
        } else if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(entityTypeCode) || 
                   "CATEGORIE".equals(entityTypeCode)) {
            return "Catégorie";
        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(entityTypeCode) || 
                   "GROUPE".equals(entityTypeCode)) {
            return "Groupe";
        } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(entityTypeCode) || 
                   "SERIE".equals(entityTypeCode)) {
            return "Série";
        } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(entityTypeCode)) {
            return "Type";
        }
        
        return "Entité";
    }

    /**
     * Retourne la liste des types d'entités pour le filtre
     */
    public List<SelectItem> getEntityTypeFilterItems() {
        List<SelectItem> items = new ArrayList<>();
        
        // Option "Tous les types"
        items.add(new SelectItem("", "Tous les types"));
        
        try {
            // Récupérer tous les types d'entités depuis la base de données
            List<EntityType> entityTypes = entityTypeRepository.findAll();
            
            // Trier par code
            entityTypes.sort(Comparator.comparing(EntityType::getCode));
            
            // Ajouter chaque type avec son label
            for (EntityType entityType : entityTypes) {
                if (entityType != null && entityType.getCode() != null) {
                    String label = getEntityTypeDisplayLabel(entityType.getCode());
                    items.add(new SelectItem(entityType.getCode(), label));
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des types d'entités", e);
        }
        
        return items;
    }

    /**
     * Retourne le label d'affichage pour un code de type d'entité
     */
    private String getEntityTypeDisplayLabel(String code) {
        if (code == null) {
            return "Inconnu";
        }
        
        // Utiliser if-else pour éviter les problèmes avec les constantes et les valeurs littérales
        if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(code)) {
            return "Collection";
        } else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(code) || "REFERENTIEL".equals(code)) {
            return "Référentiel";
        } else if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(code) || "CATEGORIE".equals(code)) {
            return "Catégorie";
        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(code) || "GROUPE".equals(code)) {
            return "Groupe";
        } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(code) || "SERIE".equals(code)) {
            return "Série";
        } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(code)) {
            return "Type";
        } else {
            return code;
        }
    }

    /**
     * Retourne la liste des statuts pour le filtre
     */
    public List<SelectItem> getStatutFilterItems() {
        List<SelectItem> items = new ArrayList<>();
        items.add(new SelectItem("", "Tous"));
        items.add(new SelectItem("public", "Public"));
        items.add(new SelectItem("prive", "Privé"));
        return items;
    }

    /**
     * Retourne la liste des états pour le filtre
     */
    public List<SelectItem> getEtatFilterItems() {
        List<SelectItem> items = new ArrayList<>();
        items.add(new SelectItem("", "Tous"));
        items.add(new SelectItem("publie", "Publié"));
        items.add(new SelectItem("proposition", "Proposition"));
        return items;
    }

    /**
     * Retourne la liste des types de recherche pour le filtre
     */
    public List<SelectItem> getSearchTypeFilterItems() {
        List<SelectItem> items = new ArrayList<>();
        items.add(new SelectItem("CONTAINS", "Contient"));
        items.add(new SelectItem("STARTS_WITH", "Commence par"));
        items.add(new SelectItem("EXACT", "Chaîne exacte"));
        return items;
    }
}

