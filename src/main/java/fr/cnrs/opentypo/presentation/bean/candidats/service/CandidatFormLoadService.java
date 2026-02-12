package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Charge les données pour les listes déroulantes et sélecteurs du formulaire candidat.
 */
@Service
@Slf4j
public class CandidatFormLoadService {

    @Inject private EntityTypeRepository entityTypeRepository;
    @Inject private LangueRepository langueRepository;
    @Inject private UtilisateurRepository utilisateurRepository;

    public List<EntityType> loadEntityTypes() {
        try {
            return entityTypeRepository.findAll().stream()
                    .filter(et -> !EntityConstants.ENTITY_TYPE_REFERENCE.equals(et.getCode())
                            && !EntityConstants.ENTITY_TYPE_COLLECTION.equals(et.getCode()))
                    .toList();
        } catch (Exception e) {
            log.error("Erreur lors du chargement des types d'entités", e);
            return new ArrayList<>();
        }
    }

    public List<Langue> loadLanguages() {
        try {
            return langueRepository.findAllByOrderByNomAsc();
        } catch (Exception e) {
            log.error("Erreur lors du chargement des langues", e);
            return new ArrayList<>();
        }
    }

    public List<Utilisateur> loadAuteursSorted() {
        try {
            List<Utilisateur> list = utilisateurRepository.findAll();
            list.sort(Comparator
                    .comparing(Utilisateur::getNom, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Utilisateur::getPrenom, String.CASE_INSENSITIVE_ORDER));
            return list;
        } catch (Exception e) {
            log.error("Erreur lors du chargement des utilisateurs disponibles", e);
            return new ArrayList<>();
        }
    }
}
