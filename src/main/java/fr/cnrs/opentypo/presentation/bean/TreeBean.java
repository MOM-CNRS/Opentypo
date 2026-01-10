package fr.cnrs.opentypo.presentation.bean;

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


@Getter
@Setter
@SessionScoped
@Named(value = "treeBean")
@Slf4j
public class TreeBean implements Serializable {

    @Inject
    private ApplicationBean applicationBean;

    private TreeNode selectedNode;
    private TreeNode root;


    @PostConstruct
    public void init() {
        // Racine (invisible)
        root = new DefaultTreeNode("root", null);
        // L'arbre sera initialisé dynamiquement quand une collection est sélectionnée
    }
    
    /**
     * Initialise l'arbre avec les référentiels de la collection sélectionnée
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
            root = collectionRoot;
        } else {
            root = new DefaultTreeNode("root", null);
        }
        
        selectedNode = null; // Réinitialiser la sélection
        
        if (selectedCollection != null) {
            try {
                // Charger les référentiels de la collection
                applicationBean.loadCollectionReferences();
                var references = applicationBean.getCollectionReferences();
                
                if (references != null && !references.isEmpty()) {
                    for (Entity reference : references) {
                        DefaultTreeNode node = new DefaultTreeNode(reference.getNom(), root);
                        // Stocker l'entité dans le nœud pour pouvoir la récupérer lors du clic
                        node.setData(reference);
                        
                        // Restaurer la sélection si c'est la même entité
                        if (previouslySelectedEntity != null && 
                            previouslySelectedEntity.getId() != null &&
                            previouslySelectedEntity.getId().equals(reference.getId())) {
                            selectedNode = node;
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
