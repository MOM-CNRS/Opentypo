package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 * Service pour construire l'arbre complet d'une collection (tous les niveaux) en une fois.
 * Utilise une CTE récursive en base et un chargement par lot pour de bonnes perfs.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class TreeService implements Serializable {

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private EntityRepository entityRepository;

    /**
     * Construit l'arbre avec uniquement la collection et ses enfants directs (un niveau).
     * Les enfants des nœuds seront chargés à la demande lors du dépliage.
     *
     * @param collection la collection racine (doit avoir un id)
     * @return le nœud racine PrimeFaces (avec data = collection), ou un nœud vide si erreur
     */
    public TreeNode buildRootWithDirectChildrenOnly(Entity collection) {
        if (collection == null || collection.getId() == null) {
            DefaultTreeNode empty = new DefaultTreeNode("root", null);
            return empty;
        }
        Long rootId = collection.getId();

        try {
            List<Entity> directChildren = entityRelationRepository.findChildrenByParentOrdered(collection);

            Entity rootEntity = collection;
            if (rootEntity.getEntityType() == null) {
                List<Entity> rootList = entityRepository.findByIdInWithEntityType(List.of(rootId));
                if (!rootList.isEmpty()) rootEntity = rootList.get(0);
            }

            String rootLabel = rootEntity.getNom() != null ? rootEntity.getNom() : "Collection";
            DefaultTreeNode rootNode = new DefaultTreeNode(rootLabel, null);
            rootNode.setData(rootEntity);

            for (Entity childEntity : directChildren) {
                String label = childEntity.getNom() != null ? childEntity.getNom() : childEntity.getCode();
                if (label == null) label = "?";
                DefaultTreeNode childNode = new DefaultTreeNode(label, rootNode);
                childNode.setData(childEntity);
            }

            log.debug("Arbre (enfants directs uniquement) chargé pour la collection {} : {} enfants",
                    rootId, directChildren.size());
            return rootNode;
        } catch (Exception e) {
            log.error("Erreur lors du chargement de l'arbre (enfants directs) pour la collection {}", rootId, e);
            DefaultTreeNode fallback = new DefaultTreeNode(collection.getNom() != null ? collection.getNom() : "Collection", null);
            fallback.setData(collection);
            return fallback;
        }
    }

    /**
     * Construit l'arbre complet (collection + tous les descendants) pour la collection donnée.
     * 1 requête récursive pour les relations, 1 requête pour toutes les entités.
     *
     * @param collection la collection racine (doit avoir un id)
     * @return le nœud racine PrimeFaces (avec data = collection), ou un nœud vide si erreur
     */
    public TreeNode buildFullSubtree(Entity collection) {
        if (collection == null || collection.getId() == null) {
            DefaultTreeNode empty = new DefaultTreeNode("root", null);
            return empty;
        }
        Long rootId = collection.getId();

        try {
            // 1) Toutes les relations (parent_id, child_id, display_order) du sous-arbre
            List<Object[]> relations = entityRelationRepository.findAllDescendantRelations(rootId);

            Set<Long> allIds = new HashSet<>();
            allIds.add(rootId);
            Map<Long, List<ChildOrder>> parentToChildOrders = new HashMap<>();

            for (Object[] row : relations) {
                Long parentId = asLong(row[0]);
                Long childId = asLong(row[1]);
                int displayOrder = asInt(row[2], 999999);
                if (parentId != null && childId != null) {
                    allIds.add(childId);
                    parentToChildOrders.computeIfAbsent(parentId, k -> new ArrayList<>()).add(new ChildOrder(childId, displayOrder));
                }
            }

            // 2) Charger toutes les entités en une fois (entityType inclus)
            List<Entity> entities = entityRepository.findByIdInWithEntityType(new ArrayList<>(allIds));
            Map<Long, Entity> entityMap = entities.stream().collect(Collectors.toMap(Entity::getId, e -> e));

            Entity rootEntity = entityMap.get(rootId);
            if (rootEntity == null) {
                rootEntity = collection;
            }

            String rootLabel = rootEntity.getNom() != null ? rootEntity.getNom() : "Collection";
            DefaultTreeNode rootNode = new DefaultTreeNode(rootLabel, null);
            rootNode.setData(rootEntity);

            buildChildren(rootNode, rootId, parentToChildOrders, entityMap);

            log.debug("Arbre complet chargé pour la collection {} : {} entités, {} relations",
                    rootId, entities.size(), relations.size());
            return rootNode;
        } catch (Exception e) {
            log.error("Erreur lors du chargement de l'arbre pour la collection {}", rootId, e);
            DefaultTreeNode fallback = new DefaultTreeNode(collection.getNom() != null ? collection.getNom() : "Collection", null);
            fallback.setData(collection);
            return fallback;
        }
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return null;
    }

    private static int asInt(Object o, int defaultValue) {
        if (o == null) return defaultValue;
        if (o instanceof Number n) return n.intValue();
        return defaultValue;
    }

    private record ChildOrder(long childId, int displayOrder) {}

    /** Retourne le libellé utilisé pour le tri (nom ou code), jamais null. */
    private static String getEntitySortLabel(Entity e) {
        if (e == null) return "";
        String s = e.getNom();
        if (s == null || s.isBlank()) s = e.getCode();
        return s != null ? s : "";
    }

    private void buildChildren(TreeNode parentNode, Long parentId,
                               Map<Long, List<ChildOrder>> parentToChildOrders,
                               Map<Long, Entity> entityMap) {
        List<ChildOrder> childOrders = parentToChildOrders.get(parentId);
        if (childOrders == null || childOrders.isEmpty()) return;

        // Tri par display_order puis alphabétique par libellé
        List<ChildOrder> sorted = new ArrayList<>(childOrders);
        sorted.sort(Comparator
                .comparingInt(ChildOrder::displayOrder)
                .thenComparing(co -> getEntitySortLabel(entityMap.get(co.childId())), String.CASE_INSENSITIVE_ORDER));

        for (ChildOrder co : sorted) {
            Long childId = co.childId();
            Entity childEntity = entityMap.get(childId);
            if (childEntity == null) continue;
            String label = childEntity.getNom() != null ? childEntity.getNom() : childEntity.getCode();
            if (label == null) label = "?";
            DefaultTreeNode childNode = new DefaultTreeNode(label, parentNode);
            childNode.setData(childEntity);
            buildChildren(childNode, childId, parentToChildOrders, entityMap);
        }
    }
}
