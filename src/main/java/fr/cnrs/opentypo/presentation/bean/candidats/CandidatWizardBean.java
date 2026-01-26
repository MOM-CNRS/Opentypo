package fr.cnrs.opentypo.presentation.bean.candidats;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.SearchBean;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatEntityService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.model.TreeNode;

import java.io.Serializable;

/**
 * Bean pour la gestion du wizard de création de candidat
 * Responsable de la navigation entre les étapes et de la validation
 */
@Named("candidatWizardBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class CandidatWizardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private LoginBean loginBean;

    @Inject
    private SearchBean searchBean;

    @Inject
    private CandidatEntityService candidatEntityService;

    @Inject
    private CandidatFormDataBean candidatFormDataBean;

    @Inject
    private CandidatReferenceTreeBean candidatReferenceTreeBean;

    // État du wizard
    private int currentStep = 0; // 0 = étape 1, 1 = étape 2, 2 = étape 3
    private Entity currentEntity; // Entité créée à l'étape 1

    // Données de l'étape 1
    private Long selectedEntityTypeId;
    private String entityCode;
    private String entityLabel;
    private String selectedLangueCode;
    private Long selectedCollectionId;
    private Entity selectedParentEntity;

    /**
     * Passe à l'étape suivante du wizard
     */
    public String nextStep() {
        log.info("nextStep() appelée - currentStep actuel: {}", currentStep);
        
        if (currentStep == 0) { // De l'étape 1 à l'étape 2
            log.info("Validation de l'étape 1...");
            if (validateStep1()) {
                try {
                    // Récupérer l'utilisateur actuel depuis LoginBean
                    Utilisateur currentUser = loginBean != null ? loginBean.getCurrentUser() : null;
                    currentEntity = candidatEntityService.createEntityFromStep1(
                        selectedEntityTypeId, entityCode, entityLabel, selectedLangueCode, currentUser);
                    log.info("Entité créée à l'étape 1 avec l'ID: {}", currentEntity.getId());
                    
                    // Mettre à jour les références dans les autres beans
                    candidatFormDataBean.setCurrentEntityAndLangue(currentEntity, selectedLangueCode);
                    candidatReferenceTreeBean.setSelectedLangueCode(selectedLangueCode);
                    
                    currentStep++;
                    log.info("Passage à l'étape 2 - currentStep = {}", currentStep);
                    log.info("isStep1() = {}, isStep2() = {}, isStep3() = {}", isStep1(), isStep2(), isStep3());
                    
                    // Forcer la mise à jour AJAX des composants
                    PrimeFaces.current().ajax().update(":createCandidatForm:wizardContent");
                } catch (Exception e) {
                    log.error("Erreur lors de la création de l'entité à l'étape 1", e);
                    showError("Une erreur est survenue lors de la création de l'entité : " + e.getMessage());
                    return null;
                }
            } else {
                log.warn("Validation échouée à l'étape 1, reste sur l'étape 1");
            }
        } else if (currentStep == 1) { // De l'étape 2 à l'étape 3
            log.info("Validation de l'étape 2...");
            if (validateStep2()) {
                // Récupérer l'entité parent depuis le nœud sélectionné
                TreeNode<?> selectedTreeNode = candidatReferenceTreeBean.getSelectedTreeNode();
                if (selectedTreeNode != null) {
                    Entity parent = (Entity) selectedTreeNode.getData();
                    selectedParentEntity = entityRepository.findById(parent.getId()).orElse(null);
                    
                    // Créer la relation parent-enfant
                    if (currentEntity != null && currentEntity.getId() != null && selectedParentEntity != null) {
                        try {
                            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
                            if (refreshedEntity != null) {
                                candidatEntityService.createParentChildRelation(selectedParentEntity, refreshedEntity);
                            }
                        } catch (Exception e) {
                            log.error("Erreur lors de la création de la relation parent-enfant", e);
                            showError("Une erreur est survenue lors de la création de la relation : " + e.getMessage());
                            return null;
                        }
                    }
                }
                
                // Charger les données existantes depuis la base de données
                candidatFormDataBean.loadExistingStep3Data();
                
                currentStep++;
                log.info("Passage à l'étape 3 - currentStep = {}", currentStep);
                log.info("isStep1() = {}, isStep2() = {}, isStep3() = {}", isStep1(), isStep2(), isStep3());
                
                // Forcer la mise à jour AJAX des composants
                PrimeFaces.current().ajax().update(":createCandidatForm:wizardContent");
            } else {
                log.warn("Validation échouée à l'étape 2, reste sur l'étape 2");
            }
        } else {
            log.warn("nextStep() appelée mais currentStep = {} (hors limites)", currentStep);
        }
        
        return null; // Reste sur la même page avec mise à jour AJAX
    }

    /**
     * Retourne à l'étape précédente du wizard
     */
    public String previousStep() {
        log.info("previousStep() appelée - currentStep actuel: {}", currentStep);
        if (currentStep > 0) {
            currentStep--;
            log.info("Retour à l'étape précédente - currentStep = {}", currentStep);
            
            // Forcer la mise à jour AJAX des composants
            PrimeFaces.current().ajax().update(":createCandidatForm:wizardContent");
        }
        return null; // Reste sur la même page avec mise à jour AJAX
    }

    /**
     * Vérifie si on est à l'étape 1
     */
    public boolean isStep1() {
        boolean result = currentStep == 0;
        log.debug("isStep1() appelée - currentStep = {}, retourne = {}", currentStep, result);
        return result;
    }

    /**
     * Vérifie si on est à l'étape 2
     */
    public boolean isStep2() {
        boolean result = currentStep == 1;
        log.debug("isStep2() appelée - currentStep = {}, retourne = {}", currentStep, result);
        return result;
    }

    /**
     * Vérifie si on est à l'étape 3
     */
    public boolean isStep3() {
        boolean result = currentStep == 2;
        log.debug("isStep3() appelée - currentStep = {}, retourne = {}", currentStep, result);
        return result;
    }

    /**
     * Valide les données de l'étape 1
     */
    private boolean validateStep1() {
        boolean isValid = true;
        
        if (selectedEntityTypeId == null) {
            showError("Le type d'entité est requis.");
            isValid = false;
        }
        
        if (entityCode == null || entityCode.trim().isEmpty()) {
            showError("Le code est requis.");
            isValid = false;
        }
        
        if (entityLabel == null || entityLabel.trim().isEmpty()) {
            showError("Le label est requis.");
            isValid = false;
        }
        
        if (selectedLangueCode == null || selectedLangueCode.trim().isEmpty()) {
            showError("La langue est requise.");
            isValid = false;
        }
        
        if (!isValid) {
            PrimeFaces.current().ajax().update(":growl");
        }
        
        return isValid;
    }

    /**
     * Valide les données de l'étape 2
     */
    private boolean validateStep2() {
        boolean isValid = true;
        
        TreeNode<?> selectedTreeNode = candidatReferenceTreeBean.getSelectedTreeNode();
        if (selectedTreeNode == null) {
            showError("Le référentiel est requis.");
            isValid = false;
        }
        
        if (!isValid) {
            PrimeFaces.current().ajax().update(":growl");
        }
        
        return isValid;
    }

    /**
     * Réinitialise le formulaire du wizard
     */
    public void resetWizardForm() {
        currentStep = 0;
        currentEntity = null;
        selectedEntityTypeId = null;
        entityCode = null;
        entityLabel = null;
        selectedLangueCode = searchBean.getLangSelected();
        selectedCollectionId = null;
        selectedParentEntity = null;
        
        // Réinitialiser les autres beans
        candidatReferenceTreeBean.setSelectedCollectionId(null);
        candidatReferenceTreeBean.setSelectedDirectEntityId(null);
        candidatReferenceTreeBean.setReferenceTreeRoot(null);
        candidatReferenceTreeBean.setSelectedTreeNode(null);
        candidatFormDataBean.resetFormData();
    }

    /**
     * Récupère le type d'entité sélectionné
     */
    public EntityType getSelectedEntityType() {
        if (selectedEntityTypeId == null) {
            return null;
        }
        return entityTypeRepository.findById(selectedEntityTypeId).orElse(null);
    }

    /**
     * Termine le processus de création de candidat et redirige vers la liste des candidats
     * Met à jour l'entité avec les valeurs finales depuis CandidatFormDataBean
     */
    public String terminerCandidat() {
        try {
            // Vérifier que currentEntity existe
            if (currentEntity == null || currentEntity.getId() == null) {
                showError("L'entité n'a pas été créée. Veuillez compléter les étapes précédentes.");
                PrimeFaces.current().ajax().update(":growl");
                return null;
            }
            
            // Recharger l'entité depuis la base
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity == null) {
                showError("L'entité n'a pas été trouvée dans la base de données.");
                PrimeFaces.current().ajax().update(":growl");
                return null;
            }
            
            // Mettre à jour les champs depuis formDataBean
            String refs = null;
            if (candidatFormDataBean.getReferencesBibliographiques() != null && 
                !candidatFormDataBean.getReferencesBibliographiques().isEmpty()) {
                refs = String.join("; ", candidatFormDataBean.getReferencesBibliographiques());
            }
            
            String ateliersStr = null;
            if (candidatFormDataBean.getAteliers() != null && 
                !candidatFormDataBean.getAteliers().isEmpty()) {
                ateliersStr = String.join("; ", candidatFormDataBean.getAteliers());
            }
            
            candidatEntityService.updateEntityBasicFields(
                refreshedEntity,
                candidatFormDataBean.getCandidatCommentaire(),
                candidatFormDataBean.getCandidatBibliographie(),
                refs,
                ateliersStr,
                candidatFormDataBean.getGroupTpq(),
                candidatFormDataBean.getGroupTaq()
            );
            
            // Mettre à jour la période si nécessaire
            if (candidatFormDataBean.getGroupPeriode() != null && 
                !candidatFormDataBean.getGroupPeriode().trim().isEmpty()) {
                String currentCommentaire = refreshedEntity.getCommentaire();
                if (currentCommentaire == null || currentCommentaire.trim().isEmpty()) {
                    refreshedEntity.setCommentaire("Période: " + candidatFormDataBean.getGroupPeriode().trim());
                } else if (!currentCommentaire.contains("Période:")) {
                    refreshedEntity.setCommentaire(currentCommentaire + "\n\nPériode: " + candidatFormDataBean.getGroupPeriode().trim());
                }
                entityRepository.save(refreshedEntity);
            }
            
            log.info("Entité mise à jour avec les valeurs finales: ID={}", refreshedEntity.getId());
            
            // Réinitialiser le formulaire
            resetWizardForm();
            
            // Rediriger vers la liste des candidats avec un paramètre de succès
            return "/candidats/candidats.xhtml?faces-redirect=true&success=true";
            
        } catch (Exception e) {
            log.error("Erreur lors de la finalisation du candidat", e);
            return "/candidats/candidats.xhtml?faces-redirect=true&error=true";
        }
    }

    // Méthodes utilitaires pour les messages
    private void showError(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", message));
        }
    }
}
