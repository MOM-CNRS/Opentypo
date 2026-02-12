package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
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
    public void saveCommentaire(Long entityId, String commentaire) {
        updateEntity(entityId, e -> e.setCommentaire(commentaire != null ? commentaire.trim() : null));
    }

    @Transactional
    public void saveBibliographie(Long entityId, String bibliographie) {
        updateEntity(entityId, e -> e.setBibliographie(bibliographie != null ? bibliographie.trim() : null));
    }

    @Transactional
    public void saveReferencesBibliographiques(Long entityId, List<String> refs) {
        String joined = (refs != null && !refs.isEmpty()) ? String.join("; ", refs) : null;
        updateEntity(entityId, e -> e.setRereferenceBibliographique(joined));
    }

    @Transactional
    public void saveAteliers(Long entityId, List<String> ateliersList) {
        String joined = (ateliersList != null && !ateliersList.isEmpty()) ? String.join("; ", ateliersList) : null;
        updateEntity(entityId, e -> e.setAteliers(joined));
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
                e.setPublique(false); // Brouillon toujours privé
            } else {
                e.setPublique(publique != null ? publique : true);
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
