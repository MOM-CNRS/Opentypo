package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.DescriptionItem;
import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.NameItem;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Image;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.domain.entity.UserPermission;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.application.service.EntityImageService;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ImageRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.util.EntityValidator;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.DualListModel;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.Part;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Getter
@Setter
@SessionScoped
@Named(value = "referenceBean")
@Slf4j
public class ReferenceBean implements Serializable {

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private LoginBean loginBean;

    @Autowired
    private TreeBean treeBean;
    
    @Autowired
    private ApplicationBean applicationBean;

    @Autowired
    private EntityEditModeBean entityEditModeBean;

    @Autowired
    private SearchBean searchBean;

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Autowired
    private LangueRepository langueRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private CollectionBean collectionBean;

    @Autowired
    private EntityImageService entityImageService;

    @Autowired
    private ImageRepository imageRepository;

    /** URL de l'image (URL saisie ou fichier enregistré à la validation) */
    private String uploadedImageUrl;
    /** Légende de l'image principale */
    private String uploadedImageLegende;
    /** Libellé affiché pour l'image ("URL externe") */
    private String uploadedImageName;
    /** Saisie manuelle d'une URL d'image */
    private String imageUrlInput;
    /** Fichier image sélectionné localement (enregistré dans images/ à la validation) */
    private Part uploadedFilePart;

    /** PickList pour sélectionner les gestionnaires de référentiel (IDs) - création dialog */
    private DualListModel<Long> gestionnairesPickList;

    /** PickList pour l'édition inline des gestionnaires de référentiel */
    private DualListModel<Long> editingGestionnairesPickList;

    private transient List<String> cachedReferenceGestionnaires;
    private transient Long cachedReferenceGestionnairesEntityId;

    // Propriétés pour le formulaire de création de référentiel
    private String referenceCode;
    private List<String> referenceBibliographiqueList = new ArrayList<>();

    // Liste des noms (labels) multilingues
    private List<NameItem> referenceNames = new ArrayList<>();
    // Liste des descriptions multilingues
    private List<DescriptionItem> referenceDescriptions = new ArrayList<>();
    // Champs temporaires pour la saisie d'un nouveau nom
    private String newNameValue;
    private String newNameLangueCode;
    // Champs temporaires pour la saisie d'une nouvelle description
    private String newDescriptionValue;
    private String newDescriptionLangueCode;
    // Référentiel public ou privé
    private Boolean referencePublique = true;
    // Langues disponibles (chargées à la demande)
    private List<Langue> availableLanguages;

    /** ID du référentiel en cours d'édition dans le dialog (null = mode création). */
    private Long editingReferenceId;

    private String editingReferenceCode;
    private String editingReferenceDescription;
    private String editingReferenceLabel;
    private String editingReferenceBibliographie;
    /** Liste des références bibliographiques pour l'édition inline (stockée avec ";" dans rereference_bibliographique) */
    private List<String> editingReferenceBibliographiqueList = new ArrayList<>();
    /** Code langue pour laquelle on édite le label (ex: fr, en). */
    private String editingLabelLangueCode;
    private String editingDescriptionLangueCode;



    /** Liste des utilisateurs éligibles comme gestionnaires de référentiel (groupe Utilisateur) */
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

    /**
     * PickList pour l'édition inline des gestionnaires. Retourne une liste vide si non initialisé.
     */
    public DualListModel<Long> getEditingGestionnairesPickList() {
        if (editingGestionnairesPickList == null) {
            editingGestionnairesPickList = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
        }
        return editingGestionnairesPickList;
    }

    private void initGestionnairesPickList() {
        List<Long> sourceIds = getGestionnairesList().stream()
                .map(Utilisateur::getId)
                .filter(Objects::nonNull)
                .toList();
        gestionnairesPickList = new DualListModel<>(new ArrayList<>(sourceIds), new ArrayList<>());
    }

    private void initGestionnairesPickListForEdit(Long entityId) {
        List<Long> sourceIds = getGestionnairesList().stream()
                .map(Utilisateur::getId)
                .filter(Objects::nonNull)
                .toList();
        List<Long> targetIds = (entityId != null && userPermissionRepository != null)
                ? userPermissionRepository.findUserIdsByEntityIdAndRole(entityId, PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())
                : new ArrayList<>();
        List<Long> sourceFiltered = sourceIds.stream().filter(id -> !targetIds.contains(id)).toList();
        gestionnairesPickList = new DualListModel<>(new ArrayList<>(sourceFiltered), new ArrayList<>(targetIds));
    }

    /**
     * Initialise le PickList des gestionnaires pour l'édition inline d'un référentiel existant.
     */
    public void initEditingGestionnairesPickListForEdit(Long entityId) {
        List<Long> sourceIds = getGestionnairesList().stream()
                .map(Utilisateur::getId)
                .filter(Objects::nonNull)
                .toList();
        List<Long> targetIds = (entityId != null && userPermissionRepository != null)
                ? userPermissionRepository.findUserIdsByEntityIdAndRole(entityId, PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())
                : new ArrayList<>();
        List<Long> sourceFiltered = sourceIds.stream().filter(id -> !targetIds.contains(id)).toList();
        editingGestionnairesPickList = new DualListModel<>(new ArrayList<>(sourceFiltered), new ArrayList<>(targetIds));
    }

    /**
     * Retourne la liste des noms affichables des gestionnaires du référentiel (rôle "Gestionnaire de référentiel").
     */
    public List<String> getReferenceGestionnairesDisplayNames(Entity reference) {
        if (reference == null || reference.getId() == null || userPermissionRepository == null) {
            return List.of();
        }
        if (reference.getId().equals(cachedReferenceGestionnairesEntityId) && cachedReferenceGestionnaires != null) {
            return cachedReferenceGestionnaires;
        }
        List<Long> userIds = userPermissionRepository.findUserIdsByEntityIdAndRole(
                reference.getId(), PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel());
        if (userIds == null || userIds.isEmpty()) {
            cachedReferenceGestionnairesEntityId = reference.getId();
            cachedReferenceGestionnaires = List.of();
            return List.of();
        }
        List<String> names = userIds.stream()
                .map(this::getUtilisateurDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
        cachedReferenceGestionnairesEntityId = reference.getId();
        cachedReferenceGestionnaires = names;
        return names;
    }

    private void invalidateReferenceGestionnairesCache(Long entityId) {
        if (entityId != null && entityId.equals(cachedReferenceGestionnairesEntityId)) {
            cachedReferenceGestionnairesEntityId = null;
            cachedReferenceGestionnaires = null;
        }
    }

    /** Libellé affiché pour un utilisateur dans le PickList (à partir de l'ID) */
    public String getUtilisateurDisplayName(Long userId) {
        if (userId == null || utilisateurRepository == null) return "";
        return utilisateurRepository.findById(userId)
                .map(u -> ((u.getPrenom() != null ? u.getPrenom() : "") + " " + (u.getNom() != null ? u.getNom() : "")).trim())
                .orElse("");
    }

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

    /**
     * Sauvegarde les gestionnaires du référentiel (appelé par EntityUpdateBean.saveModification pour entity type 1).
     */
    @Transactional
    public void saveReferenceGestionnaires(Entity savedReference) {
        saveUserPermissionsForReference(savedReference);
    }

    @Transactional
    protected void saveUserPermissionsForReference(Entity savedReference) {
        if (userPermissionRepository == null || savedReference == null || savedReference.getId() == null) return;
        DualListModel<Long> pickListToUse = (editingGestionnairesPickList != null)
                ? editingGestionnairesPickList
                : gestionnairesPickList;
        userPermissionRepository.deleteByEntityIdAndRole(savedReference.getId(), PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel());
        List<?> targetList = (pickListToUse != null && pickListToUse.getTarget() != null)
                ? pickListToUse.getTarget() : List.of();
        for (Object raw : targetList) {
            Long userId = toLong(raw);
            if (userId == null) continue;
            UserPermission.UserPermissionId id = new UserPermission.UserPermissionId();
            id.setUserId(userId);
            id.setEntityId(savedReference.getId());
            if (!userPermissionRepository.existsById(id)) {
                Utilisateur utilisateur = utilisateurRepository.findById(userId).orElse(null);
                if (utilisateur != null) {
                    UserPermission permission = new UserPermission();
                    permission.setUtilisateur(utilisateur);
                    permission.setEntity(savedReference);
                    permission.setId(id);
                    permission.setRole(PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel());
                    permission.setCreateDate(LocalDateTime.now());
                    userPermissionRepository.save(permission);
                }
            }
        }
        invalidateReferenceGestionnairesCache(savedReference.getId());
    }

    /**
     * Prépare le dialog pour la création : réinitialise le formulaire et passe en mode création.
     */
    public void prepareCreateReference() {
        editingReferenceId = null;
        referenceCode = null;
        referenceBibliographiqueList = new ArrayList<>();
        referenceNames = new ArrayList<>();
        referenceDescriptions = new ArrayList<>();
        newNameValue = null;
        newNameLangueCode = searchBean.getLangSelected();
        newDescriptionValue = null;
        newDescriptionLangueCode = searchBean.getLangSelected();
        referencePublique = true;
        gestionnairesPickList = null;
        uploadedImageUrl = null;
        uploadedImageLegende = null;
        uploadedImageName = null;
        imageUrlInput = null;
        uploadedFilePart = null;
    }

    /**
     * Indique si le dialog est en mode édition (true) ou création (false).
     */
    public boolean isEditModeInDialog() {
        return editingReferenceId != null;
    }

    /** Bascule la visibilité public/privé du référentiel dans le formulaire du dialog */
    public void toggleReferencePublique() {
        referencePublique = (referencePublique == null || !referencePublique);
    }

    private void loadAvailableLanguages() {
        if (availableLanguages == null) {
            availableLanguages = langueRepository.findAllByOrderByNomAsc();
        }
    }

    /**
     * Applique une URL d'image saisie manuellement.
     * L'URL sera enregistrée dans la table image lors de la création du référentiel.
     */
    public void applyImageUrl() {
        if (imageUrlInput == null || imageUrlInput.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez saisir une URL."));
            return;
        }
        String url = imageUrlInput.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "L'URL doit commencer par http:// ou https://"));
            return;
        }
        uploadedImageUrl = url;
        uploadedImageName = "URL externe";
        imageUrlInput = null;
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "URL de l'image enregistrée."));
        PrimeFaces.current().ajax().update(":referenceDialogForm:imageSection growl");
    }

    /**
     * Supprime l'image sélectionnée (URL).
     */
    public void clearImage() {
        uploadedImageUrl = null;
        uploadedImageLegende = null;
        uploadedImageName = null;
        imageUrlInput = null;
        uploadedFilePart = null;
        PrimeFaces.current().ajax().update(":referenceDialogForm:imageSection growl");
    }

    public void addNameFromInput() {
        if (newNameValue == null || newNameValue.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Le nom est requis."));
            return;
        }
        if (newNameLangueCode == null || newNameLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La langue est requise."));
            return;
        }
        if (isLangueAlreadyUsedInNames(newNameLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Cette langue est déjà utilisée pour un autre nom."));
            return;
        }

        // Vérifier si un référentiel existe déjà avec le même nom et la même langue
        String nomTrimmed = newNameValue.trim();
        String langueCode = newNameLangueCode.trim();
        if (entityRepository.existsByLabelNomAndLangueCodeAndEntityTypeCode(
                nomTrimmed, langueCode, EntityConstants.ENTITY_TYPE_REFERENCE)) {
            String langueNom = Optional.ofNullable(langueRepository.findByCode(langueCode))
                    .map(Langue::getNom)
                    .orElse(langueCode);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention",
                    "Un référentiel existe déjà avec le nom « " + nomTrimmed + " » en " + langueNom + "."));
            return;
        }

        if (referenceNames == null) referenceNames = new ArrayList<>();

        Langue langue = langueRepository.findByCode(newNameLangueCode);
        referenceNames.add(new NameItem(newNameValue.trim(), newNameLangueCode, langue));
        newNameValue = null;
        newNameLangueCode = null;
    }

    public void removeName(NameItem item) {
        if (referenceNames != null) referenceNames.remove(item);
    }

    public boolean isLangueAlreadyUsedInNames(String code, NameItem exclude) {
        if (referenceNames == null || code == null) return false;
        return referenceNames.stream()
            .filter(i -> i != exclude && i.getLangueCode() != null)
            .anyMatch(i -> i.getLangueCode().equals(code));
    }

    public List<Langue> getAvailableLanguagesForNewName() {
        loadAvailableLanguages();
        if (availableLanguages == null) return new ArrayList<>();
        return availableLanguages.stream()
            .filter(l -> !isLangueAlreadyUsedInNames(l.getCode(), null))
            .collect(java.util.stream.Collectors.toList());
    }

    public void addDescriptionFromInput() {
        if (newDescriptionValue == null || newDescriptionValue.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La description est requise."));
            return;
        }
        if (newDescriptionLangueCode == null || newDescriptionLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La langue est requise."));
            return;
        }
        if (isLangueAlreadyUsedInDescriptions(newDescriptionLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Cette langue est déjà utilisée pour une autre description."));
            return;
        }
        if (referenceDescriptions == null) referenceDescriptions = new ArrayList<>();
        Langue langue = langueRepository.findByCode(newDescriptionLangueCode);
        referenceDescriptions.add(new DescriptionItem(newDescriptionValue.trim(), newDescriptionLangueCode, langue));
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
    }

    public void removeDescription(DescriptionItem item) {
        if (referenceDescriptions != null) referenceDescriptions.remove(item);
    }

    public boolean isLangueAlreadyUsedInDescriptions(String code, DescriptionItem exclude) {
        if (referenceDescriptions == null || code == null) return false;
        return referenceDescriptions.stream()
            .filter(i -> i != exclude && i.getLangueCode() != null)
            .anyMatch(i -> i.getLangueCode().equals(code));
    }

    public List<Langue> getAvailableLanguagesForNewDescription() {
        loadAvailableLanguages();
        if (availableLanguages == null) return new ArrayList<>();
        return availableLanguages.stream()
            .filter(l -> !isLangueAlreadyUsedInDescriptions(l.getCode(), null))
            .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void createReference() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Si un fichier local a été sélectionné, l'enregistrer dans le répertoire images avant création
        if (uploadedFilePart != null && uploadedFilePart.getSize() > 0) {
            try {
                String contextPath = facesContext.getExternalContext().getRequestContextPath();
                String url = entityImageService.saveUploadedImage(uploadedFilePart, contextPath);
                String fileName = uploadedFilePart.getSubmittedFileName();
                if (fileName == null || fileName.isBlank()) fileName = "image";
                uploadedImageUrl = url;
                uploadedImageName = fileName;
                uploadedFilePart = null;
            } catch (Exception e) {
                log.error("Erreur lors de l'enregistrement de l'image", e);
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                        "Impossible d'enregistrer l'image : " + (e.getMessage() != null ? e.getMessage() : "erreur inconnue")));
                return;
            }
        }

        boolean validCode;
        if (editingReferenceId != null) {
            validCode = EntityValidator.validateCodeForEdit(referenceCode, editingReferenceId, entityRepository);
        } else {
            validCode = EntityValidator.validateCode(referenceCode, entityRepository);
        }
        if (!validCode) {
            return;
        }

        if (referenceNames == null || referenceNames.isEmpty()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Au moins un nom est requis."));
            return;
        }

        for (NameItem item : referenceNames) {
            if (item.getNom() == null || item.getNom().trim().isEmpty()) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Tous les noms doivent avoir une valeur."));
                return;
            }
        }

        String codeTrimmed = referenceCode.trim();
        EntityType referenceType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_REFERENCE)
                .orElseThrow(() -> new IllegalStateException(
                        "Le type d'entité '" + EntityConstants.ENTITY_TYPE_REFERENCE + "' n'existe pas dans la base de données."));

        Entity newReference = createNewReference(codeTrimmed, referenceType);
        entityRepository.save(newReference);
        saveUserPermissionsForReference(newReference);

        // Rattacher le référentiel à la collection courante si une collection est sélectionnée
        if (applicationBean != null && applicationBean.getSelectedCollection() != null) {
            attachReferenceToCollection(newReference, applicationBean.getSelectedCollection());
        }

        applicationBean.loadReferences();
        searchBean.loadReferences();

        // Recharger les référentiels de la collection
        if (applicationBean != null && applicationBean.getSelectedCollection() != null) {
            applicationBean.refreshCollectionReferencesList();
        }

        treeBean.addReferenceToTree(newReference);

        String labelPrincipal = referenceNames.get(0).getNom();
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                "Le référentiel '" + labelPrincipal + "' a été créé avec succès."));

        prepareCreateReference();

        // Fermer le dialog
        PrimeFaces.current().executeScript("PF('referenceDialog').hide();");

        // Mettre à jour les composants
        PrimeFaces.current().ajax().update(
                ViewConstants.COMPONENT_GROWL + ", :referenceDialogForm, :collectionReferencesContainer, "
                        + ViewConstants.COMPONENT_TREE_WIDGET);
    }

    /**
     * Sauvegarde les modifications du référentiel.
     * Enregistre : code, label (selon langue choisie), description (selon langue choisie),
     * référence bibliographique ; ajoute l'utilisateur courant aux auteurs s'il n'y figure pas.
     */
    @Transactional
    public void saveReference(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() == null) {
            return;
        }

        try {
            Entity referenceToUpdate = entityRepository.findById(applicationBean.getSelectedReference().getId()).get();

            // Mettre à jour le code uniquement si modifié
            String newCode = editingReferenceCode != null ? editingReferenceCode.trim() : null;
            if (!Objects.equals(newCode, referenceToUpdate.getCode()) && newCode != null && !newCode.isEmpty()) {
                referenceToUpdate.setCode(newCode);
            }

            // Mise à jour des labels : priorité à referenceNames (modifier) sinon editingReferenceLabel (legacy)
            if (referenceNames != null && !referenceNames.isEmpty()) {
                if (referenceToUpdate.getLabels() == null) referenceToUpdate.setLabels(new ArrayList<>());
                referenceToUpdate.getLabels().clear();
                for (NameItem item : referenceNames) {
                    if (item.getLangue() != null && StringUtils.hasText(item.getNom())) {
                        Label lbl = new Label();
                        lbl.setNom(item.getNom().trim());
                        lbl.setLangue(item.getLangue());
                        lbl.setEntity(referenceToUpdate);
                        referenceToUpdate.getLabels().add(lbl);
                    }
                }
            } else {
                // Fallback : mise à jour du label pour une seule langue
                String labelLangueCode = editingLabelLangueCode != null ? editingLabelLangueCode : "fr";
                Langue labelLangue = langueRepository.findByCode(labelLangueCode);
                String newLabel = editingReferenceLabel != null ? editingReferenceLabel.trim() : "";
                if (labelLangue != null) {
                    Optional<Label> labelOpt = referenceToUpdate.getLabels() != null
                            ? referenceToUpdate.getLabels().stream()
                            .filter(l -> l.getLangue() != null && labelLangueCode.equalsIgnoreCase(l.getLangue().getCode()))
                            .findFirst()
                            : Optional.empty();
                    if (labelOpt.isPresent()) {
                        labelOpt.get().setNom(newLabel);
                    } else if (!newLabel.isEmpty()) {
                        Label newLabelEntity = new Label();
                        newLabelEntity.setNom(newLabel);
                        newLabelEntity.setEntity(referenceToUpdate);
                        newLabelEntity.setLangue(labelLangue);
                        if (referenceToUpdate.getLabels() == null) referenceToUpdate.setLabels(new ArrayList<>());
                        referenceToUpdate.getLabels().add(newLabelEntity);
                    }
                }
            }

            // Mise à jour des descriptions : priorité à referenceDescriptions (modifier) sinon editingReferenceDescription (legacy)
            if (referenceDescriptions != null && !referenceDescriptions.isEmpty()) {
                if (referenceToUpdate.getDescriptions() == null) referenceToUpdate.setDescriptions(new ArrayList<>());
                referenceToUpdate.getDescriptions().clear();
                for (DescriptionItem item : referenceDescriptions) {
                    if (item.getLangue() != null && StringUtils.hasText(item.getValeur())) {
                        Description desc = new Description();
                        desc.setValeur(item.getValeur().trim());
                        desc.setLangue(item.getLangue());
                        desc.setEntity(referenceToUpdate);
                        referenceToUpdate.getDescriptions().add(desc);
                    }
                }
            } else {
                // Fallback : mise à jour de la description pour une seule langue
                String descLangueCode = editingDescriptionLangueCode != null ? editingDescriptionLangueCode : "fr";
                Langue descLangue = langueRepository != null ? langueRepository.findByCode(descLangueCode) : null;
                String newDesc = editingReferenceDescription != null ? editingReferenceDescription.trim() : "";
                if (descLangue != null) {
                    Optional<Description> descOpt = referenceToUpdate.getDescriptions() != null
                            ? referenceToUpdate.getDescriptions().stream()
                            .filter(d -> d.getLangue() != null && descLangueCode.equalsIgnoreCase(d.getLangue().getCode()))
                            .findFirst()
                            : Optional.empty();
                    if (descOpt.isPresent()) {
                        descOpt.get().setValeur(newDesc);
                    } else if (!newDesc.isEmpty()) {
                        Description newDescription = new Description();
                        newDescription.setValeur(newDesc);
                        newDescription.setEntity(referenceToUpdate);
                        newDescription.setLangue(descLangue);
                        if (referenceToUpdate.getDescriptions() == null) referenceToUpdate.setDescriptions(new ArrayList<>());
                        referenceToUpdate.getDescriptions().add(newDescription);
                    }
                }
            }

            // Mettre à jour la référence bibliographique (liste jointe avec "; ")
            String newBib = (editingReferenceBibliographiqueList != null && !editingReferenceBibliographiqueList.isEmpty())
                    ? editingReferenceBibliographiqueList.stream()
                            .filter(s -> s != null && !s.trim().isEmpty())
                            .map(String::trim)
                            .collect(Collectors.joining("; "))
                    : null;
            if (newBib != null && newBib.isEmpty()) newBib = null;
            String currentBib = referenceToUpdate.getReferenceBibliographique();
            if (!Objects.equals(newBib, currentBib)) {
                referenceToUpdate.setReferenceBibliographique(newBib);
            }

            // Ajouter l'utilisateur courant aux auteurs s'il n'y figure pas
            Utilisateur currentUser = loginBean != null ? loginBean.getCurrentUser() : null;
            if (currentUser != null && currentUser.getId() != null && utilisateurRepository != null) {
                Utilisateur managedUser = utilisateurRepository.findById(currentUser.getId()).orElse(null);
                if (managedUser != null) {
                    List<Utilisateur> auteurs = referenceToUpdate.getAuteurs();
                    if (auteurs == null) {
                        referenceToUpdate.setAuteurs(new ArrayList<>());
                        auteurs = referenceToUpdate.getAuteurs();
                    }
                    boolean alreadyAuthor = auteurs.stream()
                            .anyMatch(u -> u.getId() != null && u.getId().equals(managedUser.getId()));
                    if (!alreadyAuthor) {
                        auteurs.add(managedUser);
                    }
                }
            }

            Entity savedEntity = entityRepository.save(referenceToUpdate);
            saveUserPermissionsForReference(savedEntity);
            applicationBean.setSelectedEntity(savedEntity);

            if (treeBean != null) {
                treeBean.updateEntityInTree(savedEntity);
            }

            applicationBean.getBreadCrumbElements().set(applicationBean.getBreadCrumbElements().size() - 1, applicationBean.getSelectedReference());

            entityEditModeBean.cancelEditing();

            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "Les modifications ont été enregistrées avec succès."));
            }

            log.info("Référentiel mis à jour avec succès: {}", applicationBean.getSelectedReference().getCode());
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du référentiel", e);
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Une erreur est survenue lors de la sauvegarde : " + e.getMessage()));
            }
        }
    }

    /**
     * Supprime le référentiel par son ID et toutes ses entités rattachées (directement ou indirectement).
     * Utilisé depuis la liste des référentiels d'une collection.
     */
    @Transactional
    public void deleteReferenceById(ApplicationBean applicationBean, Long referenceId) {
        if (referenceId == null) {
            addErrorMessage("Aucun référentiel sélectionné.");
            return;
        }
        Entity reference = entityRepository.findById(referenceId).orElse(null);
        if (reference == null) {
            addErrorMessage("Référentiel introuvable.");
            return;
        }
        doDeleteReference(applicationBean, reference);
    }

    /**
     * Supprime le référentiel sélectionné et toutes ses entités rattachées
     */
    @Transactional
    public void deleteReference(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() == null || applicationBean.getSelectedReference().getId() == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Aucun référentiel sélectionné."));
            }
            return;
        }
        doDeleteReference(applicationBean, applicationBean.getSelectedReference());
    }

    private void doDeleteReference(ApplicationBean applicationBean, Entity reference) {

        applicationBean.setSelectedEntity(applicationBean.getSelectedCollection());
        // Supprimer récursivement le référentiel et toutes ses entités enfants
        applicationBean.deleteEntityRecursively(reference);
        applicationBean.setChilds(new ArrayList<>());

        // Recharger les référentiels de la collection
        applicationBean.refreshCollectionReferencesList();

        // Mettre à jour l'arbre
        treeBean.initializeTreeWithCollection();

        // Afficher un message de succès
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le référentiel '" + reference.getNom() + "' et toutes ses entités rattachées ont été supprimés avec succès."));
        }

        collectionBean.showCollectionDetail(applicationBean, applicationBean.getSelectedEntity());

        // Afficher le panel de la collection
        applicationBean.getPanelState().showCollectionDetail();

        log.info("Référentiel supprimé avec succès: {} (ID: {})", referenceCode, reference.getId());
    }

    /**
     * Crée une nouvelle entité référentiel à partir des listes noms/descriptions
     */
    private Entity createNewReference(String code, EntityType type) {
        Entity newReference = new Entity();
        newReference.setCode(code);

        String bibJoined = null;
        if (referenceBibliographiqueList != null && !referenceBibliographiqueList.isEmpty()) {
            bibJoined = referenceBibliographiqueList.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.joining("; "));
            if (bibJoined.isEmpty()) bibJoined = null;
        }
        newReference.setReferenceBibliographique(bibJoined);

        if (StringUtils.hasText(uploadedImageUrl)) {
            Image image = new Image();
            image.setUrl(uploadedImageUrl.trim());
            image.setLegende(StringUtils.hasText(uploadedImageLegende) ? uploadedImageLegende.trim() : null);
            image.setEntity(newReference);
            if (newReference.getImages() == null) {
                newReference.setImages(new ArrayList<>());
            }
            newReference.getImages().add(image);
        }

        newReference.setEntityType(type);
        newReference.setStatut(Boolean.TRUE.equals(referencePublique) ? EntityStatusEnum.PUBLIQUE.name() : EntityStatusEnum.PRIVEE.name());
        newReference.setCreateDate(LocalDateTime.now());

        if (referenceNames != null && !referenceNames.isEmpty()) {
            List<Label> labels = new ArrayList<>();
            for (NameItem item : referenceNames) {
                if (item.getLangue() != null && StringUtils.hasText(item.getNom())) {
                    Label lbl = new Label();
                    lbl.setNom(item.getNom().trim());
                    lbl.setLangue(item.getLangue());
                    lbl.setEntity(newReference);
                    labels.add(lbl);
                }
            }
            newReference.setLabels(labels);
        }

        if (referenceDescriptions != null && !referenceDescriptions.isEmpty()) {
            List<Description> descriptions = new ArrayList<>();
            for (DescriptionItem item : referenceDescriptions) {
                if (item.getLangue() != null && StringUtils.hasText(item.getValeur())) {
                    Description desc = new Description();
                    desc.setValeur(item.getValeur().trim());
                    desc.setLangue(item.getLangue());
                    desc.setEntity(newReference);
                    descriptions.add(desc);
                }
            }
            newReference.setDescriptions(descriptions);
        }

        Utilisateur currentUser = loginBean.getCurrentUser();
        if (currentUser != null) {
            newReference.setCreateBy(currentUser.getEmail());
            List<Utilisateur> auteurs = new ArrayList<>();
            auteurs.add(currentUser);
            newReference.setAuteurs(auteurs);
        }

        return newReference;
    }

    /**
     * Rattache un référentiel à une collection
     */
    private void attachReferenceToCollection(Entity reference, Entity collection) {
        // Vérifier si la relation existe déjà
        if (!entityRelationRepository.existsByParentAndChild(collection.getId(), reference.getId())) {
            fr.cnrs.opentypo.domain.entity.EntityRelation relation =
                    new fr.cnrs.opentypo.domain.entity.EntityRelation();
            relation.setParent(collection);
            relation.setChild(reference);
            entityRelationRepository.save(relation);
        }
    }

    /**
     * Ajoute un message d'erreur
     */
    private void addErrorMessage(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", message));
        PrimeFaces.current().ajax().update(ViewConstants.COMPONENT_GROWL + ", :referenceDialogForm");
    }

    public boolean canEditReference(ApplicationBean applicationBean) {

        if (!loginBean.isAuthenticated() || applicationBean.getSelectedEntity() == null) {
            return false;
        }

        if (loginBean.isAdminTechniqueOrFonctionnel()) {
            return true;
        }

        Long userId = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
        if (userId == null) return false;

        Entity collection = applicationBean.getSelectedCollection();
        if (collection != null && collection.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, collection.getId(),
                PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())) {
            return true;
        }

        if (applicationBean.getSelectedEntity() != null && applicationBean.getSelectedEntity().getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, applicationBean.getSelectedEntity().getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        return false;
    }

    /**
     * Import CSV typologique : administrateurs ou gestionnaire du référentiel sélectionné uniquement.
     */
    public boolean canImportTypology(ApplicationBean applicationBean) {

        if (!loginBean.isAuthenticated() || applicationBean.getSelectedEntity() == null) {
            return false;
        }

        if (applicationBean.getSelectedEntity().getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_REFERENCE.equals(applicationBean.getSelectedEntity().getEntityType().getCode())) {
            return false;
        }

        if (loginBean.isAdminTechniqueOrFonctionnel()) {
            return true;
        }

        Long userId = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
        if (userId == null) {
            return false;
        }

        return userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, applicationBean.getSelectedEntity().getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel());
    }

    public boolean showReferenceStatut() {
        return !loginBean.isAuthenticated() || !loginBean.isAdminTechniqueOrFonctionnel();
    }
}
