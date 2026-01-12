package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.SerieService;
import fr.cnrs.opentypo.application.service.TypeService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.NodeExpandEvent;
import java.io.Serializable;
import java.util.List;


@Getter
@Setter
@SessionScoped
@Named(value = "treeBean")
@Slf4j
public class TreeBean implements Serializable {

    @Inject
    private transient ApplicationBean applicationBean;

    @Inject
    private transient CategoryService categoryService;

    @Inject
    private transient GroupService groupService;

    @Inject
    private transient SerieService serieService;

    @Inject
    private transient TypeService typeService;

    private TreeNode selectedNode;
    private TreeNode root;

    private static final long serialVersionUID = 1L;


    @PostConstruct
    public void init() {
        // Racine (invisible)
        root = new DefaultTreeNode("root", null);
        // L'arbre sera initialisé dynamiquement quand une collection est sélectionnée
    }

    /**
     * Initialise l'arbre avec les référentiels de la collection sélectionnée
     * Charge uniquement le premier niveau (référentiels) - les enfants seront chargés à la demande
     */
    public void initializeTreeWithCollection() {
        // Sauvegarder l'entité sélectionnée avant la réinitialisation
        Entity previouslySelectedEntity = null;
        if (selectedNode != null && selectedNode.getData() != null && selectedNode.getData() instanceof Entity) {
            previouslySelectedEntity = (Entity) selectedNode.getData();
        }

        // Réinitialiser la racine avec le nom de la collection
        Entity selectedCollection = null;
        if (applicationBean != null && applicationBean.getSelectedCollection() != null) {
            selectedCollection = applicationBean.getSelectedCollection();
            String collectionName = selectedCollection.getNom() != null ? selectedCollection.getNom() : "Collection";
            DefaultTreeNode collectionRoot = new DefaultTreeNode(collectionName, null);
            // Stocker l'entité collection dans le nœud racine
            collectionRoot.setData(selectedCollection);
            // Ne pas étendre la racine - l'arbre reste replié par défaut
            root = collectionRoot;
        } else {
            root = new DefaultTreeNode("root", null);
        }

        selectedNode = null; // Réinitialiser la sélection

        if (selectedCollection != null) {
            try {
                // Charger uniquement les référentiels de la collection (niveau 1)
                // Les catégories seront chargées quand on dépliera un référentiel
                applicationBean.refreshCollectionReferencesList();
                var references = applicationBean.getCollectionReferences();

                if (references != null && !references.isEmpty()) {
                    for (Entity reference : references) {
                        DefaultTreeNode referenceNode = new DefaultTreeNode(reference.getNom(), root);
                        // Stocker l'entité dans le nœud pour pouvoir la récupérer lors du clic
                        referenceNode.setData(reference);
                        // Ne pas charger les catégories maintenant - elles seront chargées à la demande

                        // Restaurer la sélection si c'est la même entité
                        if (previouslySelectedEntity != null && previouslySelectedEntity.getId() != null && previouslySelectedEntity.getId().equals(reference.getId())) {
                            selectedNode = referenceNode;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de l'initialisation de l'arbre avec la collection", e);
            }
        }
    }


    /**
     * Ajoute un référentiel à l'arbre
     */
    public void addreferenceToTree(Entity reference) {
        if (root != null && reference != null) {
            DefaultTreeNode node = new DefaultTreeNode(reference.getNom(), root);
            node.setData(reference);
        }
    }

    /**
     * Ajoute une catégorie à l'arbre comme enfant d'un référentiel
     */
    public void addCategoryToTree(Entity category, Entity parentReference) {
        if (root != null && category != null && parentReference != null) {
            // Trouver le nœud du référentiel parent dans l'arbre
            TreeNode referenceNode = findNodeByEntity(root, parentReference);
            if (referenceNode != null) {
                DefaultTreeNode categoryNode = new DefaultTreeNode(category.getNom(), referenceNode);
                categoryNode.setData(category);
                // Ne pas étendre le nœud parent - l'arbre reste replié par défaut
            } else {
                log.warn("Nœud référentiel non trouvé pour ajouter la catégorie : {}", parentReference.getNom());
            }
        }
    }

    /**
     * Ajoute un groupe à l'arbre sous la catégorie parente
     */
    public void addGroupToTree(Entity group, Entity parentCategory) {
        if (root != null && group != null && parentCategory != null) {
            // Trouver le nœud de la catégorie parente dans l'arbre
            TreeNode categoryNode = findNodeByEntity(root, parentCategory);
            if (categoryNode != null) {
                DefaultTreeNode groupNode = new DefaultTreeNode(group.getNom(), categoryNode);
                groupNode.setData(group);
                // Ne pas étendre le nœud parent - l'arbre reste replié par défaut
            } else {
                log.warn("Nœud catégorie non trouvé pour ajouter le groupe : {}", parentCategory.getNom());
            }
        }
    }

    /**
     * Ajoute une série à l'arbre sous le groupe parent
     */
    public void addSerieToTree(Entity serie, Entity parentGroup) {
        if (root != null && serie != null && parentGroup != null) {
            // Trouver le nœud du groupe parent dans l'arbre
            TreeNode groupNode = findNodeByEntity(root, parentGroup);
            if (groupNode != null) {
                DefaultTreeNode serieNode = new DefaultTreeNode(serie.getNom(), groupNode);
                serieNode.setData(serie);
                // Ne pas étendre le nœud parent - l'arbre reste replié par défaut
            } else {
                log.warn("Nœud groupe non trouvé pour ajouter la série : {}", parentGroup.getNom());
            }
        }
    }

    /**
     * Ajoute un type à l'arbre sous le groupe parent
     */
    public void addTypeToTree(Entity type, Entity parentGroup) {
        if (root != null && type != null && parentGroup != null) {
            // Trouver le nœud du groupe parent dans l'arbre
            TreeNode groupNode = findNodeByEntity(root, parentGroup);
            if (groupNode != null) {
                DefaultTreeNode typeNode = new DefaultTreeNode(type.getNom(), groupNode);
                typeNode.setData(type);
                // Ne pas étendre le nœud parent - l'arbre reste replié par défaut
            } else {
                log.warn("Nœud groupe non trouvé pour ajouter le type : {}", parentGroup.getNom());
            }
        }
    }

    /**
     * Charge toutes les catégories d'un référentiel dans l'arbre
     * (Méthode conservée pour compatibilité, mais le chargement se fait maintenant via onNodeExpand)
     */
    public void loadCategoriesForReference(Entity reference) {
        if (root == null || reference == null) {
            return;
        }

        // Trouver le nœud du référentiel dans l'arbre
        TreeNode referenceNode = findNodeByEntity(root, reference);
        if (referenceNode == null) {
            log.warn("Nœud référentiel non trouvé pour charger les catégories : {}", reference.getNom());
            return;
        }

        // Charger les catégories si le nœud n'a pas encore d'enfants
        if (referenceNode.getChildCount() == 0) {
            loadCategoriesForReference(referenceNode, reference);
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

        // Récupérer l'entité stockée dans le nœud
        if (selectedNode != null && selectedNode.getData() != null) {
            Object data = selectedNode.getData();
            if (data instanceof Entity) {
                Entity entity = (Entity) data;

                // Vérifier si c'est une collection (racine)
                if (entity.getEntityType() != null &&
                    EntityConstants.ENTITY_TYPE_COLLECTION.equals(entity.getEntityType().getCode())) {
                    // Si on clique sur la collection, afficher les détails de la collection
                    if (applicationBean != null) {
                        applicationBean.showCollectionDetail(entity);
                    }
                }
                // Vérifier si c'est un référentiel
                else if (entity.getEntityType() != null &&
                    EntityConstants.ENTITY_TYPE_REFERENCE.equals(entity.getEntityType().getCode())) {
                    // Afficher la page référentiel
                    if (applicationBean != null) {
                        applicationBean.showReferenceDetail(entity);
                    }
                }
                // Vérifier si c'est une catégorie
                else if (entity.getEntityType() != null &&
                    (EntityConstants.ENTITY_TYPE_CATEGORY.equals(entity.getEntityType().getCode()) ||
                     "CATEGORIE".equals(entity.getEntityType().getCode()))) {
                    // Afficher la page catégorie
                    if (applicationBean != null) {
                        applicationBean.showCategoryDetail(entity);
                    }
                }
                // Vérifier si c'est un groupe
                else if (entity.getEntityType() != null &&
                    (EntityConstants.ENTITY_TYPE_GROUP.equals(entity.getEntityType().getCode()) ||
                     "GROUPE".equals(entity.getEntityType().getCode()))) {
                    // Afficher la page groupe
                    if (applicationBean != null) {
                        applicationBean.showGroupe(entity);
                    }
                }
                // Vérifier si c'est une série
                else if (entity.getEntityType() != null &&
                    (EntityConstants.ENTITY_TYPE_SERIES.equals(entity.getEntityType().getCode()) ||
                     "SERIE".equals(entity.getEntityType().getCode()))) {
                    // Afficher la page série
                    if (applicationBean != null) {
                        applicationBean.showSerie(entity);
                    }
                }
                // Vérifier si c'est un type
                else if (entity.getEntityType() != null &&
                    EntityConstants.ENTITY_TYPE_TYPE.equals(entity.getEntityType().getCode())) {
                    // Afficher la page type
                    if (applicationBean != null) {
                        applicationBean.showType(entity);
                    }
                }
            }
        }
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

        try {
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

            // Charger les enfants selon le type d'entité
            if (entity.getEntityType() != null) {
                String entityTypeCode = entity.getEntityType().getCode();

                // Si c'est une collection, charger les référentiels
                if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(entityTypeCode)) {
                    loadReferencesForCollection(node, entity);
                }
                // Si c'est un référentiel, charger les catégories
                else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entityTypeCode) ||
                         "REFERENTIEL".equals(entityTypeCode)) {
                    loadCategoriesForReference(node, entity);
                }
                // Si c'est une catégorie, charger les groupes
                else if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(entityTypeCode) ||
                         "CATEGORIE".equals(entityTypeCode)) {
                    loadGroupsForCategory(node, entity);
                }
                // Si c'est un groupe, charger les séries et types
                else if (EntityConstants.ENTITY_TYPE_GROUP.equals(entityTypeCode) ||
                         "GROUPE".equals(entityTypeCode)) {
                    loadSeriesAndTypesForGroup(node, entity);
                }
                
                int childCountAfter = node.getChildCount();
                log.debug("Après chargement, le nœud {} a maintenant {} enfants (avant: {})", 
                         entity.getNom(), childCountAfter, childCountBefore);
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des enfants du nœud : {}", entity.getNom(), e);
        }
    }

    /**
     * Charge les enfants d'un nœud quand il est déplié (lazy loading niveau par niveau)
     */
    public void onNodeExpand(NodeExpandEvent event) {
        TreeNode expandedNode = event.getTreeNode();
        // Charger les enfants si nécessaire
        loadChildrenIfNeeded(expandedNode);
    }

    /**
     * Charge les référentiels d'une collection
     * Recherche les référentiels rattachés et les ajoute comme enfants du nœud collection
     */
    private void loadReferencesForCollection(TreeNode collectionNode, Entity collection) {
        if (applicationBean == null) {
            return;
        }

        try {
            // Rechercher les référentiels rattachés à la collection dans la base de données
            applicationBean.refreshCollectionReferencesList();
            var references = applicationBean.getCollectionReferences();

            if (references != null && !references.isEmpty()) {
                for (Entity reference : references) {
                    // Vérifier si le référentiel existe déjà pour éviter les doublons
                    boolean exists = false;
                    if (collectionNode.getChildren() != null) {
                        for (Object childObj : collectionNode.getChildren()) {
                            if (childObj instanceof TreeNode) {
                                TreeNode childNode = (TreeNode) childObj;
                                if (childNode.getData() != null && childNode.getData() instanceof Entity) {
                                    Entity childEntity = (Entity) childNode.getData();
                                    if (childEntity.getId() != null && reference.getId() != null &&
                                        childEntity.getId().equals(reference.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // Ajouter le référentiel seulement s'il n'existe pas déjà
                    if (!exists) {
                        DefaultTreeNode referenceNode = new DefaultTreeNode(reference.getNom(), collectionNode);
                        referenceNode.setData(reference);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des référentiels pour la collection : {}", collection.getNom(), e);
        }
    }

    /**
     * Charge les catégories d'un référentiel
     * Recherche les catégories rattachées et les ajoute comme enfants du nœud référentiel
     */
    private void loadCategoriesForReference(TreeNode referenceNode, Entity reference) {
        if (categoryService == null) {
            return;
        }

        try {
            // Rechercher les catégories rattachées au référentiel dans la base de données
            List<Entity> categories = categoryService.loadCategoriesByReference(reference);

            if (categories != null && !categories.isEmpty()) {
                for (Entity category : categories) {
                    // Vérifier si la catégorie existe déjà pour éviter les doublons
                    boolean exists = false;
                    if (referenceNode.getChildren() != null) {
                        for (Object childObj : referenceNode.getChildren()) {
                            if (childObj instanceof TreeNode) {
                                TreeNode childNode = (TreeNode) childObj;
                                if (childNode.getData() != null && childNode.getData() instanceof Entity) {
                                    Entity childEntity = (Entity) childNode.getData();
                                    if (childEntity.getId() != null && category.getId() != null &&
                                        childEntity.getId().equals(category.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // Ajouter la catégorie seulement si elle n'existe pas déjà
                    if (!exists) {
                        DefaultTreeNode categoryNode = new DefaultTreeNode(category.getNom(), referenceNode);
                        categoryNode.setData(category);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des catégories pour le référentiel : {}", reference.getNom(), e);
        }
    }

    /**
     * Charge les groupes d'une catégorie
     * Recherche les groupes rattachés et les ajoute comme enfants du nœud catégorie
     */
    private void loadGroupsForCategory(TreeNode categoryNode, Entity category) {
        if (groupService == null) {
            return;
        }

        try {
            // Rechercher les groupes rattachés à la catégorie dans la base de données
            List<Entity> groups = groupService.loadCategoryGroups(category);

            if (groups != null && !groups.isEmpty()) {
                for (Entity group : groups) {
                    // Vérifier si le groupe existe déjà pour éviter les doublons
                    boolean exists = false;
                    if (categoryNode.getChildren() != null) {
                        for (Object childObj : categoryNode.getChildren()) {
                            if (childObj instanceof TreeNode) {
                                TreeNode childNode = (TreeNode) childObj;
                                if (childNode.getData() != null && childNode.getData() instanceof Entity) {
                                    Entity childEntity = (Entity) childNode.getData();
                                    if (childEntity.getId() != null && group.getId() != null &&
                                        childEntity.getId().equals(group.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // Ajouter le groupe seulement s'il n'existe pas déjà
                    if (!exists) {
                        DefaultTreeNode groupNode = new DefaultTreeNode(group.getNom(), categoryNode);
                        groupNode.setData(group);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des groupes pour la catégorie : {}", category.getNom(), e);
        }
    }

    /**
     * Charge les séries et types d'un groupe
     * Recherche les séries et types rattachés et les ajoute comme enfants du nœud groupe
     */
    private void loadSeriesAndTypesForGroup(TreeNode groupNode, Entity group) {
        if (serieService == null || typeService == null) {
            return;
        }

        try {
            // Rechercher les séries rattachées au groupe dans la base de données
            List<Entity> series = serieService.loadGroupSeries(group);
            if (series != null && !series.isEmpty()) {
                for (Entity serie : series) {
                    // Vérifier si la série existe déjà pour éviter les doublons
                    boolean exists = false;
                    if (groupNode.getChildren() != null) {
                        for (Object childObj : groupNode.getChildren()) {
                            if (childObj instanceof TreeNode) {
                                TreeNode childNode = (TreeNode) childObj;
                                if (childNode.getData() != null && childNode.getData() instanceof Entity) {
                                    Entity childEntity = (Entity) childNode.getData();
                                    if (childEntity.getId() != null && serie.getId() != null &&
                                        childEntity.getId().equals(serie.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // Ajouter la série seulement si elle n'existe pas déjà
                    if (!exists) {
                        DefaultTreeNode serieNode = new DefaultTreeNode(serie.getNom(), groupNode);
                        serieNode.setData(serie);
                    }
                }
            }

            // Rechercher les types rattachés au groupe dans la base de données
            List<Entity> types = typeService.loadGroupTypes(group);
            if (types != null && !types.isEmpty()) {
                for (Entity type : types) {
                    // Vérifier si le type existe déjà pour éviter les doublons
                    boolean exists = false;
                    if (groupNode.getChildren() != null) {
                        for (Object childObj : groupNode.getChildren()) {
                            if (childObj instanceof TreeNode) {
                                TreeNode childNode = (TreeNode) childObj;
                                if (childNode.getData() != null && childNode.getData() instanceof Entity) {
                                    Entity childEntity = (Entity) childNode.getData();
                                    if (childEntity.getId() != null && type.getId() != null &&
                                        childEntity.getId().equals(type.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // Ajouter le type seulement s'il n'existe pas déjà
                    if (!exists) {
                        DefaultTreeNode typeNode = new DefaultTreeNode(type.getNom(), groupNode);
                        typeNode.setData(type);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des séries et types pour le groupe : {}", group.getNom(), e);
        }
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

        String entityTypeCode = entity.getEntityType().getCode();
        
        // Retourner l'icône selon le type d'entité
        if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(entityTypeCode)) {
            return "fa fa-folder-open";
        } else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entityTypeCode) ||
                   "REFERENTIEL".equals(entityTypeCode)) {
            return "fa fa-book";
        } else if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(entityTypeCode) ||
                   "CATEGORIE".equals(entityTypeCode)) {
            return "fa fa-tags";
        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(entityTypeCode) ||
                   "GROUPE".equals(entityTypeCode)) {
            return "fa fa-users";
        } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(entityTypeCode) ||
                   "SERIE".equals(entityTypeCode)) {
            return "fa fa-list";
        } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(entityTypeCode)) {
            return "fa fa-tag";
        }
        
        // Icône par défaut
        return "fa fa-file";
    }

    /**
     * Retourne le nom de l'entité depuis un TreeNode ou directement depuis une Entity
     * Utilisé dans l'expression EL pour afficher le nom dans l'arbre
     */
    public String getNodeCode(Object node) {
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
     * Sélectionne le nœud correspondant à une référence dans l'arbre
     * @param reference La référence à sélectionner
     */
    public void selectReferenceNode(Entity reference) {
        if (reference == null || root == null) {
            return;
        }

        // Parcourir récursivement l'arbre pour trouver le nœud correspondant
        var foundNode = findNodeByEntity(root, reference);
        if (foundNode != null) {
            this.selectedNode = foundNode;
            // S'assurer que le nœud parent est étendu pour que le nœud soit visible
            expandNodePath(foundNode);
            log.debug("Nœud de référence sélectionné : {}", reference.getNom());
        } else {
            log.warn("Nœud de référence non trouvé pour : {}", reference.getNom());
        }
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
                    // Charger les enfants avant d'étendre
                    if (parentEntity.getEntityType() != null) {
                        String entityTypeCode = parentEntity.getEntityType().getCode();
                        if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(entityTypeCode)) {
                            loadReferencesForCollection(parent, parentEntity);
                        } else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entityTypeCode) ||
                                   "REFERENTIEL".equals(entityTypeCode)) {
                            loadCategoriesForReference(parent, parentEntity);
                        } else if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(entityTypeCode) ||
                                   "CATEGORIE".equals(entityTypeCode)) {
                            loadGroupsForCategory(parent, parentEntity);
                        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(entityTypeCode) ||
                                   "GROUPE".equals(entityTypeCode)) {
                            loadSeriesAndTypesForGroup(parent, parentEntity);
                        }
                    }
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
