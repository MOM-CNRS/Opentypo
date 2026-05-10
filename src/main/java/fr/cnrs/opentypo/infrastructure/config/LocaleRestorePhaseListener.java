package fr.cnrs.opentypo.infrastructure.config;

import fr.cnrs.opentypo.presentation.bean.LocaleBean;
import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;

import java.util.Locale;

/**
 * Réapplique la locale stockée en session sur le {@code ViewRoot} après {@code RESTORE_VIEW},
 * pour que les libellés et composants JSF utilisent la bonne langue dès le rendu.
 */
public class LocaleRestorePhaseListener implements PhaseListener {

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        // rien : le ViewRoot n’existe pas encore
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        if (!PhaseId.RESTORE_VIEW.equals(event.getPhaseId())) {
            return;
        }
        var fc = event.getFacesContext();
        if (fc.getViewRoot() == null) {
            return;
        }
        Object loc = fc.getExternalContext().getSessionMap().get(LocaleBean.SESSION_LOCALE_KEY);
        if (loc instanceof Locale locale) {
            fc.getViewRoot().setLocale(locale);
        }
    }
}
