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
public class SerieService implements Serializable {

    @Inject
    private EntityRelationRepository entityRelationRepository;


    /**
     * Charge les séries rattachées au groupe sélectionné depuis la table entity_relation
     * Les séries sont des entités de type "SERIE" ou "SERIES" qui ont une relation parent-enfant
     * avec le groupe sélectionné dans la table entity_relation
     * 
     * @param selectedGroup Le groupe pour lequel charger les séries
     * @return Liste des entités de type série rattachées au groupe via entity_relation
     */
    public List<Entity> loadGroupSeries(Entity selectedGroup) {
        List<Entity> groupSeries = new ArrayList<>();
        if (selectedGroup != null) {
            try {
                // Recherche dans la table entity_relation les enfants (séries) du groupe parent
                // La requête SQL générée est : 
                // SELECT er.child FROM EntityRelation er 
                // WHERE er.parent = :parent AND er.child.entityType.code = :typeCode
                // Essayer d'abord avec "SERIES" puis "SERIE" pour compatibilité
                groupSeries = entityRelationRepository.findChildrenByParentAndType(selectedGroup, EntityConstants.ENTITY_TYPE_SERIES);

                // Si aucune série trouvée avec "SERIES", essayer avec "SERIE"
                if (groupSeries.isEmpty()) {
                    groupSeries = entityRelationRepository.findChildrenByParentAndType(selectedGroup, "SERIE");
                }
            } catch (Exception e) {
                log.error("Erreur lors du chargement des séries du groupe depuis entity_relation", e);
                groupSeries = new ArrayList<>();
            }
        }

        return groupSeries;
    }
}
