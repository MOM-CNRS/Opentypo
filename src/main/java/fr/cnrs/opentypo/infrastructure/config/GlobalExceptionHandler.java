package fr.cnrs.opentypo.infrastructure.config;

import jakarta.faces.FacesException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.io.IOException;
import java.util.Iterator;

/**
 * Gestionnaire d'exceptions global pour intercepter toutes les erreurs techniques
 * et rediriger vers la page d'erreur dédiée
 */
public class GlobalExceptionHandler extends ExceptionHandlerWrapper {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
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

            FacesContext facesContext = FacesContext.getCurrentInstance();
            
            if (facesContext != null && !facesContext.getResponseComplete()) {
                try {
                    // Logger l'erreur
                    logger.error("Erreur technique interceptée", exception);
                    
                    // Obtenir le message d'erreur
                    String errorMessage = exception.getMessage();
                    if (errorMessage == null || errorMessage.trim().isEmpty()) {
                        errorMessage = exception.getClass().getSimpleName();
                    }
                    
                    // Limiter la taille du message pour l'URL
                    if (errorMessage.length() > 200) {
                        errorMessage = errorMessage.substring(0, 200) + "...";
                    }
                    
                    // Encoder le message pour l'URL
                    String encodedMessage = java.net.URLEncoder.encode(errorMessage, "UTF-8");
                    
                    // Nettoyer le contexte de sécurité si nécessaire
                    try {
                        SecurityContextHolder.clearContext();
                        HttpServletRequest request = (HttpServletRequest) facesContext
                            .getExternalContext().getRequest();
                        HttpSession session = request.getSession(false);
                        if (session != null) {
                            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                        }
                    } catch (Exception e) {
                        // Ignorer les erreurs de nettoyage
                    }
                    
                    // Rediriger vers la page d'erreur
                    String redirectUrl = facesContext.getExternalContext().getRequestContextPath() 
                        + "/error.xhtml?message=" + encodedMessage;
                    facesContext.getExternalContext().redirect(redirectUrl);
                    facesContext.responseComplete();
                    
                    // Retirer l'événement de la queue
                    events.remove();
                    
                } catch (IOException e) {
                    logger.error("Erreur lors de la redirection vers la page d'erreur", e);
                    throw new FacesException("Erreur lors de la redirection après exception", e);
                }
            }
        }

        // Laisser le gestionnaire parent gérer les autres exceptions
        getWrapped().handle();
    }
}

