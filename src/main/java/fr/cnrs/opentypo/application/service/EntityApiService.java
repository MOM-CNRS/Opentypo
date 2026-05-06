package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.api.EntityCreateRequest;
import fr.cnrs.opentypo.application.dto.api.EntityResponseDto;
import fr.cnrs.opentypo.application.dto.api.EntityUpdateRequest;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Application service backing the REST API for {@link Entity} CRUD.
 */
@Service
@RequiredArgsConstructor
public class EntityApiService {

    private final EntityRepository entityRepository;
    private final EntityTypeRepository entityTypeRepository;
    private final LangueRepository langueRepository;
    private final EntityRelationRepository entityRelationRepository;
    private final EntityDeletionService entityDeletionService;

    @Transactional(readOnly = true)
    public EntityResponseDto getById(Long id) {
        Entity entity = entityRepository.findByIdForApi(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found"));
        return toDto(entity);
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
    public List<EntityResponseDto> lookupByField(String field, String match, String value, String labelLang) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "value is required");
        }
        String trimmed = value.trim();
        String f = field != null ? field.trim().toUpperCase(Locale.ROOT) : "";
        String m = match != null ? match.trim().toUpperCase(Locale.ROOT) : "";
        if (!"CODE".equals(f) && !"LABEL".equals(f)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "field must be CODE or LABEL");
        }
        if (!"EXACT".equals(m) && !"CONTAINS".equals(m)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "match must be EXACT or CONTAINS");
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown language code: " + lang);
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
                orderedUniqueIds.add(id);
            }
        }
        if (orderedUniqueIds.isEmpty()) {
            return List.of();
        }

        List<Entity> loaded = entityRepository.findByIdsForApi(orderedUniqueIds);
        Map<Long, Entity> byId = new HashMap<>(loaded.size() * 2);
        for (Entity e : loaded) {
            byId.put(e.getId(), e);
        }

        List<EntityResponseDto> out = new ArrayList<>(orderedUniqueIds.size());
        for (Long id : orderedUniqueIds) {
            Entity e = byId.get(id);
            if (e != null) {
                out.add(toDto(e));
            }
        }
        return out;
    }

    @Transactional
    public EntityResponseDto create(EntityCreateRequest request) {
        if (entityRepository.existsByCode(request.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An entity with this code already exists");
        }
        EntityType type = entityTypeRepository.findByCode(request.entityTypeCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown entity type: " + request.entityTypeCode()));
        String labelLangCode = request.labelLangCode();
        if (labelLangCode == null || labelLangCode.isBlank()) {
            labelLangCode = "fr";
        }
        Langue lang = langueRepository.findByCode(labelLangCode);
        if (lang == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown language code: " + labelLangCode);
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
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent entity not found: " + request.parentEntityId()));
            if (!entityRelationRepository.existsByParentAndChild(parent.getId(), saved.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(parent);
                relation.setChild(saved);
                entityRelationRepository.save(relation);
            }
        }

        return toDto(entityRepository.findByIdForApi(saved.getId()).orElse(saved));
    }

    @Transactional
    public EntityResponseDto update(Long id, EntityUpdateRequest request) {
        Entity entity = entityRepository.findByIdForApi(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found"));

        if (request.getStatut() != null) {
            entity.setStatut(request.getStatut());
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            String newCode = request.getCode().trim();
            if (!newCode.equals(entity.getCode())
                    && entityRepository.existsByCodeExcludingEntityId(newCode, entity.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An entity with this code already exists");
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

        entityRepository.save(entity);
        return toDto(entityRepository.findByIdForApi(id).orElse(entity));
    }

    @Transactional
    public void delete(Long id) {
        Entity entity = entityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found"));
        entityDeletionService.deleteEntityRecursively(entity);
    }

    private void applyCurrentUserAsCreator(Entity entity) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            entity.setCreateBy(auth.getName());
        }
    }

    private EntityResponseDto toDto(Entity entity) {
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
                entity.getCreateBy()
        );
    }
}
