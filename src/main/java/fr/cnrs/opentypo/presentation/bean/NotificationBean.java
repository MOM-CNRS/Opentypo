package fr.cnrs.opentypo.presentation.bean;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import org.primefaces.PrimeFaces;

import java.io.Serializable;

/**
 * Bean centralisé pour la gestion des messages utilisateur
 * Fournit des méthodes simples pour afficher différents types de notifications
 */
@Named("notificationBean")
@SessionScoped
public class NotificationBean implements Serializable {

    private static final long serialVersionUID = 1L;
    
    // Composants à mettre à jour par défaut
    private static final String DEFAULT_UPDATE_COMPONENTS = ":growl";

    /**
     * Affiche un message d'information
     * 
     * @param message Le message à afficher
     */
    public void showInfo(String message) {
        showInfo(null, message);
    }

    /**
     * Affiche un message d'information avec un titre
     * 
     * @param title Le titre du message
     * @param message Le message à afficher
     */
    public void showInfo(String title, String message) {
        addMessage(FacesMessage.SEVERITY_INFO, title, message);
    }

    /**
     * Affiche un message de succès
     * 
     * @param message Le message à afficher
     */
    public void showSuccess(String message) {
        showSuccess(null, message);
    }

    /**
     * Affiche un message de succès avec un titre
     * 
     * @param title Le titre du message
     * @param message Le message à afficher
     */
    public void showSuccess(String title, String message) {
        addMessage(FacesMessage.SEVERITY_INFO, title, message);
    }

    /**
     * Affiche un message d'avertissement
     * 
     * @param message Le message à afficher
     */
    public void showWarning(String message) {
        showWarning(null, message);
    }

    /**
     * Affiche un message d'avertissement avec un titre
     * 
     * @param title Le titre du message
     * @param message Le message à afficher
     */
    public void showWarning(String title, String message) {
        addMessage(FacesMessage.SEVERITY_WARN, title, message);
    }

    /**
     * Affiche un message d'erreur
     * 
     * @param message Le message à afficher
     */
    public void showError(String message) {
        showError(null, message);
    }

    /**
     * Affiche un message d'erreur avec un titre
     * 
     * @param title Le titre du message
     * @param message Le message à afficher
     */
    public void showError(String title, String message) {
        addMessage(FacesMessage.SEVERITY_ERROR, title, message);
    }

    /**
     * Affiche un message fatal
     * 
     * @param message Le message à afficher
     */
    public void showFatal(String message) {
        showFatal(null, message);
    }

    /**
     * Affiche un message fatal avec un titre
     * 
     * @param title Le titre du message
     * @param message Le message à afficher
     */
    public void showFatal(String title, String message) {
        addMessage(FacesMessage.SEVERITY_FATAL, title, message);
    }

    /**
     * Ajoute un message au contexte JSF et met à jour le composant growl
     * 
     * @param severity La sévérité du message
     * @param title Le titre du message (peut être null)
     * @param message Le message à afficher
     */
    private void addMessage(FacesMessage.Severity severity, String title, String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String summary = title != null && !title.isEmpty() ? title : getDefaultTitle(severity);
            FacesMessage facesMessage = new FacesMessage(severity, summary, message);
            facesContext.addMessage(null, facesMessage);
            
            // Mettre à jour le composant growl via AJAX
            updateGrowl();
        }
    }

    /**
     * Ajoute un message au contexte JSF et met à jour des composants spécifiques
     * 
     * @param severity La sévérité du message
     * @param title Le titre du message (peut être null)
     * @param message Le message à afficher
     * @param updateComponents Les composants à mettre à jour (séparés par des virgules)
     */
    public void addMessage(FacesMessage.Severity severity, String title, String message, String updateComponents) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String summary = title != null && !title.isEmpty() ? title : getDefaultTitle(severity);
            FacesMessage facesMessage = new FacesMessage(severity, summary, message);
            facesContext.addMessage(null, facesMessage);
            
            // Mettre à jour les composants spécifiés via AJAX
            updateComponents(updateComponents);
        }
    }

    /**
     * Met à jour le composant growl par défaut
     */
    private void updateGrowl() {
        try {
            PrimeFaces.current().ajax().update(DEFAULT_UPDATE_COMPONENTS);
        } catch (Exception e) {
            // Ignorer les erreurs si PrimeFaces n'est pas disponible
        }
    }

    /**
     * Met à jour des composants spécifiques
     * 
     * @param components Les composants à mettre à jour (séparés par des virgules)
     */
    private void updateComponents(String components) {
        try {
            String updateList = DEFAULT_UPDATE_COMPONENTS;
            if (components != null && !components.isEmpty()) {
                updateList = DEFAULT_UPDATE_COMPONENTS + ", " + components;
            }
            PrimeFaces.current().ajax().update(updateList);
        } catch (Exception e) {
            // Ignorer les erreurs si PrimeFaces n'est pas disponible
        }
    }

    /**
     * Retourne le titre par défaut selon la sévérité
     * 
     * @param severity La sévérité du message
     * @return Le titre par défaut
     */
    private String getDefaultTitle(FacesMessage.Severity severity) {
        if (severity == FacesMessage.SEVERITY_INFO) {
            return "Information";
        } else if (severity == FacesMessage.SEVERITY_WARN) {
            return "Avertissement";
        } else if (severity == FacesMessage.SEVERITY_ERROR) {
            return "Erreur";
        } else if (severity == FacesMessage.SEVERITY_FATAL) {
            return "Erreur fatale";
        }
        return "Notification";
    }

    /**
     * Affiche un message d'information avec mise à jour de composants personnalisés
     * 
     * @param message Le message à afficher
     * @param updateComponents Les composants à mettre à jour
     */
    public void showInfoWithUpdate(String message, String updateComponents) {
        showInfoWithUpdate(null, message, updateComponents);
    }

    /**
     * Affiche un message d'information avec titre et mise à jour de composants personnalisés
     * 
     * @param title Le titre du message
     * @param message Le message à afficher
     * @param updateComponents Les composants à mettre à jour
     */
    public void showInfoWithUpdate(String title, String message, String updateComponents) {
        addMessage(FacesMessage.SEVERITY_INFO, title, message, updateComponents);
    }

    /**
     * Affiche un message de succès avec mise à jour de composants personnalisés
     * 
     * @param message Le message à afficher
     * @param updateComponents Les composants à mettre à jour
     */
    public void showSuccessWithUpdate(String message, String updateComponents) {
        showSuccessWithUpdate(null, message, updateComponents);
    }

    /**
     * Affiche un message de succès avec titre et mise à jour de composants personnalisés
     * 
     * @param title Le titre du message
     * @param message Le message à afficher
     * @param updateComponents Les composants à mettre à jour
     */
    public void showSuccessWithUpdate(String title, String message, String updateComponents) {
        addMessage(FacesMessage.SEVERITY_INFO, title, message, updateComponents);
    }

    /**
     * Affiche un message d'avertissement avec mise à jour de composants personnalisés
     * 
     * @param message Le message à afficher
     * @param updateComponents Les composants à mettre à jour
     */
    public void showWarningWithUpdate(String message, String updateComponents) {
        showWarningWithUpdate(null, message, updateComponents);
    }

    /**
     * Affiche un message d'avertissement avec titre et mise à jour de composants personnalisés
     * 
     * @param title Le titre du message
     * @param message Le message à afficher
     * @param updateComponents Les composants à mettre à jour
     */
    public void showWarningWithUpdate(String title, String message, String updateComponents) {
        addMessage(FacesMessage.SEVERITY_WARN, title, message, updateComponents);
    }

    /**
     * Affiche un message d'erreur avec mise à jour de composants personnalisés
     * 
     * @param message Le message à afficher
     * @param updateComponents Les composants à mettre à jour
     */
    public void showErrorWithUpdate(String message, String updateComponents) {
        showErrorWithUpdate(null, message, updateComponents);
    }

    /**
     * Affiche un message d'erreur avec titre et mise à jour de composants personnalisés
     * 
     * @param title Le titre du message
     * @param message Le message à afficher
     * @param updateComponents Les composants à mettre à jour
     */
    public void showErrorWithUpdate(String title, String message, String updateComponents) {
        addMessage(FacesMessage.SEVERITY_ERROR, title, message, updateComponents);
    }
}
