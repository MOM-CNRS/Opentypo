package fr.cnrs.opentypo.application.import_typology;

import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.SerieService;
import fr.cnrs.opentypo.application.service.TypeService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Cache code → id pour éviter de recharger toute la hiérarchie à chaque ligne d'import.
 * Les entités sont rechargées par id (compatible avec {@code EntityManager.clear()} entre lots).
 */
final class TypologyImportLookupCache {

    private static final String KEY_SEP = "\u001F";

    private final EntityRepository entityRepository;
    private final Map<String, Long> categoryIdByCode = new HashMap<>();
    private final Map<String, Long> groupIdByKey = new HashMap<>();
    private final Map<String, Long> groupIdInReferenceByCode = new HashMap<>();
    private final Map<String, Long> serieIdByKey = new HashMap<>();
    private final Map<String, Long> serieIdByGroupId = new HashMap<>();
    private final Map<String, Long> typeIdByGroupKey = new HashMap<>();
    private final Map<String, Long> typeIdByGroupId = new HashMap<>();
    private final Map<Long, Entity> entityById = new HashMap<>();
    private final Map<Long, Long> parentIdByChildId = new HashMap<>();
    private final Set<String> knownLinks = new HashSet<>();

    private TypologyImportLookupCache(EntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    static TypologyImportLookupCache warm(
            Entity reference,
            CategoryService categoryService,
            GroupService groupService,
            SerieService serieService,
            TypeService typeService,
            EntityRepository entityRepository) {
        TypologyImportLookupCache cache = new TypologyImportLookupCache(entityRepository);
        for (Entity cat : categoryService.loadCategoriesByReference(reference)) {
            if (!StringUtils.hasText(cat.getCode()) || cat.getId() == null) {
                continue;
            }
            cache.categoryIdByCode.put(cat.getCode(), cat.getId());
            for (Entity grp : groupService.loadCategoryGroups(cat)) {
                if (!StringUtils.hasText(grp.getCode()) || grp.getId() == null) {
                    continue;
                }
                cache.groupIdByKey.put(gKey(cat.getCode(), grp.getCode()), grp.getId());
                cache.groupIdInReferenceByCode.putIfAbsent(grp.getCode(), grp.getId());
                for (Entity ser : serieService.loadGroupSeries(grp)) {
                    if (!StringUtils.hasText(ser.getCode()) || ser.getId() == null) {
                        continue;
                    }
                    cache.serieIdByKey.put(sKey(cat.getCode(), grp.getCode(), ser.getCode()), ser.getId());
                    cache.serieIdByGroupId.put(groupScopedKey(grp.getId(), ser.getCode()), ser.getId());
                    for (Entity typ : typeService.loadSerieTypes(ser)) {
                        if (!StringUtils.hasText(typ.getCode()) || typ.getId() == null) {
                            continue;
                        }
                        cache.typeIdByGroupKey.put(tKey(cat.getCode(), grp.getCode(), typ.getCode()), typ.getId());
                        cache.typeIdByGroupId.put(groupScopedKey(grp.getId(), typ.getCode()), typ.getId());
                    }
                }
                for (Entity typ : typeService.loadGroupTypes(grp)) {
                    if (!StringUtils.hasText(typ.getCode()) || typ.getId() == null) {
                        continue;
                    }
                    cache.typeIdByGroupKey.put(tKey(cat.getCode(), grp.getCode(), typ.getCode()), typ.getId());
                    cache.typeIdByGroupId.put(groupScopedKey(grp.getId(), typ.getCode()), typ.getId());
                }
            }
        }
        return cache;
    }

    void clearEntityInstances() {
        entityById.clear();
    }

    Optional<Entity> findCategory(String catCode) {
        return findEntityIdByCategoryCode(catCode).flatMap(this::findEntity);
    }

    Optional<Entity> findGroup(String catCode, String groupCode) {
        return findEntityIdByGroupKey(catCode, groupCode).flatMap(this::findEntity);
    }

    Optional<Entity> findSerie(String catCode, String groupCode, String serieCode) {
        return findEntityIdBySerieKey(catCode, groupCode, serieCode).flatMap(this::findEntity);
    }

    Optional<Long> findEntityIdByCategoryCode(String catCode) {
        return Optional.ofNullable(categoryIdByCode.get(catCode));
    }

    Optional<Long> findEntityIdByGroupKey(String catCode, String groupCode) {
        return Optional.ofNullable(groupIdByKey.get(gKey(catCode, groupCode)));
    }

    Optional<Long> findEntityIdByGroupInReference(String groupCode) {
        return Optional.ofNullable(groupIdInReferenceByCode.get(groupCode));
    }

    Optional<Long> findEntityIdBySerieKey(String catCode, String groupCode, String serieCode) {
        return Optional.ofNullable(serieIdByKey.get(sKey(catCode, groupCode, serieCode)));
    }

    Optional<Long> findEntityIdByTypeInGroup(String catCode, String groupCode, String typeCode) {
        return Optional.ofNullable(typeIdByGroupKey.get(tKey(catCode, groupCode, typeCode)));
    }

    Optional<Long> findExistingEntityId(
            TypologyImportKind kind,
            String targetCode,
            String catCode,
            String groupCode) {
        if (kind == null || targetCode == null || targetCode.isBlank()) {
            return Optional.empty();
        }
        return switch (kind) {
            case CATEGORIE -> findEntityIdByCategoryCode(targetCode);
            case GROUPE -> findEntityIdByGroupInReference(targetCode);
            case SERIE -> findEntityIdBySerieKey(catCode, groupCode, targetCode);
            case TYPE_SOUS_SERIE, TYPE_SOUS_GROUPE -> findEntityIdByTypeInGroup(catCode, groupCode, targetCode);
            default -> Optional.empty();
        };
    }

    Optional<Long> findExistingChildEntityId(Entity parent, String expectedTypeCode, String code) {
        if (parent == null || parent.getId() == null || !StringUtils.hasText(code)) {
            return Optional.empty();
        }
        return switch (expectedTypeCode) {
            case EntityConstants.ENTITY_TYPE_GROUP -> findEntityIdByGroupInReference(code);
            case EntityConstants.ENTITY_TYPE_SERIES ->
                    Optional.ofNullable(serieIdByGroupId.get(groupScopedKey(parent.getId(), code)));
            case EntityConstants.ENTITY_TYPE_TYPE ->
                    Optional.ofNullable(typeIdByGroupId.get(groupScopedKey(parent.getId(), code)));
            default -> Optional.empty();
        };
    }

    Optional<Entity> findEntity(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        Entity cached = entityById.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }
        return entityRepository.findById(id).map(entity -> {
            entityById.put(id, entity);
            return entity;
        });
    }

    void registerLoadedEntity(Entity entity) {
        if (entity != null && entity.getId() != null) {
            entityById.put(entity.getId(), entity);
        }
    }

    void registerCategory(Entity entity) {
        if (entity != null && StringUtils.hasText(entity.getCode()) && entity.getId() != null) {
            categoryIdByCode.put(entity.getCode(), entity.getId());
            registerLoadedEntity(entity);
        }
    }

    void registerGroup(String catCode, Entity group) {
        if (StringUtils.hasText(catCode) && group != null
                && StringUtils.hasText(group.getCode()) && group.getId() != null) {
            groupIdByKey.put(gKey(catCode, group.getCode()), group.getId());
            groupIdInReferenceByCode.putIfAbsent(group.getCode(), group.getId());
            registerLoadedEntity(group);
        }
    }

    void registerSerie(String catCode, String groupCode, Entity serie) {
        if (StringUtils.hasText(catCode) && StringUtils.hasText(groupCode) && serie != null
                && StringUtils.hasText(serie.getCode()) && serie.getId() != null) {
            serieIdByKey.put(sKey(catCode, groupCode, serie.getCode()), serie.getId());
            findEntityIdByGroupKey(catCode, groupCode).ifPresent(groupId ->
                    serieIdByGroupId.put(groupScopedKey(groupId, serie.getCode()), serie.getId()));
            registerLoadedEntity(serie);
        }
    }

    void registerType(String catCode, String groupCode, Entity type) {
        if (StringUtils.hasText(catCode) && StringUtils.hasText(groupCode) && type != null
                && StringUtils.hasText(type.getCode()) && type.getId() != null) {
            typeIdByGroupKey.put(tKey(catCode, groupCode, type.getCode()), type.getId());
            findEntityIdByGroupKey(catCode, groupCode).ifPresent(groupId ->
                    typeIdByGroupId.put(groupScopedKey(groupId, type.getCode()), type.getId()));
            registerLoadedEntity(type);
        }
    }

    Optional<Long> getKnownParentId(Long childId) {
        return Optional.ofNullable(parentIdByChildId.get(childId));
    }

    void registerParentChild(Long parentId, Long childId) {
        if (parentId != null && childId != null) {
            parentIdByChildId.put(childId, parentId);
            markLinked(parentId, childId);
        }
    }

    boolean isKnownLinked(Long parentId, Long childId) {
        return knownLinks.contains(linkKey(parentId, childId));
    }

    void markLinked(Long parentId, Long childId) {
        if (parentId != null && childId != null) {
            knownLinks.add(linkKey(parentId, childId));
        }
    }

    boolean isCodeRegisteredInScope(Entity parent, String expectedTypeCode, String code) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        return switch (expectedTypeCode) {
            case EntityConstants.ENTITY_TYPE_GROUP -> groupIdInReferenceByCode.containsKey(code);
            case EntityConstants.ENTITY_TYPE_SERIES -> parent != null && parent.getId() != null
                    && serieIdByGroupId.containsKey(groupScopedKey(parent.getId(), code));
            case EntityConstants.ENTITY_TYPE_TYPE -> parent != null && parent.getId() != null
                    && typeIdByGroupId.containsKey(groupScopedKey(parent.getId(), code));
            default -> false;
        };
    }

    private static String groupScopedKey(Long groupId, String code) {
        return groupId + KEY_SEP + code;
    }

    private static String linkKey(Long parentId, Long childId) {
        return parentId + KEY_SEP + childId;
    }

    private static String gKey(String catCode, String groupCode) {
        return catCode + KEY_SEP + groupCode;
    }

    private static String sKey(String catCode, String groupCode, String serieCode) {
        return catCode + KEY_SEP + groupCode + KEY_SEP + serieCode;
    }

    private static String tKey(String catCode, String groupCode, String typeCode) {
        return catCode + KEY_SEP + groupCode + KEY_SEP + typeCode;
    }
}
