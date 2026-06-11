package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.SitePageCode;
import fr.cnrs.opentypo.application.service.SitePageService;
import fr.cnrs.opentypo.domain.entity.SitePage;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Locale;

/**
 * Libellés dynamiques des liens footer (titres des pages statiques en base, par langue d'interface).
 */
@Named("sitePageFooterBean")
@SessionScoped
@Slf4j
public class SitePageFooterBean implements Serializable {

    @Autowired
    private SitePageService sitePageService;

    @Inject
    private LocaleBean localeBean;

    public String getContactLabel() {
        return getLinkLabel(SitePageCode.CONTACT.getCode());
    }

    public String getLegalLabel() {
        return getLinkLabel(SitePageCode.LEGAL.getCode());
    }

    public String getAccessibilityLabel() {
        return getLinkLabel(SitePageCode.ACCESSIBILITY.getCode());
    }

    public String getLinkLabel(String pageCode) {
        SitePageCode code = SitePageCode.fromCode(pageCode).orElse(null);
        if (code == null) {
            return pageCode != null ? pageCode : "";
        }
        String fallback = switch (code) {
            case CONTACT -> JsfMessages.get("footer.contact");
            case LEGAL -> JsfMessages.get("footer.legal");
            case ACCESSIBILITY -> JsfMessages.get("footer.accessibility");
        };
        try {
            SitePage page = sitePageService.getByPageAndLangue(code.getCode(), getDisplayLangueCode());
            if (page.getTitre() != null && !page.getTitre().isBlank()) {
                return page.getTitre().trim();
            }
        } catch (Exception e) {
            log.warn("Impossible de charger le titre footer pour la page {}", pageCode, e);
        }
        return fallback;
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
}
