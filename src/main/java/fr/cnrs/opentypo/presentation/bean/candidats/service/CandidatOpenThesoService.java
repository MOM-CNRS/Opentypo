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
            List<ReferenceOpentheso> l = e.getFonctionsUsage();
            if (l != null && !l.isEmpty()) {
                ReferenceOpentheso r = l.get(0);
                if (r != null) r.getValeur();
                return r;
            }
            return null;
        });
    }

    public ReferenceOpentheso loadMetrologie(Long entityId) {
        return loadRefFromCaracteristique(entityId, CaracteristiquePhysique::getMetrologie);
    }

    public ReferenceOpentheso loadFabrication(Long entityId) {
        return loadRefFromEntity(entityId, e -> {
            List<ReferenceOpentheso> l = e.getFabricationsFaconnage();
            if (l != null && !l.isEmpty()) {
                ReferenceOpentheso r = l.get(0);
                if (r != null) r.getValeur();
                return r;
            }
            return null;
        });
    }

    public ReferenceOpentheso loadCouleurPate(Long entityId) {
        return loadRefFromEntity(entityId, e -> firstRef(e.getCouleursPate()));
    }

    public ReferenceOpentheso loadNaturePate(Long entityId) {
        return loadRefFromEntity(entityId, e -> firstRef(e.getNaturesPate()));
    }

    public ReferenceOpentheso loadInclusions(Long entityId) {
        return loadRefFromEntity(entityId, e -> firstRef(e.getInclusionsPate()));
    }

    public ReferenceOpentheso loadCuissonPostCuisson(Long entityId) {
        return loadRefFromEntity(entityId, e -> firstRef(e.getCuissonsPostCuisson()));
    }

    private static ReferenceOpentheso firstRef(List<ReferenceOpentheso> l) {
        if (l == null || l.isEmpty()) return null;
        ReferenceOpentheso r = l.get(0);
        if (r != null) r.getValeur();
        return r;
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

    public ReferenceOpentheso loadMateriaux(Long entityId) {
        return loadRefFromCpm(entityId, CaracteristiquePhysiqueMonnaie::getMateriaux);
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
        if (e == null || e.getFonctionsUsage() == null || e.getFonctionsUsage().isEmpty()) {
            return DeleteResult.NOTHING_TO_DELETE;
        }
        e.getFonctionsUsage().clear();
        entityRepository.save(e);
        log.info("Fonctions / usages vidées pour l'entité ID={}", entityId);
        return DeleteResult.SUCCESS;
    }

    @Transactional
    public DeleteResult deleteMetrologie(Long entityId) {
        return deleteFromCaracteristique(entityId, cp -> cp.setMetrologie(null));
    }

    @Transactional
    public DeleteResult deleteFabrication(Long entityId) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getFabricationsFaconnage() == null || e.getFabricationsFaconnage().isEmpty()) {
            return DeleteResult.NOTHING_TO_DELETE;
        }
        e.getFabricationsFaconnage().clear();
        entityRepository.save(e);
        return DeleteResult.SUCCESS;
    }

    @Transactional
    public DeleteResult deleteCouleurPate(Long entityId) {
        return clearEntityOpenthesoList(entityId, Entity::getCouleursPate);
    }

    @Transactional
    public DeleteResult deleteNaturePate(Long entityId) {
        return clearEntityOpenthesoList(entityId, Entity::getNaturesPate);
    }

    @Transactional
    public DeleteResult deleteInclusions(Long entityId) {
        return clearEntityOpenthesoList(entityId, Entity::getInclusionsPate);
    }

    @Transactional
    public DeleteResult deleteCuissonPostCuisson(Long entityId) {
        return clearEntityOpenthesoList(entityId, Entity::getCuissonsPostCuisson);
    }

    private DeleteResult clearEntityOpenthesoList(Long entityId,
            java.util.function.Function<Entity, List<ReferenceOpentheso>> listGetter) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null) return DeleteResult.NOTHING_TO_DELETE;
        List<ReferenceOpentheso> list = listGetter.apply(e);
        if (list == null || list.isEmpty()) return DeleteResult.NOTHING_TO_DELETE;
        list.clear();
        entityRepository.save(e);
        return DeleteResult.SUCCESS;
    }

    @Transactional
    public DeleteResult deletePeriode(Long entityId) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getPeriodes() == null || e.getPeriodes().isEmpty()) {
            return DeleteResult.NOTHING_TO_DELETE;
        }
        e.getPeriodes().clear();
        entityRepository.save(e);
        log.info("Liste périodes vidée pour l'entité ID={}", entityId);
        return DeleteResult.SUCCESS;
    }

    @Transactional
    public DeleteResult deleteProduction(Long entityId) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getProductions() == null || e.getProductions().isEmpty()) {
            return DeleteResult.NOTHING_TO_DELETE;
        }
        e.getProductions().clear();
        entityRepository.save(e);
        log.info("Liste production vidée pour l'entité ID={}", entityId);
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

    /** Supprime une aire de circulation par son conceptId (pour le mode multiple autoComplete). */
    @Transactional
    public DeleteResult deleteAireCirculationByConceptId(Long entityId, String conceptId) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null || e.getAiresCirculation() == null || conceptId == null) return DeleteResult.NOTHING_TO_DELETE;
        ReferenceOpentheso toRemove = e.getAiresCirculation().stream()
                .filter(r -> conceptId.equals(r.getConceptId()))
                .findFirst().orElse(null);
        if (toRemove == null) return DeleteResult.NOTHING_TO_DELETE;
        e.getAiresCirculation().remove(toRemove);
        entityRepository.save(e);
        log.info("Aire de circulation (conceptId={}) supprimée pour l'entité ID={}", conceptId, entityId);
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
    public DeleteResult deleteMateriaux(Long entityId) { return deleteFromCpm(entityId, cpm -> cpm.setMateriaux(null)); }
    @Transactional
    public DeleteResult deleteDenomination(Long entityId) { return deleteFromCpm(entityId, cpm -> cpm.setDenomination(null)); }
    @Transactional
    public DeleteResult deleteValeur(Long entityId) { return deleteFromCpm(entityId, cpm -> cpm.setValeur(null)); }
    @Transactional
    public DeleteResult deleteTechnique(Long entityId) { return deleteFromCpm(entityId, cpm -> cpm.setTechnique(null)); }
    public enum DeleteResult { SUCCESS, NOTHING_TO_DELETE }
}
