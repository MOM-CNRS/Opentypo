package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Qui peut publier ou refuser une fiche « Type » : administrateurs fonctionnel/technique,
 * valideur rattaché au type ou au groupe contenant le type, gestionnaire du référentiel parent.
 */
@Service
public class TypeValidationAuthorityService {

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private GroupService groupService;

    @Autowired
    private TypeService typeService;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    /**
     * @param typeEntityId identifiant de l’entité de type TYPE (la fiche type)
     */
    public boolean canUserValidateOrRefuseType(Long typeEntityId, Utilisateur user) {
        if (typeEntityId == null || user == null) {
            return false;
        }
        if (isAdminTechniqueOrFonctionnel(user)) {
            return true;
        }

        Entity typeEntity = entityRepository.findById(typeEntityId).orElse(null);
        if (typeEntity == null || typeEntity.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_TYPE.equals(typeEntity.getEntityType().getCode())) {
            return false;
        }

        Long userId = user.getId();

        if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                userId, typeEntityId, PermissionRoleEnum.VALIDEUR.getLabel())) {
            return true;
        }

        Optional<Entity> group = groupService.findGroupByEntityId(typeEntityId);
        if (group.isPresent() && userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                userId, group.get().getId(), PermissionRoleEnum.VALIDEUR.getLabel())) {
            return true;
        }

        Entity reference = typeService.findReferenceAncestor(typeEntity);
        return reference != null && reference.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                        userId, reference.getId(), PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel());
    }

    private static boolean isAdminTechniqueOrFonctionnel(Utilisateur user) {
        if (user.getGroupe() == null || user.getGroupe().getNom() == null) {
            return false;
        }
        String n = user.getGroupe().getNom();
        return GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel().equalsIgnoreCase(n)
                || GroupEnum.ADMINISTRATEUR_FONCTIONNEL.getLabel().equalsIgnoreCase(n);
    }
}
