package fr.cnrs.opentypo.bean;

import fr.cnrs.opentypo.models.Language;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("applicationBean")
@SessionScoped
@Getter
@Setter
public class ApplicationBean implements Serializable {

    private List<Language> languages;

    private boolean showCards = true;
    private boolean showReferentielPanel = false;
    private boolean showCategoryPanel = false;
    private boolean showGroupePanel = false;
    private boolean showSeriePanel = false;
    private boolean showTypePanel = false;
    
    // Propriétés pour le formulaire de création de catégorie
    private String categoryCode;
    private String categoryLabel;
    private String categoryDescription;

    @PostConstruct
    public void initialization() {
        showCards();

        languages = new ArrayList<>();
        languages.add(new Language(1, "fr", "Français", "fr"));
        languages.add(new Language(2, "an", "Anglais", "an"));
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
    }
    
    public void showReferentiel() {

        showCards = false;
        showReferentielPanel = true;
        showCategoryPanel = false;
        showGroupePanel = false;
        showSeriePanel = false;
        showTypePanel = false;
    }

    public void showCategory() {

        showCards = false;
        showCategoryPanel = true;
        showReferentielPanel = false;
        showGroupePanel = false;
        showSeriePanel = false;
        showTypePanel = false;
    }

    public void showGroupe() {

        showCards = false;
        showCategoryPanel = false;
        showReferentielPanel = false;
        showGroupePanel = true;
        showSeriePanel = false;
        showTypePanel = false;
    }

    public void showSerie() {

        showCards = false;
        showCategoryPanel = false;
        showReferentielPanel = false;
        showGroupePanel = false;
        showSeriePanel = true;
        showTypePanel = false;
    }

    public void showType() {

        showCards = false;
        showCategoryPanel = false;
        showReferentielPanel = false;
        showGroupePanel = false;
        showSeriePanel = false;
        showTypePanel = true;
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

