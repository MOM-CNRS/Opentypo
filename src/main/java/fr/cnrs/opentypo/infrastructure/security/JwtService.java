package fr.cnrs.opentypo.infrastructure.security;

import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.config.OpentypoJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLES = "roles";

    private final OpentypoJwtProperties jwtProperties;
    private final OpentypoAuthSupport authSupport;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "opentypo.jwt.secret doit contenir au moins 32 caractères (variable OPENTYPO_JWT_SECRET)");
        }
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Utilisateur utilisateur) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getExpirationSeconds());
        return Jwts.builder()
                .subject(utilisateur.getEmail())
                .claim(CLAIM_USER_ID, utilisateur.getId())
                .claim(CLAIM_ROLES, authSupport.roleNames(utilisateur))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationSeconds() {
        return jwtProperties.getExpirationSeconds();
    }

    public Authentication parseAuthentication(String token) {
        Claims claims = parseClaims(token);
        String email = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        List<SimpleGrantedAuthority> authorities = roles == null
                ? List.of()
                : roles.stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(email, null, authorities);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtException("Jeton expiré", e);
        } catch (JwtException e) {
            throw new JwtException("Jeton invalide", e);
        }
    }
}
