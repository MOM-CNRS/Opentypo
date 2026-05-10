package fr.cnrs.opentypo.presentation.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Locale;

/**
 * Gestion de la langue d’interface (session utilisateur). Utilisé par {@code f:view locale}
 * et par le sélecteur de langue du bandeau (actions {@link #switchToFrench} / {@link #switchToEnglish}).
 */
@Named("localeBean")
@SessionScoped
public class LocaleBean implements Serializable {

    public static final String SESSION_LOCALE_KEY = "fr.cnrs.opentypo.locale";

    private static final long serialVersionUID = 1L;

    private static final String IMG_LIBRARY = "img";

    /** URL résolvable du SVG (drapeau langue active), pour fond du bouton. */
    public String getActiveFlagUrl() {
        return flagResourceUrl(isFrench() ? "flags/fr.svg" : "flags/gb.svg");
    }

    private static String flagResourceUrl(String name) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            return "";
        }
        ResourceHandler rh = fc.getApplication().getResourceHandler();
        Resource resource = rh.createResource(name, IMG_LIBRARY);
        if (resource == null) {
            return "";
        }
        return fc.getExternalContext().encodeResourceURL(resource.getRequestPath());
    }

    /**
     * Locale active pour la vue JSF et les resource bundles.
     */
    public Locale getLocale() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            return Locale.FRENCH;
        }
        Object stored = fc.getExternalContext().getSessionMap().get(SESSION_LOCALE_KEY);
        if (stored instanceof Locale locale) {
            return locale;
        }
        return fc.getApplication().getDefaultLocale();
    }

    /**
     * Balise HTML {@code lang} / usage PrimeFaces.
     */
    public String getLanguageTag() {
        return getLocale().toLanguageTag();
    }

    public boolean isFrench() {
        return Locale.FRENCH.getLanguage().equals(getLocale().getLanguage());
    }

    public boolean isEnglish() {
        return Locale.ENGLISH.getLanguage().equals(getLocale().getLanguage());
    }

    public String switchToFrench() {
        return applyLocale(Locale.FRENCH);
    }

    public String switchToEnglish() {
        return applyLocale(Locale.ENGLISH);
    }

    /**
     * Bascule la langue puis recharge la page courante (évite les incohérences AJAX / bundles).
     */
    private String applyLocale(Locale locale) {
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.getExternalContext().getSessionMap().put(SESSION_LOCALE_KEY, locale);
        if (fc.getViewRoot() != null) {
            fc.getViewRoot().setLocale(locale);
            String viewId = fc.getViewRoot().getViewId();
            return viewId + "?faces-redirect=true";
        }
        return "/index.xhtml?faces-redirect=true";
    }
}
