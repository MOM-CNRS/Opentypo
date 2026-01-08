package fr.cnrs.opentypo.infrastructure.config;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.nio.charset.StandardCharsets;

/**
 * Utilitaires pour les gestionnaires d'exceptions JSF
 */
public final class ExceptionHandlerUtils {

    private ExceptionHandlerUtils() {
        // Classe utilitaire - pas d'instanciation
    }

    /**
     * Nettoie le contexte de sécurité Spring et la session HTTP
     * 
     * @param facesContext Le contexte JSF
     * @param invalidateSession Si true, invalide la session après nettoyage
     */
    public static void cleanupSecurityContext(FacesContext facesContext, boolean invalidateSession) {
        if (facesContext == null) {
            return;
        }

        try {
            // Nettoyer le contexte de sécurité Spring
            SecurityContextHolder.clearContext();

            // Nettoyer la session HTTP
            HttpServletRequest request = (HttpServletRequest) facesContext
                .getExternalContext().getRequest();
            
            HttpSession session = request.getSession(false);
            if (session != null) {
                try {
                    // Supprimer le contexte de sécurité de la session
                    session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                    
                    // Invalider la session si demandé
                    if (invalidateSession) {
                        session.invalidate();
                    }
                } catch (IllegalStateException e) {
                    // La session est déjà invalide, continuer
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs de nettoyage pour éviter de masquer l'exception originale
        }
    }

    /**
     * Encode un message pour être utilisé dans une URL
     * 
     * @param message Le message à encoder
     * @param maxLength Longueur maximale du message
     * @return Le message encodé
     */
    public static String encodeMessageForUrl(String message, int maxLength) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }

        // Limiter la taille du message
        String truncatedMessage = message.length() > maxLength 
            ? message.substring(0, maxLength) + "..." 
            : message;

        return java.net.URLEncoder.encode(truncatedMessage, StandardCharsets.UTF_8);
    }
}

