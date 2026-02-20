package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.DescriptionItem;
import fr.cnrs.opentypo.application.dto.NameItem;
import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Image;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.UserPermission;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.DualListModel;
import org.primefaces.PrimeFaces;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Getter
@Setter
@SessionScoped
@Named(value = "collectionBean")
@Slf4j
public class CollectionBean implements Serializable {

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Inject
    private UserPermissionRepository userPermissionRepository;

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private LoginBean loginBean;

    @Inject
    private Provider<SearchBean> searchBeanProvider;

    @Inject
    private Provider<TreeBean> treeBeanProvider;

    @Inject
    private SearchBean searchBean;

    // Propriétés pour le formulaire de création de collection
    private String collectionDescription;
    private List<Langue> availableLanguages;

    // Liste des noms (un par langue)
    private List<NameItem> collectionNames = new ArrayList<>();

    // Liste des descriptions (une par langue)
    private List<DescriptionItem> collectionDescriptions = new ArrayList<>();

    // Champs temporaires pour la saisie d'un nouveau nom
    private String newNameValue;
    private String newNameLangueCode;

    // Champs temporaires pour la saisie d'une nouvelle description
    private String newDescriptionValue;
    private String newDescriptionLangueCode;

    // Collection publique ou privée
    private Boolean collectionPublique = true; // Par défaut, la collection est publique

    /** PickList pour sélectionner les gestionnaires (IDs) - évite les erreurs de conversion */
    private DualListModel<Long> gestionnairesPickList;

    private Label labelSelected;
    private Description descriptionSelected;
    private boolean editingCollection = false;

    // Champs temporaires pour l'édition
    private String editingLabelValue;
    private String editingDescriptionValue;
    private String editingLanguageCode; // Langue sélectionnée en mode édition
    private Boolean editingStatus;

    /** Statut de visibilité demandé avant confirmation (pour le dialog) */
    private Boolean requestedVisibilityStatus;


    @PostConstruct
    public void init() {
        loadAvailableLanguages();
    }

    /** Liste des utilisateurs éligibles comme gestionnaires (groupe Utilisateur) */
    public List<Utilisateur> getGestionnairesList() {
        if (utilisateurRepository == null) return new ArrayList<>();
        List<Utilisateur> list = utilisateurRepository.findByGroupeNom(GroupEnum.UTILISATEUR.getLabel());
        return list != null ? list : new ArrayList<>();
    }

    public DualListModel<Long> getGestionnairesPickList() {
        if (gestionnairesPickList == null) {
            initGestionnairesPickList();
        }
        return gestionnairesPickList;
    }

    public void setGestionnairesPickList(DualListModel<Long> gestionnairesPickList) {
        this.gestionnairesPickList = gestionnairesPickList;
    }

    private void initGestionnairesPickList() {
        List<Long> sourceIds = getGestionnairesList().stream()
                .map(Utilisateur::getId)
                .filter(id -> id != null)
                .toList();
        gestionnairesPickList = new DualListModel<>(new ArrayList<>(sourceIds), new ArrayList<>());
    }

    /** Libellé affiché pour un utilisateur dans le PickList (à partir de l'ID) */
    public String getUtilisateurDisplayName(Long userId) {
        if (userId == null || utilisateurRepository == null) return "";
        return utilisateurRepository.findById(userId)
                .map(u -> ((u.getPrenom() != null ? u.getPrenom() : "") + " " + (u.getNom() != null ? u.getNom() : "")).trim())
                .orElse("");
    }

    /**
     * Indique si l'utilisateur connecté est gestionnaire de la collection (rôle "Gestionnaire de collection"
     * dans user_permission pour cette entité).
     */
    public boolean canEditCollectionAsGestionnaire(Entity entity) {
        if (entity == null || entity.getId() == null || loginBean.getCurrentUser() == null) {
            return false;
        }

        return userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                loginBean.getCurrentUser().getId(),
                entity.getId(),
                PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel());
    }

    /**
     * Affiche les détails d'une collection spécifique
     */
    public void showCollectionDetail(ApplicationBean applicationBean, Entity collection) {

        // Recharger la collection depuis la base de données pour avoir toutes les données à jour
        Entity refreshedCollection = entityRepository.findById(collection.getId()).orElse(collection);
        applicationBean.setSelectedEntity(refreshedCollection);

        String langSelected = (searchBean.getLangSelected() != null && !searchBean.getLangSelected().isEmpty())
            ? searchBean.getLangSelected()
            : "fr";

        Optional<Label> label = refreshedCollection.getLabels().stream()
                .filter(element -> element.getLangue() != null && element.getLangue().getCode().equalsIgnoreCase(langSelected))
                .findFirst();
        if (label.isPresent()) {
            labelSelected = label.get();
            applicationBean.setSelectedEntityLabel(labelSelected.getNom().toUpperCase());
        } else {
            labelSelected = null;
            applicationBean.setSelectedEntityLabel("");
        }

        descriptionSelected = refreshedCollection.getDescriptions().stream()
                .filter(element -> element.getLangue() != null && element.getLangue().getCode().equalsIgnoreCase(langSelected))
                .findFirst()
                .orElse(null);

        editingStatus = refreshedCollection.getPublique();

        applicationBean.refreshCollectionReferencesList();

        if (refreshedCollection.getCode() != null) {
            // Utiliser le format "COL:" + code pour correspondre au format attendu par hierarchicalCollectionItems
            searchBean.setCollectionSelected("COL:" + refreshedCollection.getCode());
        }

        // Mettre à jour le breadcrumb à partir de selectedEntity
        applicationBean.setBeadCrumbElements(applicationBean.buildBreadcrumbFromSelectedEntity());

        // Initialiser l'arbre avec les référentiels de la collection
        TreeBean treeBean = treeBeanProvider != null ? treeBeanProvider.get() : null;
        if (treeBean != null) {
            treeBean.initializeTreeWithCollection();
        }

        applicationBean.showCollectionDetail();
    }

    /**
     * Met à jour le label et la description de la collection selon la langue sélectionnée
     */
    public void updateCollectionLanguage(ApplicationBean applicationBean) {
        Entity collection = applicationBean.getSelectedCollection();

        if (collection == null) {
            return;
        }

        SearchBean searchBean = searchBeanProvider.get();
        String langCode = searchBean != null && searchBean.getLangSelected() != null
            ? searchBean.getLangSelected()
            : "fr";

        // Mettre à jour le label sélectionné
        Optional<Label> label = collection.getLabels().stream()
                .filter(element -> element.getLangue() != null &&
                       element.getLangue().getCode().equalsIgnoreCase(langCode))
                .findFirst();
        if (label.isPresent()) {
            labelSelected = label.get();
            applicationBean.setSelectedEntityLabel(labelSelected.getNom().toUpperCase());
        } else {
            labelSelected = null;
            applicationBean.setSelectedEntityLabel(collection.getNom() != null ? collection.getNom().toUpperCase() : "");
        }

        // Mettre à jour la description sélectionnée
        descriptionSelected = collection.getDescriptions().stream()
                .filter(element -> element.getLangue() != null &&
                        element.getLangue().getCode().equalsIgnoreCase(langCode))
                .findFirst()
                .orElse(null);
    }

    /**
     * Active le mode édition pour la collection
     */
    public void startEditingCollection(ApplicationBean applicationBean) {
        log.debug("Début de startEditingCollection()");
        editingCollection = true;
        log.debug("editingCollection mis à true");

        // Initialiser avec la langue actuellement sélectionnée
        SearchBean searchBean = searchBeanProvider.get();
        editingLanguageCode = searchBean != null && searchBean.getLangSelected() != null
            ? searchBean.getLangSelected()
            : "fr";
        log.debug("Langue d'édition sélectionnée: {}", editingLanguageCode);

        // Charger les valeurs pour la langue sélectionnée
        loadEditingValuesForLanguage(applicationBean, editingLanguageCode);
    }

    /**
     * Charge les valeurs d'édition pour une langue donnée
     */
    public void loadEditingValuesForLanguage(ApplicationBean applicationBean, String langCode) {
        Entity collection = applicationBean.getSelectedCollection();

        if (collection == null || langCode == null || langCode.isEmpty()) {
            editingLabelValue = "";
            editingDescriptionValue = "";
            return;
        }

        // Recharger la collection depuis la base de données pour avoir les données à jour
        Entity refreshedCollection = collection;
        if (collection.getId() != null) {
            refreshedCollection = entityRepository.findById(collection.getId())
                    .orElse(collection);
            applicationBean.setSelectedEntity(refreshedCollection);
        }

        // Chercher le label pour la langue sélectionnée
        Optional<Label> label = refreshedCollection.getLabels().stream()
                .filter(l -> l.getLangue() != null && l.getLangue().getCode().equalsIgnoreCase(langCode))
                .findFirst();

        if (label.isPresent() && label.get().getNom() != null) {
            editingLabelValue = label.get().getNom();
        } else {
            // Si pas de label pour cette langue, utiliser le nom par défaut de l'entité
            editingLabelValue = refreshedCollection.getNom() != null ? refreshedCollection.getNom() : "";
        }

        // Chercher la description pour la langue sélectionnée
        Optional<Description> description = refreshedCollection.getDescriptions().stream()
                .filter(d -> d.getLangue() != null && d.getLangue().getCode().equalsIgnoreCase(langCode))
                .findFirst();
        if (description.isPresent() && description.get().getValeur() != null) {
            editingDescriptionValue = description.get().getValeur();
        } else {
            // Si pas de description pour cette langue, utiliser le commentaire par défaut
            editingDescriptionValue = refreshedCollection.getCommentaire() != null ? refreshedCollection.getCommentaire() : "";
        }

        editingStatus = refreshedCollection.getPublique();
    }

    /**
     * Change la langue en mode édition et charge les valeurs correspondantes
     */
    public void changeEditingLanguage(ApplicationBean applicationBean) {
        // Récupérer la valeur depuis les paramètres de la requête si elle n'est pas dans le bean
        String langCode = editingLanguageCode;

        // Si la valeur est null ou vide, essayer de la récupérer depuis les paramètres de la requête
        if (langCode == null || langCode.isEmpty()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                // Essayer plusieurs noms possibles pour le paramètre
                String[] possibleNames = {
                    "collectionEditForm:collectionEditLangSelect",
                    "collectionEditLangSelect",
                    "collectionEditForm:collectionEditLangSelect_input"
                };

                for (String paramName : possibleNames) {
                    String paramValue = facesContext.getExternalContext().getRequestParameterMap().get(paramName);
                    if (paramValue != null && !paramValue.isEmpty()) {
                        langCode = paramValue;
                        editingLanguageCode = langCode; // Mettre à jour le bean
                        log.debug("Valeur récupérée depuis les paramètres de requête ({}): {}", paramName, langCode);
                        break;
                    }
                }
            }
        }

        log.debug("Changement de langue d'édition vers: {}", langCode);
        if (langCode != null && !langCode.isEmpty()) {
            loadEditingValuesForLanguage(applicationBean, langCode);
        } else {
            log.warn("Tentative de changement de langue avec un code null ou vide. editingLanguageCode: {}", editingLanguageCode);
            // Essayer de récupérer depuis SearchBean comme fallback
            SearchBean searchBean = searchBeanProvider.get();
            if (searchBean != null && searchBean.getLangSelected() != null) {
                langCode = searchBean.getLangSelected();
                editingLanguageCode = langCode;
                log.debug("Utilisation de la langue depuis SearchBean: {}", langCode);
                loadEditingValuesForLanguage(applicationBean, langCode);
            } else {
                editingLabelValue = "";
                editingDescriptionValue = "";
            }
        }
    }

    /**
     * Bascule la visibilité de la collection (mode création) : publique ↔ privée.
     */
    public void toggleCollectionPublique() {
        collectionPublique = collectionPublique == null ? false : !collectionPublique;
    }

    /**
     * Prépare l'affichage du dialog de confirmation avant changement de visibilité.
     * @param makePublic true pour rendre publique, false pour rendre privée
     */
    public void prepareConfirmVisibilityChange(boolean makePublic) {
        requestedVisibilityStatus = makePublic;
    }

    /**
     * Applique le changement de visibilité après confirmation et persiste en base.
     */
    @Transactional
    public void applyVisibilityChange(ApplicationBean applicationBean) {
        if (applicationBean == null || requestedVisibilityStatus == null) {
            return;
        }
        Entity collection = applicationBean.getSelectedCollection();
        if (collection == null || collection.getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Aucune collection sélectionnée."));
            return;
        }
        try {
            Boolean targetStatus = requestedVisibilityStatus;
            Entity refreshed = entityRepository.findById(collection.getId()).orElse(collection);
            refreshed.setPublique(targetStatus);
            Entity saved = entityRepository.save(refreshed);
            applicationBean.setSelectedEntity(saved);
            editingStatus = targetStatus;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                            targetStatus ? "La collection est maintenant publique." : "La collection est maintenant privée."));
            log.info("Visibilité de la collection {} modifiée: {}", saved.getCode(), targetStatus ? "publique" : "privée");
        } catch (Exception e) {
            log.error("Erreur lors du changement de visibilité", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur est survenue : " + e.getMessage()));
        } finally {
            requestedVisibilityStatus = null;
        }
    }

    /** Message pour le dialog de confirmation de changement de visibilité */
    public String getVisibilityConfirmMessage() {
        if (requestedVisibilityStatus == null) return "";
        return requestedVisibilityStatus
                ? "Voulez-vous rendre cette collection publique ? Elle sera visible par tous les utilisateurs."
                : "Voulez-vous rendre cette collection privée ? Seuls les utilisateurs autorisés pourront y accéder.";
    }

    /** Titre du dialog selon le changement demandé */
    public String getVisibilityConfirmTitle() {
        if (requestedVisibilityStatus == null) return "Changer la visibilité";
        return requestedVisibilityStatus ? "Rendre la collection publique" : "Rendre la collection privée";
    }

    /**
     * Annule les modifications et désactive le mode édition
     */
    public void cancelEditingCollection() {
        editingCollection = false;
        editingLabelValue = null;
        editingDescriptionValue = null;
        editingLanguageCode = null;
    }

    /**
     * Sauvegarde les modifications de la collection
     */
    @Transactional
    public void saveCollectionChanges(ApplicationBean applicationBean) {

        Entity collection = applicationBean.getSelectedCollection();

        if (collection == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucune collection sélectionnée."));
            }
            return;
        }

        // Recharger la collection depuis la base de données pour avoir les données à jour
        Entity refreshedCollection = collection;
        if (collection.getId() != null) {
            try {
                refreshedCollection = entityRepository.findById(collection.getId())
                        .orElse(collection);
                applicationBean.setSelectedEntity(refreshedCollection);
            } catch (Exception e) {
                log.error("Erreur lors du rechargement de la collection pour la sauvegarde", e);
                refreshedCollection = collection;
            }
        }

        // Mettre à jour ou créer le label
        if (editingLabelValue != null && !editingLabelValue.trim().isEmpty()) {
            Optional<Label> existingLabel = refreshedCollection.getLabels().stream()
                    .filter(l -> l.getLangue() != null && l.getLangue().getCode().equalsIgnoreCase(editingLanguageCode))
                    .findFirst();

            if (existingLabel.isPresent()) {
                // Mettre à jour le label existant
                existingLabel.get().setNom(editingLabelValue.trim());
            } else {
                // Créer un nouveau label
                Langue langue = langueRepository.findByCode(editingLanguageCode);
                if (langue != null) {
                    Label newLabel = new Label();
                    newLabel.setNom(editingLabelValue.trim());
                    newLabel.setEntity(refreshedCollection);
                    newLabel.setLangue(langue);
                    if (refreshedCollection.getLabels() == null) {
                        refreshedCollection.setLabels(new ArrayList<>());
                    }
                    refreshedCollection.getLabels().add(newLabel);
                } else {
                    log.warn("Langue avec le code {} non trouvée pour créer le label", editingLanguageCode);
                }
            }
        }

        // Mettre à jour ou créer la description
        if (editingDescriptionValue != null) {
            String descriptionValue = editingDescriptionValue.trim();
            // Permettre les descriptions vides (pour supprimer une description)
            Optional<Description> existingDescription = refreshedCollection.getDescriptions().stream()
                    .filter(d -> d.getLangue() != null && d.getLangue().getCode().equalsIgnoreCase(editingLanguageCode))
                    .findFirst();

            if (existingDescription.isPresent()) {
                // Mettre à jour la description existante
                existingDescription.get().setValeur(descriptionValue);
            } else {
                // Créer une nouvelle description seulement si elle n'est pas vide
                if (!descriptionValue.isEmpty()) {
                    Langue langue = langueRepository.findByCode(editingLanguageCode);
                    if (langue != null) {
                        Description newDescription = new Description();
                        newDescription.setValeur(descriptionValue);
                        newDescription.setEntity(refreshedCollection);
                        newDescription.setLangue(langue);
                        if (refreshedCollection.getDescriptions() == null) {
                            refreshedCollection.setDescriptions(new ArrayList<>());
                        }
                        refreshedCollection.getDescriptions().add(newDescription);
                    } else {
                        log.warn("Langue avec le code {} non trouvée pour créer la description", editingLanguageCode);
                    }
                }
            }
        }

        refreshedCollection.setPublique(editingStatus);

        // Sauvegarder la collection
        Entity savedCollection = entityRepository.save(refreshedCollection);
        applicationBean.setSelectedEntity(savedCollection);

        // Mettre à jour les valeurs sélectionnées selon la langue actuelle
        updateCollectionLanguage(applicationBean);

        // Désactiver le mode édition
        editingCollection = false;
        editingLabelValue = null;
        editingDescriptionValue = null;
        editingLanguageCode = null;

        // Afficher un message de succès
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Les modifications ont été enregistrées avec succès."));
        }

        log.info("Collection modifiée avec succès: {} (ID: {})", savedCollection.getCode(), savedCollection.getId());
    }

    /**
     * Charge les langues disponibles depuis la base de données
     */
    private void loadAvailableLanguages() {
        try {
            availableLanguages = langueRepository.findAllByOrderByNomAsc();
        } catch (Exception e) {
            log.error("Erreur lors du chargement des langues", e);
            availableLanguages = new ArrayList<>();
        }
    }

    /**
     * Réinitialise tous les champs du formulaire de création de collection
     */
    public void resetCollectionForm() {
        collectionNames = new ArrayList<>();
        collectionDescriptions = new ArrayList<>();
        newNameValue = null;
        newNameLangueCode = null;
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        collectionPublique = true; // Par défaut, la collection est publique
        gestionnairesPickList = null; // Sera réinitialisé au prochain get

        PrimeFaces.current().executeScript("PF('addCollectionDialog').show();");
        log.debug("Formulaire de collection réinitialisé");
    }

    /**
     * Ajoute un nouveau nom depuis les champs de saisie
     */
    public void addNameFromInput() {
        if (newNameValue == null || newNameValue.trim().isEmpty()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Le nom est requis."));
            }
            return;
        }

        if (newNameLangueCode == null || newNameLangueCode.trim().isEmpty()) {
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
        if (isLangueAlreadyUsedInNames(newNameLangueCode, null)) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Cette langue est déjà utilisée pour un autre nom."));
            }
            return;
        }

        if (collectionNames == null) {
            collectionNames = new ArrayList<>();
        }

        Langue langue = langueRepository.findByCode(newNameLangueCode);
        NameItem newItem = new NameItem(newNameValue.trim(), newNameLangueCode, langue);
        collectionNames.add(newItem);

        // Réinitialiser les champs de saisie
        newNameValue = null;
        newNameLangueCode = null;
    }

    /**
     * Supprime un nom de la liste
     */
    public void removeName(NameItem nameItem) {
        if (collectionNames != null) {
            collectionNames.remove(nameItem);
        }
    }

    /**
     * Vérifie si une langue est déjà utilisée dans les noms
     */
    public boolean isLangueAlreadyUsedInNames(String langueCode, NameItem currentItem) {
        if (collectionNames == null || langueCode == null || langueCode.isEmpty()) {
            return false;
        }
        return collectionNames.stream()
            .filter(item -> item != currentItem && item.getLangueCode() != null)
            .anyMatch(item -> item.getLangueCode().equals(langueCode));
    }

    /**
     * Obtient les langues disponibles pour un nouveau nom (excluant celles déjà utilisées)
     */
    public List<Langue> getAvailableLanguagesForNewName() {
        if (availableLanguages == null) {
            return new ArrayList<>();
        }
        return availableLanguages.stream()
            .filter(langue -> !isLangueAlreadyUsedInNames(langue.getCode(), null))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Obtient toutes les langues disponibles pour le sélecteur en mode édition
     */
    public List<Langue> getAvailableLanguagesForSelector() {
        if (availableLanguages == null || availableLanguages.isEmpty()) {
            // Si les langues ne sont pas chargées, les charger maintenant
            loadAvailableLanguages();
        }
        return availableLanguages != null ? availableLanguages : new ArrayList<>();
    }

    /**
     * Ajoute une nouvelle description depuis les champs de saisie
     */
    public void addDescriptionFromInput() {
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
        if (isLangueAlreadyUsedInDescriptions(newDescriptionLangueCode, null)) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Cette langue est déjà utilisée pour une autre description."));
            }
            return;
        }

        if (collectionDescriptions == null) {
            collectionDescriptions = new ArrayList<>();
        }

        Langue langue = langueRepository.findByCode(newDescriptionLangueCode);
        DescriptionItem newItem = new DescriptionItem(newDescriptionValue.trim(), newDescriptionLangueCode, langue);
        collectionDescriptions.add(newItem);

        // Réinitialiser les champs de saisie
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
    }

    /**
     * Supprime une description de la liste
     */
    public void removeDescription(DescriptionItem descriptionItem) {
        if (collectionDescriptions != null) {
            collectionDescriptions.remove(descriptionItem);
        }
    }

    /**
     * Vérifie si une langue est déjà utilisée dans les descriptions
     */
    public boolean isLangueAlreadyUsedInDescriptions(String langueCode, DescriptionItem currentItem) {
        if (collectionDescriptions == null || langueCode == null || langueCode.isEmpty()) {
            return false;
        }
        return collectionDescriptions.stream()
            .filter(item -> item != currentItem && item.getLangueCode() != null)
            .anyMatch(item -> item.getLangueCode().equals(langueCode));
    }

    /**
     * Obtient les langues disponibles pour une nouvelle description (excluant celles déjà utilisées)
     */
    public List<Langue> getAvailableLanguagesForNewDescription() {
        if (availableLanguages == null) {
            return new ArrayList<>();
        }
        return availableLanguages.stream()
            .filter(langue -> !isLangueAlreadyUsedInDescriptions(langue.getCode(), null))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Met à jour la langue d'un nom
     */
    public void updateNameLangue(NameItem nameItem) {
        if (nameItem != null && nameItem.getLangueCode() != null && !nameItem.getLangueCode().isEmpty()) {
            Langue langue = langueRepository.findByCode(nameItem.getLangueCode());
            nameItem.setLangue(langue);
        }
    }

    /**
     * Crée une ligne dans user_permission pour chaque gestionnaire sélectionné (rôle Gestionnaire de collection).
     */
    private void saveUserPermissionsForCollection(Entity savedCollection) {
        if (userPermissionRepository == null || savedCollection == null || savedCollection.getId() == null) {
            return;
        }
        List<?> targetList = (gestionnairesPickList != null && gestionnairesPickList.getTarget() != null)
                ? gestionnairesPickList.getTarget() : List.of();
        for (Object raw : targetList) {
            Long userId = toLong(raw);
            if (userId == null) continue;
            UserPermission.UserPermissionId id = new UserPermission.UserPermissionId();
            id.setUserId(userId);
            id.setEntityId(savedCollection.getId());
            if (!userPermissionRepository.existsById(id)) {
                Utilisateur utilisateur = utilisateurRepository.findById(userId).orElse(null);
                if (utilisateur != null) {
                    UserPermission permission = new UserPermission();
                    permission.setUtilisateur(utilisateur);
                    permission.setEntity(savedCollection);
                    permission.setId(id);
                    permission.setRole(PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel());
                    permission.setCreateDate(LocalDateTime.now());
                    userPermissionRepository.save(permission);
                }
            }
        }
    }

    /** Convertit une valeur (Long ou String) en Long pour le PickList. */
    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            if (s.isBlank()) return null;
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public void createCollection(ApplicationBean applicationBean) {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Validation : au moins un nom doit être défini
        if (collectionNames == null || collectionNames.isEmpty()) {
            addErrorMessage("Au moins un nom est requis.");
            return;
        }

        // Validation : tous les noms doivent avoir un nom et une langue
        for (NameItem nameItem : collectionNames) {
            if (nameItem.getNom() == null || nameItem.getNom().trim().isEmpty()) {
                addErrorMessage("Tous les noms doivent avoir une valeur.");
                return;
            }
            if (nameItem.getLangueCode() == null || nameItem.getLangueCode().trim().isEmpty()) {
                addErrorMessage("Tous les noms doivent avoir une langue sélectionnée.");
                return;
            }
        }

        // Validation : pas de doublons de langues pour les noms
        List<String> languesUtiliseesNames = new ArrayList<>();
        for (NameItem nameItem : collectionNames) {
            if (languesUtiliseesNames.contains(nameItem.getLangueCode())) {
                addErrorMessage("Chaque langue ne peut être utilisée qu'une seule fois pour les noms.");
                return;
            }
            languesUtiliseesNames.add(nameItem.getLangueCode());
        }

        // Validation : toutes les descriptions doivent avoir une langue (si elles ont une valeur)
        for (DescriptionItem descriptionItem : collectionDescriptions) {
            if (descriptionItem.getValeur() != null && !descriptionItem.getValeur().trim().isEmpty()) {
                if (descriptionItem.getLangueCode() == null || descriptionItem.getLangueCode().trim().isEmpty()) {
                    addErrorMessage("Toutes les descriptions doivent avoir une langue sélectionnée.");
                    return;
                }
            }
        }

        // Utiliser le premier nom comme nom principal et code
        EntityType collectionType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_COLLECTION)
                .orElseThrow(() -> new IllegalStateException(
                        "Le type d'entité '" + EntityConstants.ENTITY_TYPE_COLLECTION + "' n'existe pas dans la base de données."));

        Entity nouvelleCollection = entityRepository.save(createNewCollection(
                collectionNames.getFirst().getNom().trim().toUpperCase(),
                collectionNames.getFirst().getNom().trim(),
                collectionType));

        // Sauvegarder les permissions des gestionnaires sélectionnés (optionnel)
        saveUserPermissionsForCollection(nouvelleCollection);

        refreshCollectionsList(applicationBean);

        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                "La collection '" + collectionNames.getFirst().getNom().trim() + "' a été créée avec succès."));

        PrimeFaces.current().ajax().update(ViewConstants.COMPONENT_GROWL + ", :collectionForm, "
                + ViewConstants.COMPONENT_CARDS_CONTAINER + ", :searchForm");
    }

    public String getImageUrlOrDefault(Image image) {
        if (image != null && image.getUrl() != null && !image.getUrl().trim().isEmpty()) {
            return image.getUrl();
        }
        // Utiliser le ResourceHandler JSF pour obtenir l'URL correcte de la ressource
        jakarta.faces.context.FacesContext facesContext = jakarta.faces.context.FacesContext.getCurrentInstance();
        if (facesContext != null) {
            jakarta.faces.application.ResourceHandler resourceHandler = facesContext.getApplication().getResourceHandler();
            jakarta.faces.application.Resource resource = resourceHandler.createResource("openTypo-defaut.svg", "img");
            if (resource != null) {
                return resource.getRequestPath();
            }
        }
        // Fallback : chemin direct (fonctionne si contextPath est vide)
        return "/resources/img/openTypo-defaut.svg";
    }

    /**
     * Crée une nouvelle entité collection avec les labels et descriptions pour chaque langue
     */
    private Entity createNewCollection(String code, String nomPrincipal, EntityType type) {
        Entity nouvelleCollection = new Entity();
        nouvelleCollection.setCode(code);
        nouvelleCollection.setEntityType(type);
        nouvelleCollection.setStatut(EntityStatusEnum.AUTOMATIC.name());
        nouvelleCollection.setPublique(collectionPublique != null ? collectionPublique : true);
        nouvelleCollection.setCreateDate(LocalDateTime.now());

        Langue languePrincipale = langueRepository.findByCode(searchBean.getLangSelected());
        if (!StringUtils.isEmpty(nomPrincipal)) {
            Label labelPrincipal = new Label();
            labelPrincipal.setNom(nomPrincipal.trim());
            labelPrincipal.setLangue(languePrincipale);
            labelPrincipal.setEntity(nouvelleCollection);
            List<Label> labels = new ArrayList<>();
            labels.add(labelPrincipal);
            nouvelleCollection.setLabels(labels);
        }

        Utilisateur currentUser = loginBean.getCurrentUser();
        if (currentUser != null) {
            nouvelleCollection.setCreateBy(currentUser.getEmail());
            List<Utilisateur> auteurs = new ArrayList<>();
            auteurs.add(currentUser);
            nouvelleCollection.setAuteurs(auteurs);
        }

        // Créer les labels pour chaque langue
        if (collectionNames != null && !collectionNames.isEmpty()) {
            for (NameItem nameItem : collectionNames) {
                if (nameItem.getNom() != null && !nameItem.getNom().trim().isEmpty() &&
                    nameItem.getLangueCode() != null && !nameItem.getLangueCode().trim().isEmpty()) {

                    Langue langue = langueRepository.findByCode(nameItem.getLangueCode());
                    if (langue != null) {
                        Label label = new Label();
                        label.setNom(nameItem.getNom().trim());
                        label.setEntity(nouvelleCollection);
                        label.setLangue(langue);
                        nouvelleCollection.getLabels().add(label);
                    }
                }
            }
        }

        // Créer les descriptions pour chaque langue
        if (collectionDescriptions != null && !collectionDescriptions.isEmpty()) {
            for (DescriptionItem descriptionItem : collectionDescriptions) {
                if (descriptionItem.getValeur() != null && !descriptionItem.getValeur().trim().isEmpty() &&
                    descriptionItem.getLangueCode() != null && !descriptionItem.getLangueCode().trim().isEmpty()) {

                    Langue langue = langueRepository.findByCode(descriptionItem.getLangueCode());
                    if (langue != null) {
                        Description description = new Description();
                        description.setValeur(descriptionItem.getValeur().trim());
                        description.setEntity(nouvelleCollection);
                        description.setLangue(langue);
                        nouvelleCollection.getDescriptions().add(description);
                    }
                }
            }
        }

        return nouvelleCollection;
    }

    /**
     * Recharge les listes de collections dans les beans concernés
     */
    private void refreshCollectionsList(ApplicationBean applicationBean) {
        applicationBean.loadPublicCollections();
        SearchBean sb = searchBeanProvider.get();
        if (sb != null) {
            sb.loadCollections();
        }
    }

    /**
     * Ajoute un message d'erreur
     */
    private void addErrorMessage(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", message));
        PrimeFaces.current().ajax().update(ViewConstants.COMPONENT_GROWL + ", :collectionForm");
    }

    /**
     * Supprime une collection et toutes ses entités rattachées
     */
    @Transactional
    public void deleteCollection(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedEntity() == null || applicationBean.getSelectedEntity().getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur", "Aucune collection sélectionnée."));
            return;
        }

        String collectionCode = applicationBean.getSelectedEntity().getCode();
        String collectionName = applicationBean.getSelectedEntity().getNom();
        Long collectionId = applicationBean.getSelectedEntity().getId();

        // Supprimer récursivement la collection et toutes ses entités enfants
        applicationBean.deleteEntityRecursively(applicationBean.getSelectedEntity());

        // Réinitialiser la sélection si c'était la collection sélectionnée
        if (applicationBean.getSelectedEntity() != null && applicationBean.getSelectedEntity().getId().equals(collectionId)) {
            applicationBean.setSelectedEntity(null);
            applicationBean.setSelectedEntityLabel("");
            applicationBean.setChilds(new ArrayList<>());
        }

        // Recharger les collections
        applicationBean.loadPublicCollections();

        // Mettre à jour l'arbre
        applicationBean.getTreeBean().initializeTreeWithCollection();

        // Afficher un message de succès
        if (FacesContext.getCurrentInstance() != null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "La collection '" + collectionName + "' et toutes ses entités rattachées ont été supprimées avec succès."));
        }

        // Afficher le panel des collections
        applicationBean.getPanelState().showCollections();

        log.info("Collection supprimée avec succès: {} (ID: {})", collectionCode, collectionId);
    }

    /**
     * Édite une collection (affiche les détails pour édition)
     */
    public void editCollection(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedEntity() == null) {
            if (FacesContext.getCurrentInstance() != null) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur", "Aucune collection sélectionnée."));
            }
            return;
        }

        // Afficher les détails de la collection pour édition
        showCollectionDetail(applicationBean, applicationBean.getSelectedEntity());

        // Activer le mode édition
        startEditingCollection(applicationBean);

        log.debug("Mode édition activé pour la collection: {}", applicationBean.getSelectedEntity().getCode());
    }
}
