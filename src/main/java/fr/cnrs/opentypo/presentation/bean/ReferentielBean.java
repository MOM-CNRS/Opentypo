package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.ReferentielOpentheso;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferentielOpenthesoRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
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
public class ReferentielBean implements Serializable {

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
        
        // Validation des champs obligatoires
        if (referentielCode == null || referentielCode.trim().isEmpty()) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Le code du référentiel est requis."));
            PrimeFaces.current().ajax().update(":growl, :referentielForm");
            return;
        }
        
        if (referentielLabel == null || referentielLabel.trim().isEmpty()) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Le label du référentiel est requis."));
            PrimeFaces.current().ajax().update(":growl, :referentielForm");
            return;
        }
        
        // Vérifier la validité du code (unicité)
        String codeTrimmed = referentielCode.trim();
        if (entityRepository.existsByCode(codeTrimmed)) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Un référentiel avec ce code existe déjà."));
            PrimeFaces.current().ajax().update(":growl, :referentielForm");
            return;
        }

        // Vérifier la validité du label (longueur)
        String labelTrimmed = referentielLabel.trim();
        if (labelTrimmed.length() > 255) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Le label ne peut pas dépasser 255 caractères."));
            PrimeFaces.current().ajax().update(":growl, :referentielForm");
            return;
        }
        
        // Vérifier la validité du code (longueur)
        if (codeTrimmed.length() > 100) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Le code ne peut pas dépasser 100 caractères."));
            PrimeFaces.current().ajax().update(":growl, :referentielForm");
            return;
        }
        
        try {
            // Récupérer le type d'entité "Référentiel"
            EntityType referentielType = entityTypeRepository.findByCode("REFERENTIEL")
                .orElseThrow(() -> new IllegalStateException("Le type d'entité 'REFERENTIEL' n'existe pas dans la base de données."));
            
            // Créer la nouvelle entité référentiel
            Entity nouveauReferentiel = new Entity();
            nouveauReferentiel.setCode(codeTrimmed);
            nouveauReferentiel.setNom(labelTrimmed);
            nouveauReferentiel.setCommentaire(referentielDescription != null ? referentielDescription.trim() : null);
            nouveauReferentiel.setBibliographie(referenceBibliographique != null ? referenceBibliographique.trim() : null);
            nouveauReferentiel.setEntityType(referentielType);
            nouveauReferentiel.setPublique(true);

            // Définir la période si fournie
            //nouveauReferentiel.setPeriode();
            
            // Définir l'utilisateur créateur et comme auteur
            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                nouveauReferentiel.setCreateBy(currentUser.getEmail());
                // Ajouter l'utilisateur connecté comme auteur
                List<Utilisateur> auteurs = new ArrayList<>();
                auteurs.add(currentUser);
                nouveauReferentiel.setAuteurs(auteurs);
            }

            nouveauReferentiel.setCreateDate(LocalDateTime.now());
            
            // Sauvegarder en base de données
            entityRepository.save(nouveauReferentiel);
            
            // Recharger la liste des référentiels dans ApplicationBean et SearchBean
            if (applicationBean != null) {
                applicationBean.loadReferentiels();
            }
            if (searchBean != null) {
                searchBean.loadReferentiels();
            }
            
            // Gérer les catégories du référentiel si fournies
            // Note: Les catégories seront liées via EntityRelation dans une étape ultérieure
            // Pour l'instant, on les stocke dans une liste pour traitement ultérieur
            
            // Créer un nouveau nœud référentiel dans l'arbre
            if (treeBean != null && treeBean.getRoot() != null) {
                new DefaultTreeNode(labelTrimmed, treeBean.getRoot());
            }
            
            // Message de succès
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le référentiel '" + labelTrimmed + "' a été créé avec succès."));
            
            // Réinitialiser le formulaire
            resetReferentielForm();
            
            PrimeFaces.current().ajax().update(":growl, :referentielForm, :treeWidget, :cardsContainer");
            
        } catch (IllegalStateException e) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :referentielForm");
        } catch (Exception e) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors de la création du référentiel : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :referentielForm");
        }
    }
}
