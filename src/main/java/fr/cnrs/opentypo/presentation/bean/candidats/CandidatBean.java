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
import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.OpenThesoDialogBean;
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
import java.util.Arrays;
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

    @Inject
    private OpenThesoDialogBean openThesoDialogBean;

    @Inject
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

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
    private List<ReferenceOpentheso> airesCirculation = new ArrayList<>(); // Liste des aires de circulation
    private String selectedLangueCode;
    private Long selectedCollectionId;
    private Entity selectedParentEntity;
    private Long selectedDirectEntityId; // Entité directement rattachée à la collection
    private Entity currentEntity; // Entité créée à l'étape 1, utilisée dans les étapes suivantes
    
    // Liste des données pour les sélecteurs
    private List<EntityType> availableEntityTypes;
    private List<Langue> availableLanguages;
    private List<Entity> availableCollections;
    private List<Entity> availableReferences;
    private List<Entity> availableDirectEntities; // Entités directement rattachées à la collection
    
    // Arbre pour les référentiels et leurs enfants
    private TreeNode referenceTreeRoot;
    private TreeNode selectedTreeNode;
    
    // Index du wizard (0 = étape 1, 1 = étape 2, 2 = étape 3)
    private int currentStep = 0;



    @PostConstruct
    public void init() {
        chargerCandidats();
        loadAvailableEntityTypes();
        loadAvailableLanguages();
        availableCollections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION);
        availableDirectEntities = new ArrayList<>();
    }

    /**
     * Passe à l'étape suivante du wizard
     */
    public String nextStep() {
        log.info("nextStep() appelée - currentStep actuel: {}", currentStep);
        
        if (currentStep == 0) { // De l'étape 1 à l'étape 2
            log.info("Validation de l'étape 1...");
            // Validation manuelle
            if (validateStep1()) {
                // Créer l'entité dans la base de données avec les données de l'étape 1
                try {
                    currentEntity = createEntityFromStep1();
                    log.info("Entité créée à l'étape 1 avec l'ID: {}", currentEntity.getId());
                } catch (Exception e) {
                    log.error("Erreur lors de la création de l'entité à l'étape 1", e);
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Une erreur est survenue lors de la création de l'entité : " + e.getMessage()));
                    PrimeFaces.current().ajax().update(":growl");
                    return null;
                }
                currentStep++;
                log.info("Passage à l'étape 2 - currentStep = {}", currentStep);
            } else {
                log.warn("Validation échouée à l'étape 1, reste sur l'étape 1");
            }
        } else if (currentStep == 1) { // De l'étape 2 à l'étape 3
            log.info("Validation de l'étape 2...");
            // Validation manuelle
            if (validateStep2()) {
                // Récupérer l'entité parent depuis le nœud sélectionné
                Entity parent = (Entity) selectedTreeNode.getData();
                selectedParentEntity = entityRepository.findById(parent.getId()).orElse(null);
                
                // Créer la relation parent-enfant si l'entité de l'étape 1 existe
                if (currentEntity != null && currentEntity.getId() != null && selectedParentEntity != null) {
                    try {
                        // Recharger l'entité depuis la base pour éviter les problèmes de détachement
                        Entity refreshedEntity = entityRepository.findById(currentEntity.getId())
                            .orElse(null);
                        
                        if (refreshedEntity != null) {
                            // Vérifier si la relation existe déjà
                            boolean relationExists = entityRelationRepository.existsByParentAndChild(
                                selectedParentEntity.getId(), refreshedEntity.getId());
                            
                            if (!relationExists) {
                                // Créer la relation parent-enfant
                                EntityRelation relation = new EntityRelation();
                                relation.setParent(selectedParentEntity);
                                relation.setChild(refreshedEntity);
                                entityRelationRepository.save(relation);
                                
                                log.info("Relation créée entre parent (ID={}) et enfant (ID={})", 
                                    selectedParentEntity.getId(), refreshedEntity.getId());
                            } else {
                                log.info("Relation existe déjà entre parent (ID={}) et enfant (ID={})", 
                                    selectedParentEntity.getId(), refreshedEntity.getId());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Erreur lors de la création de la relation parent-enfant", e);
                        FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Erreur",
                                "Une erreur est survenue lors de la création de la relation : " + e.getMessage()));
                        PrimeFaces.current().ajax().update(":growl");
                        return null;
                    }
                } else {
                    log.warn("Impossible de créer la relation : currentEntity={}, selectedParentEntity={}", 
                        currentEntity != null ? currentEntity.getId() : null,
                        selectedParentEntity != null ? selectedParentEntity.getId() : null);
                }
                
                // Charger les données existantes depuis la base de données
                loadExistingStep3Data();
                
                currentStep++;
                log.info("Passage à l'étape 3 - currentStep = {}", currentStep);
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
        }
        
        return null; // Reste sur la même page avec mise à jour AJAX
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
    
    /**
     * Méthode appelée lors de l'accès à la page candidats
     * Recharge les données pour s'assurer qu'elles sont à jour
     */
    public void loadCandidatsPage() {
        log.debug("Chargement de la page candidats, rechargement des données");
        candidatsLoaded = false; // Invalider le cache
        chargerCandidats();
        
        // Vérifier les paramètres de requête pour afficher des messages
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String success = facesContext.getExternalContext().getRequestParameterMap().get("success");
            String error = facesContext.getExternalContext().getRequestParameterMap().get("error");
            
            if ("true".equals(success)) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "Le candidat a été créé avec succès. Vous pouvez le voir dans la liste des candidats en cours."));
                PrimeFaces.current().ajax().update(":growl");
            } else if ("true".equals(error)) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Une erreur est survenue lors de la sauvegarde."));
                PrimeFaces.current().ajax().update(":growl");
            }
        }
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
        
        // Réinitialiser l'arbre et la sélection de référence
        referenceTreeRoot = null;
        selectedTreeNode = null;
        selectedDirectEntityId = null;
        
        // Charger les entités directement rattachées
        loadDirectEntitiesForCollection();
        
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
        
        // Forcer la mise à jour du conteneur de l'arbre et du champ des entités directes
        PrimeFaces.current().ajax().update(":createCandidatForm:referenceTreeContainer :createCandidatForm:directEntitySelect");
    }
    
    /**
     * Charge les entités directement rattachées à la collection sélectionnée
     */
    public void loadDirectEntitiesForCollection() {
        log.debug("loadDirectEntitiesForCollection appelée - selectedCollectionId: {}", selectedCollectionId);
        
        // Réinitialiser la sélection
        selectedDirectEntityId = null;
        availableDirectEntities = new ArrayList<>();
        
        if (selectedCollectionId != null) {
            try {
                // Recharger la collection depuis la base pour éviter les problèmes de lazy loading
                Entity refreshedCollection = entityRepository.findById(selectedCollectionId)
                    .orElse(null);
                
                if (refreshedCollection == null) {
                    log.warn("Collection avec l'ID {} non trouvée", selectedCollectionId);
                    return;
                }
                
                // Charger toutes les entités directement rattachées à la collection (tous types confondus)
                List<Entity> allDirectEntities = entityRelationRepository.findChildrenByParent(refreshedCollection);
                
                // Filtrer selon l'authentification
                boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
                availableDirectEntities = allDirectEntities.stream()
                    .filter(e -> {
                        // Si l'utilisateur est authentifié, afficher toutes les entités
                        // Sinon, afficher uniquement les entités publiques
                        return isAuthenticated || (e.getPublique() != null && e.getPublique());
                    })
                    .collect(Collectors.toList());
                
                log.debug("Entités directement rattachées chargées pour la collection {}: {} entités (utilisateur authentifié: {})", 
                    refreshedCollection.getCode(), availableDirectEntities.size(), isAuthenticated);
            } catch (Exception e) {
                log.error("Erreur lors du chargement des entités directement rattachées à la collection", e);
                availableDirectEntities = new ArrayList<>();
            }
        } else {
            log.debug("Aucune collection sélectionnée, liste des entités directes vidée");
        }
    }
    
    /**
     * Récupère le label d'une entité pour l'affichage dans le selectOneMenu
     */
    public String getDirectEntityLabel(Entity entity) {
        if (entity == null) {
            return "";
        }
        // Essayer d'obtenir le label dans la langue sélectionnée
        if (selectedLangueCode != null && entity.getLabels() != null) {
            Optional<Label> labelOpt = entity.getLabels().stream()
                .filter(l -> selectedLangueCode.equals(l.getLangue().getCode()))
                .findFirst();
            if (labelOpt.isPresent() && labelOpt.get().getNom() != null && !labelOpt.get().getNom().trim().isEmpty()) {
                return entity.getCode() + " - " + labelOpt.get().getNom();
            }
        }
        // Sinon, on utilise le code uniquement
        return entity.getCode();
    }
    
    /**
     * Méthode appelée lors du changement de l'entité directe sélectionnée
     * Construit l'arbre à partir de la référence sélectionnée
     */
    public void onDirectEntityChange() {
        log.debug("onDirectEntityChange appelée - selectedDirectEntityId: {}", selectedDirectEntityId);
        
        // Réinitialiser l'arbre
        referenceTreeRoot = null;
        selectedTreeNode = null;
        
        if (selectedDirectEntityId != null && !selectedDirectEntityId.toString().trim().isEmpty()) {
            try {
                // Recharger l'entité depuis la base pour éviter les problèmes de lazy loading
                Entity selectedReference = entityRepository.findById(selectedDirectEntityId)
                    .orElse(null);
                
                if (selectedReference == null) {
                    log.warn("Référence avec l'ID {} non trouvée", selectedDirectEntityId);
                    return;
                }
                
                // Filtrer selon l'authentification
                boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
                
                // Vérifier si l'entité est publique ou si l'utilisateur est authentifié
                if (!isAuthenticated && (selectedReference.getPublique() == null || !selectedReference.getPublique())) {
                    log.debug("Référence {} non accessible (non publique et utilisateur non authentifié)", selectedReference.getCode());
                    return;
                }
                
                // Créer le nœud racine avec la référence sélectionnée
                String referenceCode = selectedReference.getCode() != null ? selectedReference.getCode() : "Référence";
                String referenceLabel = selectedReference.getNom() != null ? selectedReference.getNom() : referenceCode;
                DefaultTreeNode rootNode = new DefaultTreeNode(referenceLabel, null);
                rootNode.setData(selectedReference);
                referenceTreeRoot = rootNode;
                
                // Charger les enfants de la référence sélectionnée (catégories, groupes, etc.)
                loadChildrenRecursively(rootNode, selectedReference, isAuthenticated);
                
                log.debug("Arbre construit à partir de la référence {}: {} enfants chargés (utilisateur authentifié: {})", 
                    selectedReference.getCode(), rootNode.getChildCount(), isAuthenticated);
            } catch (Exception e) {
                log.error("Erreur lors de la construction de l'arbre à partir de la référence sélectionnée", e);
            }
        } else {
            // Si aucune référence n'est sélectionnée, réinitialiser l'arbre
            log.debug("Aucune référence sélectionnée, arbre réinitialisé");
            referenceTreeRoot = null;
            selectedTreeNode = null;
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
        // Aire de circulation : concaténer toutes les valeurs avec "; "
        String aireCirculationStr = "";
        if (entity.getAiresCirculation() != null && !entity.getAiresCirculation().isEmpty()) {
            aireCirculationStr = entity.getAiresCirculation().stream()
                .filter(ref -> "AIRE_CIRCULATION".equals(ref.getCode()))
                .map(ReferenceOpentheso::getValeur)
                .filter(v -> v != null && !v.isEmpty())
                .collect(Collectors.joining("; "));
        }
        candidat.setAireCirculation(aireCirculationStr);
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
        currentEntity = null; // Réinitialiser l'entité créée
        selectedEntityTypeId = null;
        entityCode = null;
        entityLabel = null;
        selectedLangueCode = searchBean.getLangSelected();
        selectedCollectionId = null;
        selectedDirectEntityId = null;
        availableReferences = new ArrayList<>();
        availableDirectEntities = new ArrayList<>();
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
        ateliers = new ArrayList<>();
        airesCirculation = new ArrayList<>();
        collectionDescription = null;
        collectionPublique = true;
        
        // Réinitialiser les champs du groupe
        groupPeriode = null;
        groupTpq = null;
        groupTaq = null;
    }
    
    /**
     * Charge les données existantes depuis currentEntity pour l'étape 3
     */
    private void loadExistingStep3Data() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        // Recharger l'entité depuis la base avec toutes ses relations
        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity == null) {
            return;
        }
        
        // Charger les labels existants (excluant le label principal de l'étape 1)
        candidatLabels = new ArrayList<>();
        if (refreshedEntity.getLabels() != null) {
            for (Label label : refreshedEntity.getLabels()) {
                // Exclure le label principal de l'étape 1
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
        
        // Charger les aires de circulation depuis la relation OneToMany
        if (refreshedEntity.getAiresCirculation() != null) {
            airesCirculation = refreshedEntity.getAiresCirculation().stream()
                .filter(ref -> "AIRE_CIRCULATION".equals(ref.getCode()))
                .collect(Collectors.toList());
        } else {
            airesCirculation = new ArrayList<>();
        }
        
        // Charger les champs spécifiques selon le type d'entité
        if (refreshedEntity.getEntityType() != null) {
            String typeCode = refreshedEntity.getEntityType().getCode();
            
            if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(typeCode)) {
                // Pour les collections, charger la description depuis DescriptionDetail si disponible
                // Note: collectionDescription et collectionPublique sont gérés différemment
                collectionPublique = refreshedEntity.getPublique();
            } else if (EntityConstants.ENTITY_TYPE_TYPE.equals(typeCode)) {
                typeDescription = refreshedEntity.getCommentaire(); // Utiliser commentaire pour la description du type
            }
        }
        
        // Charger les champs spécifiques au groupe
        groupTpq = refreshedEntity.getTpq();
        groupTaq = refreshedEntity.getTaq();
        if (refreshedEntity.getPeriode() != null) {
            groupPeriode = refreshedEntity.getPeriode().getValeur();
        }
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
    private List<String> ateliers = new ArrayList<>();
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
     * Crée une entité dans la base de données avec les données de l'étape 1
     * @return L'entité créée
     */
    private Entity createEntityFromStep1() {
        // Récupérer le type d'entité
        EntityType entityType = entityTypeRepository.findById(selectedEntityTypeId)
            .orElseThrow(() -> new IllegalStateException("Le type d'entité sélectionné n'existe pas."));
        
        // Créer la nouvelle entité avec seulement les données de l'étape 1
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
            labelPrincipal.setLangue(languePrincipale);
            labelPrincipal.setEntity(newEntity);
            List<Label> labels = new ArrayList<>();
            labels.add(labelPrincipal);
            newEntity.setLabels(labels);
        }
        
        // Sauvegarder l'entité
        Entity savedEntity = entityRepository.save(newEntity);
        
        log.info("Entité créée à l'étape 1: ID={}, Code={}, Nom={}", 
            savedEntity.getId(), savedEntity.getCode(), savedEntity.getNom());
        
        return savedEntity;
    }

    /**
     * Termine le processus de création de candidat et redirige vers la liste des candidats
     * Met à jour l'entité avec les valeurs finales : période, commentaire, références bibliographiques, bibliographie, TAQ, TPQ
     */
    public String terminerCandidat() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        try {
            // Vérifier que currentEntity existe
            if (currentEntity == null || currentEntity.getId() == null) {
                if (facesContext != null) {
                    facesContext.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "L'entité n'a pas été créée. Veuillez compléter les étapes précédentes."));
                    PrimeFaces.current().ajax().update(":growl");
                }
                return null;
            }
            
            // Recharger l'entité depuis la base pour éviter les problèmes de détachement
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity == null) {
                if (facesContext != null) {
                    facesContext.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "L'entité n'a pas été trouvée dans la base de données."));
                    PrimeFaces.current().ajax().update(":growl");
                }
                return null;
            }
            
            // Mettre à jour le commentaire
            if (candidatCommentaire != null) {
                refreshedEntity.setCommentaire(candidatCommentaire.trim());
            }
            
            // Mettre à jour la bibliographie
            if (candidatBibliographie != null) {
                refreshedEntity.setBibliographie(candidatBibliographie.trim());
            }
            
            // Mettre à jour les références bibliographiques (concaténées avec ';')
            if (referencesBibliographiques != null && !referencesBibliographiques.isEmpty()) {
                String refs = String.join("; ", referencesBibliographiques);
                refreshedEntity.setRereferenceBibliographique(refs);
            } else {
                refreshedEntity.setRereferenceBibliographique(null);
            }
            
            // Mettre à jour les ateliers (concaténées avec ';')
            if (ateliers != null && !ateliers.isEmpty()) {
                String ateliersStr = String.join("; ", ateliers);
                refreshedEntity.setAteliers(ateliersStr);
            } else {
                refreshedEntity.setAteliers(null);
            }
            
            // Mettre à jour TPQ
            if (groupTpq != null) {
                refreshedEntity.setTpq(groupTpq);
            }
            
            // Mettre à jour TAQ
            if (groupTaq != null) {
                refreshedEntity.setTaq(groupTaq);
            }
            
            // Mettre à jour la période
            // Si groupPeriode est une chaîne simple, on peut l'ajouter au commentaire
            // Si c'est géré via ReferenceOpentheso, on doit créer ou récupérer la référence
            if (groupPeriode != null && !groupPeriode.trim().isEmpty()) {
                // Pour l'instant, on ajoute la période au commentaire si elle n'est pas déjà présente
                // ou on peut créer une ReferenceOpentheso si nécessaire
                // Note: La période peut être gérée via le dialog OpenTheso avec le code "PERIODE"
                // Ici, on suppose que groupPeriode est une chaîne simple à ajouter au commentaire
                String currentCommentaire = refreshedEntity.getCommentaire();
                if (currentCommentaire == null || currentCommentaire.trim().isEmpty()) {
                    refreshedEntity.setCommentaire("Période: " + groupPeriode.trim());
                } else if (!currentCommentaire.contains("Période:")) {
                    refreshedEntity.setCommentaire(currentCommentaire + "\n\nPériode: " + groupPeriode.trim());
                }
            }
            
            // Sauvegarder l'entité mise à jour
            entityRepository.save(refreshedEntity);
            log.info("Entité mise à jour avec les valeurs finales: ID={}", refreshedEntity.getId());
            
            // Recharger la liste des candidats pour avoir les données à jour
            chargerCandidats();
            
            // Réinitialiser le formulaire
            resetWizardForm();
            currentStep = 0;
            
            // Rediriger vers la liste des candidats avec un paramètre de succès
            // Le message sera affiché dans loadCandidatsPage() en vérifiant le paramètre
            return "/candidats/candidats.xhtml?faces-redirect=true&success=true";
            
        } catch (Exception e) {
            log.error("Erreur lors de la finalisation du candidat", e);
            // Rediriger avec un paramètre d'erreur
            return "/candidats/candidats.xhtml?faces-redirect=true&error=true";
        }
    }
    
    /**
     * Sauvegarde le candidat en créant l'entité dans la base de données avec le statut PROPOSITION
     * (Méthode conservée pour compatibilité, mais l'enregistrement se fait maintenant au fur et à mesure)
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
            
            // Utiliser l'entité créée à l'étape 1 ou en créer une nouvelle si elle n'existe pas
            Entity newEntity;
            if (currentEntity != null && currentEntity.getId() != null) {
                // Recharger l'entité depuis la base pour éviter les problèmes de détachement
                newEntity = entityRepository.findById(currentEntity.getId())
                    .orElseThrow(() -> new IllegalStateException("L'entité créée à l'étape 1 n'existe plus."));
                log.info("Utilisation de l'entité existante créée à l'étape 1: ID={}", newEntity.getId());
            } else {
                // Créer une nouvelle entité (fallback si l'étape 1 n'a pas créé l'entité)
                log.warn("Aucune entité trouvée de l'étape 1, création d'une nouvelle entité");
                EntityType entityType = entityTypeRepository.findById(selectedEntityTypeId)
                    .orElseThrow(() -> new IllegalStateException("Le type d'entité sélectionné n'existe pas."));
                
                newEntity = new Entity();
                newEntity.setCode(entityCode.trim());
                newEntity.setNom(entityLabel.trim());
                newEntity.setEntityType(entityType);
                newEntity.setStatut(EntityStatusEnum.PROPOSITION.name());
                newEntity.setPublique(true);
                newEntity.setCreateDate(LocalDateTime.now());
                
                Utilisateur currentUser = loginBean.getCurrentUser();
                if (currentUser != null) {
                    newEntity.setCreateBy(currentUser.getEmail());
                    List<Utilisateur> auteurs = new ArrayList<>();
                    auteurs.add(currentUser);
                    newEntity.setAuteurs(auteurs);
                }
            }
            
            // Ajouter le label principal si pas déjà présent (de l'étape 1)
            if (newEntity.getLabels() == null || newEntity.getLabels().isEmpty()) {
                Langue languePrincipale = langueRepository.findByCode(selectedLangueCode);
                if (languePrincipale != null) {
                    Label labelPrincipal = new Label();
                    labelPrincipal.setNom(entityLabel.trim());
                    labelPrincipal.setEntity(newEntity);
                    labelPrincipal.setLangue(languePrincipale);
                    if (newEntity.getLabels() == null) {
                        newEntity.setLabels(new ArrayList<>());
                    }
                    newEntity.getLabels().add(labelPrincipal);
                }
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
            EntityType entityType = newEntity.getEntityType();
            if (entityType != null && (EntityConstants.ENTITY_TYPE_CATEGORY.equals(entityType.getCode()) || 
                "CATEGORIE".equals(entityType.getCode()))) {
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
            } else if (entityType != null && EntityConstants.ENTITY_TYPE_TYPE.equals(entityType.getCode())) {
                if (typeDescription != null && !typeDescription.trim().isEmpty()) {
                    newEntity.setCommentaire(typeDescription.trim());
                }
            } else if (entityType != null && (EntityConstants.ENTITY_TYPE_SERIES.equals(entityType.getCode()) || 
                       "SERIE".equals(entityType.getCode()))) {
                if (serieDescription != null && !serieDescription.trim().isEmpty()) {
                    newEntity.setCommentaire(serieDescription.trim());
                }
            } else if (entityType != null && (EntityConstants.ENTITY_TYPE_GROUP.equals(entityType.getCode()) || 
                       "GROUPE".equals(entityType.getCode()))) {
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
            } else if (entityType != null && EntityConstants.ENTITY_TYPE_COLLECTION.equals(entityType.getCode())) {
                if (collectionDescription != null && !collectionDescription.trim().isEmpty()) {
                    newEntity.setCommentaire(collectionDescription.trim());
                }
                if (collectionPublique != null) {
                    newEntity.setPublique(collectionPublique);
                }
            }
            
            // Utiliser la référence ReferenceOpentheso créée depuis le dialog OpenTheso
            if (openThesoDialogBean.getCreatedReference() != null) {
                try {
                    ReferenceOpentheso createdReference = openThesoDialogBean.getCreatedReference();
                    
                    // Vérifier le code pour déterminer quel champ de l'entité mettre à jour
                    String code = createdReference.getCode();
                    if ("PRODUCTION".equals(code)) {
                        newEntity.setProduction(createdReference);
                        log.info("Référence Production associée à l'entité: {}", createdReference.getId());
                    }
                    // Ajouter d'autres codes ici si nécessaire (PERIODE, etc.)
                    // else if ("PERIODE".equals(code)) {
                    //     newEntity.setPeriode(createdReference);
                    // }
                    
                } catch (Exception e) {
                    log.error("Erreur lors de l'association de la référence ReferenceOpentheso", e);
                    // Ne pas bloquer la sauvegarde du candidat en cas d'erreur
                }
            }
            
            // Sauvegarder l'entité (mise à jour si elle existe déjà, création sinon)
            Entity savedEntity = entityRepository.save(newEntity);
            
            // Créer la relation avec le parent (référentiel) si elle n'existe pas déjà
            if (selectedParentEntity != null) {
                // Vérifier si la relation existe déjà
                boolean relationExists = entityRelationRepository.existsByParentAndChild(
                    selectedParentEntity.getId(), savedEntity.getId());
                if (!relationExists) {
                    EntityRelation relation = new EntityRelation();
                    relation.setParent(selectedParentEntity);
                    relation.setChild(savedEntity);
                    entityRelationRepository.save(relation);
                }
            }
            
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

    public boolean hasProduction() {
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

    public boolean hasAireCirculation() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return false;
        }

        try {
            // Recharger l'entité pour avoir la liste à jour
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null && refreshedEntity.getAiresCirculation() != null) {
                // Filtrer pour ne garder que celles avec le code AIRE_CIRCULATION
                return refreshedEntity.getAiresCirculation().stream()
                    .anyMatch(ref -> "AIRE_CIRCULATION".equals(ref.getCode()));
            }
            return false;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de l'aire de circulation", e);
            return false;
        }
    }

    /**
     * Récupère la liste des aires de circulation pour l'entité courante
     * Charge depuis la base de données si la liste n'est pas encore chargée
     */
    public List<ReferenceOpentheso> getAiresCirculation() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return new ArrayList<>();
        }

        try {
            // Recharger l'entité pour avoir la liste à jour
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null && refreshedEntity.getAiresCirculation() != null) {
                // Filtrer pour ne garder que celles avec le code AIRE_CIRCULATION
                airesCirculation = refreshedEntity.getAiresCirculation().stream()
                    .filter(ref -> "AIRE_CIRCULATION".equals(ref.getCode()))
                    .collect(Collectors.toList());
            } else {
                airesCirculation = new ArrayList<>();
            }
            return airesCirculation != null ? airesCirculation : new ArrayList<>();
        } catch (Exception e) {
            log.error("Erreur lors du chargement des aires de circulation", e);
            return new ArrayList<>();
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
        if (currentEntity == null || currentEntity.getId() == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "L'entité n'a pas encore été créée. Veuillez d'abord compléter les étapes précédentes."));
            }
            return;
        }
        
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
        
        // Recharger l'entité depuis la base pour éviter les problèmes de détachement
        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "L'entité n'a pas été trouvée dans la base de données."));
            }
            return;
        }
        
        // Vérifier si la langue est déjà utilisée dans la base de données
        if (refreshedEntity.getLabels() != null) {
            boolean langueAlreadyUsed = refreshedEntity.getLabels().stream()
                .anyMatch(label -> label.getLangue() != null && 
                    label.getLangue().getCode() != null &&
                    label.getLangue().getCode().equals(newLabelLangueCode));
            
            if (langueAlreadyUsed) {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                if (facesContext != null) {
                    facesContext.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Attention",
                            "Cette langue est déjà utilisée pour un autre label."));
                }
                return;
            }
        }
        
        // Créer et sauvegarder le label dans la base de données
        Langue langue = langueRepository.findByCode(newLabelLangueCode);
        if (langue == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "La langue sélectionnée n'a pas été trouvée."));
            }
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
        
        // Sauvegarder l'entité (le label sera sauvegardé grâce au cascade)
        entityRepository.save(refreshedEntity);
        
        // Mettre à jour la liste locale pour l'affichage
        if (candidatLabels == null) {
            candidatLabels = new ArrayList<>();
        }
        CategoryLabelItem newItem = new CategoryLabelItem(
            newLabelValue.trim(), 
            newLabelLangueCode, 
            langue);
        candidatLabels.add(newItem);
        
        // Réinitialiser les champs de saisie
        newLabelValue = null;
        newLabelLangueCode = null;
        
        // Message de succès
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le label a été ajouté avec succès."));
        }
    }
    
    /**
     * Supprime un label de catégorie de la liste et de la base de données
     */
    public void removeCandidatLabel(CategoryLabelItem labelItem) {
        if (currentEntity == null || currentEntity.getId() == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "L'entité n'a pas encore été créée."));
            }
            return;
        }
        
        // Recharger l'entité depuis la base
        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity == null || refreshedEntity.getLabels() == null) {
            return;
        }
        
        // Trouver et supprimer le label correspondant dans la base de données
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
        
        // Message de succès
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le label a été supprimé avec succès."));
        }
    }
    
    /**
     * Vérifie si une langue est déjà utilisée dans les labels de catégorie (dans la liste locale et la base de données)
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
                        // Exclure le label principal de l'étape 1
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
     * Ajoute une nouvelle description de catégorie depuis les champs de saisie et la sauvegarde dans la base de données
     */
    public void addCandidatDescriptionFromInput() {
        if (currentEntity == null || currentEntity.getId() == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "L'entité n'a pas encore été créée. Veuillez d'abord compléter les étapes précédentes."));
            }
            return;
        }
        
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
        
        // Recharger l'entité depuis la base pour éviter les problèmes de détachement
        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "L'entité n'a pas été trouvée dans la base de données."));
            }
            return;
        }
        
        // Vérifier si la langue est déjà utilisée dans la base de données
        if (refreshedEntity.getDescriptions() != null) {
            boolean langueAlreadyUsed = refreshedEntity.getDescriptions().stream()
                .anyMatch(desc -> desc.getLangue() != null && 
                    desc.getLangue().getCode() != null &&
                    desc.getLangue().getCode().equals(newDescriptionLangueCode));
            
            if (langueAlreadyUsed) {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                if (facesContext != null) {
                    facesContext.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Attention",
                            "Cette langue est déjà utilisée pour une autre description."));
                }
                return;
            }
        }
        
        // Créer et sauvegarder la description dans la base de données
        Langue langue = langueRepository.findByCode(newDescriptionLangueCode);
        if (langue == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "La langue sélectionnée n'a pas été trouvée."));
            }
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
        
        // Sauvegarder l'entité (la description sera sauvegardée grâce au cascade)
        entityRepository.save(refreshedEntity);
        
        // Mettre à jour la liste locale pour l'affichage
        if (descriptions == null) {
            descriptions = new ArrayList<>();
        }
        CategoryDescriptionItem newItem = new CategoryDescriptionItem(
            newDescriptionValue.trim(), 
            newDescriptionLangueCode, 
            langue);
        descriptions.add(newItem);
        
        // Réinitialiser les champs de saisie
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        
        // Message de succès
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "La description a été ajoutée avec succès."));
        }
    }
    
    /**
     * Supprime une description de catégorie de la liste et de la base de données
     */
    public void removeCandidatDescription(CategoryDescriptionItem descriptionItem) {
        if (currentEntity == null || currentEntity.getId() == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "L'entité n'a pas encore été créée."));
            }
            return;
        }
        
        // Recharger l'entité depuis la base
        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity == null || refreshedEntity.getDescriptions() == null) {
            return;
        }
        
        // Trouver et supprimer la description correspondante dans la base de données
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
        
        // Message de succès
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "La description a été supprimée avec succès."));
        }
    }
    
    /**
     * Vérifie si une langue est déjà utilisée dans les descriptions de catégorie (dans la liste locale et la base de données)
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
                // Note: collectionDescription pourrait être stocké dans DescriptionDetail ou commentaire
                // Pour l'instant, on utilise commentaire
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
     * Sauvegarde automatiquement les champs du groupe (période, TPQ, TAQ) dans la base de données
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
                // Note: groupPeriode est géré via ReferenceOpentheso, pas directement dans Entity
                entityRepository.save(refreshedEntity);
                log.debug("Champs du groupe sauvegardés pour l'entité ID: {}", refreshedEntity.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde des champs du groupe", e);
        }
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

    /**
     * Met à jour le champ candidatProduction avec le terme sélectionné depuis OpenTheso
     */
    /**
     * Met à jour le champ production depuis OpenTheso après validation
     * Recharge l'entité depuis la base de données pour avoir la référence à jour
     */
    public void updateProductionFromOpenTheso() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        try {
            // Recharger l'entité depuis la base de données pour avoir la référence production à jour
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null && refreshedEntity.getProduction() != null) {
                // Mettre à jour le champ candidatProduction avec la valeur de la référence
                candidatProduction = refreshedEntity.getProduction().getValeur();
                // Mettre à jour currentEntity pour garder la synchronisation
                currentEntity = refreshedEntity;
                log.info("Champ production mis à jour pour l'entité ID={}: {}", currentEntity.getId(), candidatProduction);
            } else if (openThesoDialogBean.getCreatedReference() != null) {
                // Fallback : utiliser la référence créée si l'entité n'a pas encore été rechargée
                candidatProduction = openThesoDialogBean.getCreatedReference().getValeur();
            } else if (openThesoDialogBean.getSelectedConcept() != null) {
                // Fallback : utiliser le concept sélectionné
                candidatProduction = openThesoDialogBean.getSelectedConcept().getSelectedTerm();
            }
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du champ production depuis OpenTheso", e);
        }
    }

    /**
     * Met à jour le champ aire de circulation depuis OpenTheso après validation
     * Recharge la liste des aires de circulation depuis la base de données
     */
    public void updateAireCirculationFromOpenTheso() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return;
        }
        
        try {
            // Recharger l'entité depuis la base de données pour avoir la liste à jour
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null) {
                currentEntity = refreshedEntity;
                // Filtrer pour ne garder que celles avec le code AIRE_CIRCULATION
                if (currentEntity.getAiresCirculation() != null) {
                    airesCirculation = currentEntity.getAiresCirculation().stream()
                        .filter(ref -> "AIRE_CIRCULATION".equals(ref.getCode()))
                        .collect(Collectors.toList());
                } else {
                    airesCirculation = new ArrayList<>();
                }
                log.info("Liste des aires de circulation mise à jour pour l'entité ID={}: {} valeurs", 
                    currentEntity.getId(), airesCirculation.size());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du champ aire de circulation depuis OpenTheso", e);
        }
    }

    /**
     * Supprime une aire de circulation spécifique de l'entité et la référence associée
     * @param referenceId ID de la référence ReferenceOpentheso à supprimer
     */
    public void deleteAireCirculation(Long referenceId) {
        if (currentEntity == null || currentEntity.getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucune entité sélectionnée."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        
        if (referenceId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucune référence sélectionnée pour suppression."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        
        try {
            // Recharger l'entité depuis la base de données
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity != null && refreshedEntity.getAiresCirculation() != null) {
                // Trouver la référence à supprimer
                ReferenceOpentheso referenceToDelete = refreshedEntity.getAiresCirculation().stream()
                    .filter(ref -> ref.getId().equals(referenceId) && "AIRE_CIRCULATION".equals(ref.getCode()))
                    .findFirst()
                    .orElse(null);
                
                if (referenceToDelete != null) {
                    // Retirer la référence de la liste (cascade supprimera automatiquement grâce à orphanRemoval)
                    refreshedEntity.getAiresCirculation().remove(referenceToDelete);
                    entityRepository.save(refreshedEntity);
                    
                    // Mettre à jour currentEntity et la liste locale
                    currentEntity = refreshedEntity;
                    airesCirculation = currentEntity.getAiresCirculation().stream()
                        .filter(ref -> "AIRE_CIRCULATION".equals(ref.getCode()))
                        .collect(Collectors.toList());
                    
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Succès",
                            "L'aire de circulation a été supprimée avec succès."));
                    PrimeFaces.current().ajax().update(":growl");
                    
                    log.info("Aire de circulation supprimée (ID={}) pour l'entité ID={}", 
                        referenceId, currentEntity.getId());
                } else {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Attention",
                            "La référence sélectionnée n'existe pas ou n'appartient pas à cette entité."));
                    PrimeFaces.current().ajax().update(":growl");
                }
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Aucune aire de circulation à supprimer."));
                PrimeFaces.current().ajax().update(":growl");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'aire de circulation", e);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors de la suppression : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    /**
     * Prépare la suppression d'une aire de circulation (pour le dialog de confirmation)
     */
    private Long aireCirculationToDeleteId;

    public void prepareDeleteAireCirculation(Long referenceId) {
        this.aireCirculationToDeleteId = referenceId;
    }

    public void deleteAireCirculation() {
        deleteAireCirculation(aireCirculationToDeleteId);
        aireCirculationToDeleteId = null;
    }

    /**
     * Supprime l'entité créée à l'étape 1 et toutes ses relations si l'utilisateur confirme l'abandon
     */
    public void abandonnerProposition() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        if (currentEntity == null || currentEntity.getId() == null) {
            // Aucune entité à supprimer
            log.info("Aucune entité à supprimer lors de l'abandon");
            resetWizardForm();
            return;
        }
        
        try {
            Long entityId = currentEntity.getId();
            log.info("Suppression de l'entité créée à l'étape 1: ID={}", entityId);
            
            // Recharger l'entité depuis la base pour éviter les problèmes de détachement
            Entity entityToDelete = entityRepository.findById(entityId).orElse(null);
            
            if (entityToDelete != null) {
                // Supprimer toutes les relations où cette entité est enfant
                List<EntityRelation> relationsAsChild = entityRelationRepository.findByChild(entityToDelete);
                if (relationsAsChild != null && !relationsAsChild.isEmpty()) {
                    entityRelationRepository.deleteAll(relationsAsChild);
                    log.debug("{} relations supprimées (entité comme enfant)", relationsAsChild.size());
                }
                
                // Supprimer toutes les relations où cette entité est parent
                List<EntityRelation> relationsAsParent = entityRelationRepository.findByParent(entityToDelete);
                if (relationsAsParent != null && !relationsAsParent.isEmpty()) {
                    entityRelationRepository.deleteAll(relationsAsParent);
                    log.debug("{} relations supprimées (entité comme parent)", relationsAsParent.size());
                }
                
                // Supprimer l'entité elle-même
                entityRepository.delete(entityToDelete);
                log.info("Entité supprimée avec succès: ID={}", entityId);
                
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "La proposition a été abandonnée et supprimée."));
            } else {
                log.warn("Entité avec l'ID {} non trouvée pour suppression", entityId);
            }
            
            // Réinitialiser le formulaire
            resetWizardForm();
            currentEntity = null;
            
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'entité", e);
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors de la suppression : " + e.getMessage()));
        }
    }
}

