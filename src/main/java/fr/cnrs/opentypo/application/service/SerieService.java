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
public class SerieService implements Serializable {

    @Inject
    private EntityRelationRepository entityRelationRepository;


    /**
     * Charge les séries rattachées au groupe sélectionné, ordonnées par display_order puis par code.
     * Ordre par défaut : alphabétique croissant.
     */
    public List<Entity> loadGroupSeries(Entity selectedGroup) {
        if (selectedGroup == null) return new ArrayList<>();
        try {
            List<EntityRelation> relations = entityRelationRepository.findRelationsByParentAndTypeOrdered(
                    selectedGroup, EntityConstants.ENTITY_TYPE_SERIES);
            if (relations.isEmpty()) {
                relations = entityRelationRepository.findRelationsByParentAndTypeOrdered(selectedGroup, "SERIE");
            }
            return relations.stream().map(EntityRelation::getChild).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des séries du groupe depuis entity_relation", e);
            return new ArrayList<>();
        }
    }

    /**
     * Vérifie si le groupe a des séries avec un ordre personnalisé.
     */
    public boolean hasCustomSeriesOrder(Entity group) {
        if (group == null || group.getId() == null) return false;
        return entityRelationRepository.hasCustomOrderForChildren(group.getId(), EntityConstants.ENTITY_TYPE_SERIES);
    }

    /**
     * Met à jour l'ordre d'affichage des séries pour un groupe donné.
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
