package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Collecte les IDs d'une entité et de tout son sous-arbre dans {@code entity_relation}
 * (groupes → séries → types, etc.). Préfère la CTE PostgreSQL ; si le résultat ne reflète pas
 * les enfants réels ou si la requête échoue, parcourt l'arbre en largeur via JPA.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class EntityStatusCascadeService implements Serializable {

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Autowired
    private EntityRepository entityRepository;

    /**
     * @return l'ID racine et tous les descendants (aucun doublon, ordre d'insertion stable)
     */
    public Set<Long> collectSelfAndDescendantIds(Long rootId) {
        Set<Long> ids = new LinkedHashSet<>();
        if (rootId == null) {
            return ids;
        }
        ids.add(rootId);

        try {
            List<Object[]> relations = entityRelationRepository.findAllDescendantRelations(rootId);
            if (relations != null) {
                for (Object[] row : relations) {
                    if (row == null || row.length < 2) {
                        continue;
                    }
                    Long parentId = asLong(row[0]);
                    Long childId = asLong(row[1]);
                    if (parentId != null) {
                        ids.add(parentId);
                    }
                    if (childId != null) {
                        ids.add(childId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("CTE descendants échouée pour entity {}, repli BFS", rootId, e);
        }

        if (ids.size() == 1 && hasDirectChildren(rootId)) {
            log.warn("Descendants absents ou incomplets pour {}, repli BFS", rootId);
            collectDescendantsBreadthFirst(rootId, ids);
        }

        return ids;
    }

    private boolean hasDirectChildren(Long rootId) {
        return entityRepository.findById(rootId)
                .map(parent -> !entityRelationRepository.findChildrenByParent(parent).isEmpty())
                .orElse(false);
    }

    private void collectDescendantsBreadthFirst(Long rootId, Set<Long> ids) {
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long pid = queue.poll();
            entityRepository.findById(pid).ifPresent(parent -> {
                for (Entity child : entityRelationRepository.findChildrenByParent(parent)) {
                    if (child.getId() != null && ids.add(child.getId())) {
                        queue.add(child.getId());
                    }
                }
            });
        }
    }

    private static Long asLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Long l) {
            return l;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
