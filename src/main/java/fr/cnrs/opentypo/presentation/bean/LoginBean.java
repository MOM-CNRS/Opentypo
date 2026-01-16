package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.application.service.UtilisateurService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Named("loginBean")
@SessionScoped
@Getter
@Setter
public class LoginBean implements Serializable {

    @Inject
    private fr.cnrs.opentypo.presentation.bean.UserBean userBean;

    @Inject
    private UtilisateurService utilisateurService;
    
    @Inject
    private fr.cnrs.opentypo.presentation.bean.NotificationBean notificationBean;

    private String username; // Email de l'utilisateur
    private String password;
    private boolean authenticated = false;
    private Utilisateur currentUser; // Utilisateur actuellement connecté


    public void openLoginDialog() {
        PrimeFaces.current().executeScript("PF('loginDialog').show();");
    }

    public void login() {
        // Validation des champs
        if (username == null || username.trim().isEmpty()) {
            notificationBean.showErrorWithUpdate("Champ requis",
                "Veuillez saisir votre email.",
                ":loginForm:loginMessages");
            return;
        }
        
        if (password == null || password.trim().isEmpty()) {
            notificationBean.showErrorWithUpdate("Champ requis",
                "Veuillez saisir votre mot de passe.",
                ":loginForm:loginMessages");
            return;
        }
        
        // Vérifier d'abord si l'utilisateur existe et s'il est actif
        Optional<Utilisateur> utilisateurOpt = utilisateurService.findByEmail(username.trim());
        
        if (utilisateurOpt.isPresent()) {
            Utilisateur utilisateur = utilisateurOpt.get();
            
            // Vérifier si le compte est actif AVANT de vérifier le mot de passe
            if (utilisateur.getActive() == null || !utilisateur.getActive()) {
                notificationBean.showErrorWithUpdate("Compte désactivé",
                    "Votre compte a été désactivé. Veuillez contacter un administrateur pour plus d'informations.",
                    ":loginForm:loginMessages");
                password = null;
                PrimeFaces.current().ajax().update(":loginForm:loginMessages, :loginForm:password");
                return;
            }
        }
        
        // Authentification via le service (vérifie le mot de passe)
        utilisateurOpt = utilisateurService.authenticate(username.trim(), password);
        
        if (utilisateurOpt.isPresent()) {
            Utilisateur utilisateur = utilisateurOpt.get();
            currentUser = utilisateur;
            authenticated = true;
            
            // Définir le nom d'utilisateur pour l'affichage (nom complet)
            String displayName = utilisateur.getPrenom() + " " + utilisateur.getNom();
            userBean.setUsername(displayName);
            
            // Construire les rôles basés sur le groupe de l'utilisateur
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            
            // Ajouter un rôle spécifique basé sur le groupe
            if (utilisateur.getGroupe() != null) {
                String groupeNom = utilisateur.getGroupe().getNom();
                if (GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel().equalsIgnoreCase(groupeNom)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                } else if (GroupEnum.EDITEUR.getLabel().equalsIgnoreCase(groupeNom)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_EDITOR"));
                } else if (GroupEnum.LECTEUR.getLabel().equalsIgnoreCase(groupeNom)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_READER"));
                }
            }
            
            // Créer une authentification Spring Security
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                utilisateur.getEmail(),
                null,
                authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Sauvegarder l'authentification dans la session HTTP
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
                .getExternalContext().getRequest();
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
            
            notificationBean.showSuccessWithUpdate("Connexion réussie",
                "Bienvenue dans votre espace de recherche, " + displayName + ".",
                ":growl, :headerForm, :sidebarForm, :create-collection-section, :centerContent");
            
            // Réinitialiser les champs
            username = null;
            password = null;
        } else {
            notificationBean.showErrorWithUpdate("Échec de l'authentification",
                "Les identifiants saisis sont incorrects. Veuillez réessayer.",
                ":loginForm:loginMessages");
            
            // Réinitialiser le mot de passe pour sécurité
            password = null;
            PrimeFaces.current().ajax().update(":loginForm:loginMessages, :loginForm:password");
        }
    }

    public String logout() {
        String previousUser = userBean.getUsername();
        authenticated = false;
        currentUser = null;
        userBean.setUsername(null);
        
        // Nettoyer l'authentification Spring Security
        SecurityContextHolder.clearContext();
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            session.invalidate();
        }
        
        // Vérifier si on est déjà sur index.xhtml
        String viewId = facesContext.getViewRoot().getViewId();
        boolean isOnIndexPage = viewId != null && viewId.contains("/index.xhtml");
        
        // Afficher le message de déconnexion
        notificationBean.showInfo("Déconnexion",
            previousUser != null 
                ? "Au revoir, " + previousUser + ". Vous avez été déconnecté avec succès."
                : "Vous avez été déconnecté avec succès.");
        
        // Si on n'est pas déjà sur index.xhtml, rediriger
        if (!isOnIndexPage) {
            try {
                // Rediriger vers index.xhtml
                String redirectUrl = request.getContextPath() + "/index.xhtml?logout=true";
                facesContext.getExternalContext().redirect(redirectUrl);
                facesContext.responseComplete();
            } catch (IOException e) {
                // En cas d'erreur, retourner la navigation JSF
                return "index?faces-redirect=true&logout=true";
            }
        }
        
        return null; // Rester sur la page actuelle si déjà sur index.xhtml
    }

    public String getUserDisplayName() {
        if (authenticated && currentUser != null) {
            return currentUser.getPrenom() + " " + currentUser.getNom();
        }
        return authenticated && userBean.getUsername() != null 
            ? userBean.getUsername() 
            : "Non connecté";
    }

    /**
     * Retourne l'utilisateur actuellement connecté
     * 
     * @return L'utilisateur connecté ou null
     */
    public Utilisateur getCurrentUser() {
        return currentUser;
    }

    /**
     * Vérifie si l'utilisateur actuel est un administrateur
     * 
     * @return true si l'utilisateur est administrateur, false sinon
     */
    public boolean isAdminTechnique() {
        return currentUser != null 
            && currentUser.getGroupe() != null 
            && GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel().equalsIgnoreCase(currentUser.getGroupe().getNom());
    }

    /**
     * Vérifie si l'utilisateur actuel est un éditeur
     * 
     * @return true si l'utilisateur est éditeur, false sinon
     */
    public boolean isEditor() {
        return currentUser != null 
            && currentUser.getGroupe() != null 
            && GroupEnum.EDITEUR.getLabel().equalsIgnoreCase(currentUser.getGroupe().getNom());
    }

    /**
     * Vérifie si l'utilisateur est connecté et a les droits d'administration ou d'édition
     * 
     * @return true si l'utilisateur est connecté et est admin ou éditeur, false sinon
     */
    public boolean canCreateOrEdit() {
        return authenticated && currentUser != null && (isAdminTechnique() || isEditor());
    }
}

