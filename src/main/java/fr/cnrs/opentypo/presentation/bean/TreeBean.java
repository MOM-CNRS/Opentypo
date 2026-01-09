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
        
        // Réinitialiser la racine
        root = new DefaultTreeNode("root", null);
        selectedNode = null; // Réinitialiser la sélection
        
        if (applicationBean != null && applicationBean.getSelectedCollection() != null) {
            try {
                // Charger les référentiels de la collection
                applicationBean.loadCollectionReferences();
                var referentiels = applicationBean.getCollectionReferences();
                
                if (referentiels != null && !referentiels.isEmpty()) {
                    for (Entity referentiel : referentiels) {
                        DefaultTreeNode node = new DefaultTreeNode(referentiel.getNom(), root);
                        // Stocker l'entité dans le nœud pour pouvoir la récupérer lors du clic
                        node.setData(referentiel);
                        
                        // Restaurer la sélection si c'est la même entité
                        if (previouslySelectedEntity != null && 
                            previouslySelectedEntity.getId() != null &&
                            previouslySelectedEntity.getId().equals(referentiel.getId())) {
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
    public void addReferentielToTree(Entity referentiel) {
        if (root != null && referentiel != null) {
            DefaultTreeNode node = new DefaultTreeNode(referentiel.getNom(), root);
            node.setData(referentiel);
        }
    }

    public void onNodeSelect(NodeSelectEvent event) {
        this.selectedNode = event.getTreeNode();

        // Récupérer l'entité stockée dans le nœud
        if (selectedNode != null && selectedNode.getData() != null) {
            Object data = selectedNode.getData();
            if (data instanceof Entity) {
                Entity entity = (Entity) data;

                // Vérifier si c'est un référentiel
                if (entity.getEntityType() != null &&
                    EntityConstants.ENTITY_TYPE_REFERENTIEL.equals(entity.getEntityType().getCode())) {
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

}
