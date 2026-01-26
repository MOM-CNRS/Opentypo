package fr.cnrs.opentypo.presentation.bean.candidats;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.presentation.bean.SearchBean;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bean pour la gestion des données de sélection (types, langues, collections)
 * Responsable du chargement et de la fourniture des listes pour les composants de sélection
 */
@Named("candidatSelectionDataBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class CandidatSelectionDataBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private SearchBean searchBean;

    private List<EntityType> availableEntityTypes;
    private List<Langue> availableLanguages;
    private List<Entity> availableCollections;

    @PostConstruct
    public void init() {
        loadAvailableEntityTypes();
        loadAvailableLanguages();
        availableCollections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION);
    }

    /**
     * Charge les types d'entités disponibles (sauf REFERENTIEL)
     */
    public void loadAvailableEntityTypes() {
        try {
            availableEntityTypes = entityTypeRepository.findAll().stream()
                .filter(et -> !EntityConstants.ENTITY_TYPE_REFERENCE.equals(et.getCode()))
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des types d'entités", e);
            availableEntityTypes = new ArrayList<>();
        }
    }

    /**
     * Charge les langues disponibles
     */
    public void loadAvailableLanguages() {
        try {
            availableLanguages = langueRepository.findAllByOrderByNomAsc();
        } catch (Exception e) {
            log.error("Erreur lors du chargement des langues", e);
            availableLanguages = new ArrayList<>();
        }
    }

    /**
     * Récupère le nom d'un type d'entité
     */
    public String getEntityTypeName(EntityType entityType) {
        if (entityType == null) {
            return "";
        }
        return entityType.getCode();
    }

    /**
     * Récupère le nom d'une langue sélectionnée
     */
    public String getSelectedLangueName(String langueCode) {
        if (langueCode == null || langueCode.isEmpty()) {
            return "";
        }
        Langue langue = langueRepository.findByCode(langueCode);
        return langue != null ? langue.getNom() : langueCode;
    }

    /**
     * Récupère le label d'une collection pour l'affichage
     */
    public String getCollectionLabel(Entity collection) {
        if (collection == null) {
            return "";
        }
        
        String selectedLangue = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        Optional<Label> existingLabel = collection.getLabels().stream()
            .filter(l -> l.getLangue() != null && l.getLangue().getCode().equalsIgnoreCase(selectedLangue))
            .findFirst();

        return existingLabel.isPresent() ? existingLabel.get().getNom() : collection.getCode();
    }
}
