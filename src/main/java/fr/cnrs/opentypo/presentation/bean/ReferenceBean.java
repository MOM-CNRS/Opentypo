package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.util.EntityValidator;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@SessionScoped
@Named(value = "referenceBean")
@Slf4j
public class ReferenceBean implements Serializable {

    private static final String reference_FORM = ":referenceForm";

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Inject
    private LoginBean loginBean;

    @Inject
    private TreeBean treeBean;
    
    @Inject
    private ApplicationBean applicationBean;
    
    @Inject
    private SearchBean searchBean;

    @Inject
    private EntityRelationRepository entityRelationRepository;


    // Propriétés pour le formulaire de création de référentiel
    private String referenceCode;
    private String referenceLabel;
    private String referenceDescription;
    private String periodeId; // ID de la période (referenceOpentheso)
    private String referenceBibliographique;
    private String categorieIds; // IDs des catégories (Entity de type Catégorie)

    
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
     * Crée une nouvelle entité référentiel
     */
    private Entity createNewReference(String code, String label, EntityType type) {
        Entity newReference = new Entity();
        newReference.setCode(code);
        newReference.setNom(label);
        newReference.setCommentaire(referenceDescription != null ? referenceDescription.trim() : null);
        newReference.setBibliographie(referenceBibliographique != null ? referenceBibliographique.trim() : null);
        newReference.setEntityType(type);
        newReference.setPublique(true);
        newReference.setCreateDate(LocalDateTime.now());

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
            applicationBean.loadreferences();
        }
        if (searchBean != null) {
            searchBean.loadreferences();
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
            treeBean.addreferenceToTree(reference);
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
