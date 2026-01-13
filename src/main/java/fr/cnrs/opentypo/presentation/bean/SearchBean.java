package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
        if (appBean == null) {
            return;
        }
        
        if (collectionSelected == null || collectionSelected.isEmpty()) {
            // "Toutes les collections" est sélectionné - afficher le panel des collections
            appBean.showCollections();
        } else {
            // Une collection spécifique est sélectionnée - afficher ses détails
            collections.stream()
                    .filter(c -> c.getCode().equals(collectionSelected))
                    .findFirst().ifPresent(appBean::showCollectionDetail);

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
                
                // Trouver la collection sélectionnée
                Entity selectedCollection = collections.stream()
                    .filter(c -> c != null && c.getCode() != null && c.getCode().equals(collectionSelected))
                    .findFirst()
                    .orElse(null);
                
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
        if (appBean == null) {
            return;
        }
        
        String entityTypeCode = entity.getEntityType().getCode();
        
        // Appeler la méthode appropriée selon le type d'entité
        if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(entityTypeCode)) {
            appBean.showCollectionDetail(entity);
        } else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entityTypeCode) || 
                   "REFERENTIEL".equals(entityTypeCode)) {
            appBean.showReferenceDetail(entity);
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

