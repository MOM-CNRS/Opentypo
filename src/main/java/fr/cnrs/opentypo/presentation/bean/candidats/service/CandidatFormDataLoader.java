package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.*;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryDescriptionItem;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryLabelItem;
import fr.cnrs.opentypo.presentation.bean.candidats.model.Step3FormData;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Charge les données d'une Entity vers le formulaire étape 3 (Step3FormData).
 */
@Service
@Slf4j
public class CandidatFormDataLoader {

    @Inject
    private EntityRepository entityRepository;

    /**
     * Charge les données pour l'étape 3 (wizard) - exclut le label principal de l'étape 1.
     */
    public Step3FormData loadForWizardStep3(Long entityId, String principalLangueCode) {
        Entity entity = entityRepository.findById(entityId).orElse(null);
        return loadFromEntity(entity, true, principalLangueCode);
    }

    /**
     * Charge les données pour la visualisation (inclut tous les labels).
     */
    public Step3FormData loadForView(Long entityId) {
        Entity entity = entityRepository.findById(entityId).orElse(null);
        return loadFromEntity(entity, false, null);
    }

    private Step3FormData loadFromEntity(Entity entity, boolean excludePrincipalLabel, String principalLangueCode) {
        if (entity == null || entity.getId() == null) {
            return Step3FormData.builder().build();
        }

        Entity refreshedEntity = entityRepository.findById(entity.getId()).orElse(null);
        if (refreshedEntity == null) {
            return Step3FormData.builder().build();
        }

        List<CategoryLabelItem> labels = new ArrayList<>();
        if (refreshedEntity.getLabels() != null) {
            for (Label label : refreshedEntity.getLabels()) {
                if (label.getLangue() != null) {
                    if (excludePrincipalLabel && principalLangueCode != null
                            && principalLangueCode.equals(label.getLangue().getCode())) {
                        continue;
                    }
                    labels.add(new CategoryLabelItem(
                            label.getNom(),
                            label.getLangue().getCode(),
                            label.getLangue()
                    ));
                }
            }
        }

        List<CategoryDescriptionItem> descriptions = new ArrayList<>();
        if (refreshedEntity.getDescriptions() != null) {
            for (Description desc : refreshedEntity.getDescriptions()) {
                descriptions.add(new CategoryDescriptionItem(
                        desc.getValeur(),
                        desc.getLangue() != null ? desc.getLangue().getCode() : null,
                        desc.getLangue()
                ));
            }
        }

        List<String> refsBiblio = new ArrayList<>();
        if (refreshedEntity.getReferenceBibliographique() != null
                && !refreshedEntity.getReferenceBibliographique().isEmpty()) {
            refsBiblio = new ArrayList<>(Arrays.asList(refreshedEntity.getReferenceBibliographique().split("; ")));
        }

        List<String> ateliersList = new ArrayList<>();
        if (refreshedEntity.getAteliers() != null && !refreshedEntity.getAteliers().isEmpty()) {
            ateliersList = new ArrayList<>(Arrays.asList(refreshedEntity.getAteliers().split("; ")));
        }

        List<String> attestationsList = new ArrayList<>();
        if (refreshedEntity.getAttestations() != null && !refreshedEntity.getAttestations().isEmpty()) {
            attestationsList = new ArrayList<>(Arrays.asList(refreshedEntity.getAttestations().split("; ")));
        }

        List<String> sitesList = new ArrayList<>();
        if (refreshedEntity.getSitesArcheologiques() != null && !refreshedEntity.getSitesArcheologiques().isEmpty()) {
            sitesList = new ArrayList<>(Arrays.asList(refreshedEntity.getSitesArcheologiques().split("; ")));
        }

        List<ReferenceOpentheso> aires = new ArrayList<>();
        if (refreshedEntity.getAiresCirculation() != null) {
            aires = refreshedEntity.getAiresCirculation().stream()
                    .filter(ref -> ReferenceOpenthesoEnum.AIRE_CIRCULATION.name().equals(ref.getCode()))
                    .collect(Collectors.toList());
        }

        String production = null;
        if (refreshedEntity.getProduction() != null) {
            refreshedEntity.getProduction().getValeur();
            production = refreshedEntity.getProduction().getValeur();
        }

        // DescriptionDetail
        DescriptionDetail descDetail = refreshedEntity.getDescriptionDetail();
        String decors = null;
        List<String> marques = new ArrayList<>();
        ReferenceOpentheso fonction = null;
        if (descDetail != null) {
            decors = descDetail.getDecors();
            if (descDetail.getMarques() != null && !descDetail.getMarques().isEmpty()) {
                marques = new ArrayList<>(Arrays.asList(descDetail.getMarques().split("; ")));
            }
            fonction = descDetail.getFonction();
            if (fonction != null) fonction.getValeur();
        }

        // CaracteristiquePhysique
        CaracteristiquePhysique carPhysique = refreshedEntity.getCaracteristiquePhysique();
        ReferenceOpentheso metrologie = null;
        ReferenceOpentheso fabrication = null;
        if (carPhysique != null) {
            if (carPhysique.getMetrologie() != null) {
                carPhysique.getMetrologie().getValeur();
                metrologie = carPhysique.getMetrologie();
            }
            if (carPhysique.getFabrication() != null) {
                carPhysique.getFabrication().getValeur();
                fabrication = carPhysique.getFabrication();
            }
        }

        // DescriptionPate
        DescriptionPate descPate = refreshedEntity.getDescriptionPate();
        String descriptionPate = null;
        ReferenceOpentheso couleurPate = null;
        ReferenceOpentheso naturePate = null;
        ReferenceOpentheso inclusions = null;
        ReferenceOpentheso cuisson = null;
        if (descPate != null) {
            descriptionPate = descPate.getDescription();
            if (descPate.getCouleur() != null) {
                descPate.getCouleur().getValeur();
                couleurPate = descPate.getCouleur();
            }
            if (descPate.getNature() != null) {
                descPate.getNature().getValeur();
                naturePate = descPate.getNature();
            }
            if (descPate.getInclusion() != null) {
                descPate.getInclusion().getValeur();
                inclusions = descPate.getInclusion();
            }
            if (descPate.getCuisson() != null) {
                descPate.getCuisson().getValeur();
                cuisson = descPate.getCuisson();
            }
        }

        List<String> referentiels = new ArrayList<>();
        if (refreshedEntity.getMetadata() != null && refreshedEntity.getMetadata().getReference() != null) {
            referentiels = new ArrayList<>(Arrays.asList(refreshedEntity.getMetadata().getReference().split("; ")));
        }

        String typeDescription = null;
        if (refreshedEntity.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_TYPE.equals(refreshedEntity.getEntityType().getCode())) {
            typeDescription = refreshedEntity.getCommentaire();
        }

        List<Utilisateur> auteurs = new ArrayList<>();
        if (refreshedEntity.getAuteurs() != null) {
            refreshedEntity.getAuteurs().size();
            auteurs = new ArrayList<>(refreshedEntity.getAuteurs());
        }

        // DescriptionMonnaie (collection MONNAIE)
        DescriptionMonnaie descMonnaie = refreshedEntity.getDescriptionMonnaie();
        String droit = null, legendeDroit = null, coinsMonetairesDroit = null;
        String revers = null, legendeRevers = null, coinsMonetairesRevers = null;
        if (descMonnaie != null) {
            droit = descMonnaie.getDroit();
            legendeDroit = descMonnaie.getLegendeDroit();
            coinsMonetairesDroit = descMonnaie.getCoinsMonetairesDroit();
            revers = descMonnaie.getRevers();
            legendeRevers = descMonnaie.getLegendeRevers();
            coinsMonetairesRevers = descMonnaie.getCoinsMonetairesRevers();
        }

        // CaracteristiquePhysiqueMonnaie (collection MONNAIE)
        CaracteristiquePhysiqueMonnaie cpm = refreshedEntity.getCaracteristiquePhysiqueMonnaie();
        ReferenceOpentheso materiau = null, denomination = null, valeur = null, technique = null;
        String metrologieMonnaie = null;
        if (cpm != null) {
            materiau = cpm.getMateriau();
            if (materiau != null) materiau.getValeur();
            denomination = cpm.getDenomination();
            if (denomination != null) denomination.getValeur();
            metrologieMonnaie = cpm.getMetrologie();
            valeur = cpm.getValeur();
            if (valeur != null) valeur.getValeur();
            technique = cpm.getTechnique();
            if (technique != null) technique.getValeur();
            fabrication = cpm.getFabrication();
            if (fabrication != null) fabrication.getValeur();
        }

        return Step3FormData.builder()
                .candidatLabels(labels)
                .descriptions(descriptions)
                .candidatMetadataCommentaire(refreshedEntity.getMetadataCommentaire())
                .candidatBibliographie(refreshedEntity.getBibliographie())
                .referencesBibliographiques(refsBiblio)
                .ateliers(ateliersList)
                .attestations(attestationsList)
                .sitesArcheologiques(sitesList)
                .airesCirculation(aires)
                .decors(decors)
                .marquesEstampilles(marques)
                .fonctionUsage(fonction)
                .metrologie(metrologie)
                .fabricationFaconnage(fabrication)
                .descriptionPate(descriptionPate)
                .couleurPate(couleurPate)
                .naturePate(naturePate)
                .inclusions(inclusions)
                .cuissonPostCuisson(cuisson)
                .reference(refreshedEntity.getReference())
                .typologieScientifique(refreshedEntity.getTypologieScientifique())
                .identifiantPerenne(refreshedEntity.getIdentifiantPerenne())
                .ancienneVersion(refreshedEntity.getAncienneVersion())
                .candidatProduction(production)
                .collectionPublique(refreshedEntity.getPublique())
                .typeDescription(typeDescription)
                .tpq(refreshedEntity.getTpq())
                .taq(refreshedEntity.getTaq())
                .periode(refreshedEntity.getPeriode() != null ? refreshedEntity.getPeriode().getValeur() : "")
                .corpusExterne(refreshedEntity.getMetadata() != null ? refreshedEntity.getMetadata().getCorpusExterne() : "")
                .droit(droit)
                .legendeDroit(legendeDroit)
                .coinsMonetairesDroit(coinsMonetairesDroit)
                .revers(revers)
                .legendeRevers(legendeRevers)
                .coinsMonetairesRevers(coinsMonetairesRevers)
                .materiau(materiau)
                .denomination(denomination)
                .metrologieMonnaie(metrologieMonnaie)
                .valeur(valeur)
                .technique(technique)
                .fabrication(fabrication)
                .selectedAuteurs(auteurs)
                .droit(droit)
                .references(referentiels)
                .build();
    }
}
