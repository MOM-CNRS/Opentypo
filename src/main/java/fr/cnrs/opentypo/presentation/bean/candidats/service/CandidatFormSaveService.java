package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysiqueMonnaie;
import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
import fr.cnrs.opentypo.domain.entity.DescriptionMonnaie;
import fr.cnrs.opentypo.domain.entity.DescriptionPate;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityMetadata;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service pour les sauvegardes des champs du formulaire candidat.
 */
@Service
@Slf4j
public class CandidatFormSaveService {

    @Inject
    private EntityRepository entityRepository;

    @Transactional
    public void saveCommentaireDatation(Long entityId, String commentaireDatation) {
        updateEntity(entityId, e -> e.setCommentaireDatation(commentaireDatation != null ? commentaireDatation.trim() : null));
    }

    @Transactional
    public void saveCommentaire(Long entityId, String commentaire) {
        updateEntity(entityId, e -> e.setCommentaire(commentaire != null ? commentaire.trim() : null));
    }

    @Transactional
    public void saveReference(Long entityId, String reference) {
        updateEntity(entityId, e -> e.setReference(reference != null ? reference.trim() : null));
    }

    @Transactional
    public void saveBibliographie(Long entityId, String bibliographie) {
        updateEntity(entityId, e -> e.setBibliographie(bibliographie != null ? bibliographie.trim() : null));
    }

    @Transactional
    public void saveMetadataCommentaire(Long entityId, String commentaire) {
        updateEntity(entityId, e -> e.setMetadataCommentaire(commentaire != null ? commentaire.trim() : null));
    }

    @Transactional
    public void saveAncienneVersion(Long entityId, String ancienneVersion) {
        updateEntity(entityId, e -> e.setAncienneVersion(ancienneVersion != null ? ancienneVersion.trim() : null));
    }

    @Transactional
    public void saveTypologieScientifique(Long entityId, String typologieScientifique) {
        updateEntity(entityId, e -> e.setTypologieScientifique(typologieScientifique != null ? typologieScientifique.trim() : null));
    }

    @Transactional
    public void saveIdentifiantPerenne(Long entityId, String identifiantPerenne) {
        updateEntity(entityId, e -> e.setIdentifiantPerenne(identifiantPerenne != null ? identifiantPerenne.trim() : null));
    }

    @Transactional
    public void saveReferencesBibliographiques(Long entityId, List<String> refs) {
        String joined = (refs != null && !refs.isEmpty()) ? String.join("; ", refs) : null;
        updateEntity(entityId, e -> e.setReferenceBibliographique(joined));
    }

    @Transactional
    public void saveSitesArcheologiques(Long entityId, List<String> sitesArcheologiques) {
        String joined = (sitesArcheologiques != null && !sitesArcheologiques.isEmpty()) ? String.join("; ", sitesArcheologiques) : null;
        updateEntity(entityId, e -> e.setSitesArcheologiques(joined));
    }

    @Transactional
    public void saveReferences(Long entityId, List<String> referentiel) {
        String joined = (referentiel != null && !referentiel.isEmpty()) ? String.join("; ", referentiel) : null;
        updateEntity(entityId, e -> e.setReferences(joined));
    }

    @Transactional
    public void saveAteliers(Long entityId, List<String> ateliersList) {
        String joined = (ateliersList != null && !ateliersList.isEmpty()) ? String.join("; ", ateliersList) : null;
        updateEntity(entityId, e -> e.setAteliers(joined));
    }

    @Transactional
    public void saveAttestations(Long entityId, List<String> attestations) {
        String joined = (attestations != null && !attestations.isEmpty()) ? String.join("; ", attestations) : null;
        updateEntity(entityId, e -> e.setAttestations(joined));
    }

    @Transactional
    public void saveMarquesEstampilles(Long entityId, List<String> marques) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null) return;
        DescriptionDetail dd = e.getDescriptionDetail();
        if (dd == null) {
            dd = new DescriptionDetail();
            dd.setEntity(e);
            e.setDescriptionDetail(dd);
        }
        String joined = (marques != null && !marques.isEmpty()) ? String.join("; ", marques) : null;
        dd.setMarques(joined);
        entityRepository.save(e);
        log.debug("Marques/estampilles sauvegardées pour l'entité ID: {}", entityId);
    }

    @Transactional
    public void saveDescriptionPate(Long entityId, String description) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null) return;
        DescriptionPate dp = e.getDescriptionPate();
        if (dp == null) {
            dp = new DescriptionPate();
            dp.setEntity(e);
            e.setDescriptionPate(dp);
        }
        dp.setDescription(description != null && !description.trim().isEmpty() ? description.trim() : null);
        entityRepository.save(e);
        log.debug("Description de pâte sauvegardée pour l'entité ID: {}", entityId);
    }

    @Transactional
    public void saveTypeDescription(Long entityId, String typeDescription) {
        updateEntity(entityId, e -> e.setCommentaire(typeDescription != null ? typeDescription.trim() : null));
    }

    @Transactional
    public void saveCollectionDescription(Long entityId, String collectionDescription) {
        updateEntity(entityId, e -> e.setCommentaire(collectionDescription != null ? collectionDescription.trim() : null));
    }

    @Transactional
    public void saveCollectionPublique(Long entityId, Boolean publique, String entityStatut) {
        updateEntity(entityId, e -> {
            if ("PROPOSITION".equals(entityStatut)) {
                // Brouillon : rester en PROPOSITION (la visibilité est définie à la validation)
            } else {
                e.setStatut(Boolean.TRUE.equals(publique) ? EntityStatusEnum.PUBLIQUE.name() : EntityStatusEnum.PRIVEE.name());
            }
        });
    }

    @Transactional
    public void saveTpq(Long entityId, Integer tpq) {
        updateEntity(entityId, e -> e.setTpq(tpq));
    }

    @Transactional
    public void saveTaq(Long entityId, Integer taq) {
        updateEntity(entityId, e -> e.setTaq(taq));
    }

    @Transactional
    public void saveDecors(Long entityId, String decors) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null) return;
        DescriptionDetail dd = e.getDescriptionDetail();
        if (dd == null) {
            dd = new DescriptionDetail();
            dd.setEntity(e);
            e.setDescriptionDetail(dd);
        }
        dd.setDecors(decors != null && !decors.trim().isEmpty() ? decors.trim() : null);
        entityRepository.save(e);
        log.debug("Décors sauvegardés pour l'entité ID: {}", entityId);
    }

    @Transactional
    public void saveDroit(Long entityId, String droit) {
        saveDescriptionMonnaieField(entityId, dm -> dm.setDroit(trimOrNull(droit)));
    }

    @Transactional
    public void saveLegendeDroit(Long entityId, String legendeDroit) {
        saveDescriptionMonnaieField(entityId, dm -> dm.setLegendeDroit(trimOrNull(legendeDroit)));
    }

    @Transactional
    public void saveCoinsMonetairesDroit(Long entityId, String value) {
        saveDescriptionMonnaieField(entityId, dm -> dm.setCoinsMonetairesDroit(trimOrNull(value)));
    }

    @Transactional
    public void saveRevers(Long entityId, String revers) {
        saveDescriptionMonnaieField(entityId, dm -> dm.setRevers(trimOrNull(revers)));
    }

    @Transactional
    public void saveLegendeRevers(Long entityId, String legendeRevers) {
        saveDescriptionMonnaieField(entityId, dm -> dm.setLegendeRevers(trimOrNull(legendeRevers)));
    }

    @Transactional
    public void saveCoinsMonetairesRevers(Long entityId, String value) {
        saveDescriptionMonnaieField(entityId, dm -> dm.setCoinsMonetairesRevers(trimOrNull(value)));
    }

    @Transactional
    public void saveMetrologieMonnaie(Long entityId, String metrologie) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null) return;
        CaracteristiquePhysiqueMonnaie cpm = e.getCaracteristiquePhysiqueMonnaie();
        if (cpm == null) {
            cpm = new CaracteristiquePhysiqueMonnaie();
            cpm.setEntity(e);
            e.setCaracteristiquePhysiqueMonnaie(cpm);
        }
        cpm.setMetrologie(trimOrNull(metrologie));
        entityRepository.save(e);
    }

    private void saveDescriptionMonnaieField(Long entityId, java.util.function.Consumer<DescriptionMonnaie> updater) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null) return;
        DescriptionMonnaie dm = e.getDescriptionMonnaie();
        if (dm == null) {
            dm = new DescriptionMonnaie();
            dm.setEntity(e);
            e.setDescriptionMonnaie(dm);
        }
        updater.accept(dm);
        entityRepository.save(e);
        log.debug("Description monnaie sauvegardée pour l'entité ID: {}", entityId);
    }

    private static String trimOrNull(String s) {
        return s != null && !s.trim().isEmpty() ? s.trim() : null;
    }

    @Transactional
    public void saveCorpus(Long entityId, String corpusExterne) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e == null) return;
        EntityMetadata meta = e.getMetadata();
        if (meta == null) {
            meta = new EntityMetadata();
            meta.setEntity(e);
            e.setMetadata(meta);
        }
        meta.setCorpusExterne(corpusExterne);
        entityRepository.save(e);
        log.debug("Corpus sauvegardé pour l'entité ID: {}", entityId);
    }

    private void updateEntity(Long entityId, java.util.function.Consumer<Entity> updater) {
        Entity e = entityRepository.findById(entityId).orElse(null);
        if (e != null) {
            updater.accept(e);
            entityRepository.save(e);
            log.debug("Entité ID={} mise à jour", entityId);
        }
    }
}
