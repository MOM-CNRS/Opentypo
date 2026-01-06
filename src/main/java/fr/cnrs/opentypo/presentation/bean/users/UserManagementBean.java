package fr.cnrs.opentypo.presentation.bean.users;

import fr.cnrs.opentypo.presentation.bean.NotificationBean;
import fr.cnrs.opentypo.domain.entity.Groupe;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.GroupeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.application.service.UtilisateurService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DualListModel;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Named("userManagementBean")
@SessionScoped
@Getter
@Setter
public class UserManagementBean implements Serializable {

    @Inject
    private fr.cnrs.opentypo.presentation.bean.UserBean currentUserBean;

    @Inject
    private fr.cnrs.opentypo.presentation.bean.LoginBean loginBean;

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Inject
    private GroupeRepository groupeRepository;

    @Inject
    private UtilisateurService utilisateurService;

    @Inject
    private NotificationBean notificationBean;

    private List<User> users = new ArrayList<>();
    private User selectedUser;
    private User newUser;
    private boolean isEditMode = false;
    private List<Groupe> availableGroups = new ArrayList<>(); // Liste des groupes disponibles depuis la base
    private Long selectedGroupeId; // ID du groupe sélectionné dans le formulaire
    
    // Pour le PickList
    private DualListModel<String> pickListModel; // Modèle DualList pour le PickList

    @PostConstruct
    public void init() {
        // Vérifier que l'utilisateur est administrateur
        if (!isAdmin()) {
            redirectToUnauthorized();
            return;
        }
        chargerUsers();
        chargerGroupes();
        initialiserPickList();
    }
    
    /**
     * Initialise le PickList avec des données d'exemple
     * Vous pouvez adapter cette méthode selon vos besoins
     */
    private void initialiserPickList() {
        List<String> source = new ArrayList<>();
        source.add("Référentiel 1");
        source.add("Référentiel 2");
        source.add("Référentiel 3");
        source.add("Référentiel 4");
        
        List<String> target = new ArrayList<>();
        pickListModel = new DualListModel<>(source, target);
    }

    /**
     * Vérifie si l'utilisateur actuel est un administrateur
     * 
     * @return true si l'utilisateur est administrateur, false sinon
     */
    private boolean isAdmin() {
        return loginBean != null && loginBean.isAdmin();
    }

    /**
     * Redirige vers la page d'accueil avec un message d'erreur si l'utilisateur n'est pas autorisé
     */
    private void redirectToUnauthorized() {
        try {
            jakarta.faces.context.FacesContext facesContext = jakarta.faces.context.FacesContext.getCurrentInstance();
            if (facesContext != null) {
                notificationBean.showError("Accès refusé", 
                    "Seuls les administrateurs peuvent accéder à la gestion des utilisateurs.");
                String redirectUrl = facesContext.getExternalContext().getRequestContextPath() + "/index.xhtml?unauthorized=true";
                facesContext.getExternalContext().redirect(redirectUrl);
                facesContext.responseComplete();
            }
        } catch (Exception e) {
            // Ignorer les erreurs de redirection
        }
    }

    /**
     * Charge la liste des groupes disponibles depuis la base de données
     */
    public void chargerGroupes() {
        try {
            availableGroups = groupeRepository.findAll();
        } catch (Exception e) {
            availableGroups = new ArrayList<>();
            notificationBean.showError("Erreur", "Erreur lors du chargement des groupes : " + e.getMessage());
        }
    }

    /**
     * Retourne la liste des groupes disponibles
     * 
     * @return Liste des groupes
     */
    public List<Groupe> getAvailableGroups() {
        if (availableGroups.isEmpty()) {
            chargerGroupes();
        }
        return availableGroups;
    }

    public void chargerUsers() {
        if (!isAdmin()) {
            redirectToUnauthorized();
            return;
        }
        try {
            List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
            users = utilisateurs.stream()
                .map(this::convertToUser)
                .collect(Collectors.toList());
        } catch (Exception e) {
            users = new ArrayList<>();
            notificationBean.showError("Erreur", "Erreur lors du chargement des utilisateurs : " + e.getMessage());
        }
    }

    public void initNouveauUser() {
        if (!isAdmin()) {
            redirectToUnauthorized();
            return;
        }
        jakarta.faces.context.FacesContext facesContext = jakarta.faces.context.FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String userIdParam = facesContext.getExternalContext().getRequestParameterMap().get("userId");
            if (userIdParam != null && !userIdParam.isEmpty()) {
                try {
                    Long userId = Long.parseLong(userIdParam);
                    Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(userId);
                    if (utilisateurOpt.isPresent()) {
                        initEditUser(convertToUser(utilisateurOpt.get()));
                        return;
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        newUser = new User();
        newUser.setCreatedBy(currentUserBean.getUsername() != null ? currentUserBean.getUsername() : "SYSTEM");
        newUser.setActive(true);
        newUser.setRole(User.Role.VIEWER);
        selectedGroupeId = null; // Réinitialiser la sélection du groupe
        isEditMode = false;
        // S'assurer que les groupes sont chargés
        if (availableGroups.isEmpty()) {
            chargerGroupes();
        }
        // Réinitialiser le PickList
        initialiserPickList();
    }

    public void initEditUser(User user) {
        newUser = new User();
        newUser.setId(user.getId());
        newUser.setUsername(user.getUsername());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(""); // Ne pas afficher le mot de passe
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setRole(user.getRole());
        newUser.setActive(user.isActive());
        newUser.setDateCreation(user.getDateCreation());
        newUser.setCreatedBy(user.getCreatedBy());
        isEditMode = true;
        // S'assurer que les groupes sont chargés
        if (availableGroups.isEmpty()) {
            chargerGroupes();
        }
        // Charger le groupe actuel de l'utilisateur pour la modification
        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(user.getId());
            if (utilisateurOpt.isPresent() && utilisateurOpt.get().getGroupe() != null) {
                selectedGroupeId = utilisateurOpt.get().getGroupe().getId();
            } else {
                selectedGroupeId = null;
            }
        } catch (Exception e) {
            selectedGroupeId = null;
        }
        // Réinitialiser le PickList (vous pouvez charger les valeurs depuis la base de données ici)
        initialiserPickList();
    }

    public void sauvegarderUser() {
        // Vérifier que l'utilisateur est administrateur
        if (!isAdmin()) {
            notificationBean.showErrorWithUpdate("Accès refusé", 
                "Seuls les administrateurs peuvent gérer les utilisateurs.", 
                ":growl, :userForm");
            return;
        }

        // Validation
        if (newUser.getEmail() == null || newUser.getEmail().trim().isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur", "L'email est requis.", ":growl, :userForm");
            return;
        }

        if (newUser.getFirstName() == null || newUser.getFirstName().trim().isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur", "Le prénom est requis.", ":growl, :userForm");
            return;
        }

        if (newUser.getLastName() == null || newUser.getLastName().trim().isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur", "Le nom est requis.", ":growl, :userForm");
            return;
        }

        if (selectedGroupeId == null) {
            notificationBean.showErrorWithUpdate("Erreur", "Le groupe est requis.", ":growl, :userForm");
            return;
        }

        try {
            // Récupérer le groupe sélectionné depuis la base de données
            Optional<Groupe> groupeOpt = groupeRepository.findById(selectedGroupeId);
            
            if (groupeOpt.isEmpty()) {
                notificationBean.showErrorWithUpdate("Erreur", 
                    "Le groupe sélectionné n'existe pas dans la base de données.", 
                    ":growl, :userForm");
                return;
            }

            Groupe groupe = groupeOpt.get();

            if (!isEditMode) {
                // Création d'un nouvel utilisateur
                if (utilisateurRepository.existsByEmail(newUser.getEmail().trim())) {
                    notificationBean.showErrorWithUpdate("Erreur", 
                        "Un utilisateur avec cet email existe déjà.", 
                        ":growl, :userForm");
                    return;
                }

                if (newUser.getPassword() == null || newUser.getPassword().trim().isEmpty()) {
                    notificationBean.showErrorWithUpdate("Erreur", 
                        "Le mot de passe est requis pour un nouvel utilisateur.", 
                        ":growl, :userForm");
                    return;
                }

                // Créer l'entité Utilisateur
                Utilisateur utilisateur = new Utilisateur();
                utilisateur.setNom(newUser.getLastName().trim());
                utilisateur.setPrenom(newUser.getFirstName().trim());
                utilisateur.setEmail(newUser.getEmail().trim());
                utilisateur.setPasswordHash(utilisateurService.encodePassword(newUser.getPassword()));
                utilisateur.setGroupe(groupe);
                utilisateur.setActive(newUser.isActive());
                utilisateur.setCreateBy(currentUserBean.getUsername() != null ? currentUserBean.getUsername() : "SYSTEM");
                utilisateur.setCreateDate(LocalDateTime.now());

                utilisateurRepository.save(utilisateur);
                notificationBean.showSuccessWithUpdate("Succès", 
                    "L'utilisateur a été créé avec succès.", 
                    ":growl, :userForm");
            } else {
                // Modification d'un utilisateur existant
                Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(newUser.getId());
                
                if (utilisateurOpt.isEmpty()) {
                    notificationBean.showErrorWithUpdate("Erreur", 
                        "L'utilisateur à modifier n'existe pas.", 
                        ":growl, :userForm");
                    return;
                }

                Utilisateur utilisateur = utilisateurOpt.get();

                // Vérifier si l'email est unique (sauf pour l'utilisateur actuel)
                if (!utilisateur.getEmail().equals(newUser.getEmail().trim())) {
                    if (utilisateurRepository.existsByEmail(newUser.getEmail().trim())) {
                        notificationBean.showErrorWithUpdate("Erreur", 
                            "Un utilisateur avec cet email existe déjà.", 
                            ":growl, :userForm");
                        return;
                    }
                }

                // Mettre à jour les champs
                utilisateur.setNom(newUser.getLastName().trim());
                utilisateur.setPrenom(newUser.getFirstName().trim());
                utilisateur.setEmail(newUser.getEmail().trim());
                utilisateur.setGroupe(groupe);
                utilisateur.setActive(newUser.isActive());

                // Mettre à jour le mot de passe seulement si un nouveau mot de passe est fourni
                if (newUser.getPassword() != null && !newUser.getPassword().trim().isEmpty()) {
                    utilisateur.setPasswordHash(utilisateurService.encodePassword(newUser.getPassword()));
                }

                utilisateurRepository.save(utilisateur);
                notificationBean.showSuccessWithUpdate("Succès", 
                    "L'utilisateur a été modifié avec succès.", 
                    ":growl, :userForm");
            }

            // Recharger la liste des utilisateurs
            chargerUsers();

            // Rediriger vers la liste après un court délai pour permettre l'affichage du message
            PrimeFaces.current().executeScript("setTimeout(function() { window.location.href='/users/users.xhtml'; }, 1500);");

        } catch (Exception e) {
            notificationBean.showErrorWithUpdate("Erreur", 
                "Une erreur s'est produite lors de la sauvegarde : " + e.getMessage(), 
                ":growl, :userForm");
        }
    }

    public void supprimerUser(User user) {
        // Vérifier que l'utilisateur est administrateur
        if (!isAdmin()) {
            notificationBean.showErrorWithUpdate("Accès refusé", 
                "Seuls les administrateurs peuvent supprimer des utilisateurs.", 
                ":growl, :usersForm");
            return;
        }

        if (user == null || user.getId() == null) {
            notificationBean.showErrorWithUpdate("Erreur", "Aucun utilisateur sélectionné pour la suppression.", ":growl, :usersForm");
            return;
        }

        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(user.getId());
            if (utilisateurOpt.isPresent()) {
                Utilisateur utilisateur = utilisateurOpt.get();
                
                // Vérifier si l'utilisateur n'est pas l'utilisateur actuellement connecté
                if (currentUserBean.getUsername() != null && 
                    utilisateur.getEmail().equals(currentUserBean.getUsername())) {
                    notificationBean.showErrorWithUpdate("Erreur", 
                        "Vous ne pouvez pas supprimer votre propre compte.", 
                        ":growl, :usersForm");
                    return;
                }

                utilisateurRepository.delete(utilisateur);
                notificationBean.showSuccessWithUpdate("Succès", 
                    "L'utilisateur " + utilisateur.getPrenom() + " " + utilisateur.getNom() + " a été supprimé avec succès.", 
                    ":growl, :usersForm");
                
                // Recharger la liste des utilisateurs
                chargerUsers();
            } else {
                notificationBean.showErrorWithUpdate("Erreur", 
                    "L'utilisateur à supprimer n'existe pas.", 
                    ":growl, :usersForm");
            }
        } catch (Exception e) {
            notificationBean.showErrorWithUpdate("Erreur", 
                "Erreur lors de la suppression : " + e.getMessage(), 
                ":growl, :usersForm");
        }
        PrimeFaces.current().ajax().update(":growl, :usersForm");
    }

    public void toggleUserActive(User user) {
        // Vérifier que l'utilisateur est administrateur
        if (!isAdmin()) {
            notificationBean.showErrorWithUpdate("Accès refusé", 
                "Seuls les administrateurs peuvent modifier le statut des utilisateurs.", 
                ":growl, :usersForm");
            PrimeFaces.current().ajax().update(":growl, :usersForm");
            return;
        }

        if (user == null || user.getId() == null) {
            notificationBean.showErrorWithUpdate("Erreur", 
                "Aucun utilisateur sélectionné.", 
                ":growl, :usersForm");
            PrimeFaces.current().ajax().update(":growl, :usersForm");
            return;
        }

        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(user.getId());
            if (utilisateurOpt.isEmpty()) {
                notificationBean.showErrorWithUpdate("Erreur", 
                    "L'utilisateur n'existe pas.", 
                    ":growl, :usersForm");
                PrimeFaces.current().ajax().update(":growl, :usersForm");
                return;
            }

            Utilisateur utilisateur = utilisateurOpt.get();
            boolean newActiveState = !(utilisateur.getActive() != null && utilisateur.getActive());
            utilisateur.setActive(newActiveState);
            utilisateurRepository.save(utilisateur);

            notificationBean.showSuccessWithUpdate("Succès", 
                "Le statut de l'utilisateur a été " + (newActiveState ? "activé" : "désactivé") + " avec succès.", 
                ":growl, :usersForm");
            
            chargerUsers(); // Recharger la liste
            PrimeFaces.current().ajax().update(":growl, :usersForm");
        } catch (Exception e) {
            notificationBean.showErrorWithUpdate("Erreur", 
                "Erreur lors de la modification du statut : " + e.getMessage(), 
                ":growl, :usersForm");
            PrimeFaces.current().ajax().update(":growl, :usersForm");
        }
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

    /**
     * Convertit une entité Utilisateur en DTO User
     */
    private User convertToUser(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        User user = new User();
        user.setId(utilisateur.getId());
        user.setEmail(utilisateur.getEmail() != null ? utilisateur.getEmail() : "");
        user.setUsername(utilisateur.getEmail() != null ? utilisateur.getEmail() : ""); // Utiliser l'email comme username
        user.setFirstName(utilisateur.getPrenom() != null ? utilisateur.getPrenom() : "");
        user.setLastName(utilisateur.getNom() != null ? utilisateur.getNom() : "");
        user.setPassword(""); // Ne pas exposer le mot de passe
        user.setRole(getRoleFromGroupe(utilisateur.getGroupe()));
        user.setActive(utilisateur.getActive() != null ? utilisateur.getActive() : true); // Utiliser le champ actif de l'entité
        user.setDateCreation(utilisateur.getCreateDate());
        user.setCreatedBy(utilisateur.getCreateBy() != null ? utilisateur.getCreateBy() : "SYSTEM");
        return user;
    }

    /**
     * Convertit un rôle User.Role en nom de groupe
     */
    private String getGroupeNomFromRole(User.Role role) {
        if (role == null) return "Lecteur";
        switch (role) {
            case ADMIN: return "Administrateur";
            case EDITOR: return "Éditeur";
            case VIEWER: return "Lecteur";
            default: return "Lecteur";
        }
    }

    /**
     * Convertit un groupe en rôle User.Role
     */
    private User.Role getRoleFromGroupe(Groupe groupe) {
        if (groupe == null || groupe.getNom() == null) return User.Role.VIEWER;
        String nom = groupe.getNom();
        if ("Administrateur".equalsIgnoreCase(nom)) {
            return User.Role.ADMIN;
        } else if ("Éditeur".equalsIgnoreCase(nom)) {
            return User.Role.EDITOR;
        } else {
            return User.Role.VIEWER;
        }
    }

    // Getter pour editMode (pour compatibilité avec la vue)
    public boolean isEditMode() {
        return isEditMode;
    }
}
