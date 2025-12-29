package fr.cnrs.opentypo.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;
import java.util.Collections;

@Named("loginBean")
@SessionScoped
@Getter
@Setter
public class LoginBean implements Serializable {

    @Inject
    private UserBean userBean;

    private String username;
    private String password;
    private boolean authenticated = false;

    public void openLoginDialog() {
        PrimeFaces.current().executeScript("PF('loginDialog').show();");
    }

    public void login() {
        // Validation des champs
        if (username == null || username.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("loginForm:username",
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Champ requis",
                    "Veuillez saisir votre nom d'utilisateur."));
            PrimeFaces.current().ajax().update(":loginForm:loginMessages");
            return;
        }
        
        if (password == null || password.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage("loginForm:password",
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Champ requis",
                    "Veuillez saisir votre mot de passe."));
            PrimeFaces.current().ajax().update(":loginForm:loginMessages");
            return;
        }
        
        // Authentification
        if ("admin".equals(username.trim()) && "admin".equals(password)) {
            authenticated = true;
            userBean.setUsername(username.trim());
            
            // Créer une authentification Spring Security
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                username.trim(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Sauvegarder l'authentification dans la session HTTP
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
                .getExternalContext().getRequest();
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Connexion réussie",
                    "Bienvenue dans votre espace de recherche, " + username.trim() + "."));
            
            PrimeFaces.current().ajax().update(":growl, :headerForm, :sidebarForm");
            
            // Réinitialiser les champs
            username = null;
            password = null;
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Échec de l'authentification",
                    "Les identifiants saisis sont incorrects. Veuillez réessayer."));
            
            // Réinitialiser le mot de passe pour sécurité
            password = null;
            PrimeFaces.current().ajax().update(":loginForm:loginMessages, :loginForm:password");
        }
    }

    public void logout() {
        String previousUser = userBean.getUsername();
        authenticated = false;
        userBean.setUsername(null);
        
        // Nettoyer l'authentification Spring Security
        SecurityContextHolder.clearContext();
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
            .getExternalContext().getRequest();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        }
        
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Déconnexion",
                previousUser != null 
                    ? "Au revoir, " + previousUser + ". Vous avez été déconnecté avec succès."
                    : "Vous avez été déconnecté avec succès."));
        
        PrimeFaces.current().ajax().update(":growl, :headerForm, :sidebarForm");
    }

    public String getUserDisplayName() {
        return authenticated && userBean.getUsername() != null 
            ? userBean.getUsername() 
            : "Non connecté";
    }
}

