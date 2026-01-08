package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.common.models.Language;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.presentation.bean.util.PanelStateManager;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named("applicationBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class ApplicationBean implements Serializable {

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private EntityRepository entityRepository;

    private final PanelStateManager panelState = new PanelStateManager();

    private List<Language> languages;
    
    private List<Entity> referentiels;
    
    // Propriétés pour le formulaire de création de catégorie
    private String categoryCode;
    private String categoryLabel;
    private String categoryDescription;

    // Getters pour compatibilité avec XHTML
    public boolean isShowCards() { return panelState.isShowCards(); }
    public boolean isShowReferentielPanel() { return panelState.isShowReferentielPanel(); }
    public boolean isShowCategoryPanel() { return panelState.isShowCategoryPanel(); }
    public boolean isShowGroupePanel() { return panelState.isShowGroupePanel(); }
    public boolean isShowSeriePanel() { return panelState.isShowSeriePanel(); }
    public boolean isShowTypePanel() { return panelState.isShowTypePanel(); }
    public boolean isShowTreePanel() { return panelState.isShowTreePanel(); }

    @PostConstruct
    public void initialization() {
        checkSessionExpiration();
        loadLanguages();
        loadReferentiels();
    }

    /**
     * Vérifie si la session ou la vue a expiré
     */
    private void checkSessionExpiration() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String sessionExpired = facesContext.getExternalContext()
                .getRequestParameterMap().get(ViewConstants.PARAM_SESSION_EXPIRED);
            String viewExpired = facesContext.getExternalContext()
                .getRequestParameterMap().get(ViewConstants.PARAM_VIEW_EXPIRED);
            
            if (ViewConstants.PARAM_TRUE.equals(sessionExpired) 
                || ViewConstants.PARAM_TRUE.equals(viewExpired)) {
                panelState.showCards();
            }
        } else {
            panelState.showCards();
        }
    }

    /**
     * Charge les langues depuis la base de données
     */
    private void loadLanguages() {
        languages = new ArrayList<>();
        try {
            List<Langue> languesFromDb = langueRepository.findAllByOrderByNomAsc();
            int id = 1;
            for (Langue langue : languesFromDb) {
                languages.add(new Language(
                    id++,
                    langue.getCode(),
                    langue.getNom(),
                    langue.getCode()
                ));
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des langues depuis la base de données", e);
            // Valeurs par défaut en cas d'erreur
            languages.add(new Language(1, "fr", "Français", "fr"));
            languages.add(new Language(2, "en", "Anglais", "en"));
        }
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

    public boolean isShowDetail() {
        return panelState.isShowDetail();
    }

    public void showCards() {
        panelState.showCards();
    }
    
    public void showReferentiel() {
        panelState.showReferentiel();
    }

    public void showCategory() {
        panelState.showCategory();
    }

    public void showGroupe() {
        panelState.showGroupe();
    }

    public void showSerie() {
        panelState.showSerie();
    }

    public void showType() {
        panelState.showType();
    }
    
    public void resetCategoryForm() {
        categoryCode = null;
        categoryLabel = null;
        categoryDescription = null;
    }
    
    public void createCategory() {
        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateCode(
                categoryCode, entityRepository, ":categoryForm")) {
            return;
        }
        
        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateLabel(
                categoryLabel, ":categoryForm")) {
            return;
        }
        
        // TODO: Implémenter la logique de sauvegarde de la catégorie
        
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "La catégorie '" + categoryLabel + "' a été créée avec succès."));
        
        resetCategoryForm();
        PrimeFaces.current().ajax().update(":growl, :categoryForm, :contentPanels");
    }
}

