package fr.cnrs.opentypo.presentation.bean.candidats;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.SearchBean;
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
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.event.NodeSelectEvent;

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

    private List<Candidat> candidats = new ArrayList<>();
    private Candidat candidatSelectionne;
    private Candidat nouveauCandidat;
    private int activeTabIndex = 0; // 0 = en cours, 1 = validés, 2 = refusés
    
    // Champs pour l'étape 1 du formulaire
    private Long selectedEntityTypeId;
    private String entityCode;
    private String entityLabel;
    private String selectedLangueCode;
    private Long selectedCollectionId;
    private Entity selectedReference;
    
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
        selectedReference = null;
        
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
        
        if (!isValid) {
            facesContext.validationFailed();
        }
        
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
        
        if (!isValid) {
            facesContext.validationFailed();
        }
        
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
        selectedReference = null;
        availableReferences = new ArrayList<>();
        referenceTreeRoot = null;
        selectedTreeNode = null;
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
}

