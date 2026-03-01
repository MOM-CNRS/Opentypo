package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.DescriptionItem;
import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.NameItem;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.domain.entity.UserPermission;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.util.EntityUtils;
import fr.cnrs.opentypo.presentation.bean.util.EntityValidator;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.DualListModel;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

    private static final String reference_FORM = ":referenceDialogForm";
    private final CollectionBean collectionBean;

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

    /** PickList pour sélectionner les gestionnaires de référentiel (IDs) - création dialog */
    private DualListModel<Long> gestionnairesPickList;

    /** PickList pour l'édition inline des gestionnaires de référentiel */
    private DualListModel<Long> editingGestionnairesPickList;

    private transient List<String> cachedReferenceGestionnaires;
    private transient Long cachedReferenceGestionnairesEntityId;

    // Propriétés pour le formulaire de création de référentiel
    private String referenceCode;
    private String periodeId; // ID de la période (referenceOpentheso)
    private List<String> referenceBibliographiqueList = new ArrayList<>();
    private String categorieIds; // IDs des catégories (Entity de type Catégorie)

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

    // État d'édition pour le référentiel
    private boolean editingReference = false;
    private String editingReferenceCode;
    private String editingReferenceDescription;
    private String editingReferenceLabel;
    private String editingReferenceBibliographie;
    /** Liste des références bibliographiques pour l'édition inline (stockée avec ";" dans rereference_bibliographique) */
    private List<String> editingReferenceBibliographiqueList = new ArrayList<>();
    /** Code langue pour laquelle on édite le label (ex: fr, en). */
    private String editingLabelLangueCode;
    /** Code langue pour laquelle on édite la description (ex: fr, en). */
    private String editingDescriptionLangueCode;

    @Inject
    public ReferenceBean(@Named("collectionBean") CollectionBean collectionBean) {
        this.collectionBean = collectionBean;
    }


    public void resetReferenceForm() {
        editingReferenceId = null;
        referenceCode = null;
        periodeId = null;
        referenceBibliographiqueList = new ArrayList<>();
        categorieIds = null;
        referenceNames = new ArrayList<>();
        referenceDescriptions = new ArrayList<>();
        newNameValue = null;
        newNameLangueCode = null;
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        referencePublique = true;
        gestionnairesPickList = null;
    }

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
        if (editingGestionnairesPickList == null && editingReference) {
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
    private void initEditingGestionnairesPickListForEdit(Long entityId) {
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

    @Transactional
    protected void saveUserPermissionsForReference(Entity savedReference) {
        if (userPermissionRepository == null || savedReference == null || savedReference.getId() == null) return;
        DualListModel<Long> pickListToUse = (editingReference && editingGestionnairesPickList != null)
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
        resetReferenceForm();
    }

    /**
     * Prépare le dialog pour l'édition à partir de l'ID du référentiel.
     * Utilisé lorsque le passage direct de l'entité depuis ui:repeat pose problème.
     */
    public void prepareEditReferenceInDialogById(Long referenceId) {
        if (referenceId == null) {
            return;
        }
        loadReferenceIntoForm(referenceId);
    }

    /**
     * Prépare le dialog pour l'édition : charge les données du référentiel sélectionné.
     */
    public void prepareEditReferenceInDialog(Entity reference) {
        if (reference == null || reference.getId() == null) {
            return;
        }
        loadReferenceIntoForm(reference.getId());
    }

    /**
     * Charge les données du référentiel dans le formulaire du dialog.
     */
    private void loadReferenceIntoForm(Long referenceId) {
        Entity refLoaded = entityRepository.findById(referenceId)
                .orElseThrow(() -> new IllegalStateException("Référentiel introuvable (id: " + referenceId + ")"));
        editingReferenceId = refLoaded.getId();
        referenceCode = refLoaded.getCode();
        referencePublique = EntityStatusEnum.PUBLIQUE.name().equals(refLoaded.getStatut());
        initGestionnairesPickListForEdit(refLoaded.getId());

        referenceNames = new ArrayList<>();
        if (refLoaded.getLabels() != null) {
            for (Label l : refLoaded.getLabels()) {
                if (l != null && l.getLangue() != null && StringUtils.hasText(l.getNom())) {
                    referenceNames.add(new NameItem(l.getNom(), l.getLangue().getCode(), l.getLangue()));
                }
            }
        }

        referenceDescriptions = new ArrayList<>();
        if (refLoaded.getDescriptions() != null) {
            for (Description d : refLoaded.getDescriptions()) {
                if (d != null && d.getLangue() != null && StringUtils.hasText(d.getValeur())) {
                    referenceDescriptions.add(new DescriptionItem(d.getValeur(), d.getLangue().getCode(), d.getLangue()));
                }
            }
        }

        referenceBibliographiqueList = new ArrayList<>();
        String bib = refLoaded.getBibliographie();
        if (StringUtils.hasText(bib)) {
            for (String part : bib.split(";")) {
                String t = part != null ? part.trim() : "";
                if (!t.isEmpty()) {
                    referenceBibliographiqueList.add(t);
                }
            }
        }

        newNameValue = null;
        newNameLangueCode = null;
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
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
            try {
                availableLanguages = langueRepository.findAllByOrderByNomAsc();
            } catch (Exception e) {
                log.error("Erreur lors du chargement des langues", e);
                availableLanguages = new ArrayList<>();
            }
        }
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

        boolean validCode;
        if (editingReferenceId != null) {
            validCode = EntityValidator.validateCodeForEdit(referenceCode, editingReferenceId, entityRepository, reference_FORM);
        } else {
            validCode = EntityValidator.validateCode(referenceCode, entityRepository, reference_FORM);
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

        if (editingReferenceId != null) {
            updateReferenceFromDialog(facesContext, codeTrimmed);
            return;
        }

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

        resetReferenceForm();

        // Fermer le dialog
        PrimeFaces.current().executeScript("PF('referenceDialog').hide();");

        // Mettre à jour les composants
        PrimeFaces.current().ajax().update(
                ViewConstants.COMPONENT_GROWL + ", " + reference_FORM + ", "
                        + ViewConstants.COMPONENT_TREE_WIDGET + ", :collectionReferencesContainer");
    }

    public void cancelEditingReference() {
        entityEditModeBean.cancelEditing();
        editingReference = false;
        editingReferenceCode = null;
        editingReferenceDescription = null;
        editingReferenceLabel = null;
        editingReferenceBibliographie = null;
        editingReferenceBibliographiqueList = new ArrayList<>();
        editingLabelLangueCode = null;
        editingDescriptionLangueCode = null;
        editingGestionnairesPickList = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Sauvegarde uniquement le code du référentiel en base.
     * Appelé par le bouton Enregistrer à côté du champ Code.
     */
    @Transactional
    public void saveCodeOnly(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedReference() == null) {
            return;
        }

        if (editingReferenceCode == null || editingReferenceCode.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Code requis",
                    "Le code ne peut pas être vide."));
            return;
        }
        Entity referenceToUpdate = entityRepository.findById(applicationBean.getSelectedReference().getId()).orElse(null);
        if (referenceToUpdate == null) return;
        referenceToUpdate.setCode(editingReferenceCode);
        Entity savedEntity = entityRepository.save(referenceToUpdate);
        applicationBean.setSelectedEntity(savedEntity);

        if (treeBean != null) {
            treeBean.updateEntityInTree(savedEntity);
        }

        applicationBean.getBeadCrumbElements().getLast().setCode(editingReferenceCode);

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Enregistré", "Le code a été mis à jour."));
    }

    /**
     * Sauvegarde uniquement la référence bibliographique en base (auto-save).
     * Appelé via p:ajax sur le composant chips à chaque ajout/suppression.
     */
    @Transactional
    public void saveBibliographieOnly(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedReference() == null) {
            return;
        }
        try {
            Entity referenceToUpdate = entityRepository.findById(applicationBean.getSelectedReference().getId())
                    .orElse(null);
            if (referenceToUpdate == null) return;

            String newBib = (editingReferenceBibliographiqueList != null && !editingReferenceBibliographiqueList.isEmpty())
                    ? editingReferenceBibliographiqueList.stream()
                            .filter(s -> s != null && !s.trim().isEmpty())
                            .map(String::trim)
                            .collect(Collectors.joining("; "))
                    : null;
            if (newBib != null && newBib.isEmpty()) newBib = null;

            referenceToUpdate.setReferenceBibliographique(newBib);
            Entity savedEntity = entityRepository.save(referenceToUpdate);
            applicationBean.setSelectedEntity(savedEntity);

            if (treeBean != null) {
                treeBean.updateEntityInTree(savedEntity);
            }

            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Enregistré",
                        "La référence bibliographique a été mise à jour."));
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde de la référence bibliographique", e);
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Une erreur est survenue : " + e.getMessage()));
            }
        }
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

            applicationBean.getBeadCrumbElements().set(applicationBean.getBeadCrumbElements().size() - 1, applicationBean.getSelectedReference());

            entityEditModeBean.cancelEditing();
            editingReference = false;
            editingReferenceCode = null;
            editingReferenceDescription = null;
            editingReferenceLabel = null;
            editingReferenceBibliographie = null;
            editingReferenceBibliographiqueList = new ArrayList<>();
            editingLabelLangueCode = null;
            editingDescriptionLangueCode = null;
            editingGestionnairesPickList = null;

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
        try {
            String referenceCode = reference.getCode();
            String referenceName = reference.getNom();
            Long referenceId = reference.getId();

            // Supprimer récursivement le référentiel et toutes ses entités enfants
            applicationBean.deleteEntityRecursively(reference);

            // Réinitialiser la sélection
            applicationBean.setSelectedEntity(null);
            applicationBean.setChilds(new ArrayList<>());

            // Recharger les référentiels de la collection
            if (applicationBean.getSelectedCollection() != null) {
                applicationBean.refreshCollectionReferencesList();
            }

            // Mettre à jour l'arbre
            treeBean.initializeTreeWithCollection();

            // Afficher un message de succès
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "Le référentiel '" + referenceName + "' et toutes ses entités rattachées ont été supprimés avec succès."));
            }

            // Afficher le panel de la collection
            if (applicationBean.getSelectedReference() != null) {
                applicationBean.getPanelState().showCollectionDetail();
            } else {
                applicationBean.getPanelState().showCollections();
            }

            log.info("Référentiel supprimé avec succès: {} (ID: {})", referenceCode, referenceId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du référentiel", e);
            addErrorMessage("Une erreur est survenue lors de la suppression : " + e.getMessage());
        }
    }

    /**
     * Active le mode édition pour le référentiel sélectionné.
     * Charge le code, la description, le label et la référence bibliographique.
     * Les langues d'édition label/description sont initialisées avec la langue sélectionnée (SearchBean).
     */
    public void startEditingReference(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() == null) {
            return;
        }
        entityEditModeBean.startEditing();
        String codeLang = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        editingReference = true;
        editingReferenceCode = applicationBean.getSelectedReference().getCode() != null ? applicationBean.getSelectedReference().getCode() : "";
        editingLabelLangueCode = codeLang;
        editingDescriptionLangueCode = codeLang;
        editingReferenceDescription = EntityUtils.getDescriptionValueForLanguage(applicationBean.getSelectedReference(), codeLang);
        editingReferenceLabel = EntityUtils.getLabelValueForLanguage(applicationBean.getSelectedReference(), codeLang);
        String bibStr = applicationBean.getSelectedReference().getReferenceBibliographique();
        editingReferenceBibliographie = bibStr != null ? bibStr : "";
        editingReferenceBibliographiqueList = new ArrayList<>();
        if (bibStr != null && !bibStr.isEmpty()) {
            editingReferenceBibliographiqueList = Arrays.stream(bibStr.split("[;；]"))
                    .map(s -> s != null ? s.trim() : "")
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        if (applicationBean.getSelectedReference().getId() != null) {
            initEditingGestionnairesPickListForEdit(applicationBean.getSelectedReference().getId());
        }
        // Initialiser les listes labels et descriptions pour le bloc Traductions/Descriptions du modifier
        initEditingLabelsAndDescriptions(applicationBean.getSelectedReference());
    }

    /**
     * Initialise referenceNames et referenceDescriptions à partir des labels/descriptions
     * du référentiel sélectionné (pour le formulaire modifier).
     */
    private void initEditingLabelsAndDescriptions(Entity reference) {
        if (reference == null) return;
        referenceNames = new ArrayList<>();
        if (reference.getLabels() != null) {
            for (Label l : reference.getLabels()) {
                if (l != null && l.getLangue() != null && StringUtils.hasText(l.getNom())) {
                    referenceNames.add(new NameItem(l.getNom(), l.getLangue().getCode(), l.getLangue()));
                }
            }
        }
        referenceDescriptions = new ArrayList<>();
        if (reference.getDescriptions() != null) {
            for (Description d : reference.getDescriptions()) {
                if (d != null && d.getLangue() != null && StringUtils.hasText(d.getValeur())) {
                    referenceDescriptions.add(new DescriptionItem(d.getValeur(), d.getLangue().getCode(), d.getLangue()));
                }
            }
        }
        newNameValue = null;
        newNameLangueCode = null;
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
    }

    /**
     * Appelé lorsque l'utilisateur change la langue du label dans le menu déroulant.
     * Recharge la valeur du label pour la nouvelle langue depuis l'entité.
     */
    public void onLabelLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() != null && editingLabelLangueCode != null) {
            editingReferenceLabel = EntityUtils.getLabelValueForLanguage(applicationBean.getSelectedReference(), editingLabelLangueCode);
        }
    }

    /**
     * Appelé lorsque l'utilisateur change la langue de la description dans le menu déroulant.
     * Recharge la valeur de la description pour la nouvelle langue depuis l'entité.
     */
    public void onDescriptionLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() != null && editingDescriptionLangueCode != null) {
            editingReferenceDescription = EntityUtils.getDescriptionValueForLanguage(applicationBean.getSelectedReference(), editingDescriptionLangueCode);
        }
    }

    /**
     * Met à jour le référentiel à partir des données du dialog (mode édition).
     */
    @Transactional
    protected void updateReferenceFromDialog(FacesContext facesContext, String codeTrimmed) {
        try {
            Entity referenceToUpdate = entityRepository.findById(editingReferenceId)
                    .orElseThrow(() -> new IllegalStateException("Référentiel introuvable."));

            referenceToUpdate.setCode(codeTrimmed);
            referenceToUpdate.setStatut(Boolean.TRUE.equals(referencePublique) ? EntityStatusEnum.PUBLIQUE.name() : EntityStatusEnum.PRIVEE.name());

            String bibJoined = null;
            if (referenceBibliographiqueList != null && !referenceBibliographiqueList.isEmpty()) {
                bibJoined = referenceBibliographiqueList.stream()
                        .filter(s -> s != null && !s.trim().isEmpty())
                        .map(String::trim)
                        .collect(Collectors.joining("; "));
                if (bibJoined.isEmpty()) bibJoined = null;
            }
            referenceToUpdate.setReferenceBibliographique(bibJoined);

            // Mise à jour des labels
            if (referenceToUpdate.getLabels() == null) {
                referenceToUpdate.setLabels(new ArrayList<>());
            }
            referenceToUpdate.getLabels().clear();
            if (referenceNames != null) {
                for (NameItem item : referenceNames) {
                    if (item.getLangue() != null && StringUtils.hasText(item.getNom())) {
                        Label lbl = new Label();
                        lbl.setNom(item.getNom().trim());
                        lbl.setLangue(item.getLangue());
                        lbl.setEntity(referenceToUpdate);
                        referenceToUpdate.getLabels().add(lbl);
                    }
                }
            }

            // Mise à jour des descriptions
            if (referenceToUpdate.getDescriptions() == null) {
                referenceToUpdate.setDescriptions(new ArrayList<>());
            }
            referenceToUpdate.getDescriptions().clear();
            if (referenceDescriptions != null) {
                for (DescriptionItem item : referenceDescriptions) {
                    if (item.getLangue() != null && StringUtils.hasText(item.getValeur())) {
                        Description desc = new Description();
                        desc.setValeur(item.getValeur().trim());
                        desc.setLangue(item.getLangue());
                        desc.setEntity(referenceToUpdate);
                        referenceToUpdate.getDescriptions().add(desc);
                    }
                }
            }

            entityRepository.save(referenceToUpdate);
            saveUserPermissionsForReference(referenceToUpdate);

            if (editingReferenceId.equals(applicationBean.getSelectedEntity().getId())) {
                applicationBean.setSelectedEntity(referenceToUpdate);
            }

            applicationBean.refreshCollectionReferencesList();

            applicationBean.loadReferences();
            searchBean.loadReferences();

            if (treeBean != null) {
                treeBean.initializeTreeWithCollection();
            }

            String labelPrincipal = (referenceNames != null && !referenceNames.isEmpty())
                    ? referenceNames.get(0).getNom() : codeTrimmed;
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    "Le référentiel '" + labelPrincipal + "' a été modifié avec succès."));

            resetReferenceForm();
            PrimeFaces.current().executeScript("PF('referenceDialog').hide();");
            PrimeFaces.current().ajax().update(
                    ViewConstants.COMPONENT_GROWL + ", " + reference_FORM + ", "
                            + ViewConstants.COMPONENT_TREE_WIDGET + ", :collectionReferencesContainer");
        } catch (IllegalStateException e) {
            log.error("Erreur lors de la modification du référentiel", e);
            addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la modification du référentiel", e);
            addErrorMessage("Une erreur est survenue lors de la modification : " + e.getMessage());
        }
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
        PrimeFaces.current().ajax().update(ViewConstants.COMPONENT_GROWL + ", " + reference_FORM);
    }

    public boolean canCreateReference() {
        return collectionBean.canEditCollectionAsGestionnaire(applicationBean.getSelectedCollection())
                || loginBean.isAdminTechnique();
    }

    public boolean canShowParamsPanel() {
        return canCreateReference() && !editingReference;
    }

    public boolean canEditReference() {

        if (!loginBean.isAuthenticated()) return false;

        boolean isGestionnaireReference = userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                loginBean.getCurrentUser().getId(),
                applicationBean.getSelectedEntity().getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel());

        return loginBean.isAuthenticated() && isGestionnaireReference && !editingReference;
    }

    public boolean showReferenceStatut() {
        return !loginBean.isAuthenticated() || !loginBean.isAdminTechnique();
    }
}
