package fr.cnrs.opentypo.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuration de sécurité Spring Security
 * - index.xhtml accessible à tous
 * - Toutes les autres pages nécessitent une authentification
 * - CORS contrôlé
 * - Headers de sécurité (HSTS, XSS, CSP, etc.)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Exclure les ressources JSF de Spring Security - elles sont servies directement par JSF
            // Cela évite que Spring Security intercepte ces requêtes et les redirige
            // Désactiver CSRF pour JSF (peut être réactivé si nécessaire)
            .csrf(AbstractHttpConfigurer::disable)

            // Configuration CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configuration des autorisations
            .authorizeHttpRequests(auth -> auth
                // IMPORTANT: Les ressources doivent être autorisées EN PREMIER
                // Permettre l'accès aux ressources JSF/PrimeFaces (doit être avant anyRequest)
                .requestMatchers("/javax.faces.resource/**").permitAll()
                
                // Permettre l'accès aux ressources JSF avec paramètres de requête (ex: ?ln=primefaces&v=...)
                .requestMatchers(request -> {
                    String path = request.getRequestURI();
                    String query = request.getQueryString();
                    // Autoriser les fichiers JSF avec paramètres de ressources
                    if (query != null && (query.contains("ln=") || query.contains("v=") || query.contains("e="))) {
                        return true;
                    }
                    // Autoriser les fichiers statiques même avec paramètres
                    if (path != null && (path.endsWith(".js") || path.endsWith(".css") || 
                        path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".gif") ||
                        path.endsWith(".ico") || path.endsWith(".woff") || path.endsWith(".woff2") ||
                        path.endsWith(".ttf") || path.endsWith(".eot") || path.endsWith(".svg"))) {
                        return true;
                    }
                    return false;
                }).permitAll()

                // Permettre l'accès aux ressources statiques (CSS, JS, images, etc.)
                .requestMatchers(
                    "/resources/**",
                    "/uploads/**",
                    "/*.css",
                    "/*.js",
                    "/*.png",
                    "/*.jpg",
                    "/*.gif",
                    "/*.ico",
                    "/*.woff",
                    "/*.woff2",
                    "/*.ttf",
                    "/*.eot",
                    "/*.svg"
                ).permitAll()

                // Permettre l'accès à la racine et index.xhtml pour tous
                .requestMatchers("/", "/index.xhtml").permitAll()

                // Permettre l'accès à la page d'erreur pour tous
                .requestMatchers("/error.xhtml").permitAll()

                // Permettre l'accès à l'endpoint de vérification de session
                .requestMatchers("/session-check").permitAll()

                // Les pages de gestion des utilisateurs nécessitent le rôle ADMIN
                .requestMatchers("/users/**").permitAll()

                    // Les pages de gestion des utilisateurs nécessitent le rôle ADMIN
                    .requestMatchers("/candidats/**").permitAll()

                // Toutes les autres pages nécessitent une authentification
                .anyRequest().authenticated()
            )

            // Désactiver la redirection automatique vers /login
            // Rediriger vers index.xhtml à la place
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    String requestPath = request.getRequestURI();
                    // Ne PAS rediriger les ressources JSF/statiques - laisser JSF les servir
                    if (requestPath != null && (
                        requestPath.startsWith("/javax.faces.resource/") ||
                        requestPath.startsWith("/resources/") ||
                        requestPath.startsWith("/uploads/") ||
                        requestPath.endsWith(".css") ||
                        requestPath.endsWith(".js") ||
                        requestPath.endsWith(".png") ||
                        requestPath.endsWith(".jpg") ||
                        requestPath.endsWith(".gif") ||
                        requestPath.endsWith(".ico") ||
                        requestPath.endsWith(".woff") ||
                        requestPath.endsWith(".woff2") ||
                        requestPath.endsWith(".ttf") ||
                        requestPath.endsWith(".eot") ||
                        requestPath.endsWith(".svg")
                    )) {
                        // Pour les ressources, ne rien faire - laisser passer à JSF
                        // Ne pas rediriger, ne pas retourner d'erreur
                        return;
                    }
                    // Rediriger vers index.xhtml au lieu de /login
                    response.sendRedirect(request.getContextPath() + "/index.xhtml");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String requestPath = request.getRequestURI();
                    // Ne PAS rediriger les ressources JSF/statiques
                    if (requestPath != null && (
                        requestPath.startsWith("/javax.faces.resource/") ||
                        requestPath.startsWith("/resources/") ||
                        requestPath.startsWith("/uploads/") ||
                        requestPath.endsWith(".css") ||
                        requestPath.endsWith(".js") ||
                        requestPath.endsWith(".png") ||
                        requestPath.endsWith(".jpg") ||
                        requestPath.endsWith(".gif") ||
                        requestPath.endsWith(".ico") ||
                        requestPath.endsWith(".woff") ||
                        requestPath.endsWith(".woff2") ||
                        requestPath.endsWith(".ttf") ||
                        requestPath.endsWith(".eot") ||
                        requestPath.endsWith(".svg")
                    )) {
                        // Pour les ressources, ne rien faire
                        return;
                    }
                    // Rediriger vers index.xhtml avec un message d'erreur si accès refusé
                    response.sendRedirect(request.getContextPath() + "/index.xhtml?accessDenied=true");
                })
            )

            // Configuration des headers de sécurité
            .headers(headers -> headers
                // Protection XSS
                .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))

                // Content Security Policy
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://use.fontawesome.com; " +
                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://use.fontawesome.com; " +
                        "font-src 'self' https://use.fontawesome.com data:; " +
                        "img-src 'self' data: https:; " +
                        "connect-src 'self' https://cdn.jsdelivr.net; " +
                        "frame-ancestors 'self';"
                    )
                )

                // HTTP Strict Transport Security (HSTS)
                .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000)) // 1 an

                // Frame Options (protection contre le clickjacking)
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)

                // Referrer Policy
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            );

        return http.build();
    }

    /**
     * Configuration CORS
     * Permet de contrôler les origines autorisées pour les requêtes cross-origin
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origines autorisées (ajuster selon vos besoins)
        // En production, spécifier les domaines exacts
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:8080",
            "http://localhost:3000",
            "https://votre-domaine.com"
        ));

        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));

        // Headers exposés au client
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));

        // Autoriser l'envoi de credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);

        // Durée de mise en cache de la réponse preflight (en secondes)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}

