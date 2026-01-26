package fr.cnrs.opentypo.presentation.bean.candidats;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.SearchBean;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Bean pour la gestion de l'arbre de références dans le wizard de création
 * Responsable du chargement et de la construction de l'arbre hiérarchique
 */
@Named("candidatReferenceTreeBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class CandidatReferenceTreeBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private LoginBean loginBean;

    @Inject
    private SearchBean searchBean;

    private Long selectedCollectionId;
    private Long selectedDirectEntityId;
    private String selectedLangueCode;
    private List<Entity> availableDirectEntities = new ArrayList<>();
    private TreeNode referenceTreeRoot;
    private TreeNode selectedTreeNode;

    /**
     * Charge les référentiels d'une collection sélectionnée depuis la base de données
     */
    public void loadReferencesForCollection() {
        log.debug("loadReferencesForCollection appelée - selectedCollectionId: {}", selectedCollectionId);
        
        referenceTreeRoot = null;
        selectedTreeNode = null;
        selectedDirectEntityId = null;
        
        loadDirectEntitiesForCollection();
        
        if (selectedCollectionId != null) {
            try {
                Entity refreshedCollection = entityRepository.findById(selectedCollectionId).orElse(null);
                
                if (refreshedCollection == null) {
                    log.warn("Collection avec l'ID {} non trouvée", selectedCollectionId);
                    return;
                }
                
                String collectionCode = refreshedCollection.getCode() != null ? refreshedCollection.getCode() : "Collection";
                DefaultTreeNode rootNode = new DefaultTreeNode(collectionCode, null);
                rootNode.setData(refreshedCollection);
                referenceTreeRoot = rootNode;
                
                List<Entity> allReferences = entityRelationRepository.findChildrenByParentAndType(
                    refreshedCollection, EntityConstants.ENTITY_TYPE_REFERENCE);
                
                boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
                List<Entity> filteredReferences = allReferences.stream()
                    .filter(r -> isAuthenticated || (r.getPublique() != null && r.getPublique()))
                    .collect(Collectors.toList());
                
                for (Entity reference : filteredReferences) {
                    DefaultTreeNode referenceNode = new DefaultTreeNode(reference.getNom(), referenceTreeRoot);
                    referenceNode.setData(reference);
                    loadChildrenRecursively(referenceNode, reference, isAuthenticated);
                }
                
                log.debug("Arbre construit pour la collection {}: {} référentiels", 
                    refreshedCollection.getCode(), filteredReferences.size());
            } catch (Exception e) {
                log.error("Erreur lors du chargement des référentiels de la collection", e);
            }
        }
        
        PrimeFaces.current().ajax().update(":createCandidatForm:referenceTreeContainer :createCandidatForm:directEntitySelect");
    }

    /**
     * Charge les entités directement rattachées à la collection
     */
    public void loadDirectEntitiesForCollection() {
        log.debug("loadDirectEntitiesForCollection appelée - selectedCollectionId: {}", selectedCollectionId);
        
        selectedDirectEntityId = null;
        availableDirectEntities = new ArrayList<>();
        
        if (selectedCollectionId != null) {
            try {
                Entity refreshedCollection = entityRepository.findById(selectedCollectionId).orElse(null);
                
                if (refreshedCollection == null) {
                    log.warn("Collection avec l'ID {} non trouvée", selectedCollectionId);
                    return;
                }
                
                List<Entity> allDirectEntities = entityRelationRepository.findChildrenByParent(refreshedCollection);
                
                boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
                availableDirectEntities = allDirectEntities.stream()
                    .filter(e -> isAuthenticated || (e.getPublique() != null && e.getPublique()))
                    .collect(Collectors.toList());
                
                log.debug("Entités directement rattachées chargées: {} entités", availableDirectEntities.size());
            } catch (Exception e) {
                log.error("Erreur lors du chargement des entités directement rattachées", e);
                availableDirectEntities = new ArrayList<>();
            }
        }
    }

    /**
     * Charge récursivement les enfants d'une entité pour construire l'arbre
     */
    private void loadChildrenRecursively(TreeNode parentNode, Entity parentEntity, boolean isAuthenticated) {
        if (parentEntity == null || parentNode == null) {
            return;
        }
        
        try {
            List<Entity> children = entityRelationRepository.findChildrenByParent(parentEntity);
            
            List<Entity> filteredChildren = children.stream()
                .filter(child -> {
                    // Filtrer selon l'authentification
                    return isAuthenticated || (child.getPublique() != null && child.getPublique());
                })
                .collect(Collectors.toList());
            
            for (Entity child : filteredChildren) {
                DefaultTreeNode childNode = new DefaultTreeNode(child.getNom(), parentNode);
                childNode.setData(child);
                // Charger récursivement les enfants de cet enfant
                loadChildrenRecursively(childNode, child, isAuthenticated);
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement récursif des enfants", e);
        }
    }

    /**
     * Récupère le label d'une entité pour l'affichage
     */
    public String getDirectEntityLabel(Entity entity) {
        if (entity == null) {
            return "";
        }
        if (selectedLangueCode != null && entity.getLabels() != null) {
            Optional<Label> labelOpt = entity.getLabels().stream()
                .filter(l -> selectedLangueCode.equals(l.getLangue().getCode()))
                .findFirst();
            if (labelOpt.isPresent() && labelOpt.get().getNom() != null && !labelOpt.get().getNom().trim().isEmpty()) {
                return entity.getCode() + " - " + labelOpt.get().getNom();
            }
        }
        return entity.getCode();
    }

    /**
     * Gère le changement de sélection d'une entité directe
     */
    public void onDirectEntityChange() {
        referenceTreeRoot = null;
        selectedTreeNode = null;
        
        if (selectedDirectEntityId != null) {
            try {
                Entity selectedEntity = entityRepository.findById(selectedDirectEntityId).orElse(null);
                if (selectedEntity != null) {
                    boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
                    DefaultTreeNode rootNode = new DefaultTreeNode(selectedEntity.getNom(), null);
                    rootNode.setData(selectedEntity);
                    referenceTreeRoot = rootNode;
                    loadChildrenRecursively(rootNode, selectedEntity, isAuthenticated);
                }
            } catch (Exception e) {
                log.error("Erreur lors du chargement de l'entité directe", e);
            }
        }
        
        PrimeFaces.current().ajax().update(":createCandidatForm:referenceTreeContainer");
    }

    /**
     * Vérifie si une collection est sélectionnée
     */
    public boolean isCollectionSelected() {
        return selectedCollectionId != null;
    }
}
