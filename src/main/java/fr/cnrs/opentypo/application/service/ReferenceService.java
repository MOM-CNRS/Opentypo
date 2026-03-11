package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@Transactional
public class ReferenceService implements Serializable {

    @Inject
    private EntityRelationRepository entityRelationRepository;


    /**
     * Charge les référentiels rattachés à la collection sélectionnée, ordonnés par display_order puis par code.
     * Ordre par défaut : alphabétique croissant.
     */
    public List<Entity> loadReferencesByCollection(Entity collectionSelected) {
        List<EntityRelation> relations = entityRelationRepository.findRelationsByParentAndTypeOrdered(
                collectionSelected, EntityConstants.ENTITY_TYPE_REFERENCE);
        return relations.stream()
                .map(EntityRelation::getChild)
                .collect(Collectors.toList());
    }

    /**
     * Met à jour l'ordre d'affichage des référentiels pour une collection donnée.
     */
    public void updateDisplayOrder(Long parentId, List<Long> orderedChildIds) {
        if (parentId == null || orderedChildIds == null || orderedChildIds.isEmpty()) return;
        for (int i = 0; i < orderedChildIds.size(); i++) {
            final int order = i;
            entityRelationRepository.findByParentIdAndChildId(parentId, orderedChildIds.get(i))
                    .ifPresent(rel -> rel.setDisplayOrder(order));
        }
    }

    /**
     * Charge les enfants d'une entité parent, ordonnés par display_order puis par code.
     * Ordre par défaut alphabétique quand display_order est null.
     */
    public List<Entity> loadChildOfEntity(Entity parent) {
        if (parent != null) {
            return entityRelationRepository.findChildrenByParentOrdered(parent);
        }
        return new ArrayList<>();
    }
}
