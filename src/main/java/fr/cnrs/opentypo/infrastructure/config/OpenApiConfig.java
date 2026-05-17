package fr.cnrs.opentypo.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Métadonnées OpenAPI 3 et schéma de sécurité JWT Bearer.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(
                        "Jeton JWT obtenu via POST /api/v1/auth/login. "
                                + "Envoyer l'en-tête : Authorization: Bearer <accessToken>");

        return new OpenAPI()
                .info(new Info()
                        .title("OpenTypo — API REST")
                        .description(
                                "API pour la typologie CNRS OpenTypo (collections, référentiels, catégories, "
                                        + "groupes, séries, types archéologiques). "
                                        + "Authentification : POST /api/v1/auth/login puis Bearer JWT.")
                        .version("0.0.1")
                        .contact(new Contact()
                                .name("CNRS — OpenTypo")
                                .url("https://opentypo.mom.fr")))
                .externalDocs(new ExternalDocumentation()
                        .description("Site OpenTypo")
                        .url("https://opentypo.mom.fr"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, bearerAuth))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
