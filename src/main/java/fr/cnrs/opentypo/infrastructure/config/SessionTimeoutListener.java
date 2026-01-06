package fr.cnrs.opentypo.infrastructure.config;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener pour gérer l'expiration de session
 */
@WebListener
public class SessionTimeoutListener implements HttpSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionTimeoutListener.class);

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        logger.debug("Session créée: {}", se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        logger.debug("Session expirée: {}", se.getSession().getId());
        // La session est déjà détruite, on ne peut plus accéder aux attributs
        // La redirection sera gérée par le filtre SessionExpirationFilter
    }
}

