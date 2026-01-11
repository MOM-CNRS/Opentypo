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
public class TypeService implements Serializable {

    @Inject
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
        List<Entity> groupTypes = new ArrayList<>();
        if (selectedGroup != null) {
            try {
                // Recherche dans la table entity_relation les enfants (types) du groupe parent
                // La requête SQL générée est : 
                // SELECT er.child FROM EntityRelation er 
                // WHERE er.parent = :parent AND er.child.entityType.code = :typeCode
                groupTypes = entityRelationRepository.findChildrenByParentAndType(
                        selectedGroup,
                        EntityConstants.ENTITY_TYPE_TYPE
                );
            } catch (Exception e) {
                log.error("Erreur lors du chargement des types du groupe depuis entity_relation", e);
                groupTypes = new ArrayList<>();
            }
        }

        return groupTypes;
    }
}
