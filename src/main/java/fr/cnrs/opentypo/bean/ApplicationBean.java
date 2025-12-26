package fr.cnrs.opentypo.bean;

import fr.cnrs.opentypo.models.Language;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("applicationBean")
@SessionScoped
@Getter
@Setter
public class ApplicationBean implements Serializable {

    public List<Language> languages;

    private boolean showCards = true;
    private boolean showReferentielPanel = false;
    private boolean showCategoryPanel = false;
    private boolean showGroupePanel = false;
    private boolean showSeriePanel = false;
    private boolean showTypePanel = false;

    @PostConstruct
    public void initialization() {
        showCards();

        languages = new ArrayList<Language>();
        languages.add(new Language(1, "fr", "Français", "fr"));
        languages.add(new Language(2, "an", "Anglais", "an"));
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
}

