package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.*;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryDescriptionItem;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryLabelItem;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CandidatSauvegardeRequest;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CandidatSauvegardeResult;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour la sauvegarde complète d'un candidat (méthode sauvegarderCandidat).
 */
@Service
@Slf4j
public class CandidatSauvegardeService {

    @Inject private EntityRepository entityRepository;
    @Inject private EntityTypeRepository entityTypeRepository;
    @Inject private EntityRelationRepository entityRelationRepository;
    @Inject private LangueRepository langueRepository;

    /**
     * Valide les champs obligatoires et retourne la liste des erreurs (vide si OK).
     */
    public List<String> validateSauvegarde(CandidatSauvegardeRequest req) {
        List<String> errors = new ArrayList<>();
        if (req.getSelectedEntityTypeId() == null) errors.add("Le type d'entité est requis.");
        if (req.getEntityCode() == null || req.getEntityCode().trim().isEmpty()) errors.add("Le code est requis.");
        if (req.getEntityLabel() == null || req.getEntityLabel().trim().isEmpty()) errors.add("Le label est requis.");
        if (req.getSelectedLangueCode() == null) errors.add("La langue est requise.");
        if (req.getSelectedCollectionId() == null) errors.add("La collection est requise.");
        if (req.getSelectedParentEntity() == null) errors.add("Le référentiel est requis.");
        return errors;
    }

    @Transactional
    public CandidatSauvegardeResult executeSauvegarde(CandidatSauvegardeRequest req) {
        List<String> validationErrors = validateSauvegarde(req);
        if (!validationErrors.isEmpty()) {
            return CandidatSauvegardeResult.builder()
                    .success(false)
                    .errorMessage(String.join(" ", validationErrors))
                    .build();
        }

        try {
            Entity newEntity = loadOrCreateEntity(req);
            addLabelsAndDescriptions(newEntity, req);
            applyTypeSpecificFields(newEntity, req);
            applyDescriptionMonnaie(newEntity, req);
            applyCaracteristiquePhysiqueMonnaie(newEntity, req);
            applyOpenThesoReference(newEntity, req);

            Entity savedEntity = entityRepository.save(newEntity);
            createParentRelation(savedEntity, req);

            return CandidatSauvegardeResult.builder()
                    .success(true)
                    .savedEntity(savedEntity)
                    .successMessage("Le candidat a été créé avec succès avec le statut PROPOSITION.")
                    .build();
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du candidat", e);
            return CandidatSauvegardeResult.builder()
                    .success(false)
                    .errorMessage("Une erreur est survenue lors de la sauvegarde : " + e.getMessage())
                    .build();
        }
    }

    private Entity loadOrCreateEntity(CandidatSauvegardeRequest req) {
        if (req.getCurrentEntity() != null && req.getCurrentEntity().getId() != null) {
            return entityRepository.findById(req.getCurrentEntity().getId())
                    .orElseThrow(() -> new IllegalStateException("L'entité créée à l'étape 1 n'existe plus."));
        }
        EntityType entityType = entityTypeRepository.findById(req.getSelectedEntityTypeId())
                .orElseThrow(() -> new IllegalStateException("Le type d'entité sélectionné n'existe pas."));

        Entity newEntity = new Entity();
        newEntity.setCode(req.getEntityCode().trim());
        newEntity.setEntityType(entityType);
        newEntity.setStatut(EntityStatusEnum.PROPOSITION.name());
        newEntity.setPublique(false);
        newEntity.setCreateDate(LocalDateTime.now());

        if (req.getCurrentUser() != null) {
            newEntity.setCreateBy(req.getCurrentUser().getEmail());
            List<Utilisateur> auteurs = new ArrayList<>();
            auteurs.add(req.getCurrentUser());
            newEntity.setAuteurs(auteurs);
        }
        return newEntity;
    }

    private void addLabelsAndDescriptions(Entity entity, CandidatSauvegardeRequest req) {
        if (entity.getLabels() == null || entity.getLabels().isEmpty()) {
            Langue langue = langueRepository.findByCode(req.getSelectedLangueCode());
            if (langue != null) {
                Label label = new Label();
                label.setNom(req.getEntityLabel().trim());
                label.setEntity(entity);
                label.setLangue(langue);
                if (entity.getLabels() == null) entity.setLabels(new ArrayList<>());
                entity.getLabels().add(label);
            }
        }

        if (req.getCandidatLabels() != null && !req.getCandidatLabels().isEmpty()) {
            for (CategoryLabelItem item : req.getCandidatLabels()) {
                if (item.getNom() != null && !item.getNom().trim().isEmpty()
                        && item.getLangueCode() != null && !item.getLangueCode().trim().isEmpty()) {
                    Langue langue = langueRepository.findByCode(item.getLangueCode());
                    if (langue != null) {
                        Label label = new Label();
                        label.setNom(item.getNom().trim());
                        label.setEntity(entity);
                        label.setLangue(langue);
                        entity.getLabels().add(label);
                    }
                }
            }
        }

        if (req.getDescriptions() != null && !req.getDescriptions().isEmpty()) {
            for (CategoryDescriptionItem item : req.getDescriptions()) {
                if (item.getValeur() != null && !item.getValeur().trim().isEmpty()
                        && item.getLangueCode() != null && !item.getLangueCode().trim().isEmpty()) {
                    Langue langue = langueRepository.findByCode(item.getLangueCode());
                    if (langue != null) {
                        Description desc = new Description();
                        desc.setValeur(item.getValeur().trim());
                        desc.setEntity(entity);
                        desc.setLangue(langue);
                        entity.getDescriptions().add(desc);
                    }
                }
            }
        }
    }

    private void applyTypeSpecificFields(Entity entity, CandidatSauvegardeRequest req) {
        EntityType entityType = entity.getEntityType();
        if (entityType == null) return;

        String code = entityType.getCode();
        if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(code) || "CATEGORIE".equals(code)) {
            if (req.getCandidatBibliographie() != null && !req.getCandidatBibliographie().trim().isEmpty()) {
                entity.setBibliographie(req.getCandidatBibliographie().trim());
            }
            if (req.getImagePrincipaleUrl() != null && !req.getImagePrincipaleUrl().trim().isEmpty()) {
                entity.setImagePrincipaleUrl(req.getImagePrincipaleUrl().trim());
            }
            if (req.getReferencesBibliographiques() != null && !req.getReferencesBibliographiques().isEmpty()) {
                entity.setReferenceBibliographique(String.join("; ", req.getReferencesBibliographiques()));
            }
        } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(code)) {
            if (req.getTypeDescription() != null && !req.getTypeDescription().trim().isEmpty()) {
                entity.setCommentaire(req.getTypeDescription().trim());
            }
        } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(code)) {
            if (req.getSerieDescription() != null && !req.getSerieDescription().trim().isEmpty()) {
                entity.setCommentaire(req.getSerieDescription().trim());
            }
        } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(code)) {
            StringBuilder sb = new StringBuilder();
            if (req.getGroupDescription() != null && !req.getGroupDescription().trim().isEmpty()) {
                sb.append(req.getGroupDescription().trim());
            }
            if (req.getPeriode() != null && !req.getPeriode().trim().isEmpty()) {
                if (sb.length() > 0) sb.append("\n\nPériode: ");
                else sb.append("Période: ");
                sb.append(req.getPeriode().trim());
            }
            if (sb.length() > 0) entity.setCommentaire(sb.toString());
            if (req.getTpq() != null) entity.setTpq(req.getTpq());
            if (req.getTaq() != null) entity.setTaq(req.getTaq());
        } else if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(code)) {
            if (req.getCollectionDescription() != null && !req.getCollectionDescription().trim().isEmpty()) {
                entity.setCommentaire(req.getCollectionDescription().trim());
            }
            entity.setPublique(false);
        }
    }

    private void applyCaracteristiquePhysiqueMonnaie(Entity entity, CandidatSauvegardeRequest req) {
        boolean hasAny = (req.getMateriau() != null) || (req.getDenomination() != null)
                || (req.getMetrologieMonnaie() != null && !req.getMetrologieMonnaie().trim().isEmpty())
                || (req.getValeur() != null) || (req.getTechnique() != null) || (req.getFabrication() != null);
        if (!hasAny) return;
        CaracteristiquePhysiqueMonnaie cpm = entity.getCaracteristiquePhysiqueMonnaie();
        if (cpm == null) {
            cpm = new CaracteristiquePhysiqueMonnaie();
            cpm.setEntity(entity);
            entity.setCaracteristiquePhysiqueMonnaie(cpm);
        }
        if (req.getMateriau() != null) cpm.setMateriau(req.getMateriau());
        if (req.getDenomination() != null) cpm.setDenomination(req.getDenomination());
        if (req.getMetrologieMonnaie() != null) cpm.setMetrologie(req.getMetrologieMonnaie().trim().isEmpty() ? null : req.getMetrologieMonnaie().trim());
        if (req.getValeur() != null) cpm.setValeur(req.getValeur());
        if (req.getTechnique() != null) cpm.setTechnique(req.getTechnique());
        if (req.getFabrication() != null) cpm.setFabrication(req.getFabrication());
    }

    private void applyDescriptionMonnaie(Entity entity, CandidatSauvegardeRequest req) {
        boolean hasAny = (req.getDroit() != null && !req.getDroit().trim().isEmpty())
                || (req.getLegendeDroit() != null && !req.getLegendeDroit().trim().isEmpty())
                || (req.getCoinsMonetairesDroit() != null && !req.getCoinsMonetairesDroit().trim().isEmpty())
                || (req.getRevers() != null && !req.getRevers().trim().isEmpty())
                || (req.getLegendeRevers() != null && !req.getLegendeRevers().trim().isEmpty())
                || (req.getCoinsMonetairesRevers() != null && !req.getCoinsMonetairesRevers().trim().isEmpty());
        if (!hasAny) return;
        DescriptionMonnaie dm = entity.getDescriptionMonnaie();
        if (dm == null) {
            dm = new DescriptionMonnaie();
            dm.setEntity(entity);
            entity.setDescriptionMonnaie(dm);
        }
        if (req.getDroit() != null) dm.setDroit(req.getDroit().trim().isEmpty() ? null : req.getDroit().trim());
        if (req.getLegendeDroit() != null) dm.setLegendeDroit(req.getLegendeDroit().trim().isEmpty() ? null : req.getLegendeDroit().trim());
        if (req.getCoinsMonetairesDroit() != null) dm.setCoinsMonetairesDroit(req.getCoinsMonetairesDroit().trim().isEmpty() ? null : req.getCoinsMonetairesDroit().trim());
        if (req.getRevers() != null) dm.setRevers(req.getRevers().trim().isEmpty() ? null : req.getRevers().trim());
        if (req.getLegendeRevers() != null) dm.setLegendeRevers(req.getLegendeRevers().trim().isEmpty() ? null : req.getLegendeRevers().trim());
        if (req.getCoinsMonetairesRevers() != null) dm.setCoinsMonetairesRevers(req.getCoinsMonetairesRevers().trim().isEmpty() ? null : req.getCoinsMonetairesRevers().trim());
    }

    private void applyOpenThesoReference(Entity entity, CandidatSauvegardeRequest req) {
        if (req.getOpenThesoCreatedReference() == null) return;
        ReferenceOpentheso ref = req.getOpenThesoCreatedReference();
        if (ReferenceOpenthesoEnum.PRODUCTION.name().equals(ref.getCode())) {
            entity.setProduction(ref);
        }
    }

    private void createParentRelation(Entity savedEntity, CandidatSauvegardeRequest req) {
        Entity parent = req.getSelectedParentEntity();
        if (parent == null) return;
        if (!entityRelationRepository.existsByParentAndChild(parent.getId(), savedEntity.getId())) {
            EntityRelation relation = new EntityRelation();
            relation.setParent(parent);
            relation.setChild(savedEntity);
            entityRelationRepository.save(relation);
        }
    }
}
