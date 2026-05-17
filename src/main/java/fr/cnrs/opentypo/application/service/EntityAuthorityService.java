package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.application.dto.api.ApiErrorMessages;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Droits d'écriture sur les entités typologiques (création, modification, suppression).
 */
@Service
@RequiredArgsConstructor
public class EntityAuthorityService {

    private final EntityRepository entityRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final TypeService typeService;
    private final GroupService groupService;
    private final CollectionService collectionService;

    // --- Création ---

    public void assertCanCreate(Utilisateur user, String entityTypeCode, Long parentEntityId) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ApiErrorMessages.UNAUTHORIZED);
        }
        if (!canCreate(user, entityTypeCode, parentEntityId)) {
            if (requiresAdminOnly(entityTypeCode)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrorMessages.CREATE_REFERENTIEL_ADMIN_ONLY);
            }
            if (parentEntityId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrorMessages.CREATE_PARENT_REQUIRED);
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrorMessages.CREATE_ENTITY_FORBIDDEN);
        }
    }

    public boolean canCreate(Utilisateur user, String entityTypeCode, Long parentEntityId) {
        if (user == null || entityTypeCode == null || entityTypeCode.isBlank()) {
            return false;
        }
        if (isAdminTechniqueOrFonctionnel(user)) {
            return true;
        }
        if (requiresAdminOnly(entityTypeCode)) {
            return false;
        }
        if (parentEntityId == null) {
            return false;
        }
        Long referentialId = resolveReferentialIdFromParent(parentEntityId);
        return referentialId != null && isGestionnaireReferentiel(user, referentialId);
    }

    // --- Suppression ---

    public void assertCanDelete(Utilisateur user, Entity entity) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ApiErrorMessages.UNAUTHORIZED);
        }
        if (!canDelete(user, entity)) {
            String typeCode = entityTypeCode(entity);
            if (requiresAdminOnly(typeCode)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrorMessages.DELETE_REFERENTIEL_ADMIN_ONLY);
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrorMessages.DELETE_ENTITY_FORBIDDEN);
        }
    }

    public boolean canDelete(Utilisateur user, Entity entity) {
        if (user == null || entity == null) {
            return false;
        }
        if (isAdminTechniqueOrFonctionnel(user)) {
            return true;
        }
        String typeCode = entityTypeCode(entity);
        if (requiresAdminOnly(typeCode)) {
            return false;
        }
        Long referentialId = resolveReferentialIdFromEntity(entity);
        return referentialId != null && isGestionnaireReferentiel(user, referentialId);
    }

    // --- Modification ---

    public void assertCanUpdate(Utilisateur user, Entity entity) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ApiErrorMessages.UNAUTHORIZED);
        }
        if (!canUpdate(user, entity)) {
            String typeCode = entityTypeCode(entity);
            if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(typeCode)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrorMessages.UPDATE_REFERENCE_FORBIDDEN);
            }
            if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(typeCode)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrorMessages.UPDATE_COLLECTION_FORBIDDEN);
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrorMessages.UPDATE_ENTITY_FORBIDDEN);
        }
    }

    public boolean canUpdate(Utilisateur user, Entity entity) {
        if (user == null || entity == null) {
            return false;
        }
        String typeCode = entityTypeCode(entity);
        if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(typeCode)) {
            return canUpdateCollection(user, entity);
        }
        if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(typeCode)) {
            return canUpdateReferentiel(user, entity);
        }
        return canUpdateTypologyChild(user, entity);
    }

    private boolean canUpdateCollection(Utilisateur user, Entity collection) {
        if (isAdminTechniqueOrFonctionnel(user)) {
            return true;
        }
        return collection != null && collection.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                        user.getId(),
                        collection.getId(),
                        PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel());
    }

    private boolean canUpdateReferentiel(Utilisateur user, Entity referentiel) {
        if (isAdminTechniqueOrFonctionnel(user)) {
            return true;
        }
        if (referentiel == null || referentiel.getId() == null) {
            return false;
        }
        if (isGestionnaireReferentiel(user, referentiel.getId())) {
            return true;
        }
        Entity collection = collectionService.findCollectionIdByEntityId(referentiel.getId());
        return collection != null && collection.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                        user.getId(),
                        collection.getId(),
                        PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel());
    }

    /**
     * Catégorie, groupe, série, type : admin, gestionnaire du référentiel, ou rédacteur du groupe d'ancrage.
     * Pour un groupe, le rédacteur doit être rattaché à ce groupe.
     */
    private boolean canUpdateTypologyChild(Utilisateur user, Entity entity) {
        if (isAdminTechniqueOrFonctionnel(user)) {
            return true;
        }
        Long referentialId = resolveReferentialIdFromEntity(entity);
        if (referentialId != null && isGestionnaireReferentiel(user, referentialId)) {
            return true;
        }
        return groupService.findGroupByEntityId(entity.getId())
                .filter(group -> group.getId() != null)
                .map(group -> userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                        user.getId(), group.getId(), PermissionRoleEnum.REDACTEUR.getLabel()))
                .orElse(false);
    }

    // --- Visibilité (public / privé) ---

    public void assertCanChangeVisibility(Utilisateur user, Entity entity) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ApiErrorMessages.UNAUTHORIZED);
        }
        if (!canChangeVisibility(user, entity)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrorMessages.VISIBILITY_CHANGE_FORBIDDEN);
        }
    }

    /**
     * Même périmètre que la modification de contenu ; pour les groupes, les gestionnaires de référentiel
     * peuvent aussi basculer la visibilité (aligné sur l'interface JSF).
     */
    public boolean canChangeVisibility(Utilisateur user, Entity entity) {
        if (user == null || entity == null) {
            return false;
        }
        if (canUpdate(user, entity)) {
            return true;
        }
        if (EntityConstants.ENTITY_TYPE_GROUP.equals(entityTypeCode(entity))) {
            return canDelete(user, entity);
        }
        return false;
    }

    // --- Utilitaires ---

    private static boolean requiresAdminOnly(String entityTypeCode) {
        return EntityConstants.ENTITY_TYPE_REFERENCE.equals(entityTypeCode)
                || EntityConstants.ENTITY_TYPE_COLLECTION.equals(entityTypeCode);
    }

    private static String entityTypeCode(Entity entity) {
        return entity.getEntityType() != null ? entity.getEntityType().getCode() : null;
    }

    private Long resolveReferentialIdFromParent(Long parentEntityId) {
        Entity parent = entityRepository.findById(parentEntityId).orElse(null);
        if (parent == null) {
            return null;
        }
        if (parent.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_REFERENCE.equals(parent.getEntityType().getCode())) {
            return parent.getId();
        }
        Entity referential = typeService.findReferenceAncestor(parent);
        return referential != null ? referential.getId() : null;
    }

    private Long resolveReferentialIdFromEntity(Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_REFERENCE.equals(entity.getEntityType().getCode())) {
            return entity.getId();
        }
        Entity referential = typeService.findReferenceAncestor(entity);
        return referential != null ? referential.getId() : null;
    }

    private boolean isGestionnaireReferentiel(Utilisateur user, Long referentialId) {
        return userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                user.getId(),
                referentialId,
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel());
    }

    private static boolean isAdminTechniqueOrFonctionnel(Utilisateur user) {
        if (user.getGroupe() == null || user.getGroupe().getNom() == null) {
            return false;
        }
        String groupeNom = user.getGroupe().getNom();
        return GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel().equalsIgnoreCase(groupeNom)
                || GroupEnum.ADMINISTRATEUR_FONCTIONNEL.getLabel().equalsIgnoreCase(groupeNom);
    }
}
