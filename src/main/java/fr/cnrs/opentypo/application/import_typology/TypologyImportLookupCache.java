package fr.cnrs.opentypo.application.import_typology;

import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.SerieService;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Cache code → id pour éviter de recharger toute la hiérarchie à chaque ligne d'import.
 * Les entités sont rechargées par id (compatible avec {@code EntityManager.clear()} entre lots).
 */
final class TypologyImportLookupCache {

    private static final String KEY_SEP = "\u001F";

    private final EntityRepository entityRepository;
    private final Map<String, Long> categoryIdByCode = new HashMap<>();
    private final Map<String, Long> groupIdByKey = new HashMap<>();
    private final Map<String, Long> serieIdByKey = new HashMap<>();

    private TypologyImportLookupCache(EntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    static TypologyImportLookupCache warm(
            Entity reference,
            CategoryService categoryService,
            GroupService groupService,
            SerieService serieService,
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
                for (Entity ser : serieService.loadGroupSeries(grp)) {
                    if (!StringUtils.hasText(ser.getCode()) || ser.getId() == null) {
                        continue;
                    }
                    cache.serieIdByKey.put(sKey(cat.getCode(), grp.getCode(), ser.getCode()), ser.getId());
                }
            }
        }
        return cache;
    }

    Optional<Entity> findCategory(String catCode) {
        Long id = categoryIdByCode.get(catCode);
        return id == null ? Optional.empty() : entityRepository.findById(id);
    }

    Optional<Entity> findGroup(String catCode, String groupCode) {
        Long id = groupIdByKey.get(gKey(catCode, groupCode));
        return id == null ? Optional.empty() : entityRepository.findById(id);
    }

    Optional<Entity> findSerie(String catCode, String groupCode, String serieCode) {
        Long id = serieIdByKey.get(sKey(catCode, groupCode, serieCode));
        return id == null ? Optional.empty() : entityRepository.findById(id);
    }

    void registerCategory(Entity entity) {
        if (entity != null && StringUtils.hasText(entity.getCode()) && entity.getId() != null) {
            categoryIdByCode.put(entity.getCode(), entity.getId());
        }
    }

    void registerGroup(String catCode, Entity group) {
        if (StringUtils.hasText(catCode) && group != null
                && StringUtils.hasText(group.getCode()) && group.getId() != null) {
            groupIdByKey.put(gKey(catCode, group.getCode()), group.getId());
        }
    }

    void registerSerie(String catCode, String groupCode, Entity serie) {
        if (StringUtils.hasText(catCode) && StringUtils.hasText(groupCode) && serie != null
                && StringUtils.hasText(serie.getCode()) && serie.getId() != null) {
            serieIdByKey.put(sKey(catCode, groupCode, serie.getCode()), serie.getId());
        }
    }

    private static String gKey(String catCode, String groupCode) {
        return catCode + KEY_SEP + groupCode;
    }

    private static String sKey(String catCode, String groupCode, String serieCode) {
        return catCode + KEY_SEP + groupCode + KEY_SEP + serieCode;
    }
}
