package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.domain.entity.Image;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferentielOpenthesoRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.infrastructure.service.ImageService;
import fr.cnrs.opentypo.presentation.bean.util.EntityValidator;
import org.primefaces.model.file.UploadedFile;
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
@Named(value = "referentielBean")
@Slf4j
public class ReferentielBean implements Serializable {

    private static final String REFERENTIEL_FORM = ":referentielForm";

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private ReferentielOpenthesoRepository referentielOpenthesoRepository;

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

    @Inject
    private ImageService imageService;

    
    // Propriétés pour le formulaire de création de référentiel
    private String referentielCode;
    private String referentielLabel;
    private String referentielDescription;
    private String periodeId; // ID de la période (ReferentielOpentheso)
    private String referenceBibliographique;
    private String categorieIds; // IDs des catégories (Entity de type Catégorie)
    private UploadedFile uploadedImage; // Fichier image uploadé

    
    public void resetReferentielForm() {
        referentielCode = null;
        referentielLabel = null;
        referentielDescription = null;
        periodeId = null;
        referenceBibliographique = null;
        categorieIds = null;
        uploadedImage = null;
    }
    
    public void creerReferentiel() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        // Validation des champs
        if (!EntityValidator.validateCode(referentielCode, entityRepository, REFERENTIEL_FORM)) {
            return;
        }
        
        if (!EntityValidator.validateLabel(referentielLabel, REFERENTIEL_FORM)) {
            return;
        }
        
        String codeTrimmed = referentielCode.trim();
        String labelTrimmed = referentielLabel.trim();
        
        try {
            EntityType referentielType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_REFERENTIEL)
                .orElseThrow(() -> new IllegalStateException(
                    "Le type d'entité '" + EntityConstants.ENTITY_TYPE_REFERENTIEL + "' n'existe pas dans la base de données."));
            
            Entity nouveauReferentiel = createNewReferentiel(codeTrimmed, labelTrimmed, referentielType);
            entityRepository.save(nouveauReferentiel);
            
            // Rattacher le référentiel à la collection courante si une collection est sélectionnée
            if (applicationBean != null && applicationBean.getSelectedCollection() != null) {
                attachReferentielToCollection(nouveauReferentiel, applicationBean.getSelectedCollection());
            }
            
            refreshReferentielsList();
            
            // Recharger les référentiels de la collection
            if (applicationBean != null && applicationBean.getSelectedCollection() != null) {
                applicationBean.loadCollectionReferences();
            }
            
            updateTree(nouveauReferentiel);
            
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le référentiel '" + labelTrimmed + "' a été créé avec succès."));
            
            resetReferentielForm();
            
            // Fermer le dialog
            PrimeFaces.current().executeScript("PF('addReferentielDialog').hide();");
            
            // Mettre à jour les composants
            PrimeFaces.current().ajax().update(
                ViewConstants.COMPONENT_GROWL + ", " + REFERENTIEL_FORM + ", " 
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
    private Entity createNewReferentiel(String code, String label, EntityType type) {
        Entity nouveauReferentiel = new Entity();
        nouveauReferentiel.setCode(code);
        nouveauReferentiel.setNom(label);
        nouveauReferentiel.setCommentaire(referentielDescription != null ? referentielDescription.trim() : null);
        nouveauReferentiel.setBibliographie(referenceBibliographique != null ? referenceBibliographique.trim() : null);
        nouveauReferentiel.setEntityType(type);
        nouveauReferentiel.setPublique(true);
        nouveauReferentiel.setCreateDate(LocalDateTime.now());

        // Gérer l'upload d'image si un fichier a été fourni
        if (uploadedImage != null && imageService != null) {
            Image image = imageService.saveUploadedImage(uploadedImage);
            if (image != null) {
                nouveauReferentiel.setImage(image);
            }
        }

        Utilisateur currentUser = loginBean.getCurrentUser();
        if (currentUser != null) {
            nouveauReferentiel.setCreateBy(currentUser.getEmail());
            List<Utilisateur> auteurs = new ArrayList<>();
            auteurs.add(currentUser);
            nouveauReferentiel.setAuteurs(auteurs);
        }

        return nouveauReferentiel;
    }

    /**
     * Recharge les listes de référentiels dans les beans concernés
     */
    private void refreshReferentielsList() {
        if (applicationBean != null) {
            applicationBean.loadReferentiels();
        }
        if (searchBean != null) {
            searchBean.loadReferentiels();
        }
    }

    /**
     * Rattache un référentiel à une collection
     */
    private void attachReferentielToCollection(Entity referentiel, Entity collection) {
        try {
            // Vérifier si la relation existe déjà
            if (!entityRelationRepository.existsByParentAndChild(collection.getId(), referentiel.getId())) {
                fr.cnrs.opentypo.domain.entity.EntityRelation relation = 
                    new fr.cnrs.opentypo.domain.entity.EntityRelation();
                relation.setParent(collection);
                relation.setChild(referentiel);
                entityRelationRepository.save(relation);
            }
        } catch (Exception e) {
            log.error("Erreur lors du rattachement du référentiel à la collection", e);
        }
    }

    /**
     * Met à jour l'arbre avec le nouveau référentiel
     */
    private void updateTree(Entity referentiel) {
        if (treeBean != null) {
            treeBean.addReferentielToTree(referentiel);
        }
    }

    /**
     * Ajoute un message d'erreur
     */
    private void addErrorMessage(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", message));
        PrimeFaces.current().ajax().update(ViewConstants.COMPONENT_GROWL + ", " + REFERENTIEL_FORM);
    }
}
