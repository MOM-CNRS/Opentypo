package fr.cnrs.opentypo.presentation.bean.users;

import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.presentation.bean.NotificationBean;
import fr.cnrs.opentypo.domain.entity.Groupe;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.GroupeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.application.service.UtilisateurService;
import fr.cnrs.opentypo.presentation.bean.UserBean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Slf4j
@Getter
@Setter
@SessionScoped
@Named("userManagementBean")
public class UserManagementBean implements Serializable {

    @Inject
    private UserBean currentUserBean;

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Inject
    private GroupeRepository groupeRepository;

    @Inject
    private UtilisateurService utilisateurService;

    @Inject
    private NotificationBean notificationBean;

    private List<Utilisateur> users = new ArrayList<>();
    private Utilisateur selectedUser;
    private Utilisateur newUser;
    private boolean isEditMode = false;
    private List<Groupe> availableGroups = new ArrayList<>(); // Liste des groupes disponibles depuis la base
    private Long selectedGroupeId; // ID du groupe sélectionné dans le formulaire
    private Groupe selectedGroupe;


    @PostConstruct
    public void init() {
        users = utilisateurRepository.findAll();
        availableGroups = groupeRepository.findAll();
    }

    public void initNouveauUser() throws IOException {

        newUser = new Utilisateur();
        newUser.setCreateBy(currentUserBean.getUsername() != null ? currentUserBean.getUsername() : "SYSTEM");
        newUser.setActive(true);
        newUser.setGroupe(null);
        selectedGroupeId = null; // Réinitialiser la sélection du groupe
        selectedGroupe = null;
        isEditMode = false;
        // S'assurer que les groupes sont chargés
        if (availableGroups.isEmpty()) {
            users = utilisateurRepository.findAll();
        }

        FacesContext.getCurrentInstance().getExternalContext()
                .redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/users/create.xhtml");
    }

    public void initEditUser(Utilisateur utilisateur) throws IOException {
        newUser = utilisateur;
        isEditMode = true;
        // S'assurer que les groupes sont chargés
        if (availableGroups.isEmpty()) {
            availableGroups = groupeRepository.findAll();
        }
        // Charger le groupe actuel de l'utilisateur pour la modification
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(utilisateur.getId());
        if (utilisateurOpt.isPresent() && utilisateurOpt.get().getGroupe() != null) {
            selectedGroupeId = utilisateurOpt.get().getGroupe().getId();
            selectedGroupe = utilisateurOpt.get().getGroupe();
        } else {
            selectedGroupeId = null;
            selectedGroupe = null;
        }

        FacesContext.getCurrentInstance().getExternalContext()
                .redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/users/create.xhtml");
    }

    public void onGroupeChange() {
        selectedGroupe = groupeRepository.findById(selectedGroupeId).orElse(null);
    }

    public void sauvegarderUser() {

        // Validation complète des champs
        String email = newUser.getEmail() != null ? newUser.getEmail().trim() : "";
        String firstName = newUser.getPrenom() != null ? newUser.getPrenom().trim() : "";
        String lastName = newUser.getNom() != null ? newUser.getNom().trim() : "";
        String password = newUser.getPasswordHash() != null ? newUser.getPasswordHash().trim() : "";

        // Validation email
        if (email.isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "L'email est obligatoire.", ":growl, :userForm");
            return;
        }
        
        // Validation format email
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!email.matches(emailPattern)) {
            notificationBean.showErrorWithUpdate("Erreur de validation", 
                "Le format de l'email est invalide. Veuillez saisir une adresse email valide (exemple: utilisateur@domaine.com).", 
                ":growl, :userForm");
            return;
        }
        
        if (email.length() > 255) {
            notificationBean.showErrorWithUpdate("Erreur de validation", 
                "L'email ne peut pas dépasser 255 caractères.", 
                ":growl, :userForm");
            return;
        }

        // Validation prénom
        if (firstName.isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le prénom est obligatoire.", ":growl, :userForm");
            return;
        }
        
        if (firstName.length() < 2) {
            notificationBean.showErrorWithUpdate("Erreur de validation", 
                "Le prénom doit contenir au moins 2 caractères.", 
                ":growl, :userForm");
            return;
        }
        
        if (firstName.length() > 100) {
            notificationBean.showErrorWithUpdate("Erreur de validation", 
                "Le prénom ne peut pas dépasser 100 caractères.", 
                ":growl, :userForm");
            return;
        }

        // Validation nom
        if (lastName.isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le nom est obligatoire.", ":growl, :userForm");
            return;
        }
        
        if (lastName.length() < 2) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le nom doit contenir au moins 2 caractères.",
                ":growl, :userForm");
            return;
        }
        
        if (lastName.length() > 100) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le nom ne peut pas dépasser 100 caractères.",
                    ":growl, :userForm");
            return;
        }

        // Validation mot de passe
        if (!isEditMode) {
            if (password.isEmpty()) {
                notificationBean.showErrorWithUpdate("Erreur de validation", "Le mot de passe est obligatoire pour un nouvel utilisateur.",
                    ":growl, :userForm");
                return;
            }
            
            if (password.length() < 6) {
                notificationBean.showErrorWithUpdate("Erreur de validation", "Le mot de passe doit contenir au moins 6 caractères.",
                    ":growl, :userForm");
                return;
            }
        } else {
            // En mode édition, si un mot de passe est fourni, il doit respecter les règles
            if (!password.isEmpty() && password.length() < 6) {
                notificationBean.showErrorWithUpdate("Erreur de validation", 
                    "Le mot de passe doit contenir au moins 6 caractères.", 
                    ":growl, :userForm");
                return;
            }
        }

        // Validation groupe
        if (selectedGroupe == null) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le groupe est obligatoire.", ":growl, :userForm");
            return;
        }

        if (!isEditMode) {
            // Création d'un nouvel utilisateur
            if (utilisateurRepository.existsByEmail(newUser.getEmail().trim())) {
                notificationBean.showErrorWithUpdate("Erreur",
                        "Un utilisateur avec cet email existe déjà.",
                        ":growl, :userForm");
                return;
            }

            if (newUser.getPasswordHash() == null || newUser.getPasswordHash().trim().isEmpty()) {
                notificationBean.showErrorWithUpdate("Erreur",
                        "Le mot de passe est requis pour un nouvel utilisateur.",
                        ":growl, :userForm");
                return;
            }

            // Créer l'entité Utilisateur
            Utilisateur utilisateur = new Utilisateur();
            utilisateur.setNom(newUser.getNom().trim());
            utilisateur.setPrenom(newUser.getPrenom().trim());
            utilisateur.setEmail(newUser.getEmail().trim());
            utilisateur.setPasswordHash(utilisateurService.encodePassword(newUser.getPasswordHash()));
            utilisateur.setGroupe(selectedGroupe);
            utilisateur.setActive(newUser.getActive());
            utilisateur.setCreateBy(currentUserBean.getUsername() != null ? currentUserBean.getUsername() : "SYSTEM");
            utilisateur.setCreateDate(LocalDateTime.now());
            utilisateur = utilisateurRepository.save(utilisateur);

            notificationBean.showSuccessWithUpdate("Succès", "L'utilisateur a été créé avec succès.",
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
            utilisateur.setNom(newUser.getNom().trim());
            utilisateur.setPrenom(newUser.getPrenom().trim());
            utilisateur.setEmail(newUser.getEmail().trim());
            utilisateur.setGroupe(selectedGroupe);
            utilisateur.setActive(newUser.getActive());

            // Mettre à jour le mot de passe seulement si un nouveau mot de passe est fourni
            if (newUser.getPasswordHash() != null && !newUser.getPasswordHash().trim().isEmpty()) {
                utilisateur.setPasswordHash(utilisateurService.encodePassword(newUser.getPasswordHash()));
            }

            utilisateur = utilisateurRepository.save(utilisateur);

            notificationBean.showSuccessWithUpdate("Succès", "L'utilisateur a été modifié avec succès.",
                    ":growl, :userForm");
        }

        // Recharger la liste des utilisateurs
        users = utilisateurRepository.findAll();

        // Rediriger vers la liste après un court délai pour permettre l'affichage du message
        PrimeFaces.current().executeScript("setTimeout(function() { window.location.href='/users/users.xhtml'; }, 500);");
    }

    public void supprimerUser(Utilisateur utilisateur) {

        if (utilisateur == null || utilisateur.getId() == null) {
            notificationBean.showErrorWithUpdate("Erreur", "Aucun utilisateur sélectionné pour la suppression.", ":growl, :usersForm");
            return;
        }

        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(utilisateur.getId());
            if (utilisateurOpt.isPresent()) {
                Utilisateur user = utilisateurOpt.get();
                
                // Vérifier si l'utilisateur n'est pas l'utilisateur actuellement connecté
                if (currentUserBean.getUsername() != null && user.getEmail().equals(currentUserBean.getUsername())) {
                    notificationBean.showErrorWithUpdate("Erreur", "Vous ne pouvez pas supprimer votre propre compte.",
                        ":growl, :usersForm");
                    return;
                }

                utilisateurRepository.delete(user);
                notificationBean.showSuccessWithUpdate("Succès",
                        "L'utilisateur " + user.getPrenom() + " " + user.getNom() + " a été supprimé avec succès.",
                    ":growl, :usersForm");
                
                // Recharger la liste des utilisateurs
                users = utilisateurRepository.findAll();
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

    public String getGroupeEtiquette(Groupe groupe) {
        if ( groupe == null || groupe.getNom() == null ) {
            return "role-badge-viewer";
        }
        if (GroupEnum.UTILISATEUR.getLabel().equalsIgnoreCase(groupe.getNom())) {
            return "role-badge-viewer";
        }
        return "role-badge-admin";
    }
}
