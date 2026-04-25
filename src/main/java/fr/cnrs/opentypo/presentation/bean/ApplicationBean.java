package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.application.dto.SerieWithTypes;
import fr.cnrs.opentypo.application.service.AuditService;
import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.CollectionService;
import fr.cnrs.opentypo.application.service.EntityDeletionService;
import fr.cnrs.opentypo.application.service.EntityImageService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.ReferenceService;
import fr.cnrs.opentypo.application.service.SerieService;
import fr.cnrs.opentypo.application.service.TypeService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.common.models.Language;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Image;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.UserPermission;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.AuteurRepository;
import fr.cnrs.opentypo.infrastructure.persistence.CaracteristiquePhysiqueMonnaieRepository;
import fr.cnrs.opentypo.infrastructure.persistence.CaracteristiquePhysiqueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.CommentaireRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionDetailRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionMonnaieRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionPateRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityMetadataRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ExternalAlignmentRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ImageRepository;
import fr.cnrs.opentypo.infrastructure.persistence.InternalAlignmentRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LabelRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ParametrageRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.CandidatBean;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatReferenceTreeService;
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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Comparator;
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
    private InternalAlignmentRepository internalAlignmentRepository;

    @Inject
    private ExternalAlignmentRepository externalAlignmentRepository;

    @Inject
    @Lazy
    private CollectionBean collectionBean;

    @Inject
    private Provider<TreeBean> treeBeanProvider;

    @Inject
    private UserBean userBean;

    @Inject
    private Provider<CandidatBean> candidatBeanProvider;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ParametrageRepository parametrageRepository;

    @Autowired
    private CandidatReferenceTreeService candidatReferenceTreeService;

    @Autowired
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    @Autowired
    private AuteurRepository auteurRepository;

    @Autowired
    private CommentaireRepository commentaireRepository;

    @Autowired
    private DescriptionRepository descriptionRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private EntityImageService entityImageService;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private EntityMetadataRepository entityMetadataRepository;

    @Autowired
    private DescriptionDetailRepository descriptionDetailRepository;

    @Autowired
    private CaracteristiquePhysiqueRepository caracteristiquePhysiqueRepository;

    @Autowired
    private DescriptionPateRepository descriptionPateRepository;

    @Autowired
    private DescriptionMonnaieRepository descriptionMonnaieRepository;

    @Autowired
    private CaracteristiquePhysiqueMonnaieRepository caracteristiquePhysiqueMonnaieRepository;

    @Autowired
    private EntityDeletionService entityDeletionService;

    @Autowired
    private AuditService auditService;

    private static final DateTimeFormatter ENTITY_AUDIT_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PanelStateManager panelState = new PanelStateManager();
    private List<Language> languages;
    private List<Entity> breadCrumbElements, references, collections;
    
    /** Entité actuellement sélectionnée (collection, référentiel, catégorie, groupe, série ou type). */
    private Entity selectedEntity;

    /** Enfants directs de l'entité sélectionnée (référentiels, catégories, groupes, séries ou types selon le niveau). */
    private List<Entity> childs = new ArrayList<>();

    /** Filtre de recherche pour les référentiels de la collection. */
    private String referencesSearchQuery = "";
    /** Page courante pour la pagination des référentiels (1-based). */
    private int referencesCurrentPage = 1;
    /** Afficher tous les référentiels en une seule page. */
    private boolean referencesViewAll = false;
    private static final int REFERENCES_PAGE_SIZE = 6;

    /** Filtre de recherche pour les catégories du référentiel. */
    private String categoriesSearchQuery = "";
    /** Page courante pour la pagination des catégories (1-based). */
    private int categoriesCurrentPage = 1;
    /** Afficher toutes les catégories en une seule page. */
    private boolean categoriesViewAll = false;
    private static final int CATEGORIES_PAGE_SIZE = 6;

    /** Filtre de recherche pour les groupes de la catégorie. */
    private String groupesSearchQuery = "";
    /** Page courante pour la pagination des groupes (1-based). */
    private int groupesCurrentPage = 1;
    /** Afficher tous les groupes en une seule page. */
    private boolean groupesViewAll = false;
    private static final int GROUPES_PAGE_SIZE = 6;

    /** Filtre de recherche pour les séries du groupe. */
    private String seriesSearchQuery = "";
    /** Page courante pour la pagination des séries (1-based). */
    private int seriesCurrentPage = 1;
    /** Afficher toutes les séries en une seule page (sans pagination). */
    private boolean seriesViewAll = false;
    private static final int SERIES_PAGE_SIZE = 6;

    /** Filtre de recherche pour les types de la série. */
    private String typesSearchQuery = "";
    /** Page courante pour la pagination des types (1-based). */
    private int typesCurrentPage = 1;
    /** Afficher tous les types en une seule page. */
    private boolean typesViewAll = false;
    private static final int TYPES_PAGE_SIZE = 6;

    /** Mode de tri unifié pour collections, référentiels, catégories, groupes, séries et types. */
    public enum ListSortMode {
        MANUAL("Ordre personnalisé"),
        ALPHA_ASC("A → Z"),
        ALPHA_DESC("Z → A");

        private final String label;

        ListSortMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private ListSortMode collectionsSortMode = ListSortMode.MANUAL;
    private ListSortMode referencesSortMode = ListSortMode.MANUAL;
    private ListSortMode categoriesSortMode = ListSortMode.MANUAL;
    private ListSortMode groupesSortMode = ListSortMode.MANUAL;
    private ListSortMode seriesSortMode = ListSortMode.MANUAL;
    private ListSortMode typesSortMode = ListSortMode.MANUAL;

    /** Type de liste en cours de réorganisation (un seul dialog générique). */
    public enum OrderingType {
        COLLECTIONS("Réorganiser l'ordre des typologies"),
        TYPES("Réorganiser l'ordre des types"),
        SERIES("Réorganiser l'ordre des séries"),
        GROUPES("Réorganiser l'ordre des groupes"),
        CATEGORIES("Réorganiser l'ordre des catégories"),
        REFERENCES("Réorganiser l'ordre des référentiels");

        private final String title;

        OrderingType(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    /** Liste des IDs pour le dialog de réorganisation (ordre modifiable). */
    private List<Long> orderingIds = new ArrayList<>();
    /** Map label par ID pour l'affichage dans le dialog. */
    private java.util.Map<Long, String> orderingLabelsById = new java.util.HashMap<>();
    /** Type de liste actuellement en cours de réorganisation. */
    private OrderingType currentOrderingType;

    // Titre de l'écran
    private String selectedEntityLabel;

    /** Statut de visibilité demandé avant confirmation (pour le dialog) */
    private Boolean requestedVisibilityStatus;

    /** Action proposition demandée : true = publier (PUBLIQUE), false = refuser (REFUSE) */
    private Boolean requestedPropositionAction;


    @PostConstruct
    public void initialization() {
        checkSessionExpiration();
        loadLanguages();
        loadAllCollections();
    }

    /**
     * Retourne le TreeBean (résolution paresseuse via Provider pour éviter la dépendance circulaire).
     */
    public TreeBean getTreeBean() {
        return treeBeanProvider != null ? treeBeanProvider.get() : null;
    }

    /**
     * Appelé en preRenderView en premier. Si l'URL contient un code d'entité (/DECOCER),
     * charge la collection, initialise l'arbre et affiche l'élément correspondant.
     */
    public void handleEntityCodeFromUrlIfPresent() {
        jakarta.faces.context.FacesContext fc = jakarta.faces.context.FacesContext.getCurrentInstance();
        if (fc == null || fc.isPostback()) return;
        Object codeAttr = fc.getExternalContext().getRequestMap().get("entityCodeFromUrl");
        String code = codeAttr instanceof String s ? s : null;
        if (code == null || code.isBlank()) return;

        Entity entity = entityRepository.findByCode(code).orElse(null);
        if (entity == null) return;
        if (!isEntityVisibleForCurrentUser(entity)) {
            log.debug("Entité {} non visible pour l'utilisateur actuel, redirection vers la racine", code);
            try {
                String ctx = fc.getExternalContext().getRequestContextPath();
                fc.getExternalContext().redirect(ctx + "/");
                fc.responseComplete();
            } catch (java.io.IOException e) {
                log.warn("Redirection impossible vers / : {}", e.getMessage());
            }
            return;
        }

        Entity collection = findAncestorOfType(entity, EntityConstants.ENTITY_TYPE_COLLECTION);
        if (collection == null || collection.getId() == null) {
            log.warn("Aucune collection trouvée pour l'entité {}, redirection vers la racine", code);
            try {
                String ctx = fc.getExternalContext().getRequestContextPath();
                fc.getExternalContext().redirect(ctx + "/");
                fc.responseComplete();
            } catch (java.io.IOException e) {
                log.warn("Redirection impossible vers / : {}", e.getMessage());
            }
            return;
        }

        Entity refreshedCollection = entityRepository.findById(collection.getId()).orElse(collection);
        setSelectedEntity(refreshedCollection);
        getTreeBean().initializeTreeWithCollection();
        collectionBean.showCollectionDetail(this, refreshedCollection);
        String typeCode = entity.getEntityType() != null ? entity.getEntityType().getCode() : null;
        if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(typeCode)) {
            // already done by showCollectionDetail
        } else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(typeCode)) {
            showReferenceDetail(entity);
        } else if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(typeCode)) {
            showCategoryDetail(entity);
        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(typeCode)) {
            showGroupe(entity);
        } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(typeCode)) {
            showSerie(entity);
        } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(typeCode)) {
            showType(entity);
        }
        /* Garantir que l'arbre est visible lorsqu'on accède via URL (nouveau navigateur, etc.) */
        panelState.setShowTreePanel(true);
        fc.getExternalContext().getRequestMap().put("showTreeOnUrlLoad", Boolean.TRUE);
        fc.getExternalContext().getRequestMap().remove("entityCodeFromUrl");
    }

    /**
     * Appelé en preRenderView (requête GET / rafraîchissement). Resynchronise l'arbre avec selectedEntity
     * pour préserver le mode édition et la sélection après un rafraîchissement manuel.
     */
    public void ensureStateConsistencyOnRefresh() {
        jakarta.faces.context.FacesContext fc = jakarta.faces.context.FacesContext.getCurrentInstance();
        if (fc == null || fc.isPostback() || selectedEntity == null) return;
        TreeBean tb = getTreeBean();
        if (tb != null) tb.expandPathAndSelectEntity(selectedEntity);
    }

    /**
     * Retourne le CandidatBean (résolution paresseuse via Provider pour éviter la dépendance circulaire).
     */
    public CandidatBean getCandidatBean() {
        return candidatBeanProvider != null ? candidatBeanProvider.get() : null;
    }

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
     * Indique si une entité est visible pour l'utilisateur actuel, selon les règles métier :
     * - Jamais d'entités avec statut REFUSED.
     * - Utilisateur non connecté : uniquement les entités avec le champ publique = true.
     * - Utilisateur connecté : entités avec publique = true ou false selon les règles ci-dessous :
     *   - Collections publiques : toutes les entités rattachées sont visibles.
     *   - Administrateur technique : toutes les entités sauf REFUSED.
     *   - Autres groupes : collections autorisées via user_permission et entités rattachées.
     */
    public boolean isEntityVisibleForCurrentUser(Entity entity) {
        if (entity == null) {
            return false;
        }

        // 0) Administrateur technique : voit tous les éléments (y compris REFUSED)
        if (loginBean.isAdminTechniqueOrFonctionnel()) {
            return true;
        }

        // 1) Ne jamais afficher les entités au statut REFUSED (pour les non-admin)
        if (EntityStatusEnum.REFUSE.name().equals(entity.getStatut())) {
            return false;
        }

        // 2) Utilisateur non connecté (offline) : uniquement les entités statut = PUBLIQUE
        //    et statut différent de REFUSED et PROPOSITION
        if (!loginBean.isAuthenticated()) {
            return entity.getStatut() != null && EntityStatusEnum.PUBLIQUE.name().equals(entity.getStatut());
        }

        // 2b) Utilisateur connecté : pour les collections, afficher toutes
        if (entity.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_COLLECTION.equals(entity.getEntityType().getCode())) {
            return true;
        }

        // 3) Utilisateur connecté : règle des collections publiques — toutes les collections
        //    avec statut PUBLIQUE et tous leurs éléments sont visibles.
        Entity collectionAncestorForPublicRule = null;
        if (entity.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_COLLECTION.equals(entity.getEntityType().getCode())) {
            collectionAncestorForPublicRule = entity;
        } else {
            collectionAncestorForPublicRule = findAncestorOfType(entity, EntityConstants.ENTITY_TYPE_COLLECTION);
        }
        if (collectionAncestorForPublicRule != null
                && EntityStatusEnum.PUBLIQUE.name().equals(collectionAncestorForPublicRule.getStatut())) {
            return true;
        }

        // 4) Autres groupes connectés : filtrer par collections autorisées
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
     * Vérifie si l'utilisateur connecté est Gestionnaire de référentiel pour l'entité référence donnée.
     */
    private boolean isCurrentUserGestionnaireReferentielFor(Entity reference) {
        if (loginBean == null || !loginBean.isAuthenticated() || loginBean.getCurrentUser() == null
                || reference == null || reference.getId() == null) {
            return false;
        }
        return userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                loginBean.getCurrentUser().getId(),
                reference.getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel());
    }

    /**
     * Vérifie si l'utilisateur connecté a une permission (n'importe quel rôle) sur le groupe.
     */
    private boolean isCurrentUserHasAnyRoleOnGroup(Entity group) {
        if (loginBean == null || !loginBean.isAuthenticated() || loginBean.getCurrentUser() == null
                || group == null || group.getId() == null) {
            return false;
        }
        return userPermissionRepository.existsByUserIdAndEntityId(
                loginBean.getCurrentUser().getId(),
                group.getId());
    }

    /**
     * Indique si une entité est visible dans une liste de catalogue (références, catégories, groupes, séries, types).
     * Règles :
     * - Mode offline (non connecté) : publique = true ET statut différent de REFUSED et PROPOSITION
     * - Mode connecté :
     *   - Références : statut != REFUSED, PROPOSITION ; OU (statut = PROPOSITION et user est Gestionnaire de référentiel)
     *   - Groupes : statut != REFUSED, PROPOSITION ; OU (statut = PROPOSITION et (user est admin réf de la référence OU user a un rôle sur le groupe))
     *   - Catégories, séries, types : statut != REFUSED, PROPOSITION ; OU (statut = PROPOSITION et accès via référence/group)
     */
    public boolean isEntityVisibleInCatalogList(Entity entity, String entityTypeCode) {
        if (entity == null) {
            return false;
        }

        // 0) Administrateur technique : voit tous les éléments (y compris REFUSED, PROPOSITION)
        boolean isAdminTechnique = loginBean != null && loginBean.isAdminTechniqueOrFonctionnel();
        if (isAdminTechnique) {
            return true;
        }

        String statut = entity.getStatut();
        boolean isRefused = EntityStatusEnum.REFUSE.name().equals(statut);
        boolean isProposition = EntityStatusEnum.PROPOSITION.name().equals(statut);
        boolean authenticated = loginBean != null && loginBean.isAuthenticated();

        // 1) Jamais afficher REFUSED (pour les non-admin)
        if (isRefused) {
            return false;
        }

        // 2) Mode offline : statut = PUBLIQUE ET différent de PROPOSITION
        if (!authenticated) {
            return EntityStatusEnum.PUBLIQUE.name().equals(statut) && !isProposition;
        }

        // 3) Connecté : statut != PROPOSITION -> visible si isEntityVisibleForCurrentUser
        if (!isProposition) {
            return isEntityVisibleForCurrentUser(entity);
        }

        // 4) Statut = PROPOSITION : règles spécifiques par type
        Entity referenceAncestor = findAncestorOfType(entity, EntityConstants.ENTITY_TYPE_REFERENCE);
        Entity groupAncestor = findAncestorOfType(entity, EntityConstants.ENTITY_TYPE_GROUP);

        if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entityTypeCode)) {
            return isCurrentUserGestionnaireReferentielFor(entity);
        }
        if (EntityConstants.ENTITY_TYPE_GROUP.equals(entityTypeCode)) {
            boolean isAdminRefOfReference = referenceAncestor != null
                    && isCurrentUserGestionnaireReferentielFor(referenceAncestor);
            boolean hasRoleOnGroup = isCurrentUserHasAnyRoleOnGroup(entity);
            return isAdminRefOfReference || hasRoleOnGroup;
        }
        if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(entityTypeCode)) {
            return referenceAncestor != null && isCurrentUserGestionnaireReferentielFor(referenceAncestor);
        }
        if (EntityConstants.ENTITY_TYPE_SERIES.equals(entityTypeCode) || "SERIE".equals(entityTypeCode)) {
            boolean isAdminRefOfReference = referenceAncestor != null
                    && isCurrentUserGestionnaireReferentielFor(referenceAncestor);
            boolean hasRoleOnGroup = groupAncestor != null && isCurrentUserHasAnyRoleOnGroup(groupAncestor);
            return isAdminRefOfReference || hasRoleOnGroup;
        }
        if (EntityConstants.ENTITY_TYPE_TYPE.equals(entityTypeCode)) {
            boolean isAdminRefOfReference = referenceAncestor != null
                    && isCurrentUserGestionnaireReferentielFor(referenceAncestor);
            boolean hasRoleOnGroup = groupAncestor != null && isCurrentUserHasAnyRoleOnGroup(groupAncestor);
            return isAdminRefOfReference || hasRoleOnGroup;
        }

        return isEntityVisibleForCurrentUser(entity);
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
                referencesCurrentPage = 1;
                referencesSearchQuery = "";
                referencesViewAll = false;
                referencesSortMode = hasCustomReferencesOrder() ? ListSortMode.MANUAL : ListSortMode.ALPHA_ASC;
                break;
            case EntityConstants.ENTITY_TYPE_REFERENCE:
                childs = categoryService.loadCategoriesByReference(selectedEntity);
                categoriesCurrentPage = 1;
                categoriesSearchQuery = "";
                categoriesViewAll = false;
                categoriesSortMode = categoryService.hasCustomCategoriesOrder(selectedEntity) ? ListSortMode.MANUAL : ListSortMode.ALPHA_ASC;
                break;
            case EntityConstants.ENTITY_TYPE_CATEGORY:
                childs = groupService.loadCategoryGroups(selectedEntity);
                groupesCurrentPage = 1;
                groupesSearchQuery = "";
                groupesViewAll = false;
                groupesSortMode = groupService.hasCustomGroupesOrder(selectedEntity) ? ListSortMode.MANUAL : ListSortMode.ALPHA_ASC;
                break;
            case EntityConstants.ENTITY_TYPE_GROUP:
                List<Entity> series = serieService.loadGroupSeries(selectedEntity);
                List<Entity> types = typeService.loadGroupTypes(selectedEntity);
                childs = new ArrayList<>();
                if (series != null) childs.addAll(series);
                if (types != null) childs.addAll(types);
                seriesCurrentPage = 1;
                seriesSearchQuery = "";
                seriesViewAll = false;
                typesCurrentPage = 1;
                typesSearchQuery = "";
                seriesSortMode = serieService.hasCustomSeriesOrder(selectedEntity) ? ListSortMode.MANUAL : ListSortMode.ALPHA_ASC;
                typesSortMode = typeService.hasCustomTypesOrder(selectedEntity) ? ListSortMode.MANUAL : ListSortMode.ALPHA_ASC;
                break;
            case EntityConstants.ENTITY_TYPE_SERIES:
                childs = typeService.loadSerieTypes(selectedEntity);
                typesCurrentPage = 1;
                typesSearchQuery = "";
                typesViewAll = false;
                typesSortMode = typeService.hasCustomTypesOrder(selectedEntity) ? ListSortMode.MANUAL : ListSortMode.ALPHA_ASC;
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

    /** Affichage date de création (entité courante), mis en cache par requête. */
    public String getFormattedSelectedEntityCreateDate() {
        if (selectedEntity == null || selectedEntity.getCreateDate() == null) {
            return "";
        }
        Long id = selectedEntity.getId();
        String cacheKey = "opentypo_fmtCreate_" + id;
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null) {
            Map<String, Object> rm = fc.getExternalContext().getRequestMap();
            if (rm.containsKey(cacheKey)) {
                return (String) rm.get(cacheKey);
            }
        }
        String v = selectedEntity.getCreateDate().format(ENTITY_AUDIT_DATETIME_FORMAT);
        if (fc != null) {
            fc.getExternalContext().getRequestMap().put(cacheKey, v);
        }
        return v;
    }

    /** Dernière modification (Envers / revinfo), mis en cache par requête. */
    public String getFormattedSelectedEntityLastModificationDate() {
        if (selectedEntity == null || selectedEntity.getId() == null) {
            return "";
        }
        Long id = selectedEntity.getId();
        String cacheKey = "opentypo_fmtLastMod_" + id;
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null) {
            Map<String, Object> rm = fc.getExternalContext().getRequestMap();
            if (rm.containsKey(cacheKey)) {
                return (String) rm.get(cacheKey);
            }
        }
        String v = auditService.findLastModificationDate(id)
                .map(dt -> dt.format(ENTITY_AUDIT_DATETIME_FORMAT))
                .orElse("—");
        if (fc != null) {
            fc.getExternalContext().getRequestMap().put(cacheKey, v);
        }
        return v;
    }

    /** Enfants de type Référentiel (filtre sur childs + visibilité catalogue). */
    public List<Entity> getChildsReferences() {
        return filterChildsByTypeWithVisibility(EntityConstants.ENTITY_TYPE_REFERENCE);
    }
    /** Enfants de type Catégorie (filtre sur childs + visibilité catalogue). */
    public List<Entity> getChildsCategories() {
        return filterChildsByTypeWithVisibility(EntityConstants.ENTITY_TYPE_CATEGORY);
    }
    /** Enfants de type Groupe (filtre sur childs + visibilité catalogue). */
    public List<Entity> getChildsGroupes() {
        return filterChildsByTypeWithVisibility(EntityConstants.ENTITY_TYPE_GROUP);
    }
    /** Enfants de type Série (filtre sur childs + visibilité catalogue). */
    public List<Entity> getChildsSeries() {
        return filterChildsByTypeWithVisibility(EntityConstants.ENTITY_TYPE_SERIES);
    }
    /** Enfants de type Type (filtre sur childs + visibilité catalogue). */
    public List<Entity> getChildsTypes() {
        return filterChildsByTypeWithVisibility(EntityConstants.ENTITY_TYPE_TYPE);
    }

    private List<Entity> filterChildsByTypeWithVisibility(String entityTypeCode) {
        if (childs == null) return new ArrayList<>();
        var filtered = childs.stream()
                .filter(e -> e != null && e.getEntityType() != null
                        && entityTypeCode.equals(e.getEntityType().getCode())
                        && isEntityVisibleInCatalogList(e, entityTypeCode))
                .collect(Collectors.toList());
        // Ordre personnalisé (display_order) ou alphabétique — déjà appliqué par les services
        if (EntityConstants.ENTITY_TYPE_TYPE.equals(entityTypeCode)
                || EntityConstants.ENTITY_TYPE_SERIES.equals(entityTypeCode)
                || EntityConstants.ENTITY_TYPE_GROUP.equals(entityTypeCode)
                || EntityConstants.ENTITY_TYPE_CATEGORY.equals(entityTypeCode)
                || EntityConstants.ENTITY_TYPE_REFERENCE.equals(entityTypeCode)) {
            return filtered;
        }
        return filtered.stream()
                .sorted(Comparator.comparing(e -> e.getCode() != null ? e.getCode() : "", String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /** Taille des listes filtrées (évite fn:length en EL). */
    public int getFilteredReferencesSize() { return getFilteredReferences().size(); }
    public int getFilteredCategoriesSize() { return getFilteredCategories().size(); }
    public int getFilteredGroupesSize() { return getFilteredGroupes().size(); }
    public int getFilteredSeriesSize() { return getFilteredSeries().size(); }
    public int getFilteredTypesSize() { return getFilteredTypes().size(); }
    /** Nombre total d'éléments (sans filtre de recherche). */
    public int getChildsReferencesSize() { return getChildsReferences().size(); }
    public int getChildsCategoriesSize() { return getChildsCategories().size(); }
    public int getChildsGroupesSize() { return getChildsGroupes().size(); }
    public int getChildsSeriesSize() { return getChildsSeries().size(); }
    public int getChildsTypesSize() { return getChildsTypes().size(); }

    /** Référentiels filtrés par la recherche. */
    public List<Entity> getFilteredReferences() {
        List<Entity> list = getChildsReferences();
        if (list == null) return new ArrayList<>();
        String q = referencesSearchQuery != null ? referencesSearchQuery.trim().toLowerCase() : "";
        if (q.isEmpty()) return new ArrayList<>(list);
        return list.stream()
                .filter(e -> {
                    String code = e.getCode() != null ? e.getCode().toLowerCase() : "";
                    String label = getEntityLabel(e) != null ? getEntityLabel(e).toLowerCase() : "";
                    String desc = getEntityDescriptionPlainText(e) != null ? getEntityDescriptionPlainText(e).toLowerCase() : "";
                    return code.contains(q) || label.contains(q) || desc.contains(q);
                })
                .collect(Collectors.toList());
    }

    /** Vrai si la collection sélectionnée a des référentiels avec un ordre personnalisé (display_order dans entity_relation). */
    public boolean hasCustomReferencesOrder() {
        Entity collection = getSelectedCollection();
        return collection != null && referenceService.hasCustomReferencesOrder(collection);
    }

    /** Référentiels filtrés puis triés selon le mode (manuel, A-Z, Z-A). */
    public List<Entity> getSortedFilteredReferences() {
        List<Entity> filtered = getFilteredReferences();
        if (filtered.isEmpty()) return new ArrayList<>();
        if (referencesSortMode == ListSortMode.MANUAL) {
            /* L'ordre manuel vient de entity_relation.display_order ; childs est déjà ordonné par loadReferencesByCollection. */
            List<Entity> childsRefs = getChildsReferences();
            java.util.Map<Long, Integer> orderMap = new java.util.HashMap<>();
            for (int i = 0; i < childsRefs.size(); i++) {
                Entity e = childsRefs.get(i);
                if (e != null && e.getId() != null) orderMap.put(e.getId(), i);
            }
            return filtered.stream()
                    .sorted(Comparator.comparingInt(r -> orderMap.getOrDefault(r.getId(), Integer.MAX_VALUE)))
                    .collect(Collectors.toList());
        }
        return filtered.stream()
                .sorted((r1, r2) -> {
                    String n1 = getEntityLabel(r1) != null ? getEntityLabel(r1) : "";
                    String n2 = getEntityLabel(r2) != null ? getEntityLabel(r2) : "";
                    return referencesSortMode == ListSortMode.ALPHA_ASC
                            ? n1.compareToIgnoreCase(n2)
                            : n2.compareToIgnoreCase(n1);
                })
                .collect(Collectors.toList());
    }

    /** Référentiels pour la page courante ou tous si referencesViewAll. */
    public List<Entity> getPaginatedReferences() {
        List<Entity> filtered = getSortedFilteredReferences();
        if (filtered.isEmpty()) return new ArrayList<>();
        if (referencesViewAll) return new ArrayList<>(filtered);
        int from = (referencesCurrentPage - 1) * REFERENCES_PAGE_SIZE;
        if (from >= filtered.size()) {
            referencesCurrentPage = 1;
            from = 0;
        }
        int to = Math.min(from + REFERENCES_PAGE_SIZE, filtered.size());
        return filtered.subList(from, to);
    }

    public int getReferencesTotalPages() {
        int size = getSortedFilteredReferences().size();
        return size == 0 ? 0 : (int) Math.ceil((double) size / REFERENCES_PAGE_SIZE);
    }

    public void referencesGoToPage(int page) {
        int total = getReferencesTotalPages();
        if (page >= 1 && page <= total) referencesCurrentPage = page;
    }

    public void referencesNextPage() { referencesGoToPage(referencesCurrentPage + 1); }
    public void referencesPreviousPage() { referencesGoToPage(referencesCurrentPage - 1); }
    public boolean isReferencesFirstPage() { return referencesCurrentPage <= 1; }
    public boolean isReferencesLastPage() { return referencesCurrentPage >= getReferencesTotalPages(); }
    public int getReferencesCurrentPage() { return referencesCurrentPage; }
    public String getReferencesSearchQuery() { return referencesSearchQuery; }
    public void setReferencesSearchQuery(String referencesSearchQuery) {
        this.referencesSearchQuery = referencesSearchQuery != null ? referencesSearchQuery : "";
        this.referencesCurrentPage = 1;
    }

    public boolean isReferencesViewAll() { return referencesViewAll; }
    public void setReferencesViewAll(boolean referencesViewAll) { this.referencesViewAll = referencesViewAll; }
    public String getReferencesViewAllToggleTitle() {
        return referencesViewAll ? "Revenir à la pagination" : "Afficher tous les référentiels en une seule page";
    }
    public void toggleReferencesViewAll() {
        referencesViewAll = !referencesViewAll;
        if (!referencesViewAll) referencesCurrentPage = 1;
    }

    public ListSortMode getReferencesSortMode() { return referencesSortMode; }
    public void setReferencesSortMode(ListSortMode mode) {
        if (mode != null) this.referencesSortMode = mode;
    }
    public boolean isReferencesSortModeManual() { return referencesSortMode == ListSortMode.MANUAL; }
    public boolean isReferencesSortModeAlphaAsc() { return referencesSortMode == ListSortMode.ALPHA_ASC; }
    public boolean isReferencesSortModeAlphaDesc() { return referencesSortMode == ListSortMode.ALPHA_DESC; }
    public void setReferencesSortModeManual() { setReferencesSortMode(ListSortMode.MANUAL); }
    public void setReferencesSortModeAlphaAsc() { setReferencesSortMode(ListSortMode.ALPHA_ASC); }
    public void setReferencesSortModeAlphaDesc() { setReferencesSortMode(ListSortMode.ALPHA_DESC); }

    /**
     * API générique pour le composant sort-selector.
     * listType: collections, references, categories, groupes, series, types
     * mode: MANUAL, ALPHA_ASC, ALPHA_DESC
     */
    public void setSortMode(String listType, String mode) {
        if (listType == null || mode == null) return;
        try {
            ListSortMode m = ListSortMode.valueOf(mode);
            switch (listType) {
                case "collections" -> setCollectionsSortMode(m);
                case "references" -> setReferencesSortMode(m);
                case "categories" -> categoriesSortMode = m;
                case "groupes" -> groupesSortMode = m;
                case "series" -> seriesSortMode = m;
                case "types" -> typesSortMode = m;
                default -> { }
            }
        } catch (IllegalArgumentException ignored) { }
    }

    public boolean hasCustomOrder(String listType) {
        if (listType == null) return false;
        return switch (listType) {
            case "collections" -> hasCustomCollectionsOrder();
            case "references" -> hasCustomReferencesOrder();
            case "categories" -> hasCustomCategoriesOrder();
            case "groupes" -> hasCustomGroupesOrder();
            case "series" -> hasCustomSeriesOrder();
            case "types" -> hasCustomTypesOrder();
            default -> false;
        };
    }

    public boolean isSortMode(String listType, String mode) {
        if (listType == null || mode == null) return false;
        try {
            return switch (listType) {
                case "collections" -> collectionsSortMode.name().equals(mode);
                case "references" -> referencesSortMode.name().equals(mode);
                case "categories" -> categoriesSortMode.name().equals(mode);
                case "groupes" -> groupesSortMode.name().equals(mode);
                case "series" -> seriesSortMode.name().equals(mode);
                case "types" -> typesSortMode.name().equals(mode);
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /** Tri une liste filtrée selon le mode (manuel = ordre des childs, alpha asc/desc). */
    private List<Entity> sortFilteredList(List<Entity> filtered, List<Entity> orderedChilds, ListSortMode mode) {
        if (filtered == null || filtered.isEmpty()) return new ArrayList<>();
        if (mode == ListSortMode.MANUAL && orderedChilds != null && !orderedChilds.isEmpty()) {
            java.util.Map<Long, Integer> orderMap = new java.util.HashMap<>();
            for (int i = 0; i < orderedChilds.size(); i++) {
                Entity e = orderedChilds.get(i);
                if (e != null && e.getId() != null) orderMap.put(e.getId(), i);
            }
            return filtered.stream()
                    .sorted(Comparator.comparingInt(r -> orderMap.getOrDefault(r.getId(), Integer.MAX_VALUE)))
                    .collect(Collectors.toList());
        }
        return filtered.stream()
                .sorted((r1, r2) -> {
                    String n1 = getEntityLabel(r1) != null ? getEntityLabel(r1) : "";
                    String n2 = getEntityLabel(r2) != null ? getEntityLabel(r2) : "";
                    return mode == ListSortMode.ALPHA_ASC ? n1.compareToIgnoreCase(n2) : n2.compareToIgnoreCase(n1);
                })
                .collect(Collectors.toList());
    }

    /** Catégories filtrées par la recherche. */
    public List<Entity> getFilteredCategories() {
        List<Entity> list = getChildsCategories();
        if (list == null) return new ArrayList<>();
        String q = categoriesSearchQuery != null ? categoriesSearchQuery.trim().toLowerCase() : "";
        if (q.isEmpty()) return new ArrayList<>(list);
        return list.stream()
                .filter(e -> {
                    String code = e.getCode() != null ? e.getCode().toLowerCase() : "";
                    String label = getEntityLabel(e) != null ? getEntityLabel(e).toLowerCase() : "";
                    return code.contains(q) || label.contains(q);
                })
                .collect(Collectors.toList());
    }

    public boolean hasCustomCategoriesOrder() {
        Entity ref = getSelectedReference();
        return ref != null && categoryService.hasCustomCategoriesOrder(ref);
    }

    public List<Entity> getSortedFilteredCategories() {
        return sortFilteredList(getFilteredCategories(), getChildsCategories(), categoriesSortMode);
    }

    /** Catégories pour la page courante ou toutes si categoriesViewAll. */
    public List<Entity> getPaginatedCategories() {
        List<Entity> filtered = getSortedFilteredCategories();
        if (filtered.isEmpty()) return new ArrayList<>();
        if (categoriesViewAll) return new ArrayList<>(filtered);
        int from = (categoriesCurrentPage - 1) * CATEGORIES_PAGE_SIZE;
        if (from >= filtered.size()) {
            categoriesCurrentPage = 1;
            from = 0;
        }
        int to = Math.min(from + CATEGORIES_PAGE_SIZE, filtered.size());
        return filtered.subList(from, to);
    }

    public boolean isCategoriesViewAll() { return categoriesViewAll; }
    public void setCategoriesViewAll(boolean categoriesViewAll) { this.categoriesViewAll = categoriesViewAll; }
    public void toggleCategoriesViewAll() {
        categoriesViewAll = !categoriesViewAll;
        if (!categoriesViewAll) categoriesCurrentPage = 1;
    }

    public int getCategoriesTotalPages() {
        int size = getSortedFilteredCategories().size();
        return size == 0 ? 0 : (int) Math.ceil((double) size / CATEGORIES_PAGE_SIZE);
    }

    public void categoriesGoToPage(int page) {
        int total = getCategoriesTotalPages();
        if (page >= 1 && page <= total) categoriesCurrentPage = page;
    }

    public void categoriesNextPage() { categoriesGoToPage(categoriesCurrentPage + 1); }
    public void categoriesPreviousPage() { categoriesGoToPage(categoriesCurrentPage - 1); }
    public boolean isCategoriesFirstPage() { return categoriesCurrentPage <= 1; }
    public boolean isCategoriesLastPage() { return categoriesCurrentPage >= getCategoriesTotalPages(); }
    public int getCategoriesCurrentPage() { return categoriesCurrentPage; }
    public String getCategoriesSearchQuery() { return categoriesSearchQuery; }
    public void setCategoriesSearchQuery(String categoriesSearchQuery) {
        this.categoriesSearchQuery = categoriesSearchQuery != null ? categoriesSearchQuery : "";
        this.categoriesCurrentPage = 1;
    }

    public boolean isCategoriesSortModeManual() { return categoriesSortMode == ListSortMode.MANUAL; }
    public boolean isCategoriesSortModeAlphaAsc() { return categoriesSortMode == ListSortMode.ALPHA_ASC; }
    public boolean isCategoriesSortModeAlphaDesc() { return categoriesSortMode == ListSortMode.ALPHA_DESC; }
    public void setCategoriesSortModeManual() { categoriesSortMode = ListSortMode.MANUAL; }
    public void setCategoriesSortModeAlphaAsc() { categoriesSortMode = ListSortMode.ALPHA_ASC; }
    public void setCategoriesSortModeAlphaDesc() { categoriesSortMode = ListSortMode.ALPHA_DESC; }

    /** Groupes filtrés par la recherche. */
    public List<Entity> getFilteredGroupes() {
        List<Entity> list = getChildsGroupes();
        if (list == null) return new ArrayList<>();
        String q = groupesSearchQuery != null ? groupesSearchQuery.trim().toLowerCase() : "";
        if (q.isEmpty()) return new ArrayList<>(list);
        return list.stream()
                .filter(e -> {
                    String code = e.getCode() != null ? e.getCode().toLowerCase() : "";
                    String nom = e.getNom() != null ? e.getNom().toLowerCase() : "";
                    String label = getEntityLabel(e) != null ? getEntityLabel(e).toLowerCase() : "";
                    return code.contains(q) || nom.contains(q) || label.contains(q);
                })
                .collect(Collectors.toList());
    }

    public boolean hasCustomGroupesOrder() {
        Entity cat = getSelectedCategory();
        return cat != null && groupService.hasCustomGroupesOrder(cat);
    }

    public List<Entity> getSortedFilteredGroupes() {
        return sortFilteredList(getFilteredGroupes(), getChildsGroupes(), groupesSortMode);
    }

    /** Groupes pour la page courante ou tous si groupesViewAll. */
    public List<Entity> getPaginatedGroupes() {
        List<Entity> filtered = getSortedFilteredGroupes();
        if (filtered.isEmpty()) return new ArrayList<>();
        if (groupesViewAll) return new ArrayList<>(filtered);
        int from = (groupesCurrentPage - 1) * GROUPES_PAGE_SIZE;
        if (from >= filtered.size()) {
            groupesCurrentPage = 1;
            from = 0;
        }
        int to = Math.min(from + GROUPES_PAGE_SIZE, filtered.size());
        return filtered.subList(from, to);
    }

    public boolean isGroupesViewAll() { return groupesViewAll; }
    public void setGroupesViewAll(boolean groupesViewAll) { this.groupesViewAll = groupesViewAll; }
    public String getGroupesViewAllToggleTitle() {
        return groupesViewAll ? "Revenir à la pagination" : "Afficher tous les groupes en une seule page";
    }
    public void toggleGroupesViewAll() {
        groupesViewAll = !groupesViewAll;
        if (!groupesViewAll) groupesCurrentPage = 1;
    }

    public int getGroupesTotalPages() {
        int size = getSortedFilteredGroupes().size();
        return size == 0 ? 0 : (int) Math.ceil((double) size / GROUPES_PAGE_SIZE);
    }

    public void groupesGoToPage(int page) {
        int total = getGroupesTotalPages();
        if (page >= 1 && page <= total) groupesCurrentPage = page;
    }

    public void groupesNextPage() { groupesGoToPage(groupesCurrentPage + 1); }
    public void groupesPreviousPage() { groupesGoToPage(groupesCurrentPage - 1); }
    public boolean isGroupesFirstPage() { return groupesCurrentPage <= 1; }
    public boolean isGroupesLastPage() { return groupesCurrentPage >= getGroupesTotalPages(); }
    public int getGroupesCurrentPage() { return groupesCurrentPage; }
    public String getGroupesSearchQuery() { return groupesSearchQuery; }
    public void setGroupesSearchQuery(String groupesSearchQuery) {
        this.groupesSearchQuery = groupesSearchQuery != null ? groupesSearchQuery : "";
        this.groupesCurrentPage = 1;
    }

    public boolean isGroupesSortModeManual() { return groupesSortMode == ListSortMode.MANUAL; }
    public boolean isGroupesSortModeAlphaAsc() { return groupesSortMode == ListSortMode.ALPHA_ASC; }
    public boolean isGroupesSortModeAlphaDesc() { return groupesSortMode == ListSortMode.ALPHA_DESC; }
    public void setGroupesSortModeManual() { groupesSortMode = ListSortMode.MANUAL; }
    public void setGroupesSortModeAlphaAsc() { groupesSortMode = ListSortMode.ALPHA_ASC; }
    public void setGroupesSortModeAlphaDesc() { groupesSortMode = ListSortMode.ALPHA_DESC; }

    /** Séries filtrées par la recherche. */
    public List<Entity> getFilteredSeries() {
        List<Entity> list = getChildsSeries();
        if (list == null) return new ArrayList<>();
        String q = seriesSearchQuery != null ? seriesSearchQuery.trim().toLowerCase() : "";
        if (q.isEmpty()) return new ArrayList<>(list);
        return list.stream()
                .filter(e -> {
                    String code = e.getCode() != null ? e.getCode().toLowerCase() : "";
                    String nom = e.getNom() != null ? e.getNom().toLowerCase() : "";
                    String label = getEntityLabel(e) != null ? getEntityLabel(e).toLowerCase() : "";
                    return code.contains(q) || nom.contains(q) || label.contains(q);
                })
                .collect(Collectors.toList());
    }

    public boolean hasCustomSeriesOrder() {
        Entity group = getSelectedGroup();
        return group != null && serieService.hasCustomSeriesOrder(group);
    }

    public List<Entity> getSortedFilteredSeries() {
        return sortFilteredList(getFilteredSeries(), getChildsSeries(), seriesSortMode);
    }

    /** Séries pour la page courante (6 par page). */
    public List<Entity> getPaginatedSeries() {
        List<Entity> filtered = getSortedFilteredSeries();
        if (filtered.isEmpty()) return new ArrayList<>();
        int from = (seriesCurrentPage - 1) * SERIES_PAGE_SIZE;
        if (from >= filtered.size()) {
            seriesCurrentPage = 1;
            from = 0;
        }
        int to = Math.min(from + SERIES_PAGE_SIZE, filtered.size());
        return filtered.subList(from, to);
    }

    public int getSeriesTotalPages() {
        int size = getSortedFilteredSeries().size();
        return size == 0 ? 0 : (int) Math.ceil((double) size / SERIES_PAGE_SIZE);
    }

    public void seriesGoToPage(int page) {
        int total = getSeriesTotalPages();
        if (page >= 1 && page <= total) seriesCurrentPage = page;
    }

    public void seriesNextPage() { seriesGoToPage(seriesCurrentPage + 1); }
    public void seriesPreviousPage() { seriesGoToPage(seriesCurrentPage - 1); }
    public boolean isSeriesFirstPage() { return seriesCurrentPage <= 1; }
    public boolean isSeriesLastPage() { return seriesCurrentPage >= getSeriesTotalPages(); }
    public int getSeriesCurrentPage() { return seriesCurrentPage; }
    public String getSeriesSearchQuery() { return seriesSearchQuery; }
    public void setSeriesSearchQuery(String seriesSearchQuery) {
        this.seriesSearchQuery = seriesSearchQuery != null ? seriesSearchQuery : "";
        this.seriesCurrentPage = 1;
    }

    public boolean isSeriesSortModeManual() { return seriesSortMode == ListSortMode.MANUAL; }
    public boolean isSeriesSortModeAlphaAsc() { return seriesSortMode == ListSortMode.ALPHA_ASC; }
    public boolean isSeriesSortModeAlphaDesc() { return seriesSortMode == ListSortMode.ALPHA_DESC; }
    public void setSeriesSortModeManual() { seriesSortMode = ListSortMode.MANUAL; }
    public void setSeriesSortModeAlphaAsc() { seriesSortMode = ListSortMode.ALPHA_ASC; }
    public void setSeriesSortModeAlphaDesc() { seriesSortMode = ListSortMode.ALPHA_DESC; }

    /**
     * Séries avec leurs types, filtrées par recherche (pour le panneau Groupe).
     * Chaque série contient la liste de ses types, triés selon typesSortMode (personnalisé, A→Z, Z→A).
     */
    public List<SerieWithTypes> getSeriesWithTypesFiltered() {
        List<Entity> filtered = getSortedFilteredSeries();
        if (filtered == null || filtered.isEmpty()) return new ArrayList<>();
        List<SerieWithTypes> result = new ArrayList<>();
        for (Entity serie : filtered) {
            List<Entity> types = typeService.loadSerieTypes(serie);
            if (types == null) types = new ArrayList<>();
            types = sortTypesByMode(types, serie);
            result.add(new SerieWithTypes(serie, types));
        }
        return result;
    }

    /** Tri des types selon typesSortMode (pour séries et liste directe). */
    private List<Entity> sortTypesByMode(List<Entity> types, Entity parent) {
        if (types == null || types.isEmpty()) return new ArrayList<>(types);
        if (typesSortMode == ListSortMode.MANUAL && parent != null && typeService.hasCustomTypesOrder(parent)) {
            return new ArrayList<>(types); // loadSerieTypes/loadGroupTypes déjà ordonné par display_order
        }
        return types.stream()
                .sorted((t1, t2) -> {
                    String n1 = getEntityLabel(t1) != null ? getEntityLabel(t1) : (t1.getCode() != null ? t1.getCode() : "");
                    String n2 = getEntityLabel(t2) != null ? getEntityLabel(t2) : (t2.getCode() != null ? t2.getCode() : "");
                    return typesSortMode == ListSortMode.ALPHA_ASC
                            ? n1.compareToIgnoreCase(n2)
                            : n2.compareToIgnoreCase(n1);
                })
                .collect(Collectors.toList());
    }

    /** Séries avec types pour la page courante (pagination) ou toutes si seriesViewAll. */
    public List<SerieWithTypes> getPaginatedSeriesWithTypes() {
        List<SerieWithTypes> all = getSeriesWithTypesFiltered();
        if (all.isEmpty()) return new ArrayList<>();
        if (seriesViewAll) return new ArrayList<>(all);
        int from = (seriesCurrentPage - 1) * SERIES_PAGE_SIZE;
        if (from >= all.size()) {
            seriesCurrentPage = 1;
            from = 0;
        }
        int to = Math.min(from + SERIES_PAGE_SIZE, all.size());
        return all.subList(from, to);
    }

    public boolean isSeriesViewAll() { return seriesViewAll; }
    public void setSeriesViewAll(boolean seriesViewAll) { this.seriesViewAll = seriesViewAll; }
    /** Bascule l'affichage tout / paginé et réinitialise la page. */
    public void toggleSeriesViewAll() {
        seriesViewAll = !seriesViewAll;
        if (!seriesViewAll) seriesCurrentPage = 1;
    }

    /** Types filtrés par la recherche. */
    public boolean hasCustomTypesOrder() {
        Entity parent = getSelectedGroup();
        if (parent == null) parent = getSelectedSerie();
        return parent != null && typeService.hasCustomTypesOrder(parent);
    }

    public List<Entity> getFilteredTypes() {
        List<Entity> list = getChildsTypes();
        if (list == null) return new ArrayList<>();
        String q = typesSearchQuery != null ? typesSearchQuery.trim().toLowerCase() : "";
        if (q.isEmpty()) return new ArrayList<>(list);
        return list.stream()
                .filter(e -> {
                    String code = e.getCode() != null ? e.getCode().toLowerCase() : "";
                    String nom = e.getNom() != null ? e.getNom().toLowerCase() : "";
                    String label = getEntityLabel(e) != null ? getEntityLabel(e).toLowerCase() : "";
                    return code.contains(q) || nom.contains(q) || label.contains(q);
                })
                .collect(Collectors.toList());
    }

    /**
     * Types directs du groupe filtrés par la recherche série (vue unifiée Séries & Types).
     * Permet d'afficher et filtrer les types rattachés directement au groupe dans la même grille.
     */
    public List<Entity> getFilteredDirectTypesForUnifiedView() {
        List<Entity> list = getChildsTypes();
        if (list == null || list.isEmpty()) return new ArrayList<>();
        String q = seriesSearchQuery != null ? seriesSearchQuery.trim().toLowerCase() : "";
        if (q.isEmpty()) return new ArrayList<>(list);
        return list.stream()
                .filter(e -> {
                    String code = e.getCode() != null ? e.getCode().toLowerCase() : "";
                    String nom = e.getNom() != null ? e.getNom().toLowerCase() : "";
                    String label = getEntityLabel(e) != null ? getEntityLabel(e).toLowerCase() : "";
                    return code.contains(q) || nom.contains(q) || label.contains(q);
                })
                .collect(Collectors.toList());
    }

    public List<Entity> getSortedFilteredTypes() {
        return sortFilteredList(getFilteredTypes(), getChildsTypes(), typesSortMode);
    }

    /** Types pour la page courante ou tous si typesViewAll. */
    public List<Entity> getPaginatedTypes() {
        List<Entity> filtered = getSortedFilteredTypes();
        if (filtered.isEmpty()) return new ArrayList<>();
        if (typesViewAll) return new ArrayList<>(filtered);
        int from = (typesCurrentPage - 1) * TYPES_PAGE_SIZE;
        if (from >= filtered.size()) {
            typesCurrentPage = 1;
            from = 0;
        }
        int to = Math.min(from + TYPES_PAGE_SIZE, filtered.size());
        return filtered.subList(from, to);
    }

    public boolean isTypesViewAll() { return typesViewAll; }
    public void setTypesViewAll(boolean typesViewAll) { this.typesViewAll = typesViewAll; }
    public void toggleTypesViewAll() {
        typesViewAll = !typesViewAll;
        if (!typesViewAll) typesCurrentPage = 1;
    }

    public int getTypesTotalPages() {
        int size = getSortedFilteredTypes().size();
        return size == 0 ? 0 : (int) Math.ceil((double) size / TYPES_PAGE_SIZE);
    }

    public boolean isTypesSortModeManual() { return typesSortMode == ListSortMode.MANUAL; }
    public boolean isTypesSortModeAlphaAsc() { return typesSortMode == ListSortMode.ALPHA_ASC; }
    public boolean isTypesSortModeAlphaDesc() { return typesSortMode == ListSortMode.ALPHA_DESC; }
    public void setTypesSortModeManual() { typesSortMode = ListSortMode.MANUAL; }
    public void setTypesSortModeAlphaAsc() { typesSortMode = ListSortMode.ALPHA_ASC; }
    public void setTypesSortModeAlphaDesc() { typesSortMode = ListSortMode.ALPHA_DESC; }

    public void typesGoToPage(int page) {
        int total = getTypesTotalPages();
        if (page >= 1 && page <= total) typesCurrentPage = page;
    }

    public void typesNextPage() { typesGoToPage(typesCurrentPage + 1); }
    public void typesPreviousPage() { typesGoToPage(typesCurrentPage - 1); }
    public boolean isTypesFirstPage() { return typesCurrentPage <= 1; }
    public boolean isTypesLastPage() { return typesCurrentPage >= getTypesTotalPages(); }
    public int getTypesCurrentPage() { return typesCurrentPage; }
    public String getTypesSearchQuery() { return typesSearchQuery; }
    public void setTypesSearchQuery(String typesSearchQuery) {
        this.typesSearchQuery = typesSearchQuery != null ? typesSearchQuery : "";
        this.typesCurrentPage = 1;
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
                String fallbackTitle = entityLabel != null ? entityLabel + " - " + index : "Image " + index;
                String legende = (img.getLegende() != null && !img.getLegende().trim().isEmpty())
                        ? img.getLegende().trim() : fallbackTitle;
                photos.add(new Photo(img.getUrl(), img.getUrl(), legende, legende));
                index++;
            }
        }
        return photos;
    }

    /**
     * Retourne le TYPE qui précède l'entité sélectionnée (même parent, ordre alphabétique croissant).
     * Retourne null si l'entité sélectionnée n'est pas un TYPE ou s'il n'y a pas de prédécesseur.
     */
    public Entity getPreviousType() {
        if (selectedEntity == null || selectedEntity.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_TYPE.equals(selectedEntity.getEntityType().getCode())) {
            return null;
        }
        Entity selectedSerie = getSelectedSerie();
        if (selectedSerie == null) return null;
        List<Entity> siblings = typeService.loadSerieTypes(selectedSerie);
        if (siblings == null || siblings.isEmpty()) return null;
        List<Entity> sorted = siblings.stream()
                .sorted(Comparator.comparing(e -> e.getNom() != null ? e.getNom().toLowerCase() : "",
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
        int idx = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (selectedEntity.getId() != null && selectedEntity.getId().equals(sorted.get(i).getId())) {
                idx = i;
                break;
            }
        }
        return (idx > 0) ? sorted.get(idx - 1) : null;
    }

    /**
     * Retourne le TYPE qui suit l'entité sélectionnée (même parent, ordre alphabétique croissant).
     * Retourne null si l'entité sélectionnée n'est pas un TYPE ou s'il n'y a pas de successeur.
     */
    public Entity getNextType() {
        if (selectedEntity == null || selectedEntity.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_TYPE.equals(selectedEntity.getEntityType().getCode())) {
            return null;
        }
        Entity selectedSerie = getSelectedSerie();
        if (selectedSerie == null) return null;
        List<Entity> siblings = typeService.loadSerieTypes(selectedSerie);
        if (siblings == null || siblings.isEmpty()) return null;
        List<Entity> sorted = siblings.stream()
                .sorted(Comparator.comparing(e -> e.getNom() != null ? e.getNom().toLowerCase() : "",
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
        int idx = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (selectedEntity.getId() != null && selectedEntity.getId().equals(sorted.get(i).getId())) {
                idx = i;
                break;
            }
        }
        return (idx >= 0 && idx < sorted.size() - 1) ? sorted.get(idx + 1) : null;
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

    /**
     * Vérifie si la session ou la vue a expiré
     */
    private void checkSessionExpiration() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String sessionExpired = facesContext.getExternalContext().getRequestParameterMap().get(ViewConstants.PARAM_SESSION_EXPIRED);
            String viewExpired = facesContext.getExternalContext().getRequestParameterMap().get(ViewConstants.PARAM_VIEW_EXPIRED);
            
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
     * Retourne le libellé de la langue pour un code donné (ex. "fr" → "Français").
     */
    public String getLanguageLabel(String code) {
        if (code == null || code.isEmpty() || languages == null) return "Français";
        return languages.stream()
                .filter(l -> code.equalsIgnoreCase(l.getCode()))
                .findFirst()
                .map(Language::getValue)
                .orElse("Français");
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
     * selon les droits de l'utilisateur. Le tri est appliqué par getSortedCollections() selon le mode.
     */
    public void loadAllCollections() {
        collections = entityRepository.findByEntityTypeCodeWithLabels(EntityConstants.ENTITY_TYPE_COLLECTION);
        if (!CollectionUtils.isEmpty(collections)) {
            collections = collections.stream()
                    .filter(this::isEntityVisibleForCurrentUser)
                    .collect(Collectors.toList());
            if (!hasCustomCollectionsOrder()) {
                collectionsSortMode = ListSortMode.ALPHA_ASC;
            }
        }
    }

    /** Vrai si au moins une collection a un ordre personnalisé (display_order défini). */
    public boolean hasCustomCollectionsOrder() {
        if (collections == null || collections.isEmpty()) return false;
        return collections.stream().anyMatch(c -> c.getDisplayOrder() != null);
    }

    /** Collections triées selon le mode (manuel, A-Z, Z-A). */
    public List<Entity> getSortedCollections() {
        if (collections == null) return new ArrayList<>();
        return collections.stream()
                .sorted((c1, c2) -> switch (collectionsSortMode) {
                    case MANUAL -> {
                        Integer o1 = c1.getDisplayOrder();
                        Integer o2 = c2.getDisplayOrder();
                        int cmp = Integer.compare(
                                o1 != null ? o1 : Integer.MAX_VALUE,
                                o2 != null ? o2 : Integer.MAX_VALUE);
                        if (cmp != 0) yield cmp;
                        String n1 = getEntityLabel(c1);
                        String n2 = getEntityLabel(c2);
                        yield n1.compareToIgnoreCase(n2);
                    }
                    case ALPHA_ASC -> getEntityLabel(c1).compareToIgnoreCase(getEntityLabel(c2));
                    case ALPHA_DESC -> getEntityLabel(c2).compareToIgnoreCase(getEntityLabel(c1));
                })
                .collect(Collectors.toList());
    }

    public ListSortMode getCollectionsSortMode() { return collectionsSortMode; }
    public void setCollectionsSortMode(ListSortMode mode) {
        if (mode != null) this.collectionsSortMode = mode;
    }
    public boolean isCollectionsSortModeManual() { return collectionsSortMode == ListSortMode.MANUAL; }
    public boolean isCollectionsSortModeAlphaAsc() { return collectionsSortMode == ListSortMode.ALPHA_ASC; }
    public boolean isCollectionsSortModeAlphaDesc() { return collectionsSortMode == ListSortMode.ALPHA_DESC; }

    /** Change le mode de tri des collections (appelé depuis la vue). */
    public void setCollectionsSortModeManual() { setCollectionsSortMode(ListSortMode.MANUAL); }
    public void setCollectionsSortModeAlphaAsc() { setCollectionsSortMode(ListSortMode.ALPHA_ASC); }
    public void setCollectionsSortModeAlphaDesc() { setCollectionsSortMode(ListSortMode.ALPHA_DESC); }

    /** Ordre manuel pour le dialog de réorganisation (toujours display_order puis alpha). */
    private List<Entity> getCollectionsForOrdering() {
        if (collections == null) return new ArrayList<>();
        return collections.stream()
                .sorted((c1, c2) -> {
                    Integer o1 = c1.getDisplayOrder();
                    Integer o2 = c2.getDisplayOrder();
                    int cmp = Integer.compare(o1 != null ? o1 : Integer.MAX_VALUE, o2 != null ? o2 : Integer.MAX_VALUE);
                    if (cmp != 0) return cmp;
                    return getEntityLabel(c1).compareToIgnoreCase(getEntityLabel(c2));
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Méthode appelée lorsque la langue change dans la barre de recherche
     * Recharge les collections avec le nouveau tri selon la langue sélectionnée
     */
    public void onLanguageChange() {
        // Recharger les collections pour appliquer le nouveau tri selon la langue
        loadAllCollections();
        
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
    public void navigateToBreadcrumbElement(Entity entity, EntityUpdateBean entityUpdateBean) {
        if (entity == null || entity.getEntityType() == null) {
            return;
        }

        if (entityUpdateBean.isEditingEntity()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention !",
                            "Vous devez quitter la page de modification avant de changer de page"));
            return;
        }

        entityUpdateBean.setEditingEntity(false);
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

    public void showCollections(EntityUpdateBean entityUpdateBean) {

        if (entityUpdateBean.isEditingEntity()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention !",
                            "Vous devez quitter la page de modification avant de changer de page"));
            return;
        }

        this.selectedEntityLabel = "";
        this.selectedEntity = null;
        this.breadCrumbElements = new ArrayList<>();
        this.childs = new ArrayList<>();
        searchBean.setCollectionSelected(null);
        panelState.showCollections();
    }

    /**
     * Appelé au clic sur le menu Accueil. Réinitialise l'interface (panelState.showCollections) puis redirige vers l'Accueil.
     */
    public String goToAccueil(EntityUpdateBean entityUpdateBean) throws Exception {
        if (entityUpdateBean.isEditingEntity()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention !",
                            "Vous devez quitter la page de modification avant de changer de page"));
            return "";
        }
        showCollections(entityUpdateBean);
        return "/index.xhtml?faces-redirect=true";
    }

    public String goToBrouillons(EntityUpdateBean entityUpdateBean) {
        if (entityUpdateBean.isEditingEntity()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention !",
                            "Vous devez quitter la page de modification avant de changer de page"));
            return "";
        }
        return "/candidats/candidats.xhtml?faces-redirect=true";
    }

    public String goToUtilisateurs(EntityUpdateBean entityUpdateBean) {
        if (entityUpdateBean.isEditingEntity()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention !",
                            "Vous devez quitter la page de modification avant de changer de page"));
            return "";
        }
        return "/users/users.xhtml?faces-redirect=true";
    }

    public String goToScientificAuthors(EntityUpdateBean entityUpdateBean) {
        if (entityUpdateBean.isEditingEntity()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention !",
                            "Vous devez quitter la page de modification avant de changer de page"));
            return "";
        }
        return "/authors/scientific-authors.xhtml?faces-redirect=true";
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
        breadCrumbElements = buildBreadcrumbFromSelectedEntity();
        getTreeBean().expandPathAndSelectEntity(selectedEntity);
    }

    /**
     * Affiche les détails d'un référentiel spécifique
     */
    public void showReferenceDetail(Entity reference) {
        this.selectedEntity = reference;
        panelState.showReference();
        breadCrumbElements = buildBreadcrumbFromSelectedEntity();
        refreshChilds();
        getTreeBean().selectReferenceNode(reference);
        getTreeBean().loadChildForEntity(reference);
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
        breadCrumbElements = buildBreadcrumbFromSelectedEntity();
        getTreeBean().expandPathAndSelectEntity(selectedEntity);
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
        breadCrumbElements = buildBreadcrumbFromSelectedEntity();
        getTreeBean().expandPathAndSelectEntity(selectedEntity);
    }

    /**
     * Affiche les détails d'une catégorie spécifique
     */
    public void showCategoryDetail(Entity category) {
        this.selectedEntity = category;
        panelState.showCategory();
        refreshChilds();
        breadCrumbElements = buildBreadcrumbFromSelectedEntity();
        getTreeBean().expandPathAndSelectEntity(category);
    }

    public void refreshCategoryGroupsList() {
        refreshChilds();
    }

    /**
     * Supprime récursivement une entité et toutes ses entités enfants.
     * Délègue à {@link fr.cnrs.opentypo.application.service.EntityDeletionService}.
     *
     * @param entity L'entité à supprimer
     */
    public void deleteEntityRecursively(Entity entity) {
        entityDeletionService.deleteEntityRecursively(entity);
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
     * Enregistre l'ordre d'affichage des types pour l'entité sélectionnée (série ou groupe).
     * @param orderedTypeIds Liste des IDs des types dans l'ordre souhaité
     */
    public void saveTypesOrder(List<Long> orderedTypeIds) {
        if (selectedEntity == null || selectedEntity.getId() == null || orderedTypeIds == null) return;
        typeService.updateTypesDisplayOrder(selectedEntity.getId(), orderedTypeIds);
        refreshChilds();
    }

    /* === Réorganisation unifiée (un seul dialog pour types, séries, groupes, catégories, référentiels) === */

    /** Prépare le dialog de réorganisation pour le type donné. */
    public void prepareOrderDialog(OrderingType type) {
        currentOrderingType = type;
        List<Entity> list = getChildsForOrdering(type);
        if (list == null || list.isEmpty()) {
            orderingIds = new ArrayList<>();
            orderingLabelsById = new java.util.HashMap<>();
            return;
        }
        orderingIds = list.stream()
                .filter(e -> e != null && e.getId() != null)
                .map(Entity::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        orderingLabelsById = list.stream()
                .filter(e -> e != null && e.getId() != null)
                .collect(Collectors.toMap(Entity::getId, e -> buildOrderingLabel(e, type), (a, b) -> a));
    }

    private List<Entity> getChildsForOrdering(OrderingType type) {
        return switch (type) {
            case COLLECTIONS -> getCollectionsForOrdering();
            case TYPES -> getChildsTypes();
            case SERIES -> getChildsSeries();
            case GROUPES -> getChildsGroupes();
            case CATEGORIES -> getChildsCategories();
            case REFERENCES -> getChildsReferences();
        };
    }

    private String buildOrderingLabel(Entity e, OrderingType type) {
        if (type == OrderingType.COLLECTIONS) {
            return getEntityLabel(e);
        }
        String code = e.getCode() != null ? e.getCode() : "";
        if (type == OrderingType.GROUPES && e.getNom() != null) {
            return code + " - " + e.getNom();
        }
        return code;
    }

    /** Enregistre l'ordre depuis le dialog et ferme. */
    public void saveOrderFromDialog() {
        if (orderingIds == null || orderingIds.isEmpty() || currentOrderingType == null) {
            return;
        }
        List<Long> validIds = orderingIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
        if (validIds.isEmpty()) return;

        switch (currentOrderingType) {
            case COLLECTIONS -> {
                collectionService.updateDisplayOrderForCollections(validIds);
                loadAllCollections();
                collectionsSortMode = ListSortMode.MANUAL;
            }
            case TYPES -> {
                if (selectedEntity != null && selectedEntity.getId() != null) {
                    saveTypesOrder(validIds);
                    typesSortMode = ListSortMode.MANUAL;
                }
            }
            case SERIES -> {
                if (selectedEntity != null && selectedEntity.getId() != null) {
                    serieService.updateDisplayOrder(selectedEntity.getId(), validIds);
                    refreshChilds();
                    seriesSortMode = ListSortMode.MANUAL;
                }
            }
            case GROUPES -> {
                if (selectedEntity != null && selectedEntity.getId() != null) {
                    groupService.updateDisplayOrder(selectedEntity.getId(), validIds);
                    refreshChilds();
                    groupesSortMode = ListSortMode.MANUAL;
                }
            }
            case CATEGORIES -> {
                if (selectedEntity != null && selectedEntity.getId() != null) {
                    categoryService.updateDisplayOrder(selectedEntity.getId(), validIds);
                    refreshChilds();
                    categoriesSortMode = ListSortMode.MANUAL;
                }
            }
            case REFERENCES -> {
                if (selectedEntity != null && selectedEntity.getId() != null) {
                    referenceService.updateDisplayOrder(selectedEntity.getId(), validIds);
                    refreshChilds();
                    referencesSortMode = ListSortMode.MANUAL;
                }
            }
        }
        // Rafraîchir l'arbre pour refléter le nouvel ordre
        TreeBean tb = getTreeBean();
        if (tb != null) {
            tb.reloadChildrenForEntity(selectedEntity);
        }
    }

    public List<Long> getOrderingIds() {
        return orderingIds;
    }

    public void setOrderingIds(List<Long> ids) {
        this.orderingIds = ids != null
                ? ids.stream().filter(id -> id != null).collect(Collectors.toList())
                : new ArrayList<>();
    }

    /** Retourne le label d'un élément par son ID (pour l'affichage dans le OrderList). */
    public String getOrderingLabel(Long id) {
        return id != null && orderingLabelsById != null ? orderingLabelsById.getOrDefault(id, "") : "";
    }

    /** Titre du dialog (affiché dynamiquement selon le type). */
    public String getOrderingDialogTitle() {
        return currentOrderingType != null ? currentOrderingType.getTitle() : "";
    }

    /* === Méthodes de convenance (conservées pour compatibilité des vues) === */
    public void prepareCollectionsOrderDialog() { prepareOrderDialog(OrderingType.COLLECTIONS); }
    public void prepareTypesOrderDialog() { prepareOrderDialog(OrderingType.TYPES); }
    public void prepareSeriesOrderDialog() { prepareOrderDialog(OrderingType.SERIES); }
    public void prepareGroupesOrderDialog() { prepareOrderDialog(OrderingType.GROUPES); }
    public void prepareCategoriesOrderDialog() { prepareOrderDialog(OrderingType.CATEGORIES); }
    public void prepareReferencesOrderDialog() { prepareOrderDialog(OrderingType.REFERENCES); }

    public String getEntityLabel(Entity entitySelected) {

        String codeLang = searchBean.getLangSelected();
        return entitySelected.getLabels().stream()
                .filter(label -> codeLang.equalsIgnoreCase(label.getLangue().getCode()))
                .findFirst()
                .map(Label::getNom)
                .orElse("Non renseigné");
    }

    private static final int DESCRIPTION_MAX_CHARS = 400;

    public int getDescriptionMaxChars() {
        return DESCRIPTION_MAX_CHARS;
    }

    public String getEntityDescription(Entity entitySelected) {

        String codeLang = searchBean.getLangSelected();
        return entitySelected.getDescriptions().stream()
                .filter(description -> codeLang.equalsIgnoreCase(description.getLangue().getCode()))
                .findFirst()
                .map(Description::getValeur)
                .orElse(null);
    }

    /**
     * Texte brut de la description (sans balises HTML) pour le comptage des caractères.
     */
    public String getEntityDescriptionPlainText(Entity entitySelected) {
        String html = getEntityDescription(entitySelected);
        if (html == null || html.isEmpty()) return "";
        String withoutTags = html.replaceAll("<[^>]+>", " ");
        return withoutTags.replaceAll("\\s+", " ").trim();
    }

    /**
     * Indique si la description dépasse la limite de caractères.
     */
    public boolean isEntityDescriptionLong(Entity entitySelected, int maxChars) {
        return getEntityDescriptionPlainText(entitySelected).length() > maxChars;
    }

    /**
     * Description tronquée à maxChars caractères (texte brut).
     */
    public String getEntityDescriptionTruncated(Entity entitySelected, int maxChars) {
        String plain = getEntityDescriptionPlainText(entitySelected);
        if (plain.length() <= maxChars) return plain;
        return plain.substring(0, maxChars) + "...";
    }

    public boolean showCommentaireBloc() {

        if (EntityStatusEnum.PUBLIQUE.name().equals(selectedEntity.getStatut())) {
            return false;
        }

        return loginBean.getCurrentUser() != null
            && (selectedEntity.getEntityType().getId() == 3 || selectedEntity.getEntityType().getId() == 4 || selectedEntity.getEntityType().getId() == 5);
    }

    /**
     * Indique si le bloc commentaires/discussion doit être visible sur la page groupe.
     * Visible si : entité privée, utilisateur connecté, et l'un des rôles : administrateur technique,
     * gestionnaire de collection, gestionnaire de référentiel, rédacteur, relecteur ou valideur du groupe.
     */
    public boolean showCommentaireBlocGroup() {
        if (selectedEntity == null || EntityStatusEnum.PUBLIQUE.name().equals(selectedEntity.getStatut())) {
            return false;
        }
        if (!loginBean.isAuthenticated() || loginBean.getCurrentUser() == null) {
            return false;
        }
        if (selectedEntity.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_GROUP.equals(selectedEntity.getEntityType().getCode())) {
            return false;
        }
        if (loginBean.isAdminTechniqueOrFonctionnel()) {
            return true;
        }
        Long userId = loginBean.getCurrentUser().getId();
        Entity collection = getSelectedCollection();
        if (collection != null && collection.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, collection.getId(),
                PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())) {
            return true;
        }
        Entity reference = getSelectedReference();
        if (reference != null && reference.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, reference.getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, selectedEntity.getId(),
                PermissionRoleEnum.REDACTEUR.getLabel())) {
            return true;
        }
        if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, selectedEntity.getId(),
                PermissionRoleEnum.RELECTEUR.getLabel())) {
            return true;
        }
        if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, selectedEntity.getId(),
                PermissionRoleEnum.VALIDEUR.getLabel())) {
            return true;
        }
        return false;
    }

    /**
     * Indique si le bloc commentaires/discussion doit être visible sur la page série.
     * Visible si : entité privée, utilisateur connecté, et l'un des rôles : administrateur technique,
     * gestionnaire de collection, gestionnaire de référentiel, rédacteur, relecteur ou valideur du groupe contenant la série.
     */
    public boolean showCommentaireBlocSerie() {
        if (selectedEntity == null || EntityStatusEnum.PUBLIQUE.name().equals(selectedEntity.getStatut())) {
            return false;
        }
        if (!loginBean.isAuthenticated() || loginBean.getCurrentUser() == null) {
            return false;
        }
        if (selectedEntity.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_SERIES.equals(selectedEntity.getEntityType().getCode())) {
            return false;
        }
        if (loginBean.isAdminTechniqueOrFonctionnel()) {
            return true;
        }
        Long userId = loginBean.getCurrentUser().getId();
        Entity collection = getSelectedCollection();
        if (collection != null && collection.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, collection.getId(),
                PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())) {
            return true;
        }
        Entity reference = getSelectedReference();
        if (reference != null && reference.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, reference.getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        Entity group = getSelectedGroup();
        if (group != null && group.getId() != null) {
            if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, group.getId(), PermissionRoleEnum.REDACTEUR.getLabel())) {
                return true;
            }
            if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, group.getId(), PermissionRoleEnum.RELECTEUR.getLabel())) {
                return true;
            }
            if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, group.getId(), PermissionRoleEnum.VALIDEUR.getLabel())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indique si le bloc commentaires/discussion doit être visible sur la page type.
     * Visible si : entité privée, utilisateur connecté, et l'un des rôles : administrateur technique,
     * gestionnaire de collection, gestionnaire de référentiel, rédacteur, relecteur ou valideur du groupe contenant le type.
     */
    public boolean showCommentaireBlocType() {
        if (selectedEntity == null || EntityStatusEnum.PUBLIQUE.name().equals(selectedEntity.getStatut())) {
            return false;
        }
        if (!loginBean.isAuthenticated() || loginBean.getCurrentUser() == null) {
            return false;
        }
        if (selectedEntity.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_TYPE.equals(selectedEntity.getEntityType().getCode())) {
            return false;
        }
        if (loginBean.isAdminTechniqueOrFonctionnel()) {
            return true;
        }
        Long userId = loginBean.getCurrentUser().getId();
        Entity collection = getSelectedCollection();
        if (collection != null && collection.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, collection.getId(),
                PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())) {
            return true;
        }
        Entity reference = getSelectedReference();
        if (reference != null && reference.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, reference.getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        Entity group = getSelectedGroup();
        if (group != null && group.getId() != null) {
            if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, group.getId(), PermissionRoleEnum.REDACTEUR.getLabel())) {
                return true;
            }
            if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, group.getId(), PermissionRoleEnum.RELECTEUR.getLabel())) {
                return true;
            }
            if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, group.getId(), PermissionRoleEnum.VALIDEUR.getLabel())) {
                return true;
            }
        }
        return false;
    }

    public boolean isCashTypo() {

        Entity entity = collectionService.findCollectionIdByEntityId(selectedEntity.getId());
        String label = candidatReferenceTreeService.getCollectionLabel(entity, searchBean.getLangSelected());
        return "MONNAIE".equals(label) || "CASH".equals(label);
    }

    /** Retourne le label (ou code) de la collection de l'entité sélectionnée. */
    public String getSelectedCollectionLabel() {
        if (selectedEntity == null || selectedEntity.getId() == null) return "";
        Entity collection = collectionService.findCollectionIdByEntityId(selectedEntity.getId());
        if (collection == null) return "";
        String label = candidatReferenceTreeService.getCollectionLabel(collection, searchBean.getLangSelected());
        return label != null ? label : (collection.getCode() != null ? collection.getCode() : "");
    }

    /** Indique si l'entité sélectionnée appartient à une collection Céramique. */
    public boolean isCeramiqueTypo() {
        String label = getSelectedCollectionLabel();
        if (label == null || label.isBlank()) return false;
        String upper = label.toUpperCase();
        return upper.contains("CERAMIQUE") || upper.contains("CERAMIC");
    }

    /** Indique si l'entité sélectionnée appartient à une collection Instrumentum. */
    public boolean isInstrumentumTypo() {
        String label = getSelectedCollectionLabel();
        if (label == null || label.isBlank()) return false;
        return label.toUpperCase().contains("INSTRUMENTUM");
    }

    /**
     * Annule l'édition du référentiel
     */
    /**
     * Prépare l'affichage du dialog de confirmation avant changement de visibilité.
     * @param makePublic true pour rendre public, false pour rendre privé
     */
    public void prepareConfirmVisibilityChange(boolean makePublic) {
        if (EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) {
            String msg = getEntityLabel(selectedEntity) + " est une proposition, elle doit être approuvée avant pour pouvoir changer le statut";
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur", msg));
            return;
        }
        requestedVisibilityStatus = makePublic;
        PrimeFaces.current().executeScript("PF('confirmReferenceVisibilityChangeDialog').show();");
    }

    /**
     * Applique le changement de visibilité après confirmation et persiste en base.
     */
    @Transactional
    public void applyVisibilityChange(ApplicationBean applicationBean) {
        if (applicationBean == null || requestedVisibilityStatus == null) {
            return;
        }

        selectedEntity.setStatut(requestedVisibilityStatus ? EntityStatusEnum.PUBLIQUE.name() : EntityStatusEnum.PRIVEE.name());
        selectedEntity = entityRepository.save(applicationBean.getSelectedEntity());
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                        requestedVisibilityStatus ? "L'élément est maintenant public." : "L'élément est maintenant privé."));
        log.info("La visibilité du {} modifiée: {}", applicationBean.getSelectedEntity().getCode(), requestedVisibilityStatus ? "public" : "privé");
        requestedVisibilityStatus = null;
    }

    /** Message pour le dialog de confirmation de changement de visibilité */
    public String getVisibilityConfirmMessage() {
        if (requestedVisibilityStatus == null) return "";
        return requestedVisibilityStatus
                ? "Voulez-vous rendre cet élément public ? Il sera visible par tous les utilisateurs."
                : "Voulez-vous rendre cet élément privé ? Seuls les utilisateurs autorisés pourront y accéder.";
    }

    /** Titre du dialog selon le changement demandé */
    public String getVisibilityConfirmTitle() {
        if (requestedVisibilityStatus == null) return "Changer la visibilité";
        return requestedVisibilityStatus ? "Rendre cet élément public" : "Rendre cet élément privé";
    }

    // ==================== Publier / Refuser (entité PROPOSITION) ====================

    /**
     * Prépare l'affichage du dialog de confirmation pour Publier ou Refuser une proposition.
     * @param publish true pour publier (PUBLIQUE), false pour refuser (REFUSE)
     */
    public void prepareConfirmPropositionAction(boolean publish) {
        if (selectedEntity == null || !EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) {
            return;
        }
        requestedPropositionAction = publish;
        PrimeFaces.current().executeScript("PF('confirmPropositionActionDialog').show();");
    }

    /**
     * Applique Publier ou Refuser après confirmation et persiste en base.
     * Met à jour le statut de l'élément et de tous ses fils (directs et indirects).
     */
    @Transactional
    public void applyPropositionAction(ApplicationBean applicationBean) {
        if (applicationBean == null || requestedPropositionAction == null || selectedEntity == null) {
            return;
        }
        if (!EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention",
                            "L'entité n'est plus en statut proposition."));
            requestedPropositionAction = null;
            return;
        }
        String newStatut = requestedPropositionAction ? EntityStatusEnum.PUBLIQUE.name() : EntityStatusEnum.REFUSE.name();
        Set<Long> entityIdsToUpdate = collectEntityAndDescendantIds(selectedEntity.getId());
        for (Long entityId : entityIdsToUpdate) {
            entityRepository.findById(entityId).ifPresent(entity -> {
                entity.setStatut(newStatut);
                entityRepository.save(entity);
            });
        }
        selectedEntity = entityRepository.findById(selectedEntity.getId()).orElse(selectedEntity);
        selectedEntity.setStatut(newStatut);
        int count = entityIdsToUpdate.size();
        String msg = requestedPropositionAction
                ? (count > 1
                        ? "L'élément et " + (count - 1) + " sous-élément(s) ont été publiés avec succès."
                        : "L'élément a été publié avec succès.")
                : (count > 1
                        ? "L'élément et " + (count - 1) + " sous-élément(s) ont été refusés."
                        : "L'élément a été refusé.");
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", msg));
        log.info("Statut modifié ({} éléments): {} -> {}", count, selectedEntity.getCode(),
                requestedPropositionAction ? "PUBLIQUE" : "REFUSE");
        requestedPropositionAction = null;
    }

    /** Collecte l'ID de l'entité et de tous ses descendants (directs et indirects). */
    private Set<Long> collectEntityAndDescendantIds(Long rootId) {
        Set<Long> ids = new HashSet<>();
        if (rootId == null) return ids;
        ids.add(rootId);
        try {
            List<Object[]> relations = entityRelationRepository.findAllDescendantRelations(rootId);
            if (relations != null) {
                for (Object[] row : relations) {
                    if (row != null && row.length >= 2 && row[1] != null) {
                        ids.add(((Number) row[1]).longValue());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Erreur lors de la collecte des descendants pour entity {}: {}", rootId, e.getMessage());
        }
        return ids;
    }

    /** Message pour le dialog de confirmation Publier/Refuser */
    public String getPropositionConfirmMessage() {
        if (requestedPropositionAction == null) return "";
        return requestedPropositionAction
                ? "Voulez-vous publier cet élément ? Lui et tous ses sous-éléments (séries, types, etc.) seront visibles par tous les utilisateurs."
                : "Voulez-vous refuser cette proposition ? L'élément et tous ses sous-éléments ne seront plus visibles dans le référentiel.";
    }

    /** Titre du dialog Publier/Refuser */
    public String getPropositionConfirmTitle() {
        if (requestedPropositionAction == null) return "";
        return requestedPropositionAction ? "Publier cette proposition" : "Refuser cette proposition";
    }

    /** Indique si l'entité sélectionnée est en statut PROPOSITION */
    public boolean isSelectedEntityProposition() {
        return selectedEntity != null && EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut());
    }

    /**
     * Indique si un champ doit être affiché.
     * Si statut = PROPOSITION : toujours afficher (même vide).
     * Sinon : afficher uniquement si le champ a une valeur.
     */
    public boolean showFieldWithValue(Object value) {
        if (selectedEntity == null) return false;
        if (EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) return true;
        if (value == null) return false;
        if (value instanceof String s) return !s.trim().isEmpty();
        if (value instanceof java.util.Collection<?> c) return !c.isEmpty();
        return true;
    }

    public List<Utilisateur> uniqueAuteurs(java.util.Collection<Utilisateur> auteurs) {
        if (auteurs == null || auteurs.isEmpty()) {
            return List.of();
        }
        List<Utilisateur> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Utilisateur auteur : auteurs) {
            if (auteur == null) {
                continue;
            }
            String key;
            if (auteur.getId() != null) {
                key = "id:" + auteur.getId();
            } else {
                String prenom = auteur.getPrenom() != null ? auteur.getPrenom().trim().toLowerCase() : "";
                String nom = auteur.getNom() != null ? auteur.getNom().trim().toLowerCase() : "";
                key = "name:" + prenom + "|" + nom;
            }
            if (!seen.contains(key)) {
                seen.add(key);
                unique.add(auteur);
            }
        }
        return unique;
    }

    /**
     * Indique si un bloc doit être affiché (au moins un champ avec valeur, ou statut PROPOSITION).
     */
    private boolean hasAnyValue(Object v) {
        if (v == null) return false;
        if (v instanceof String s) return !s.trim().isEmpty();
        if (v instanceof java.util.Collection<?> c) return !c.isEmpty();
        return true;
    }

    /** Bloc Gestion : référence, typologique, identifiant, ancienne version */
    public boolean showGestionBlock() {
        if (selectedEntity == null) return false;
        if (EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) return true;
        var m = selectedEntity.getMetadata();
        return hasAnyValue(m != null ? m.getReference() : null) || hasAnyValue(selectedEntity.getTypologieScientifique())
                || hasAnyValue(selectedEntity.getIdentifiantPerenne()) || hasAnyValue(selectedEntity.getAncienneVersion());
    }

    /** Bloc Caractéristiques physiques (céramique) */
    public boolean showCaracteristiquesPhysiqueBlock() {
        if (selectedEntity == null) return false;
        if (EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) return true;
        var cp = selectedEntity.getCaracteristiquePhysique();
        var dp = selectedEntity.getDescriptionPate();
        return hasAnyValue(cp != null ? cp.getMetrologie() : null) || hasAnyValue(cp != null ? cp.getFabrication() : null)
                || hasAnyValue(dp != null ? dp.getDescription() : null) || hasAnyValue(dp != null ? dp.getCouleur() : null)
                || hasAnyValue(dp != null ? dp.getNature() : null) || hasAnyValue(dp != null ? dp.getInclusion() : null)
                || hasAnyValue(dp != null ? dp.getCuisson() : null);
    }

    /** Bloc Caractéristiques physiques monnaie */
    public boolean showCaracteristiquesPhysiqueMonnaieBlock() {
        if (selectedEntity == null) return false;
        if (EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) return true;
        var cpm = selectedEntity.getCaracteristiquePhysiqueMonnaie();
        if (cpm == null) return false;
        return hasAnyValue(cpm.getMateriaux()) || hasAnyValue(cpm.getDenomination()) || hasAnyValue(cpm.getMetrologie())
                || hasAnyValue(cpm.getValeur()) || hasAnyValue(cpm.getTechnique()) || hasAnyValue(cpm.getFabrication());
    }

    /** Bloc Caractéristiques physiques instrumentum */
    public boolean showCaracteristiquesPhysiqueInstrumentumBlock() {
        if (selectedEntity == null) return false;
        if (EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) return true;
        var cp = selectedEntity.getCaracteristiquePhysique();
        if (cp == null) return false;
        return hasAnyValue(cp.getMateriaux()) || hasAnyValue(cp.getForme()) || hasAnyValue(cp.getDimensions())
                || hasAnyValue(cp.getTechnique()) || hasAnyValue(cp.getFabrication());
    }

    /** Bloc Description (décors, marques, fonction, catégorie fonctionnelle) */
    public boolean showDescriptionBlock() {
        if (selectedEntity == null) return false;
        if (EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) return true;
        var dd = selectedEntity.getDescriptionDetail();
        return hasAnyValue(dd != null ? dd.getDecors() : null) || hasAnyValue(dd != null ? dd.getMarques() : null)
                || hasAnyValue(dd != null ? dd.getFonction() : null) || hasAnyValue(selectedEntity.getCategorieFonctionnelle());
    }

    /** Bloc Production */
    public boolean showProductionBlock() {
        if (selectedEntity == null) return false;
        if (EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) return true;
        var m = selectedEntity.getMetadata();
        return hasAnyValue(selectedEntity.getProduction()) || hasAnyValue(m != null ? m.getAteliers() : null)
                || hasAnyValue(selectedEntity.getAiresCirculation());
    }

    /** Bloc Description monnaie */
    public boolean showDescriptionMonnaieBlock() {
        if (selectedEntity == null) return false;
        if (EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) return true;
        var dm = selectedEntity.getDescriptionMonnaie();
        if (dm == null) return false;
        return hasAnyValue(dm.getDroit()) || hasAnyValue(dm.getLegendeDroit()) || hasAnyValue(dm.getCoinsMonetairesDroit())
                || hasAnyValue(dm.getRevers()) || hasAnyValue(dm.getLegendeRevers()) || hasAnyValue(dm.getCoinsMonetairesRevers());
    }

    /** Bloc Datation (période, TPQ, TAQ, commentaire datation) */
    public boolean showDatationBlock() {
        if (selectedEntity == null) return false;
        if (EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) return true;
        return hasAnyValue(selectedEntity.getPeriode()) || hasAnyValue(selectedEntity.getTpq())
                || hasAnyValue(selectedEntity.getTaq()) || hasAnyValue(selectedEntity.getCommentaireDatation());
    }

    @Getter
    @AllArgsConstructor
    public static class InternalAlignmentViewItem implements Serializable {
        private Long targetTypeId;
        private String targetTypeCode;
        private String targetTypeLabel;
        private String matchType;
    }

    @Getter
    @AllArgsConstructor
    public static class ExternalAlignmentViewItem implements Serializable {
        private String label;
        private String url;
        private String matchType;
    }

    public List<InternalAlignmentViewItem> getSelectedTypeInternalAlignments() {
        if (selectedEntity == null || selectedEntity.getId() == null || selectedEntity.getEntityType() == null
                || !"TYPE".equals(selectedEntity.getEntityType().getCode()) || internalAlignmentRepository == null) {
            return List.of();
        }
        return internalAlignmentRepository.findBySourceTypeIdWithTarget(selectedEntity.getId()).stream()
                .filter(alignment -> alignment.getTargetType() != null)
                .map(alignment -> new InternalAlignmentViewItem(
                        alignment.getTargetType().getId(),
                        alignment.getTargetType().getCode(),
                        getEntityLabel(alignment.getTargetType()),
                        alignment.getMatchType()))
                .collect(Collectors.toList());
    }

    public List<ExternalAlignmentViewItem> getSelectedTypeExternalAlignments() {
        if (selectedEntity == null || selectedEntity.getId() == null || selectedEntity.getEntityType() == null
                || !"TYPE".equals(selectedEntity.getEntityType().getCode()) || externalAlignmentRepository == null) {
            return List.of();
        }
        return externalAlignmentRepository.findBySourceType_IdOrderByIdAsc(selectedEntity.getId()).stream()
                .map(alignment -> new ExternalAlignmentViewItem(
                        alignment.getLabel(),
                        alignment.getUrl(),
                        alignment.getMatchType()))
                .collect(Collectors.toList());
    }

    /** Bloc Alignements (internes + externes) */
    public boolean showAlignementsBlock() {
        if (selectedEntity == null) return false;
        if (EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) return true;
        return hasAnyValue(getSelectedTypeInternalAlignments())
                || hasAnyValue(getSelectedTypeExternalAlignments());
    }

    /**
     * Indique si l'utilisateur connecté peut publier ou refuser une proposition (groupe).
     * Visible si : administrateur technique, gestionnaire de la collection, validateur du groupe,
     * ou gestionnaire de la référence contenant le groupe.
     */
    public boolean canPublishOrRefuseProposition() {
        if (!loginBean.isAuthenticated() || selectedEntity == null
                || !EntityStatusEnum.PROPOSITION.name().equals(selectedEntity.getStatut())) {
            return false;
        }
        if (loginBean.isAdminTechniqueOrFonctionnel()) {
            return true;
        }
        Long userId = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
        if (userId == null) return false;

        Entity collection = getSelectedCollection();
        if (collection != null && collection.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, collection.getId(),
                PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())) {
            return true;
        }
        if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, selectedEntity.getId(),
                PermissionRoleEnum.VALIDEUR.getLabel())) {
            return true;
        }
        Entity reference = getSelectedReference();
        if (reference != null && reference.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, reference.getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        return false;
    }
}

