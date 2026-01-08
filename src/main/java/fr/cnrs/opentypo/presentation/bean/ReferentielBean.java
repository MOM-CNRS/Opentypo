package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferentielOpenthesoRepository;
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
import org.primefaces.model.DefaultTreeNode;

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

    
    // Propriétés pour le formulaire de création de référentiel
    private String referentielCode;
    private String referentielLabel;
    private String referentielDescription;
    private String periodeId; // ID de la période (ReferentielOpentheso)
    private String referenceBibliographique;
    private String categorieIds; // IDs des catégories (Entity de type Catégorie)

    
    public void resetReferentielForm() {
        referentielCode = null;
        referentielLabel = null;
        referentielDescription = null;
        periodeId = null;
        referenceBibliographique = null;
        categorieIds = null;
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
            
            refreshReferentielsList();
            updateTree(labelTrimmed);
            
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le référentiel '" + labelTrimmed + "' a été créé avec succès."));
            
            resetReferentielForm();
            PrimeFaces.current().ajax().update(
                ViewConstants.COMPONENT_GROWL + ", " + REFERENTIEL_FORM + ", " 
                + ViewConstants.COMPONENT_TREE_WIDGET + ", " + ViewConstants.COMPONENT_CARDS_CONTAINER);
            
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
     * Met à jour l'arbre avec le nouveau référentiel
     */
    private void updateTree(String label) {
        if (treeBean != null && treeBean.getRoot() != null) {
            new DefaultTreeNode(label, treeBean.getRoot());
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
