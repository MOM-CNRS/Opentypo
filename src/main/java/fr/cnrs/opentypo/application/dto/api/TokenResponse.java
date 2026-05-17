package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Jeton d'accès JWT pour les appels API.")
public class TokenResponse {

    @Schema(description = "Jeton JWT", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private final String accessToken;

    @Schema(description = "Type de jeton", example = "Bearer")
    private final String tokenType;

    @Schema(description = "Durée de validité en secondes", example = "86400")
    private final long expiresIn;

    @Schema(description = "Adresse e-mail de l'utilisateur authentifié", example = "utilisateur@exemple.fr")
    private final String login;
}
