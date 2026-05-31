package fr.cnrs.opentypo.application.import_typology;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpSession;

/**
 * Progression d'import/analyse stockée dans la session HTTP (partagée entre le thread worker et le poll AJAX).
 */
public final class TypologyImportProgressSession {

    public static final String SESSION_ATTRIBUTE = "typologyImportProgress";

    private TypologyImportProgressSession() {
    }

    public static TypologyImportProgress getOrCreate(HttpSession session) {
        if (session == null) {
            return new TypologyImportProgress();
        }
        Object existing = session.getAttribute(SESSION_ATTRIBUTE);
        if (existing instanceof TypologyImportProgress progress) {
            return progress;
        }
        TypologyImportProgress progress = new TypologyImportProgress();
        session.setAttribute(SESSION_ATTRIBUTE, progress);
        return progress;
    }

    public static TypologyImportProgress fromFacesContext() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            return new TypologyImportProgress();
        }
        HttpSession session = (HttpSession) fc.getExternalContext().getSession(false);
        return getOrCreate(session);
    }

    public static void bind(HttpSession session, TypologyImportProgress progress) {
        if (session != null && progress != null) {
            session.setAttribute(SESSION_ATTRIBUTE, progress);
        }
    }

    /** Réécrit l'attribut de session après mise à jour (visible par le poll AJAX). */
    public static void publish(HttpSession session, TypologyImportProgress progress) {
        bind(session, progress);
    }
}
