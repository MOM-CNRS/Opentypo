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
public class CategoryService implements Serializable {

    @Inject
    private EntityRelationRepository entityRelationRepository;


    /**
     * Charge les catégories rattachées au référentiel sélectionné
     */
    public List<Entity> loadCategoriesByReference(Entity selectedReference) {
        List<Entity> referenceCategories = new ArrayList<>();
        if (selectedReference != null) {
            try {
                // Essayer d'abord avec "CATEGORY" puis "CATEGORIE" pour compatibilité
                referenceCategories = entityRelationRepository.findChildrenByParentAndType(selectedReference,
                        EntityConstants.ENTITY_TYPE_CATEGORY);

                // Si aucune catégorie trouvée avec "CATEGORY", essayer avec "CATEGORIE"
                if (referenceCategories.isEmpty()) {
                    referenceCategories = entityRelationRepository.findChildrenByParentAndType(selectedReference, "CATEGORIE");
                }
            } catch (Exception e) {
                log.error("Erreur lors du chargement des catégories du référentiel", e);
                referenceCategories = new ArrayList<>();
            }
        }

        return referenceCategories;
    }
}
