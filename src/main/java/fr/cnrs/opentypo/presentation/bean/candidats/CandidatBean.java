package fr.cnrs.opentypo.presentation.bean.candidats;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.application.service.IiifImageService;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.SearchBean;
import fr.cnrs.opentypo.presentation.bean.UserBean;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.model.file.UploadedFile;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Inject
    private SearchBean searchBean;

    @Inject
    private IiifImageService iiifImageService;

    private List<Candidat> candidats = new ArrayList<>();
    private Candidat candidatSelectionne;
    private Candidat nouveauCandidat;
    private Candidat candidatAValider; // Candidat sélectionné pour validation
    private Candidat candidatASupprimer; // Candidat sélectionné pour suppression
    private int activeTabIndex = 0; // 0 = en cours, 1 = validés, 2 = refusés
    private boolean candidatsLoaded = false; // Flag pour savoir si les candidats ont été chargés
    
    // Champs pour l'étape 1 du formulaire
    private Long selectedEntityTypeId;
    private String entityCode;
    private String entityLabel;
    private String candidatProduction;
    private String selectedLangueCode;
    private Long selectedCollectionId;
    private Entity selectedParentEntity;
    
    // Liste des données pour les sélecteurs
    private List<EntityType> availableEntityTypes;
    private List<Langue> availableLanguages;
    private List<Entity> availableCollections;
    private List<Entity> availableReferences;
    
    // Arbre pour les référentiels et leurs enfants
    private TreeNode referenceTreeRoot;
    private TreeNode selectedTreeNode;
    
    // Index du wizard (0 = étape 1, 1 = étape 2, 2 = étape 3)
    private int currentStep = 0;
    
    /**
     * Passe à l'étape suivante du wizard
     */
    public void nextStep() {
        log.info("nextStep() appelée - currentStep actuel: {}", currentStep);
        
        if (currentStep == 0) { // De l'étape 1 à l'étape 2
            log.info("Validation de l'étape 1...");
            // Validation manuelle
            if (validateStep1()) {
                currentStep++;
                log.info("Passage à l'étape 2 - currentStep = {}", currentStep);
            } else {
                log.warn("Validation échouée à l'étape 1, reste sur l'étape 1");
            }
        } else if (currentStep == 1) { // De l'étape 2 à l'étape 3
            log.info("Validation de l'étape 2...");
            // Validation manuelle
            if (validateStep2()) {
                currentStep++;
                Entity parent = (Entity) selectedTreeNode.getData();
                selectedParentEntity = entityRepository.findById(parent.getId()).orElse(null);
                log.info("Passage à l'étape 3 - currentStep = {}", currentStep);
            } else {
                log.warn("Validation échouée à l'étape 2, reste sur l'étape 2");
            }
        } else {
            log.warn("nextStep() appelée mais currentStep = {} (hors limites)", currentStep);
        }
    }
    
    /**
     * Retourne à l'étape précédente du wizard
     */
    public void previousStep() {
        if (currentStep > 0) {
            currentStep--;
        }
    }
    
    /**
     * Retourne true si on est sur l'étape 1
     */
    public boolean isStep1() {
        return currentStep == 0;
    }
    
    /**
     * Retourne true si on est sur l'étape 2
     */
    public boolean isStep2() {
        return currentStep == 1;
    }
    
    /**
     * Retourne true si on est sur l'étape 3
     */
    public boolean isStep3() {
        return currentStep == 2;
    }


    @PostConstruct
    public void init() {
        chargerCandidats();
        loadAvailableEntityTypes();
        loadAvailableLanguages();
        availableCollections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION);
    }
    
    /**
     * Méthode appelée lors de l'accès à la page candidats
     * Recharge les données pour s'assurer qu'elles sont à jour
     */
    public void loadCandidatsPage() {
        log.debug("Chargement de la page candidats, rechargement des données");
        candidatsLoaded = false; // Invalider le cache
        chargerCandidats();
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
     * Charge les référentiels d'une collection sélectionnée depuis la base de données
     * Construit un arbre avec les référentiels et leurs enfants
     * Filtre selon l'état d'authentification de l'utilisateur
     */
    public void loadReferencesForCollection() {
        log.debug("loadReferencesForCollection appelée - selectedCollectionId: {}", selectedCollectionId);
        
        // Réinitialiser l'arbre
        referenceTreeRoot = null;
        selectedTreeNode = null;
        
        if (selectedCollectionId != null) {
            try {
                // Recharger la collection depuis la base pour éviter les problèmes de lazy loading
                Entity refreshedCollection = entityRepository.findById(selectedCollectionId)
                    .orElse(null);
                
                if (refreshedCollection == null) {
                    log.warn("Collection avec l'ID {} non trouvée", selectedCollectionId);
                    return;
                }
                
                // Créer le nœud racine avec le code de la collection
                String collectionCode = refreshedCollection.getCode() != null ? refreshedCollection.getCode() : "Collection";
                DefaultTreeNode rootNode = new DefaultTreeNode(collectionCode, null);
                rootNode.setData(refreshedCollection);
                referenceTreeRoot = rootNode;
                
                // Charger les référentiels rattachés à la collection sélectionnée
                List<Entity> allReferences = entityRelationRepository.findChildrenByParentAndType(
                    refreshedCollection, EntityConstants.ENTITY_TYPE_REFERENCE);
                
                // Filtrer selon l'authentification
                boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
                List<Entity> filteredReferences = allReferences.stream()
                    .filter(r -> {
                        // Si l'utilisateur est authentifié, afficher tous les référentiels
                        // Sinon, afficher uniquement les référentiels publics
                        return isAuthenticated || (r.getPublique() != null && r.getPublique());
                    })
                    .collect(Collectors.toList());
                
                // Construire l'arbre avec les référentiels et leurs enfants
                for (Entity reference : filteredReferences) {
                    DefaultTreeNode referenceNode = new DefaultTreeNode(reference.getNom(), referenceTreeRoot);
                    referenceNode.setData(reference);
                    // Charger les enfants du référentiel (catégories, groupes, etc.)
                    loadChildrenRecursively(referenceNode, reference, isAuthenticated);
                }
                
                log.debug("Arbre construit pour la collection {}: {} référentiels (utilisateur authentifié: {})", 
                    refreshedCollection.getCode(), filteredReferences.size(), isAuthenticated);
            } catch (Exception e) {
                log.error("Erreur lors du chargement des référentiels de la collection", e);
            }
        } else {
            log.debug("Aucune collection sélectionnée, arbre vidé");
        }
        
        // Forcer la mise à jour du conteneur de l'arbre
        PrimeFaces.current().ajax().update(":createCandidatForm:referenceTreeContainer");
    }
    
    /**
     * Charge récursivement les enfants d'une entité dans l'arbre
     */
    private void loadChildrenRecursively(TreeNode parentNode, Entity parentEntity, boolean isAuthenticated) {
        try {
            // Charger tous les enfants directs
            List<Entity> children = entityRelationRepository.findChildrenByParent(parentEntity);
            
            // Filtrer selon l'authentification
            List<Entity> filteredChildren = children.stream()
                .filter(child -> isAuthenticated || (child.getPublique() != null && child.getPublique()))
                .collect(Collectors.toList());
            
            // Ajouter chaque enfant comme nœud et charger ses propres enfants
            for (Entity child : filteredChildren) {
                DefaultTreeNode childNode = new DefaultTreeNode(child.getNom(), parentNode);
                childNode.setData(child);
                // Charger récursivement les enfants de cet enfant
                loadChildrenRecursively(childNode, child, isAuthenticated);
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des enfants de l'entité {}", parentEntity.getNom(), e);
        }
    }

    /**
     * Valide l'étape 1 du formulaire (Type et identification)
     */
    private boolean validateStep1() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        boolean isValid = true;
        
        if (selectedEntityTypeId == null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le type d'entité est requis."));
            isValid = false;
        }
        if (entityCode == null || entityCode.trim().isEmpty()) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le code est requis."));
            isValid = false;
        }
        if (entityLabel == null || entityLabel.trim().isEmpty()) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le label est requis."));
            isValid = false;
        }
        if (selectedLangueCode == null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "La langue est requise."));
            isValid = false;
        }
        
        // Ne pas appeler validationFailed() car cela empêche la mise à jour des composants
        // On retourne simplement false et on laisse nextStep() gérer la mise à jour
        
        return isValid;
    }
    
    /**
     * Valide l'étape 2 du formulaire (Collection et référentiel)
     */
    private boolean validateStep2() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        boolean isValid = true;
        
        if (selectedCollectionId == null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "La collection est requise."));
            isValid = false;
        }
        if (selectedTreeNode == null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Le référentiel est requis."));
            isValid = false;
        }
        
        // Ne pas appeler validationFailed() car cela empêche la mise à jour des composants
        // On retourne simplement false et on laisse nextStep() gérer la mise à jour
        
        return isValid;
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

    /**
     * Charge les candidats depuis la base de données
     * Cette méthode est appelée lors de l'initialisation et peut être appelée manuellement pour rafraîchir les données
     */
    public void chargerCandidats() {
        try {
            // Charger les entités avec statut PROPOSITION, ACCEPTED, REFUSED
            List<Entity> entitiesProposition = entityRepository.findByStatut(EntityStatusEnum.PROPOSITION.name());
            List<Entity> entitiesAccepted = entityRepository.findByStatut(EntityStatusEnum.ACCEPTED.name());
            List<Entity> entitiesRefused = entityRepository.findByStatut(EntityStatusEnum.REFUSED.name());
            
            // Convertir en Candidat et stocker dans la liste
            candidats.clear();
            candidats.addAll(entitiesProposition.stream()
                .map(this::convertEntityToCandidat)
                .collect(Collectors.toList()));
            candidats.addAll(entitiesAccepted.stream()
                .map(this::convertEntityToCandidat)
                .collect(Collectors.toList()));
            candidats.addAll(entitiesRefused.stream()
                .map(this::convertEntityToCandidat)
                .collect(Collectors.toList()));
            
            candidatsLoaded = true;
            log.info("Chargement des candidats terminé: {} PROPOSITION, {} ACCEPTED, {} REFUSED", 
                entitiesProposition.size(), entitiesAccepted.size(), entitiesRefused.size());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des candidats depuis la base de données", e);
            candidats.clear();
            candidatsLoaded = false;
        }
    }

    /**
     * Convertit une Entity en Candidat pour l'affichage
     */
    private Candidat convertEntityToCandidat(Entity entity) {
        if (entity == null) {
            return null;
        }
        
        Candidat candidat = new Candidat();
        candidat.setId(entity.getId());
        candidat.setTypeCode(entity.getEntityType() != null ? entity.getEntityType().getCode() : "");
        
        // Récupérer le label principal (premier label disponible ou nom par défaut)
        String label = entity.getNom();
        if (entity.getLabels() != null && !entity.getLabels().isEmpty()) {
            // Essayer de trouver un label en français, sinon prendre le premier
            Optional<Label> labelFr = entity.getLabels().stream()
                .filter(l -> l.getLangue() != null && "fr".equalsIgnoreCase(l.getLangue().getCode()))
                .findFirst();
            if (labelFr.isPresent()) {
                label = labelFr.get().getNom();
            } else {
                label = entity.getLabels().get(0).getNom();
            }
        }
        candidat.setLabel(label);
        
        // Récupérer la langue principale (premier label ou "fr" par défaut)
        String langue = "fr";
        if (entity.getLabels() != null && !entity.getLabels().isEmpty() && 
            entity.getLabels().get(0).getLangue() != null) {
            langue = entity.getLabels().get(0).getLangue().getCode();
        }
        candidat.setLangue(langue);
        
        // Période
        candidat.setPeriode(entity.getPeriode() != null ? entity.getPeriode().getValeur() : "");
        
        // Dates et autres champs
        candidat.setTpq(entity.getTpq());
        candidat.setTaq(entity.getTaq());
        candidat.setCommentaireDatation(entity.getCommentaire());
        candidat.setAppellationUsuelle(entity.getAppellation());
        
        // Description (première description disponible)
        String description = "";
        if (entity.getDescriptions() != null && !entity.getDescriptions().isEmpty()) {
            Optional<Description> descFr = entity.getDescriptions().stream()
                .filter(d -> d.getLangue() != null && "fr".equalsIgnoreCase(d.getLangue().getCode()))
                .findFirst();
            if (descFr.isPresent()) {
                description = descFr.get().getValeur();
            } else {
                description = entity.getDescriptions().get(0).getValeur();
            }
        }
        candidat.setDescription(description);
        
        candidat.setProduction(entity.getProduction() != null ? entity.getProduction().getValeur() : "");
        candidat.setAireCirculation(entity.getAireCirculation() != null ? entity.getAireCirculation().getValeur() : "");
        candidat.setCategorieFonctionnelle(entity.getCategorieFonctionnelle() != null ? 
            entity.getCategorieFonctionnelle().getValeur() : "");
        candidat.setReference(entity.getReference());
        candidat.setTypologiqueScientifique(entity.getTypologieScientifique());
        candidat.setIdentifiantPerenne(entity.getIdentifiantPerenne());
        candidat.setAncienneVersion(entity.getAncienneVersion());
        candidat.setBibliographie(entity.getBibliographie());
        
        // Dates
        candidat.setDateCreation(entity.getCreateDate());
        // Pour dateModification, on utilise createDate si pas de modification (pour REFUSED/ACCEPTED, on pourrait utiliser Envers)
        candidat.setDateModification(entity.getCreateDate());
        
        // Créateur
        candidat.setCreateur(entity.getCreateBy() != null ? entity.getCreateBy() : "");
        
        // Statut
        if (EntityStatusEnum.PROPOSITION.name().equals(entity.getStatut())) {
            candidat.setStatut(Candidat.Statut.EN_COURS);
        } else if (EntityStatusEnum.ACCEPTED.name().equals(entity.getStatut())) {
            candidat.setStatut(Candidat.Statut.VALIDE);
        } else if (EntityStatusEnum.REFUSED.name().equals(entity.getStatut())) {
            candidat.setStatut(Candidat.Statut.REFUSE);
        } else {
            candidat.setStatut(Candidat.Statut.EN_COURS);
        }
        
        return candidat;
    }

    /**
     * Retourne la liste des candidats en cours (statut PROPOSITION)
     */
    public List<Candidat> getCandidatsEnCours() {
        // Recharger les données si nécessaire (lazy loading)
        chargerCandidatsIfNeeded();
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.EN_COURS)
            .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des candidats validés (statut ACCEPTED)
     */
    public List<Candidat> getCandidatsValides() {
        // Recharger les données si nécessaire (lazy loading)
        chargerCandidatsIfNeeded();
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.VALIDE)
            .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des candidats refusés (statut REFUSED)
     */
    public List<Candidat> getCandidatsRefuses() {
        // Recharger les données si nécessaire (lazy loading)
        chargerCandidatsIfNeeded();
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.REFUSE)
            .collect(Collectors.toList());
    }
    
    /**
     * Recharge les candidats si nécessaire (lazy loading)
     */
    private void chargerCandidatsIfNeeded() {
        if (!candidatsLoaded) {
            chargerCandidats();
        }
    }

    public void initNouveauCandidat() {
        // Ne réinitialiser que lors du premier chargement de la page, pas lors des requêtes AJAX
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null && facesContext.isPostback()) {
            // C'est une requête AJAX, ne pas réinitialiser
            return;
        }
        
        nouveauCandidat = new Candidat();
        nouveauCandidat.setCreateur(userBean.getUsername());
        nouveauCandidat.setStatut(Candidat.Statut.EN_COURS);
        // Réinitialiser les champs du wizard
        resetWizardForm();
        // Charger les collections disponibles depuis la base de données
        availableCollections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION);
    }
    
    /**
     * Réinitialise le formulaire du wizard
     */
    public void resetWizardForm() {
        // Ne réinitialiser currentStep que si on n'est pas déjà dans le wizard
        // Cela évite de réinitialiser l'étape lors des mises à jour AJAX
        if (currentStep == 0 && selectedEntityTypeId == null && entityCode == null) {
            // Seulement réinitialiser si le formulaire est vraiment vide
        } else {
            // Ne pas réinitialiser currentStep si on est déjà dans le wizard
            return;
        }
        currentStep = 0;
        selectedEntityTypeId = null;
        entityCode = null;
        entityLabel = null;
        selectedLangueCode = null;
        selectedCollectionId = null;
        availableReferences = new ArrayList<>();
        referenceTreeRoot = null;
        selectedTreeNode = null;
        // Réinitialiser les champs de l'étape 2
        typeDescription = null;
        serieDescription = null;
        groupDescription = null;
        categoryDescription = null;
        imagePrincipaleUrl = null;
        candidatLabels = new ArrayList<>();
        newLabelValue = null;
        newLabelLangueCode = null;
        descriptions = new ArrayList<>();
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        candidatCommentaire = null;
        candidatBibliographie = null;
        referencesBibliographiques = new ArrayList<>();
        collectionDescription = null;
        collectionPublique = true;
        
        // Réinitialiser les champs du groupe
        groupPeriode = null;
        groupTpq = null;
        groupTaq = null;
    }
    
    // Champs pour l'étape 2 selon le type
    private String typeDescription;
    private String serieDescription;
    private String groupDescription;
    private String categoryDescription; // Ancien champ, conservé pour compatibilité
    private String imagePrincipaleUrl;
    private UploadedFile uploadedImageFile;
    private List<CategoryLabelItem> candidatLabels = new ArrayList<>();
    private String newLabelValue;
    private String newLabelLangueCode;
    private List<CategoryDescriptionItem> descriptions = new ArrayList<>();
    private String newDescriptionValue;
    private String newDescriptionLangueCode;
    private String candidatCommentaire;
    private String candidatBibliographie;
    private List<String> referencesBibliographiques = new ArrayList<>();
    private String collectionDescription;
    private Boolean collectionPublique = true;
    
    // Champs pour le formulaire de groupe
    private String groupPeriode;
    private Integer groupTpq;
    private Integer groupTaq;
    
    /**
     * Classe interne pour représenter un label de catégorie avec sa langue
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryLabelItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String nom;
        private String langueCode;
        private Langue langue;
    }
    
    /**
     * Classe interne pour représenter une description de catégorie avec sa langue
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDescriptionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String valeur;
        private String langueCode;
        private Langue langue;
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
     * Récupère la collection sélectionnée
     */
    public Entity getSelectedCollection() {
        if (selectedCollectionId == null) {
            return null;
        }
        return entityRepository.findById(selectedCollectionId).orElse(null);
    }
    
    /**
     * Récupère la langue sélectionnée
     */
    public Langue getSelectedLangue() {
        if (selectedLangueCode == null) {
            return null;
        }
        return langueRepository.findByCode(selectedLangueCode);
    }
    
    /**
     * Récupère le nom de la langue sélectionnée
     */
    public String getSelectedLangueName() {
        // Si un candidat est sélectionné, utiliser sa langue
        if (candidatSelectionne != null && candidatSelectionne.getLangue() != null) {
            Langue langue = langueRepository.findByCode(candidatSelectionne.getLangue());
            return langue != null ? langue.getNom() : candidatSelectionne.getLangue();
        }
        // Sinon, utiliser la langue sélectionnée dans le wizard
        Langue langue = getSelectedLangue();
        return langue != null ? langue.getNom() : "";
    }
    
    /**
     * Sauvegarde le candidat en créant l'entité dans la base de données avec le statut PROPOSITION
     */
    public void sauvegarderCandidat() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        try {
            // Validation des champs obligatoires
            if (selectedEntityTypeId == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Le type d'entité est requis."));
                PrimeFaces.current().ajax().update(":growl", ":createCandidatForm");
                return;
            }
            
            if (entityCode == null || entityCode.trim().isEmpty()) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Le code est requis."));
                PrimeFaces.current().ajax().update(":growl", ":createCandidatForm");
                return;
            }
            
            if (entityLabel == null || entityLabel.trim().isEmpty()) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Le label est requis."));
                PrimeFaces.current().ajax().update(":growl", ":createCandidatForm");
                return;
            }
            
            if (selectedLangueCode == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "La langue est requise."));
                PrimeFaces.current().ajax().update(":growl", ":createCandidatForm");
                return;
            }
            
            if (selectedCollectionId == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "La collection est requise."));
                PrimeFaces.current().ajax().update(":growl", ":createCandidatForm");
                return;
            }
            
            if (selectedTreeNode == null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Le référentiel est requis."));
                PrimeFaces.current().ajax().update(":growl", ":createCandidatForm");
                return;
            }
            
            // Récupérer le type d'entité
            EntityType entityType = entityTypeRepository.findById(selectedEntityTypeId)
                .orElseThrow(() -> new IllegalStateException("Le type d'entité sélectionné n'existe pas."));
            
            // Créer la nouvelle entité
            Entity newEntity = new Entity();
            newEntity.setCode(entityCode.trim());
            newEntity.setNom(entityLabel.trim());
            newEntity.setEntityType(entityType);
            newEntity.setStatut(EntityStatusEnum.PROPOSITION.name());
            newEntity.setPublique(true);
            newEntity.setCreateDate(LocalDateTime.now());
            
            // Récupérer l'utilisateur actuel
            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                newEntity.setCreateBy(currentUser.getEmail());
                List<Utilisateur> auteurs = new ArrayList<>();
                auteurs.add(currentUser);
                newEntity.setAuteurs(auteurs);
            }
            
            // Ajouter le label principal (de l'étape 1)
            Langue languePrincipale = langueRepository.findByCode(selectedLangueCode);
            if (languePrincipale != null) {
                Label labelPrincipal = new Label();
                labelPrincipal.setNom(entityLabel.trim());
                labelPrincipal.setEntity(newEntity);
                labelPrincipal.setLangue(languePrincipale);
                newEntity.getLabels().add(labelPrincipal);
            }
            
            // Ajouter les traductions de labels (pour les catégories)
            if (candidatLabels != null && !candidatLabels.isEmpty()) {
                for (CategoryLabelItem labelItem : candidatLabels) {
                    if (labelItem.getNom() != null && !labelItem.getNom().trim().isEmpty() &&
                        labelItem.getLangueCode() != null && !labelItem.getLangueCode().trim().isEmpty()) {
                        
                        Langue langue = langueRepository.findByCode(labelItem.getLangueCode());
                        if (langue != null) {
                            Label label = new Label();
                            label.setNom(labelItem.getNom().trim());
                            label.setEntity(newEntity);
                            label.setLangue(langue);
                            newEntity.getLabels().add(label);
                        }
                    }
                }
            }
            
            // Ajouter les descriptions (pour les catégories)
            if (descriptions != null && !descriptions.isEmpty()) {
                for (CategoryDescriptionItem descriptionItem : descriptions) {
                    if (descriptionItem.getValeur() != null && !descriptionItem.getValeur().trim().isEmpty() &&
                        descriptionItem.getLangueCode() != null && !descriptionItem.getLangueCode().trim().isEmpty()) {
                        
                        Langue langue = langueRepository.findByCode(descriptionItem.getLangueCode());
                        if (langue != null) {
                            Description description = new Description();
                            description.setValeur(descriptionItem.getValeur().trim());
                            description.setEntity(newEntity);
                            description.setLangue(langue);
                            newEntity.getDescriptions().add(description);
                        }
                    }
                }
            }
            
            // Ajouter les autres champs spécifiques selon le type
            if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(entityType.getCode()) || 
                "CATEGORIE".equals(entityType.getCode())) {
                // Champs spécifiques aux catégories
                if (candidatCommentaire != null && !candidatCommentaire.trim().isEmpty()) {
                    newEntity.setCommentaire(candidatCommentaire.trim());
                }
                if (candidatBibliographie != null && !candidatBibliographie.trim().isEmpty()) {
                    newEntity.setBibliographie(candidatBibliographie.trim());
                }
                if (imagePrincipaleUrl != null && !imagePrincipaleUrl.trim().isEmpty()) {
                    newEntity.setImagePrincipaleUrl(imagePrincipaleUrl.trim());
                }
                // Les références bibliographiques peuvent être stockées dans rereferenceBibliographique
                if (referencesBibliographiques != null && !referencesBibliographiques.isEmpty()) {
                    String refs = String.join("; ", referencesBibliographiques);
                    newEntity.setRereferenceBibliographique(refs);
                }
            } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(entityType.getCode())) {
                if (typeDescription != null && !typeDescription.trim().isEmpty()) {
                    newEntity.setCommentaire(typeDescription.trim());
                }
            } else if (EntityConstants.ENTITY_TYPE_SERIES.equals(entityType.getCode()) || 
                       "SERIE".equals(entityType.getCode())) {
                if (serieDescription != null && !serieDescription.trim().isEmpty()) {
                    newEntity.setCommentaire(serieDescription.trim());
                }
            } else if (EntityConstants.ENTITY_TYPE_GROUP.equals(entityType.getCode()) || 
                       "GROUPE".equals(entityType.getCode())) {
                // Construire le commentaire avec description et période
                StringBuilder commentaireBuilder = new StringBuilder();
                if (groupDescription != null && !groupDescription.trim().isEmpty()) {
                    commentaireBuilder.append(groupDescription.trim());
                }
                if (groupPeriode != null && !groupPeriode.trim().isEmpty()) {
                    if (commentaireBuilder.length() > 0) {
                        commentaireBuilder.append("\n\nPériode: ").append(groupPeriode.trim());
                    } else {
                        commentaireBuilder.append("Période: ").append(groupPeriode.trim());
                    }
                }
                if (commentaireBuilder.length() > 0) {
                    newEntity.setCommentaire(commentaireBuilder.toString());
                }
                // Ajouter TPQ et TAQ
                if (groupTpq != null) {
                    newEntity.setTpq(groupTpq);
                }
                if (groupTaq != null) {
                    newEntity.setTaq(groupTaq);
                }
            } else if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(entityType.getCode())) {
                if (collectionDescription != null && !collectionDescription.trim().isEmpty()) {
                    newEntity.setCommentaire(collectionDescription.trim());
                }
                if (collectionPublique != null) {
                    newEntity.setPublique(collectionPublique);
                }
            }
            
            // Sauvegarder l'entité
            Entity savedEntity = entityRepository.save(newEntity);
            
            // Créer la relation avec le parent (référentiel)
            EntityRelation relation = new EntityRelation();
            relation.setParent(selectedParentEntity);
            relation.setChild(savedEntity);
            entityRelationRepository.save(relation);
            
            // Message de succès
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le candidat a été créé avec succès avec le statut PROPOSITION."));
            
            // Réinitialiser le formulaire
            resetWizardForm();
            currentStep = 0;
            
            // Mettre à jour le growl et le formulaire
            PrimeFaces.current().ajax().update(":growl", ":createCandidatForm");
            
            // Rediriger vers la liste des candidats après un court délai
            PrimeFaces.current().executeScript("setTimeout(function(){window.location.href='/candidats/candidats.xhtml';}, 1500);");
            
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du candidat", e);
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors de la sauvegarde : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl", ":createCandidatForm");
        }
    }

    /**
     * Prépare la validation d'un candidat (stocke le candidat et ouvre le dialogue)
     */
    public void prepareValidateCandidat(Candidat candidat) {
        this.candidatAValider = candidat;
    }
    
    /**
     * Prépare la suppression d'un candidat (stocke le candidat et ouvre le dialogue)
     */
    public void prepareDeleteCandidat(Candidat candidat) {
        this.candidatASupprimer = candidat;
    }
    
    /**
     * Valide un candidat après confirmation (change le statut à ACCEPTED)
     */
    public void validerCandidatConfirm() {
        if (candidatAValider != null) {
            validerCandidat(candidatAValider);
            candidatAValider = null; // Réinitialiser après validation
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucun candidat sélectionné pour validation."));
            PrimeFaces.current().ajax().update(":growl");
        }
    }
    
    /**
     * Supprime un candidat après confirmation
     */
    public void supprimerCandidatConfirm() {
        if (candidatASupprimer != null) {
            supprimerCandidat(candidatASupprimer);
            candidatASupprimer = null; // Réinitialiser après suppression
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucun candidat sélectionné pour suppression."));
            PrimeFaces.current().ajax().update(":growl");
        }
    }
    
    /**
     * Valide un candidat (change le statut à ACCEPTED)
     */
    public void validerCandidat(Candidat candidat) {
        try {
            if (candidat == null || candidat.getId() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Candidat invalide."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            Entity entity = entityRepository.findById(candidat.getId())
                .orElse(null);
            
            if (entity == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Entité introuvable."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            // Initialiser la liste des auteurs pour éviter les problèmes de lazy loading
            if (entity.getAuteurs() != null) {
                entity.getAuteurs().size(); // Force le chargement
            }
            
            entity.setStatut(EntityStatusEnum.ACCEPTED.name());
            
            // Ajouter l'utilisateur actuel dans la liste des auteurs s'il n'y est pas déjà
            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                // Initialiser la liste des auteurs si nécessaire
                if (entity.getAuteurs() == null) {
                    entity.setAuteurs(new ArrayList<>());
                }
                // Vérifier si l'utilisateur n'est pas déjà dans la liste
                boolean userAlreadyAuthor = entity.getAuteurs().stream()
                    .anyMatch(auteur -> auteur.getId().equals(currentUser.getId()));
                if (!userAlreadyAuthor) {
                    entity.getAuteurs().add(currentUser);
                }
            }
            
            entityRepository.save(entity);
            
            // Recharger les candidats pour mettre à jour l'affichage
            candidatsLoaded = false;
            chargerCandidats();
            
            String userName = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Utilisateur";
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le candidat a été validé par " + userName + "."));
            PrimeFaces.current().ajax().update(":growl, :candidatsForm");
        } catch (Exception e) {
            log.error("Erreur lors de la validation du candidat", e);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors de la validation : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    /**
     * Refuse un candidat (change le statut à REFUSED)
     */
    public void refuserCandidat(Candidat candidat) {
        try {
            if (candidat == null || candidat.getId() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Candidat invalide."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            Entity entity = entityRepository.findById(candidat.getId())
                .orElse(null);
            
            if (entity == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Entité introuvable."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            // Initialiser la liste des auteurs pour éviter les problèmes de lazy loading
            if (entity.getAuteurs() != null) {
                entity.getAuteurs().size(); // Force le chargement
            }
            
            entity.setStatut(EntityStatusEnum.REFUSED.name());
            
            // Ajouter l'utilisateur actuel dans la liste des auteurs s'il n'y est pas déjà
            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                // Initialiser la liste des auteurs si nécessaire
                if (entity.getAuteurs() == null) {
                    entity.setAuteurs(new ArrayList<>());
                }
                // Vérifier si l'utilisateur n'est pas déjà dans la liste
                boolean userAlreadyAuthor = entity.getAuteurs().stream()
                    .anyMatch(auteur -> auteur.getId().equals(currentUser.getId()));
                if (!userAlreadyAuthor) {
                    entity.getAuteurs().add(currentUser);
                }
            }
            
            entityRepository.save(entity);
            
            // Recharger les candidats pour mettre à jour l'affichage
            candidatsLoaded = false;
            chargerCandidats();
            
            String userName = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Utilisateur";
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le candidat a été refusé par " + userName + "."));
            PrimeFaces.current().ajax().update(":growl, :candidatsForm");
        } catch (Exception e) {
            log.error("Erreur lors du refus du candidat", e);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors du refus : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    /**
     * Supprime un candidat (supprime l'entité de la base de données)
     */
    public void supprimerCandidat(Candidat candidat) {
        try {
            if (candidat == null || candidat.getId() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Candidat invalide."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            Entity entity = entityRepository.findById(candidat.getId())
                .orElse(null);
            
            if (entity == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Entité introuvable."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            // Récupérer le nom de l'utilisateur avant suppression
            Utilisateur currentUser = loginBean.getCurrentUser();
            String userName = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Utilisateur";
            
            entityRepository.delete(entity);
            
            // Recharger les candidats pour mettre à jour l'affichage
            candidatsLoaded = false;
            chargerCandidats();
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le candidat a été supprimé par " + userName + "."));
            PrimeFaces.current().ajax().update(":growl, :candidatsForm");
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du candidat", e);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors de la suppression : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    public String visualiserCandidat(Candidat candidat) {
        candidatSelectionne = candidat;
        return "/candidats/view.xhtml?faces-redirect=true";
    }

    public String getCollectionLabel(Entity collection) {
        Optional<Label> existingLabel = collection.getLabels().stream()
                .filter(l -> l.getLangue() != null && l.getLangue().getCode().equalsIgnoreCase(searchBean.getLangSelected()))
                .findFirst();

        return existingLabel.isPresent() ? existingLabel.get().getNom() : collection.getCode();
    }
    
    /**
     * Vérifie si un objet est une instance de Entity
     * Utilisé dans les expressions EL où instanceof n'est pas supporté
     */
    public boolean isEntity(Object obj) {
        return obj != null && obj instanceof Entity;
    }
    
    /**
     * Vérifie si le nœud de l'arbre contient une Entity comme data
     * Gère le cas où node peut être un TreeNode ou directement la valeur de data
     */
    public boolean isNodeEntity(Object node) {
        if (node == null) {
            return false;
        }
        // Si node est directement une Entity
        if (node instanceof Entity) {
            return true;
        }
        // Si node est un TreeNode, vérifier son data
        if (node instanceof TreeNode) {
            Object data = ((TreeNode) node).getData();
            return data != null && data instanceof Entity;
        }
        // Si node est une String ou autre type, ce n'est pas une Entity
        return false;
    }
    
    /**
     * Récupère l'Entity depuis un nœud de l'arbre
     * Retourne null si le nœud ne contient pas une Entity
     */
    public Entity getEntityFromNode(Object node) {
        if (node == null) {
            return null;
        }
        // Si node est directement une Entity
        if (node instanceof Entity) {
            return (Entity) node;
        }
        // Si node est un TreeNode, récupérer son data
        if (node instanceof TreeNode) {
            Object data = ((TreeNode) node).getData();
            if (data instanceof Entity) {
                return (Entity) data;
            }
        }
        return null;
    }

    /**
     * Récupère la valeur d'affichage d'un nœud (nom de l'Entity, code de la collection, ou valeur par défaut)
     */
    public String getNodeDisplayValue(Object node) {
        Entity entity = getEntityFromNode(node);
        if (entity != null) {
            // Si c'est le nœud racine (collection), afficher le code
            if (node instanceof TreeNode) {
                TreeNode treeNode = (TreeNode) node;
                if (treeNode.getParent() == null) {
                    // C'est le nœud racine, afficher le code de la collection
                    return entity.getCode() != null ? entity.getCode() : entity.getNom();
                }
            }
            // Sinon, afficher le nom de l'entité
            return entity.getCode();
        }
        // Si ce n'est pas une Entity, retourner la représentation string du nœud
        if (node instanceof TreeNode) {
            Object data = ((TreeNode) node).getData();
            return data != null ? data.toString() : node.toString();
        }
        return node != null ? node.toString() : "";
    }
    
    /**
     * Vérifie si une collection est sélectionnée
     */
    public boolean isCollectionSelected() {
        return selectedCollectionId != null;
    }
    
    /**
     * Vérifie si un nœud est le nœud racine (a un parent null)
     */
    public boolean isRootNode(Object node) {
        if (node instanceof TreeNode) {
            return ((TreeNode) node).getParent() == null;
        }
        return false;
    }
    
    /**
     * Gère l'upload de l'image principale vers IIIF
     * Upload l'image vers le serveur IIIF et récupère l'URL pour l'enregistrer
     */
    public void handleImageUpload(FileUploadEvent event) {
        if (event == null || event.getFile() == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Aucun fichier sélectionné."));
            }
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        
        UploadedFile uploadedFile = event.getFile();
        
        try {
            log.info("Début de l'upload de l'image {} vers IIIF", uploadedFile.getFileName());
            
            // Convertir UploadedFile en MultipartFile pour le service IIIF
            MultipartFile multipartFile = new UploadedFileToMultipartFileAdapter(uploadedFile);
            
            // Uploader vers IIIF et récupérer l'URL
            String iiifUrl = iiifImageService.uploadImage(multipartFile);
            
            // Stocker l'URL dans imagePrincipaleUrl
            imagePrincipaleUrl = iiifUrl;
            
            log.info("Image uploadée avec succès. URL IIIF: {}", iiifUrl);
            
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "L'image a été uploadée avec succès vers IIIF. URL: " + iiifUrl));
            }
            
            PrimeFaces.current().ajax().update(":createCandidatForm:imageUploadContainer :growl");
            
        } catch (Exception e) {
            log.error("Erreur lors de l'upload de l'image vers IIIF", e);
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                String errorMessage = "Une erreur est survenue lors de l'upload de l'image vers IIIF";
                if (e.getMessage() != null) {
                    errorMessage += ": " + e.getMessage();
                }
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        errorMessage));
            }
            PrimeFaces.current().ajax().update(":growl");
        }
    }
    
    /**
     * Adapter pour convertir UploadedFile (PrimeFaces) en MultipartFile (Spring)
     */
    private static class UploadedFileToMultipartFileAdapter implements MultipartFile {
        private final UploadedFile uploadedFile;
        
        public UploadedFileToMultipartFileAdapter(UploadedFile uploadedFile) {
            this.uploadedFile = uploadedFile;
        }
        
        @Override
        public String getName() {
            return uploadedFile.getFileName();
        }
        
        @Override
        public String getOriginalFilename() {
            return uploadedFile.getFileName();
        }
        
        @Override
        public String getContentType() {
            return uploadedFile.getContentType();
        }
        
        @Override
        public boolean isEmpty() {
            return uploadedFile.getSize() == 0;
        }
        
        @Override
        public long getSize() {
            return uploadedFile.getSize();
        }
        
        @Override
        public byte[] getBytes() throws IOException {
            return uploadedFile.getContent();
        }
        
        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return uploadedFile.getInputStream();
        }
        
        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            throw new UnsupportedOperationException("transferTo not supported");
        }
    }
    
    /**
     * Supprime l'image principale
     */
    public void removeImage() {
        imagePrincipaleUrl = null;
        uploadedImageFile = null;
        PrimeFaces.current().ajax().update(":createCandidatForm:imageUploadContainer");
    }
    
    /**
     * Ajoute un nouveau label de catégorie depuis les champs de saisie
     */
    public void addLabelFromInput() {
        if (newLabelValue == null || newLabelValue.trim().isEmpty()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Le label est requis."));
            }
            return;
        }
        
        if (newLabelLangueCode == null || newLabelLangueCode.trim().isEmpty()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "La langue est requise."));
            }
            return;
        }
        
        // Vérifier si la langue est déjà utilisée
        if (isLangueAlreadyUsedIncandidatLabels(newLabelLangueCode, null)) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Cette langue est déjà utilisée pour un autre label."));
            }
            return;
        }
        
        // Vérifier que la langue n'est pas celle de l'étape 1
        if (selectedLangueCode != null && selectedLangueCode.equals(newLabelLangueCode)) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Cette langue est déjà utilisée pour le label principal (étape 1)."));
            }
            return;
        }
        
        if (candidatLabels == null) {
            candidatLabels = new ArrayList<>();
        }
        
        Langue langue = langueRepository.findByCode(newLabelLangueCode);
        CategoryLabelItem newItem = new CategoryLabelItem(
            newLabelValue.trim(), 
            newLabelLangueCode, 
            langue);
        candidatLabels.add(newItem);
        
        // Réinitialiser les champs de saisie
        newLabelValue = null;
        newLabelLangueCode = null;
    }
    
    /**
     * Supprime un label de catégorie de la liste
     */
    public void removeCandidatLabel(CategoryLabelItem labelItem) {
        if (candidatLabels != null) {
            candidatLabels.remove(labelItem);
        }
    }
    
    /**
     * Vérifie si une langue est déjà utilisée dans les labels de catégorie
     */
    public boolean isLangueAlreadyUsedIncandidatLabels(String langueCode, CategoryLabelItem currentItem) {
        if (candidatLabels == null || langueCode == null || langueCode.isEmpty()) {
            return false;
        }
        return candidatLabels.stream()
            .filter(item -> item != currentItem && item.getLangueCode() != null)
            .anyMatch(item -> item.getLangueCode().equals(langueCode));
    }
    
    /**
     * Obtient les langues disponibles pour un nouveau label (excluant celle de l'étape 1 et celles déjà utilisées)
     */
    public List<Langue> getAvailableLanguagesForNewLabel() {
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
     * Ajoute une nouvelle description de catégorie depuis les champs de saisie
     */
    public void addCandidatDescriptionFromInput() {
        if (newDescriptionValue == null || newDescriptionValue.trim().isEmpty()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "La description est requise."));
            }
            return;
        }
        
        if (newDescriptionLangueCode == null || newDescriptionLangueCode.trim().isEmpty()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "La langue est requise."));
            }
            return;
        }
        
        // Vérifier si la langue est déjà utilisée
        if (isLangueAlreadyUsedIndescriptions(newDescriptionLangueCode, null)) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Cette langue est déjà utilisée pour une autre description."));
            }
            return;
        }
        
        if (descriptions == null) {
            descriptions = new ArrayList<>();
        }
        
        Langue langue = langueRepository.findByCode(newDescriptionLangueCode);
        CategoryDescriptionItem newItem = new CategoryDescriptionItem(
            newDescriptionValue.trim(), 
            newDescriptionLangueCode, 
            langue);
        descriptions.add(newItem);
        
        // Réinitialiser les champs de saisie
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
    }
    
    /**
     * Supprime une description de catégorie de la liste
     */
    public void removeCandidatDescription(CategoryDescriptionItem descriptionItem) {
        if (descriptions != null) {
            descriptions.remove(descriptionItem);
        }
    }
    
    /**
     * Vérifie si une langue est déjà utilisée dans les descriptions de catégorie
     */
    public boolean isLangueAlreadyUsedIndescriptions(String langueCode, CategoryDescriptionItem currentItem) {
        if (descriptions == null || langueCode == null || langueCode.isEmpty()) {
            return false;
        }
        return descriptions.stream()
            .filter(item -> item != currentItem && item.getLangueCode() != null)
            .anyMatch(item -> item.getLangueCode().equals(langueCode));
    }
    
    /**
     * Obtient les langues disponibles pour une nouvelle description (toutes les langues disponibles, excluant seulement celles déjà utilisées)
     */
        public List<Langue> getAvailableLanguagesForNewDescription() {
        if (availableLanguages == null) {
            return new ArrayList<>();
        }
        return availableLanguages.stream()
            .filter(langue -> {
                // Exclure uniquement les langues déjà utilisées dans les descriptions (pour éviter les doublons)
                // La langue principale de l'étape 1 est incluse car on peut avoir une description dans la même langue
                return !isLangueAlreadyUsedIndescriptions(langue.getCode(), null);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Surcharge de getEntityTypeName pour accepter un Candidat
     */
    public String getEntityTypeName(Candidat candidat) {
        if (candidat == null || candidat.getTypeCode() == null) {
            return "";
        }
        String code = candidat.getTypeCode();
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
    
    /**
     * Surcharge de getCollectionLabel pour accepter un Candidat
     */
    public String getCollectionLabel(Candidat candidat) {
        if (candidat == null || candidat.getId() == null) {
            return "Aucune collection";
        }
        
        try {
            Optional<Entity> entityOpt = entityRepository.findById(candidat.getId());
            if (entityOpt.isPresent()) {
                Entity entity = entityOpt.get();
                
                // Trouver la collection parente
                List<Entity> parents = entityRelationRepository.findParentsByChild(entity);
                if (parents != null && !parents.isEmpty()) {
                    for (Entity parent : parents) {
                        if (parent.getEntityType() != null &&
                            EntityConstants.ENTITY_TYPE_COLLECTION.equals(parent.getEntityType().getCode())) {
                            // Charger les labels de la collection
                            if (parent.getLabels() != null) {
                                parent.getLabels().size(); // Force le chargement
                            }
                            return getCollectionLabel(parent);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la collection depuis le candidat", e);
        }
        
        return "Aucune collection";
    }
    
    /**
     * Obtient le code de l'entité à partir du candidat sélectionné
     */
    public String getEntityCodeFromCandidat() {
        if (candidatSelectionne == null || candidatSelectionne.getId() == null) {
            return "Non sélectionné";
        }
        
        try {
            Optional<Entity> entityOpt = entityRepository.findById(candidatSelectionne.getId());
            if (entityOpt.isPresent()) {
                return entityOpt.get().getCode();
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du code de l'entité", e);
        }
        
        return "Non sélectionné";
    }
    
    /**
     * Obtient le code de l'entité parente à partir du candidat sélectionné
     */
    public String getParentCodeFromCandidat() {
        if (candidatSelectionne == null || candidatSelectionne.getId() == null) {
            return "Non sélectionné";
        }
        
        try {
            Optional<Entity> entityOpt = entityRepository.findById(candidatSelectionne.getId());
            if (entityOpt.isPresent()) {
                Entity entity = entityOpt.get();
                
                // Trouver l'entité parente (non-collection)
                List<Entity> parents = entityRelationRepository.findParentsByChild(entity);
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
}

