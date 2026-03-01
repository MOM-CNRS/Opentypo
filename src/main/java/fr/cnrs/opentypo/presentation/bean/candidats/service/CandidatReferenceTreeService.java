package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion de l'arbre des référentiels dans le formulaire candidat.
 * Charge les collections, référentiels et entités pour l'étape 2 du wizard.
 */
@Service
@Slf4j
public class CandidatReferenceTreeService {

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    /**
     * Résultat du chargement des référentiels pour une collection.
     */
    public record ReferenceTreeResult(TreeNode treeRoot, List<Entity> directEntities) {}

    /**
     * Charge les référentiels d'une collection et construit l'arbre.
     * Filtre selon l'état d'authentification.
     */
    public ReferenceTreeResult loadReferencesForCollection(Long collectionId, boolean isAuthenticated) {
        if (collectionId == null) {
            return new ReferenceTreeResult(null, new ArrayList<>());
        }

        Entity refreshedCollection = entityRepository.findById(collectionId).orElse(null);
        if (refreshedCollection == null) {
            log.warn("Collection avec l'ID {} non trouvée", collectionId);
            return new ReferenceTreeResult(null, new ArrayList<>());
        }

        List<Entity> directEntities = loadDirectEntitiesForCollection(collectionId, isAuthenticated);

        String collectionCode = refreshedCollection.getCode() != null ? refreshedCollection.getCode() : "Collection";
        DefaultTreeNode rootNode = new DefaultTreeNode(collectionCode, null);
        rootNode.setData(refreshedCollection);

        List<Entity> allReferences = entityRelationRepository.findChildrenByParentAndType(
                refreshedCollection, EntityConstants.ENTITY_TYPE_REFERENCE);

        List<Entity> filteredReferences = allReferences.stream()
                .filter(r -> isAuthenticated || EntityStatusEnum.PUBLIQUE.name().equals(r.getStatut()))
                .toList();

        for (Entity reference : filteredReferences) {
            DefaultTreeNode referenceNode = new DefaultTreeNode(reference.getNom(), rootNode);
            referenceNode.setData(reference);
            loadChildrenRecursively(referenceNode, reference, isAuthenticated);
        }

        log.debug("Arbre construit pour la collection {}: {} référentiels", refreshedCollection.getCode(), filteredReferences.size());
        return new ReferenceTreeResult(rootNode, directEntities);
    }

    /**
     * Charge les entités directement rattachées à une collection.
     */
    public List<Entity> loadDirectEntitiesForCollection(Long collectionId, boolean isAuthenticated) {
        if (collectionId == null) {
            return new ArrayList<>();
        }

        Entity refreshedCollection = entityRepository.findById(collectionId).orElse(null);
        if (refreshedCollection == null) {
            return new ArrayList<>();
        }

        List<Entity> allDirectEntities = entityRelationRepository.findChildrenByParent(refreshedCollection);
        return allDirectEntities.stream()
                .filter(e -> isAuthenticated || EntityStatusEnum.PUBLIQUE.name().equals(e.getStatut()))
                .toList();
    }

    /**
     * Construit l'arbre à partir d'une entité directe sélectionnée (référentiel).
     */
    public TreeNode buildTreeFromDirectEntity(Long entityId, boolean isAuthenticated) {
        if (entityId == null) {
            return null;
        }

        Entity selectedReference = entityRepository.findById(entityId).orElse(null);
        if (selectedReference == null) {
            log.warn("Référence avec l'ID {} non trouvée", entityId);
            return null;
        }

        if (!isAuthenticated && !EntityStatusEnum.PUBLIQUE.name().equals(selectedReference.getStatut())) {
            log.debug("Référence {} non accessible (non publique)", selectedReference.getCode());
            return null;
        }

        String referenceLabel = selectedReference.getNom() != null ? selectedReference.getNom() : selectedReference.getCode();
        DefaultTreeNode rootNode = new DefaultTreeNode(referenceLabel, null);
        rootNode.setData(selectedReference);
        loadChildrenRecursively(rootNode, selectedReference, isAuthenticated);
        return rootNode;
    }

    private void loadChildrenRecursively(TreeNode parentNode, Entity parentEntity, boolean isAuthenticated) {
        try {
            List<Entity> children = entityRelationRepository.findChildrenByParent(parentEntity);
            List<Entity> filteredChildren = children.stream()
                    .filter(child -> isAuthenticated || EntityStatusEnum.PUBLIQUE.name().equals(child.getStatut()))
                    .toList();

            for (Entity child : filteredChildren) {
                DefaultTreeNode childNode = new DefaultTreeNode(child.getNom(), parentNode);
                childNode.setData(child);
                loadChildrenRecursively(childNode, child, isAuthenticated);
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des enfants de l'entité {}", parentEntity.getNom(), e);
        }
    }

    /**
     * Retourne le label d'une entité pour l'affichage.
     */
    public String getDirectEntityLabel(Entity entity, String selectedLangueCode) {
        if (entity == null) return "";
        if (selectedLangueCode != null && entity.getLabels() != null) {
            Optional<Label> labelOpt = entity.getLabels().stream()
                    .filter(l -> l.getLangue() != null && selectedLangueCode.equals(l.getLangue().getCode()))
                    .findFirst();
            if (labelOpt.isPresent()) {
                Label label = labelOpt.get();
                if (label.getNom() != null && !label.getNom().trim().isEmpty()) {
                    return entity.getCode() + " - " + label.getNom();
                }
            }
        }
        return entity.getCode() != null ? entity.getCode() : "";
    }

    /**
     * Retourne le nom du type d'entité pour l'affichage.
     */
    public String getEntityTypeName(EntityType entityType) {
        if (entityType == null) return "";
        return switch (entityType.getCode()) {
            case EntityConstants.ENTITY_TYPE_COLLECTION -> "Collection";
            case EntityConstants.ENTITY_TYPE_CATEGORY -> "Catégorie";
            case EntityConstants.ENTITY_TYPE_GROUP -> "Groupe";
            case EntityConstants.ENTITY_TYPE_SERIES -> "Série";
            case EntityConstants.ENTITY_TYPE_TYPE -> "Type";
            case EntityConstants.ENTITY_TYPE_REFERENCE -> "Référentiel";
            default -> entityType.getCode();
        };
    }

    /**
     * Extrait l'Entity du nœud (TreeNode ou Entity directement).
     */
    public Entity getEntityFromNode(Object node) {
        if (node == null) return null;
        if (node instanceof Entity entity) return entity;
        if (node instanceof TreeNode treeNode) {
            Object data = treeNode.getData();
            return data instanceof Entity entity ? entity : null;
        }
        return null;
    }

    /**
     * Retourne la valeur d'affichage d'un nœud.
     */
    public String getNodeDisplayValue(Object node) {
        Entity entity = getEntityFromNode(node);
        if (entity != null) {
            if (node instanceof TreeNode treeNode && treeNode.getParent() == null) {
                return entity.getCode() != null ? entity.getCode() : entity.getNom();
            }
            return entity.getCode();
        }
        if (node instanceof TreeNode treeNode) {
            Object data = treeNode.getData();
            return data != null ? data.toString() : node.toString();
        }
        return node != null ? node.toString() : "";
    }

    /**
     * Vérifie si le nœud est la racine.
     */
    public boolean isRootNode(Object node) {
        return node instanceof TreeNode treeNode && treeNode.getParent() == null;
    }

    /**
     * Vérifie si l'objet est une Entity.
     */
    public boolean isEntity(Object obj) {
        return obj instanceof Entity;
    }

    /**
     * Vérifie si le nœud contient une Entity.
     */
    public boolean isNodeEntity(Object node) {
        if (node == null) return false;
        if (node instanceof Entity) return true;
        return node instanceof TreeNode treeNode && treeNode.getData() instanceof Entity;
    }

    /**
     * Retourne le label d'une collection pour la langue donnée.
     */
    public String getCollectionLabel(Entity collection, String langCode) {
        if (collection == null) return "Aucune collection";
        if (langCode != null && collection.getLabels() != null) {
            Optional<Label> opt = collection.getLabels().stream()
                    .filter(l -> l.getLangue() != null && langCode.equalsIgnoreCase(l.getLangue().getCode()))
                    .findFirst();
            if (opt.isPresent()) return opt.get().getNom();
        }
        return collection.getCode() != null ? collection.getCode() : "";
    }
}
