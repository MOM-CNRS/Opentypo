package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@Transactional
public class CategoryService implements Serializable {

    @Autowired
    private EntityRelationRepository entityRelationRepository;


    /**
     * Charge les catégories rattachées au référentiel sélectionné, ordonnées par display_order puis par code.
     * Ordre par défaut : alphabétique croissant.
     */
    public List<Entity> loadCategoriesByReference(Entity selectedReference) {
        List<EntityRelation> relations = entityRelationRepository.findRelationsByParentAndTypeOrdered(
                selectedReference, EntityConstants.ENTITY_TYPE_CATEGORY);
        return relations.stream().map(EntityRelation::getChild).collect(Collectors.toList());
    }

    /**
     * Met à jour l'ordre d'affichage des catégories pour un référentiel donné.
     */
    public void updateDisplayOrder(Long parentId, List<Long> orderedChildIds) {
        if (parentId == null || orderedChildIds == null || orderedChildIds.isEmpty()) return;
        for (int i = 0; i < orderedChildIds.size(); i++) {
            final int order = i;
            entityRelationRepository.findByParentIdAndChildId(parentId, orderedChildIds.get(i))
                    .ifPresent(rel -> rel.setDisplayOrder(order));
        }
    }
}
