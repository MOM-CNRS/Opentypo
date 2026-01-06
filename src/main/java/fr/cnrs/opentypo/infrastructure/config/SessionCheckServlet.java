package fr.cnrs.opentypo.infrastructure.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet pour vérifier l'état de la session
 * Utilisé par le polling côté client pour détecter l'expiration
 */
@WebServlet("/session-check")
public class SessionCheckServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        boolean sessionValid = false;
        boolean authenticated = false;
        
        if (session != null) {
            try {
                // Vérifier le temps d'inactivité de la session
                long timeSinceLastAccess = System.currentTimeMillis() - session.getLastAccessedTime();
                long maxInactiveInterval = session.getMaxInactiveInterval() * 1000L; // Convertir en millisecondes
                
                // Si la session a dépassé le temps d'inactivité maximum, elle est expirée
                if (maxInactiveInterval > 0 && timeSinceLastAccess > maxInactiveInterval) {
                    // Session expirée par l'inactivité
                    sessionValid = false;
                } else {
                    // Vérifier si le contexte de sécurité existe
                    Object securityContext = session.getAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                    
                    if (securityContext != null) {
                        sessionValid = true;
                        // Vérifier si l'utilisateur est authentifié
                        authenticated = SecurityContextHolder.getContext().getAuthentication() != null
                            && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                            && !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser");
                    }
                }
            } catch (IllegalStateException e) {
                // Session invalide
                sessionValid = false;
            }
        }
        
        // Retourner l'état de la session en JSON
        PrintWriter out = response.getWriter();
        out.print("{\"valid\":" + sessionValid + ",\"authenticated\":" + authenticated + "}");
        out.flush();
    }
}

