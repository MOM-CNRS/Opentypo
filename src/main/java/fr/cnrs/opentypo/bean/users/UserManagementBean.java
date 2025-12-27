package fr.cnrs.opentypo.bean.users;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Named("userManagementBean")
@SessionScoped
@Getter
@Setter
public class UserManagementBean implements Serializable {

    @Inject
    private fr.cnrs.opentypo.bean.UserBean currentUserBean;

    private List<User> users = new ArrayList<>();
    private User selectedUser;
    private User newUser;
    private boolean isEditMode = false;

    @PostConstruct
    public void init() {
        chargerUsers();
    }

    public void chargerUsers() {
        // Données d'exemple - à remplacer par un service réel
        if (users.isEmpty()) {
            users.add(new User(1L, "admin", "admin@example.com", "admin", "Admin", "User", 
                User.Role.ADMIN, true, LocalDateTime.now().minusDays(30), null, "system"));
            
            users.add(new User(2L, "editor1", "editor1@example.com", "editor", "Editor", "One", 
                User.Role.EDITOR, true, LocalDateTime.now().minusDays(15), null, "admin"));
            
            users.add(new User(3L, "viewer1", "viewer1@example.com", "viewer", "Viewer", "One", 
                User.Role.VIEWER, true, LocalDateTime.now().minusDays(7), null, "admin"));
        }
    }

    public void initNouveauUser() {
        jakarta.faces.context.FacesContext facesContext = jakarta.faces.context.FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String userIdParam = facesContext.getExternalContext().getRequestParameterMap().get("userId");
            if (userIdParam != null && !userIdParam.isEmpty()) {
                try {
                    Long userId = Long.parseLong(userIdParam);
                    User userToEdit = users.stream()
                        .filter(u -> u.getId().equals(userId))
                        .findFirst()
                        .orElse(null);
                    if (userToEdit != null) {
                        initEditUser(userToEdit);
                        return;
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        newUser = new User();
        newUser.setCreatedBy(currentUserBean.getUsername());
        newUser.setActive(true);
        newUser.setRole(User.Role.VIEWER);
        isEditMode = false;
    }

    public void initEditUser(User user) {
        newUser = new User();
        newUser.setId(user.getId());
        newUser.setUsername(user.getUsername());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(user.getPassword());
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setRole(user.getRole());
        newUser.setActive(user.isActive());
        newUser.setDateCreation(user.getDateCreation());
        newUser.setCreatedBy(user.getCreatedBy());
        isEditMode = true;
    }

    public void sauvegarderUser() {
        if (newUser.getUsername() == null || newUser.getUsername().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Le nom d'utilisateur est requis."));
            PrimeFaces.current().ajax().update(":growl, :userForm");
            return;
        }

        if (newUser.getEmail() == null || newUser.getEmail().trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "L'email est requis."));
            PrimeFaces.current().ajax().update(":growl, :userForm");
            return;
        }

        if (!isEditMode) {
            // Vérifier si l'utilisateur existe déjà
            boolean exists = users.stream()
                .anyMatch(u -> u.getUsername().equals(newUser.getUsername()));
            
            if (exists) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Un utilisateur avec ce nom existe déjà."));
                PrimeFaces.current().ajax().update(":growl, :userForm");
                return;
            }

            // Nouveau user
            Long nouveauId = users.stream()
                .mapToLong(User::getId)
                .max()
                .orElse(0L) + 1;
            newUser.setId(nouveauId);
            newUser.setDateCreation(LocalDateTime.now());
            users.add(newUser);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "L'utilisateur a été créé avec succès."));
        } else {
            // Modification
            User existingUser = users.stream()
                .filter(u -> u.getId().equals(newUser.getId()))
                .findFirst()
                .orElse(null);
            
            if (existingUser != null) {
                existingUser.setEmail(newUser.getEmail());
                existingUser.setPassword(newUser.getPassword());
                existingUser.setFirstName(newUser.getFirstName());
                existingUser.setLastName(newUser.getLastName());
                existingUser.setRole(newUser.getRole());
                existingUser.setActive(newUser.isActive());
                existingUser.setDateModification(LocalDateTime.now());
            }
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "L'utilisateur a été modifié avec succès."));
        }
        
        PrimeFaces.current().ajax().update(":growl, :usersForm");
        PrimeFaces.current().executeScript("window.location.href='/users/users.xhtml';");
    }

    public void supprimerUser(User user) {
        users.remove(user);
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "L'utilisateur a été supprimé."));
        PrimeFaces.current().ajax().update(":growl, :usersForm");
    }

    public void toggleUserActive(User user) {
        user.setActive(!user.isActive());
        user.setDateModification(LocalDateTime.now());
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "Le statut de l'utilisateur a été modifié."));
        PrimeFaces.current().ajax().update(":growl, :usersForm");
    }

    public String getRoleLabel(User.Role role) {
        if (role == null) return "";
        switch (role) {
            case ADMIN: return "Administrateur";
            case EDITOR: return "Éditeur";
            case VIEWER: return "Lecteur";
            default: return role.toString();
        }
    }
}

