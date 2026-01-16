package fr.cnrs.opentypo.presentation.bean.users;

import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.presentation.bean.NotificationBean;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Groupe;
import fr.cnrs.opentypo.domain.entity.UserPermission;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.GroupeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.application.service.UtilisateurService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DualListModel;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Named("userManagementBean")
@SessionScoped
@Getter
@Setter
public class UserManagementBean implements Serializable {

    @Inject
    private fr.cnrs.opentypo.presentation.bean.UserBean currentUserBean;

    @Inject
    private fr.cnrs.opentypo.presentation.bean.LoginBean loginBean;

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Inject
    private GroupeRepository groupeRepository;

    @Inject
    private UtilisateurService utilisateurService;

    @Inject
    private NotificationBean notificationBean;

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository userPermissionRepository;

    private List<User> users = new ArrayList<>();
    private User selectedUser;
    private User newUser;
    private boolean isEditMode = false;
    private List<Groupe> availableGroups = new ArrayList<>(); // Liste des groupes disponibles depuis la base
    private Long selectedGroupeId; // ID du groupe sélectionné dans le formulaire
    private Groupe selectedGroupe;
    
    // Pour le PickList (ancien système - à remplacer)
    private DualListModel<String> pickListModel;
    
    // Pour la sélection des référentiels autorisés
    private List<SelectItem> hierarchicalCollectionItems = new ArrayList<>(); // Liste hiérarchique collections/référentiels (ancien système)
    private List<String> selectedReferenceCodes = new ArrayList<>(); // Codes des référentiels/collections sélectionnés
    
    // Nouveau modèle pour le dataTable
    private List<CollectionReferenceItem> collectionReferenceItems = new ArrayList<>(); // Liste structurée pour le dataTable
    private String searchFilter = ""; // Filtre de recherche
    
    // Liste des référentiels uniquement (pour administrateur référentiel)
    private List<Entity> allReferences = new ArrayList<>(); // Tous les référentiels publics
    private List<String> selectedReferenceCodesOnly = new ArrayList<>(); // Codes des référentiels sélectionnés (sans collections)
    
    /**
     * Classe interne pour représenter une collection avec ses référentiels
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CollectionReferenceItem implements Serializable {
        private Entity collection;
        private List<Entity> references = new ArrayList<>();
        private boolean collectionSelected = false;
        private List<Boolean> referencesSelected = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        // Vérifier que l'utilisateur est administrateur
        if (!isAdminTechnique()) {
            redirectToUnauthorized();
            return;
        }
        chargerUsers();
        chargerGroupes();
        chargerCollectionsEtReferences();
        initialiserPickList();
    }
    
    /**
     * Charge les collections et référentiels depuis la base de données
     * et crée une liste hiérarchique pour la sélection (ancien système)
     */
    public void chargerCollectionsEtReferences() {
        hierarchicalCollectionItems = new ArrayList<>();
        collectionReferenceItems = new ArrayList<>();
        allReferences = new ArrayList<>();
        try {
            // Charger les collections
            List<Entity> collections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION);
            collections = collections.stream()
                .filter(c -> c != null && c.getPublique() != null && c.getPublique())
                .collect(Collectors.toList());
            
            // Pour chaque collection
            for (Entity collection : collections) {
                if (collection == null || collection.getCode() == null) {
                    continue;
                }
                
                // Créer un item pour le dataTable
                CollectionReferenceItem item = new CollectionReferenceItem();
                item.setCollection(collection);
                item.setCollectionSelected(false);
                
                // Récupérer les références rattachées à cette collection
                List<Entity> collectionReferences = entityRelationRepository.findChildrenByParentAndType(
                    collection, EntityConstants.ENTITY_TYPE_REFERENCE);
                
                // Filtrer pour ne garder que les références publiques
                collectionReferences = collectionReferences.stream()
                    .filter(r -> r != null && r.getPublique() != null && r.getPublique())
                    .collect(Collectors.toList());
                
                item.setReferences(collectionReferences);
                // Initialiser la liste des sélections de références
                List<Boolean> refsSelected = new ArrayList<>();
                for (int i = 0; i < collectionReferences.size(); i++) {
                    refsSelected.add(false);
                }
                item.setReferencesSelected(refsSelected);
                
                collectionReferenceItems.add(item);
                
                // Ajouter les références à la liste globale des référentiels
                allReferences.addAll(collectionReferences);
                
                // Ancien système pour compatibilité
                String collectionValue = "COL:" + collection.getCode();
                String collectionDisplayCode = collection.getCode();
                if (collectionDisplayCode.length() > 50) {
                    collectionDisplayCode = collectionDisplayCode.substring(0, 47) + "...";
                }
                hierarchicalCollectionItems.add(new SelectItem(collectionValue, "📁 " + collectionDisplayCode));
                
                for (Entity reference : collectionReferences) {
                    if (reference != null && reference.getCode() != null) {
                        String value = "REF:" + collection.getCode() + ":" + reference.getCode();
                        String displayCode = reference.getCode();
                        if (displayCode.length() > 50) {
                            displayCode = displayCode.substring(0, 47) + "...";
                        }
                        hierarchicalCollectionItems.add(new SelectItem(value, "\u00A0\u00A0\u00A0\u00A0📖 " + displayCode));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des collections et référentiels", e);
            hierarchicalCollectionItems = new ArrayList<>();
            collectionReferenceItems = new ArrayList<>();
            allReferences = new ArrayList<>();
        }
    }
    
    /**
     * Retourne la liste filtrée des collections et référentiels pour le dataTable
     */
    public List<CollectionReferenceItem> getFilteredCollectionReferenceItems() {
        if (searchFilter == null || searchFilter.trim().isEmpty()) {
            return collectionReferenceItems;
        }
        
        String filterLower = searchFilter.toLowerCase().trim();
        List<CollectionReferenceItem> filtered = new ArrayList<>();
        
        for (CollectionReferenceItem item : collectionReferenceItems) {
            if (item.getCollection() == null) {
                continue;
            }
            
            // Vérifier si la collection correspond au filtre
            boolean collectionMatches = item.getCollection().getCode() != null &&
                item.getCollection().getCode().toLowerCase().contains(filterLower);
            
            // Vérifier si au moins une référence correspond au filtre
            boolean hasMatchingReference = false;
            if (item.getReferences() != null) {
                for (Entity ref : item.getReferences()) {
                    if (ref != null && ref.getCode() != null &&
                        ref.getCode().toLowerCase().contains(filterLower)) {
                        hasMatchingReference = true;
                        break;
                    }
                }
            }
            
            // Si la collection ou une référence correspond, inclure l'item
            if (collectionMatches || hasMatchingReference) {
                filtered.add(item);
            }
        }
        
        return filtered;
    }
    
    /**
     * Retourne le message à afficher quand le dataTable est vide
     */
    public String getEmptyTableMessage() {
        if (searchFilter == null || searchFilter.trim().isEmpty()) {
            return "Aucune collection disponible";
        } else {
            return "Aucun résultat trouvé pour votre recherche : \"" + searchFilter + "\"";
        }
    }
    
    /**
     * Retourne le message à afficher quand le dataTable des référentiels est vide
     */
    public String getEmptyReferencesTableMessage() {
        if (searchFilter == null || searchFilter.trim().isEmpty()) {
            return "Aucun référentiel disponible";
        } else {
            return "Aucun résultat trouvé pour votre recherche : \"" + searchFilter + "\"";
        }
    }
    
    /**
     * Vérifie si le groupe sélectionné est "Administrateur technique"
     */
    public boolean isAdministrateurTechnique() {
        if (selectedGroupe == null) {
            return false;
        }
        boolean isAdminTech = "Administrateur technique".equalsIgnoreCase(selectedGroupe.getNom()) ||
                "Administrateur".equalsIgnoreCase(selectedGroupe.getNom());
        log.debug("Vérification groupe '{}' -> isAdministrateurTechnique: {}", selectedGroupe.getNom(), isAdminTech);
        return isAdminTech;
    }
    
    /**
     * Getter pour l'expression EL (isAdministrateurTechnique -> administrateurTechnique)
     */
    public boolean isAdministrateurTechniqueGetter() {
        return isAdministrateurTechnique();
    }
    
    /**
     * Vérifie si le panel des référentiels doit être visible
     * Visible uniquement pour "Éditeur" et "Lecteur"
     */
    public boolean isReferencesPanelVisible() {
        if (selectedGroupe == null) {
            return false;
        }
        boolean visible = "Éditeur".equalsIgnoreCase(selectedGroupe.getNom()) || "Lecteur".equalsIgnoreCase(selectedGroupe.getNom());
        log.debug("Vérification groupe '{}' -> isReferencesPanelVisible: {}", selectedGroupe.getNom(), visible);
        return visible;
    }
    
    /**
     * Vérifie si le groupe sélectionné est "Administrateur Référentiel"
     */
    public boolean isAdministrateurReferentiel() {
        if (selectedGroupe == null) {
            return false;
        }
        return "Administrateur Référentiel".equalsIgnoreCase(selectedGroupe.getNom());
    }
    
    /**
     * Getter pour l'expression EL (isAdministrateurReferentiel -> administrateurReferentiel)
     */
    public boolean isAdministrateurReferentielGetter() {
        return isAdministrateurReferentiel();
    }
    
    /**
     * Retourne la liste filtrée des référentiels uniquement (pour administrateur référentiel)
     */
    public List<Entity> getFilteredReferences() {
        if (searchFilter == null || searchFilter.trim().isEmpty()) {
            return allReferences;
        }
        
        String filterLower = searchFilter.toLowerCase().trim();
        return allReferences.stream()
            .filter(ref -> ref != null && ref.getCode() != null && 
                    ref.getCode().toLowerCase().contains(filterLower))
            .collect(Collectors.toList());
    }
    
    /**
     * Gère la sélection/désélection d'un référentiel (pour administrateur référentiel)
     */
    public void toggleReferenceSelectionOnly(Entity reference) {
        if (reference == null || reference.getCode() == null) {
            return;
        }
        
        String refCode = reference.getCode();
        if (selectedReferenceCodesOnly.contains(refCode)) {
            selectedReferenceCodesOnly.remove(refCode);
        } else {
            selectedReferenceCodesOnly.add(refCode);
        }
    }
    
    /**
     * Vérifie si un référentiel est sélectionné (pour administrateur référentiel)
     */
    public boolean referenceSelected(Entity reference) {
        if (reference == null || reference.getCode() == null) {
            return false;
        }
        return selectedReferenceCodesOnly.contains(reference.getCode());
    }
    
    /**
     * Gère la sélection/désélection d'une collection
     */
    public void toggleCollectionSelection(CollectionReferenceItem item) {
        if (item == null || item.getCollection() == null || item.getCollection().getCode() == null) {
            return;
        }
        
        // Trouver l'item correspondant dans la liste originale
        CollectionReferenceItem originalItem = null;
        for (CollectionReferenceItem original : collectionReferenceItems) {
            if (original != null && original.getCollection() != null && 
                original.getCollection().getCode() != null &&
                original.getCollection().getCode().equals(item.getCollection().getCode())) {
                originalItem = original;
                break;
            }
        }
        
        if (originalItem == null) {
            return;
        }
        
        // Inverser la sélection
        boolean newValue = !originalItem.isCollectionSelected();
        originalItem.setCollectionSelected(newValue);
        
        // Si la collection est sélectionnée, sélectionner toutes ses références
        if (originalItem.isCollectionSelected() && originalItem.getReferences() != null) {
            // S'assurer que la liste des sélections a la bonne taille
            if (originalItem.getReferencesSelected() == null) {
                originalItem.setReferencesSelected(new ArrayList<>());
            }
            
            // Ajuster la taille de la liste si nécessaire
            while (originalItem.getReferencesSelected().size() < originalItem.getReferences().size()) {
                originalItem.getReferencesSelected().add(false);
            }
            
            // Sélectionner toutes les références
            for (int i = 0; i < originalItem.getReferences().size(); i++) {
                originalItem.getReferencesSelected().set(i, true);
            }
        } else if (!originalItem.isCollectionSelected() && originalItem.getReferences() != null && originalItem.getReferencesSelected() != null) {
            // Si la collection est désélectionnée, désélectionner toutes ses références
            for (int i = 0; i < originalItem.getReferencesSelected().size(); i++) {
                originalItem.getReferencesSelected().set(i, false);
            }
        }
        
        // Mettre à jour selectedReferenceCodes
        updateSelectedReferenceCodes();
    }
    
    /**
     * Gère la sélection/désélection d'une référence
     */
    public void toggleReferenceSelection(CollectionReferenceItem item, int referenceIndex) {
        if (item == null || item.getCollection() == null || item.getCollection().getCode() == null ||
            referenceIndex < 0) {
            return;
        }
        
        // Trouver l'item correspondant dans la liste originale
        CollectionReferenceItem originalItem = null;
        for (CollectionReferenceItem original : collectionReferenceItems) {
            if (original != null && original.getCollection() != null && 
                original.getCollection().getCode() != null &&
                original.getCollection().getCode().equals(item.getCollection().getCode())) {
                originalItem = original;
                break;
            }
        }
        
        if (originalItem == null || originalItem.getReferencesSelected() == null ||
            referenceIndex >= originalItem.getReferencesSelected().size()) {
            return;
        }
        
        // Inverser la sélection de la référence
        boolean newValue = !originalItem.getReferencesSelected().get(referenceIndex);
        originalItem.getReferencesSelected().set(referenceIndex, newValue);
        
        // Si toutes les références sont sélectionnées, sélectionner la collection
        // Sinon, désélectionner la collection
        if (originalItem.getReferences() != null && originalItem.getReferencesSelected() != null) {
            boolean allSelected = true;
            for (Boolean selected : originalItem.getReferencesSelected()) {
                if (!selected) {
                    allSelected = false;
                    break;
                }
            }
            originalItem.setCollectionSelected(allSelected);
        }
        
        // Mettre à jour selectedReferenceCodes
        updateSelectedReferenceCodes();
    }
    
    /**
     * Met à jour selectedReferenceCodes à partir des sélections dans collectionReferenceItems
     */
    private void updateSelectedReferenceCodes() {
        selectedReferenceCodes = new ArrayList<>();
        
        for (CollectionReferenceItem item : collectionReferenceItems) {
            if (item == null || item.getCollection() == null) {
                continue;
            }
            
            if (item.isCollectionSelected()) {
                // Si la collection est sélectionnée, ajouter la collection
                selectedReferenceCodes.add("COL:" + item.getCollection().getCode());
            } else if (item.getReferences() != null && item.getReferencesSelected() != null) {
                // Sinon, ajouter uniquement les références sélectionnées individuellement
                for (int i = 0; i < item.getReferences().size() && i < item.getReferencesSelected().size(); i++) {
                    if (item.getReferencesSelected().get(i) && item.getReferences().get(i) != null) {
                        Entity reference = item.getReferences().get(i);
                        selectedReferenceCodes.add("REF:" + item.getCollection().getCode() + ":" + reference.getCode());
                    }
                }
            }
        }
    }
    
    /**
     * Initialise le PickList avec des données d'exemple (ancien système)
     */
    private void initialiserPickList() {
        List<String> source = new ArrayList<>();
        source.add("Référentiel 1");
        source.add("Référentiel 2");
        source.add("Référentiel 3");
        source.add("Référentiel 4");
        
        List<String> target = new ArrayList<>();
        pickListModel = new DualListModel<>(source, target);
    }

    /**
     * Vérifie si l'utilisateur actuel est un administrateur
     * 
     * @return true si l'utilisateur est administrateur, false sinon
     */
    private boolean isAdminTechnique() {
        return loginBean != null && loginBean.isAdminTechnique();
    }

    /**
     * Redirige vers la page d'accueil avec un message d'erreur si l'utilisateur n'est pas autorisé
     */
    private void redirectToUnauthorized() {
        try {
            jakarta.faces.context.FacesContext facesContext = jakarta.faces.context.FacesContext.getCurrentInstance();
            if (facesContext != null) {
                notificationBean.showError("Accès refusé", 
                    "Seuls les administrateurs peuvent accéder à la gestion des utilisateurs.");
                String redirectUrl = facesContext.getExternalContext().getRequestContextPath() + "/index.xhtml?unauthorized=true";
                facesContext.getExternalContext().redirect(redirectUrl);
                facesContext.responseComplete();
            }
        } catch (Exception e) {
            // Ignorer les erreurs de redirection
        }
    }

    /**
     * Charge la liste des groupes disponibles depuis la base de données
     */
    public void chargerGroupes() {
        try {
            availableGroups = groupeRepository.findAll();
        } catch (Exception e) {
            availableGroups = new ArrayList<>();
            notificationBean.showError("Erreur", "Erreur lors du chargement des groupes : " + e.getMessage());
        }
    }
    
    /**
     * Listener appelé quand le groupe est changé
     */
    public void onGroupeChange() {
        log.info("=== onGroupeChange appelé ===");
        log.info("selectedGroupeId: {}", selectedGroupeId);
        
        // Mettre à jour selectedGroupe à partir de selectedGroupeId
        if (selectedGroupeId != null && selectedGroupeId > 0) {
            try {
                Optional<Groupe> groupeOpt = groupeRepository.findById(selectedGroupeId);
                if (groupeOpt.isPresent()) {
                    selectedGroupe = groupeOpt.get();
                    log.info("Groupe mis à jour: {} (ID: {})", selectedGroupe.getNom(), selectedGroupeId);
                } else {
                    selectedGroupe = null;
                    log.warn("Aucun groupe trouvé avec l'ID: {}", selectedGroupeId);
                }
            } catch (Exception e) {
                selectedGroupe = null;
                log.error("Erreur lors de la recherche du groupe", e);
            }
        } else {
            selectedGroupe = null;
            log.info("selectedGroupeId est null ou 0 - selectedGroupe mis à null");
        }
        
        // Forcer la mise à jour des panels
        PrimeFaces.current().ajax().update(":userForm:referencesPanel");
    }

    /**
     * Retourne la liste des groupes disponibles
     * 
     * @return Liste des groupes
     */
    public List<Groupe> getAvailableGroups() {
        if (availableGroups.isEmpty()) {
            chargerGroupes();
        }
        return availableGroups;
    }

    public void chargerUsers() {
        if (!isAdminTechnique()) {
            redirectToUnauthorized();
            return;
        }
        try {
            List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
            users = utilisateurs.stream()
                .map(this::convertToUser)
                .collect(Collectors.toList());
        } catch (Exception e) {
            users = new ArrayList<>();
            notificationBean.showError("Erreur", "Erreur lors du chargement des utilisateurs : " + e.getMessage());
        }
    }

    public void initNouveauUser() {
        if (!isAdminTechnique()) {
            redirectToUnauthorized();
            return;
        }
        jakarta.faces.context.FacesContext facesContext = jakarta.faces.context.FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String userIdParam = facesContext.getExternalContext().getRequestParameterMap().get("userId");
            if (userIdParam != null && !userIdParam.isEmpty()) {
                try {
                    Long userId = Long.parseLong(userIdParam);
                    Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(userId);
                    if (utilisateurOpt.isPresent()) {
                        initEditUser(convertToUser(utilisateurOpt.get()));
                        return;
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        newUser = new User();
        newUser.setCreatedBy(currentUserBean.getUsername() != null ? currentUserBean.getUsername() : "SYSTEM");
        newUser.setActive(true);
        newUser.setRole(User.Role.VIEWER);
        selectedGroupeId = null; // Réinitialiser la sélection du groupe
        selectedGroupe = null;
        selectedReferenceCodes = new ArrayList<>(); // Réinitialiser les référentiels sélectionnés
        selectedReferenceCodesOnly = new ArrayList<>(); // Réinitialiser les référentiels sélectionnés (admin référentiel)
        isEditMode = false;
        // S'assurer que les groupes sont chargés
        if (availableGroups.isEmpty()) {
            chargerGroupes();
        }
        // Charger les collections et référentiels
        chargerCollectionsEtReferences();
        // Réinitialiser le PickList
        initialiserPickList();
    }

    public void initEditUser(User user) {
        newUser = new User();
        newUser.setId(user.getId());
        newUser.setUsername(user.getUsername());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(""); // Ne pas afficher le mot de passe
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setRole(user.getRole());
        newUser.setActive(user.isActive());
        newUser.setDateCreation(user.getDateCreation());
        newUser.setCreatedBy(user.getCreatedBy());
        isEditMode = true;
        // S'assurer que les groupes sont chargés
        if (availableGroups.isEmpty()) {
            chargerGroupes();
        }
        // Charger le groupe actuel de l'utilisateur pour la modification
        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(user.getId());
            if (utilisateurOpt.isPresent() && utilisateurOpt.get().getGroupe() != null) {
                selectedGroupeId = utilisateurOpt.get().getGroupe().getId();
                selectedGroupe = utilisateurOpt.get().getGroupe();
            } else {
                selectedGroupeId = null;
                selectedGroupe = null;
            }
        } catch (Exception e) {
            selectedGroupeId = null;
            selectedGroupe = null;
        }
        // Charger les collections et référentiels
        chargerCollectionsEtReferences();
        // Charger les référentiels sélectionnés depuis la base de données
        chargerPermissionsUtilisateur(user.getId());
        // Appliquer les permissions chargées aux items du dataTable (seulement si ce n'est pas un admin référentiel)
        if (!isAdministrateurReferentiel() && selectedReferenceCodes != null && !selectedReferenceCodes.isEmpty()) {
            appliquerPermissionsAuxItems();
        }
        // Réinitialiser le PickList (ancien système)
        initialiserPickList();
    }

    public void sauvegarderUser() {
        // Vérifier que l'utilisateur est administrateur
        if (!isAdminTechnique()) {
            notificationBean.showErrorWithUpdate("Accès refusé", 
                "Seuls les administrateurs peuvent gérer les utilisateurs.", 
                ":growl, :userForm");
            return;
        }

        // Validation complète des champs
        String email = newUser.getEmail() != null ? newUser.getEmail().trim() : "";
        String firstName = newUser.getFirstName() != null ? newUser.getFirstName().trim() : "";
        String lastName = newUser.getLastName() != null ? newUser.getLastName().trim() : "";
        String password = newUser.getPassword() != null ? newUser.getPassword().trim() : "";

        // Validation email
        if (email.isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "L'email est obligatoire.", ":growl, :userForm");
            return;
        }
        
        // Validation format email
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!email.matches(emailPattern)) {
            notificationBean.showErrorWithUpdate("Erreur de validation", 
                "Le format de l'email est invalide. Veuillez saisir une adresse email valide (exemple: utilisateur@domaine.com).", 
                ":growl, :userForm");
            return;
        }
        
        if (email.length() > 255) {
            notificationBean.showErrorWithUpdate("Erreur de validation", 
                "L'email ne peut pas dépasser 255 caractères.", 
                ":growl, :userForm");
            return;
        }

        // Validation prénom
        if (firstName.isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le prénom est obligatoire.", ":growl, :userForm");
            return;
        }
        
        if (firstName.length() < 2) {
            notificationBean.showErrorWithUpdate("Erreur de validation", 
                "Le prénom doit contenir au moins 2 caractères.", 
                ":growl, :userForm");
            return;
        }
        
        if (firstName.length() > 100) {
            notificationBean.showErrorWithUpdate("Erreur de validation", 
                "Le prénom ne peut pas dépasser 100 caractères.", 
                ":growl, :userForm");
            return;
        }

        // Validation nom
        if (lastName.isEmpty()) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le nom est obligatoire.", ":growl, :userForm");
            return;
        }
        
        if (lastName.length() < 2) {
            notificationBean.showErrorWithUpdate("Erreur de validation", 
                "Le nom doit contenir au moins 2 caractères.", 
                ":growl, :userForm");
            return;
        }
        
        if (lastName.length() > 100) {
            notificationBean.showErrorWithUpdate("Erreur de validation", 
                "Le nom ne peut pas dépasser 100 caractères.", 
                ":growl, :userForm");
            return;
        }

        // Validation mot de passe
        if (!isEditMode) {
            if (password.isEmpty()) {
                notificationBean.showErrorWithUpdate("Erreur de validation", 
                    "Le mot de passe est obligatoire pour un nouvel utilisateur.", 
                    ":growl, :userForm");
                return;
            }
            
            if (password.length() < 6) {
                notificationBean.showErrorWithUpdate("Erreur de validation", 
                    "Le mot de passe doit contenir au moins 6 caractères.", 
                    ":growl, :userForm");
                return;
            }
        } else {
            // En mode édition, si un mot de passe est fourni, il doit respecter les règles
            if (!password.isEmpty() && password.length() < 6) {
                notificationBean.showErrorWithUpdate("Erreur de validation", 
                    "Le mot de passe doit contenir au moins 6 caractères.", 
                    ":growl, :userForm");
                return;
            }
        }

        // Validation groupe
        if (selectedGroupe == null) {
            notificationBean.showErrorWithUpdate("Erreur de validation", "Le groupe est obligatoire.", ":growl, :userForm");
            return;
        }

        try {
            if (!isEditMode) {
                // Création d'un nouvel utilisateur
                if (utilisateurRepository.existsByEmail(newUser.getEmail().trim())) {
                    notificationBean.showErrorWithUpdate("Erreur", 
                        "Un utilisateur avec cet email existe déjà.", 
                        ":growl, :userForm");
                    return;
                }

                if (newUser.getPassword() == null || newUser.getPassword().trim().isEmpty()) {
                    notificationBean.showErrorWithUpdate("Erreur", 
                        "Le mot de passe est requis pour un nouvel utilisateur.", 
                        ":growl, :userForm");
                    return;
                }

                // Créer l'entité Utilisateur
                Utilisateur utilisateur = new Utilisateur();
                utilisateur.setNom(newUser.getLastName().trim());
                utilisateur.setPrenom(newUser.getFirstName().trim());
                utilisateur.setEmail(newUser.getEmail().trim());
                utilisateur.setPasswordHash(utilisateurService.encodePassword(newUser.getPassword()));
                utilisateur.setGroupe(selectedGroupe);
                utilisateur.setActive(newUser.isActive());
                utilisateur.setCreateBy(currentUserBean.getUsername() != null ? currentUserBean.getUsername() : "SYSTEM");
                utilisateur.setCreateDate(LocalDateTime.now());

                utilisateur = utilisateurRepository.save(utilisateur);
                
                // Gérer les permissions (référentiels autorisés)
                sauvegarderPermissions(utilisateur);
                
                notificationBean.showSuccessWithUpdate("Succès", 
                    "L'utilisateur a été créé avec succès.", 
                    ":growl, :userForm");
            } else {
                // Modification d'un utilisateur existant
                Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(newUser.getId());
                
                if (utilisateurOpt.isEmpty()) {
                    notificationBean.showErrorWithUpdate("Erreur", 
                        "L'utilisateur à modifier n'existe pas.", 
                        ":growl, :userForm");
                    return;
                }

                Utilisateur utilisateur = utilisateurOpt.get();

                // Vérifier si l'email est unique (sauf pour l'utilisateur actuel)
                if (!utilisateur.getEmail().equals(newUser.getEmail().trim())) {
                    if (utilisateurRepository.existsByEmail(newUser.getEmail().trim())) {
                        notificationBean.showErrorWithUpdate("Erreur", 
                            "Un utilisateur avec cet email existe déjà.", 
                            ":growl, :userForm");
                        return;
                    }
                }

                // Mettre à jour les champs
                utilisateur.setNom(newUser.getLastName().trim());
                utilisateur.setPrenom(newUser.getFirstName().trim());
                utilisateur.setEmail(newUser.getEmail().trim());
                utilisateur.setGroupe(selectedGroupe);
                utilisateur.setActive(newUser.isActive());

                // Mettre à jour le mot de passe seulement si un nouveau mot de passe est fourni
                if (newUser.getPassword() != null && !newUser.getPassword().trim().isEmpty()) {
                    utilisateur.setPasswordHash(utilisateurService.encodePassword(newUser.getPassword()));
                }

                utilisateur = utilisateurRepository.save(utilisateur);
                
                // Gérer les permissions (référentiels autorisés)
                sauvegarderPermissions(utilisateur);
                
                notificationBean.showSuccessWithUpdate("Succès", 
                    "L'utilisateur a été modifié avec succès.", 
                    ":growl, :userForm");
            }

            // Recharger la liste des utilisateurs
            chargerUsers();

            // Rediriger vers la liste après un court délai pour permettre l'affichage du message
            PrimeFaces.current().executeScript("setTimeout(function() { window.location.href='/users/users.xhtml'; }, 1500);");

        } catch (Exception e) {
            notificationBean.showErrorWithUpdate("Erreur", 
                "Une erreur s'est produite lors de la sauvegarde : " + e.getMessage(), 
                ":growl, :userForm");
        }
    }

    public void supprimerUser(User user) {
        // Vérifier que l'utilisateur est administrateur
        if (!isAdminTechnique()) {
            notificationBean.showErrorWithUpdate("Accès refusé", 
                "Seuls les administrateurs peuvent supprimer des utilisateurs.", 
                ":growl, :usersForm");
            return;
        }

        if (user == null || user.getId() == null) {
            notificationBean.showErrorWithUpdate("Erreur", "Aucun utilisateur sélectionné pour la suppression.", ":growl, :usersForm");
            return;
        }

        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(user.getId());
            if (utilisateurOpt.isPresent()) {
                Utilisateur utilisateur = utilisateurOpt.get();
                
                // Vérifier si l'utilisateur n'est pas l'utilisateur actuellement connecté
                if (currentUserBean.getUsername() != null && 
                    utilisateur.getEmail().equals(currentUserBean.getUsername())) {
                    notificationBean.showErrorWithUpdate("Erreur", 
                        "Vous ne pouvez pas supprimer votre propre compte.", 
                        ":growl, :usersForm");
                    return;
                }

                utilisateurRepository.delete(utilisateur);
                notificationBean.showSuccessWithUpdate("Succès", 
                    "L'utilisateur " + utilisateur.getPrenom() + " " + utilisateur.getNom() + " a été supprimé avec succès.", 
                    ":growl, :usersForm");
                
                // Recharger la liste des utilisateurs
                chargerUsers();
            } else {
                notificationBean.showErrorWithUpdate("Erreur", 
                    "L'utilisateur à supprimer n'existe pas.", 
                    ":growl, :usersForm");
            }
        } catch (Exception e) {
            notificationBean.showErrorWithUpdate("Erreur", 
                "Erreur lors de la suppression : " + e.getMessage(), 
                ":growl, :usersForm");
        }
        PrimeFaces.current().ajax().update(":growl, :usersForm");
    }

    public void toggleUserActive(User user) {
        // Vérifier que l'utilisateur est administrateur
        if (!isAdminTechnique()) {
            notificationBean.showErrorWithUpdate("Accès refusé", 
                "Seuls les administrateurs peuvent modifier le statut des utilisateurs.", 
                ":growl, :usersForm");
            PrimeFaces.current().ajax().update(":growl, :usersForm");
            return;
        }

        if (user == null || user.getId() == null) {
            notificationBean.showErrorWithUpdate("Erreur", 
                "Aucun utilisateur sélectionné.", 
                ":growl, :usersForm");
            PrimeFaces.current().ajax().update(":growl, :usersForm");
            return;
        }

        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(user.getId());
            if (utilisateurOpt.isEmpty()) {
                notificationBean.showErrorWithUpdate("Erreur", 
                    "L'utilisateur n'existe pas.", 
                    ":growl, :usersForm");
                PrimeFaces.current().ajax().update(":growl, :usersForm");
                return;
            }

            Utilisateur utilisateur = utilisateurOpt.get();
            boolean newActiveState = !(utilisateur.getActive() != null && utilisateur.getActive());
            utilisateur.setActive(newActiveState);
            utilisateurRepository.save(utilisateur);

            notificationBean.showSuccessWithUpdate("Succès", 
                "Le statut de l'utilisateur a été " + (newActiveState ? "activé" : "désactivé") + " avec succès.", 
                ":growl, :usersForm");
            
            chargerUsers(); // Recharger la liste
            PrimeFaces.current().ajax().update(":growl, :usersForm");
        } catch (Exception e) {
            notificationBean.showErrorWithUpdate("Erreur", 
                "Erreur lors de la modification du statut : " + e.getMessage(), 
                ":growl, :usersForm");
            PrimeFaces.current().ajax().update(":growl, :usersForm");
        }
    }

    public String getRoleLabel(User.Role role) {
        if (role == null) return "";
        switch (role) {
            case ADMIN: return GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel();
            case EDITOR: return GroupEnum.EDITEUR.getLabel();
            case VIEWER: return GroupEnum.LECTEUR.getLabel();
            default: return role.toString();
        }
    }

    /**
     * Convertit une entité Utilisateur en DTO User
     */
    private User convertToUser(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        User user = new User();
        user.setId(utilisateur.getId());
        user.setEmail(utilisateur.getEmail() != null ? utilisateur.getEmail() : "");
        user.setUsername(utilisateur.getEmail() != null ? utilisateur.getEmail() : ""); // Utiliser l'email comme username
        user.setFirstName(utilisateur.getPrenom() != null ? utilisateur.getPrenom() : "");
        user.setLastName(utilisateur.getNom() != null ? utilisateur.getNom() : "");
        user.setPassword(""); // Ne pas exposer le mot de passe
        user.setRole(getRoleFromGroupe(utilisateur.getGroupe()));
        user.setActive(utilisateur.getActive() != null ? utilisateur.getActive() : true); // Utiliser le champ actif de l'entité
        user.setDateCreation(utilisateur.getCreateDate());
        user.setCreatedBy(utilisateur.getCreateBy() != null ? utilisateur.getCreateBy() : "SYSTEM");
        return user;
    }

    /**
     * Convertit un rôle User.Role en nom de groupe
     */
    private String getGroupeNomFromRole(User.Role role) {
        if (role == null) return GroupEnum.LECTEUR.getLabel();
        switch (role) {
            case ADMIN: return GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel();
            case EDITOR: return GroupEnum.EDITEUR.getLabel();
            case VIEWER: return GroupEnum.LECTEUR.getLabel();
            default: return GroupEnum.LECTEUR.getLabel();
        }
    }

    /**
     * Convertit un groupe en rôle User.Role
     */
    private User.Role getRoleFromGroupe(Groupe groupe) {
        if (groupe == null || groupe.getNom() == null) return User.Role.VIEWER;
        String nom = groupe.getNom();
        if (GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel().equalsIgnoreCase(nom)) {
            return User.Role.ADMIN;
        } else if (GroupEnum.EDITEUR.getLabel().equalsIgnoreCase(nom)) {
            return User.Role.EDITOR;
        } else {
            return User.Role.VIEWER;
        }
    }

    /**
     * Charge les permissions existantes d'un utilisateur et les met dans selectedReferenceCodes ou selectedReferenceCodesOnly
     */
    private void chargerPermissionsUtilisateur(Long userId) {
        selectedReferenceCodes = new ArrayList<>();
        selectedReferenceCodesOnly = new ArrayList<>();
        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(userId);
            if (utilisateurOpt.isPresent()) {
                Utilisateur utilisateur = utilisateurOpt.get();
                List<UserPermission> permissions = userPermissionRepository.findByUtilisateur(utilisateur);
                
                // Vérifier si l'utilisateur est administrateur référentiel
                boolean isAdminRef = utilisateur.getGroupe() != null && 
                    "Administrateur Référentiel".equalsIgnoreCase(utilisateur.getGroupe().getNom());
                
                if (isAdminRef) {
                    // Pour administrateur référentiel, charger uniquement les codes des référentiels
                    for (UserPermission permission : permissions) {
                        if (permission.getEntity() != null) {
                            Entity entity = permission.getEntity();
                            if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entity.getEntityType().getCode()) &&
                                entity.getCode() != null) {
                                selectedReferenceCodesOnly.add(entity.getCode());
                            }
                        }
                    }
                } else {
                    // Pour les autres groupes, utiliser le format avec collections
                    for (UserPermission permission : permissions) {
                        if (permission.getEntity() != null) {
                            Entity entity = permission.getEntity();
                            if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(entity.getEntityType().getCode())) {
                                // C'est une collection
                                selectedReferenceCodes.add("COL:" + entity.getCode());
                            } else if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entity.getEntityType().getCode())) {
                                // C'est une référence - trouver sa collection parente
                                List<Entity> parents = entityRelationRepository.findParentsByChild(entity);
                                Entity parentCollection = parents.stream()
                                    .filter(p -> EntityConstants.ENTITY_TYPE_COLLECTION.equals(p.getEntityType().getCode()))
                                    .findFirst()
                                    .orElse(null);
                                
                                if (parentCollection != null) {
                                    selectedReferenceCodes.add("REF:" + parentCollection.getCode() + ":" + entity.getCode());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des permissions pour l'utilisateur ID: {}", userId, e);
            selectedReferenceCodes = new ArrayList<>();
            selectedReferenceCodesOnly = new ArrayList<>();
        }
    }
    
    /**
     * Applique les permissions chargées aux items du dataTable
     */
    private void appliquerPermissionsAuxItems() {
        if (selectedReferenceCodes == null || selectedReferenceCodes.isEmpty()) {
            return;
        }
        
        for (CollectionReferenceItem item : collectionReferenceItems) {
            if (item == null || item.getCollection() == null) {
                continue;
            }
            
            String collectionCode = item.getCollection().getCode();
            String collectionValue = "COL:" + collectionCode;
            
            // Vérifier si la collection est sélectionnée
            boolean collectionSelected = selectedReferenceCodes.contains(collectionValue);
            item.setCollectionSelected(collectionSelected);
            
            // Vérifier les références sélectionnées
            if (item.getReferences() != null && item.getReferencesSelected() != null) {
                for (int i = 0; i < item.getReferences().size() && i < item.getReferencesSelected().size(); i++) {
                    Entity reference = item.getReferences().get(i);
                    if (reference != null && reference.getCode() != null) {
                        String refValue = "REF:" + collectionCode + ":" + reference.getCode();
                        boolean refSelected = selectedReferenceCodes.contains(refValue);
                        item.getReferencesSelected().set(i, refSelected);
                    }
                }
                
                // Si toutes les références sont sélectionnées, sélectionner aussi la collection
                if (!collectionSelected) {
                    boolean allRefsSelected = true;
                    for (Boolean selected : item.getReferencesSelected()) {
                        if (!selected) {
                            allRefsSelected = false;
                            break;
                        }
                    }
                    if (allRefsSelected && !item.getReferencesSelected().isEmpty()) {
                        item.setCollectionSelected(true);
                    }
                }
            }
        }
    }

    /**
     * Sauvegarde les permissions (référentiels autorisés) pour un utilisateur
     */
    private void sauvegarderPermissions(Utilisateur utilisateur) {
        try {
            // Supprimer les anciennes permissions
            List<UserPermission> existingPermissions = userPermissionRepository.findByUtilisateur(utilisateur);
            for (UserPermission permission : existingPermissions) {
                userPermissionRepository.delete(permission);
            }

            // Vérifier si l'utilisateur est administrateur référentiel
            boolean isAdminRef = utilisateur.getGroupe() != null && 
                "Administrateur Référentiel".equalsIgnoreCase(utilisateur.getGroupe().getNom());
            
            if (isAdminRef) {
                // Pour administrateur référentiel, utiliser selectedReferenceCodesOnly
                if (selectedReferenceCodesOnly != null && !selectedReferenceCodesOnly.isEmpty()) {
                    for (String referenceCode : selectedReferenceCodesOnly) {
                        if (referenceCode == null || referenceCode.trim().isEmpty()) {
                            continue;
                        }
                        
                        Optional<Entity> entityOpt = entityRepository.findByCode(referenceCode.trim());
                        if (entityOpt.isPresent()) {
                            Entity entity = entityOpt.get();
                            // Vérifier que c'est bien une référence
                            if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entity.getEntityType().getCode())) {
                                UserPermission permission = new UserPermission();
                                permission.setUtilisateur(utilisateur);
                                permission.setEntity(entity);
                                UserPermission.UserPermissionId id = new UserPermission.UserPermissionId();
                                id.setUserId(utilisateur.getId());
                                id.setEntityId(entity.getId());
                                permission.setId(id);
                                userPermissionRepository.save(permission);
                            }
                        }
                    }
                }
                return; // Sortir de la méthode pour les administrateurs référentiels
            }

            // Pour les autres groupes, utiliser selectedReferenceCodes (format avec collections)
            if (selectedReferenceCodes != null && !selectedReferenceCodes.isEmpty()) {
                for (String referenceCode : selectedReferenceCodes) {
                    if (referenceCode == null || referenceCode.trim().isEmpty()) {
                        continue;
                    }
                    
                    Entity entity = null;
                    
                    // Parser le code pour déterminer si c'est une collection ou une référence
                    if (referenceCode.startsWith("COL:")) {
                        // C'est une collection
                        String collectionCode = referenceCode.substring(4);
                        entity = entityRepository.findByCode(collectionCode).orElse(null);
                        
                        // Si une collection est sélectionnée, ajouter aussi toutes ses références
                        if (entity != null) {
                            // Créer la permission pour la collection
                            UserPermission permission = new UserPermission();
                            permission.setUtilisateur(utilisateur);
                            permission.setEntity(entity);
                            UserPermission.UserPermissionId id = new UserPermission.UserPermissionId();
                            id.setUserId(utilisateur.getId());
                            id.setEntityId(entity.getId());
                            permission.setId(id);
                            userPermissionRepository.save(permission);
                            
                            // Ajouter les permissions pour toutes les références de cette collection
                            List<Entity> references = entityRelationRepository.findChildrenByParentAndType(
                                entity, EntityConstants.ENTITY_TYPE_REFERENCE);
                            for (Entity reference : references) {
                                if (reference != null && reference.getPublique() != null && reference.getPublique()) {
                                    // Vérifier si la permission n'existe pas déjà
                                    UserPermission.UserPermissionId refId = new UserPermission.UserPermissionId();
                                    refId.setUserId(utilisateur.getId());
                                    refId.setEntityId(reference.getId());
                                    
                                    if (!userPermissionRepository.existsById(refId)) {
                                        UserPermission refPermission = new UserPermission();
                                        refPermission.setUtilisateur(utilisateur);
                                        refPermission.setEntity(reference);
                                        refPermission.setId(refId);
                                        userPermissionRepository.save(refPermission);
                                    }
                                }
                            }
                        }
                    } else if (referenceCode.startsWith("REF:")) {
                        // C'est une référence : format "REF:collectionCode:referenceCode"
                        String[] parts = referenceCode.split(":", 3);
                        if (parts.length == 3) {
                            String referenceCodeOnly = parts[2];
                            entity = entityRepository.findByCode(referenceCodeOnly).orElse(null);
                            
                            if (entity != null) {
                                UserPermission.UserPermissionId id = new UserPermission.UserPermissionId();
                                id.setUserId(utilisateur.getId());
                                id.setEntityId(entity.getId());
                                
                                // Vérifier si la permission n'existe pas déjà
                                if (!userPermissionRepository.existsById(id)) {
                                    UserPermission permission = new UserPermission();
                                    permission.setUtilisateur(utilisateur);
                                    permission.setEntity(entity);
                                    permission.setId(id);
                                    userPermissionRepository.save(permission);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde des permissions pour l'utilisateur : {}", utilisateur.getEmail(), e);
            // Ne pas bloquer la sauvegarde de l'utilisateur si les permissions échouent
        }
    }

    // Getter pour editMode (pour compatibilité avec la vue)
    public boolean isEditMode() {
        return isEditMode;
    }
}
