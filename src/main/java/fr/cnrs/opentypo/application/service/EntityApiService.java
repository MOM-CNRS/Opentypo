package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.application.dto.api.ApiErrorMessages;
import fr.cnrs.opentypo.application.dto.api.EntityCreateRequest;
import fr.cnrs.opentypo.application.dto.api.EntityDescriptionSectionDto;
import fr.cnrs.opentypo.application.dto.api.EntityLabelDto;
import fr.cnrs.opentypo.application.dto.api.EntityPhysicalCharacteristicsSectionDto;
import fr.cnrs.opentypo.application.dto.api.EntityResponseDto;
import fr.cnrs.opentypo.application.dto.api.EntityApiStatutFilter;
import fr.cnrs.opentypo.application.dto.api.EntityListOrder;
import fr.cnrs.opentypo.application.dto.api.EntityTextDescriptionDto;
import fr.cnrs.opentypo.application.dto.api.EntityUpdateRequest;
import fr.cnrs.opentypo.application.dto.api.EntityVisibilityRequest;
import fr.cnrs.opentypo.application.mapper.EntityApiDetailMapper;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ImageRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Application service backing the REST API for {@link Entity} CRUD.
 */
@Service
@RequiredArgsConstructor
public class EntityApiService {

    private static final int DEFAULT_SEARCH_LIMIT = 200;
    private static final int MAX_SEARCH_LIMIT = 1000;
    private static final int DEFAULT_TYPOLOGY_LIMIT = 500;
    private static final int MAX_TYPOLOGY_LIMIT = 5000;

    private final ArkIdentifierService arkIdentifierService;

    private final EntityRepository entityRepository;
    private final EntityTypeRepository entityTypeRepository;
    private final LangueRepository langueRepository;
    private final EntityRelationRepository entityRelationRepository;
    private final EntityDeletionService entityDeletionService;
    private final ImageRepository imageRepository;
    private final EntityApiDetailMapper entityApiDetailMapper;
    private final EntityAuthorityService entityAuthorityService;
    private final UtilisateurRepository utilisateurRepository;
    private final EntityStatusCascadeService entityStatusCascadeService;

    @Transactional(readOnly = true)
    public EntityResponseDto getById(Long id) {
        Entity entity = entityRepository.findByIdForApi(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrorMessages.ENTITY_NOT_FOUND));
        return toDtoList(List.of(entity), true).getFirst();
    }

    @Transactional(readOnly = true)
    public EntityResponseDto getByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrorMessages.CODE_REQUIRED);
        }
        Entity entity = entityRepository.findByCodeForApi(code.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrorMessages.ENTITY_NOT_FOUND));
        return toDtoList(List.of(entity), true).getFirst();
    }

    @Transactional(readOnly = true)
    public List<EntityResponseDto> getChildren(Long id, EntityApiStatutFilter statutFilter) {
        Entity parent = entityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrorMessages.ENTITY_NOT_FOUND));
        List<Long> childIds = entityRelationRepository.findChildrenByParentOrdered(parent).stream()
                .map(Entity::getId)
                .toList();
        return toDtoList(filterByStatut(loadEntitiesInOrder(childIds), statutFilter));
    }

    @Transactional(readOnly = true)
    public List<EntityResponseDto> getParents(Long id, EntityApiStatutFilter statutFilter) {
        Entity child = entityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrorMessages.ENTITY_NOT_FOUND));
        List<Long> parentIds = entityRelationRepository.findParentsByChild(child).stream()
                .map(Entity::getId)
                .toList();
        return toDtoList(filterByStatut(loadEntitiesInOrder(parentIds), statutFilter));
    }

    /**
     * Liste ou recherche d'entités ({@code GET /api/v1/entities}).
     */
    @Transactional(readOnly = true)
    public List<EntityResponseDto> listEntities(
            String field,
            String match,
            String value,
            String statut,
            String lang,
            Integer limit,
            String order,
            Long rootId) {
        EntityApiStatutFilter statutFilter = EntityApiStatutFilter.parse(statut);
        int effectiveLimit = resolveLimit(limit);
        EntityListOrder listOrder = EntityListOrder.parse(order);
        Optional<Set<Long>> subtreeScope = resolveSubtreeScope(rootId);

        boolean hasPartialLookup = (field != null || match != null || value != null)
                && (field == null || match == null || value == null);
        if (hasPartialLookup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrorMessages.LOOKUP_PARAMS_INCOMPLETE);
        }
        if (field != null) {
            return lookupByField(field, match, value, lang, statutFilter, effectiveLimit, listOrder, subtreeScope);
        }
        return listByStatut(statutFilter, effectiveLimit, listOrder, subtreeScope);
    }

    /**
     * Liste les collections (typologies au sens plateforme) pour {@code GET /api/v1/typologies}.
     */
    @Transactional(readOnly = true)
    public List<EntityResponseDto> listCollections(EntityApiStatutFilter statutFilter, Integer limit) {
        EntityApiStatutFilter effectiveStatut = statutFilter.isFiltered()
                ? statutFilter
                : EntityApiStatutFilter.PUBLIQUE;
        int effectiveLimit = resolveTypologyLimit(limit);
        List<Long> ids = entityRepository.findIdsByEntityTypeForApi(
                EntityConstants.ENTITY_TYPE_COLLECTION,
                effectiveStatut.statutForQuery(),
                PageRequest.of(0, effectiveLimit));
        return toDtoList(loadEntitiesInOrder(ids));
    }

    /**
     * Recherche par code métier uniquement ou par libellé dans une langue, en mode exact ou « contient ».
     *
     * @param field   {@code CODE} ou {@code LABEL}
     * @param match   {@code EXACT} ou {@code CONTAINS}
     * @param value   texte recherché
     * @param labelLang code langue (obligatoire si field=LABEL, défaut {@code fr})
     */
    @Transactional(readOnly = true)
    private List<EntityResponseDto> lookupByField(
            String field,
            String match,
            String value,
            String labelLang,
            EntityApiStatutFilter statutFilter,
            int limit,
            EntityListOrder order,
            Optional<Set<Long>> subtreeScope) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrorMessages.VALUE_REQUIRED);
        }
        String trimmed = value.trim();
        String f = field != null ? field.trim().toUpperCase(Locale.ROOT) : "";
        String m = match != null ? match.trim().toUpperCase(Locale.ROOT) : "";
        if (!"CODE".equals(f) && !"LABEL".equals(f)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrorMessages.FIELD_MUST_BE_CODE_OR_LABEL);
        }
        if (!"EXACT".equals(m) && !"CONTAINS".equals(m)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrorMessages.MATCH_MUST_BE_EXACT_OR_CONTAINS);
        }

        List<Long> ids;
        if ("CODE".equals(f)) {
            if ("EXACT".equals(m)) {
                ids = entityRepository.findIdByMetadataCodeExactIgnoreCase(trimmed)
                        .map(id -> List.of(id))
                        .orElse(List.of());
            } else {
                ids = entityRepository.findIdsByMetadataCodeContaining(trimmed);
            }
        } else {
            String lang = labelLang != null && !labelLang.isBlank() ? labelLang.trim() : "fr";
            if (langueRepository.findByCode(lang) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrorMessages.unknownLanguageCode(lang));
            }
            if ("EXACT".equals(m)) {
                ids = entityRepository.findIdsByLabelExactInLang(trimmed, lang);
            } else {
                ids = entityRepository.findIdsByLabelContainingInLang(trimmed, lang);
            }
        }

        List<Long> orderedUniqueIds = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && seen.add(id)) {
                if (subtreeScope.isEmpty() || subtreeScope.get().contains(id)) {
                    orderedUniqueIds.add(id);
                }
            }
        }
        List<Entity> entities = filterByStatut(loadEntitiesInOrder(orderedUniqueIds), statutFilter);
        entities = order.sortEntities(entities);
        if (entities.size() > limit) {
            entities = entities.subList(0, limit);
        }
        return toDtoList(entities);
    }

    @Transactional(readOnly = true)
    public List<EntityResponseDto> listByStatut(
            EntityApiStatutFilter statutFilter,
            int limit,
            EntityListOrder order,
            Optional<Set<Long>> subtreeScope) {
        List<Long> ids = listIdsForApi(statutFilter, limit, order, subtreeScope);
        return toDtoList(loadEntitiesInOrder(ids));
    }

    @Transactional
    public EntityResponseDto create(EntityCreateRequest request) {
        Utilisateur currentUser = resolveCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ApiErrorMessages.UNAUTHORIZED));
        entityAuthorityService.assertCanCreate(
                currentUser, request.entityTypeCode().trim(), request.parentEntityId());

        if (entityRepository.existsByCode(request.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ApiErrorMessages.CODE_ALREADY_EXISTS);
        }
        EntityType type = entityTypeRepository.findByCode(request.entityTypeCode())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, ApiErrorMessages.unknownEntityType(request.entityTypeCode())));
        String labelLangCode = request.labelLangCode();
        if (labelLangCode == null || labelLangCode.isBlank()) {
            labelLangCode = "fr";
        }
        Langue lang = langueRepository.findByCode(labelLangCode);
        if (lang == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrorMessages.unknownLanguageCode(labelLangCode));
        }

        Entity entity = new Entity();
        entity.setEntityType(type);
        entity.setCode(request.code().trim());
        entity.setStatut(request.statut() != null && !request.statut().isBlank()
                ? request.statut()
                : EntityStatusEnum.PUBLIQUE.name());
        entity.setCreateDate(LocalDateTime.now());
        applyCurrentUserAsCreator(entity);

        Label label = new Label();
        label.setNom(request.labelNom().trim());
        label.setLangue(lang);
        label.setEntity(entity);
        entity.setLabels(new ArrayList<>(List.of(label)));

        Entity saved = entityRepository.save(entity);

        if (request.parentEntityId() != null) {
            Entity parent = entityRepository.findById(request.parentEntityId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, ApiErrorMessages.parentEntityNotFound(request.parentEntityId())));
            if (!entityRelationRepository.existsByParentAndChild(parent.getId(), saved.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(parent);
                relation.setChild(saved);
                entityRelationRepository.save(relation);
            }
        }

        Entity refreshed = entityRepository.findByIdForApi(saved.getId()).orElse(saved);
        return toDtoList(List.of(refreshed), true).getFirst();
    }

    @Transactional
    public EntityResponseDto update(Long id, EntityUpdateRequest request) {
        Utilisateur currentUser = resolveCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ApiErrorMessages.UNAUTHORIZED));
        Entity entity = entityRepository.findByIdForApi(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrorMessages.ENTITY_NOT_FOUND));
        entityAuthorityService.assertCanUpdate(currentUser, entity);

        if (request.getStatut() != null) {
            entity.setStatut(request.getStatut());
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            String newCode = request.getCode().trim();
            if (!newCode.equals(entity.getCode())
                    && entityRepository.existsByCodeExcludingEntityId(newCode, entity.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, ApiErrorMessages.CODE_ALREADY_EXISTS);
            }
            entity.setCode(newCode);
        }
        if (request.getIdArk() != null) {
            entity.setIdArk(request.getIdArk());
        }
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getAppellation() != null) {
            entity.setAppellation(request.getAppellation());
        }
        if (request.getBibliographie() != null) {
            entity.setBibliographie(request.getBibliographie());
        }
        if (request.getZoteroItemKeys() != null) {
            String z = request.getZoteroItemKeys().trim();
            entity.setZoteroItemKeys(z.isEmpty() ? null : z);
        }
        if (request.getMetadataCommentaire() != null) {
            entity.setMetadataCommentaire(request.getMetadataCommentaire());
        }

        if (EntityStatusEnum.PUBLIQUE.name().equals(entity.getStatut())) {
            arkIdentifierService.ensureArkIfAbsentForPublishedTypologyEntity(entity);
        }

        entityRepository.save(entity);
        Entity refreshed = entityRepository.findByIdForApi(id).orElse(entity);
        return toDtoList(List.of(refreshed), true).getFirst();
    }

    @Transactional
    public EntityResponseDto updateVisibility(Long id, EntityVisibilityRequest request) {
        Utilisateur currentUser = resolveCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ApiErrorMessages.UNAUTHORIZED));
        Entity entity = entityRepository.findByIdForApi(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrorMessages.ENTITY_NOT_FOUND));
        entityAuthorityService.assertCanChangeVisibility(currentUser, entity);

        if (EntityStatusEnum.PROPOSITION.name().equals(entity.getStatut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrorMessages.VISIBILITY_PROPOSITION_FORBIDDEN);
        }

        String newStatut = Boolean.TRUE.equals(request.publique())
                ? EntityStatusEnum.PUBLIQUE.name()
                : EntityStatusEnum.PRIVEE.name();
        entity.setStatut(newStatut);
        if (EntityStatusEnum.PUBLIQUE.name().equals(newStatut)) {
            arkIdentifierService.ensureArkIfAbsentForPublishedTypologyEntity(entity);
        }

        entityRepository.save(entity);
        Entity refreshed = entityRepository.findByIdForApi(id).orElse(entity);
        return toDtoList(List.of(refreshed), true).getFirst();
    }

    @Transactional
    public void delete(Long id) {
        Utilisateur currentUser = resolveCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ApiErrorMessages.UNAUTHORIZED));
        Entity entity = entityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrorMessages.ENTITY_NOT_FOUND));
        entityAuthorityService.assertCanDelete(currentUser, entity);
        entityDeletionService.deleteEntityRecursively(entity);
    }

    private static int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_SEARCH_LIMIT;
        }
        if (limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrorMessages.LIMIT_MIN_ONE);
        }
        return Math.min(limit, MAX_SEARCH_LIMIT);
    }

    private static int resolveTypologyLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_TYPOLOGY_LIMIT;
        }
        if (limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrorMessages.LIMIT_MIN_ONE);
        }
        return Math.min(limit, MAX_TYPOLOGY_LIMIT);
    }

    private static List<Entity> filterByStatut(List<Entity> entities, EntityApiStatutFilter statutFilter) {
        if (statutFilter == null || !statutFilter.isFiltered() || entities.isEmpty()) {
            return entities;
        }
        return entities.stream().filter(e -> statutFilter.matches(e.getStatut())).toList();
    }

    private Optional<Set<Long>> resolveSubtreeScope(Long rootId) {
        if (rootId == null) {
            return Optional.empty();
        }
        if (!entityRepository.existsById(rootId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, ApiErrorMessages.entityNotFound(rootId));
        }
        return Optional.of(entityStatusCascadeService.collectSelfAndDescendantIds(rootId));
    }

    private List<Long> listIdsForApi(
            EntityApiStatutFilter statutFilter,
            int limit,
            EntityListOrder order,
            Optional<Set<Long>> subtreeScope) {
        String statut = statutFilter.statutForQuery();
        Pageable pageable = order.usesJpaSort()
                ? order.toPageable(limit)
                : PageRequest.of(0, limit);
        if (subtreeScope.isPresent()) {
            Set<Long> entityIds = subtreeScope.get();
            if (entityIds.isEmpty()) {
                return List.of();
            }
            return switch (order) {
                case CODE_ASC -> entityRepository.listIdsForApiInSubtreeOrderByCodeAsc(entityIds, statut, pageable);
                case CODE_DESC -> entityRepository.listIdsForApiInSubtreeOrderByCodeDesc(entityIds, statut, pageable);
                default -> entityRepository.listIdsForApiInSubtree(entityIds, statut, pageable);
            };
        }
        return switch (order) {
            case CODE_ASC -> entityRepository.listIdsForApiOrderByCodeAsc(statut, pageable);
            case CODE_DESC -> entityRepository.listIdsForApiOrderByCodeDesc(statut, pageable);
            default -> entityRepository.listIdsForApi(statut, pageable);
        };
    }

    private List<Entity> loadEntitiesInOrder(List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return List.of();
        }
        List<Entity> loaded = entityRepository.findByIdsForApi(orderedIds);
        Map<Long, Entity> byId = new HashMap<>(loaded.size() * 2);
        for (Entity e : loaded) {
            byId.put(e.getId(), e);
        }
        List<Entity> ordered = new ArrayList<>(orderedIds.size());
        for (Long id : orderedIds) {
            Entity e = byId.get(id);
            if (e != null) {
                ordered.add(e);
            }
        }
        return ordered;
    }

    private List<EntityResponseDto> toDtoList(List<Entity> entities) {
        return toDtoList(entities, false);
    }

    private List<EntityResponseDto> toDtoList(List<Entity> entities, boolean includeDetailSections) {
        if (entities.isEmpty()) {
            return List.of();
        }
        List<Long> entityIds = entities.stream().map(Entity::getId).toList();
        Map<Long, List<Long>> parentsByChild = groupRelationIds(
                entityRelationRepository.findParentIdsByChildIds(entityIds), 0, 1);
        Map<Long, List<Long>> childrenByParent = groupRelationIds(
                entityRelationRepository.findChildIdsByParentIds(entityIds), 0, 1);
        Map<Long, String> primaryImageUrlByEntity = loadPrimaryImageUrls(entityIds);

        List<EntityResponseDto> out = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            Long id = entity.getId();
            List<EntityTextDescriptionDto> presentationDescriptions = null;
            EntityDescriptionSectionDto description = null;
            EntityPhysicalCharacteristicsSectionDto physicalCharacteristics = null;
            if (includeDetailSections) {
                presentationDescriptions = entityApiDetailMapper.loadPresentationDescriptions(id);
                description = entityApiDetailMapper.loadDescriptionSection(id, entity);
                physicalCharacteristics = entityApiDetailMapper.loadPhysicalCharacteristicsSection(id);
            }
            out.add(toDto(
                    entity,
                    parentsByChild.getOrDefault(id, List.of()),
                    childrenByParent.getOrDefault(id, List.of()),
                    primaryImageUrlByEntity.get(id),
                    presentationDescriptions,
                    description,
                    physicalCharacteristics));
        }
        return out;
    }

    private static Map<Long, List<Long>> groupRelationIds(List<Object[]> rows, int keyIndex, int valueIndex) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Long>> map = new HashMap<>();
        for (Object[] row : rows) {
            Long key = (Long) row[keyIndex];
            Long value = (Long) row[valueIndex];
            map.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        return map;
    }

    private Map<Long, String> loadPrimaryImageUrls(Collection<Long> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = imageRepository.findPrimaryImageUrlsByEntityIds(entityIds);
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> map = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            Number entityId = (Number) row[0];
            map.put(entityId.longValue(), (String) row[1]);
        }
        return map;
    }

    private static List<EntityLabelDto> toLabelDtos(Entity entity) {
        if (entity.getLabels() == null || entity.getLabels().isEmpty()) {
            return List.of();
        }
        List<EntityLabelDto> labels = new ArrayList<>(entity.getLabels().size());
        for (Label label : entity.getLabels()) {
            String langCode = label.getLangue() != null ? label.getLangue().getCode() : null;
            labels.add(new EntityLabelDto(langCode, label.getNom()));
        }
        return Collections.unmodifiableList(labels);
    }

    private Optional<Utilisateur> resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null
                || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return utilisateurRepository.findByEmail(auth.getName());
    }

    private void applyCurrentUserAsCreator(Entity entity) {
        resolveCurrentUser().ifPresent(user -> entity.setCreateBy(user.getEmail()));
    }

    private static EntityResponseDto toDto(
            Entity entity,
            List<Long> parentEntityIds,
            List<Long> childEntityIds,
            String primaryImageUrl,
            List<EntityTextDescriptionDto> presentationDescriptions,
            EntityDescriptionSectionDto description,
            EntityPhysicalCharacteristicsSectionDto physicalCharacteristics) {
        String typeCode = entity.getEntityType() != null ? entity.getEntityType().getCode() : null;
        return new EntityResponseDto(
                entity.getId(),
                entity.getCode(),
                typeCode,
                entity.getStatut(),
                entity.getNom(),
                entity.getIdArk(),
                entity.getDisplayOrder(),
                entity.getCreateDate(),
                entity.getCreateBy(),
                entity.getAppellation(),
                entity.getBibliographie(),
                entity.getZoteroItemKeys(),
                entity.getMetadataCommentaire(),
                entity.getCommentaireDatation(),
                entity.getTpq(),
                entity.getTaq(),
                primaryImageUrl,
                toLabelDtos(entity),
                List.copyOf(parentEntityIds),
                List.copyOf(childEntityIds),
                presentationDescriptions,
                description,
                physicalCharacteristics);
    }
}
