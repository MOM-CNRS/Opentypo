package fr.cnrs.opentypo.infrastructure.security;

import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Construction des autorisations Spring Security à partir d'un {@link Utilisateur}
 * (même logique que la connexion JSF).
 */
@Component
public class OpentypoAuthSupport {

    public List<GrantedAuthority> buildAuthorities(Utilisateur utilisateur) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (utilisateur.getGroupe() != null) {
            String groupeNom = utilisateur.getGroupe().getNom();
            if (GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel().equalsIgnoreCase(groupeNom)
                    || GroupEnum.ADMINISTRATEUR_FONCTIONNEL.getLabel().equalsIgnoreCase(groupeNom)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
        }
        return authorities;
    }

    public List<String> roleNames(Utilisateur utilisateur) {
        return buildAuthorities(utilisateur).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    public Authentication buildAuthentication(Utilisateur utilisateur) {
        return new UsernamePasswordAuthenticationToken(
                utilisateur.getEmail(), null, buildAuthorities(utilisateur));
    }
}
