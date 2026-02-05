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
public class TypeService implements Serializable {

    @Autowired
    private EntityRelationRepository entityRelationRepository;


    /**
     * Charge les types rattachés au groupe sélectionné depuis la table entity_relation
     * Les types sont des entités de type "TYPE" qui ont une relation parent-enfant
     * avec le groupe sélectionné dans la table entity_relation
     * 
     * @param selectedGroup Le groupe pour lequel charger les types
     * @return Liste des entités de type TYPE rattachées au groupe via entity_relation
     */
    public List<Entity> loadGroupTypes(Entity selectedGroup) {

        return entityRelationRepository.findChildrenByParentAndType(selectedGroup, EntityConstants.ENTITY_TYPE_TYPE);
    }

    /**
     * Charge les types rattachés à la série sélectionnée depuis la table entity_relation
     * Les types sont des entités de type "TYPE" qui ont une relation parent-enfant
     * avec la série sélectionnée dans la table entity_relation
     * 
     * @param selectedSerie La série pour laquelle charger les types
     * @return Liste des entités de type TYPE rattachées à la série via entity_relation
     */
    public List<Entity> loadSerieTypes(Entity selectedSerie) {
        return entityRelationRepository.findChildrenByParentAndType(selectedSerie, EntityConstants.ENTITY_TYPE_TYPE);
    }
}
