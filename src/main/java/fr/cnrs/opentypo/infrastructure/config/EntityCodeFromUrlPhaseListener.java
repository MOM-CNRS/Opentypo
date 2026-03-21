package fr.cnrs.opentypo.infrastructure.config;

import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;
import lombok.extern.slf4j.Slf4j;

/**
 * PhaseListener exécuté en RESTORE_VIEW (BEFORE_PHASE) pour traiter le code d'entité
 * depuis l'URL (/DECOCER) avant la construction de la vue.
 * Cela garantit que {@code applicationBean.showTreePanel} est à {@code true} avant
 * que l'attribut {@code rendered} du panneau arbre soit évalué, corrigeant le cas
 * où l'arbre n'apparaît pas au premier chargement via URL dans un nouveau navigateur.
 */
@Slf4j
public class EntityCodeFromUrlPhaseListener implements PhaseListener {

    private static final long serialVersionUID = 1L;

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        var fc = event.getFacesContext();
        if (fc == null || fc.isPostback()) {
            return;
        }
        Object codeAttr = fc.getExternalContext().getRequestMap().get("entityCodeFromUrl");
        if (!(codeAttr instanceof String code) || code.isBlank()) {
            return;
        }
        try {
            Object bean = fc.getApplication().evaluateExpressionGet(fc, "#{applicationBean}", Object.class);
            if (bean != null && bean instanceof fr.cnrs.opentypo.presentation.bean.ApplicationBean appBean) {
                appBean.handleEntityCodeFromUrlIfPresent();
            }
        } catch (Exception e) {
            log.warn("EntityCodeFromUrlPhaseListener: erreur lors du traitement du code d'entité: {}", e.getMessage());
        }
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        // Non utilisé
    }
}
