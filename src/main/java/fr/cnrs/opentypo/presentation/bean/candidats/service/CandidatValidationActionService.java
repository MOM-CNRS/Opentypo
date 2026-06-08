package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.service.ArkIdentifierService;
import fr.cnrs.opentypo.application.service.DemandeValidationRequirementsService;
import fr.cnrs.opentypo.application.service.TypeValidationAuthorityService;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service pour les actions de validation/refus/suppression des candidats.
 */
@Service
@Slf4j
public class CandidatValidationActionService {

    @Inject private EntityRepository entityRepository;
    @Inject private ArkIdentifierService arkIdentifierService;
    @Inject private DemandeValidationRequirementsService demandeValidationRequirementsService;
    @Inject private TypeValidationAuthorityService typeValidationAuthorityService;
    @Inject private CandidatEntityService candidatEntityService;

    public record ActionResult(boolean success, String message, String errorMessage, String redirectUrl) {
        public ActionResult(boolean success, String message, String errorMessage) {
            this(success, message, errorMessage, success ? "/candidats/candidats.xhtml?faces-redirect=true" : null);
        }
    }

    @Transactional
    public ActionResult validerCandidat(Long candidatId, Utilisateur currentUser) {
        if (candidatId == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.invalidCandidate"));

        Entity entity = entityRepository.findById(candidatId).orElse(null);
        if (entity == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.entityNotFound"));

        if (!typeValidationAuthorityService.canUserValidateOrRefuseType(candidatId, currentUser)) {
            return new ActionResult(false, null, JsfMessages.get("candidat.validation.noPermission"));
        }

        if (!EntityStatusEnum.IN_VALIDATION.name().equals(entity.getStatut())) {
            return new ActionResult(false, null, JsfMessages.get("candidat.validation.onlyPendingCanPublish"));
        }

        List<String> missing = demandeValidationRequirementsService.computeMissingRequiredLabels(candidatId);
        if (!missing.isEmpty()) {
            return new ActionResult(false, null,
                    JsfMessages.format("candidat.validation.missingRequiredFields", String.join("; ", missing)));
        }

        if (entity.getAuteurs() != null) entity.getAuteurs().size();

        entity.setStatut(EntityStatusEnum.PUBLIQUE.name());
        arkIdentifierService.ensureArkIfAbsentForPublishedTypologyEntity(entity);
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);

        String userName = formatUserName(currentUser);
        return new ActionResult(true, JsfMessages.format("candidat.validation.validatedBy", userName), null);
    }

    @Transactional
    public ActionResult refuserCandidat(Long candidatId, Utilisateur currentUser) {
        if (candidatId == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.invalidCandidate"));

        Entity entity = entityRepository.findById(candidatId).orElse(null);
        if (entity == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.entityNotFound"));

        if (!typeValidationAuthorityService.canUserValidateOrRefuseType(candidatId, currentUser)) {
            return new ActionResult(false, null, JsfMessages.get("candidat.validation.noPermission"));
        }

        if (!EntityStatusEnum.IN_VALIDATION.name().equals(entity.getStatut())) {
            return new ActionResult(false, null, JsfMessages.get("candidat.validation.onlyPendingCanRefuse"));
        }

        if (entity.getAuteurs() != null) entity.getAuteurs().size();
        entity.setStatut(EntityStatusEnum.REFUSE.name());
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);

        String userName = formatUserName(currentUser);
        return new ActionResult(true, JsfMessages.format("candidat.validation.refusedBy", userName), null);
    }

    /** Transition PROPOSITION vers IN_VALIDATION si les champs obligatoires sont complets. */
    @Transactional
    public ActionResult demanderValidation(Long entityId, Utilisateur currentUser) {
        if (entityId == null) {
            return new ActionResult(false, null, JsfMessages.get("candidat.validation.invalidEntity"));
        }
        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null) {
            return new ActionResult(false, null, JsfMessages.get("candidat.validation.entityNotFound"));
        }
        if (!EntityStatusEnum.PROPOSITION.name().equals(entity.getStatut())) {
            return new ActionResult(false, null, JsfMessages.get("candidat.validation.onlyDraftCanSubmit"));
        }
        List<String> missing = demandeValidationRequirementsService.computeMissingRequiredLabels(entityId);
        if (!missing.isEmpty()) {
            return new ActionResult(false, null,
                    JsfMessages.format("candidat.validation.missingRequiredFields", String.join("; ", missing)));
        }
        entity.setStatut(EntityStatusEnum.IN_VALIDATION.name());
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);
        return new ActionResult(true, JsfMessages.get("candidat.validation.requestRecorded"), null);
    }

    /**
     * Remet un candidat (validé ou refusé) en statut brouillon (PROPOSITION).
     */
    @Transactional
    public ActionResult remettreEnBrouillon(Long candidatId, Utilisateur currentUser) {
        if (candidatId == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.invalidCandidate"));

        Entity entity = entityRepository.findById(candidatId).orElse(null);
        if (entity == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.entityNotFound"));

        entity.setStatut(EntityStatusEnum.PROPOSITION.name());
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);

        String userName = formatUserName(currentUser);
        return new ActionResult(true, JsfMessages.format("candidat.validation.draftRestoredBy", userName), null);
    }

    @Transactional
    public ActionResult supprimerCandidat(Long candidatId) {
        if (candidatId == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.invalidCandidate"));

        Entity entity = entityRepository.findById(candidatId).orElse(null);
        if (entity == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.entityNotFound"));

        try {
            candidatEntityService.deleteEntityWithRelations(entity);
            return new ActionResult(true, JsfMessages.get("candidat.validation.deleted"), null);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du candidat", e);
            return new ActionResult(false, null, JsfMessages.format("common.error.delete", e.getMessage()));
        }
    }

    @Transactional
    public ActionResult validerCandidatFromView(Long entityId, List<Utilisateur> selectedAuteurs,
                                                List<String> attestations, List<String> sitesArcheologiques,
                                                String referentiel, String typologieScientifique,
                                                String identifiantPerenne, String ancienneVersion,
                                                Utilisateur currentUser) {
        if (entityId == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.noEntityToValidate"));
        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.entityNotFound"));
        if (!typeValidationAuthorityService.canUserValidateOrRefuseType(entityId, currentUser)) {
            return new ActionResult(false, null, JsfMessages.get("candidat.validation.noPermission"));
        }
        applyModifications(entity, selectedAuteurs, attestations, sitesArcheologiques, referentiel, typologieScientifique, identifiantPerenne, ancienneVersion);
        if (!EntityStatusEnum.IN_VALIDATION.name().equals(entity.getStatut())) {
            return new ActionResult(false, null, JsfMessages.get("candidat.validation.onlyPendingCanPublish"));
        }
        List<String> missing = demandeValidationRequirementsService.computeMissingRequiredLabels(entityId);
        if (!missing.isEmpty()) {
            return new ActionResult(false, null,
                    JsfMessages.format("candidat.validation.missingRequiredFields", String.join("; ", missing)));
        }
        entity.setStatut(EntityStatusEnum.PUBLIQUE.name());
        arkIdentifierService.ensureArkIfAbsentForPublishedTypologyEntity(entity);
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);
        String userName = formatUserName(currentUser);
        return new ActionResult(true, JsfMessages.format("candidat.validation.validatedBy", userName), null);
    }

    @Transactional
    public ActionResult refuserCandidatFromView(Long entityId, List<Utilisateur> selectedAuteurs,
                                               List<String> attestations, List<String> sitesArcheologiques,
                                               String referentiel, String typologieScientifique,
                                               String identifiantPerenne, String ancienneVersion,
                                               Utilisateur currentUser) {
        if (entityId == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.noEntityToRefuse"));
        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null) return new ActionResult(false, null, JsfMessages.get("candidat.validation.entityNotFound"));
        if (!typeValidationAuthorityService.canUserValidateOrRefuseType(entityId, currentUser)) {
            return new ActionResult(false, null, JsfMessages.get("candidat.validation.noPermission"));
        }
        applyModifications(entity, selectedAuteurs, attestations, sitesArcheologiques, referentiel, typologieScientifique, identifiantPerenne, ancienneVersion);
        if (!EntityStatusEnum.IN_VALIDATION.name().equals(entity.getStatut())) {
            return new ActionResult(false, null, JsfMessages.get("candidat.validation.onlyPendingCanRefuse"));
        }
        entity.setStatut(EntityStatusEnum.REFUSE.name());
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);
        String userName = formatUserName(currentUser);
        return new ActionResult(true, JsfMessages.format("candidat.validation.refusedBy", userName), null);
    }

    private static String formatUserName(Utilisateur currentUser) {
        return currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom()
                : JsfMessages.get("candidat.validation.defaultUser");
    }

    private void applyModifications(Entity entity, List<Utilisateur> auteurs, List<String> attestations,
                                   List<String> sites, String ref, String typo, String idPerenne, String ancVersion) {
        if (auteurs != null) { if (entity.getAuteurs() != null) entity.getAuteurs().size(); entity.setAuteurs(new ArrayList<>(auteurs)); }
        entity.setAttestations(attestations != null && !attestations.isEmpty() ? String.join("; ", attestations) : null);
        entity.setSitesArcheologiques(sites != null && !sites.isEmpty() ? String.join("; ", sites) : null);
        entity.setReference(ref);
        entity.setTypologieScientifique(typo);
        entity.setIdentifiantPerenne(idPerenne);
        entity.setAncienneVersion(ancVersion);
    }

    private void addUserAsAuthor(Entity entity, Utilisateur user) {
        if (user == null) return;
        if (entity.getAuteurs() == null) entity.setAuteurs(new ArrayList<>());
        boolean already = entity.getAuteurs().stream().anyMatch(a -> a.getId() != null && a.getId().equals(user.getId()));
        if (!already) entity.getAuteurs().add(user);
    }
}
