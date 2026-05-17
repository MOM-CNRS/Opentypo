package fr.cnrs.opentypo.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration JWT pour l'authentification de l'API REST.
 */
@Component
@ConfigurationProperties(prefix = "opentypo.jwt")
@Getter
@Setter
public class OpentypoJwtProperties {

    /**
     * Clé secrète HMAC (≥ 32 caractères pour HS256).
     */
    private String secret = "";

    /**
     * Durée de validité du jeton en secondes (défaut 24 h).
     */
    private long expirationSeconds = 86_400L;
}
