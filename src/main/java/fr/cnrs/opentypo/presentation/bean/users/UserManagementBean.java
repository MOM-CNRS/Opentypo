package fr.cnrs.opentypo.presentation.bean.users;

import fr.cnrs.opentypo.application.dto.CollectionReferenceItem;
import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
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
import fr.cnrs.opentypo.presentation.bean.UserBean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DualListModel;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Named("userManagementBean")
@SessionScoped
@Getter
@Setter
public class UserManagementBean implements Serializable {

    @Inject
    private UserBean currentUserBean;

    @Inject
    private LoginBean loginBean;

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
    private UserPermissionRepository userPermissionRepository;

    private List<Utilisateur> users = new ArrayList<>();
    private Utilisateur selectedUser;
    private Utilisateur newUser;
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
            // Charger les collections (liste complète, tous statuts)
            List<Entity> collections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION).stream()
                .filter(Objects::nonNull)
                .toList();
            
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
     * Affiche le panel des collections pour tout groupe sauf "Administrateur technique".
     */
    public boolean showCollectionsPanel() {
        if (selectedGroupe == null) {
            return false;
        }
        return !"Administrateur technique".equalsIgnoreCase(selectedGroupe.getNom());
    }

    /**
     * Indique si le groupe sélectionné est "Administrateur Référentiel".
     */
    public boolean isAdministrateurReferentiel() {
        if (selectedGroupe == null) {
            return false;
        }
        return "Administrateur Référentiel".equalsIgnoreCase(selectedGroupe.getNom());
    }

    /**
     * Retourne la liste des collections pour le dataTable (avec filtre de recherche optionnel).
     * Charge la liste depuis la base si nécessaire (liste complète, tous statuts).
     */
    public List<CollectionReferenceItem> getFilteredCollectionReferenceItems() {
        if (collectionReferenceItems.isEmpty()) {
            chargerCollectionsEtReferences();
        }
        if (searchFilter == null || searchFilter.trim().isEmpty()) {
            return collectionReferenceItems;
        }
        String filterLower = searchFilter.toLowerCase().trim();
        List<CollectionReferenceItem> filtered = new ArrayList<>();
        for (CollectionReferenceItem item : collectionReferenceItems) {
            if (item == null || item.getCollection() == null) {
                continue;
            }
            String code = item.getCollection().getCode();
            String nom = item.getCollection().getNom();
            boolean codeMatch = code != null && code.toLowerCase().contains(filterLower);
            boolean nomMatch = nom != null && nom.toLowerCase().contains(filterLower);
            if (codeMatch || nomMatch) {
                filtered.add(item);
            }
        }
        return filtered;
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
     * Appelé lors du clic sur la case à cocher d'une collection (panel commun à tous les groupes sauf Admin technique).
     * Délègue à la logique Admin Référentiel ou Éditeur/Lecteur selon le groupe.
     */
    public void onCollectionToggle(CollectionReferenceItem item) {
        if (isAdministrateurReferentiel()) {
            toggleCollectionSelectionForAdminRef(item);
        } else {
            toggleCollectionSelection(item);
        }
    }

    /**
     * Gère la sélection/désélection d'une collection pour le groupe Administrateur Référentiel.
     * Sélectionner une collection ajoute tous ses référentiels à selectedReferenceCodesOnly ;
     * désélectionner retire tous ses référentiels.
     */
    public void toggleCollectionSelectionForAdminRef(CollectionReferenceItem item) {
        if (item == null || item.getCollection() == null || item.getCollection().getCode() == null) {
            return;
        }
        CollectionReferenceItem originalItem = null;
        for (CollectionReferenceItem original : collectionReferenceItems) {
            if (original != null && original.getCollection() != null
                && original.getCollection().getCode() != null
                && original.getCollection().getCode().equals(item.getCollection().getCode())) {
                originalItem = original;
                break;
            }
        }
        if (originalItem == null) {
            return;
        }
        boolean newValue = !originalItem.isCollectionSelected();
        originalItem.setCollectionSelected(newValue);
        if (originalItem.getReferences() == null) {
            return;
        }
        if (newValue) {
            for (Entity ref : originalItem.getReferences()) {
                if (ref != null && ref.getCode() != null && !selectedReferenceCodesOnly.contains(ref.getCode())) {
                    selectedReferenceCodesOnly.add(ref.getCode());
                }
            }
        } else {
            for (Entity ref : originalItem.getReferences()) {
                if (ref != null && ref.getCode() != null) {
                    selectedReferenceCodesOnly.remove(ref.getCode());
                }
            }
        }
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
        
        // Si au moins une référence est sélectionnée, sélectionner la collection automatiquement
        // Si aucune référence n'est sélectionnée, désélectionner la collection
        boolean atLeastOneSelected = originalItem.getReferencesSelected().stream().anyMatch(Boolean::booleanValue);
        originalItem.setCollectionSelected(atLeastOneSelected);

        // Mettre à jour selectedReferenceCodes
        updateSelectedReferenceCodes();
    }
    
    /**
     * Met à jour selectedReferenceCodes à partir des sélections dans collectionReferenceItems.
     * - Collection cochée et tous les référentiels cochés → on enregistre "COL:" (accès à toute la collection).
     * - Sinon → on enregistre uniquement les "REF:col:ref" pour les référentiels cochés.
     */
    private void updateSelectedReferenceCodes() {
        selectedReferenceCodes = new ArrayList<>();
        
        for (CollectionReferenceItem item : collectionReferenceItems) {
            if (item == null || item.getCollection() == null) {
                continue;
            }
            
            boolean allRefsSelected = item.getReferences() != null && item.getReferencesSelected() != null
                && !item.getReferencesSelected().isEmpty()
                && item.getReferencesSelected().stream().allMatch(Boolean::booleanValue);
            
            if (item.isCollectionSelected() && allRefsSelected) {
                // Collection explicitement sélectionnée (tous les référentiels cochés)
                selectedReferenceCodes.add("COL:" + item.getCollection().getCode());
            } else if (item.getReferences() != null && item.getReferencesSelected() != null) {
                // Références sélectionnées individuellement (la collection peut être cochée visuellement)
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
        selectedGroupe = groupeRepository.findById(selectedGroupeId).orElse(null);
        // Ne pas vider collectionReferenceItems : la liste des collections reste celle chargée en base
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

        users = utilisateurRepository.findAll();
    }

    public void initNouveauUser() throws IOException {
        if (!isAdminTechnique()) {
            redirectToUnauthorized();
            return;
        }
        jakarta.faces.context.FacesContext facesContext = jakarta.faces.context.FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String userIdParam = facesContext.getExternalContext().getRequestParameterMap().get("userId");
            if (userIdParam != null && !userIdParam.isEmpty()) {
                Long userId = Long.parseLong(userIdParam);
                Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(userId);
                if (utilisateurOpt.isPresent()) {
                    initEditUser(utilisateurOpt.get());
                    return;
                }
            }
        }
        newUser = new Utilisateur();
        newUser.setCreateBy(currentUserBean.getUsername() != null ? currentUserBean.getUsername() : "SYSTEM");
        newUser.setActive(true);
        newUser.setGroupe(null);
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

        FacesContext.getCurrentInstance().getExternalContext()
                .redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/users/create.xhtml");
    }

    public void initEditUser(Utilisateur utilisateur) throws IOException {
        newUser = utilisateur;
        isEditMode = true;
        // S'assurer que les groupes sont chargés
        if (availableGroups.isEmpty()) {
            chargerGroupes();
        }
        // Charger le groupe actuel de l'utilisateur pour la modification
        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(utilisateur.getId());
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
        chargerPermissionsUtilisateur(utilisateur.getId());
        if (selectedReferenceCodes != null && !selectedReferenceCodes.isEmpty()) {
            appliquerPermissionsAuxItems();
        }
        // Réinitialiser le PickList (ancien système)
        initialiserPickList();


        FacesContext.getCurrentInstance().getExternalContext()
                .redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/users/create.xhtml");
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
        String firstName = newUser.getPrenom() != null ? newUser.getPrenom().trim() : "";
        String lastName = newUser.getNom() != null ? newUser.getNom().trim() : "";
        String password = newUser.getPasswordHash() != null ? newUser.getPasswordHash().trim() : "";

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

        // Si le groupe n'est pas Administrateur technique, au moins une collection doit être sélectionnée
        if (showCollectionsPanel()) {
            boolean atLeastOneCollectionSelected = collectionReferenceItems != null
                && collectionReferenceItems.stream()
                    .anyMatch(item -> item != null && item.isCollectionSelected());
            if (!atLeastOneCollectionSelected) {
                notificationBean.showErrorWithUpdate("Erreur de validation",
                    "Vous devez sélectionner au moins une collection pour ce groupe.",
                    ":growl, :userForm");
                return;
            }
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

                if (newUser.getPasswordHash() == null || newUser.getPasswordHash().trim().isEmpty()) {
                    notificationBean.showErrorWithUpdate("Erreur", 
                        "Le mot de passe est requis pour un nouvel utilisateur.", 
                        ":growl, :userForm");
                    return;
                }

                // Créer l'entité Utilisateur
                Utilisateur utilisateur = new Utilisateur();
                utilisateur.setNom(newUser.getNom().trim());
                utilisateur.setPrenom(newUser.getPrenom().trim());
                utilisateur.setEmail(newUser.getEmail().trim());
                utilisateur.setPasswordHash(utilisateurService.encodePassword(newUser.getPasswordHash()));
                utilisateur.setGroupe(selectedGroupe);
                utilisateur.setActive(newUser.getActive());
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
                utilisateur.setNom(newUser.getNom().trim());
                utilisateur.setPrenom(newUser.getPrenom().trim());
                utilisateur.setEmail(newUser.getEmail().trim());
                utilisateur.setGroupe(selectedGroupe);
                utilisateur.setActive(newUser.getActive());

                // Mettre à jour le mot de passe seulement si un nouveau mot de passe est fourni
                if (newUser.getPasswordHash() != null && !newUser.getPasswordHash().trim().isEmpty()) {
                    utilisateur.setPasswordHash(utilisateurService.encodePassword(newUser.getPasswordHash()));
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

    public void supprimerUser(Utilisateur utilisateur) {
        // Vérifier que l'utilisateur est administrateur
        if (!isAdminTechnique()) {
            notificationBean.showErrorWithUpdate("Accès refusé", 
                "Seuls les administrateurs peuvent supprimer des utilisateurs.", 
                ":growl, :usersForm");
            return;
        }

        if (utilisateur == null || utilisateur.getId() == null) {
            notificationBean.showErrorWithUpdate("Erreur", "Aucun utilisateur sélectionné pour la suppression.", ":growl, :usersForm");
            return;
        }

        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(utilisateur.getId());
            if (utilisateurOpt.isPresent()) {
                Utilisateur user = utilisateurOpt.get();
                
                // Vérifier si l'utilisateur n'est pas l'utilisateur actuellement connecté
                if (currentUserBean.getUsername() != null && user.getEmail().equals(currentUserBean.getUsername())) {
                    notificationBean.showErrorWithUpdate("Erreur", "Vous ne pouvez pas supprimer votre propre compte.",
                        ":growl, :usersForm");
                    return;
                }

                utilisateurRepository.delete(user);
                notificationBean.showSuccessWithUpdate("Succès",
                        "L'utilisateur " + user.getPrenom() + " " + user.getNom() + " a été supprimé avec succès.",
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

    public void toggleUserActive(Utilisateur user) {
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
                // Une ligne user_permission par collection sélectionnée : on charge les codes collection (COL:)
                for (UserPermission permission : permissions) {
                    if (permission.getEntity() != null) {
                        Entity entity = permission.getEntity();
                        if (EntityConstants.ENTITY_TYPE_COLLECTION.equals(entity.getEntityType().getCode())
                            && entity.getCode() != null) {
                            selectedReferenceCodes.add("COL:" + entity.getCode());
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
                
                // Si au moins une référence est sélectionnée, afficher la collection comme cochée
                if (!collectionSelected) {
                    boolean atLeastOneRefSelected = item.getReferencesSelected().stream().anyMatch(Boolean::booleanValue);
                    if (atLeastOneRefSelected) {
                        item.setCollectionSelected(true);
                    }
                }
            }
        }
    }
    
    /**
     * Applique les permissions chargées (selectedReferenceCodesOnly) aux cases à cocher
     * des collections pour le groupe Administrateur Référentiel : une collection est
     * cochée si tous ses référentiels sont dans selectedReferenceCodesOnly.
     */
    private void appliquerPermissionsAuxItemsForAdminRef() {
        if (selectedReferenceCodesOnly == null) {
            selectedReferenceCodesOnly = new ArrayList<>();
        }
        for (CollectionReferenceItem item : collectionReferenceItems) {
            if (item == null) {
                continue;
            }
            if (item.getCollection() == null || item.getReferences() == null || item.getReferences().isEmpty()) {
                item.setCollectionSelected(false);
                continue;
            }
            boolean allRefsSelected = true;
            for (Entity ref : item.getReferences()) {
                if (ref == null || ref.getCode() == null || !selectedReferenceCodesOnly.contains(ref.getCode())) {
                    allRefsSelected = false;
                    break;
                }
            }
            item.setCollectionSelected(allRefsSelected);
        }
    }

    /**
     * Sauvegarde les permissions pour un utilisateur.
     * - Administrateur technique : aucune ligne dans user_permission pour les collections.
     * - Autres groupes : une ligne dans user_permission par collection sélectionnée (entité collection).
     */
    private void sauvegarderPermissions(Utilisateur utilisateur) {
        // Supprimer les anciennes permissions
        List<UserPermission> existingPermissions = userPermissionRepository.findByUtilisateur(utilisateur);
        for (UserPermission permission : existingPermissions) {
            userPermissionRepository.delete(permission);
        }

        // Administrateur technique : pas de permissions collections à enregistrer
        if (utilisateur.getGroupe() != null
                && "Administrateur technique".equalsIgnoreCase(utilisateur.getGroupe().getNom())) {
            return;
        }

        // Pour tout autre groupe : une ligne dans user_permission par collection sélectionnée
        if (collectionReferenceItems != null) {
            for (CollectionReferenceItem item : collectionReferenceItems) {
                if (item == null || !item.isCollectionSelected() || item.getCollection() == null) {
                    continue;
                }
                Entity collection = item.getCollection();
                UserPermission.UserPermissionId id = new UserPermission.UserPermissionId();
                id.setUserId(utilisateur.getId());
                id.setEntityId(collection.getId());
                if (!userPermissionRepository.existsById(id)) {
                    UserPermission permission = new UserPermission();
                    permission.setUtilisateur(utilisateur);
                    permission.setEntity(collection);
                    permission.setId(id);
                    permission.setCreateDate(LocalDateTime.now());
                    userPermissionRepository.save(permission);
                }
            }
        }
    }

    public String getGroupeEtiquette(Groupe groupe) {
        if (GroupEnum.LECTEUR.getLabel().equalsIgnoreCase(groupe.getNom())) {
            return "role-badge-viewer";
        } else if (GroupEnum.EDITEUR.getLabel().equalsIgnoreCase(groupe.getNom())) {
            return "role-badge-editor";
        } else {
            return "role-badge-admin";
        }
    }

    // Getter pour editMode (pour compatibilité avec la vue)
    public boolean isEditMode() {
        return isEditMode;
    }
}
