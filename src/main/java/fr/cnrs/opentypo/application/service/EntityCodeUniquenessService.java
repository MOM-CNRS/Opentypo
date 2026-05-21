package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Vérifie l'unicité des codes métier selon le périmètre hiérarchique :
 * catégorie et groupe dans le référentiel ; série et type dans le groupe.
 */
@Service
@RequiredArgsConstructor
public class EntityCodeUniquenessService {

    private final EntityRepository entityRepository;
    private final GroupService groupService;
    private final TypeService typeService;

    public boolean isCategoryCodeTakenInReference(Entity reference, String code, Long excludeEntityId) {
        return findEntityIdInReference(reference, EntityConstants.ENTITY_TYPE_CATEGORY, code, excludeEntityId)
                .isPresent();
    }

    public boolean isGroupCodeTakenInReference(Entity reference, String code, Long excludeEntityId) {
        return findEntityIdInReference(reference, EntityConstants.ENTITY_TYPE_GROUP, code, excludeEntityId)
                .isPresent();
    }

    public boolean isSerieCodeTakenInGroup(Entity group, String code, Long excludeEntityId) {
        if (group == null || group.getId() == null || code == null || code.isBlank()) {
            return false;
        }
        return findEntityIdByTypedCodeInSubtree(
                group.getId(), EntityConstants.ENTITY_TYPE_SERIES, code.trim(), excludeEntityId)
                .isPresent();
    }

    public boolean isTypeCodeTakenInGroup(Entity group, String code, Long excludeEntityId) {
        if (group == null || group.getId() == null || code == null || code.isBlank()) {
            return false;
        }
        return findEntityIdByTypedCodeInSubtree(
                group.getId(), EntityConstants.ENTITY_TYPE_TYPE, code.trim(), excludeEntityId)
                .isPresent();
    }

    public Optional<Long> findEntityIdInReference(
            Entity reference, String entityTypeCode, String code, Long excludeEntityId) {
        if (reference == null || reference.getId() == null || code == null || code.isBlank()) {
            return Optional.empty();
        }
        return findEntityIdByTypedCodeInSubtree(
                reference.getId(), entityTypeCode, code.trim(), excludeEntityId);
    }

    public Optional<Long> findEntityIdInReference(Entity reference, String entityTypeCode, String code) {
        return findEntityIdInReference(reference, entityTypeCode, code, null);
    }

    public Optional<Long> findSerieIdInGroup(Entity group, String code) {
        if (group == null || group.getId() == null || code == null || code.isBlank()) {
            return Optional.empty();
        }
        return findEntityIdByTypedCodeInSubtree(
                group.getId(), EntityConstants.ENTITY_TYPE_SERIES, code.trim(), null);
    }

    public Optional<Long> findTypeIdInGroup(Entity group, String code) {
        if (group == null || group.getId() == null || code == null || code.isBlank()) {
            return Optional.empty();
        }
        return findEntityIdByTypedCodeInSubtree(
                group.getId(), EntityConstants.ENTITY_TYPE_TYPE, code.trim(), null);
    }

    public Optional<Entity> resolveGroup(Entity parent) {
        if (parent == null || parent.getId() == null) {
            return Optional.empty();
        }
        if (parent.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_GROUP.equals(parent.getEntityType().getCode())) {
            return Optional.of(parent);
        }
        return groupService.findGroupByEntityId(parent.getId());
    }

    public Optional<Entity> resolveReference(Entity parent) {
        if (parent == null) {
            return Optional.empty();
        }
        if (parent.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_REFERENCE.equals(parent.getEntityType().getCode())) {
            return Optional.of(parent);
        }
        return Optional.ofNullable(typeService.findReferenceAncestor(parent));
    }

    public boolean isCodeTakenForCreate(String entityTypeCode, Entity parent, String code, Long excludeEntityId) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return switch (entityTypeCode) {
            case EntityConstants.ENTITY_TYPE_CATEGORY -> resolveReference(parent)
                    .map(ref -> isCategoryCodeTakenInReference(ref, code, excludeEntityId))
                    .orElse(false);
            case EntityConstants.ENTITY_TYPE_GROUP -> resolveReference(parent)
                    .map(ref -> isGroupCodeTakenInReference(ref, code, excludeEntityId))
                    .orElse(false);
            case EntityConstants.ENTITY_TYPE_SERIES -> resolveGroup(parent)
                    .map(grp -> isSerieCodeTakenInGroup(grp, code, excludeEntityId))
                    .orElse(false);
            case EntityConstants.ENTITY_TYPE_TYPE -> resolveGroup(parent)
                    .map(grp -> isTypeCodeTakenInGroup(grp, code, excludeEntityId))
                    .orElse(false);
            default -> entityRepository.existsByCode(code.trim());
        };
    }

    public boolean isCodeTakenForUpdate(Entity entity, String newCode) {
        if (entity == null || newCode == null || newCode.isBlank()) {
            return false;
        }
        String trimmed = newCode.trim();
        if (trimmed.equals(entity.getCode())) {
            return false;
        }
        String typeCode = entity.getEntityType() != null ? entity.getEntityType().getCode() : null;
        if (typeCode == null) {
            return entityRepository.existsByCodeExcludingEntityId(trimmed, entity.getId());
        }
        Entity reference = typeService.findReferenceAncestor(entity);
        return switch (typeCode) {
            case EntityConstants.ENTITY_TYPE_CATEGORY -> reference != null
                    && isCategoryCodeTakenInReference(reference, trimmed, entity.getId());
            case EntityConstants.ENTITY_TYPE_GROUP -> reference != null
                    && isGroupCodeTakenInReference(reference, trimmed, entity.getId());
            case EntityConstants.ENTITY_TYPE_SERIES -> groupService.findGroupByEntityId(entity.getId())
                    .map(grp -> isSerieCodeTakenInGroup(grp, trimmed, entity.getId()))
                    .orElse(false);
            case EntityConstants.ENTITY_TYPE_TYPE -> groupService.findGroupByEntityId(entity.getId())
                    .map(grp -> isTypeCodeTakenInGroup(grp, trimmed, entity.getId()))
                    .orElse(false);
            default -> entityRepository.existsByCodeExcludingEntityId(trimmed, entity.getId());
        };
    }

    private Optional<Long> findEntityIdByTypedCodeInSubtree(
            Long rootId, String entityTypeCode, String code, Long excludeEntityId) {
        return entityRepository.findEntityIdByTypedCodeInSubtree(rootId, entityTypeCode, code, excludeEntityId);
    }
}
