package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Named("searchBean")
@SessionScoped
@Getter
@Setter
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
            referentiels = entityRepository.findByEntityTypeCode("REFERENTIEL");
            // Filtrer uniquement les référentiels publics si nécessaire
            referentiels = referentiels.stream()
                .filter(r -> r.getPublique() != null && r.getPublique())
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des référentiels depuis la base de données: " + e.getMessage());
            referentiels = new ArrayList<>();
        }
    }

    public void applySearch() {

    }

}

