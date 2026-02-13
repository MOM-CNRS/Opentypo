package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.domain.entity.*;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service pour la gestion des références OpenTheso dans le formulaire candidat.
 * Gère les opérations has*, update*FromOpenTheso et delete* pour les références.
 */
@Service
@Slf4j
public class CandidatOpenThesoService {

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    public ReferenceOpentheso loadFonctionUsage(Long entityId) {
        return loadRefFromEntity(entityId, e -> {
            DescriptionDetail dd = e.getDescriptionDetail();
            if (dd != null && dd.getFonction() != null) {
                dd.getFonction().getValeur();
                return dd.getFonction();
            }
            return null;
        });
    }

    public ReferenceOpentheso loadMetrologie(Long entityId) {
        return loadRefFromCaracteristique(entityId, CaracteristiquePhysique::getMetrologie);
    }

    public ReferenceOpentheso loadFabrication(Long entityId) {
        return loadRefFromCaracteristique(entityId, CaracteristiquePhysique::getFabrication);
    }

    public ReferenceOpentheso loadCouleurPate(Long entityId) {
        return loadRefFromDescriptionPate(entityId, DescriptionPate::getCouleur);
    }

    public ReferenceOpentheso loadNaturePate(Long entityId) {
        return loadRefFromDescriptionPate(entityId, DescriptionPate::getNature);
    }

    public ReferenceOpentheso loadInclusions(Long entityId) {
        return loadRefFromDescriptionPate(entityId, DescriptionPate::getInclusion);
    }

    public ReferenceOpentheso loadCuissonPostCuisson(Long entityId) {
        return loadRefFromDescriptionPate(entityId, DescriptionPate::getCuisson);
    }

    public ReferenceOpentheso loadProduction(Long entityId) {
        return loadRefFromEntity(entityId, e -> {
            ReferenceOpentheso p = e.getProduction();
            if (p != null) p.getValeur();
            return p;
        });
    }

    public String loadProductionValue(Long entityId) {
        ReferenceOpentheso p = loadProduction(entityId);
        return p != null ? p.getValeur() : null;
    }

    public ReferenceOpentheso loadPeriode(Long entityId) {
        return loadRefFromEntity(entityId, e -> {
            ReferenceOpentheso p = e.getPeriode();
            if (p != null) p.getValeur();
            return p;
        });
    }

    public ReferenceOpentheso loadMateriau(Long entityId) {
        return loadRefFromCpm(entityId, CaracteristiquePhysiqueMonnaie::getMateriau);
    }

    public ReferenceOpentheso loadDenomination(Long entityId) {
        return loadRefFromCpm(entityId, CaracteristiquePhysiqueMonnaie::getDenomination);
    }

    public ReferenceOpentheso loadValeur(Long entityId) {
        return loadRefFromCpm(entityId, CaracteristiquePhysiqueMonnaie::getValeur);
    }

    public ReferenceOpentheso loadTechnique(Long entityId) {
        return loadRefFromCpm(entityId, CaracteristiquePhysiqueMonnaie::getTechnique);
    }

    public ReferenceOpentheso loadFabricationMonnaie(Long entityId) {
        return loadRefFromCpm(entityId, CaracteristiquePhysiqueMonnaie::getFabrication);
    }

    public String loadMetrologieMonnaie(Long entityId) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getCaracteristiquePhysiqueMonnaie() == null) return null;
        return e.getCaracteristiquePhysiqueMonnaie().getMetrologie();
    }

    private ReferenceOpentheso loadRefFromCpm(Long entityId, java.util.function.Function<CaracteristiquePhysiqueMonnaie, ReferenceOpentheso> extractor) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getCaracteristiquePhysiqueMonnaie() == null) return null;
        ReferenceOpentheso ref = extractor.apply(e.getCaracteristiquePhysiqueMonnaie());
        if (ref != null) ref.getValeur();
        return ref;
    }

    public List<ReferenceOpentheso> loadAiresCirculation(Long entityId) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getAiresCirculation() == null) return new ArrayList<>();
        return e.getAiresCirculation().stream()
                .filter(ref -> ReferenceOpenthesoEnum.AIRE_CIRCULATION.name().equals(ref.getCode()))
                .toList();
    }

    private ReferenceOpentheso loadRefFromEntity(Long entityId, java.util.function.Function<Entity, ReferenceOpentheso> extractor) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        return e != null ? extractor.apply(e) : null;
    }

    private ReferenceOpentheso loadRefFromCaracteristique(Long entityId, java.util.function.Function<CaracteristiquePhysique, ReferenceOpentheso> extractor) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getCaracteristiquePhysique() == null) return null;
        ReferenceOpentheso ref = extractor.apply(e.getCaracteristiquePhysique());
        if (ref != null) ref.getValeur();
        return ref;
    }

    private ReferenceOpentheso loadRefFromDescriptionPate(Long entityId, java.util.function.Function<DescriptionPate, ReferenceOpentheso> extractor) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getDescriptionPate() == null) return null;
        ReferenceOpentheso ref = extractor.apply(e.getDescriptionPate());
        if (ref != null) ref.getValeur();
        return ref;
    }

    @Transactional
    public DeleteResult deleteFonctionUsage(Long entityId) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getDescriptionDetail() == null) return DeleteResult.NOTHING_TO_DELETE;
        e.getDescriptionDetail().setFonction(null);
        entityRepository.save(e);
        log.info("Fonction/usage supprimée pour l'entité ID={}", entityId);
        return DeleteResult.SUCCESS;
    }

    @Transactional
    public DeleteResult deleteMetrologie(Long entityId) {
        return deleteFromCaracteristique(entityId, cp -> cp.setMetrologie(null));
    }

    @Transactional
    public DeleteResult deleteFabrication(Long entityId) {
        return deleteFromCaracteristique(entityId, cp -> cp.setFabrication(null));
    }

    @Transactional
    public DeleteResult deleteCouleurPate(Long entityId) {
        return deleteFromDescriptionPate(entityId, dp -> dp.setCouleur(null));
    }

    @Transactional
    public DeleteResult deleteNaturePate(Long entityId) {
        return deleteFromDescriptionPate(entityId, dp -> dp.setNature(null));
    }

    @Transactional
    public DeleteResult deleteInclusions(Long entityId) {
        return deleteFromDescriptionPate(entityId, dp -> dp.setInclusion(null));
    }

    @Transactional
    public DeleteResult deleteCuissonPostCuisson(Long entityId) {
        return deleteFromDescriptionPate(entityId, dp -> dp.setCuisson(null));
    }

    @Transactional
    public DeleteResult deletePeriode(Long entityId) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getPeriode() == null) return DeleteResult.NOTHING_TO_DELETE;
        referenceOpenthesoRepository.deleteById(e.getPeriode().getId());
        e.setPeriode(null);
        entityRepository.save(e);
        log.info("Période supprimée pour l'entité ID={}", entityId);
        return DeleteResult.SUCCESS;
    }

    @Transactional
    public DeleteResult deleteProduction(Long entityId) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getProduction() == null) return DeleteResult.NOTHING_TO_DELETE;
        referenceOpenthesoRepository.deleteById(e.getProduction().getId());
        e.setProduction(null);
        entityRepository.save(e);
        log.info("Production supprimée pour l'entité ID={}", entityId);
        return DeleteResult.SUCCESS;
    }

    @Transactional
    public DeleteResult deleteAireCirculation(Long entityId, Long referenceId) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getAiresCirculation() == null) return DeleteResult.NOTHING_TO_DELETE;
        ReferenceOpentheso toRemove = e.getAiresCirculation().stream()
                .filter(r -> r.getId() != null && r.getId().equals(referenceId))
                .findFirst().orElse(null);
        if (toRemove == null) return DeleteResult.NOTHING_TO_DELETE;
        e.getAiresCirculation().remove(toRemove);
        entityRepository.save(e); // orphanRemoval cascade supprime la référence
        log.info("Aire de circulation supprimée pour l'entité ID={}", entityId);
        return DeleteResult.SUCCESS;
    }

    private DeleteResult deleteFromCaracteristique(Long entityId, java.util.function.Consumer<CaracteristiquePhysique> deleter) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getCaracteristiquePhysique() == null) return DeleteResult.NOTHING_TO_DELETE;
        deleter.accept(e.getCaracteristiquePhysique());
        entityRepository.save(e);
        log.info("Référence supprimée de CaracteristiquePhysique pour l'entité ID={}", entityId);
        return DeleteResult.SUCCESS;
    }

    private DeleteResult deleteFromDescriptionPate(Long entityId, java.util.function.Consumer<DescriptionPate> deleter) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getDescriptionPate() == null) return DeleteResult.NOTHING_TO_DELETE;
        deleter.accept(e.getDescriptionPate());
        entityRepository.save(e);
        log.info("Référence supprimée de DescriptionPate pour l'entité ID={}", entityId);
        return DeleteResult.SUCCESS;
    }

    private DeleteResult deleteFromCpm(Long entityId, java.util.function.Consumer<CaracteristiquePhysiqueMonnaie> deleter) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getCaracteristiquePhysiqueMonnaie() == null) return DeleteResult.NOTHING_TO_DELETE;
        deleter.accept(e.getCaracteristiquePhysiqueMonnaie());
        entityRepository.save(e);
        return DeleteResult.SUCCESS;
    }

    @Transactional
    public DeleteResult deleteMateriau(Long entityId) { return deleteFromCpm(entityId, cpm -> cpm.setMateriau(null)); }
    @Transactional
    public DeleteResult deleteDenomination(Long entityId) { return deleteFromCpm(entityId, cpm -> cpm.setDenomination(null)); }
    @Transactional
    public DeleteResult deleteValeur(Long entityId) { return deleteFromCpm(entityId, cpm -> cpm.setValeur(null)); }
    @Transactional
    public DeleteResult deleteTechnique(Long entityId) { return deleteFromCpm(entityId, cpm -> cpm.setTechnique(null)); }
    @Transactional
    public DeleteResult deleteFabricationMonnaie(Long entityId) { return deleteFromCpm(entityId, cpm -> cpm.setFabrication(null)); }

    public enum DeleteResult { SUCCESS, NOTHING_TO_DELETE }
}
