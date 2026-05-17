package fr.cnrs.opentypo.presentation.rest;

import fr.cnrs.opentypo.application.dto.api.ApiErrorResponse;
import fr.cnrs.opentypo.application.dto.api.LoginRequest;
import fr.cnrs.opentypo.application.dto.api.TokenResponse;
import fr.cnrs.opentypo.application.service.AuthApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentification", description = "Connexion par identifiant et mot de passe ; obtention d'un jeton JWT.")
@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthApiService authApiService;

    @Operation(
            operationId = "login",
            summary = "Connexion et obtention d'un jeton JWT",
            description = """
                    Authentifie un utilisateur avec son adresse e-mail (champ `login`) et son mot de passe.
                    Le jeton retourné doit être envoyé dans l'en-tête `Authorization: Bearer <token>` pour accéder aux autres endpoints API.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentification réussie.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Corps de requête invalide.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Identifiants incorrects.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Compte désactivé.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authApiService.login(request);
    }
}
