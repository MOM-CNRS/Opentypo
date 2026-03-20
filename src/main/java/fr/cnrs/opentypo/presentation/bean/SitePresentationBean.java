package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.service.SitePresentationService;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.SitePresentation;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@SessionScoped
@Named("sitePresentationBean")
@Slf4j
public class SitePresentationBean implements Serializable {

    @Autowired
    private SitePresentationService sitePresentationService;

    @Autowired
    private LangueRepository langueRepository;

    @Autowired
    private SearchBean searchBean;

    @Autowired
    private LoginBean loginBean;

    private boolean editing;
    private String editLangueCode;
    private String editTitre;
    private String editDescription;

    public SitePresentation getCurrentPresentation() {
        String lang = searchBean != null && searchBean.getLangSelected() != null
                ? searchBean.getLangSelected()
                : "fr";
        return sitePresentationService.getByLangueCode(lang);
    }

    /** Réservé aux administrateurs (technique ou fonctionnel). */
    public boolean canEditPresentation() {
        return loginBean != null && loginBean.isAdminTechniqueOrFonctionnel();
    }

    /** Langues de la table langue. */
    public List<Langue> getAvailableLanguages() {
        return langueRepository != null ? langueRepository.findAllByOrderByNomAsc() : List.of();
    }

    /** Passe en mode édition inline (même écran). */
    public void startEdit() {
        if (!canEditPresentation()) return;
        String lang = searchBean != null && searchBean.getLangSelected() != null
                ? searchBean.getLangSelected()
                : "fr";
        loadPresentationForEdit(lang);
        editing = true;
    }

    /** Annule le mode édition. */
    public void cancelEdit() {
        editing = false;
    }

    public void onEditLangueChange() {
        if (editLangueCode != null && !editLangueCode.isEmpty()) {
            loadPresentationForEdit(editLangueCode);
        }
    }

    public void loadPresentationForEdit(String langueCode) {
        editLangueCode = langueCode;
        SitePresentation p = sitePresentationService.getByLangueCode(langueCode);
        editTitre = p.getTitre();
        editDescription = p.getDescription();
    }

    @Transactional
    public void savePresentation() {
        if (!canEditPresentation()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Vous n'avez pas les droits pour modifier la présentation."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        if (editLangueCode == null || editLangueCode.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Veuillez sélectionner une langue."));
            PrimeFaces.current().ajax().update(":sitePresentationContent :growl");
            return;
        }
        try {
            sitePresentationService.save(editLangueCode, editTitre, editDescription);
            editing = false;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "La présentation a été enregistrée."));
            PrimeFaces.current().ajax().update(":growl :sitePresentationContent");
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de la présentation", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                            "Une erreur est survenue : " + (e.getMessage() != null ? e.getMessage() : e.toString())));
            PrimeFaces.current().ajax().update(":sitePresentationContent :growl");
        }
    }
}
