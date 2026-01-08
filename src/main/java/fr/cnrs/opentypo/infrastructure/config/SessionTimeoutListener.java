package fr.cnrs.opentypo.infrastructure.config;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener pour gérer l'expiration de session
 */
@WebListener
@Slf4j
public class SessionTimeoutListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.debug("Session créée: {}", se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        log.debug("Session expirée: {}", se.getSession().getId());
        // La session est déjà détruite, on ne peut plus accéder aux attributs
        // La redirection sera gérée par le filtre SessionExpirationFilter
    }
}

