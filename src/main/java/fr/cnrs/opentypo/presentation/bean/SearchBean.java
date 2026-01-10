package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Getter
@Setter
@SessionScoped
@Named("searchBean")
public class SearchBean implements Serializable {

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private Provider<ApplicationBean> applicationBeanProvider;

    private String searchSelected;
    private String collectionSelected;
    private String langSelected = "fr"; // Français sélectionné par défaut
    
    private List<Entity> references;
    private List<Entity> collections;

    @PostConstruct
    public void init() {
        loadreferences();
        loadCollections();
    }
    
    /**
     * Gère le changement de sélection de collection
     */
    public void onCollectionChange() {
        ApplicationBean appBean = applicationBeanProvider.get();
        if (appBean == null) {
            return;
        }
        
        if (collectionSelected == null || collectionSelected.isEmpty()) {
            // "Toutes les collections" est sélectionné - afficher le panel des collections
            appBean.showCollections();
        } else {
            // Une collection spécifique est sélectionnée - afficher ses détails
            collections.stream()
                    .filter(c -> c.getCode().equals(collectionSelected))
                    .findFirst().ifPresent(appBean::showCollectionDetail);

        }
    }
    
    /**
     * Charge les référentiels depuis la base de données
     */
    public void loadreferences() {
        references = new ArrayList<>();
        try {
            references = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_REFERENCE);
            references = references.stream()
                .filter(r -> r.getPublique() != null && r.getPublique())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des référentiels depuis la base de données", e);
            references = new ArrayList<>();
        }
    }

    /**
     * Charge les collections depuis la base de données
     */
    public void loadCollections() {
        collections = new ArrayList<>();
        try {
            collections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION);
            collections = collections.stream()
                .filter(c -> c.getPublique() != null && c.getPublique())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des collections depuis la base de données", e);
            collections = new ArrayList<>();
        }
    }

    public void applySearch() {
        // TODO: Implémenter la logique de recherche
    }
}

