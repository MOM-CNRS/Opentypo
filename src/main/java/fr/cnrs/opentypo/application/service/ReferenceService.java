package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@Transactional
public class ReferenceService implements Serializable {

    @Inject
    private EntityRelationRepository entityRelationRepository;


    /**
     * Charge les référentiels rattachés à la collection sélectionnée
     */
    public List<Entity> loadReferencesByCollection(Entity collectionSelected) {

        return entityRelationRepository.findChildrenByParentAndType(collectionSelected, EntityConstants.ENTITY_TYPE_REFERENCE);
    }

    /**
     * Charge les référentiels rattachés à la collection sélectionnée
     */
    public List<Entity> loadChildOfEntity(Entity parent) {

        if (parent != null) {
            return entityRelationRepository.findChildrenByParent(parent);
        }
        return new ArrayList<>();
    }
}
