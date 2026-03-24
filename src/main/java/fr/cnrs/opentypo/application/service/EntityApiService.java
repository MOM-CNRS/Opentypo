package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.api.EntityCreateRequest;
import fr.cnrs.opentypo.application.dto.api.EntityResponseDto;
import fr.cnrs.opentypo.application.dto.api.EntitySearchCriteria;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
     * Collection GET: optional filters (AND). See {@link EntitySearchCriteria}.
     */
    @Transactional(readOnly = true)
    public Page<EntityResponseDto> search(EntitySearchCriteria criteria, Pageable pageable) {
        Page<Entity> page = entityRepository.searchEntities(
                criteria.typeCode(),
                criteria.statut(),
                criteria.code(),
                criteria.codeContains(),
                criteria.idArk(),
                criteria.q(),
                criteria.labelLang(),
                pageable);
        return page.map(this::toDto);
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
