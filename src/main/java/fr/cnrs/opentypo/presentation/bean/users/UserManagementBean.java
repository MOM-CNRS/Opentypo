package fr.cnrs.opentypo.presentation.bean.users;

import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.NotificationBean;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
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

    @Inject
    private LoginBean loginBean;

    private List<Utilisateur> users = new ArrayList<>();
    private Utilisateur selectedUser;
    private Utilisateur newUser;
    private boolean isEditMode = false;
    private List<Groupe> availableGroups = new ArrayList<>(); // Liste des groupes disponibles depuis la base
    private Long selectedGroupeId; // ID du groupe sélectionné dans le formulaire
    private Groupe selectedGroupe;
    /** Confirmation du mot de passe saisi (non persisté). */
    private String passwordConfirmation;


    @PostConstruct
    public void init() {
        reloadUsers();
        reloadAvailableGroups();
    }

    public void onUsersPageEnter() throws IOException {
        if (!canAccessUserManagement()) {
            denyAccess(JsfMessages.get("users.msg.accessDenied.page"));
            return;
        }
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null && !ctx.isPostback()) {
            reloadUsers();
        }
    }

    public void onCreatePageEnter() throws IOException {
        if (isEditMode && !canEditOrDeleteUsers()) {
            denyAccess(JsfMessages.get("users.msg.accessDenied.edit"));
            return;
        }
        if (!canCreateUsers()) {
            denyAccess(JsfMessages.get("users.msg.accessDenied.create"));
            return;
        }
        if (!isEditMode && newUser == null) {
            prepareNewUserForm();
        }
    }

    public void initNouveauUser() throws IOException {
        if (!canCreateUsers()) {
            notificationBean.showError(JsfMessages.get("common.growl.accessDenied"),
                    JsfMessages.get("users.msg.accessDenied.create"));
            return;
        }

        prepareNewUserForm();
        reloadAvailableGroups();

        FacesContext.getCurrentInstance().getExternalContext()
                .redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/users/create.xhtml");
    }

    public void initEditUser(Utilisateur utilisateur) throws IOException {
        if (!canEditOrDeleteUsers()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.accessDenied"),
                    JsfMessages.get("users.msg.accessDenied.edit"),
                    ":growl, :usersForm");
            return;
        }

        if (utilisateur == null || utilisateur.getId() == null) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"),
                    JsfMessages.get("users.error.userMissing"),
                    ":growl, :usersForm");
            return;
        }

        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByIdWithGroupe(utilisateur.getId());
        if (utilisateurOpt.isEmpty()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"),
                    JsfMessages.get("users.error.userMissing"),
                    ":growl, :usersForm");
            return;
        }

        prepareEditUserForm(utilisateurOpt.get());
        reloadAvailableGroups();

        FacesContext.getCurrentInstance().getExternalContext()
                .redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/users/create.xhtml");
    }

    public void onGroupeChange() {
        selectedGroupe = groupeRepository.findById(selectedGroupeId).orElse(null);
    }

    public void sauvegarderUser() {
        if (isEditMode) {
            if (!canEditOrDeleteUsers()) {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.accessDenied"),
                        JsfMessages.get("users.msg.accessDenied.edit"),
                        ":growl, :userForm");
                return;
            }
        } else if (!canCreateUsers()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.accessDenied"),
                    JsfMessages.get("users.msg.accessDenied.create"),
                    ":growl, :userForm");
            return;
        }

        // Validation complète des champs
        String email = newUser.getEmail() != null ? newUser.getEmail().trim() : "";
        String firstName = newUser.getPrenom() != null ? newUser.getPrenom().trim() : "";
        String lastName = newUser.getNom() != null ? newUser.getNom().trim() : "";
        String password = newUser.getPasswordHash() != null ? newUser.getPasswordHash().trim() : "";
        String passwordConfirm = passwordConfirmation != null ? passwordConfirmation.trim() : "";

        // Validation email
        if (email.isEmpty()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"), JsfMessages.get("users.validation.email"), ":growl, :userForm");
            return;
        }
        
        // Validation format email
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!email.matches(emailPattern)) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                JsfMessages.get("users.validation.emailFormat"),
                ":growl, :userForm");
            return;
        }
        
        if (email.length() > EntityConstants.VARCHAR_COLUMN_MAX_LENGTH) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                JsfMessages.get("users.validation.emailMax"),
                ":growl, :userForm");
            return;
        }

        // Validation prénom
        if (firstName.isEmpty()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"), JsfMessages.get("users.validation.firstName"), ":growl, :userForm");
            return;
        }
        
        if (firstName.length() < 2) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                JsfMessages.get("users.validation.firstNameMin"),
                ":growl, :userForm");
            return;
        }
        
        if (firstName.length() > EntityConstants.VARCHAR_COLUMN_MAX_LENGTH) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                JsfMessages.get("users.validation.firstNameMax"),
                ":growl, :userForm");
            return;
        }

        // Validation nom
        if (lastName.isEmpty()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"), JsfMessages.get("users.validation.lastName"), ":growl, :userForm");
            return;
        }
        
        if (lastName.length() < 2) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"), JsfMessages.get("users.validation.lastNameMin"),
                ":growl, :userForm");
            return;
        }
        
        if (lastName.length() > EntityConstants.VARCHAR_COLUMN_MAX_LENGTH) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"), JsfMessages.get("users.validation.lastNameMax"),
                    ":growl, :userForm");
            return;
        }

        // Validation mot de passe
        if (!isEditMode) {
            if (password.isEmpty()) {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"), JsfMessages.get("users.validation.passwordNew"),
                    ":growl, :userForm");
                return;
            }

            if (password.length() < 6) {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"), JsfMessages.get("users.validation.passwordMin"),
                    ":growl, :userForm");
                return;
            }

            if (passwordConfirm.isEmpty()) {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                        JsfMessages.get("users.validation.passwordConfirmRequired"),
                        ":growl, :userForm");
                return;
            }

            if (!password.equals(passwordConfirm)) {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                        JsfMessages.get("users.validation.passwordMismatch"),
                        ":growl, :userForm");
                return;
            }
        } else {
            // En mode édition, si un mot de passe est fourni, il doit respecter les règles
            if (!password.isEmpty()) {
                if (password.length() < 6) {
                    notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                            JsfMessages.get("users.validation.passwordMin"),
                            ":growl, :userForm");
                    return;
                }
                if (passwordConfirm.isEmpty()) {
                    notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                            JsfMessages.get("users.validation.passwordConfirmRequired"),
                            ":growl, :userForm");
                    return;
                }
                if (!password.equals(passwordConfirm)) {
                    notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                            JsfMessages.get("users.validation.passwordMismatch"),
                            ":growl, :userForm");
                    return;
                }
            }
        }

        // Validation groupe
        if (selectedGroupe == null) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"), JsfMessages.get("users.validation.group"), ":growl, :userForm");
            return;
        }

        if (!isSelectedGroupAllowedForCurrentUser()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.accessDenied"),
                    JsfMessages.get("users.msg.accessDenied.group"),
                    ":growl, :userForm");
            return;
        }

        if (!isEditMode) {
            // Création d'un nouvel utilisateur
            if (utilisateurRepository.existsByEmail(newUser.getEmail().trim())) {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"),
                        JsfMessages.get("users.error.emailExists"),
                        ":growl, :userForm");
                return;
            }

            if (newUser.getPasswordHash() == null || newUser.getPasswordHash().trim().isEmpty()) {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"),
                        JsfMessages.get("users.error.passwordRequiredCreate"),
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

            utilisateurRepository.save(utilisateur);

            notificationBean.showSuccessWithUpdate(JsfMessages.get("common.growl.success"), JsfMessages.get("users.success.created"), ":growl, :userForm");
        } else {
            // Modification d'un utilisateur existant
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(newUser.getId());

            if (utilisateurOpt.isEmpty()) {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"), JsfMessages.get("users.error.userMissing"),
                        ":growl, :userForm");
                return;
            }

            Utilisateur utilisateur = utilisateurOpt.get();

            // Vérifier si l'email est unique (sauf pour l'utilisateur actuel)
            if (!utilisateur.getEmail().equals(newUser.getEmail().trim())) {
                if (utilisateurRepository.existsByEmail(newUser.getEmail().trim())) {
                    notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"), JsfMessages.get("users.error.emailExists"),
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

            notificationBean.showSuccessWithUpdate(JsfMessages.get("common.growl.success"), JsfMessages.get("users.success.updated"),
                    ":growl, :userForm");
        }

        clearFormAfterSave();

        // Rediriger vers la liste après un court délai pour permettre l'affichage du message
        // (la liste sera rechargée par onUsersPageEnter au prochain GET)
        PrimeFaces.current().executeScript("setTimeout(function() { window.location.href='/users/users.xhtml'; }, 100);");
    }

    public void supprimerUser(Utilisateur utilisateur) {
        if (!canEditOrDeleteUsers()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.accessDenied"),
                    JsfMessages.get("users.msg.accessDenied.delete"),
                    ":growl, :usersForm");
            return;
        }

        if (utilisateur == null || utilisateur.getId() == null) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"), JsfMessages.get("users.error.delete.none"), ":growl, :usersForm");
            return;
        }

        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(utilisateur.getId());
            if (utilisateurOpt.isPresent()) {
                Utilisateur user = utilisateurOpt.get();
                
                // Vérifier si l'utilisateur n'est pas l'utilisateur actuellement connecté
                if (currentUserBean.getUsername() != null && user.getEmail().equals(currentUserBean.getUsername())) {
                    notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"), JsfMessages.get("users.error.delete.self"),
                        ":growl, :usersForm");
                    return;
                }

                utilisateurRepository.delete(user);
                notificationBean.showSuccessWithUpdate(JsfMessages.get("common.growl.success"),
                        JsfMessages.format("users.success.delete", user.getPrenom(), user.getNom()),
                    ":growl, :usersForm");
                
                reloadUsers();
            } else {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"), JsfMessages.get("users.error.delete.notFound"),
                    ":growl, :usersForm");
            }
        } catch (Exception e) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"),
                    JsfMessages.format("users.error.delete.failed", e.getMessage()),
                ":growl, :usersForm");
        }
        PrimeFaces.current().ajax().update(":growl, :usersForm");
    }

    public boolean canAccessUserManagement() {
        return loginBean != null && loginBean.canAccessUserManagement();
    }

    public boolean canCreateUsers() {
        return canAccessUserManagement();
    }

    public boolean canEditOrDeleteUsers() {
        return loginBean != null && loginBean.isAdminTechniqueOrFonctionnel();
    }

    public List<Groupe> getFormGroups() {
        if (canEditOrDeleteUsers()) {
            return availableGroups;
        }
        return availableGroups.stream()
                .filter(groupe -> groupe != null
                        && GroupEnum.UTILISATEUR.getLabel().equalsIgnoreCase(groupe.getNom()))
                .toList();
    }

    private void prepareNewUserForm() {
        newUser = new Utilisateur();
        newUser.setCreateBy(currentUserBean.getUsername() != null ? currentUserBean.getUsername() : "SYSTEM");
        newUser.setActive(true);
        newUser.setGroupe(null);
        selectedGroupeId = null;
        selectedGroupe = null;
        passwordConfirmation = null;
        isEditMode = false;
    }

    /**
     * Copie les données persistées dans un objet formulaire détaché (évite de réutiliser
     * une entité de la liste JSF, source de conflits Hibernate).
     */
    private void prepareEditUserForm(Utilisateur source) {
        newUser = new Utilisateur();
        newUser.setId(source.getId());
        newUser.setNom(source.getNom());
        newUser.setPrenom(source.getPrenom());
        newUser.setEmail(source.getEmail());
        newUser.setActive(source.getActive());
        newUser.setPasswordHash(null);
        if (source.getGroupe() != null) {
            selectedGroupeId = source.getGroupe().getId();
            selectedGroupe = groupeRepository.findById(selectedGroupeId).orElse(null);
        } else {
            selectedGroupeId = null;
            selectedGroupe = null;
        }
        passwordConfirmation = null;
        isEditMode = true;
    }

    private void clearFormAfterSave() {
        newUser = null;
        passwordConfirmation = null;
        selectedGroupeId = null;
        selectedGroupe = null;
        isEditMode = false;
    }

    private void reloadUsers() {
        users = utilisateurRepository.findAllWithGroupe();
    }

    private void reloadAvailableGroups() {
        availableGroups = groupeRepository.findAll();
    }

    private boolean isSelectedGroupAllowedForCurrentUser() {
        if (canEditOrDeleteUsers()) {
            return true;
        }
        return selectedGroupe != null
                && GroupEnum.UTILISATEUR.getLabel().equalsIgnoreCase(selectedGroupe.getNom());
    }

    private void denyAccess(String detailMessage) throws IOException {
        notificationBean.showError(JsfMessages.get("common.growl.accessDenied"), detailMessage);
        FacesContext.getCurrentInstance().getExternalContext()
                .redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/index.xhtml");
    }

    public String getGroupeEtiquette(Groupe groupe) {
        if (groupe == null || groupe.getNom() == null) {
            return "role-badge-viewer";
        }
        if (GroupEnum.UTILISATEUR.getLabel().equalsIgnoreCase(groupe.getNom())) {
            return "role-badge-viewer";
        }
        // Administrateur technique et Administrateur fonctionnel : badge admin
        return "role-badge-admin";
    }
}
