package fr.cnrs.opentypo.infrastructure.config;

import jakarta.faces.FacesException;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Iterator;

/**
 * Gestionnaire d'exceptions personnalisé pour gérer les ViewExpiredException.
 * Redirige vers index.xhtml en nettoyant la session.
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

        // Laisser le gestionnaire parent gérer les autres exceptions
        getWrapped().handle();
    }

    /**
     * Gère une ViewExpiredException en nettoyant la session et redirigeant vers la page d'accueil.
     */
    private void handleViewExpiredException(Iterator<ExceptionQueuedEvent> events, ExceptionQueuedEvent event) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        if (facesContext == null || facesContext.getResponseComplete()) {
            events.remove();
            return;
        }

        try {
            // Nettoyer le contexte de sécurité et invalider la session
            ExceptionHandlerUtils.cleanupSecurityContext(facesContext, true);
            
            // Rediriger vers index.xhtml
            HttpServletRequest request = (HttpServletRequest) facesContext
                .getExternalContext().getRequest();
            String redirectUrl = request.getContextPath() + INDEX_PAGE + "?" + VIEW_EXPIRED_PARAM;
            facesContext.getExternalContext().redirect(redirectUrl);
            facesContext.responseComplete();
            
            // Retirer l'événement de la queue
            events.remove();
            
        } catch (IOException e) {
            // Si la redirection échoue, retirer quand même l'événement pour éviter une boucle
            events.remove();
            throw new FacesException("Erreur lors de la redirection après ViewExpiredException", e);
        }
    }
}

