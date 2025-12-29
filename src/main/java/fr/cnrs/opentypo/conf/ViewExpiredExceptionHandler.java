package fr.cnrs.opentypo.conf;

import jakarta.faces.FacesException;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.io.IOException;
import java.util.Iterator;

/**
 * Gestionnaire d'exceptions personnalisé pour gérer les ViewExpiredException
 * Redirige vers index.xhtml en nettoyant la session
 */
public class ViewExpiredExceptionHandler extends ExceptionHandlerWrapper {

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
                FacesContext facesContext = FacesContext.getCurrentInstance();
                
                if (facesContext != null && !facesContext.getResponseComplete()) {
                    try {
                        // Nettoyer le contexte de sécurité Spring
                        SecurityContextHolder.clearContext();
                        
                        // Nettoyer la session HTTP
                        HttpServletRequest request = (HttpServletRequest) facesContext
                            .getExternalContext().getRequest();
                        HttpSession session = request.getSession(false);
                        
                        if (session != null) {
                            // Supprimer le contexte de sécurité de la session
                            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                            // Invalider la session pour forcer une nouvelle session
                            session.invalidate();
                        }
                        
                        // Rediriger vers index.xhtml
                        String redirectUrl = request.getContextPath() + "/index.xhtml?viewExpired=true";
                        facesContext.getExternalContext().redirect(redirectUrl);
                        facesContext.responseComplete();
                        
                        // Retirer l'événement de la queue
                        events.remove();
                        
                    } catch (IOException e) {
                        throw new FacesException("Erreur lors de la redirection après ViewExpiredException", e);
                    }
                }
            }
        }

        // Laisser le gestionnaire parent gérer les autres exceptions
        getWrapped().handle();
    }
}

