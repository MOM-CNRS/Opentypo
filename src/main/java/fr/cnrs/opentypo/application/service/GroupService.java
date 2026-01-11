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
public class GroupService implements Serializable {

    @Inject
    private EntityRelationRepository entityRelationRepository;


    /**
     * Charge les groupes rattachés à la catégorie sélectionnée
     */
    public List<Entity> loadCategoryGroups(Entity selectedCategory) {
        List<Entity> categoryGroups = new ArrayList<>();
        if (selectedCategory != null) {
            try {
                // Essayer d'abord avec "GROUP" puis "GROUPE" pour compatibilité
                categoryGroups = entityRelationRepository.findChildrenByParentAndType(
                        selectedCategory,
                        EntityConstants.ENTITY_TYPE_GROUP
                );

                // Si aucun groupe trouvé avec "GROUP", essayer avec "GROUPE"
                if (categoryGroups.isEmpty()) {
                    categoryGroups = entityRelationRepository.findChildrenByParentAndType(
                            selectedCategory,
                            "GROUPE"
                    );
                }
            } catch (Exception e) {
                log.error("Erreur lors du chargement des groupes de la catégorie", e);
                categoryGroups = new ArrayList<>();
            }
        }

        return categoryGroups;
    }
}
