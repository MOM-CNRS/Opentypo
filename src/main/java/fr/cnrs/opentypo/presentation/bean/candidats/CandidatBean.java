package fr.cnrs.opentypo.presentation.bean.candidats;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.UserBean;
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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named("candidatBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class CandidatBean implements Serializable {

    @Inject
    private UserBean userBean;

    @Inject
    private LoginBean loginBean;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    private List<Candidat> candidats = new ArrayList<>();
    private Candidat candidatSelectionne;
    private Candidat nouveauCandidat;
    private int activeTabIndex = 0; // 0 = en cours, 1 = validés, 2 = refusés
    
    // Champs pour l'étape 1 du formulaire
    private EntityType selectedEntityType;
    private String entityCode;
    private String entityLabel;
    private Langue selectedLangue;
    private Entity selectedCollection;
    private Entity selectedReference;
    
    // Liste des données pour les sélecteurs
    private List<EntityType> availableEntityTypes;
    private List<Langue> availableLanguages;
    private List<Entity> availableCollections;
    private List<Entity> availableReferences;
    
    // Index du wizard (0 = étape 1, 1 = étape 2)
    private int wizardStep = 0;

    @PostConstruct
    public void init() {
        chargerCandidats();
        loadAvailableEntityTypes();
        loadAvailableLanguages();
        loadAvailableCollections();
    }
    
    /**
     * Charge les types d'entités disponibles (sauf REFERENTIEL)
     */
    public void loadAvailableEntityTypes() {
        try {
            availableEntityTypes = entityTypeRepository.findAll().stream()
                .filter(et -> !EntityConstants.ENTITY_TYPE_REFERENCE.equals(et.getCode()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des types d'entités", e);
            availableEntityTypes = new ArrayList<>();
        }
    }
    
    /**
     * Charge les langues disponibles
     */
    public void loadAvailableLanguages() {
        try {
            availableLanguages = langueRepository.findAllByOrderByNomAsc();
        } catch (Exception e) {
            log.error("Erreur lors du chargement des langues", e);
            availableLanguages = new ArrayList<>();
        }
    }
    
    /**
     * Charge les collections disponibles depuis la base de données
     * Si l'utilisateur est déconnecté, affiche uniquement les collections publiques
     * Si l'utilisateur est connecté, affiche toutes les collections (publiques et privées)
     */
    public void loadAvailableCollections() {
        try {
            // Charger toutes les collections depuis la base de données
            List<Entity> allCollections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION);
            
            // Filtrer selon l'authentification
            boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
            availableCollections = allCollections.stream()
                .filter(c -> {
                    // Si l'utilisateur est authentifié, afficher toutes les collections
                    // Sinon, afficher uniquement les collections publiques
                    return isAuthenticated || (c.getPublique() != null && c.getPublique());
                })
                .collect(Collectors.toList());
            
            log.debug("Collections chargées: {} (utilisateur authentifié: {})", 
                availableCollections.size(), isAuthenticated);
        } catch (Exception e) {
            log.error("Erreur lors du chargement des collections depuis la base de données", e);
            availableCollections = new ArrayList<>();
        }
    }
    
    /**
     * Charge les référentiels d'une collection sélectionnée depuis la base de données
     * Filtre les référentiels selon l'état d'authentification de l'utilisateur
     */
    public void loadReferencesForCollection() {
        availableReferences = new ArrayList<>();
        if (selectedCollection != null) {
            try {
                // Charger les référentiels rattachés à la collection sélectionnée
                List<Entity> allReferences = entityRelationRepository.findChildrenByParentAndType(
                    selectedCollection, EntityConstants.ENTITY_TYPE_REFERENCE);
                
                // Filtrer selon l'authentification
                boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
                availableReferences = allReferences.stream()
                    .filter(r -> {
                        // Si l'utilisateur est authentifié, afficher tous les référentiels
                        // Sinon, afficher uniquement les référentiels publics
                        return isAuthenticated || (r.getPublique() != null && r.getPublique());
                    })
                    .collect(Collectors.toList());
                
                log.debug("Référentiels chargés pour la collection {}: {} (utilisateur authentifié: {})", 
                    selectedCollection.getCode(), availableReferences.size(), isAuthenticated);
            } catch (Exception e) {
                log.error("Erreur lors du chargement des référentiels de la collection", e);
                availableReferences = new ArrayList<>();
            }
        } else {
            // Si aucune collection n'est sélectionnée, vider la liste des référentiels
            availableReferences = new ArrayList<>();
        }
        // Réinitialiser la sélection du référentiel si la collection change
        selectedReference = null;
    }
    
    /**
     * Passe à l'étape suivante du wizard
     */
    public void nextStep() {
        if (wizardStep == 0) {
            // Valider l'étape 1
            if (validateStep1()) {
                wizardStep = 1;
            }
        }
    }
    
    /**
     * Retourne à l'étape précédente du wizard
     */
    public void previousStep() {
        if (wizardStep > 0) {
            wizardStep--;
        }
    }
    
    /**
     * Valide l'étape 1 du formulaire
     */
    private boolean validateStep1() {
        if (selectedEntityType == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le type d'entité est requis."));
            return false;
        }
        if (entityCode == null || entityCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le code est requis."));
            return false;
        }
        if (entityLabel == null || entityLabel.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le label est requis."));
            return false;
        }
        if (selectedLangue == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "La langue est requise."));
            return false;
        }
        if (selectedCollection == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "La collection est requise."));
            return false;
        }
        if (selectedReference == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le référentiel est requis."));
            return false;
        }
        return true;
    }
    
    /**
     * Retourne le nom du type d'entité pour l'affichage
     */
    public String getEntityTypeName(EntityType entityType) {
        if (entityType == null) {
            return "";
        }
        String code = entityType.getCode();
        switch (code) {
            case EntityConstants.ENTITY_TYPE_COLLECTION:
                return "Collection";
            case EntityConstants.ENTITY_TYPE_CATEGORY:
                return "Catégorie";
            case EntityConstants.ENTITY_TYPE_GROUP:
                return "Groupe";
            case EntityConstants.ENTITY_TYPE_SERIES:
                return "Série";
            case EntityConstants.ENTITY_TYPE_TYPE:
                return "Type";
            default:
                return code;
        }
    }

    public void chargerCandidats() {
        // Données d'exemple - à remplacer par un service réel
        if (candidats.isEmpty()) {
            candidats.add(new Candidat(1L, "TYPE-001", "Amphore type A", "fr", "Période romaine", 100, 200, 
                "Commentaire datation", "Amphore", "Description", "Production locale", "Atelier 1", 
                "Aire méditerranéenne", "Catégorie 1", "Céramique", "Ronde", "20x30cm", "Tournage", 
                "Fabrication manuelle", new ArrayList<>(), new ArrayList<>(), "REF-001", "TYP-001", 
                "ID-001", "V1", "Bibliographie", Candidat.Statut.EN_COURS, LocalDateTime.now(), null, "admin"));
            
            candidats.add(new Candidat(2L, "TYPE-002", "Vase type B", "fr", "Période grecque", 300, 400, 
                "Commentaire", "Vase", "Description", "Production", "Atelier 2", "Aire", "Catégorie", 
                "Céramique", "Oval", "15x25cm", "Moulage", "Fabrication", new ArrayList<>(), new ArrayList<>(), 
                "REF-002", "TYP-002", "ID-002", "V1", "Bibliographie", Candidat.Statut.VALIDE, 
                LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(1), "admin"));
        }
    }

    public List<Candidat> getCandidatsEnCours() {
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.EN_COURS)
            .collect(Collectors.toList());
    }

    public List<Candidat> getCandidatsValides() {
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.VALIDE)
            .collect(Collectors.toList());
    }

    public List<Candidat> getCandidatsRefuses() {
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.REFUSE)
            .collect(Collectors.toList());
    }

    public void initNouveauCandidat() {
        nouveauCandidat = new Candidat();
        nouveauCandidat.setCreateur(userBean.getUsername());
        nouveauCandidat.setStatut(Candidat.Statut.EN_COURS);
        // Réinitialiser les champs du wizard
        resetWizardForm();
        // Charger les collections disponibles depuis la base de données
        loadAvailableCollections();
    }
    
    /**
     * Réinitialise le formulaire du wizard
     */
    public void resetWizardForm() {
        wizardStep = 0;
        selectedEntityType = null;
        entityCode = null;
        entityLabel = null;
        selectedLangue = null;
        selectedCollection = null;
        selectedReference = null;
        availableReferences = new ArrayList<>();
        // Réinitialiser les champs de l'étape 2
        typeDescription = null;
        serieDescription = null;
        groupDescription = null;
        categoryDescription = null;
        collectionDescription = null;
        collectionPublique = true;
    }
    
    // Champs pour l'étape 2 selon le type
    private String typeDescription;
    private String serieDescription;
    private String groupDescription;
    private String categoryDescription;
    private String collectionDescription;
    private Boolean collectionPublique = true;
    
    /**
     * Gère la navigation dans le wizard
     */
    public String onFlowProcess(org.primefaces.event.FlowEvent event) {
        String oldStep = event.getOldStep();
        String newStep = event.getNewStep();
        
        // Si on passe de l'étape 1 à l'étape 2, valider l'étape 1
        if ("step1".equals(oldStep) && "step2".equals(newStep)) {
            if (!validateStep1()) {
                return oldStep; // Rester sur l'étape 1
            }
        }
        
        return newStep;
    }

    public void sauvegarderCandidat() {
        if (nouveauCandidat.getId() == null) {
            // Nouveau candidat
            Long nouveauId = candidats.stream()
                .mapToLong(Candidat::getId)
                .max()
                .orElse(0L) + 1;
            nouveauCandidat.setId(nouveauId);
            nouveauCandidat.setDateCreation(LocalDateTime.now());
            candidats.add(nouveauCandidat);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le candidat a été créé avec succès."));
        } else {
            // Modification
            nouveauCandidat.setDateModification(LocalDateTime.now());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le candidat a été modifié avec succès."));
        }
        
        PrimeFaces.current().ajax().update(":growl, :candidatsForm");
        PrimeFaces.current().executeScript("window.location.href='/candidats/candidats.xhtml';");
    }

    public void validerCandidat(Candidat candidat) {
        candidat.setStatut(Candidat.Statut.VALIDE);
        candidat.setDateModification(LocalDateTime.now());
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "Le candidat a été validé."));
        PrimeFaces.current().ajax().update(":growl, :candidatsForm");
    }

    public void refuserCandidat(Candidat candidat) {
        candidat.setStatut(Candidat.Statut.REFUSE);
        candidat.setDateModification(LocalDateTime.now());
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "Le candidat a été refusé."));
        PrimeFaces.current().ajax().update(":growl, :candidatsForm");
    }

    public void supprimerCandidat(Candidat candidat) {
        candidats.remove(candidat);
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "Le candidat a été supprimé."));
        PrimeFaces.current().ajax().update(":growl, :candidatsForm");
    }

    public String visualiserCandidat(Candidat candidat) {
        candidatSelectionne = candidat;
        return "/candidats/view.xhtml?faces-redirect=true";
    }
}

