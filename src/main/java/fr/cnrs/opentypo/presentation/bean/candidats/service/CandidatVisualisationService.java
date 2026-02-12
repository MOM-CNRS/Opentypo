package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryDescriptionItem;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryLabelItem;
import fr.cnrs.opentypo.presentation.bean.candidats.model.Step3FormData;
import fr.cnrs.opentypo.presentation.bean.candidats.model.VisualisationPrepareResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import jakarta.inject.Inject;
import fr.cnrs.opentypo.domain.entity.Utilisateur;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour la visualisation et l'enregistrement des modifications des candidats.
 */
@Service
@Slf4j
public class CandidatVisualisationService {

    @Inject private EntityRepository entityRepository;
    @Inject private EntityRelationRepository entityRelationRepository;
    @Inject private CandidatFormDataLoader formDataLoader;

    public record EnregistrerResult(boolean success, String redirectUrl, String errorMessage) {}

    /**
     * Prépare les données pour la visualisation d'un candidat.
     */
    public VisualisationPrepareResult prepareVisualisation(Long candidatId, String defaultLangueCode) {
        if (candidatId == null) {
            return VisualisationPrepareResult.builder().success(false).errorMessage("ID candidat invalide.").build();
        }

        Entity entity = entityRepository.findById(candidatId).orElse(null);
        if (entity == null) {
            return VisualisationPrepareResult.builder().success(false).errorMessage("L'entité sélectionnée n'existe pas").build();
        }

        try {
            if (entity.getLabels() != null) entity.getLabels().size();
            if (entity.getEntityType() != null) entity.getEntityType().getCode();

            String langueCode = defaultLangueCode != null ? defaultLangueCode : "fr";

            List<CategoryLabelItem> labels = CollectionUtils.isEmpty(entity.getLabels()) ? new ArrayList<>()
                    : entity.getLabels().stream()
                    .map(l -> CategoryLabelItem.builder()
                            .nom(l.getNom())
                            .langueCode(l.getLangue() != null ? l.getLangue().getCode() : null)
                            .langue(l.getLangue())
                            .build())
                    .collect(Collectors.toList());

            List<CategoryDescriptionItem> descs = CollectionUtils.isEmpty(entity.getDescriptions()) ? new ArrayList<>()
                    : entity.getDescriptions().stream()
                    .map(d -> CategoryDescriptionItem.builder()
                            .valeur(d.getValeur())
                            .langueCode(d.getLangue() != null ? d.getLangue().getCode() : null)
                            .langue(d.getLangue())
                            .build())
                    .collect(Collectors.toList());

            String entityLabel = "";
            Optional<Label> labelOpt = entity.getLabels() != null ? entity.getLabels().stream()
                    .filter(l -> l.getLangue() != null && langueCode.equalsIgnoreCase(l.getLangue().getCode()))
                    .findFirst() : Optional.empty();
            if (labelOpt.isPresent()) {
                entityLabel = labelOpt.get().getNom();
            } else {
                entityLabel = entity.getNom() != null ? entity.getNom() : entity.getCode();
            }

            List<Entity> parents = entityRelationRepository.findParentsByChild(entity);
            Entity collection = null;
            Entity parentEntity = null;
            if (parents != null && !parents.isEmpty()) {
                for (Entity p : parents) {
                    if (p.getEntityType() != null && EntityConstants.ENTITY_TYPE_COLLECTION.equals(p.getEntityType().getCode())) {
                        collection = p;
                        if (collection.getLabels() != null) collection.getLabels().size();
                        break;
                    }
                }
                for (Entity p : parents) {
                    if (p.getEntityType() != null && !EntityConstants.ENTITY_TYPE_COLLECTION.equals(p.getEntityType().getCode())) {
                        parentEntity = p;
                        break;
                    }
                }
            }

            Step3FormData step3Data = formDataLoader.loadForView(entity.getId());
            String periode = entity.getPeriode() != null ? entity.getPeriode().getValeur() : null;

            return VisualisationPrepareResult.builder()
                    .success(true)
                    .redirectUrl("/candidats/view.xhtml?faces-redirect=true")
                    .selectedEntityTypeId(entity.getEntityType() != null ? entity.getEntityType().getId() : null)
                    .entityCode(entity.getCode())
                    .selectedLangueCode(langueCode)
                    .entityLabel(entityLabel)
                    .selectedCollectionId(collection != null ? collection.getId() : null)
                    .selectedParentEntity(parentEntity)
                    .currentEntity(entity)
                    .candidatLabels(labels)
                    .descriptions(descs)
                    .periode(periode)
                    .step3Data(step3Data)
                    .build();
        } catch (Exception e) {
            log.error("Erreur lors de la préparation de la visualisation", e);
            return VisualisationPrepareResult.builder()
                    .success(false)
                    .errorMessage("Une erreur est survenue : " + e.getMessage())
                    .build();
        }
    }

    /**
     * Prépare les données pour prepareValidateCandidat (ouverture du dialogue de validation).
     */
    public PrepareValidateResult prepareValidate(Long candidatId) {
        if (candidatId == null) return new PrepareValidateResult(false, null, null, null, null, "Candidat null");

        Entity entity = entityRepository.findById(candidatId).orElse(null);
        if (entity == null) return new PrepareValidateResult(false, null, null, null, null, "Le candidat n'existe pas");

        List<CategoryLabelItem> labels = CollectionUtils.isEmpty(entity.getLabels()) ? new ArrayList<>()
                : entity.getLabels().stream()
                .map(l -> CategoryLabelItem.builder().nom(l.getNom()).langueCode(l.getLangue().getCode()).langue(l.getLangue()).build())
                .collect(Collectors.toList());

        List<CategoryDescriptionItem> descs = CollectionUtils.isEmpty(entity.getDescriptions()) ? new ArrayList<>()
                : entity.getDescriptions().stream()
                .map(d -> CategoryDescriptionItem.builder().valeur(d.getValeur()).langueCode(d.getLangue().getCode()).langue(d.getLangue()).build())
                .collect(Collectors.toList());

        String periode = entity.getPeriode() != null ? entity.getPeriode().getValeur() : null;
        return new PrepareValidateResult(true, entity.getEntityType().getId(), labels, descs, periode, null);
    }

    public record PrepareValidateResult(boolean success, Long entityTypeId, List<CategoryLabelItem> labels,
                                       List<CategoryDescriptionItem> descriptions, String periode, String errorMessage) {}

    @Transactional
    public EnregistrerResult enregistrerModifications(Long entityId, List<Utilisateur> selectedAuteurs,
                                                      List<String> attestations, List<String> sitesArcheologiques,
                                                      String referentiel, String typologieScientifique,
                                                      String identifiantPerenne, String ancienneVersion) {
        if (entityId == null) return new EnregistrerResult(false, null, "Aucune entité à enregistrer.");

        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null) return new EnregistrerResult(false, null, "Entité introuvable.");

        try {
            if (selectedAuteurs != null) {
                if (entity.getAuteurs() != null) entity.getAuteurs().size();
                entity.setAuteurs(new ArrayList<>(selectedAuteurs));
            }
            entity.setAttestations(attestations != null && !attestations.isEmpty() ? String.join("; ", attestations) : null);
            entity.setSitesArcheologiques(sitesArcheologiques != null && !sitesArcheologiques.isEmpty() ? String.join("; ", sitesArcheologiques) : null);
            entity.setReference(referentiel);
            entity.setTypologieScientifique(typologieScientifique);
            entity.setIdentifiantPerenne(identifiantPerenne);
            entity.setAncienneVersion(ancienneVersion);
            entityRepository.save(entity);
            return new EnregistrerResult(true, "/candidats/candidats.xhtml?faces-redirect=true", null);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement des modifications", e);
            return new EnregistrerResult(false, null, "Une erreur est survenue : " + e.getMessage());
        }
    }
}
