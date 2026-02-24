package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@Transactional
public class GroupService implements Serializable {

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Autowired
    private EntityRepository entityRepository;


    /**
     * Charge les groupes rattachés à la catégorie sélectionnée
     */
    public List<Entity> loadCategoryGroups(Entity selectedCategory) {

        return entityRelationRepository.findChildrenByParentAndType(selectedCategory, EntityConstants.ENTITY_TYPE_GROUP);
    }

    /**
     * Retourne le groupe auquel l'entité d'ID donné est rattachée.
     * Si l'entité passée en paramètre est elle-même un groupe, on retourne cette entité.
     * Sinon, on remonte l'arbre des relations parent-enfant jusqu'à trouver un ancêtre de type GROUPE.
     *
     * @param entityId identifiant de l'entité de départ
     * @return le groupe rattaché à l'entité, ou Optional.empty() si non trouvé
     */
    public Optional<Entity> findGroupByEntityId(Long entityId) {
        if (entityId == null) {
            return Optional.empty();
        }

        Entity current = entityRepository.findById(entityId).orElse(null);
        if (current == null) {
            return Optional.empty();
        }

        while (current != null) {
            if (current.getEntityType() != null
                    && EntityConstants.ENTITY_TYPE_GROUP.equals(current.getEntityType().getCode())) {
                return Optional.of(current);
            }

            List<Entity> parents = entityRelationRepository.findParentsByChild(current);
            if (parents == null || parents.isEmpty()) {
                break;
            }
            current = parents.get(0);
        }

        return Optional.empty();
    }
}
