package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
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
import fr.cnrs.opentypo.domain.entity.Image;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.UserPermission;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.photos.Photo;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
@SessionScoped
@Named("applicationBean")
public class ApplicationBean implements Serializable {

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
    private UserPermissionRepository userPermissionRepository;

    @Inject
    private CollectionBean collectionBean;

    @Inject
    private TreeBean treeBean;


    private final PanelStateManager panelState = new PanelStateManager();
    private List<Language> languages;
    private List<Entity> beadCrumbElements, references, collections;
    
    /** Entité actuellement sélectionnée (collection, référentiel, catégorie, groupe, série ou type). */
    private Entity selectedEntity;

    /** Enfants directs de l'entité sélectionnée (référentiels, catégories, groupes, séries ou types selon le niveau). */
    private List<Entity> childs = new ArrayList<>();

    // Titre de l'écran
    private String selectedEntityLabel;

    /**
     * Retourne l'ensemble des IDs de collections que l'utilisateur connecté est autorisé à consulter
     * (via la table user_permission). Pour un utilisateur non connecté, l'ensemble est vide.
     */
    private Set<Long> getAllowedCollectionIdsForCurrentUser() {
        if (loginBean == null || !loginBean.isAuthenticated() || loginBean.getCurrentUser() == null) {
            return new HashSet<>();
        }
        Utilisateur current = loginBean.getCurrentUser();
        List<UserPermission> permissions = userPermissionRepository.findByUtilisateur(current);
        Set<Long> ids = new HashSet<>();
        if (permissions == null) {
            return ids;
        }
        for (UserPermission permission : permissions) {
            if (permission == null || permission.getEntity() == null) {
                continue;
            }
            Entity e = permission.getEntity();
            if (e.getEntityType() != null
                    && EntityConstants.ENTITY_TYPE_COLLECTION.equals(e.getEntityType().getCode())
                    && e.getId() != null) {
                ids.add(e.getId());
            }
        }
        return ids;
    }

    /**
     * Indique si une entité est visible pour l'utilisateur connecté, selon les règles métier :
     * - Jamais d'entités avec statut REFUSED.
     * - Utilisateur non connecté : uniquement les entités publiques.
     * - Administrateur technique : toutes les entités sauf REFUSED.
     * - Autres groupes : uniquement les collections autorisées et les entités rattachées
     *   à ces collections (via les relations), quel que soit le statut public/privé.
     */
    public boolean isEntityVisibleForCurrentUser(Entity entity) {
        if (entity == null) {
            return false;
        }

        // 1) Ne jamais afficher les entités au statut REFUSED
        if (EntityStatusEnum.REFUSED.name().equals(entity.getStatut())) {
            return false;
        }

        // 2) Règle globale : toutes les collections publiques et tous leurs éléments
        //    sont visibles pour tout le monde (connecté ou non).
        Entity collectionAncestorForPublicRule = null;
        if (entity.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_COLLECTION.equals(entity.getEntityType().getCode())) {
            collectionAncestorForPublicRule = entity;
        } else {
            collectionAncestorForPublicRule = findAncestorOfType(entity, EntityConstants.ENTITY_TYPE_COLLECTION);
        }
        if (collectionAncestorForPublicRule != null
                && Boolean.TRUE.equals(collectionAncestorForPublicRule.getPublique())) {
            return true;
        }

        boolean authenticated = loginBean != null && loginBean.isAuthenticated();
        boolean isAdminTechnique = loginBean != null && loginBean.isAdminTechnique();

        // 3) Utilisateur non connecté : ne voit que le contenu public
        if (!authenticated) {
            return Boolean.TRUE.equals(entity.getPublique());
        }

        // 4) Administrateur technique : tout voir (sauf REFUSED déjà filtré)
        if (isAdminTechnique) {
            return true;
        }

        // 5) Autres groupes connectés : filtrer par collections autorisées
        Set<Long> allowedCollectionIds = getAllowedCollectionIdsForCurrentUser();
        if (allowedCollectionIds == null || allowedCollectionIds.isEmpty()) {
            return false;
        }

        Entity collectionAncestor = null;
        if (entity.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_COLLECTION.equals(entity.getEntityType().getCode())) {
            collectionAncestor = entity;
        } else {
            collectionAncestor = findAncestorOfType(entity, EntityConstants.ENTITY_TYPE_COLLECTION);
        }

        return collectionAncestor != null
                && collectionAncestor.getId() != null
                && allowedCollectionIds.contains(collectionAncestor.getId());
    }

    /**
     * Retourne l'ancêtre de type donné en remontant l'arbre des relations parent-enfant.
     */
    private Entity findAncestorOfType(Entity entity, String typeCode) {
        if (entity == null || typeCode == null) return null;
        Entity current = entity;
        while (current != null) {
            if (current.getEntityType() != null && typeCode.equals(current.getEntityType().getCode())) {
                return current;
            }
            List<Entity> parents = entityRelationRepository.findParentsByChild(current);
            if (parents == null || parents.isEmpty()) break;
            current = parents.get(0);
        }
        return null;
    }

    /**
     * Construit le fil d'Ariane (collection → ... → selectedEntity) à partir de selectedEntity.
     */
    public List<Entity> buildBreadcrumbFromSelectedEntity() {
        if (selectedEntity == null) return new ArrayList<>();
        List<Entity> pathToRoot = new ArrayList<>();
        Entity current = selectedEntity;
        while (current != null) {
            pathToRoot.add(0, current);
            List<Entity> parents = entityRelationRepository.findParentsByChild(current);
            if (parents == null || parents.isEmpty()) break;
            current = parents.get(0);
        }
        return pathToRoot;
    }

    /**
     * Recharge la liste des enfants directs de selectedEntity selon son type.
     */
    public void refreshChilds() {

        if (selectedEntity == null) {
            childs = new ArrayList<>();
            return;
        }

        switch(selectedEntity.getEntityType().getCode()) {
            case EntityConstants.ENTITY_TYPE_COLLECTION:
                childs = referenceService.loadReferencesByCollection(selectedEntity);
                break;
            case EntityConstants.ENTITY_TYPE_REFERENCE:
                childs = categoryService.loadCategoriesByReference(selectedEntity);
                break;
            case EntityConstants.ENTITY_TYPE_CATEGORY:
                childs = groupService.loadCategoryGroups(selectedEntity);
                break;
            case EntityConstants.ENTITY_TYPE_GROUP:
                List<Entity> series = serieService.loadGroupSeries(selectedEntity);
                List<Entity> types = typeService.loadGroupTypes(selectedEntity);
                childs = new ArrayList<>();
                if (series != null) childs.addAll(series);
                if (types != null) childs.addAll(types);
            case EntityConstants.ENTITY_TYPE_SERIES:
                childs = typeService.loadSerieTypes(selectedEntity);
                break;
            default:
                childs = new ArrayList<>();
        }
    }

    public Entity getSelectedCollection() { return findAncestorOfType(selectedEntity, EntityConstants.ENTITY_TYPE_COLLECTION); }
    public Entity getSelectedReference() { return findAncestorOfType(selectedEntity, EntityConstants.ENTITY_TYPE_REFERENCE); }
    public Entity getSelectedCategory() { return findAncestorOfType(selectedEntity, EntityConstants.ENTITY_TYPE_CATEGORY); }
    public Entity getSelectedGroup() { return findAncestorOfType(selectedEntity, EntityConstants.ENTITY_TYPE_GROUP); }
    public Entity getSelectedSerie() { return findAncestorOfType(selectedEntity, EntityConstants.ENTITY_TYPE_SERIES); }
    public Entity getSelectedType() { return findAncestorOfType(selectedEntity, EntityConstants.ENTITY_TYPE_TYPE); }

    /** Enfants de type Série (quand selectedEntity est un groupe). */
    public List<Entity> getChildsSeries() {
        if (childs == null) return new ArrayList<>();
        if (selectedEntity == null || selectedEntity.getEntityType() == null) return new ArrayList<>();
        if (!EntityConstants.ENTITY_TYPE_GROUP.equals(selectedEntity.getEntityType().getCode())) return new ArrayList<>();
        return childs.stream()
                .filter(e -> e.getEntityType() != null && EntityConstants.ENTITY_TYPE_SERIES.equals(e.getEntityType().getCode()))
                .collect(Collectors.toList());
    }
    /** Enfants de type Type (quand selectedEntity est un groupe). */
    public List<Entity> getChildsTypes() {
        if (childs == null) return new ArrayList<>();
        if (selectedEntity == null || selectedEntity.getEntityType() == null) return new ArrayList<>();
        if (!EntityConstants.ENTITY_TYPE_GROUP.equals(selectedEntity.getEntityType().getCode())) return new ArrayList<>();
        return childs.stream()
                .filter(e -> e.getEntityType() != null && EntityConstants.ENTITY_TYPE_TYPE.equals(e.getEntityType().getCode()))
                .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des photos pour la galerie, construite à partir des images de l'entité sélectionnée.
     * Les images proviennent de la table image (entity_id = selectedEntity.id).
     */
    public List<Photo> getGalleriaPhotos() {
        List<Photo> photos = new ArrayList<>();
        if (selectedEntity == null || selectedEntity.getImages() == null) {
            return photos;
        }
        String entityLabel = selectedEntity.getNom() != null ? selectedEntity.getNom() : selectedEntity.getCode();
        int index = 1;
        for (Image img : selectedEntity.getImages()) {
            if (img != null && img.getUrl() != null && !img.getUrl().trim().isEmpty()) {
                String title = entityLabel != null ? entityLabel + " - " + index : "Image " + index;
                photos.add(new Photo(img.getUrl(), img.getUrl(), title, title));
                index++;
            }
        }
        return photos;
    }

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
    public void loadReferences() {
        references = new ArrayList<>();
        try {
            references = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_REFERENCE);
            // Filtrer selon les droits de l'utilisateur (publique / groupe / user_permission / statut REFUSED)
            references = references.stream()
                    .filter(this::isEntityVisibleForCurrentUser)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des référentiels depuis la base de données", e);
            references = new ArrayList<>();
        }
    }

    /**
     * Charge les collections depuis la base de données et les filtre
     * selon les droits de l'utilisateur (publique / groupe / user_permission / statut REFUSED),
     * puis les trie par ordre alphabétique décroissant.
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
            
            collections = collections.stream()
                // Filtrer selon les droits et le statut
                .filter(this::isEntityVisibleForCurrentUser)
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
        if (getSelectedCollection() != null) {
            collectionBean.updateCollectionLanguage(this);
        }
    }

    public void showSelectedPanel(Entity entity) {
        switch (entity.getEntityType().getCode()) {
            case EntityConstants.ENTITY_TYPE_COLLECTION:
                collectionBean.showCollectionDetail(this, entity);
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
            collectionBean.showCollectionDetail(this, entity);
        } else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(code)) {
            showReferenceDetail(entity);
        } else if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(code)) {
            showCategoryDetail(entity);
        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(code)) {
            showGroupe(entity);
        } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(code)) {
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
        this.selectedEntity = null;
        this.beadCrumbElements = new ArrayList<>();
        this.childs = new ArrayList<>();
        searchBean.setCollectionSelected(null);
        panelState.showCollections();
    }

    /**
     * Appelé au clic sur le menu Accueil. Réinitialise l'interface (panelState.showCollections) puis redirige vers l'Accueil.
     */
    public String goToAccueil() {
        showCollections();
        return "/index.xhtml?faces-redirect=true";
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
        if (group != null && group.getId() != null) {
            try {
                this.selectedEntity = entityRepository.findById(group.getId()).orElse(group);
            } catch (Exception e) {
                log.error("Erreur lors du rechargement du groupe depuis la base de données", e);
                this.selectedEntity = group;
            }
        } else {
            this.selectedEntity = group;
        }
        panelState.showGroupe();
        refreshChilds();
        beadCrumbElements = buildBreadcrumbFromSelectedEntity();
        treeBean.expandPathAndSelectEntity(selectedEntity);
    }

    /**
     * Affiche les détails d'un référentiel spécifique
     */
    public void showReferenceDetail(Entity reference) {
        this.selectedEntity = reference;
        panelState.showReference();
        beadCrumbElements = buildBreadcrumbFromSelectedEntity();
        refreshChilds();
        treeBean.selectReferenceNode(reference);
        treeBean.loadChildForEntity(reference);
    }

    public void refreshCollectionReferencesList() {
        if (getSelectedCollection() != null) {
            childs = referenceService.loadReferencesByCollection(getSelectedCollection());
        } else {
            childs = new ArrayList<>();
        }
    }

    public void refreshReferenceCategoriesList() {
        refreshChilds();
    }

    public void showSerie() {
        panelState.showSerie();
    }
    
    /**
     * Affiche les détails d'une série spécifique
     */
    public void showSerie(Entity serie) {
        this.selectedEntity = entityRepository.findById(serie.getId()).orElse(serie);
        panelState.showSerie();
        refreshChilds();
        beadCrumbElements = buildBreadcrumbFromSelectedEntity();
        treeBean.expandPathAndSelectEntity(selectedEntity);
    }

    public void showType() {
        panelState.showType();
    }
    
    /**
     * Affiche les détails d'un type spécifique (charge les images pour la galerie).
     */
    public void showType(Entity type) {
        this.selectedEntity = entityRepository.findByIdWithImages(type.getId()).orElse(type);
        panelState.showType();
        refreshChilds();
        beadCrumbElements = buildBreadcrumbFromSelectedEntity();
        treeBean.expandPathAndSelectEntity(selectedEntity);
    }

    /**
     * Affiche les détails d'une catégorie spécifique
     */
    public void showCategoryDetail(Entity category) {
        this.selectedEntity = category;
        panelState.showCategory();
        refreshChilds();
        beadCrumbElements = buildBreadcrumbFromSelectedEntity();
        treeBean.expandPathAndSelectEntity(category);
    }

    public void refreshCategoryGroupsList() {
        refreshChilds();
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
        refreshChilds();
    }

    /**
     * Recharge la liste des types du groupe sélectionné depuis la table entity_relation
     */
    public void refreshGroupTypesList() {
        refreshChilds();
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
        collectionBean.showCollectionDetail(this, collection);
        
        // Activer le mode édition
        collectionBean.startEditingCollection(this);
        
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
                .orElse("");
    }
    
    /**
     * Supprime une collection et toutes ses entités rattachées
     */
    @Transactional
    public void deleteCollection(Entity collection) {
        if (collection == null || collection.getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur", "Aucune collection sélectionnée."));
            return;
        }

        String collectionCode = collection.getCode();
        String collectionName = collection.getNom();
        Long collectionId = collection.getId();

        // Supprimer récursivement la collection et toutes ses entités enfants
        deleteEntityRecursively(collection);

        // Réinitialiser la sélection si c'était la collection sélectionnée
        if (selectedEntity != null && selectedEntity.getId().equals(collectionId)) {
            selectedEntity = null;
            selectedEntityLabel = "";
            childs = new ArrayList<>();
        }

        // Recharger les collections
        loadPublicCollections();

        // Mettre à jour l'arbre
        treeBean.initializeTreeWithCollection();

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
    }
}

