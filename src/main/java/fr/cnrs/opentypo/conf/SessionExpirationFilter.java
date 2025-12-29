package fr.cnrs.opentypo.conf;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

/**
 * Filtre pour détecter l'expiration de session et rediriger vers index.xhtml
 */
@WebFilter(filterName = "sessionExpirationFilter", urlPatterns = {"/*"})
public class SessionExpirationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialisation non nécessaire
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        
        // Ignorer les ressources statiques et l'endpoint de vérification de session
        if (requestURI.contains("/resources/") || 
            requestURI.contains("/javax.faces.resource/") ||
            requestURI.contains("/session-check")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Pour index.xhtml, vérifier quand même la session mais ne pas rediriger
        // (permet de nettoyer la session si elle est expirée)
        if (requestURI.endsWith("/index.xhtml") ||
            requestURI.equals(contextPath + "/") ||
            requestURI.equals(contextPath)) {
            // Vérifier quand même la session pour la nettoyer si nécessaire
            if (session != null) {
                try {
                    long timeSinceLastAccess = System.currentTimeMillis() - session.getLastAccessedTime();
                    long maxInactiveInterval = session.getMaxInactiveInterval() * 1000L;
                    
                    if (maxInactiveInterval > 0 && timeSinceLastAccess > maxInactiveInterval) {
                        // Session expirée - nettoyer
                        SecurityContextHolder.clearContext();
                        session.invalidate();
                    }
                } catch (IllegalStateException e) {
                    // Session déjà invalide
                }
            }
            chain.doFilter(request, response);
            return;
        }
        
        // Vérifier si la session est invalide ou expirée
        if (session == null) {
            // Session expirée - rediriger vers index.xhtml
            SecurityContextHolder.clearContext();
            String redirectUrl = contextPath + "/index.xhtml?sessionExpired=true";
            httpResponse.sendRedirect(redirectUrl);
            return;
        }
        
        // Vérifier le temps d'inactivité de la session
        try {
            long timeSinceLastAccess = System.currentTimeMillis() - session.getLastAccessedTime();
            long maxInactiveInterval = session.getMaxInactiveInterval() * 1000L; // Convertir en millisecondes
            
            // Si la session a dépassé le temps d'inactivité maximum, elle est expirée
            if (maxInactiveInterval > 0 && timeSinceLastAccess > maxInactiveInterval) {
                // Session expirée par l'inactivité
                SecurityContextHolder.clearContext();
                session.invalidate();
                String redirectUrl = contextPath + "/index.xhtml?sessionExpired=true";
                httpResponse.sendRedirect(redirectUrl);
                return;
            }
            
            // Vérifier si l'authentification Spring Security existe toujours
            Object securityContext = session.getAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            
            if (securityContext == null) {
                // Le contexte de sécurité n'existe plus - session expirée
                SecurityContextHolder.clearContext();
                session.invalidate();
                String redirectUrl = contextPath + "/index.xhtml?sessionExpired=true";
                httpResponse.sendRedirect(redirectUrl);
                return;
            }
        } catch (IllegalStateException e) {
            // Session invalide
            SecurityContextHolder.clearContext();
            String redirectUrl = contextPath + "/index.xhtml?sessionExpired=true";
            httpResponse.sendRedirect(redirectUrl);
            return;
        }
        
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Nettoyage non nécessaire
    }
}
