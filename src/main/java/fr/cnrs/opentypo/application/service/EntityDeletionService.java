package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.infrastructure.persistence.AuteurRepository;
import fr.cnrs.opentypo.infrastructure.persistence.CaracteristiquePhysiqueMonnaieRepository;
import fr.cnrs.opentypo.infrastructure.persistence.CaracteristiquePhysiqueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.CommentaireRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionDetailRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionMonnaieRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionPateRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityMetadataRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ImageRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LabelRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ParametrageRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Deletes an {@link Entity} and all dependent rows, in an order that respects FK constraints.
 * Same behaviour as the former {@code ApplicationBean#deleteEntityRecursively} implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntityDeletionService {

    private final EntityRepository entityRepository;
    private final EntityRelationRepository entityRelationRepository;
    private final AuteurRepository auteurRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final CommentaireRepository commentaireRepository;
    private final LabelRepository labelRepository;
    private final DescriptionRepository descriptionRepository;
    private final EntityImageService entityImageService;
    private final ImageRepository imageRepository;
    private final ParametrageRepository parametrageRepository;
    private final DescriptionDetailRepository descriptionDetailRepository;
    private final CaracteristiquePhysiqueRepository caracteristiquePhysiqueRepository;
    private final DescriptionPateRepository descriptionPateRepository;
    private final DescriptionMonnaieRepository descriptionMonnaieRepository;
    private final CaracteristiquePhysiqueMonnaieRepository caracteristiquePhysiqueMonnaieRepository;
    private final ReferenceOpenthesoRepository referenceOpenthesoRepository;
    private final EntityMetadataRepository entityMetadataRepository;

    /**
     * Recursively deletes an entity and all its children in the tree, then cleans related tables.
     */
    @Transactional
    public void deleteEntityRecursively(Entity entity) {
        if (entity == null || entity.getId() == null) {
            return;
        }

        Long entityId = entity.getId();
        String entityCode = entity.getCode();

        List<Entity> children = entityRelationRepository.findChildrenByParent(entity);
        for (Entity child : children) {
            deleteEntityRecursively(child);
        }

        List<EntityRelation> parentRelations = entityRelationRepository.findByParent(entity);
        if (parentRelations != null && !parentRelations.isEmpty()) {
            entityRelationRepository.deleteAll(parentRelations);
        }
        List<EntityRelation> childRelations = entityRelationRepository.findByChild(entity);
        if (childRelations != null && !childRelations.isEmpty()) {
            entityRelationRepository.deleteAll(childRelations);
        }

        auteurRepository.deleteByEntityId(entityId);
        userPermissionRepository.deleteByEntityId(entityId);
        commentaireRepository.deleteByEntityId(entityId);
        labelRepository.deleteByEntityId(entityId);
        descriptionRepository.deleteByEntityId(entityId);
        entityImageService.deletePhysicalFilesForEntity(entityId);
        imageRepository.deleteByEntityId(entityId);
        parametrageRepository.deleteByEntityId(entityId);

        descriptionDetailRepository.deleteByEntityId(entityId);
        caracteristiquePhysiqueRepository.deleteByEntityId(entityId);
        descriptionPateRepository.deleteByEntityId(entityId);
        descriptionMonnaieRepository.deleteByEntityId(entityId);
        caracteristiquePhysiqueMonnaieRepository.deleteByEntityId(entityId);

        referenceOpenthesoRepository.clearEntityPeriodeRefsToAires(entityId);
        referenceOpenthesoRepository.clearEntityProductionRefsToAires(entityId);
        referenceOpenthesoRepository.clearEntityCategorieFonctionnelleRefsToAires(entityId);
        referenceOpenthesoRepository.deleteByEntityId(entityId);

        entityMetadataRepository.deleteByEntityId(entityId);
        entityRepository.deleteByIdDirect(entityId);

        log.info("Entity deleted: {} (id={})", entityCode, entityId);
    }
}
