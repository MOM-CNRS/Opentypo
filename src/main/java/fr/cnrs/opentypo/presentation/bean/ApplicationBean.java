package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.application.dto.SerieWithTypes;
import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.ReferenceService;
import fr.cnrs.opentypo.application.service.SerieService;
import fr.cnrs.opentypo.application.service.TypeService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.common.models.Language;
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
import fr.cnrs.opentypo.presentation.bean.candidats.Candidat;
import fr.cnrs.opentypo.presentation.bean.candidats.CandidatBean;
import fr.cnrs.opentypo.presentation.bean.candidats.converter.CandidatConverter;
import fr.cnrs.opentypo.presentation.bean.photos.Photo;
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

import java.io.IOException;
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

    @Inject
    private UserBean userBean;

    @Inject
    private CandidatBean candidatBean;


    private final PanelStateManager panelState = new PanelStateManager();
    private List<Language> languages;
    private List<Entity> beadCrumbElements, references, collections;
    
    /** Entité actuellement sélectionnée (collection, référentiel, catégorie, groupe, série ou type). */
    private Entity selectedEntity;

    /** Enfants directs de l'entité sélectionnée (référentiels, catégories, groupes, séries ou types selon le niveau). */
    private List<Entity> childs = new ArrayList<>();

    /** Filtre de recherche pour les catégories du référentiel. */
    private String categoriesSearchQuery = "";
    /** Page courante pour la pagination des catégories (1-based). */
    private int categoriesCurrentPage = 1;
    private static final int CATEGORIES_PAGE_SIZE = 6;

    /** Filtre de recherche pour les groupes de la catégorie. */
    private String groupesSearchQuery = "";
    /** Page courante pour la pagination des groupes (1-based). */
    private int groupesCurrentPage = 1;
    private static final int GROUPES_PAGE_SIZE = 6;

    /** Filtre de recherche pour les séries du groupe. */
    private String seriesSearchQuery = "";
    /** Page courante pour la pagination des séries (1-based). */
    private int seriesCurrentPage = 1;
    private static final int SERIES_PAGE_SIZE = 6;

    /** Filtre de recherche pour les types de la série. */
    private String typesSearchQuery = "";
    /** Page courante pour la pagination des types (1-based). */
    private int typesCurrentPage = 1;
    private static final int TYPES_PAGE_SIZE = 6;

    // Titre de l'écran
    private String selectedEntityLabel;


    @PostConstruct
    public void initialization() {
        checkSessionExpiration();
        loadLanguages();
        loadPublicCollections();
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
        boolean isAdminTechnique = loginBean != null && loginBean.isAdminTechnique();
        if (isAdminTechnique) {
            return true;
        }

        // 1) Ne jamais afficher les entités au statut REFUSED (pour les non-admin)
        if (EntityStatusEnum.REFUSED.name().equals(entity.getStatut())) {
            return false;
        }

        boolean authenticated = loginBean != null && loginBean.isAuthenticated();

        // 2) Utilisateur non connecté (offline) : uniquement les entités publique = true
        //    et statut différent de REFUSED et PROPOSITION
        if (!authenticated) {
            if (!Boolean.TRUE.equals(entity.getPublique())) {
                return false;
            }
            String statut = entity.getStatut();
            return statut != null
                    && !EntityStatusEnum.REFUSED.name().equals(statut)
                    && !EntityStatusEnum.PROPOSITION.name().equals(statut);
        }

        // 2b) Utilisateur connecté : pour les collections, afficher toutes (publique = true et false)
        //     (liste collections + select recherche)
        if (entity.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_COLLECTION.equals(entity.getEntityType().getCode())) {
            return true;
        }

        // 3) Utilisateur connecté : règle des collections publiques — toutes les collections
        //    publiques et tous leurs éléments sont visibles.
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
        boolean isAdminTechnique = loginBean != null && loginBean.isAdminTechnique();
        if (isAdminTechnique) {
            return true;
        }

        String statut = entity.getStatut();
        boolean isRefused = EntityStatusEnum.REFUSED.name().equals(statut);
        boolean isProposition = EntityStatusEnum.PROPOSITION.name().equals(statut);
        boolean authenticated = loginBean != null && loginBean.isAuthenticated();

        // 1) Jamais afficher REFUSED (pour les non-admin)
        if (isRefused) {
            return false;
        }

        // 2) Mode offline : publique = true ET statut différent de REFUSED et PROPOSITION
        if (!authenticated) {
            return Boolean.TRUE.equals(entity.getPublique())
                    && !isProposition;
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
                break;
            case EntityConstants.ENTITY_TYPE_REFERENCE:
                childs = categoryService.loadCategoriesByReference(selectedEntity);
                categoriesCurrentPage = 1;
                categoriesSearchQuery = "";
                break;
            case EntityConstants.ENTITY_TYPE_CATEGORY:
                childs = groupService.loadCategoryGroups(selectedEntity);
                groupesCurrentPage = 1;
                groupesSearchQuery = "";
                break;
            case EntityConstants.ENTITY_TYPE_GROUP:
                List<Entity> series = serieService.loadGroupSeries(selectedEntity);
                List<Entity> types = typeService.loadGroupTypes(selectedEntity);
                childs = new ArrayList<>();
                if (series != null) childs.addAll(series);
                if (types != null) childs.addAll(types);
                seriesCurrentPage = 1;
                seriesSearchQuery = "";
                typesCurrentPage = 1;
                typesSearchQuery = "";
                break;
            case EntityConstants.ENTITY_TYPE_SERIES:
                childs = typeService.loadSerieTypes(selectedEntity);
                typesCurrentPage = 1;
                typesSearchQuery = "";
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
        return childs.stream()
                .filter(e -> e != null && e.getEntityType() != null
                        && entityTypeCode.equals(e.getEntityType().getCode())
                        && isEntityVisibleInCatalogList(e, entityTypeCode))
                .sorted(Comparator.comparing(e -> e.getCode() != null ? e.getCode() : "", String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /** Taille des listes filtrées (évite fn:length en EL). */
    public int getFilteredCategoriesSize() { return getFilteredCategories().size(); }
    public int getFilteredGroupesSize() { return getFilteredGroupes().size(); }
    public int getFilteredSeriesSize() { return getFilteredSeries().size(); }
    public int getFilteredTypesSize() { return getFilteredTypes().size(); }

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

    /** Catégories pour la page courante (6 par page). */
    public List<Entity> getPaginatedCategories() {
        List<Entity> filtered = getFilteredCategories();
        if (filtered.isEmpty()) return new ArrayList<>();
        int from = (categoriesCurrentPage - 1) * CATEGORIES_PAGE_SIZE;
        if (from >= filtered.size()) {
            categoriesCurrentPage = 1;
            from = 0;
        }
        int to = Math.min(from + CATEGORIES_PAGE_SIZE, filtered.size());
        return filtered.subList(from, to);
    }

    public int getCategoriesTotalPages() {
        int size = getFilteredCategories().size();
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

    /** Groupes pour la page courante (6 par page). */
    public List<Entity> getPaginatedGroupes() {
        List<Entity> filtered = getFilteredGroupes();
        if (filtered.isEmpty()) return new ArrayList<>();
        int from = (groupesCurrentPage - 1) * GROUPES_PAGE_SIZE;
        if (from >= filtered.size()) {
            groupesCurrentPage = 1;
            from = 0;
        }
        int to = Math.min(from + GROUPES_PAGE_SIZE, filtered.size());
        return filtered.subList(from, to);
    }

    public int getGroupesTotalPages() {
        int size = getFilteredGroupes().size();
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

    /** Séries pour la page courante (6 par page). */
    public List<Entity> getPaginatedSeries() {
        List<Entity> filtered = getFilteredSeries();
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
        int size = getFilteredSeries().size();
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

    /**
     * Séries avec leurs types, filtrées par recherche (pour le panneau Groupe).
     * Chaque série contient la liste de ses types chargés depuis entity_relation.
     */
    public List<SerieWithTypes> getSeriesWithTypesFiltered() {
        List<Entity> filtered = getFilteredSeries();
        if (filtered == null || filtered.isEmpty()) return new ArrayList<>();
        List<SerieWithTypes> result = new ArrayList<>();
        for (Entity serie : filtered) {
            List<Entity> types = typeService.loadSerieTypes(serie);
            if (types == null) types = new ArrayList<>();
            result.add(new SerieWithTypes(serie, types));
        }
        return result;
    }

    /** Séries avec types pour la page courante (pagination). */
    public List<SerieWithTypes> getPaginatedSeriesWithTypes() {
        List<SerieWithTypes> all = getSeriesWithTypesFiltered();
        if (all.isEmpty()) return new ArrayList<>();
        int from = (seriesCurrentPage - 1) * SERIES_PAGE_SIZE;
        if (from >= all.size()) {
            seriesCurrentPage = 1;
            from = 0;
        }
        int to = Math.min(from + SERIES_PAGE_SIZE, all.size());
        return all.subList(from, to);
    }

    /** Types filtrés par la recherche. */
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

    /** Types pour la page courante (6 par page). */
    public List<Entity> getPaginatedTypes() {
        List<Entity> filtered = getFilteredTypes();
        if (filtered.isEmpty()) return new ArrayList<>();
        int from = (typesCurrentPage - 1) * TYPES_PAGE_SIZE;
        if (from >= filtered.size()) {
            typesCurrentPage = 1;
            from = 0;
        }
        int to = Math.min(from + TYPES_PAGE_SIZE, filtered.size());
        return filtered.subList(from, to);
    }

    public int getTypesTotalPages() {
        int size = getFilteredTypes().size();
        return size == 0 ? 0 : (int) Math.ceil((double) size / TYPES_PAGE_SIZE);
    }

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
                String title = entityLabel != null ? entityLabel + " - " + index : "Image " + index;
                photos.add(new Photo(img.getUrl(), img.getUrl(), title, title));
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
                    String nom1 = getEntityLabel(c1);
                    String nom2 = getEntityLabel(c2);
                    return nom1.compareToIgnoreCase(nom2);
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des collections depuis la base de données", e);
            collections = new ArrayList<>();
        }
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
                .orElse("Aucune description disponible");
    }

    public boolean showCommentaireBloc() {
        return loginBean.getCurrentUser() != null
            && (selectedEntity.getEntityType().getId() == 3 || selectedEntity.getEntityType().getId() == 4 || selectedEntity.getEntityType().getId() == 5);
    }

    public void editEntity() throws IOException {
        Candidat candidat = new CandidatConverter().convertEntityToCandidat(selectedEntity);
        candidatBean.visualiserCandidat(candidat);
        candidatBean.setFromCatalog(true);

        FacesContext.getCurrentInstance().getExternalContext()
                .redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/candidats/view.xhtml");
    }
}

