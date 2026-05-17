package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.api.ApiErrorMessages;
import fr.cnrs.opentypo.application.dto.api.LoginRequest;
import fr.cnrs.opentypo.application.dto.api.TokenResponse;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthApiService {

    private final UtilisateurService utilisateurService;
    private final JwtService jwtService;

    public TokenResponse login(LoginRequest request) {
        String login = request.getLogin().trim();

        Optional<Utilisateur> existing = utilisateurService.findByEmail(login);
        if (existing.isPresent()) {
            Utilisateur user = existing.get();
            if (user.getActive() == null || !user.getActive()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrorMessages.ACCOUNT_DISABLED);
            }
        }

        Optional<Utilisateur> authenticated = utilisateurService.authenticate(login, request.getPassword());
        if (authenticated.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ApiErrorMessages.INVALID_CREDENTIALS);
        }

        Utilisateur utilisateur = authenticated.get();
        String token = jwtService.generateToken(utilisateur);
        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .login(utilisateur.getEmail())
                .build();
    }
}
