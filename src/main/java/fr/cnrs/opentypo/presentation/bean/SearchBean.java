package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
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
    private Provider<ApplicationBean> applicationBeanProvider;

    @Inject
    private Provider<CollectionBean> collectionBeanProvider;

    @Inject
    private Provider<TreeBean> treeBeanProvider;

    private String searchSelected;
    private String collectionSelected;
    private String langSelected = "fr"; // Français sélectionné par défaut
    private String searchTerm; // Terme de recherche
    
    private List<Entity> references;
    private List<Entity> collections;
    private List<Entity> searchResults = new ArrayList<>(); // Résultats de la recherche

    @PostConstruct
    public void init() {
        loadreferences();
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
                        loadreferences();
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

    /**
     * Retourne une liste plate de SelectItem permettant de sélectionner les collections (par nom) et leurs références
     * Les collections sont sélectionnables directement
     * Les références sont listées avec un préfixe visuel pour indiquer leur collection parente
     */
    public List<SelectItem> getHierarchicalCollectionItems() {
        List<SelectItem> items = new ArrayList<>();
        
        // Ajouter l'option "Toutes les collections"
        items.add(new SelectItem("", "Toutes les collections"));
        
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
                
                // Filtrer pour ne garder que les références publiques
                collectionReferences = collectionReferences.stream()
                    .filter(r -> r != null && r.getPublique() != null && r.getPublique())
                    .collect(Collectors.toList());
                
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
     * Effectue la recherche dans les entités
     */
    public void applySearch() {
        try {
            // Initialiser la liste des résultats
            searchResults = new ArrayList<>();
            
            // Vérifier que le terme de recherche n'est pas vide
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                log.warn("Terme de recherche vide");
                return;
            }
            
            String trimmedSearchTerm = searchTerm.trim();
            log.debug("Début de la recherche avec le terme: '{}'", trimmedSearchTerm);
            
            // Si une collection est sélectionnée, rechercher uniquement dans cette collection
            if (collectionSelected != null && !collectionSelected.isEmpty()) {
                log.debug("Recherche dans la collection: {}", collectionSelected);
                
                // Vérifier que la liste des collections est chargée
                if (collections == null) {
                    loadCollections();
                }
                
                // Parser la valeur sélectionnée pour déterminer si c'est une collection ou une référence
                Entity selectedCollection = null;
                String collectionCode;
                
                if (collectionSelected.startsWith("COL:")) {
                    // C'est une collection
                    collectionCode = collectionSelected.substring(4);
                } else if (collectionSelected.startsWith("REF:")) {
                    // C'est une référence : format "REF:collectionCode:referenceCode"
                    String[] parts = collectionSelected.split(":", 3);
                    if (parts.length == 3) {
                        collectionCode = parts[1];
                    } else {
                        collectionCode = null;
                    }
                } else {
                    // Format ancien (compatibilité)
                    collectionCode = collectionSelected;
                }
                
                // Trouver la collection sélectionnée
                if (collectionCode != null) {
                    selectedCollection = collections.stream()
                        .filter(c -> c != null && c.getCode() != null && c.getCode().equals(collectionCode))
                        .findFirst()
                        .orElse(null);
                }
                
                if (selectedCollection != null) {
                    log.debug("Collection trouvée: {}", selectedCollection.getNom());
                    
                    // Récupérer toutes les entités de la collection (récursivement)
                    Set<Entity> collectionEntities = getAllEntitiesInCollection(selectedCollection);
                    log.debug("Nombre d'entités dans la collection: {}", collectionEntities.size());
                    
                    // Filtrer les entités dont le nom contient le terme de recherche
                    List<Entity> filtered = collectionEntities.stream()
                        .filter(e -> e != null && e.getNom() != null && 
                                     e.getNom().toLowerCase().contains(trimmedSearchTerm.toLowerCase()))
                        .collect(Collectors.toList());
                    
                    // Trier par ordre alphabétique décroissant (Z à A)
                    filtered.sort(Comparator.comparing((Entity e) -> 
                        e.getNom() != null ? e.getNom().toLowerCase() : "").reversed());
                    searchResults = filtered;
                    
                    log.info("Recherche effectuée dans la collection '{}' : {} résultats trouvés", 
                             selectedCollection.getNom(), searchResults.size());
                } else {
                    log.warn("Collection sélectionnée non trouvée : {}", collectionSelected);
                }
            } else {
                log.debug("Recherche dans toutes les collections");
                
                // Rechercher dans toutes les collections publiques
                // Récupérer toutes les entités publiques dont le nom contient le terme
                List<Entity> allMatchingEntities = entityRepository.findByNomContainingIgnoreCaseQuery(trimmedSearchTerm);
                log.debug("Nombre d'entités trouvées par la requête: {}", allMatchingEntities != null ? allMatchingEntities.size() : 0);
                
                // Filtrer pour ne garder que les entités publiques
                if (allMatchingEntities != null) {
                    List<Entity> filtered = allMatchingEntities.stream()
                        .filter(e -> e != null && e.getPublique() != null && e.getPublique())
                        .collect(Collectors.toList());
                    
                    // Trier par ordre alphabétique décroissant (Z à A)
                    filtered.sort(Comparator.comparing((Entity e) -> 
                        e.getNom() != null ? e.getNom().toLowerCase() : "").reversed());
                    searchResults = filtered;
                }
                
                log.info("Recherche effectuée dans toutes les collections : {} résultats trouvés", 
                         searchResults != null ? searchResults.size() : 0);
            }
            
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
}

