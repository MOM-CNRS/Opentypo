package fr.cnrs.opentypo.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Métadonnées OpenAPI 3 et schéma de sécurité (session HTTP, comme l'interface JSF).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OpenTypo — API REST")
                        .description("API pour la typologie CNRS OpenTypo (typologies, référentiels, categories, groupes, séries, types).")
                        .version("0.0.1")
                        .contact(new Contact()
                                .name("CNRS — OpenTypo")
                                .url("https://opentypo.mom.fr")))
                .externalDocs(new ExternalDocumentation()
                        .description("Site OpenTypo")
                        .url("https://opentypo.mom.fr"));
    }
}
