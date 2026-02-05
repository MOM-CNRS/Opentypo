package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;


@Slf4j
@Service
@Transactional
public class GroupService implements Serializable {

    @Autowired
    private EntityRelationRepository entityRelationRepository;


    /**
     * Charge les groupes rattachés à la catégorie sélectionnée
     */
    public List<Entity> loadCategoryGroups(Entity selectedCategory) {

        return entityRelationRepository.findChildrenByParentAndType(selectedCategory, EntityConstants.ENTITY_TYPE_GROUP);
    }
}
