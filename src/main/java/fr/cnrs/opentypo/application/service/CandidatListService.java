package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.Candidat;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour la gestion de la liste des candidats
 * Responsabilité: Chargement, conversion et filtrage des candidats
 * 
 * Principe SOLID appliqué: Single Responsibility Principle
 * Ce service a une seule responsabilité: gérer la liste des candidats
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class CandidatListService {

    @Inject
    private EntityRepository entityRepository;

    /**
     * Charge tous les candidats depuis la base de données
     * @return Liste de tous les candidats (PROPOSITION, PUBLIQUE, REFUSED)
     */
    public List<Candidat> loadAllCandidats() {
        try {
            List<Entity> entitiesProposition = entityRepository.findByStatut(EntityStatusEnum.PROPOSITION.name());
            List<Entity> entitiesAccepted = entityRepository.findByStatut(EntityStatusEnum.PUBLIQUE.name());
            List<Entity> entitiesRefused = entityRepository.findByStatut(EntityStatusEnum.REFUSE.name());
            
            List<Candidat> allCandidats = new ArrayList<>();
            allCandidats.addAll(convertToCandidats(entitiesProposition));
            allCandidats.addAll(convertToCandidats(entitiesAccepted));
            allCandidats.addAll(convertToCandidats(entitiesRefused));
            
            log.info("Chargement des candidats terminé: {} PROPOSITION, {} PUBLIQUE, {} REFUSED", 
                entitiesProposition.size(), entitiesAccepted.size(), entitiesRefused.size());
            
            return allCandidats;
        } catch (Exception e) {
            log.error("Erreur lors du chargement des candidats depuis la base de données", e);
            return new ArrayList<>();
        }
    }

    /**
     * Convertit une liste d'entités en candidats
     */
    public List<Candidat> convertToCandidats(List<Entity> entities) {
        return entities.stream()
            .map(this::convertEntityToCandidat)
            .collect(Collectors.toList());
    }

    /**
     * Convertit une Entity en Candidat pour l'affichage
     */
    public Candidat convertEntityToCandidat(Entity entity) {
        if (entity == null) {
            return null;
        }
        
        Candidat candidat = new Candidat();
        candidat.setId(entity.getId());
        
        // Type d'entité
        if (entity.getEntityType() != null) {
            candidat.setTypeCode(entity.getEntityType().getCode());
        }
        
        // Label principal (premier label trouvé)
        if (entity.getLabels() != null && !entity.getLabels().isEmpty()) {
            Label firstLabel = entity.getLabels().iterator().next();
            candidat.setLabel(firstLabel.getNom());
            if (firstLabel.getLangue() != null) {
                candidat.setLangue(firstLabel.getLangue().getCode());
            }
        } else {
            candidat.setLabel(entity.getNom());
        }
        
        // Période, TPQ, TAQ
        candidat.setTpq(entity.getTpq());
        candidat.setTaq(entity.getTaq());
        
        // Production
        if (entity.getProduction() != null) {
            candidat.setProduction(entity.getProduction().getValeur());
        }
        
        // Ateliers
        candidat.setAteliers(entity.getAteliers());
        
        // Aire de circulation (première trouvée)
        if (entity.getAiresCirculation() != null && !entity.getAiresCirculation().isEmpty()) {
            candidat.setAireCirculation(entity.getAiresCirculation().iterator().next().getValeur());
        }
        
        // Dates
        candidat.setDateCreation(entity.getCreateDate());
        candidat.setDateModification(entity.getCreateDate());
        
        // Créateur
        candidat.setCreateur(entity.getCreateBy() != null ? entity.getCreateBy() : "");
        
        // Statut
        if (EntityStatusEnum.PROPOSITION.name().equals(entity.getStatut())) {
            candidat.setStatut(Candidat.Statut.EN_COURS);
        } else if (EntityStatusEnum.PUBLIQUE.name().equals(entity.getStatut())) {
            candidat.setStatut(Candidat.Statut.VALIDE);
        } else if (EntityStatusEnum.REFUSE.name().equals(entity.getStatut())) {
            candidat.setStatut(Candidat.Statut.REFUSE);
        } else {
            candidat.setStatut(Candidat.Statut.EN_COURS);
        }
        
        return candidat;
    }

    /**
     * Filtre les candidats par statut
     */
    public List<Candidat> filterByStatut(List<Candidat> candidats, Candidat.Statut statut) {
        return candidats.stream()
            .filter(c -> c.getStatut() == statut)
            .collect(Collectors.toList());
    }

    /**
     * Retourne uniquement les candidats en cours
     */
    public List<Candidat> getCandidatsEnCours(List<Candidat> allCandidats) {
        return filterByStatut(allCandidats, Candidat.Statut.EN_COURS);
    }

    /**
     * Retourne uniquement les candidats validés
     */
    public List<Candidat> getCandidatsValides(List<Candidat> allCandidats) {
        return filterByStatut(allCandidats, Candidat.Statut.VALIDE);
    }

    /**
     * Retourne uniquement les candidats refusés
     */
    public List<Candidat> getCandidatsRefuses(List<Candidat> allCandidats) {
        return filterByStatut(allCandidats, Candidat.Statut.REFUSE);
    }
}
