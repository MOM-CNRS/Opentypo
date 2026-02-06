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


@Slf4j
@Service
@Transactional
public class CollectionService implements Serializable {

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Autowired
    private EntityRepository entityRepository;


    /**
     * Retourne l'identifiant de la collection contenant l'entité d'ID donné.
     * On remonte l'arbre des relations parent-enfant jusqu'à trouver un ancêtre
     * de type COLLECTION. Si aucune collection n'est trouvée, retourne null.
     *
     * @param entityId identifiant de l'entité de départ
     * @return identifiant de la collection parente, ou null si non trouvé
     */
    public Entity findCollectionIdByEntityId(Long entityId) {
        if (entityId == null) {
            return null;
        }

        Entity current = entityRepository.findById(entityId).orElse(null);
        if (current == null) {
            return null;
        }

        while (current != null) {
            if (current.getEntityType() != null
                    && EntityConstants.ENTITY_TYPE_COLLECTION.equals(current.getEntityType().getCode())) {
                return current;
            }

            List<Entity> parents = entityRelationRepository.findParentsByChild(current);
            if (parents == null || parents.isEmpty()) {
                break;
            }
            // On prend le premier parent trouvé (structure hiérarchique en arbre)
            current = parents.get(0);
        }

        return null;
    }
}
