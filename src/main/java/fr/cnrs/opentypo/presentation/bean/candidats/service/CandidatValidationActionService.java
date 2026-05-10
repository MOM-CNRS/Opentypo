package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.service.ArkIdentifierService;
import fr.cnrs.opentypo.application.service.DemandeValidationRequirementsService;
import fr.cnrs.opentypo.application.service.TypeValidationAuthorityService;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
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
        if (candidatId == null) return new ActionResult(false, null, "Candidat invalide.");

        Entity entity = entityRepository.findById(candidatId).orElse(null);
        if (entity == null) return new ActionResult(false, null, "Entité introuvable.");

        if (!typeValidationAuthorityService.canUserValidateOrRefuseType(candidatId, currentUser)) {
            return new ActionResult(false, null,
                    "Vous n’avez pas les droits pour valider ou refuser cette fiche.");
        }

        if (!EntityStatusEnum.IN_VALIDATION.name().equals(entity.getStatut())) {
            return new ActionResult(false, null,
                    "Seuls les éléments en attente de validation peuvent être publiés.");
        }

        List<String> missing = demandeValidationRequirementsService.computeMissingRequiredLabels(candidatId);
        if (!missing.isEmpty()) {
            return new ActionResult(false, null,
                    "Champs obligatoires incomplets : " + String.join("; ", missing));
        }

        if (entity.getAuteurs() != null) entity.getAuteurs().size();

        entity.setStatut(EntityStatusEnum.PUBLIQUE.name());
        arkIdentifierService.ensureArkIfAbsentForPublishedTypologyEntity(entity);
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);

        String userName = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Utilisateur";
        return new ActionResult(true, "Le candidat a été validé par " + userName + ".", null);
    }

    @Transactional
    public ActionResult refuserCandidat(Long candidatId, Utilisateur currentUser) {
        if (candidatId == null) return new ActionResult(false, null, "Candidat invalide.");

        Entity entity = entityRepository.findById(candidatId).orElse(null);
        if (entity == null) return new ActionResult(false, null, "Entité introuvable.");

        if (!typeValidationAuthorityService.canUserValidateOrRefuseType(candidatId, currentUser)) {
            return new ActionResult(false, null,
                    "Vous n’avez pas les droits pour valider ou refuser cette fiche.");
        }

        if (!EntityStatusEnum.IN_VALIDATION.name().equals(entity.getStatut())) {
            return new ActionResult(false, null,
                    "Seuls les éléments en attente de validation peuvent être refusés.");
        }

        if (entity.getAuteurs() != null) entity.getAuteurs().size();
        entity.setStatut(EntityStatusEnum.REFUSE.name());
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);

        String userName = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Utilisateur";
        return new ActionResult(true, "Le candidat a été refusé par " + userName + ".", null);
    }

    /** Transition PROPOSITION vers IN_VALIDATION si les champs obligatoires sont complets. */
    @Transactional
    public ActionResult demanderValidation(Long entityId, Utilisateur currentUser) {
        if (entityId == null) {
            return new ActionResult(false, null, "Entité invalide.");
        }
        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null) {
            return new ActionResult(false, null, "Entité introuvable.");
        }
        if (!EntityStatusEnum.PROPOSITION.name().equals(entity.getStatut())) {
            return new ActionResult(false, null, "Seul un brouillon peut être envoyé en validation.");
        }
        List<String> missing = demandeValidationRequirementsService.computeMissingRequiredLabels(entityId);
        if (!missing.isEmpty()) {
            return new ActionResult(false, null,
                    "Champs obligatoires incomplets : " + String.join("; ", missing));
        }
        entity.setStatut(EntityStatusEnum.IN_VALIDATION.name());
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);
        return new ActionResult(true, "La demande de validation a été enregistrée.", null);
    }

    /**
     * Remet un candidat (validé ou refusé) en statut brouillon (PROPOSITION).
     */
    @Transactional
    public ActionResult remettreEnBrouillon(Long candidatId, Utilisateur currentUser) {
        if (candidatId == null) return new ActionResult(false, null, "Candidat invalide.");

        Entity entity = entityRepository.findById(candidatId).orElse(null);
        if (entity == null) return new ActionResult(false, null, "Entité introuvable.");

        entity.setStatut(EntityStatusEnum.PROPOSITION.name());
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);

        String userName = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Utilisateur";
        return new ActionResult(true, "Le brouillon a été remis en cours par " + userName + ".", null);
    }

    @Transactional
    public ActionResult supprimerCandidat(Long candidatId) {
        if (candidatId == null) return new ActionResult(false, null, "Candidat invalide.");

        Entity entity = entityRepository.findById(candidatId).orElse(null);
        if (entity == null) return new ActionResult(false, null, "Entité introuvable.");

        try {
            candidatEntityService.deleteEntityWithRelations(entity);
            return new ActionResult(true, "Le candidat a été supprimé avec succès.", null);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du candidat", e);
            return new ActionResult(false, null, "Une erreur est survenue lors de la suppression : " + e.getMessage());
        }
    }

    @Transactional
    public ActionResult validerCandidatFromView(Long entityId, List<Utilisateur> selectedAuteurs,
                                                List<String> attestations, List<String> sitesArcheologiques,
                                                String referentiel, String typologieScientifique,
                                                String identifiantPerenne, String ancienneVersion,
                                                Utilisateur currentUser) {
        if (entityId == null) return new ActionResult(false, null, "Aucune entité à valider.");
        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null) return new ActionResult(false, null, "Entité introuvable.");
        if (!typeValidationAuthorityService.canUserValidateOrRefuseType(entityId, currentUser)) {
            return new ActionResult(false, null,
                    "Vous n’avez pas les droits pour valider ou refuser cette fiche.");
        }
        applyModifications(entity, selectedAuteurs, attestations, sitesArcheologiques, referentiel, typologieScientifique, identifiantPerenne, ancienneVersion);
        if (!EntityStatusEnum.IN_VALIDATION.name().equals(entity.getStatut())) {
            return new ActionResult(false, null,
                    "Seuls les éléments en attente de validation peuvent être publiés.");
        }
        List<String> missing = demandeValidationRequirementsService.computeMissingRequiredLabels(entityId);
        if (!missing.isEmpty()) {
            return new ActionResult(false, null,
                    "Champs obligatoires incomplets : " + String.join("; ", missing));
        }
        entity.setStatut(EntityStatusEnum.PUBLIQUE.name());
        arkIdentifierService.ensureArkIfAbsentForPublishedTypologyEntity(entity);
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);
        String userName = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Utilisateur";
        return new ActionResult(true, "Le candidat a été validé par " + userName + ".", null);
    }

    @Transactional
    public ActionResult refuserCandidatFromView(Long entityId, List<Utilisateur> selectedAuteurs,
                                               List<String> attestations, List<String> sitesArcheologiques,
                                               String referentiel, String typologieScientifique,
                                               String identifiantPerenne, String ancienneVersion,
                                               Utilisateur currentUser) {
        if (entityId == null) return new ActionResult(false, null, "Aucune entité à refuser.");
        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null) return new ActionResult(false, null, "Entité introuvable.");
        if (!typeValidationAuthorityService.canUserValidateOrRefuseType(entityId, currentUser)) {
            return new ActionResult(false, null,
                    "Vous n’avez pas les droits pour valider ou refuser cette fiche.");
        }
        applyModifications(entity, selectedAuteurs, attestations, sitesArcheologiques, referentiel, typologieScientifique, identifiantPerenne, ancienneVersion);
        if (!EntityStatusEnum.IN_VALIDATION.name().equals(entity.getStatut())) {
            return new ActionResult(false, null,
                    "Seuls les éléments en attente de validation peuvent être refusés.");
        }
        entity.setStatut(EntityStatusEnum.REFUSE.name());
        addUserAsAuthor(entity, currentUser);
        entityRepository.save(entity);
        String userName = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Utilisateur";
        return new ActionResult(true, "Le candidat a été refusé par " + userName + ".", null);
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
