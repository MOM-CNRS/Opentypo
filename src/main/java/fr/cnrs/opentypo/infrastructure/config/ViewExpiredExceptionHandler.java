package fr.cnrs.opentypo.infrastructure.config;

import jakarta.faces.FacesException;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Iterator;

/**
 * Gestionnaire d'exceptions personnalisé pour gérer les ViewExpiredException.
 * Redirige vers index.xhtml avec actualisation complète de la page (comme F5).
 */
public class ViewExpiredExceptionHandler extends ExceptionHandlerWrapper {

    private static final String INDEX_PAGE = "/index.xhtml";
    private static final String VIEW_EXPIRED_PARAM = "viewExpired=true";

    private final ExceptionHandler wrapped;

    public ViewExpiredExceptionHandler(ExceptionHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return wrapped;
    }

    @Override
    public void handle() throws FacesException {
        Iterator<ExceptionQueuedEvent> events = getUnhandledExceptionQueuedEvents().iterator();

        while (events.hasNext()) {
            ExceptionQueuedEvent event = events.next();
            ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();
            Throwable exception = context.getException();

            if (exception instanceof ViewExpiredException) {
                handleViewExpiredException(events, event);
            }
        }

        getWrapped().handle();
    }

    /**
     * Gère une ViewExpiredException : nettoyage de la session puis actualisation complète vers index.xhtml.
     * Pour les requêtes AJAX, écrit une réponse HTML qui force un rechargement total (window.location.replace).
     */
    private void handleViewExpiredException(Iterator<ExceptionQueuedEvent> events, ExceptionQueuedEvent event) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        if (facesContext == null || facesContext.getResponseComplete()) {
            events.remove();
            return;
        }

        try {
            ExceptionHandlerUtils.cleanupSecurityContext(facesContext, true);
            
            HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
            String contextPath = request.getContextPath();
            String redirectUrl = contextPath + INDEX_PAGE + "?" + VIEW_EXPIRED_PARAM + "&_r=" + System.currentTimeMillis();

            boolean isAjaxRequest = facesContext.getPartialViewContext().isAjaxRequest();

            if (isAjaxRequest) {
                // Requête AJAX : envoie une page HTML minimale qui force l'actualisation totale (comme F5).
                HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
                response.reset();
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(
                    "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/></head><body>" +
                    "<script>window.location.replace('" + redirectUrl + "');</script>" +
                    "<p>Redirection...</p></body></html>"
                );
                response.getWriter().flush();
            } else {
                facesContext.getExternalContext().redirect(redirectUrl);
            }
            
            facesContext.responseComplete();
            events.remove();
            
        } catch (IOException e) {
            events.remove();
            throw new FacesException("Erreur lors de la redirection après ViewExpiredException", e);
        }
    }
}

