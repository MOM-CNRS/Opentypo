package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.domain.entity.Commentaire;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.CommentaireRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Bean pour la gestion des commentaires sur les entités.
 * Seuls les utilisateurs connectés peuvent publier.
 * Seul le propriétaire d'un commentaire peut le modifier ou le supprimer.
 */
@Named("commentaireBean")
@SessionScoped
@Getter
@Setter
public class CommentaireBean implements Serializable {

    @Inject
    private ApplicationBean applicationBean;

    @Inject
    private LoginBean loginBean;

    @Autowired
    private CommentaireRepository commentaireRepository;

    @Autowired
    private EntityRepository entityRepository;

    private String newCommentContenu = "";
    private Long editingCommentId;
    private String editingCommentContenu = "";

    /**
     * Liste des commentaires de l'entité sélectionnée, triés par date décroissante.
     */
    public List<Commentaire> getCommentaires() {
        Entity entity = applicationBean.getSelectedEntity();
        if (entity == null || entity.getId() == null) {
            return List.of();
        }
        return commentaireRepository.findByEntity_IdOrderByDateCreationDesc(entity.getId());
    }

    /**
     * Vérifie si l'utilisateur courant est le propriétaire du commentaire.
     */
    public boolean isCommentOwner(Commentaire commentaire) {
        if (commentaire == null || commentaire.getUtilisateur() == null) {
            return false;
        }
        Utilisateur current = loginBean.getCurrentUser();
        return current != null && current.getId() != null
                && current.getId().equals(commentaire.getUtilisateur().getId());
    }

    /**
     * Formate la date de création d'un commentaire.
     */
    public String formatDate(LocalDateTime date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
    }

    /**
     * Affiche le nom de l'auteur d'un commentaire (ou "Anonyme" si absent).
     */
    public String getAuthorDisplayName(Commentaire commentaire) {
        if (commentaire == null || commentaire.getUtilisateur() == null) {
            return JsfMessages.get("comment.anonymous");
        }
        Utilisateur u = commentaire.getUtilisateur();
        return (u.getPrenom() != null ? u.getPrenom() + " " : "") + (u.getNom() != null ? u.getNom() : "");
    }

    /**
     * Ajoute un nouveau commentaire (réservé aux utilisateurs connectés).
     */
    @Transactional
    public void addCommentaire() {
        if (!loginBean.isAuthenticated()) {
            addMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("comment.error.unauthorized"),
                    JsfMessages.get("comment.error.mustLogin"));
            return;
        }
        String contenu = newCommentContenu != null ? newCommentContenu.trim() : "";
        if (contenu.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.fieldRequired"),
                    JsfMessages.get("comment.warn.contentRequired"));
            return;
        }
        Entity entity = applicationBean.getSelectedEntity();
        if (entity == null || entity.getId() == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"),
                    JsfMessages.get("comment.error.noEntity"));
            return;
        }
        Entity managedEntity = entityRepository.findById(entity.getId()).orElse(null);
        if (managedEntity == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"),
                    JsfMessages.get("comment.error.entityNotFound"));
            return;
        }
        managedEntity.addCommentaire(contenu, loginBean.getCurrentUser());
        entityRepository.save(managedEntity);
        newCommentContenu = "";
        addMessage(FacesMessage.SEVERITY_INFO, JsfMessages.get("common.growl.success"),
                JsfMessages.get("comment.success.published"));
        PrimeFaces.current().ajax().update(":contentPanels :growl");
    }

    /**
     * Démarre l'édition d'un commentaire (vérifie le propriétaire).
     */
    public void startEditComment(Commentaire commentaire) {
        if (!isCommentOwner(commentaire)) {
            return;
        }
        editingCommentId = commentaire.getId();
        editingCommentContenu = commentaire.getContenu();
    }

    /**
     * Annule l'édition en cours.
     */
    public void cancelEditComment() {
        editingCommentId = null;
        editingCommentContenu = "";
        PrimeFaces.current().ajax().update(":contentPanels :growl");
    }

    /**
     * Enregistre la modification d'un commentaire (réservé au propriétaire).
     */
    @Transactional
    public void saveCommentaire() {
        if (editingCommentId == null) return;
        String contenu = editingCommentContenu != null ? editingCommentContenu.trim() : "";
        if (contenu.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.fieldRequired"),
                    JsfMessages.get("comment.warn.emptyEdit"));
            return;
        }
        Optional<Commentaire> opt = commentaireRepository.findById(editingCommentId);
        if (opt.isEmpty()) {
            cancelEditComment();
            return;
        }
        Commentaire c = opt.get();
        if (!isCommentOwner(c)) {
            addMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("comment.error.unauthorized"),
                    JsfMessages.get("comment.error.editOwnOnly"));
            cancelEditComment();
            return;
        }
        c.setContenu(contenu);
        commentaireRepository.save(c);
        cancelEditComment();
        addMessage(FacesMessage.SEVERITY_INFO, JsfMessages.get("common.growl.success"),
                JsfMessages.get("comment.success.edited"));
        PrimeFaces.current().ajax().update(":contentPanels :growl");
    }

    /**
     * Supprime un commentaire (réservé au propriétaire).
     */
    @Transactional
    public void deleteCommentaire(Commentaire commentaire) {
        if (commentaire == null || !isCommentOwner(commentaire)) {
            addMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("comment.error.unauthorized"),
                    JsfMessages.get("comment.error.deleteOwnOnly"));
            return;
        }
        commentaireRepository.delete(commentaire);
        addMessage(FacesMessage.SEVERITY_INFO, JsfMessages.get("common.growl.success"),
                JsfMessages.get("comment.success.deleted"));
        PrimeFaces.current().ajax().update(":contentPanels :growl");
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }
}
