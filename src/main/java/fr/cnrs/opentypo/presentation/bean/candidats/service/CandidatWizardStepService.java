package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.application.service.CandidatValidationService;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.TreeNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gère la logique de navigation entre les étapes du wizard de création de candidat.
 */
@Service
@Slf4j
public class CandidatWizardStepService {

    @Inject private CandidatValidationService candidatValidationService;
    @Inject private CandidatEntityService candidatEntityService;
    @Inject private EntityRepository entityRepository;

    public record NextStepResult(boolean success, int newStep, Entity createdEntity, Entity parentEntity,
                                 boolean shouldLoadStep3Data, String errorMessage) {}

    /**
     * Exécute la transition vers l'étape suivante.
     */
    @Transactional
    public NextStepResult nextStep(int currentStep, Long selectedEntityTypeId, String entityCode, String entityLabel,
                                  String selectedLangueCode, Long selectedCollectionId, TreeNode selectedTreeNode,
                                  Entity currentEntity, Utilisateur currentUser) {
        if (currentStep == 0) {
            return nextFromStep1(selectedEntityTypeId, entityCode, entityLabel, selectedLangueCode, currentUser);
        }
        if (currentStep == 1) {
            return nextFromStep2(selectedCollectionId, selectedTreeNode, currentEntity);
        }
        log.warn("nextStep() appelée mais currentStep = {} (hors limites)", currentStep);
        return new NextStepResult(false, currentStep, null, null, false, null);
    }

    private NextStepResult nextFromStep1(Long entityTypeId, String code, String label, String langueCode, Utilisateur user) {
        log.info("Validation de l'étape 1...");
        if (!candidatValidationService.validateStep1(entityTypeId, code, label, langueCode)) {
            log.warn("Validation échouée à l'étape 1");
            return new NextStepResult(false, 0, null, null, false, null);
        }
        try {
            Entity created = candidatEntityService.createEntityFromStep1(
                    entityTypeId, code != null ? code.trim() : null, label != null ? label.trim() : null,
                    langueCode, user);
            log.info("Entité créée à l'étape 1 avec l'ID: {}", created.getId());
            return new NextStepResult(true, 1, created, null, false, null);
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'entité à l'étape 1", e);
            return new NextStepResult(false, 0, null, null, false,
                    "Une erreur est survenue lors de la création de l'entité : " + (e.getMessage() != null ? e.getMessage() : ""));
        }
    }

    private NextStepResult nextFromStep2(Long collectionId, TreeNode selectedNode, Entity currentEntity) {
        log.info("Validation de l'étape 2...");
        if (!candidatValidationService.validateStep2(collectionId, selectedNode)) {
            log.warn("Validation échouée à l'étape 2");
            return new NextStepResult(false, 1, null, null, false, null);
        }
        Entity parent = selectedNode != null && selectedNode.getData() instanceof Entity e ? e : null;
        if (parent == null) {
            return new NextStepResult(false, 1, null, null, false, null);
        }
        Entity parentEntity = entityRepository.findById(parent.getId()).orElse(null);
        if (currentEntity == null || currentEntity.getId() == null || parentEntity == null) {
            log.warn("Impossible de créer la relation : currentEntity={}, parentEntity={}",
                    currentEntity != null ? currentEntity.getId() : null,
                    parentEntity != null ? parentEntity.getId() : null);
            return new NextStepResult(true, 2, currentEntity, parentEntity, true, null);
        }
        try {
            Entity refreshed = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshed != null) {
                candidatEntityService.createParentChildRelation(parentEntity, refreshed);
                log.info("Passage à l'étape 3");
                return new NextStepResult(true, 2, refreshed, parentEntity, true, null);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la création de la relation parent-enfant", e);
            return new NextStepResult(false, 1, null, null, false,
                    "Une erreur est survenue lors de la création de la relation : " + (e.getMessage() != null ? e.getMessage() : ""));
        }
        return new NextStepResult(true, 2, currentEntity, parentEntity, true, null);
    }
}
