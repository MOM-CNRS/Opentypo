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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@Transactional
public class TypeService implements Serializable {

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private SerieService serieService;


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
     * Vérifie si le parent (groupe ou série) a des types avec un ordre personnalisé.
     */
    public boolean hasCustomTypesOrder(Entity parent) {
        if (parent == null || parent.getId() == null) return false;
        return entityRelationRepository.hasCustomOrderForChildren(parent.getId(), EntityConstants.ENTITY_TYPE_TYPE);
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

    /**
     * Retourne la liste des groupes et séries pouvant être parents d'un type (même référentiel).
     */
    public List<Entity> getPossibleParentsForType(Entity type) {
        if (type == null) return new ArrayList<>();
        Entity reference = findReferenceAncestor(type);
        return getPossibleParentsForReference(reference);
    }

    /**
     * Retourne la liste des groupes et séries pouvant être parents d'un type pour un référentiel donné.
     */
    public List<Entity> getPossibleParentsForReference(Entity reference) {
        if (reference == null) return new ArrayList<>();
        List<Entity> result = new ArrayList<>();
        List<Entity> categories = categoryService.loadCategoriesByReference(reference);
        for (Entity category : categories) {
            List<Entity> groups = groupService.loadCategoryGroups(category);
            for (Entity group : groups) {
                result.add(group);
                List<Entity> series = serieService.loadGroupSeries(group);
                result.addAll(series);
            }
        }
        return result;
    }

    /** Retourne le référentiel ancêtre d'une entité (pour vérification des permissions). */
    public Entity findReferenceAncestor(Entity entity) {
        if (entity == null) return null;
        Entity current = entity;
        while (current != null) {
            if (current.getEntityType() != null
                    && EntityConstants.ENTITY_TYPE_REFERENCE.equals(current.getEntityType().getCode())) {
                return current;
            }
            List<Entity> parents = entityRelationRepository.findParentsByChild(current);
            if (parents == null || parents.isEmpty()) break;
            current = parents.get(0);
        }
        return null;
    }

    /**
     * Change le parent d'un type : supprime l'ancienne relation et crée la nouvelle.
     */
    public void changeTypeParent(Entity type, Entity newParent) {
        if (type == null || newParent == null) return;
        List<Entity> oldParents = entityRelationRepository.findParentsByChild(type);
        for (Entity oldParent : oldParents) {
            entityRelationRepository.findByParentIdAndChildId(oldParent.getId(), type.getId())
                    .ifPresent(entityRelationRepository::delete);
        }
        if (!entityRelationRepository.existsByParentAndChild(newParent.getId(), type.getId())) {
            EntityRelation relation = new EntityRelation();
            relation.setParent(newParent);
            relation.setChild(type);
            entityRelationRepository.save(relation);
        }
    }
}
