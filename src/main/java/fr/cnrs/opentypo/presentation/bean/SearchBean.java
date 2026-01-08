package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
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
import java.util.stream.Collectors;

@Named("searchBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class SearchBean implements Serializable {

    @Inject
    private EntityRepository entityRepository;

    private String searchSelected;
    private String typeSelected;
    private String langSelected;
    
    private List<Entity> referentiels;

    @PostConstruct
    public void init() {
        loadReferentiels();
    }
    
    /**
     * Charge les référentiels depuis la base de données
     */
    public void loadReferentiels() {
        referentiels = new ArrayList<>();
        try {
            referentiels = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_REFERENTIEL);
            referentiels = referentiels.stream()
                .filter(r -> r.getPublique() != null && r.getPublique())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des référentiels depuis la base de données", e);
            referentiels = new ArrayList<>();
        }
    }

    public void applySearch() {
        // TODO: Implémenter la logique de recherche
    }
}

