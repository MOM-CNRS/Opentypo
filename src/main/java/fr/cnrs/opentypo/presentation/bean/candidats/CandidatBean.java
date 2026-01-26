package fr.cnrs.opentypo.presentation.bean.candidats;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.presentation.bean.OpenThesoDialogBean;
import fr.cnrs.opentypo.presentation.bean.candidats.converter.CandidatConverter;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatEntityService;
import jakarta.annotation.PostConstruct;
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
 * Bean principal pour le module candidat
 * Agit comme un facade/coordinateur qui délègue aux beans spécialisés
 * Maintient la compatibilité avec les fichiers XHTML existants
 */
@Named("candidatBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class CandidatBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // Beans spécialisés injectés
    @Inject
    private CandidatListBean candidatListBean;

    @Inject
    private CandidatWizardBean candidatWizardBean;

    @Inject
    private CandidatFormDataBean candidatFormDataBean;

    @Inject
    private CandidatReferenceTreeBean candidatReferenceTreeBean;

    @Inject
    private CandidatSelectionDataBean candidatSelectionDataBean;

    @Inject
    private CandidatEntityService candidatEntityService;

    @Inject
    private CandidatConverter candidatConverter;

    @Inject
    private OpenThesoDialogBean openThesoDialogBean;

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    // Champs pour compatibilité avec les XHTML
    private Candidat candidatSelectionne;
    private String candidatProduction;

    @PostConstruct
    public void init() {
        // Les beans spécialisés s'initialisent eux-mêmes
        log.info("CandidatBean refactorisé initialisé");
    }

    // ========== DÉLÉGATION AUX BEANS SPÉCIALISÉS ==========

    // Délégation à CandidatListBean
    public void loadCandidatsPage() {
        candidatListBean.loadCandidatsPage();
    }

    public java.util.List<Candidat> getCandidatsEnCours() {
        return candidatListBean.getCandidatsEnCours();
    }

    public java.util.List<Candidat> getCandidatsValides() {
        return candidatListBean.getCandidatsValides();
    }

    public java.util.List<Candidat> getCandidatsRefuses() {
        return candidatListBean.getCandidatsRefuses();
    }

    public void prepareValidateCandidat(Candidat candidat) {
        candidatListBean.prepareValidateCandidat(candidat);
    }

    public void prepareDeleteCandidat(Candidat candidat) {
        candidatListBean.prepareDeleteCandidat(candidat);
    }

    public void validerCandidat(Candidat candidat) {
        candidatListBean.validerCandidat(candidat);
    }

    public void validerCandidatConfirm() {
        if (candidatListBean.getCandidatAValider() != null) {
            validerCandidat(candidatListBean.getCandidatAValider());
            candidatListBean.setCandidatAValider(null);
        }
    }

    public void refuserCandidat(Candidat candidat) {
        candidatListBean.refuserCandidat(candidat);
    }

    public void supprimerCandidat(Candidat candidat) {
        candidatListBean.supprimerCandidat(candidat);
    }

    public void supprimerCandidatConfirm() {
        if (candidatListBean.getCandidatASupprimer() != null) {
            supprimerCandidat(candidatListBean.getCandidatASupprimer());
            candidatListBean.setCandidatASupprimer(null);
        }
    }

    public String visualiserCandidat(Candidat candidat) {
        candidatSelectionne = candidat;
        return candidatListBean.visualiserCandidat(candidat);
    }

    public int getActiveTabIndex() {
        return candidatListBean.getActiveTabIndex();
    }

    public void setActiveTabIndex(int activeTabIndex) {
        candidatListBean.setActiveTabIndex(activeTabIndex);
    }

    public Candidat getCandidatAValider() {
        return candidatListBean.getCandidatAValider();
    }

    public void setCandidatAValider(Candidat candidatAValider) {
        candidatListBean.setCandidatAValider(candidatAValider);
    }

    public Candidat getCandidatASupprimer() {
        return candidatListBean.getCandidatASupprimer();
    }

    public void setCandidatASupprimer(Candidat candidatASupprimer) {
        candidatListBean.setCandidatASupprimer(candidatASupprimer);
    }

    // Délégation à CandidatWizardBean
    public String nextStep() {
        return candidatWizardBean.nextStep();
    }

    public String previousStep() {
        return candidatWizardBean.previousStep();
    }

    public boolean isStep1() {
        return candidatWizardBean.isStep1();
    }

    public boolean isStep2() {
        return candidatWizardBean.isStep2();
    }

    public boolean isStep3() {
        return candidatWizardBean.isStep3();
    }

    public String terminerCandidat() {
        return candidatWizardBean.terminerCandidat();
    }

    public void abandonnerProposition() {
        if (candidatWizardBean.getCurrentEntity() != null) {
            candidatEntityService.deleteEntityWithRelations(candidatWizardBean.getCurrentEntity());
        }
        candidatWizardBean.resetWizardForm();
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "La proposition a été abandonnée et supprimée."));
    }

    public EntityType getSelectedEntityType() {
        return candidatWizardBean.getSelectedEntityType();
    }

    public Entity getCurrentEntity() {
        return candidatWizardBean.getCurrentEntity();
    }

    // Délégation à CandidatSelectionDataBean
    public void loadAvailableEntityTypes() {
        candidatSelectionDataBean.loadAvailableEntityTypes();
    }

    public void loadAvailableLanguages() {
        candidatSelectionDataBean.loadAvailableLanguages();
    }

    public java.util.List<EntityType> getAvailableEntityTypes() {
        return candidatSelectionDataBean.getAvailableEntityTypes();
    }

    public java.util.List<fr.cnrs.opentypo.domain.entity.Langue> getAvailableLanguages() {
        return candidatSelectionDataBean.getAvailableLanguages();
    }

    public java.util.List<Entity> getAvailableCollections() {
        return candidatSelectionDataBean.getAvailableCollections();
    }

    public String getEntityTypeName(EntityType entityType) {
        return candidatSelectionDataBean.getEntityTypeName(entityType);
    }

    public String getSelectedLangueName() {
        return candidatSelectionDataBean.getSelectedLangueName(candidatWizardBean.getSelectedLangueCode());
    }

    public String getCollectionLabel(Entity collection) {
        return candidatSelectionDataBean.getCollectionLabel(collection);
    }

    // Délégation à CandidatReferenceTreeBean
    public void loadReferencesForCollection() {
        candidatReferenceTreeBean.loadReferencesForCollection();
    }

    public void loadDirectEntitiesForCollection() {
        candidatReferenceTreeBean.loadDirectEntitiesForCollection();
    }

    public void onDirectEntityChange() {
        candidatReferenceTreeBean.onDirectEntityChange();
    }

    public String getDirectEntityLabel(Entity entity) {
        return candidatReferenceTreeBean.getDirectEntityLabel(entity);
    }

    public boolean isCollectionSelected() {
        return candidatReferenceTreeBean.isCollectionSelected();
    }

    public TreeNode getReferenceTreeRoot() {
        return candidatReferenceTreeBean.getReferenceTreeRoot();
    }

    public TreeNode getSelectedTreeNode() {
        return candidatReferenceTreeBean.getSelectedTreeNode();
    }

    public void setSelectedTreeNode(TreeNode selectedTreeNode) {
        candidatReferenceTreeBean.setSelectedTreeNode(selectedTreeNode);
    }

    public java.util.List<Entity> getAvailableDirectEntities() {
        return candidatReferenceTreeBean.getAvailableDirectEntities();
    }

    // Délégation à CandidatFormDataBean
    public void addLabelFromInput() {
        candidatFormDataBean.addLabelFromInput();
    }

    public void removeCandidatLabel(fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryLabelItem labelItem) {
        candidatFormDataBean.removeCandidatLabel(labelItem);
    }

    public void addCandidatDescriptionFromInput() {
        candidatFormDataBean.addCandidatDescriptionFromInput();
    }

    public void removeCandidatDescription(fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryDescriptionItem descriptionItem) {
        candidatFormDataBean.removeCandidatDescription(descriptionItem);
    }

    public boolean isLangueAlreadyUsedIncandidatLabels(String langueCode, fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryLabelItem currentItem) {
        return candidatFormDataBean.isLangueAlreadyUsedIncandidatLabels(langueCode, currentItem);
    }

    public boolean isLangueAlreadyUsedIndescriptions(String langueCode, fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryDescriptionItem currentItem) {
        return candidatFormDataBean.isLangueAlreadyUsedIndescriptions(langueCode, currentItem);
    }

    public java.util.List<fr.cnrs.opentypo.domain.entity.Langue> getAvailableLanguagesForNewLabel() {
        return candidatFormDataBean.getAvailableLanguagesForNewLabel(candidatSelectionDataBean.getAvailableLanguages());
    }

    public java.util.List<fr.cnrs.opentypo.domain.entity.Langue> getAvailableLanguagesForNewDescription() {
        return candidatFormDataBean.getAvailableLanguagesForNewDescription(candidatSelectionDataBean.getAvailableLanguages());
    }

    public void saveCommentaire() {
        candidatFormDataBean.saveCommentaire();
    }

    public void saveBibliographie() {
        candidatFormDataBean.saveBibliographie();
    }

    public void saveReferencesBibliographiques() {
        candidatFormDataBean.saveReferencesBibliographiques();
    }

    public void saveTypeDescription() {
        candidatFormDataBean.saveTypeDescription();
    }

    public void saveCollectionDescription() {
        candidatFormDataBean.saveCollectionDescription();
    }

    public void saveCollectionPublique() {
        candidatFormDataBean.saveCollectionPublique();
    }

    public void saveGroupFields() {
        candidatFormDataBean.saveGroupFields();
    }

    // ========== PROPRIÉTÉS POUR COMPATIBILITÉ ==========
    // Ces propriétés délèguent aux beans spécialisés pour maintenir la compatibilité

    public Long getSelectedEntityTypeId() {
        return candidatWizardBean.getSelectedEntityTypeId();
    }

    public void setSelectedEntityTypeId(Long selectedEntityTypeId) {
        candidatWizardBean.setSelectedEntityTypeId(selectedEntityTypeId);
    }

    public String getEntityCode() {
        return candidatWizardBean.getEntityCode();
    }

    public void setEntityCode(String entityCode) {
        candidatWizardBean.setEntityCode(entityCode);
    }

    public String getEntityLabel() {
        return candidatWizardBean.getEntityLabel();
    }

    public void setEntityLabel(String entityLabel) {
        candidatWizardBean.setEntityLabel(entityLabel);
    }

    public String getSelectedLangueCode() {
        return candidatWizardBean.getSelectedLangueCode();
    }

    public void setSelectedLangueCode(String selectedLangueCode) {
        candidatWizardBean.setSelectedLangueCode(selectedLangueCode);
        candidatFormDataBean.setSelectedLangueCode(selectedLangueCode);
        candidatReferenceTreeBean.setSelectedLangueCode(selectedLangueCode);
    }

    public Long getSelectedCollectionId() {
        return candidatReferenceTreeBean.getSelectedCollectionId();
    }

    public void setSelectedCollectionId(Long selectedCollectionId) {
        candidatReferenceTreeBean.setSelectedCollectionId(selectedCollectionId);
        candidatWizardBean.setSelectedCollectionId(selectedCollectionId);
    }

    public Long getSelectedDirectEntityId() {
        return candidatReferenceTreeBean.getSelectedDirectEntityId();
    }

    public void setSelectedDirectEntityId(Long selectedDirectEntityId) {
        candidatReferenceTreeBean.setSelectedDirectEntityId(selectedDirectEntityId);
    }

    public Entity getSelectedParentEntity() {
        return candidatWizardBean.getSelectedParentEntity();
    }

    public void setSelectedParentEntity(Entity selectedParentEntity) {
        candidatWizardBean.setSelectedParentEntity(selectedParentEntity);
    }

    public int getCurrentStep() {
        return candidatWizardBean.getCurrentStep();
    }

    public void setCurrentStep(int currentStep) {
        candidatWizardBean.setCurrentStep(currentStep);
    }

    // Propriétés pour les données du formulaire
    public java.util.List<fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryLabelItem> getCandidatLabels() {
        return candidatFormDataBean.getCandidatLabels();
    }

    public String getNewLabelValue() {
        return candidatFormDataBean.getNewLabelValue();
    }

    public void setNewLabelValue(String newLabelValue) {
        candidatFormDataBean.setNewLabelValue(newLabelValue);
    }

    public String getNewLabelLangueCode() {
        return candidatFormDataBean.getNewLabelLangueCode();
    }

    public void setNewLabelLangueCode(String newLabelLangueCode) {
        candidatFormDataBean.setNewLabelLangueCode(newLabelLangueCode);
    }

    public java.util.List<fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryDescriptionItem> getDescriptions() {
        return candidatFormDataBean.getDescriptions();
    }

    public String getNewDescriptionValue() {
        return candidatFormDataBean.getNewDescriptionValue();
    }

    public void setNewDescriptionValue(String newDescriptionValue) {
        candidatFormDataBean.setNewDescriptionValue(newDescriptionValue);
    }

    public String getNewDescriptionLangueCode() {
        return candidatFormDataBean.getNewDescriptionLangueCode();
    }

    public void setNewDescriptionLangueCode(String newDescriptionLangueCode) {
        candidatFormDataBean.setNewDescriptionLangueCode(newDescriptionLangueCode);
    }

    public String getCandidatCommentaire() {
        return candidatFormDataBean.getCandidatCommentaire();
    }

    public void setCandidatCommentaire(String candidatCommentaire) {
        candidatFormDataBean.setCandidatCommentaire(candidatCommentaire);
    }

    public String getCandidatBibliographie() {
        return candidatFormDataBean.getCandidatBibliographie();
    }

    public void setCandidatBibliographie(String candidatBibliographie) {
        candidatFormDataBean.setCandidatBibliographie(candidatBibliographie);
    }

    public java.util.List<String> getReferencesBibliographiques() {
        return candidatFormDataBean.getReferencesBibliographiques();
    }

    public void setReferencesBibliographiques(java.util.List<String> referencesBibliographiques) {
        candidatFormDataBean.setReferencesBibliographiques(referencesBibliographiques);
    }

    public java.util.List<String> getAteliers() {
        return candidatFormDataBean.getAteliers();
    }

    public void setAteliers(java.util.List<String> ateliers) {
        candidatFormDataBean.setAteliers(ateliers);
    }

    public void saveAteliers() {
        candidatFormDataBean.saveAteliers();
    }

    public String getTypeDescription() {
        return candidatFormDataBean.getTypeDescription();
    }

    public void setTypeDescription(String typeDescription) {
        candidatFormDataBean.setTypeDescription(typeDescription);
    }

    public String getCollectionDescription() {
        return candidatFormDataBean.getCollectionDescription();
    }

    public void setCollectionDescription(String collectionDescription) {
        candidatFormDataBean.setCollectionDescription(collectionDescription);
    }

    public Boolean getCollectionPublique() {
        return candidatFormDataBean.getCollectionPublique();
    }

    public void setCollectionPublique(Boolean collectionPublique) {
        candidatFormDataBean.setCollectionPublique(collectionPublique);
    }

    public String getGroupPeriode() {
        return candidatFormDataBean.getGroupPeriode();
    }

    public void setGroupPeriode(String groupPeriode) {
        candidatFormDataBean.setGroupPeriode(groupPeriode);
    }

    public Integer getGroupTpq() {
        return candidatFormDataBean.getGroupTpq();
    }

    public void setGroupTpq(Integer groupTpq) {
        candidatFormDataBean.setGroupTpq(groupTpq);
    }

    public Integer getGroupTaq() {
        return candidatFormDataBean.getGroupTaq();
    }

    public void setGroupTaq(Integer groupTaq) {
        candidatFormDataBean.setGroupTaq(groupTaq);
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Met à jour le champ production depuis OpenTheso après validation
     */
    public void updateProductionFromOpenTheso() {
        Entity currentEntity = candidatWizardBean.getCurrentEntity();
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        try {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null && refreshedEntity.getProduction() != null) {
                candidatProduction = refreshedEntity.getProduction().getValeur();
                candidatWizardBean.setCurrentEntity(refreshedEntity);
                log.info("Champ production mis à jour pour l'entité ID={}: {}", currentEntity.getId(), candidatProduction);
            } else if (openThesoDialogBean.getCreatedReference() != null) {
                candidatProduction = openThesoDialogBean.getCreatedReference().getValeur();
            } else if (openThesoDialogBean.getSelectedConcept() != null) {
                candidatProduction = openThesoDialogBean.getSelectedConcept().getSelectedTerm();
            }
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du champ production depuis OpenTheso", e);
        }
    }

    /**
     * Prépare la suppression de la production (affiche le dialog de confirmation)
     */
    public void prepareDeleteProduction() {
        // Le dialog sera affiché via JavaScript
    }

    /**
     * Supprime la production de l'entité et de la base de données
     */
    public void deleteProduction() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        try {
            Entity currentEntity = candidatWizardBean.getCurrentEntity();
            if (currentEntity == null || currentEntity.getId() == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "L'entité n'a pas été trouvée."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            // Recharger l'entité depuis la base
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "L'entité n'a pas été trouvée dans la base de données."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            // Récupérer la référence production avant de la supprimer
            ReferenceOpentheso productionRef = refreshedEntity.getProduction();
            
            if (productionRef != null) {
                // Retirer la référence de l'entité
                refreshedEntity.setProduction(null);
                entityRepository.save(refreshedEntity);
                
                // Supprimer la référence de la base de données
                referenceOpenthesoRepository.delete(productionRef);
                
                // Mettre à jour l'entité dans le wizard
                candidatWizardBean.setCurrentEntity(refreshedEntity);
                candidatProduction = null;
                
                log.info("Production supprimée pour l'entité ID={}, référence ID={} supprimée", 
                    refreshedEntity.getId(), productionRef.getId());
                
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "La production a été supprimée avec succès."));
            } else {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Aucune production à supprimer."));
            }
            
            PrimeFaces.current().ajax().update(":createCandidatForm:productionGroup :growl");
            
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la production", e);
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors de la suppression : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    /**
     * Vérifie si une production existe pour l'entité courante
     */
    public boolean hasProduction() {
        Entity currentEntity = candidatWizardBean.getCurrentEntity();
        if (currentEntity == null || currentEntity.getId() == null) {
            return false;
        }
        
        try {
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            return refreshedEntity != null && refreshedEntity.getProduction() != null;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de la production", e);
            return false;
        }
    }

    /**
     * Récupère le code de l'entité depuis le candidat sélectionné
     */
    public String getEntityCodeFromCandidat() {
        if (candidatSelectionne == null || candidatSelectionne.getId() == null) {
            return "Non sélectionné";
        }
        
        try {
            java.util.Optional<Entity> entityOpt = entityRepository.findById(candidatSelectionne.getId());
            if (entityOpt.isPresent()) {
                return entityOpt.get().getCode();
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du code de l'entité", e);
        }
        
        return "Non sélectionné";
    }

    /**
     * Récupère le code de l'entité parente depuis le candidat sélectionné
     */
    public String getParentCodeFromCandidat() {
        if (candidatSelectionne == null || candidatSelectionne.getId() == null) {
            return "Non sélectionné";
        }
        
        try {
            java.util.Optional<Entity> entityOpt = entityRepository.findById(candidatSelectionne.getId());
            if (entityOpt.isPresent()) {
                Entity entity = entityOpt.get();
                java.util.List<Entity> parents = entityRelationRepository.findParentsByChild(entity);
                if (parents != null && !parents.isEmpty()) {
                    for (Entity parent : parents) {
                        if (parent.getEntityType() != null &&
                            !EntityConstants.ENTITY_TYPE_COLLECTION.equals(parent.getEntityType().getCode())) {
                            return parent.getCode();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du parent depuis le candidat", e);
        }
        
        return "Non sélectionné";
    }

    /**
     * Vérifie si un objet est une instance de Entity
     */
    public boolean isEntity(Object obj) {
        return obj != null && obj instanceof Entity;
    }

    /**
     * Vérifie si le nœud de l'arbre contient une Entity comme data
     */
    public boolean isNodeEntity(Object node) {
        if (node == null) {
            return false;
        }
        if (node instanceof Entity) {
            return true;
        }
        if (node instanceof TreeNode) {
            Object data = ((TreeNode) node).getData();
            return data != null && data instanceof Entity;
        }
        return false;
    }

    /**
     * Récupère l'Entity depuis un nœud de l'arbre
     */
    public Entity getEntityFromNode(Object node) {
        if (node == null) {
            return null;
        }
        if (node instanceof Entity) {
            return (Entity) node;
        }
        if (node instanceof TreeNode) {
            Object data = ((TreeNode) node).getData();
            if (data instanceof Entity) {
                return (Entity) data;
            }
        }
        return null;
    }

    /**
     * Récupère la valeur d'affichage d'un nœud
     */
    public String getNodeDisplayValue(Object node) {
        Entity entity = getEntityFromNode(node);
        if (entity != null) {
            if (node instanceof TreeNode) {
                TreeNode treeNode = (TreeNode) node;
                if (treeNode.getParent() == null) {
                    return entity.getCode() != null ? entity.getCode() : entity.getNom();
                }
            }
            return entity.getCode();
        }
        if (node instanceof TreeNode) {
            Object data = ((TreeNode) node).getData();
            return data != null ? data.toString() : node.toString();
        }
        return node != null ? node.toString() : "";
    }

    /**
     * Vérifie si un nœud est le nœud racine
     */
    public boolean isRootNode(Object node) {
        if (node instanceof TreeNode) {
            return ((TreeNode) node).getParent() == null;
        }
        return false;
    }

    /**
     * Réinitialise le formulaire du wizard
     */
    public void resetWizardForm() {
        candidatWizardBean.resetWizardForm();
    }

    /**
     * Initialise un nouveau candidat
     */
    public void initNouveauCandidat() {
        resetWizardForm();
    }
}
