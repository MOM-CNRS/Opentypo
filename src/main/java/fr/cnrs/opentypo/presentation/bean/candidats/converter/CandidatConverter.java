package fr.cnrs.opentypo.presentation.bean.candidats.converter;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.presentation.bean.candidats.Candidat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Optional;

/**
 * Service de conversion entre Entity et Candidat
 * Centralise toute la logique de conversion pour maintenir la cohérence
 */
@Component
@Slf4j
public class CandidatConverter {

    /**
     * Convertit une Entity en Candidat pour l'affichage
     */
    public Candidat convertEntityToCandidat(Entity entity) {
        if (entity == null) {
            return null;
        }
        
        Candidat candidat = new Candidat();
        candidat.setId(entity.getId());
        candidat.setCode(entity.getCode());
        candidat.setTypeCode(entity.getEntityType() != null ? entity.getEntityType().getCode() : "");
        
        // Récupérer le label principal (premier label disponible ou nom par défaut)
        String label = entity.getNom();
        if (entity.getLabels() != null && !entity.getLabels().isEmpty()) {
            // Essayer de trouver un label en français, sinon prendre le premier
            Optional<Label> labelFr = entity.getLabels().stream()
                .filter(l -> l.getLangue() != null && "fr".equalsIgnoreCase(l.getLangue().getCode()))
                .findFirst();
            if (labelFr.isPresent()) {
                label = labelFr.get().getNom();
            } else {
                label = entity.getLabels().get(0).getNom();
            }
        }
        candidat.setLabel(label);
        
        // Récupérer la langue principale (premier label ou "fr" par défaut)
        String langue = "fr";
        if (entity.getLabels() != null && !entity.getLabels().isEmpty() && 
            entity.getLabels().get(0).getLangue() != null) {
            langue = entity.getLabels().get(0).getLangue().getCode();
        }
        candidat.setLangue(langue);
        
        // Période
        candidat.setPeriode(entity.getPeriode() != null ? entity.getPeriode().getValeur() : "");
        
        // Dates et autres champs
        candidat.setTpq(entity.getTpq());
        candidat.setTaq(entity.getTaq());
        candidat.setCommentaireDatation(entity.getCommentaire());
        candidat.setAppellationUsuelle(entity.getAppellation());
        
        // Description (première description disponible)
        String description = "";
        if (entity.getDescriptions() != null && !entity.getDescriptions().isEmpty()) {
            Optional<Description> descFr = entity.getDescriptions().stream()
                .filter(d -> d.getLangue() != null && "fr".equalsIgnoreCase(d.getLangue().getCode()))
                .findFirst();
            if (descFr.isPresent()) {
                description = descFr.get().getValeur();
            } else {
                description = entity.getDescriptions().get(0).getValeur();
            }
        }
        candidat.setDescription(description);
        
        candidat.setProduction(entity.getProduction() != null ? entity.getProduction().getValeur() : "");
        // Aire de circulation : concaténer toutes les valeurs avec "; "
        String aireCirculationStr = "";
        if (entity.getAiresCirculation() != null && !entity.getAiresCirculation().isEmpty()) {
            aireCirculationStr = entity.getAiresCirculation().stream()
                .filter(ref -> ReferenceOpenthesoEnum.AIRE_CIRCULATION.name().equals(ref.getCode()))
                .map(ref -> ref.getValeur())
                .filter(v -> v != null && !v.isEmpty())
                .collect(java.util.stream.Collectors.joining("; "));
        }
        candidat.setAireCirculation(aireCirculationStr);
        candidat.setCategorieFonctionnelle(entity.getCategorieFonctionnelle() != null ? 
            entity.getCategorieFonctionnelle().getValeur() : "");
        candidat.setReference(entity.getReference());
        candidat.setTypologiqueScientifique(entity.getTypologieScientifique());
        candidat.setIdentifiantPerenne(entity.getIdentifiantPerenne());
        candidat.setAncienneVersion(entity.getAncienneVersion());
        candidat.setBibliographie(entity.getBibliographie());
        
        // Dates
        candidat.setDateCreation(entity.getCreateDate());
        candidat.setDateModification(entity.getCreateDate());
        
        // Créateur
        candidat.setCreateur(entity.getCreateBy() != null ? entity.getCreateBy() : "");
        
        if (!CollectionUtils.isEmpty(entity.getAuteurs())) {
            candidat.setValidateur(entity.getAuteurs().getLast().getPrenom()
                    + " " + entity.getAuteurs().getLast().getNom().toUpperCase());
        }

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
}
