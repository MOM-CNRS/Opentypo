package fr.cnrs.opentypo.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Identifiants de connexion à l'API (adresse e-mail et mot de passe).")
public class LoginRequest {

    @NotBlank(message = "L'identifiant est obligatoire")
    @Schema(description = "Adresse e-mail de l'utilisateur", example = "utilisateur@exemple.fr")
    private String login;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Schema(description = "Mot de passe", example = "********")
    private String password;
}
