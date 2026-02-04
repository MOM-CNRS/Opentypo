package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.ReferenceService;
import fr.cnrs.opentypo.application.service.SerieService;
import fr.cnrs.opentypo.application.service.TreeService;
import fr.cnrs.opentypo.application.service.TypeService;
import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.NodeExpandEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Getter
@Setter
@SessionScoped
@Named(value = "treeBean")
@Slf4j
public class TreeBean implements Serializable {

    @Inject
    private transient Provider<ApplicationBean> applicationBeanProvider;

    @Inject
    private transient CategoryService categoryService;

    @Inject
    private transient GroupService groupService;

    @Inject
    private transient SerieService serieService;

    @Inject
    private transient TypeService typeService;

    @Inject
    private transient CollectionBean collectionBean;

    @Inject
    private transient TreeService treeService;

    @Inject
    private transient ReferenceService referenceService;

    @Inject
    private transient EntityRelationRepository entityRelationRepository;

    private TreeNode selectedNode;
    private TreeNode root;

    /** ID d'entité pour la sélection via formulaire (hidden input + commandButton). */
    private Long selectedEntityIdForAjax;

    /** ID d'entité pour le chargement des enfants au dépliage (hidden input + commandButton). */
    private Long expandEntityIdForAjax;

    /** Nœud dont on vient de charger les enfants : utilisé pour rendre uniquement le fragment HTML (préserve l'état de l'arbre). */
    private transient TreeNode pendingChildrenTreeNode;

    /** IDs des entités pour lesquelles on a chargé les enfants et le résultat était 0 (masquer le toggler). */
    private Set<Long> entityIdsWithNoChildren = new HashSet<>();


    @PostConstruct
    public void init() {
        // Racine (invisible)
        root = new DefaultTreeNode("root", null);
        // L'arbre sera initialisé dynamiquement quand une collection est sélectionnée
    }

    /** Résout ApplicationBean à la demande pour éviter dépendance circulaire / scope non actif au démarrage. */
    private ApplicationBean getApplicationBean() {
        return applicationBeanProvider != null ? applicationBeanProvider.get() : null;
    }

    /**
     * Initialise l'arbre avec la collection sélectionnée et charge récursivement tous les éléments
     * (référentiels, catégories, groupes, séries, types) en une fois pour de meilleures perfs.
     */
    public void initializeTreeWithCollection() {
        // Sauvegarder l'entité sélectionnée avant la réinitialisation
        Entity previouslySelectedEntity = null;
        if (selectedNode != null && selectedNode.getData() != null && selectedNode.getData() instanceof Entity) {
            previouslySelectedEntity = (Entity) selectedNode.getData();
        }

        Entity selectedCollection = null;
        ApplicationBean appBean = getApplicationBean();
        if (appBean != null && appBean.getSelectedCollection() != null) {
            selectedCollection = appBean.getSelectedCollection();
        }

        selectedNode = null;
        if (entityIdsWithNoChildren != null) {
            entityIdsWithNoChildren.clear();
        }

        if (selectedCollection != null && treeService != null) {
            try {
                root = treeService.buildRootWithDirectChildrenOnly(selectedCollection);
                if (previouslySelectedEntity != null && previouslySelectedEntity.getId() != null && root != null) {
                    TreeNode found = findNodeByEntity(root, previouslySelectedEntity);
                    if (found != null) {
                        selectedNode = found;
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de l'initialisation de l'arbre avec la collection", e);
                String collectionName = selectedCollection.getNom() != null ? selectedCollection.getNom() : "Collection";
                DefaultTreeNode fallback = new DefaultTreeNode(collectionName, null);
                fallback.setData(selectedCollection);
                root = fallback;
            }
        } else {
            root = new DefaultTreeNode("root", null);
        }
    }


    /**
     * Initialise l'arbre avec une entité spécifique comme racine
     * @param entity L'entité à utiliser comme racine de l'arbre
     */
    public void initializeTreeWithEntity(Entity entity) {
        if (entity == null) {
            root = new DefaultTreeNode("root", null);
            selectedNode = null;
            return;
        }

        // Créer un nouveau nœud racine avec l'entité sélectionnée
        String entityName = entity.getNom() != null ? entity.getNom() : "Entité";
        DefaultTreeNode entityRoot = new DefaultTreeNode(entityName, null);
        entityRoot.setData(entity);
        root = entityRoot;
        selectedNode = entityRoot;

        // Charger les enfants directs de cette entité
        try {
            loadChildrenIfNeeded(entityRoot);
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation de l'arbre avec l'entité : {}", entity.getNom(), e);
        }
    }

    /**
     * Ajoute un référentiel à l'arbre
     */
    public void addReferenceToTree(Entity reference) {
        if (root != null && reference != null) {
            DefaultTreeNode node = new DefaultTreeNode(reference.getNom(), root);
            node.setData(reference);
        }
    }

    /**
     * Ajoute une catégorie à l'arbre comme enfant d'un référentiel
     */
    public void addEntityToTree(Entity entity, Entity parentReference) {
        if (root != null && entity != null && parentReference != null) {
            // Trouver le nœud du référentiel parent dans l'arbre
            TreeNode referenceNode = findNodeByEntity(root, parentReference);
            if (referenceNode != null) {
                DefaultTreeNode categoryNode = new DefaultTreeNode(entity.getNom(), referenceNode);
                categoryNode.setData(entity);
                // Ne pas étendre le nœud parent - l'arbre reste replié par défaut
            } else {
                log.warn("Nœud référentiel non trouvé pour ajouter la catégorie : {}", parentReference.getNom());
            }
        }
    }

    public void onNodeSelect(NodeSelectEvent event) {
        this.selectedNode = event.getTreeNode();

        // Charger les enfants de l'élément sélectionné s'ils ne sont pas déjà chargés
        int childCountBefore = selectedNode != null ? selectedNode.getChildCount() : 0;
        loadChildrenIfNeeded(selectedNode);
        
        // Si des enfants ont été chargés, étendre le nœud pour les afficher
        if (selectedNode != null && selectedNode.getChildCount() > childCountBefore) {
            selectedNode.setExpanded(true);
            log.debug("Nœud {} étendu automatiquement après chargement de {} enfants", 
                     selectedNode.getData() instanceof Entity ? ((Entity) selectedNode.getData()).getNom() : "unknown",
                     selectedNode.getChildCount() - childCountBefore);
        }

        if (selectedNode != null && selectedNode.getData() != null && selectedNode.getData() instanceof Entity entity) {
            navigateToEntity(entity);
        }
    }

    /**
     * Sélectionne un nœud par ID d'entité (appel AJAX depuis le client).
     */
    public void selectEntityById() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String idStr = params != null ? params.get("entityId") : null;
        if ((idStr == null || idStr.isBlank()) && selectedEntityIdForAjax != null) {
            idStr = String.valueOf(selectedEntityIdForAjax);
        }
        if (idStr == null || idStr.isBlank()) return;
        Long entityId;
        try {
            entityId = Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            log.warn("selectEntityById: entityId invalide: {}", idStr);
            return;
        }
        if (root == null) return;
        TreeNode foundNode = findNodeByEntityId(root, entityId);
        if (foundNode == null) {
            log.warn("selectEntityById: nœud non trouvé pour entityId={}", entityId);
            return;
        }
        this.selectedNode = foundNode;
        int childCountBefore = foundNode.getChildCount();
        loadChildrenIfNeeded(foundNode);
        if (foundNode.getChildCount() > childCountBefore) {
            foundNode.setExpanded(true);
        }
        Object data = foundNode.getData();
        if (data == null || !(data instanceof Entity entity)) return;
        navigateToEntity(entity);
    }

    private void navigateToEntity(Entity entity) {
        if (entity.getEntityType() == null) return;

        ApplicationBean appBean = getApplicationBean();
        switch(entity.getEntityType().getCode()) {
            case EntityConstants.ENTITY_TYPE_COLLECTION:
                collectionBean.showCollectionDetail(entity);
                break;
            case EntityConstants.ENTITY_TYPE_REFERENCE:
                appBean.showCategoryDetail(entity);
                break;
            case EntityConstants.ENTITY_TYPE_GROUP:
                appBean.showReferenceDetail(entity);
                break;
            case EntityConstants.ENTITY_TYPE_SERIES:
                appBean.showSerie(entity);
                break;
            case EntityConstants.ENTITY_TYPE_TYPE:
                appBean.showType(entity);
                break;
        }
    }

    private TreeNode findNodeByEntityId(TreeNode node, Long entityId) {
        if (node == null || entityId == null) return null;
        if (node.getData() != null && node.getData() instanceof Entity nodeEntity) {
            if (entityId.equals(nodeEntity.getId())) return node;
        }
        if (node.getChildren() != null) {
            for (Object childObj : node.getChildren()) {
                if (childObj instanceof TreeNode child) {
                    TreeNode found = findNodeByEntityId(child, entityId);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    /**
     * Charge les enfants d'un nœud si nécessaire
     * Méthode utilitaire partagée entre onNodeExpand et onNodeSelect
     */
    private void loadChildrenIfNeeded(TreeNode node) {
        if (node == null || node.getData() == null) {
            return;
        }

        Object data = node.getData();
        if (!(data instanceof Entity)) {
            return;
        }

        Entity entity = (Entity) data;
        int childCountBefore = node.getChildCount();
        log.debug("Chargement des enfants pour le nœud : {} (type: {}), enfants actuels: {}",
                entity.getNom(),
                entity.getEntityType() != null ? entity.getEntityType().getCode() : "null",
                childCountBefore);

        // Vérifier si les enfants ont déjà été chargés
        if (childCountBefore > 0) {
            // Les enfants sont déjà chargés, ne rien faire
            log.debug("Les enfants sont déjà chargés pour le nœud : {}", entity.getNom());
            return;
        }

        loadChildForEntity(node, entity);
        int childCountAfter = node.getChildCount();
        log.debug("Après chargement, le nœud {} a maintenant {} enfants (avant: {})",
                entity.getNom(), childCountAfter, childCountBefore);
        if (childCountAfter == 0 && entity.getId() != null) {
            entityIdsWithNoChildren.add(entity.getId());
        }
    }

    public void loadChildForEntity(Entity entity) {
        if (root == null || entity == null) {
            return;
        }

        // Trouver le nœud du référentiel dans l'arbre
        TreeNode referenceNode = findNodeByEntity(root, entity);
        if (referenceNode == null) {
            log.warn("Nœud référentiel non trouvé pour charger les catégories : {}", entity.getNom());
            return;
        }

        // Charger les catégories si le nœud n'a pas encore d'enfants
        if (referenceNode.getChildCount() == 0) {
            loadChildForEntity(referenceNode, entity);
        }
    }

    private void loadChildForEntity(TreeNode entityNode, Entity collection) {
        List<Entity> elements = referenceService.loadChildOfEntity(collection);

        if (elements != null && !elements.isEmpty()) {
            for (Entity entity : elements) {
                // Vérifier si le référentiel existe déjà pour éviter les doublons
                boolean exists = false;
                if (entityNode.getChildren() != null) {
                    for (Object childObj : entityNode.getChildren()) {
                        if (childObj instanceof TreeNode) {
                            TreeNode childNode = (TreeNode) childObj;
                            if (childNode.getData() != null && childNode.getData() instanceof Entity) {
                                Entity childEntity = (Entity) childNode.getData();
                                if (childEntity.getId() != null && entity.getId() != null &&
                                        childEntity.getId().equals(entity.getId())) {
                                    exists = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                // Ajouter le référentiel seulement s'il n'existe pas déjà
                if (!exists) {
                    DefaultTreeNode referenceNode = new DefaultTreeNode(entity.getNom(), entityNode);
                    referenceNode.setData(entity);
                }
            }
        }
    }

    /**
     * Charge les enfants d'un nœud quand il est déplié (lazy loading niveau par niveau)
     */
    public void onNodeExpand(NodeExpandEvent event) {
        TreeNode expandedNode = event.getTreeNode();
        loadChildrenIfNeeded(expandedNode);
    }

    /**
     * Charge les enfants directs d'un nœud identifié par son entityId (appel AJAX au dépliage).
     * Stocke le nœud dans pendingChildrenTreeNode pour que la vue ne rende que le fragment (ul des enfants),
     * sans remplacer tout l'arbre, afin de préserver l'état déplié/empilé des autres nœuds.
     */
    public void loadChildrenForEntity() {
        pendingChildrenTreeNode = null;
        entityIdsWithNoChildren = entityIdsWithNoChildren != null ? entityIdsWithNoChildren : new HashSet<>();
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String idStr = params != null ? params.get("entityId") : null;
        if ((idStr == null || idStr.isBlank()) && expandEntityIdForAjax != null) {
            idStr = String.valueOf(expandEntityIdForAjax);
        }
        if (idStr == null || idStr.isBlank() || root == null) return;
        Long entityId;
        try {
            entityId = Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            log.warn("loadChildrenForEntity: entityId invalide: {}", idStr);
            return;
        }
        TreeNode node = findNodeByEntityId(root, entityId);
        if (node == null) {
            log.warn("loadChildrenForEntity: nœud non trouvé pour entityId={}", entityId);
            return;
        }
        loadChildrenIfNeeded(node);
        node.setExpanded(true);
        pendingChildrenTreeNode = node;
    }

    /** Nœud dont les enfants viennent d'être chargés ; utilisé pour rendre uniquement le fragment (ul) à injecter dans l'arbre. */
    public TreeNode getPendingChildrenTreeNode() {
        return pendingChildrenTreeNode;
    }

    /**
     * Retourne l'icône CSS pour un type d'entité donné
     * Utilisé pour afficher une icône différente selon le type d'entité dans l'arbre
     */
    public String getEntityIcon(Object node) {
        if (node == null) {
            return "fa fa-file";
        }

        Entity entity = null;
        
        // Si c'est directement une Entity
        if (node instanceof Entity) {
            entity = (Entity) node;
        }
        // Si c'est un TreeNode
        else if (node instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) node;
            if (treeNode.getData() != null && treeNode.getData() instanceof Entity) {
                entity = (Entity) treeNode.getData();
            }
        }

        if (entity == null || entity.getEntityType() == null) {
            return "fa fa-file";
        }

        // Retourner l'icône selon le type d'entité
        return switch (entity.getEntityType().getCode()) {
            case EntityConstants.ENTITY_TYPE_REFERENCE -> "fa fa-book";
            case EntityConstants.ENTITY_TYPE_COLLECTION -> "fa fa-folder-open";
            case EntityConstants.ENTITY_TYPE_CATEGORY -> "fa fa-tags";
            case EntityConstants.ENTITY_TYPE_GROUP -> "fa fa-users";
            case EntityConstants.ENTITY_TYPE_SERIES -> "fa fa-list";
            case EntityConstants.ENTITY_TYPE_TYPE -> "fa fa-tag";
            default -> "fa fa-file";
        };
    }

    /**
     * Retourne le nom de l'entité depuis un TreeNode ou directement depuis une Entity
     * Utilisé dans l'expression EL pour afficher le nom dans l'arbre
     */
    public String getNodeLabel(Object node) {
        if (node == null) {
            return "";
        }

        // Si c'est directement une Entity
        if (node instanceof Entity) {
            Entity entity = (Entity) node;
            return entity.getCode() != null ? entity.getCode() : "";
        }

        // Si c'est un TreeNode
        if (node instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) node;
            // Si le node a des données (Entity), récupérer le nom
            if (treeNode.getData() != null && treeNode.getData() instanceof Entity) {
                Entity entity = (Entity) treeNode.getData();
                return entity.getCode() != null ? entity.getCode() : "";
            }
            // Sinon, utiliser le label du TreeNode (toString())
            return treeNode.toString();
        }

        // Sinon, utiliser toString()
        return node.toString();
    }

    /**
     * Retourne l'entité associée à un nœud (TreeNode ou Entity), ou null.
     */
    public Entity getEntityFromNode(Object node) {
        if (node == null) return null;
        if (node instanceof Entity e) return e;
        if (node instanceof TreeNode tn && tn.getData() != null && tn.getData() instanceof Entity e) return e;
        return null;
    }

    /**
     * Indique si le nœud peut avoir des enfants (types collection, référentiel, catégorie, groupe).
     * Utilisé pour afficher le toggler même quand les enfants ne sont pas encore chargés (lazy load).
     */
    public boolean canHaveChildren(Object node) {
        Entity entity = getEntityFromNode(node);
        if (entity == null || entity.getEntityType() == null) return false;
        String code = entity.getEntityType().getCode();
        return EntityConstants.ENTITY_TYPE_COLLECTION.equals(code)
                || EntityConstants.ENTITY_TYPE_REFERENCE.equals(code)
                || EntityConstants.ENTITY_TYPE_CATEGORY.equals(code)
                || EntityConstants.ENTITY_TYPE_GROUP.equals(code);
    }

    /**
     * Indique si on sait que ce nœud n'a aucun enfant (chargement déjà fait, résultat 0).
     * Permet de masquer le toggler quand l'élément est déplié et n'a pas d'enfant.
     */
    public boolean isKnownToHaveNoChildren(Object node) {
        Entity entity = getEntityFromNode(node);
        return entity != null && entity.getId() != null
                && entityIdsWithNoChildren != null
                && entityIdsWithNoChildren.contains(entity.getId());
    }

    /** Retourne l'ID de l'entité du nœud (pour data-entity-id côté client). */
    public String getEntityIdFromNode(Object node) {
        Entity e = getEntityFromNode(node);
        return e != null && e.getId() != null ? String.valueOf(e.getId()) : "";
    }

    /** Retourne le code de l'entité du nœud (pour data-code côté client). */
    public String getEntityCodeFromNode(Object node) {
        Entity e = getEntityFromNode(node);
        return e != null && e.getCode() != null ? e.getCode() : "";
    }

    /** Indique si le nœud est actuellement sélectionné (surbrillance au rendu). */
    public boolean isNodeSelected(Object node) {
        if (selectedNode == null || node == null) return false;
        Entity nodeEntity = getEntityFromNode(node);
        Entity selectedEntity = getEntityFromNode(selectedNode);
        return nodeEntity != null && selectedEntity != null
                && nodeEntity.getId() != null && nodeEntity.getId().equals(selectedEntity.getId());
    }

    /**
     * Indique si l'élément du nœud est public (true) ou privé (false).
     * Par défaut true si pas d'entité.
     */
    public boolean isEntityPublic(Object node) {
        Entity e = getEntityFromNode(node);
        return e == null || Boolean.TRUE.equals(e.getPublique());
    }

    /**
     * Classe CSS pour l'indicateur public/privé du nœud (tree-node-public ou tree-node-private).
     */
    public String getEntityPublicCssClass(Object node) {
        return isEntityPublic(node) ? "tree-node-public" : "tree-node-private";
    }

    /**
     * Titre (tooltip) pour l'indicateur public/privé.
     */
    public String getEntityPublicTitle(Object node) {
        return isEntityPublic(node) ? "Public" : "Privé";
    }

    /**
     * Indique si le statut de l'entité est "candidat en cours de validation" (PROPOSITION).
     */
    public boolean isEntityStatusProposition(Object node) {
        Entity e = getEntityFromNode(node);
        return e != null && EntityStatusEnum.PROPOSITION.name().equals(e.getStatut());
    }

    /**
     * Indique si le statut de l'entité est validé (ACCEPTED ou AUTOMATIC).
     */
    public boolean isEntityStatusValidated(Object node) {
        Entity e = getEntityFromNode(node);
        if (e == null || e.getStatut() == null) return false;
        String s = e.getStatut();
        return EntityStatusEnum.ACCEPTED.name().equals(s) || EntityStatusEnum.AUTOMATIC.name().equals(s);
    }

    /**
     * Classe CSS pour l'indicateur de statut (tree-node-status-proposition ou tree-node-status-validated).
     * Retourne une chaîne vide si le statut n'est ni PROPOSITION ni ACCEPTED/AUTOMATIC.
     */
    public String getEntityStatusCssClass(Object node) {
        if (isEntityStatusProposition(node)) return "tree-node-status-proposition";
        if (isEntityStatusValidated(node)) return "tree-node-status-validated";
        return "";
    }

    /**
     * Titre (tooltip) pour l'indicateur de statut.
     */
    public String getEntityStatusTitle(Object node) {
        if (isEntityStatusProposition(node)) return "Candidat en cours de validation";
        if (isEntityStatusValidated(node)) return "Validée";
        return "";
    }

    /**
     * Sélectionne le nœud correspondant à une référence dans l'arbre
     * @param reference La référence à sélectionner
     */
    public void selectReferenceNode(Entity reference) {
        expandPathAndSelectEntity(reference);
    }

    /**
     * Déplie le chemin depuis la racine jusqu'à l'entité donnée et la sélectionne dans l'arbre.
     * Charge les enfants à chaque niveau si nécessaire (lazy load). À appeler quand on clique
     * sur une carte (catégorie, groupe, etc.) pour que l'arbre affiche le même élément sélectionné (même code).
     *
     * @param entity L'entité à atteindre et sélectionner (catégorie, groupe, référence, série, type, etc.)
     */
    public void expandPathAndSelectEntity(Entity entity) {
        if (entity == null || entity.getId() == null || root == null || entityRelationRepository == null) {
            return;
        }

        // Construire le chemin de l'entité vers la racine (ex. [catégorie, référentiel, collection])
        List<Entity> pathToRoot = new ArrayList<>();
        Entity e = entity;
        while (e != null) {
            pathToRoot.add(e);
            List<Entity> parents = entityRelationRepository.findParentsByChild(e);
            e = (parents == null || parents.isEmpty()) ? null : parents.get(0);
        }
        if (pathToRoot.isEmpty()) return;
        List<Entity> pathFromRoot = new ArrayList<>(pathToRoot);
        Collections.reverse(pathFromRoot);

        // La racine de l'arbre doit correspondre au premier élément du chemin (collection)
        if (root.getData() == null || !(root.getData() instanceof Entity rootEntity) ||
                !pathFromRoot.get(0).getId().equals(rootEntity.getId())) {
            log.debug("expandPathAndSelectEntity: racine de l'arbre ne correspond pas au chemin");
            return;
        }

        TreeNode current = root;
        for (int i = 1; i < pathFromRoot.size(); i++) {
            loadChildrenIfNeeded(current);
            Entity targetEntity = pathFromRoot.get(i);
            TreeNode child = findChildNodeByEntityId(current, targetEntity.getId());
            if (child == null) {
                log.warn("expandPathAndSelectEntity: nœud non trouvé pour l'entité {} (id={})", targetEntity.getCode(), targetEntity.getId());
                return;
            }
            current.setExpanded(true);
            current = child;
        }
        current.setExpanded(true);
        this.selectedNode = current;
        expandNodePath(current);
        log.debug("expandPathAndSelectEntity: nœud sélectionné : {} (id={})", entity.getCode(), entity.getId());
    }

    /** Trouve un enfant direct du nœud dont l'entité a l'ID donné. */
    private TreeNode findChildNodeByEntityId(TreeNode node, Long entityId) {
        if (node == null || entityId == null || node.getChildren() == null) return null;
        for (Object childObj : node.getChildren()) {
            if (childObj instanceof TreeNode child) {
                if (child.getData() != null && child.getData() instanceof Entity entity) {
                    if (entityId.equals(entity.getId())) return child;
                }
            }
        }
        return null;
    }

    /**
     * Étend tous les nœuds parents d'un nœud pour le rendre visible
     * Charge les enfants si nécessaire avant d'étendre
     * @param node Le nœud dont on veut étendre le chemin
     */
    private void expandNodePath(TreeNode node) {
        if (node == null) {
            return;
        }

        TreeNode parent = node.getParent();
        while (parent != null && parent != root) {
            // Charger les enfants si le nœud n'en a pas encore
            if (parent.getData() != null && parent.getData() instanceof Entity) {
                Entity parentEntity = (Entity) parent.getData();
                if (parent.getChildCount() == 0) {
                    loadChildForEntity(parent, parentEntity);
                }
            }
            parent.setExpanded(true);
            parent = parent.getParent();
        }
    }

    /**
     * Trouve récursivement un nœud dans l'arbre correspondant à une entité
     * @param node Le nœud à partir duquel commencer la recherche
     * @param entity L'entité à rechercher
     * @return Le TreeNode correspondant, ou null si non trouvé
     */
    private TreeNode findNodeByEntity(TreeNode node, Entity entity) {
        if (node == null || entity == null) {
            return null;
        }

        // Vérifier si ce nœud correspond à l'entité recherchée
        if (node.getData() != null && node.getData() instanceof Entity) {
            Entity nodeEntity = (Entity) node.getData();
            if (nodeEntity.getId() != null && entity.getId() != null &&
                nodeEntity.getId().equals(entity.getId())) {
                return node;
            }
        }

        // Parcourir récursivement les enfants
        if (node.getChildren() != null) {
            for (Object childObj : node.getChildren()) {
                if (childObj instanceof TreeNode) {
                    TreeNode child = (TreeNode) childObj;
                    TreeNode found = findNodeByEntity(child, entity);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }

        return null;
    }

}
