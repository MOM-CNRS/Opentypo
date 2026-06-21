package fr.cnrs.opentypo.presentation.bean.profile;

import fr.cnrs.opentypo.application.dto.UserPermissionItem;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.UserPermission;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.application.service.UtilisateurService;
import fr.cnrs.opentypo.presentation.bean.ApplicationBean;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.NotificationBean;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Bean pour la page de modification des informations personnelles de l'utilisateur connecté.
 */
@Slf4j
@Getter
@Setter
@SessionScoped
@Named("profileBean")
public class ProfileBean implements Serializable {

    @Inject
    private LoginBean loginBean;

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Inject
    private UtilisateurService utilisateurService;

    @Inject
    private NotificationBean notificationBean;

    @Inject
    private UserPermissionRepository userPermissionRepository;

    @Inject
    private ApplicationBean applicationBean;

    private String prenom;
    private String nom;
    private String email;
    private String nouveauMotDePasse;
    private String confirmationMotDePasse;

    @PostConstruct
    public void init() {
        chargerDonnees();
    }

    /**
     * Charge les données de l'utilisateur connecté.
     */
    public void chargerDonnees() {
        Utilisateur current = loginBean.getCurrentUser();
        if (current != null) {
            Utilisateur managed = utilisateurRepository.findById(current.getId()).orElse(null);
            if (managed != null) {
                prenom = managed.getPrenom();
                nom = managed.getNom();
                email = managed.getEmail();
            }
        }
        nouveauMotDePasse = null;
        confirmationMotDePasse = null;
    }

    /**
     * Sauvegarde les modifications du profil.
     */
    public void sauvegarder() {
        Utilisateur current = loginBean.getCurrentUser();
        if (current == null) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"),
                    JsfMessages.get("profile.error.mustLogin"), ":growl, :profileForm");
            return;
        }

        Optional<Utilisateur> opt = utilisateurRepository.findById(current.getId());
        if (opt.isEmpty()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"),
                    JsfMessages.get("profile.error.userNotFound"), ":growl, :profileForm");
            return;
        }

        String prenomTrim = prenom != null ? prenom.trim() : "";
        String nomTrim = nom != null ? nom.trim() : "";
        String emailTrim = email != null ? email.trim() : "";

        if (prenomTrim.isEmpty()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                    JsfMessages.get("profile.validation.firstName"), ":growl, :profileForm");
            return;
        }
        if (prenomTrim.length() < 2) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                    JsfMessages.get("profile.validation.firstNameMin"), ":growl, :profileForm");
            return;
        }
        if (prenomTrim.length() > EntityConstants.VARCHAR_COLUMN_MAX_LENGTH) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                    JsfMessages.format("profile.validation.firstNameMax", EntityConstants.VARCHAR_COLUMN_MAX_LENGTH),
                    ":growl, :profileForm");
            return;
        }

        if (nomTrim.isEmpty()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                    JsfMessages.get("profile.validation.lastName"), ":growl, :profileForm");
            return;
        }
        if (nomTrim.length() < 2) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                    JsfMessages.get("profile.validation.lastNameMin"), ":growl, :profileForm");
            return;
        }
        if (nomTrim.length() > EntityConstants.VARCHAR_COLUMN_MAX_LENGTH) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                    JsfMessages.format("profile.validation.lastNameMax", EntityConstants.VARCHAR_COLUMN_MAX_LENGTH),
                    ":growl, :profileForm");
            return;
        }

        if (emailTrim.isEmpty()) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                    JsfMessages.get("profile.validation.email"), ":growl, :profileForm");
            return;
        }
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!emailTrim.matches(emailPattern)) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                    JsfMessages.get("profile.validation.emailFormat"), ":growl, :profileForm");
            return;
        }
        if (emailTrim.length() > EntityConstants.VARCHAR_COLUMN_MAX_LENGTH) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                    JsfMessages.format("profile.validation.emailMax", EntityConstants.VARCHAR_COLUMN_MAX_LENGTH),
                    ":growl, :profileForm");
            return;
        }

        if (nouveauMotDePasse != null && !nouveauMotDePasse.trim().isEmpty()) {
            if (nouveauMotDePasse.trim().length() < 6) {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                        JsfMessages.get("profile.validation.passwordMin"), ":growl, :profileForm");
                return;
            }
            String confirm = confirmationMotDePasse != null ? confirmationMotDePasse.trim() : "";
            if (confirm.isEmpty()) {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                        JsfMessages.get("profile.validation.passwordConfirmRequired"), ":growl, :profileForm");
                return;
            }
            if (!nouveauMotDePasse.trim().equals(confirm)) {
                notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.validation"),
                        JsfMessages.get("profile.validation.passwordMismatch"), ":growl, :profileForm");
                return;
            }
        }

        if (utilisateurRepository.existsByEmailExcludingUserId(emailTrim, current.getId())) {
            notificationBean.showErrorWithUpdate(JsfMessages.get("common.growl.error"),
                    JsfMessages.get("profile.error.emailExists"), ":growl, :profileForm");
            return;
        }

        Utilisateur utilisateur = opt.get();
        utilisateur.setPrenom(prenomTrim);
        utilisateur.setNom(nomTrim);
        utilisateur.setEmail(emailTrim);

        if (nouveauMotDePasse != null && !nouveauMotDePasse.trim().isEmpty()) {
            utilisateur.setPasswordHash(utilisateurService.encodePassword(nouveauMotDePasse.trim()));
        }

        utilisateurRepository.save(utilisateur);

        loginBean.setCurrentUser(utilisateur);
        nouveauMotDePasse = null;
        confirmationMotDePasse = null;

        notificationBean.showSuccessWithUpdate(JsfMessages.get("common.growl.success"),
                JsfMessages.get("profile.success.updated"), ":growl, :profileForm");
        PrimeFaces.current().ajax().update(":profileForm");
    }

    /**
     * Retourne la liste des entités auxquelles l'utilisateur est rattaché (gestionnaire, rédacteur, validateur, relecteur...).
     */
    public List<UserPermissionItem> getProfilePermissions() {
        Utilisateur current = loginBean.getCurrentUser();
        if (current == null) {
            return List.of();
        }
        List<UserPermission> permissions = userPermissionRepository.findByUtilisateurWithEntityAndLabels(current);
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        List<UserPermissionItem> items = new ArrayList<>();
        for (UserPermission up : permissions) {
            Entity entity = up.getEntity();
            if (entity == null) continue;
            String entityTypeLabel = getEntityTypeLabel(entity.getEntityType() != null ? entity.getEntityType().getCode() : null);
            String entityLabel = applicationBean.getEntityLabel(entity);
            String entityLabelPlainText = applicationBean.getEntityLabelPlainText(entity);
            String entityCode = entity.getCode() != null ? entity.getCode() : "";
            String role = up.getRole() != null ? up.getRole() : "";
            items.add(new UserPermissionItem(entityTypeLabel, entityLabel, entityLabelPlainText, entityCode, role, entity.getId()));
        }
        items.sort(Comparator
                .comparing(UserPermissionItem::getEntityTypeLabel)
                .thenComparing(UserPermissionItem::getEntityLabelPlainText, Comparator.nullsFirst(String::compareToIgnoreCase)));
        return items;
    }

    private static String getEntityTypeLabel(String typeCode) {
        if (typeCode == null) return JsfMessages.get("profile.entityType.default");
        return switch (typeCode) {
            case EntityConstants.ENTITY_TYPE_COLLECTION -> JsfMessages.get("profile.entityType.collection");
            case EntityConstants.ENTITY_TYPE_REFERENCE -> JsfMessages.get("profile.entityType.reference");
            case EntityConstants.ENTITY_TYPE_CATEGORY -> JsfMessages.get("profile.entityType.category");
            case EntityConstants.ENTITY_TYPE_GROUP -> JsfMessages.get("profile.entityType.group");
            case EntityConstants.ENTITY_TYPE_SERIES -> JsfMessages.get("profile.entityType.series");
            case EntityConstants.ENTITY_TYPE_TYPE -> JsfMessages.get("profile.entityType.type");
            default -> typeCode;
        };
    }
}
