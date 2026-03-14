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
    }

    /**
     * Sauvegarde les modifications du profil.
     */
    public void sauvegarder() {
        Utilisateur current = loginBean.getCurrentUser();
        if (current == null) {
            notificationBean.showErrorWithUpdate("Erreur", "Vous devez être connecté pour modifier votre profil.", ":growl, :profileForm");
            return;
        }

        Optional<Utilisateur> opt = utilisateurRepository.findById(current.getId());
        if (opt.isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur", "Utilisateur introuvable.", ":growl, :profileForm");
            return;
        }

        String prenomTrim = prenom != null ? prenom.trim() : "";
        String nomTrim = nom != null ? nom.trim() : "";
        String emailTrim = email != null ? email.trim() : "";

        if (prenomTrim.isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le prénom est obligatoire.", ":growl, :profileForm");
            return;
        }
        if (prenomTrim.length() < 2) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le prénom doit contenir au moins 2 caractères.", ":growl, :profileForm");
            return;
        }
        if (prenomTrim.length() > 100) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le prénom ne peut pas dépasser 100 caractères.", ":growl, :profileForm");
            return;
        }

        if (nomTrim.isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le nom est obligatoire.", ":growl, :profileForm");
            return;
        }
        if (nomTrim.length() < 2) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le nom doit contenir au moins 2 caractères.", ":growl, :profileForm");
            return;
        }
        if (nomTrim.length() > 100) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le nom ne peut pas dépasser 100 caractères.", ":growl, :profileForm");
            return;
        }

        if (emailTrim.isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "L'email est obligatoire.", ":growl, :profileForm");
            return;
        }
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!emailTrim.matches(emailPattern)) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le format de l'email est invalide.", ":growl, :profileForm");
            return;
        }
        if (emailTrim.length() > 255) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "L'email ne peut pas dépasser 255 caractères.", ":growl, :profileForm");
            return;
        }

        if (nouveauMotDePasse != null && !nouveauMotDePasse.trim().isEmpty()) {
            if (nouveauMotDePasse.trim().length() < 6) {
                notificationBean.showErrorWithUpdate("Erreur de validation", "Le mot de passe doit contenir au moins 6 caractères.", ":growl, :profileForm");
                return;
            }
        }

        if (utilisateurRepository.existsByEmailExcludingUserId(emailTrim, current.getId())) {
            notificationBean.showErrorWithUpdate("Erreur", "Un utilisateur avec cet email existe déjà.", ":growl, :profileForm");
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

        notificationBean.showSuccessWithUpdate("Succès", "Votre profil a été mis à jour.", ":growl, :profileForm");
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
            String entityCode = entity.getCode() != null ? entity.getCode() : "";
            String role = up.getRole() != null ? up.getRole() : "";
            items.add(new UserPermissionItem(entityTypeLabel, entityLabel, entityCode, role, entity.getId()));
        }
        items.sort(Comparator
                .comparing(UserPermissionItem::getEntityTypeLabel)
                .thenComparing(UserPermissionItem::getEntityLabel, Comparator.nullsFirst(String::compareToIgnoreCase)));
        return items;
    }

    private static String getEntityTypeLabel(String typeCode) {
        if (typeCode == null) return "Entité";
        return switch (typeCode) {
            case EntityConstants.ENTITY_TYPE_COLLECTION -> "Collection";
            case EntityConstants.ENTITY_TYPE_REFERENCE -> "Référentiel";
            case EntityConstants.ENTITY_TYPE_CATEGORY -> "Catégorie";
            case EntityConstants.ENTITY_TYPE_GROUP -> "Groupe";
            case EntityConstants.ENTITY_TYPE_SERIES -> "Série";
            case EntityConstants.ENTITY_TYPE_TYPE -> "Type";
            default -> typeCode;
        };
    }
}
