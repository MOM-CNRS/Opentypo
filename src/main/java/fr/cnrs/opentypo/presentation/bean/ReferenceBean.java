package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.util.EntityUtils;
import fr.cnrs.opentypo.presentation.bean.util.EntityValidator;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Getter
@Setter
@SessionScoped
@Named(value = "referenceBean")
@Slf4j
public class ReferenceBean implements Serializable {

    private static final String reference_FORM = ":referenceForm";

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private LoginBean loginBean;

    @Autowired
    private TreeBean treeBean;
    
    @Autowired
    private ApplicationBean applicationBean;
    
    @Autowired
    private SearchBean searchBean;

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Autowired
    private LangueRepository langueRepository;


    // Propriétés pour le formulaire de création de référentiel
    private String referenceCode;
    private String referenceLabel;
    private String referenceDescription;
    private String periodeId; // ID de la période (referenceOpentheso)
    private String referenceBibliographique;
    private String categorieIds; // IDs des catégories (Entity de type Catégorie)

    // État d'édition pour le référentiel
    private boolean editingReference = false;
    private String editingReferenceCode;
    private String editingReferenceDescription;
    private String editingReferenceLabel;
    private String editingReferenceBibliographie;
    /** Code langue pour laquelle on édite le label (ex: fr, en). */
    private String editingLabelLangueCode;
    /** Code langue pour laquelle on édite la description (ex: fr, en). */
    private String editingDescriptionLangueCode;

    
    public void resetReferenceForm() {
        referenceCode = null;
        referenceLabel = null;
        referenceDescription = null;
        periodeId = null;
        referenceBibliographique = null;
        categorieIds = null;
    }
    
    public void createReference() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        // Validation des champs
        if (!EntityValidator.validateCode(referenceCode, entityRepository, reference_FORM)) {
            return;
        }
        
        if (!EntityValidator.validateLabel(referenceLabel, reference_FORM)) {
            return;
        }
        
        String codeTrimmed = referenceCode.trim();
        String labelTrimmed = referenceLabel.trim();
        
        try {
            EntityType referenceType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_REFERENCE)
                .orElseThrow(() -> new IllegalStateException(
                    "Le type d'entité '" + EntityConstants.ENTITY_TYPE_REFERENCE + "' n'existe pas dans la base de données."));
            
            Entity newReference = createNewReference(codeTrimmed, labelTrimmed, referenceType);
            entityRepository.save(newReference);
            
            // Rattacher le référentiel à la collection courante si une collection est sélectionnée
            if (applicationBean != null && applicationBean.getSelectedCollection() != null) {
                attachReferenceToCollection(newReference, applicationBean.getSelectedCollection());
            }
            
            refreshReferencesList();
            
            // Recharger les référentiels de la collection
            if (applicationBean != null && applicationBean.getSelectedCollection() != null) {
                applicationBean.refreshCollectionReferencesList();
            }

            updateTree(newReference);
            
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    "Le référentiel '" + labelTrimmed + "' a été créé avec succès."));

            resetReferenceForm();
            
            // Fermer le dialog
            PrimeFaces.current().executeScript("PF('referenceDialog').hide();");
            
            // Mettre à jour les composants
            PrimeFaces.current().ajax().update(
                ViewConstants.COMPONENT_GROWL + ", " + reference_FORM + ", " 
                + ViewConstants.COMPONENT_TREE_WIDGET + ", :collectionReferencesContainer");
            
        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création du référentiel", e);
            addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création du référentiel", e);
            addErrorMessage("Une erreur est survenue lors de la création du référentiel : " + e.getMessage());
        }
    }

    /**
     * Annule l'édition du référentiel
     */
    public void cancelEditingReference() {
        editingReference = false;
        editingReferenceCode = null;
        editingReferenceDescription = null;
        editingReferenceLabel = null;
        editingReferenceBibliographie = null;
        editingLabelLangueCode = null;
        editingDescriptionLangueCode = null;
    }

    /**
     * Sauvegarde les modifications du référentiel.
     * Enregistre : code, label (selon langue choisie), description (selon langue choisie),
     * référence bibliographique ; ajoute l'utilisateur courant aux auteurs s'il n'y figure pas.
     */
    @Transactional
    public void saveReference(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() == null) {
            return;
        }

        try {
            Entity referenceToUpdate = entityRepository.findById(applicationBean.getSelectedReference().getId()).get();

            // Mettre à jour le code uniquement si modifié
            String newCode = editingReferenceCode != null ? editingReferenceCode.trim() : null;
            if (!Objects.equals(newCode, referenceToUpdate.getCode()) && newCode != null && !newCode.isEmpty()) {
                referenceToUpdate.setCode(newCode);
            }

            // Langue pour le label (celle choisie dans le menu)
            String labelLangueCode = editingLabelLangueCode != null ? editingLabelLangueCode : "fr";
            Langue labelLangue = langueRepository.findByCode(labelLangueCode);
            // Mettre à jour le label (selon la langue choisie) uniquement si modifié
            String newLabel = editingReferenceLabel != null ? editingReferenceLabel.trim() : "";
            String currentLabelValue = EntityUtils.getLabelValueForLanguage(referenceToUpdate, labelLangueCode);
            if (!Objects.equals(newLabel, currentLabelValue) && labelLangue != null) {
                Optional<Label> labelOpt = referenceToUpdate.getLabels() != null
                        ? referenceToUpdate.getLabels().stream()
                        .filter(l -> l.getLangue() != null && labelLangueCode.equalsIgnoreCase(l.getLangue().getCode()))
                        .findFirst()
                        : Optional.empty();
                if (labelOpt.isPresent()) {
                    labelOpt.get().setNom(newLabel);
                } else {
                    Label newLabelEntity = new Label();
                    newLabelEntity.setNom(newLabel);
                    newLabelEntity.setEntity(referenceToUpdate);
                    newLabelEntity.setLangue(labelLangue);
                    if (referenceToUpdate.getLabels() == null) {
                        referenceToUpdate.setLabels(new ArrayList<>());
                    }
                    referenceToUpdate.getLabels().add(newLabelEntity);
                }
            }

            // Langue pour la description (celle choisie dans le menu)
            String descLangueCode = editingDescriptionLangueCode != null ? editingDescriptionLangueCode : "fr";
            Langue descLangue = langueRepository != null ? langueRepository.findByCode(descLangueCode) : null;
            // Mettre à jour la description (selon la langue choisie) uniquement si modifiée
            String newDesc = editingReferenceDescription != null ? editingReferenceDescription.trim() : "";
            String currentDescValue = EntityUtils.getDescriptionValueForLanguage(referenceToUpdate, descLangueCode);
            if (!Objects.equals(newDesc, currentDescValue) && descLangue != null) {
                Optional<Description> descOpt = referenceToUpdate.getDescriptions() != null
                        ? referenceToUpdate.getDescriptions().stream()
                        .filter(d -> d.getLangue() != null && descLangueCode.equalsIgnoreCase(d.getLangue().getCode()))
                        .findFirst()
                        : Optional.empty();
                if (descOpt.isPresent()) {
                    descOpt.get().setValeur(newDesc);
                } else {
                    Description newDescription = new Description();
                    newDescription.setValeur(newDesc);
                    newDescription.setEntity(referenceToUpdate);
                    newDescription.setLangue(descLangue);
                    if (referenceToUpdate.getDescriptions() == null) {
                        referenceToUpdate.setDescriptions(new ArrayList<>());
                    }
                    referenceToUpdate.getDescriptions().add(newDescription);
                }
            }

            // Mettre à jour la référence bibliographique (liste des références) uniquement si modifiée
            String newBib = editingReferenceBibliographie != null ? editingReferenceBibliographie.trim() : null;
            String currentBib = referenceToUpdate.getBibliographie();
            if (!Objects.equals(newBib, currentBib)) {
                referenceToUpdate.setRereferenceBibliographique(newBib);
            }

            // Ajouter l'utilisateur courant aux auteurs s'il n'y figure pas
            Utilisateur currentUser = loginBean != null ? loginBean.getCurrentUser() : null;
            if (currentUser != null && currentUser.getId() != null && utilisateurRepository != null) {
                Utilisateur managedUser = utilisateurRepository.findById(currentUser.getId()).orElse(null);
                if (managedUser != null) {
                    List<Utilisateur> auteurs = referenceToUpdate.getAuteurs();
                    if (auteurs == null) {
                        referenceToUpdate.setAuteurs(new ArrayList<>());
                        auteurs = referenceToUpdate.getAuteurs();
                    }
                    boolean alreadyAuthor = auteurs.stream()
                            .anyMatch(u -> u.getId() != null && u.getId().equals(managedUser.getId()));
                    if (!alreadyAuthor) {
                        auteurs.add(managedUser);
                    }
                }
            }

            applicationBean.setSelectedEntity(entityRepository.save(referenceToUpdate));

            applicationBean.getBeadCrumbElements().set(applicationBean.getBeadCrumbElements().size() - 1, applicationBean.getSelectedReference());

            editingReference = false;
            editingReferenceCode = null;
            editingReferenceDescription = null;
            editingReferenceLabel = null;
            editingReferenceBibliographie = null;
            editingLabelLangueCode = null;
            editingDescriptionLangueCode = null;

            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "Les modifications ont été enregistrées avec succès."));
            }

            log.info("Référentiel mis à jour avec succès: {}", applicationBean.getSelectedReference().getCode());
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du référentiel", e);
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Une erreur est survenue lors de la sauvegarde : " + e.getMessage()));
            }
        }
    }

    /**
     * Supprime le référentiel sélectionné et toutes ses entités rattachées
     */
    @Transactional
    public void deleteReference(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() == null || applicationBean.getSelectedReference().getId() == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Aucun référentiel sélectionné."));
            }
            return;
        }

        try {
            String referenceCode = applicationBean.getSelectedReference().getCode();
            String referenceName = applicationBean.getSelectedReference().getNom();
            Long referenceId = applicationBean.getSelectedReference().getId();

            // Supprimer récursivement le référentiel et toutes ses entités enfants
            applicationBean.deleteEntityRecursively(applicationBean.getSelectedReference());

            // Réinitialiser la sélection
            applicationBean.setSelectedEntity(null);
            applicationBean.setChilds(new ArrayList<>());

            // Recharger les référentiels de la collection
            if (applicationBean.getSelectedCollection() != null) {
                applicationBean.refreshCollectionReferencesList();
            }

            // Mettre à jour l'arbre
            treeBean.initializeTreeWithCollection();

            // Afficher un message de succès
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "Le référentiel '" + referenceName + "' et toutes ses entités rattachées ont été supprimés avec succès."));
            }

            // Afficher le panel de la collection
            if (applicationBean.getSelectedReference() != null) {
                applicationBean.getPanelState().showCollectionDetail();
            } else {
                applicationBean.getPanelState().showCollections();
            }

            log.info("Référentiel supprimé avec succès: {} (ID: {})", referenceCode, referenceId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du référentiel", e);
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Une erreur est survenue lors de la suppression : " + e.getMessage()));
            }
        }
    }

    /**
     * Active le mode édition pour le référentiel sélectionné.
     * Charge le code, la description, le label et la référence bibliographique.
     * Les langues d'édition label/description sont initialisées avec la langue sélectionnée (SearchBean).
     */
    public void startEditingReference(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() == null) {
            return;
        }
        String codeLang = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        editingReference = true;
        editingReferenceCode = applicationBean.getSelectedReference().getCode() != null ? applicationBean.getSelectedReference().getCode() : "";
        editingLabelLangueCode = codeLang;
        editingDescriptionLangueCode = codeLang;
        // Description selon la langue choisie
        editingReferenceDescription = EntityUtils.getDescriptionValueForLanguage(applicationBean.getSelectedReference(), codeLang);
        // Label selon la langue choisie
        editingReferenceLabel = EntityUtils.getLabelValueForLanguage(applicationBean.getSelectedReference(), codeLang);
        editingReferenceBibliographie = applicationBean.getSelectedReference().getRereferenceBibliographique() != null
                ? applicationBean.getSelectedReference().getBibliographie()
                : "";
    }

    /**
     * Appelé lorsque l'utilisateur change la langue du label dans le menu déroulant.
     * Recharge la valeur du label pour la nouvelle langue depuis l'entité.
     */
    public void onLabelLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() != null && editingLabelLangueCode != null) {
            editingReferenceLabel = EntityUtils.getLabelValueForLanguage(applicationBean.getSelectedReference(), editingLabelLangueCode);
        }
    }

    /**
     * Appelé lorsque l'utilisateur change la langue de la description dans le menu déroulant.
     * Recharge la valeur de la description pour la nouvelle langue depuis l'entité.
     */
    public void onDescriptionLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() != null && editingDescriptionLangueCode != null) {
            editingReferenceDescription = EntityUtils.getDescriptionValueForLanguage(applicationBean.getSelectedReference(), editingDescriptionLangueCode);
        }
    }

    /**
     * Crée une nouvelle entité référentiel
     */
    private Entity createNewReference(String code, String label, EntityType type) {
        Entity newReference = new Entity();
        newReference.setCode(code);
        newReference.setBibliographie(referenceBibliographique != null ? referenceBibliographique.trim() : null);
        newReference.setEntityType(type);
        newReference.setPublique(true);
        newReference.setCreateDate(LocalDateTime.now());

        Langue languePrincipale = langueRepository.findByCode(searchBean.getLangSelected());
        if (!StringUtils.isEmpty(label)) {
            Label labelPrincipal = new Label();
            labelPrincipal.setNom(label.trim());
            labelPrincipal.setLangue(languePrincipale);
            labelPrincipal.setEntity(newReference);
            List<Label> labels = new ArrayList<>();
            labels.add(labelPrincipal);
            newReference.setLabels(labels);
        }

        if (!StringUtils.isEmpty(referenceDescription)) {
            Description description = new Description();
            description.setValeur(referenceDescription);
            description.setLangue(languePrincipale);
            description.setEntity(newReference);
            List<Description> descriptions = new ArrayList<>();
            descriptions.add(description);
            newReference.setDescriptions(descriptions);
        }

        Utilisateur currentUser = loginBean.getCurrentUser();
        if (currentUser != null) {
            newReference.setCreateBy(currentUser.getEmail());
            List<Utilisateur> auteurs = new ArrayList<>();
            auteurs.add(currentUser);
            newReference.setAuteurs(auteurs);
        }

        return newReference;
    }

    /**
     * Recharge les listes de référentiels dans les beans concernés
     */
    private void refreshReferencesList() {
        if (applicationBean != null) {
            applicationBean.loadReferences();
        }
        if (searchBean != null) {
            searchBean.loadReferences();
        }
    }

    /**
     * Rattache un référentiel à une collection
     */
    private void attachReferenceToCollection(Entity reference, Entity collection) {
        try {
            // Vérifier si la relation existe déjà
            if (!entityRelationRepository.existsByParentAndChild(collection.getId(), reference.getId())) {
                fr.cnrs.opentypo.domain.entity.EntityRelation relation = 
                    new fr.cnrs.opentypo.domain.entity.EntityRelation();
                relation.setParent(collection);
                relation.setChild(reference);
                entityRelationRepository.save(relation);
            }
        } catch (Exception e) {
            log.error("Erreur lors du rattachement du référentiel à la collection", e);
        }
    }

    /**
     * Met à jour l'arbre avec le nouveau référentiel
     */
    private void updateTree(Entity reference) {
        if (treeBean != null) {
            treeBean.addReferenceToTree(reference);
        }
    }

    /**
     * Ajoute un message d'erreur
     */
    private void addErrorMessage(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", message));
        PrimeFaces.current().ajax().update(ViewConstants.COMPONENT_GROWL + ", " + reference_FORM);
    }
}
