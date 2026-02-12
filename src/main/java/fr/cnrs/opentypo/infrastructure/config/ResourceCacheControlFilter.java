package fr.cnrs.opentypo.infrastructure.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * En développement (profil {@code local}), ajoute des en-têtes HTTP pour éviter
 * la mise en cache des ressources statiques (CSS, JS) par le navigateur.
 * Les modifications de styles sont ainsi visibles sans vider le cache ou ouvrir une fenêtre privée.
 */
@Component
@Profile("local")
@Order(1)
@WebFilter(urlPatterns = "/javax.faces.resource/*")
public class ResourceCacheControlFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache");
        httpResponse.setDateHeader("Expires", 0);

        chain.doFilter(request, response);
    }
}
