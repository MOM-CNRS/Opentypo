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
    
    private boolean showReferentielPanel = false;

    @PostConstruct
    public void initialization() {
        languages = new ArrayList<Language>();
        languages.add(new Language(1, "fr", "Français", "fr"));
        languages.add(new Language(2, "an", "Anglais", "an"));
    }
    
    public void showReferentiel() {
        showReferentielPanel = true;
    }
    
    public boolean isShowCards() {
        return !showReferentielPanel;
    }
}

