package fr.cnrs.opentypo.bean;

import fr.cnrs.opentypo.models.Language;
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

@Named("applicationBean")
@SessionScoped
@Getter
@Setter
public class ApplicationBean implements Serializable {

    public List<Language> languages;

    @PostConstruct
    public void initialization() {
        languages = new ArrayList<Language>();
        languages.add(new Language(1, "fr", "Français", "fr"));
        languages.add(new Language(2, "an", "Anglais", "an"));
    }
}

