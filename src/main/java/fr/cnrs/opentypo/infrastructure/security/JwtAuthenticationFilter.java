package fr.cnrs.opentypo.infrastructure.security;

import fr.cnrs.opentypo.application.dto.api.ApiErrorMessages;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authentifie les requêtes {@code /api/**} via {@code Authorization: Bearer &lt;JWT&gt;} uniquement
 * (le cookie de session JSF n'est pas accepté sur l'API REST).
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith(request.getContextPath() + "/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (isProtectedApiRequest(request)) {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header == null || !header.startsWith(BEARER_PREFIX)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (token.isEmpty()) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            try {
                SecurityContextHolder.getContext().setAuthentication(jwtService.parseAuthentication(token));
            } catch (JwtException ex) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        ApiErrorMessages.toJson(
                                HttpServletResponse.SC_UNAUTHORIZED, ApiErrorMessages.INVALID_TOKEN));
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                try {
                    SecurityContextHolder.getContext().setAuthentication(jwtService.parseAuthentication(token));
                } catch (JwtException ignored) {
                    // Interface JSF : session HTTP inchangée si le Bearer est invalide
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isProtectedApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.contains("/api/")) {
            return false;
        }
        String contextPath = request.getContextPath();
        String authPrefix = (contextPath != null ? contextPath : "") + "/api/v1/auth/";
        return !path.startsWith(authPrefix);
    }
}
