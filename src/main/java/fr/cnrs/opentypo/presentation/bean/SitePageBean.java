package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.SitePageCode;
import fr.cnrs.opentypo.application.service.SitePageService;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.SitePage;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@Named("sitePageBean")
@ViewScoped
@Slf4j
public class SitePageBean implements Serializable {

    @Autowired
    private SitePageService sitePageService;

    @Autowired
    private LangueRepository langueRepository;

    @Inject
    private LocaleBean localeBean;

    @Autowired
    private LoginBean loginBean;

    private String pageCode;
    private boolean editing;
    private String editLangueCode;
    private String editTitre;
    private String editContenu;

    public void initPage(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        String normalized = SitePageCode.fromCode(code).map(SitePageCode::getCode).orElse(null);
        if (normalized == null) {
            return;
        }
        if (!normalized.equals(pageCode)) {
            pageCode = normalized;
            editing = false;
        }
    }

    public SitePage getCurrentPage() {
        if (pageCode == null) {
            return new SitePage();
        }
        return sitePageService.getByPageAndLangue(pageCode, getDisplayLangueCode());
    }

    public boolean canEditPage() {
        return loginBean != null && loginBean.isAdminTechniqueOrFonctionnel();
    }

    public List<Langue> getAvailableLanguages() {
        return langueRepository != null ? langueRepository.findAllByOrderByNomAsc() : List.of();
    }

    public String getPageIconClass() {
        return switch (SitePageCode.fromCode(pageCode).orElse(SitePageCode.CONTACT)) {
            case CONTACT -> "pi pi-envelope";
            case LEGAL -> "pi pi-briefcase";
            case ACCESSIBILITY -> "fa-solid fa-universal-access";
        };
    }

    public String getDisplayLangueLabel() {
        String code = getDisplayLangueCode();
        Langue langue = langueRepository != null ? langueRepository.findByCode(code) : null;
        return langue != null && langue.getNom() != null ? langue.getNom() : code;
    }

    public void startEdit() {
        if (!canEditPage()) {
            return;
        }
        loadPageForEdit(getDisplayLangueCode());
        editing = true;
    }

    public void cancelEdit() {
        editing = false;
    }

    public void onEditLangueChange() {
        if (editLangueCode != null && !editLangueCode.isEmpty()) {
            loadPageForEdit(editLangueCode);
        }
    }

    public void loadPageForEdit(String langueCode) {
        editLangueCode = langueCode;
        SitePage page = sitePageService.getByPageAndLangue(pageCode, langueCode);
        editTitre = page.getTitre();
        editContenu = page.getContenu();
    }

    @Transactional
    public void savePage() {
        if (!canEditPage()) {
            addError(JsfMessages.get("sitePage.error.noRights"));
            return;
        }
        if (editLangueCode == null || editLangueCode.isEmpty()) {
            addError(JsfMessages.get("sitePage.error.selectLang"));
            PrimeFaces.current().ajax().update(":sitePagePanel :growl :appFooter");
            return;
        }
        try {
            sitePageService.save(pageCode, editLangueCode, editTitre, editContenu);
            editing = false;
            addSuccess(JsfMessages.get("sitePage.success.saved"));
            PrimeFaces.current().ajax().update(":growl :sitePagePanel :appFooter");
        } catch (Exception e) {
            log.error("Erreur enregistrement page statique {}", pageCode, e);
            addError(JsfMessages.format("sitePage.error.save", e.getMessage() != null ? e.getMessage() : e.toString()));
            PrimeFaces.current().ajax().update(":sitePagePanel :growl :appFooter");
        }
    }

    public String goHome() {
        return "/index.xhtml?faces-redirect=true";
    }

    private String getDisplayLangueCode() {
        if (localeBean != null && localeBean.getLocale() != null) {
            String lang = localeBean.getLocale().getLanguage();
            if (Locale.ENGLISH.getLanguage().equalsIgnoreCase(lang)) {
                return "en";
            }
        }
        return "fr";
    }

    private void addError(String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"), detail));
    }

    private void addSuccess(String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, JsfMessages.get("common.growl.success"), detail));
    }
}
