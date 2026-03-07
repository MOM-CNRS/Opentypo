package fr.cnrs.opentypo.infrastructure.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Tomcat pour les requêtes multipart.
 * Augmente maxPartCount afin d'éviter FileCountLimitExceededException
 * sur les formulaires complexes (fileUpload + pickList + chips + nombreux champs).
 */
@Configuration
public class TomcatMultipartConfig {

    /** Nombre max de parts multipart (fichiers + champs). Par défaut Tomcat = 50. */
    private static final int MAX_PART_COUNT = 200;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatMultipartCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            if (connector instanceof Connector c) {
                c.setMaxPartCount(MAX_PART_COUNT);
            }
        });
    }
}
