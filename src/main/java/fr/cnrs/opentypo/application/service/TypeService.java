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
public class TypeService implements Serializable {

    @Autowired
    private EntityRelationRepository entityRelationRepository;


    /**
     * Charge les types rattachés au groupe sélectionné, ordonnés par display_order puis par code.
     * Ordre par défaut : alphabétique croissant quand display_order est null.
     */
    public List<Entity> loadGroupTypes(Entity selectedGroup) {
        return loadTypesOrdered(selectedGroup, EntityConstants.ENTITY_TYPE_TYPE);
    }

    /**
     * Charge les types rattachés à la série sélectionnée, ordonnés par display_order puis par code.
     * Ordre par défaut : alphabétique croissant quand display_order est null.
     */
    public List<Entity> loadSerieTypes(Entity selectedSerie) {
        return loadTypesOrdered(selectedSerie, EntityConstants.ENTITY_TYPE_TYPE);
    }

    private List<Entity> loadTypesOrdered(Entity parent, String typeCode) {
        List<EntityRelation> relations = entityRelationRepository.findRelationsByParentAndTypeOrdered(parent, typeCode);
        return relations.stream()
                .map(EntityRelation::getChild)
                .collect(Collectors.toList());
    }

    /**
     * Met à jour l'ordre d'affichage des types pour un parent donné.
     * @param parentId ID du parent (série ou groupe)
     * @param orderedChildIds Liste des IDs des types dans l'ordre souhaité
     */
    public void updateTypesDisplayOrder(Long parentId, List<Long> orderedChildIds) {
        if (parentId == null || orderedChildIds == null || orderedChildIds.isEmpty()) return;
        for (int i = 0; i < orderedChildIds.size(); i++) {
            final int order = i;
            Long childId = orderedChildIds.get(i);
            entityRelationRepository.findByParentIdAndChildId(parentId, childId)
                    .ifPresent(rel -> rel.setDisplayOrder(order));
        }
    }
}
