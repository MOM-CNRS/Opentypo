package fr.cnrs.opentypo.presentation.bean.candidats;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryDescriptionItem;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryLabelItem;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean pour la gestion des données du formulaire (labels, descriptions, champs spécifiques)
 * Responsable de la gestion des traductions et descriptions multilingues
 */
@Named("candidatFormDataBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class CandidatFormDataBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private LangueRepository langueRepository;

    // Labels et descriptions
    private List<CategoryLabelItem> candidatLabels = new ArrayList<>();
    private String newLabelValue;
    private String newLabelLangueCode;
    private List<CategoryDescriptionItem> descriptions = new ArrayList<>();
    private String newDescriptionValue;
    private String newDescriptionLangueCode;

    // Champs de texte
    private String candidatCommentaire;
    private String candidatBibliographie;
    private List<String> referencesBibliographiques = new ArrayList<>();
    private List<String> ateliers = new ArrayList<>();

    // Champs spécifiques par type
    private String typeDescription;
    private String serieDescription;
    private String groupDescription;
    private String categoryDescription;
    private String collectionDescription;
    private Boolean collectionPublique = true;

    // Champs pour le groupe
    private String groupPeriode;
    private Integer groupTpq;
    private Integer groupTaq;

    // Image
    private String imagePrincipaleUrl;

    // Référence à l'entité courante (injectée depuis le wizard)
    private Entity currentEntity;
    private String selectedLangueCode; // Langue principale de l'étape 1

    /**
     * Met à jour la référence à l'entité courante et la langue sélectionnée
     * Appelé depuis CandidatWizardBean lors de la création de l'entité
     */
    public void setCurrentEntityAndLangue(Entity entity, String langueCode) {
        this.currentEntity = entity;
        this.selectedLangueCode = langueCode;
    }

    /**
     * Ajoute un nouveau label depuis les champs de saisie et le sauvegarde dans la base de données
     */
    public void addLabelFromInput() {
        if (currentEntity == null || currentEntity.getId() == null) {
            showError("L'entité n'a pas encore été créée. Veuillez d'abord compléter les étapes précédentes.");
            return;
        }
        
        if (newLabelValue == null || newLabelValue.trim().isEmpty()) {
            showWarning("Le label est requis.");
            return;
        }
        
        if (newLabelLangueCode == null || newLabelLangueCode.trim().isEmpty()) {
            showWarning("La langue est requise.");
            return;
        }
        
        // Vérifier que la langue n'est pas celle de l'étape 1
        if (selectedLangueCode != null && selectedLangueCode.equals(newLabelLangueCode)) {
            showWarning("Cette langue est déjà utilisée pour le label principal (étape 1).");
            return;
        }
        
        // Recharger l'entité depuis la base
        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity == null) {
            showError("L'entité n'a pas été trouvée dans la base de données.");
            return;
        }
        
        // Vérifier si la langue est déjà utilisée
        if (refreshedEntity.getLabels() != null) {
            boolean langueAlreadyUsed = refreshedEntity.getLabels().stream()
                .anyMatch(label -> label.getLangue() != null && 
                    label.getLangue().getCode() != null &&
                    label.getLangue().getCode().equals(newLabelLangueCode));
            
            if (langueAlreadyUsed) {
                showWarning("Cette langue est déjà utilisée pour un autre label.");
                return;
            }
        }
        
        // Créer et sauvegarder le label
        Langue langue = langueRepository.findByCode(newLabelLangueCode);
        if (langue == null) {
            showError("La langue sélectionnée n'a pas été trouvée.");
            return;
        }
        
        Label newLabel = new Label();
        newLabel.setNom(newLabelValue.trim());
        newLabel.setEntity(refreshedEntity);
        newLabel.setLangue(langue);
        
        if (refreshedEntity.getLabels() == null) {
            refreshedEntity.setLabels(new ArrayList<>());
        }
        refreshedEntity.getLabels().add(newLabel);
        
        entityRepository.save(refreshedEntity);
        
        // Mettre à jour la liste locale
        if (candidatLabels == null) {
            candidatLabels = new ArrayList<>();
        }
        CategoryLabelItem newItem = new CategoryLabelItem(
            newLabelValue.trim(), 
            newLabelLangueCode, 
            langue);
        candidatLabels.add(newItem);
        
        // Réinitialiser les champs
        newLabelValue = null;
        newLabelLangueCode = null;
        
        showSuccess("Le label a été ajouté avec succès.");
    }

    /**
     * Supprime un label de la liste et de la base de données
     */
    public void removeCandidatLabel(CategoryLabelItem labelItem) {
        if (currentEntity == null || currentEntity.getId() == null) {
            showError("L'entité n'a pas encore été créée.");
            return;
        }
        
        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity == null || refreshedEntity.getLabels() == null) {
            return;
        }
        
        // Trouver et supprimer le label correspondant
        Label labelToRemove = refreshedEntity.getLabels().stream()
            .filter(label -> label.getLangue() != null && 
                label.getLangue().getCode() != null &&
                label.getLangue().getCode().equals(labelItem.getLangueCode()) &&
                label.getNom() != null &&
                label.getNom().equals(labelItem.getNom()))
            .findFirst()
            .orElse(null);
        
        if (labelToRemove != null) {
            refreshedEntity.getLabels().remove(labelToRemove);
            entityRepository.save(refreshedEntity);
        }
        
        // Supprimer de la liste locale
        if (candidatLabels != null) {
            candidatLabels.remove(labelItem);
        }
        
        showSuccess("Le label a été supprimé avec succès.");
    }

    /**
     * Ajoute une nouvelle description depuis les champs de saisie et la sauvegarde dans la base de données
     */
    public void addCandidatDescriptionFromInput() {
        if (currentEntity == null || currentEntity.getId() == null) {
            showError("L'entité n'a pas encore été créée. Veuillez d'abord compléter les étapes précédentes.");
            return;
        }
        
        if (newDescriptionValue == null || newDescriptionValue.trim().isEmpty()) {
            showWarning("La description est requise.");
            return;
        }
        
        if (newDescriptionLangueCode == null || newDescriptionLangueCode.trim().isEmpty()) {
            showWarning("La langue est requise.");
            return;
        }
        
        // Recharger l'entité depuis la base
        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity == null) {
            showError("L'entité n'a pas été trouvée dans la base de données.");
            return;
        }
        
        // Vérifier si la langue est déjà utilisée
        if (refreshedEntity.getDescriptions() != null) {
            boolean langueAlreadyUsed = refreshedEntity.getDescriptions().stream()
                .anyMatch(desc -> desc.getLangue() != null && 
                    desc.getLangue().getCode() != null &&
                    desc.getLangue().getCode().equals(newDescriptionLangueCode));
            
            if (langueAlreadyUsed) {
                showWarning("Cette langue est déjà utilisée pour une autre description.");
                return;
            }
        }
        
        // Créer et sauvegarder la description
        Langue langue = langueRepository.findByCode(newDescriptionLangueCode);
        if (langue == null) {
            showError("La langue sélectionnée n'a pas été trouvée.");
            return;
        }
        
        Description newDescription = new Description();
        newDescription.setValeur(newDescriptionValue.trim());
        newDescription.setEntity(refreshedEntity);
        newDescription.setLangue(langue);
        
        if (refreshedEntity.getDescriptions() == null) {
            refreshedEntity.setDescriptions(new ArrayList<>());
        }
        refreshedEntity.getDescriptions().add(newDescription);
        
        entityRepository.save(refreshedEntity);
        
        // Mettre à jour la liste locale
        if (descriptions == null) {
            descriptions = new ArrayList<>();
        }
        CategoryDescriptionItem newItem = new CategoryDescriptionItem(
            newDescriptionValue.trim(), 
            newDescriptionLangueCode, 
            langue);
        descriptions.add(newItem);
        
        // Réinitialiser les champs
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        
        showSuccess("La description a été ajoutée avec succès.");
    }

    /**
     * Supprime une description de la liste et de la base de données
     */
    public void removeCandidatDescription(CategoryDescriptionItem descriptionItem) {
        if (currentEntity == null || currentEntity.getId() == null) {
            showError("L'entité n'a pas encore été créée.");
            return;
        }
        
        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity == null || refreshedEntity.getDescriptions() == null) {
            return;
        }
        
        // Trouver et supprimer la description correspondante
        Description descriptionToRemove = refreshedEntity.getDescriptions().stream()
            .filter(desc -> desc.getLangue() != null && 
                desc.getLangue().getCode() != null &&
                desc.getLangue().getCode().equals(descriptionItem.getLangueCode()) &&
                desc.getValeur() != null &&
                desc.getValeur().equals(descriptionItem.getValeur()))
            .findFirst()
            .orElse(null);
        
        if (descriptionToRemove != null) {
            refreshedEntity.getDescriptions().remove(descriptionToRemove);
            entityRepository.save(refreshedEntity);
        }
        
        // Supprimer de la liste locale
        if (descriptions != null) {
            descriptions.remove(descriptionItem);
        }
        
        showSuccess("La description a été supprimée avec succès.");
    }

    /**
     * Charge les données existantes depuis currentEntity pour l'étape 3
     */
    public void loadExistingStep3Data() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity == null) {
            return;
        }
        
        // Charger les labels existants (excluant le label principal de l'étape 1)
        candidatLabels = new ArrayList<>();
        if (refreshedEntity.getLabels() != null) {
            for (Label label : refreshedEntity.getLabels()) {
                if (label.getLangue() != null && 
                    selectedLangueCode != null &&
                    label.getLangue().getCode() != null &&
                    !label.getLangue().getCode().equals(selectedLangueCode)) {
                    CategoryLabelItem item = new CategoryLabelItem(
                        label.getNom(),
                        label.getLangue().getCode(),
                        label.getLangue()
                    );
                    candidatLabels.add(item);
                }
            }
        }
        
        // Charger les descriptions existantes
        descriptions = new ArrayList<>();
        if (refreshedEntity.getDescriptions() != null) {
            for (Description desc : refreshedEntity.getDescriptions()) {
                CategoryDescriptionItem item = new CategoryDescriptionItem(
                    desc.getValeur(),
                    desc.getLangue() != null ? desc.getLangue().getCode() : null,
                    desc.getLangue()
                );
                descriptions.add(item);
            }
        }
        
        // Charger les champs de texte
        candidatCommentaire = refreshedEntity.getCommentaire();
        candidatBibliographie = refreshedEntity.getBibliographie();
        
        // Charger les références bibliographiques
        if (refreshedEntity.getRereferenceBibliographique() != null && 
            !refreshedEntity.getRereferenceBibliographique().isEmpty()) {
            String[] refs = refreshedEntity.getRereferenceBibliographique().split("; ");
            referencesBibliographiques = new ArrayList<>(Arrays.asList(refs));
        } else {
            referencesBibliographiques = new ArrayList<>();
        }
        
        // Charger les ateliers
        if (refreshedEntity.getAteliers() != null && 
            !refreshedEntity.getAteliers().isEmpty()) {
            String[] ateliersArray = refreshedEntity.getAteliers().split("; ");
            ateliers = new ArrayList<>(Arrays.asList(ateliersArray));
        } else {
            ateliers = new ArrayList<>();
        }
        
        // Charger les champs spécifiques selon le type d'entité
        if (refreshedEntity.getEntityType() != null) {
            String typeCode = refreshedEntity.getEntityType().getCode();
            
            if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(typeCode)) {
                collectionPublique = refreshedEntity.getPublique();
            } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(typeCode)) {
                typeDescription = refreshedEntity.getCommentaire();
            }
        }
        
        // Charger les champs spécifiques au groupe
        groupTpq = refreshedEntity.getTpq();
        groupTaq = refreshedEntity.getTaq();
        if (refreshedEntity.getPeriode() != null) {
            groupPeriode = refreshedEntity.getPeriode().getValeur();
        }
    }

    /**
     * Réinitialise tous les champs du formulaire
     */
    public void resetFormData() {
        candidatLabels = new ArrayList<>();
        newLabelValue = null;
        newLabelLangueCode = null;
        descriptions = new ArrayList<>();
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        candidatCommentaire = null;
        candidatBibliographie = null;
        referencesBibliographiques = new ArrayList<>();
        ateliers = new ArrayList<>();
        collectionDescription = null;
        collectionPublique = true;
        typeDescription = null;
        serieDescription = null;
        groupDescription = null;
        categoryDescription = null;
        groupPeriode = null;
        groupTpq = null;
        groupTaq = null;
        imagePrincipaleUrl = null;
    }

    /**
     * Sauvegarde automatiquement le champ commentaire dans la base de données
     */
    public void saveCommentaire() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        try {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null) {
                refreshedEntity.setCommentaire(candidatCommentaire);
                entityRepository.save(refreshedEntity);
                log.debug("Commentaire sauvegardé pour l'entité ID: {}", refreshedEntity.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du commentaire", e);
        }
    }

    /**
     * Sauvegarde automatiquement le champ bibliographie dans la base de données
     */
    public void saveBibliographie() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        try {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null) {
                refreshedEntity.setBibliographie(candidatBibliographie);
                entityRepository.save(refreshedEntity);
                log.debug("Bibliographie sauvegardée pour l'entité ID: {}", refreshedEntity.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde de la bibliographie", e);
        }
    }

    /**
     * Sauvegarde automatiquement les références bibliographiques dans la base de données
     */
    public void saveReferencesBibliographiques() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        try {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null) {
                String refs = null;
                if (referencesBibliographiques != null && !referencesBibliographiques.isEmpty()) {
                    refs = String.join("; ", referencesBibliographiques);
                }
                refreshedEntity.setRereferenceBibliographique(refs);
                entityRepository.save(refreshedEntity);
                log.debug("Références bibliographiques sauvegardées pour l'entité ID: {}", refreshedEntity.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde des références bibliographiques", e);
        }
    }

    /**
     * Sauvegarde automatiquement les ateliers dans la base de données
     */
    public void saveAteliers() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        try {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null) {
                String ateliersStr = null;
                if (ateliers != null && !ateliers.isEmpty()) {
                    ateliersStr = String.join("; ", ateliers);
                }
                refreshedEntity.setAteliers(ateliersStr);
                entityRepository.save(refreshedEntity);
                log.debug("Ateliers sauvegardés pour l'entité ID: {}", refreshedEntity.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde des ateliers", e);
        }
    }

    /**
     * Sauvegarde automatiquement la description du type dans la base de données
     */
    public void saveTypeDescription() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        try {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null && EntityConstants.ENTITY_TYPE_TYPE.equals(
                refreshedEntity.getEntityType() != null ? refreshedEntity.getEntityType().getCode() : null)) {
                refreshedEntity.setCommentaire(typeDescription);
                entityRepository.save(refreshedEntity);
                log.debug("Description du type sauvegardée pour l'entité ID: {}", refreshedEntity.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde de la description du type", e);
        }
    }

    /**
     * Sauvegarde automatiquement la description de la collection dans la base de données
     */
    public void saveCollectionDescription() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        try {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null && EntityConstants.ENTITY_TYPE_COLLECTION.equals(
                refreshedEntity.getEntityType() != null ? refreshedEntity.getEntityType().getCode() : null)) {
                refreshedEntity.setCommentaire(collectionDescription);
                entityRepository.save(refreshedEntity);
                log.debug("Description de la collection sauvegardée pour l'entité ID: {}", refreshedEntity.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde de la description de la collection", e);
        }
    }

    /**
     * Sauvegarde automatiquement le statut public de la collection dans la base de données
     */
    public void saveCollectionPublique() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        try {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null && EntityConstants.ENTITY_TYPE_COLLECTION.equals(
                refreshedEntity.getEntityType() != null ? refreshedEntity.getEntityType().getCode() : null)) {
                refreshedEntity.setPublique(collectionPublique);
                entityRepository.save(refreshedEntity);
                log.debug("Statut public de la collection sauvegardé pour l'entité ID: {}", refreshedEntity.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du statut public de la collection", e);
        }
    }

    /**
     * Sauvegarde automatiquement les champs du groupe (TPQ, TAQ) dans la base de données
     */
    public void saveGroupFields() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        try {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null && EntityConstants.ENTITY_TYPE_GROUP.equals(
                refreshedEntity.getEntityType() != null ? refreshedEntity.getEntityType().getCode() : null)) {
                refreshedEntity.setTpq(groupTpq);
                refreshedEntity.setTaq(groupTaq);
                entityRepository.save(refreshedEntity);
                log.debug("Champs du groupe sauvegardés pour l'entité ID: {}", refreshedEntity.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde des champs du groupe", e);
        }
    }

    /**
     * Vérifie si une langue est déjà utilisée dans les labels
     */
    public boolean isLangueAlreadyUsedIncandidatLabels(String langueCode, CategoryLabelItem currentItem) {
        if (langueCode == null || langueCode.isEmpty()) {
            return false;
        }
        
        // Vérifier dans la liste locale
        if (candidatLabels != null) {
            boolean foundInList = candidatLabels.stream()
                .filter(item -> item != currentItem && item.getLangueCode() != null)
                .anyMatch(item -> item.getLangueCode().equals(langueCode));
            if (foundInList) {
                return true;
            }
        }
        
        // Vérifier dans la base de données si currentEntity existe
        if (currentEntity != null && currentEntity.getId() != null) {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null && refreshedEntity.getLabels() != null) {
                boolean foundInDb = refreshedEntity.getLabels().stream()
                    .filter(label -> label.getLangue() != null && 
                        label.getLangue().getCode() != null &&
                        label.getLangue().getCode().equals(langueCode))
                    .anyMatch(label -> {
                        if (selectedLangueCode != null && selectedLangueCode.equals(langueCode)) {
                            return false;
                        }
                        return true;
                    });
                if (foundInDb) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Vérifie si une langue est déjà utilisée dans les descriptions
     */
    public boolean isLangueAlreadyUsedIndescriptions(String langueCode, CategoryDescriptionItem currentItem) {
        if (langueCode == null || langueCode.isEmpty()) {
            return false;
        }
        
        // Vérifier dans la liste locale
        if (descriptions != null) {
            boolean foundInList = descriptions.stream()
                .filter(item -> item != currentItem && item.getLangueCode() != null)
                .anyMatch(item -> item.getLangueCode().equals(langueCode));
            if (foundInList) {
                return true;
            }
        }
        
        // Vérifier dans la base de données si currentEntity existe
        if (currentEntity != null && currentEntity.getId() != null) {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null && refreshedEntity.getDescriptions() != null) {
                boolean foundInDb = refreshedEntity.getDescriptions().stream()
                    .filter(desc -> desc.getLangue() != null && 
                        desc.getLangue().getCode() != null &&
                        desc.getLangue().getCode().equals(langueCode))
                    .findAny()
                    .isPresent();
                if (foundInDb) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Obtient les langues disponibles pour un nouveau label
     */
    public List<Langue> getAvailableLanguagesForNewLabel(List<Langue> availableLanguages) {
        if (availableLanguages == null) {
            return new ArrayList<>();
        }
        return availableLanguages.stream()
            .filter(langue -> {
                // Exclure la langue de l'étape 1
                if (selectedLangueCode != null && selectedLangueCode.equals(langue.getCode())) {
                    return false;
                }
                // Exclure les langues déjà utilisées
                return !isLangueAlreadyUsedIncandidatLabels(langue.getCode(), null);
            })
            .collect(Collectors.toList());
    }

    /**
     * Obtient les langues disponibles pour une nouvelle description
     */
    public List<Langue> getAvailableLanguagesForNewDescription(List<Langue> availableLanguages) {
        if (availableLanguages == null) {
            return new ArrayList<>();
        }
        return availableLanguages.stream()
            .filter(langue -> !isLangueAlreadyUsedIndescriptions(langue.getCode(), null))
            .collect(Collectors.toList());
    }

    // Méthodes utilitaires pour les messages
    private void showError(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", message));
        }
    }

    private void showWarning(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", message));
        }
    }

    private void showSuccess(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", message));
        }
    }
}
