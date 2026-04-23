package fr.cnrs.opentypo.presentation.bean.authors;

import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.AuteurScientifique;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.AuteurScientifiqueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.NotificationBean;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Getter
@Setter
@SessionScoped
@Named("scientificAuthorBean")
public class ScientificAuthorManagementBean implements Serializable {

    @Inject
    private AuteurScientifiqueRepository auteurScientifiqueRepository;

    @Inject
    private UserPermissionRepository userPermissionRepository;

    @Inject
    private LoginBean loginBean;

    @Inject
    private NotificationBean notificationBean;

    private List<AuteurScientifique> allAuthors = new ArrayList<>();
    private List<AuteurScientifique> filteredAuthors = new ArrayList<>();
    private AuteurScientifique editingAuthor = new AuteurScientifique();
    private boolean editMode;
    private boolean editorVisible;
    private String searchText;

    @PostConstruct
    public void init() {
        reloadAuthors();
        resetForm();
    }

    public void onPageEnter() {
        reloadAuthors();
    }

    public void resetForm() {
        editingAuthor = new AuteurScientifique();
        editingAuthor.setActive(true);
        editMode = false;
        editorVisible = false;
    }

    public void startCreate() {
        if (!canManageScientificAuthors()) {
            notificationBean.showError("Accès refusé", "Vous n'avez pas les droits pour créer un auteur scientifique.");
            return;
        }
        resetForm();
        editorVisible = true;
        PrimeFaces.current().ajax().update(":authorsForm:authorEditor");
    }

    public void startEdit(AuteurScientifique author) {
        if (!canManageScientificAuthors()) {
            notificationBean.showError("Accès refusé", "Vous n'avez pas les droits pour modifier un auteur scientifique.");
            return;
        }
        if (author == null || author.getId() == null) {
            notificationBean.showError("Erreur", "Auteur scientifique introuvable.");
            return;
        }
        editingAuthor = new AuteurScientifique(author.getId(), author.getNom(), author.getPrenom(), author.getActive());
        editMode = true;
        editorVisible = true;
        PrimeFaces.current().ajax().update(":authorsForm:authorEditor");
    }

    public void saveAuthor() {
        if (!canManageScientificAuthors()) {
            notificationBean.showErrorWithUpdate("Accès refusé", "Vous n'avez pas les droits pour cette action.", ":growl, :authorsForm");
            return;
        }

        String nom = editingAuthor != null ? normalize(editingAuthor.getNom()) : "";
        String prenom = editingAuthor != null ? normalize(editingAuthor.getPrenom()) : "";

        if (!StringUtils.hasText(nom)) {
            notificationBean.showErrorWithUpdate("Validation", "Le nom est obligatoire.", ":growl, :authorsForm");
            return;
        }
        if (!StringUtils.hasText(prenom)) {
            notificationBean.showErrorWithUpdate("Validation", "Le prénom est obligatoire.", ":growl, :authorsForm");
            return;
        }
        if (nom.length() > 120 || prenom.length() > 120) {
            notificationBean.showErrorWithUpdate("Validation", "Le nom et le prénom ne peuvent pas dépasser 120 caractères.",
                    ":growl, :authorsForm");
            return;
        }

        AuteurScientifique entityToSave;
        if (editMode && editingAuthor.getId() != null) {
            Optional<AuteurScientifique> existingOpt = auteurScientifiqueRepository.findById(editingAuthor.getId());
            if (existingOpt.isEmpty()) {
                notificationBean.showErrorWithUpdate("Erreur", "L'auteur scientifique n'existe plus.", ":growl, :authorsForm");
                reloadAuthors();
                return;
            }
            entityToSave = existingOpt.get();
        } else {
            entityToSave = new AuteurScientifique();
        }

        entityToSave.setNom(nom);
        entityToSave.setPrenom(prenom);
        entityToSave.setActive(editingAuthor.getActive() == null || editingAuthor.getActive());

        auteurScientifiqueRepository.save(entityToSave);
        reloadAuthors();
        resetForm();
        notificationBean.showSuccessWithUpdate("Succès", "Auteur scientifique enregistré avec succès.", ":growl, :authorsForm");
    }

    public void deleteAuthor(AuteurScientifique author) {
        if (!canManageScientificAuthors()) {
            notificationBean.showErrorWithUpdate("Accès refusé", "Vous n'avez pas les droits pour cette action.", ":growl, :authorsForm");
            return;
        }
        if (author == null || author.getId() == null) {
            notificationBean.showErrorWithUpdate("Erreur", "Auteur scientifique introuvable.", ":growl, :authorsForm");
            return;
        }

        auteurScientifiqueRepository.deleteById(author.getId());
        reloadAuthors();
        if (editMode && editingAuthor != null && author.getId().equals(editingAuthor.getId())) {
            resetForm();
        }
        notificationBean.showSuccessWithUpdate("Succès", "Auteur scientifique supprimé.", ":growl, :authorsForm");
    }

    public void toggleVisibility(AuteurScientifique author) {
        if (!canManageScientificAuthors()) {
            notificationBean.showErrorWithUpdate("Accès refusé", "Vous n'avez pas les droits pour cette action.", ":growl, :authorsForm");
            return;
        }
        if (author == null || author.getId() == null) {
            notificationBean.showErrorWithUpdate("Erreur", "Auteur scientifique introuvable.", ":growl, :authorsForm");
            return;
        }

        Optional<AuteurScientifique> opt = auteurScientifiqueRepository.findById(author.getId());
        if (opt.isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur", "L'auteur scientifique n'existe plus.", ":growl, :authorsForm");
            reloadAuthors();
            return;
        }

        AuteurScientifique entity = opt.get();
        boolean target = entity.getActive() == null || !entity.getActive();
        entity.setActive(target);
        auteurScientifiqueRepository.save(entity);

        if (editMode && editingAuthor != null && entity.getId().equals(editingAuthor.getId())) {
            editingAuthor.setActive(target);
        }
        reloadAuthors();
        notificationBean.showInfoWithUpdate("Visibilité mise à jour",
                target ? "L'auteur scientifique est maintenant actif." : "L'auteur scientifique est maintenant masqué.",
                ":growl, :authorsForm");
    }

    public void applyFilter() {
        if (!StringUtils.hasText(searchText)) {
            filteredAuthors = new ArrayList<>(allAuthors);
            return;
        }
        String normalized = searchText.trim().toLowerCase(Locale.ROOT);
        filteredAuthors = allAuthors.stream()
                .filter(author -> author != null)
                .filter(author -> {
                    String nom = author.getNom() != null ? author.getNom().toLowerCase(Locale.ROOT) : "";
                    String prenom = author.getPrenom() != null ? author.getPrenom().toLowerCase(Locale.ROOT) : "";
                    String full = (prenom + " " + nom).trim();
                    return nom.contains(normalized) || prenom.contains(normalized) || full.contains(normalized);
                })
                .toList();
    }

    public boolean canManageScientificAuthors() {
        if (loginBean == null || !loginBean.isAuthenticated()) {
            return false;
        }
        if (loginBean.isAdminTechniqueOrFonctionnel()) {
            return true;
        }
        Utilisateur user = loginBean.getCurrentUser();
        if (user == null || user.getId() == null || userPermissionRepository == null) {
            return false;
        }

        Long userId = user.getId();
        if (userPermissionRepository.existsByUserIdAndRole(userId, PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        if (userPermissionRepository.existsByUserIdAndRole(userId, PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())) {
            return true;
        }
        return userPermissionRepository.existsByUserIdAndRoleAndEntityTypeCode(
                userId,
                PermissionRoleEnum.REDACTEUR.getLabel(),
                EntityConstants.ENTITY_TYPE_GROUP
        );
    }

    private void reloadAuthors() {
        allAuthors = auteurScientifiqueRepository.findAllByOrderByNomAscPrenomAsc();
        applyFilter();
    }

    public long getActiveAuthorsCount() {
        return allAuthors.stream()
                .filter(author -> author != null && Boolean.TRUE.equals(author.getActive()))
                .count();
    }

    public long getInactiveAuthorsCount() {
        return allAuthors.stream()
                .filter(author -> author != null && !Boolean.TRUE.equals(author.getActive()))
                .count();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
