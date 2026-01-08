package fr.cnrs.opentypo.infrastructure.config;

import jakarta.faces.FacesException;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Iterator;

/**
 * Gestionnaire d'exceptions global pour intercepter toutes les erreurs techniques
 * et rediriger vers la page d'erreur dédiée.
 * Note: Les ViewExpiredException sont gérées par ViewExpiredExceptionHandler.
 */
@Slf4j
public class GlobalExceptionHandler extends ExceptionHandlerWrapper {

    private static final String ERROR_PAGE = "/error.xhtml";
    private static final String MESSAGE_PARAM = "message";
    private static final int MAX_MESSAGE_LENGTH = 200;

    private final ExceptionHandler wrapped;

    public GlobalExceptionHandler(ExceptionHandler wrapped) {
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

            // Ignorer les ViewExpiredException - elles sont gérées par ViewExpiredExceptionHandler
            if (exception instanceof ViewExpiredException) {
                continue;
            }

            handleTechnicalException(events, event, exception);
        }

        // Laisser le gestionnaire parent gérer les autres exceptions
        getWrapped().handle();
    }

    /**
     * Gère une exception technique en la loggant et en redirigeant vers la page d'erreur.
     */
    private void handleTechnicalException(Iterator<ExceptionQueuedEvent> events, 
                                          ExceptionQueuedEvent event, 
                                          Throwable exception) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        if (facesContext == null || facesContext.getResponseComplete()) {
            events.remove();
            return;
        }

        try {
            // Logger l'erreur
                    log.error("Erreur technique interceptée", exception);
            
            // Obtenir et encoder le message d'erreur
            String errorMessage = getErrorMessage(exception);
            String encodedMessage = ExceptionHandlerUtils.encodeMessageForUrl(errorMessage, MAX_MESSAGE_LENGTH);
            
            // Nettoyer le contexte de sécurité (sans invalider la session)
            ExceptionHandlerUtils.cleanupSecurityContext(facesContext, false);
            
            // Rediriger vers la page d'erreur
            String redirectUrl = facesContext.getExternalContext().getRequestContextPath() 
                + ERROR_PAGE + "?" + MESSAGE_PARAM + "=" + encodedMessage;
            facesContext.getExternalContext().redirect(redirectUrl);
            facesContext.responseComplete();
            
            // Retirer l'événement de la queue
            events.remove();
            
        } catch (IOException e) {
                    log.error("Erreur lors de la redirection vers la page d'erreur", e);
            events.remove();
            throw new FacesException("Erreur lors de la redirection après exception", e);
        }
    }

    /**
     * Extrait un message d'erreur lisible depuis l'exception.
     */
    private String getErrorMessage(Throwable exception) {
        String errorMessage = exception.getMessage();
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            errorMessage = exception.getClass().getSimpleName();
        }
        return errorMessage;
    }
}

