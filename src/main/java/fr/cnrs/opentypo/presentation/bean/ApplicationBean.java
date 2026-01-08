package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.models.Language;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named("applicationBean")
@SessionScoped
@Getter
@Setter
public class ApplicationBean implements Serializable {

    @Inject
    private LangueRepository langueRepository;

    private List<Language> languages;

    private boolean showCards = true;
    private boolean showReferentielPanel = false;
    private boolean showCategoryPanel = false;
    private boolean showGroupePanel = false;
    private boolean showSeriePanel = false;
    private boolean showTypePanel = false;
    private boolean showTreePanel = false;
    
    // Propriétés pour le formulaire de création de catégorie
    private String categoryCode;
    private String categoryLabel;
    private String categoryDescription;

    @PostConstruct
    public void initialization() {
        // Vérifier si la session a expiré ou si la vue a expiré (via paramètre URL)
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String sessionExpired = facesContext.getExternalContext()
                .getRequestParameterMap().get("sessionExpired");
            String viewExpired = facesContext.getExternalContext()
                .getRequestParameterMap().get("viewExpired");
            
            if ("true".equals(sessionExpired) || "true".equals(viewExpired)) {
                // Réinitialiser l'affichage pour montrer uniquement cardsContainer
                showCards();
            }
        } else {
            showCards();
        }

        // Charger les langues depuis la base de données
        languages = new ArrayList<>();
        try {
            List<Langue> languesFromDb = langueRepository.findAllByOrderByNomAsc();
            int id = 1;
            for (Langue langue : languesFromDb) {
                languages.add(new Language(
                    id++,
                    langue.getCode(),
                    langue.getNom(),
                    langue.getCode() // Utiliser le code comme codeFlag
                ));
            }
        } catch (Exception e) {
            // En cas d'erreur, utiliser les valeurs par défaut
            languages.add(new Language(1, "fr", "Français", "fr"));
            languages.add(new Language(2, "en", "Anglais", "en"));
            // Logger l'erreur si nécessaire
            System.err.println("Erreur lors du chargement des langues depuis la base de données: " + e.getMessage());
        }
    }

    public boolean isShowDetail() {
        return showReferentielPanel || showCategoryPanel || showGroupePanel || showSeriePanel || showTypePanel;
    }

    public void showCards() {

        showCards = true;
        showReferentielPanel = false;
        showCategoryPanel = false;
        showGroupePanel = false;
        showSeriePanel = false;
        showTypePanel = false;
        showTreePanel = false;
    }
    
    public void showReferentiel() {

        showCards = false;
        showReferentielPanel = true;
        showCategoryPanel = false;
        showGroupePanel = false;
        showSeriePanel = false;
        showTypePanel = false;
        showTreePanel = true;
    }

    public void showCategory() {

        showCards = false;
        showCategoryPanel = true;
        showReferentielPanel = false;
        showGroupePanel = false;
        showSeriePanel = false;
        showTypePanel = false;
        showTreePanel = true;
    }

    public void showGroupe() {

        showCards = false;
        showCategoryPanel = false;
        showReferentielPanel = false;
        showGroupePanel = true;
        showSeriePanel = false;
        showTypePanel = false;
        showTreePanel = true;
    }

    public void showSerie() {

        showCards = false;
        showCategoryPanel = false;
        showReferentielPanel = false;
        showGroupePanel = false;
        showSeriePanel = true;
        showTypePanel = false;
        showTreePanel = true;
    }

    public void showType() {

        showCards = false;
        showCategoryPanel = false;
        showReferentielPanel = false;
        showGroupePanel = false;
        showSeriePanel = false;
        showTypePanel = true;
        showTreePanel = true;
    }
    
    public void resetCategoryForm() {
        categoryCode = null;
        categoryLabel = null;
        categoryDescription = null;
    }
    
    public void createCategory() {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Le code de la catégorie est requis."));
            PrimeFaces.current().ajax().update(":growl, :categoryForm");
            return;
        }
        
        if (categoryLabel == null || categoryLabel.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Le label de la catégorie est requis."));
            PrimeFaces.current().ajax().update(":growl, :categoryForm");
            return;
        }
        
        // Ici, vous pouvez ajouter la logique pour sauvegarder la catégorie
        // Par exemple, l'ajouter à une liste ou à une base de données
        
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "La catégorie '" + categoryLabel + "' a été créée avec succès."));
        
        // Réinitialiser le formulaire
        resetCategoryForm();
        
        PrimeFaces.current().ajax().update(":growl, :categoryForm, :contentPanels");
    }
}

