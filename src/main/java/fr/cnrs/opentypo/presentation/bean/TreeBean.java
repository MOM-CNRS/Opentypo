package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.GroupService;
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
     * Charge récursivement tous les enfants (référentiels, catégories, groupes)
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
                // Charger les référentiels de la collection
                applicationBean.refreshCollectionReferencesList();
                var references = applicationBean.getCollectionReferences();
                
                if (references != null && !references.isEmpty()) {
                    for (Entity reference : references) {
                        DefaultTreeNode referenceNode = new DefaultTreeNode(reference.getNom(), root);
                        // Stocker l'entité dans le nœud pour pouvoir la récupérer lors du clic
                        referenceNode.setData(reference);
                        // Ne pas étendre le nœud - l'arbre reste replié par défaut
                        
                        // Charger récursivement les catégories de ce référentiel
                        loadCategoriesRecursively(referenceNode, reference);
                        
                        // Restaurer la sélection si c'est la même entité
                        if (previouslySelectedEntity != null && 
                            previouslySelectedEntity.getId() != null &&
                            previouslySelectedEntity.getId().equals(reference.getId())) {
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
     * Charge récursivement toutes les catégories d'un référentiel et leurs groupes
     * @param referenceNode Le nœud référentiel dans l'arbre
     * @param reference L'entité référentiel
     */
    private void loadCategoriesRecursively(TreeNode referenceNode, Entity reference) {
        if (referenceNode == null || reference == null || categoryService == null) {
            return;
        }
        
        try {
            // Charger les catégories du référentiel
            List<Entity> categories = categoryService.loadCategoriesByReference(reference);
            
            if (categories != null && !categories.isEmpty()) {
                for (Entity category : categories) {
                    // Vérifier si la catégorie existe déjà dans l'arbre
                    boolean categoryExists = false;
                    if (referenceNode.getChildren() != null) {
                        for (Object childObj : referenceNode.getChildren()) {
                            if (childObj instanceof TreeNode) {
                                TreeNode childNode = (TreeNode) childObj;
                                if (childNode.getData() != null && childNode.getData() instanceof Entity) {
                                    Entity childEntity = (Entity) childNode.getData();
                                    if (childEntity.getId() != null && category.getId() != null &&
                                        childEntity.getId().equals(category.getId())) {
                                        categoryExists = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Ajouter la catégorie si elle n'existe pas déjà
                    if (!categoryExists) {
                        DefaultTreeNode categoryNode = new DefaultTreeNode(category.getNom(), referenceNode);
                        categoryNode.setData(category);
                        // Ne pas étendre le nœud - l'arbre reste replié par défaut
                        
                        // Charger récursivement les groupes de cette catégorie
                        loadGroupsRecursively(categoryNode, category);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement récursif des catégories pour le référentiel : {}", reference.getNom(), e);
        }
    }
    
    /**
     * Charge récursivement tous les groupes d'une catégorie
     * @param categoryNode Le nœud catégorie dans l'arbre
     * @param category L'entité catégorie
     */
    private void loadGroupsRecursively(TreeNode categoryNode, Entity category) {
        if (categoryNode == null || category == null || groupService == null) {
            return;
        }
        
        try {
            // Charger les groupes de la catégorie
            List<Entity> groups = groupService.loadCategoryGroups(category);
            
            if (groups != null && !groups.isEmpty()) {
                for (Entity group : groups) {
                    // Vérifier si le groupe existe déjà dans l'arbre
                    boolean groupExists = false;
                    if (categoryNode.getChildren() != null) {
                        for (Object childObj : categoryNode.getChildren()) {
                            if (childObj instanceof TreeNode) {
                                TreeNode childNode = (TreeNode) childObj;
                                if (childNode.getData() != null && childNode.getData() instanceof Entity) {
                                    Entity childEntity = (Entity) childNode.getData();
                                    if (childEntity.getId() != null && group.getId() != null &&
                                        childEntity.getId().equals(group.getId())) {
                                        groupExists = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Ajouter le groupe s'il n'existe pas déjà
                    if (!groupExists) {
                        DefaultTreeNode groupNode = new DefaultTreeNode(group.getNom(), categoryNode);
                        groupNode.setData(group);
                        // Les groupes sont des feuilles, pas besoin de les étendre
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement récursif des groupes pour la catégorie : {}", category.getNom(), e);
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
     * Charge toutes les catégories d'un référentiel dans l'arbre
     */
    public void loadCategoriesForReference(Entity reference) {
        if (root == null || reference == null || applicationBean == null) {
            return;
        }
        
        // Trouver le nœud du référentiel dans l'arbre
        TreeNode referenceNode = findNodeByEntity(root, reference);
        if (referenceNode == null) {
            log.warn("Nœud référentiel non trouvé pour charger les catégories : {}", reference.getNom());
            return;
        }
        
        // Charger les catégories depuis ApplicationBean
        applicationBean.refreshReferenceCategoriesList();
        var categories = applicationBean.getReferenceCategories();
        
        if (categories != null && !categories.isEmpty()) {
            // Vérifier si les catégories sont déjà dans l'arbre
            for (Entity category : categories) {
                // Vérifier si la catégorie existe déjà dans l'arbre
                boolean categoryExists = false;
                if (referenceNode.getChildren() != null) {
                    for (Object childObj : referenceNode.getChildren()) {
                        if (childObj instanceof TreeNode) {
                            TreeNode childNode = (TreeNode) childObj;
                            if (childNode.getData() != null && childNode.getData() instanceof Entity) {
                                Entity childEntity = (Entity) childNode.getData();
                                if (childEntity.getId() != null && category.getId() != null &&
                                    childEntity.getId().equals(category.getId())) {
                                    categoryExists = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                
                // Ajouter la catégorie si elle n'existe pas déjà
                if (!categoryExists) {
                    DefaultTreeNode categoryNode = new DefaultTreeNode(category.getNom(), referenceNode);
                    categoryNode.setData(category);
                }
            }
            
            // Ne pas étendre le nœud référentiel - l'arbre reste replié par défaut
        }
    }

    public void onNodeSelect(NodeSelectEvent event) {
        this.selectedNode = event.getTreeNode();

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
                        // Charger les catégories dans l'arbre pour cette référence
                        loadCategoriesForReference(entity);
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
            }
        }
    }

    public void onNodeExpand(NodeExpandEvent event) {
        // hook for future lazy loading; no-op for now
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
            return entity.getNom() != null ? entity.getNom() : "";
        }
        
        // Si c'est un TreeNode
        if (node instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) node;
            // Si le node a des données (Entity), récupérer le nom
            if (treeNode.getData() != null && treeNode.getData() instanceof Entity) {
                Entity entity = (Entity) treeNode.getData();
                return entity.getNom() != null ? entity.getNom() : "";
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
     * @param node Le nœud dont on veut étendre le chemin
     */
    private void expandNodePath(TreeNode node) {
        if (node == null) {
            return;
        }
        
        TreeNode parent = node.getParent();
        while (parent != null && parent != root) {
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
