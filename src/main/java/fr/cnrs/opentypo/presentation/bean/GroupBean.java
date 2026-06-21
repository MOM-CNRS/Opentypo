package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.DescriptionItem;
import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.NameItem;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.UserPermission;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.domain.entity.Parametrage;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ParametrageRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.application.service.EntityAuthorityService;
import fr.cnrs.opentypo.application.service.EntityCodeUniquenessService;
import fr.cnrs.opentypo.application.service.TypeService;
import fr.cnrs.opentypo.presentation.bean.candidats.CandidatBean;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryDescriptionItem;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryLabelItem;
import fr.cnrs.opentypo.presentation.bean.util.EntityValidator;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Getter
@Setter
@SessionScoped
@Named(value = "groupBean")
@Slf4j
public class GroupBean implements Serializable {

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private LoginBean loginBean;

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Autowired
    private LangueRepository langueRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private ApplicationBean applicationBean;

    @Autowired
    private CandidatBean candidatBean;

    @Autowired
    private EntityEditModeBean entityEditModeBean;

    @Autowired
    private EntityAuthorityService entityAuthorityService;

    @Autowired
    private EntityCodeUniquenessService entityCodeUniquenessService;

    @Autowired
    private TypeService typeService;

    @Autowired
    private TreeBean treeBean;

    @Autowired
    private ReferenceBean referenceBean;

    @Autowired
    private SearchBean searchBean;

    @Autowired
    private ParametrageRepository parametrageRepository;

    private List<NameItem> groupNames = new ArrayList<>();
    private List<DescriptionItem> groupDescriptions = new ArrayList<>();
    private String groupCode;
    private String newNameValue;
    private String groupDescription;
    private String newNameLangueCode;
    private String newDescriptionValue;
    private String newDescriptionLangueCode;
    private List<Langue> availableLanguages;
    private DualListModel<Long> redacteursPickList;
    private DualListModel<Long> relecteursPickList;
    private DualListModel<Long> validateursPickList;


    /**
     * Sauvegarde toutes les modifications du groupe (labels, descriptions, tpq, taq, commentaire).
     * Appelé par le bouton Enregistrer du formulaire modifier.
     */
    @Transactional
    public void saveGroupe(ApplicationBean appBean) {
        if (appBean == null || appBean.getSelectedEntity() == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    JsfMessages.get("common.growl.error"), JsfMessages.get("entity.none.group")));
            return;
        }
        Entity group = appBean.getSelectedEntity();
        if (group.getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    JsfMessages.get("common.growl.error"), JsfMessages.get("entity.invalid.group")));
            return;
        }

        Entity refreshed = entityRepository.findById(group.getId()).orElse(group);
        appBean.setSelectedEntity(refreshed);

        // Labels depuis candidatBean
        List<CategoryLabelItem> labels = candidatBean.getCandidatLabels();
        if (labels != null && !labels.isEmpty()) {
            if (refreshed.getLabels() == null) refreshed.setLabels(new ArrayList<>());
            refreshed.getLabels().clear();
            for (CategoryLabelItem item : labels) {
                if (item.getLangue() != null && StringUtils.hasText(item.getNom())) {
                    Label lbl = new Label();
                    lbl.setNom(item.getNom().trim());
                    lbl.setLangue(item.getLangue());
                    lbl.setEntity(refreshed);
                    refreshed.getLabels().add(lbl);
                }
            }
        }

        // Descriptions depuis candidatBean
        List<CategoryDescriptionItem> descs = candidatBean.getDescriptions();
        if (descs != null && !descs.isEmpty()) {
            if (refreshed.getDescriptions() == null) refreshed.setDescriptions(new ArrayList<>());
            refreshed.getDescriptions().clear();
            for (CategoryDescriptionItem item : descs) {
                if (item.getLangue() != null && StringUtils.hasText(item.getValeur())) {
                    Description desc = new Description();
                    desc.setValeur(item.getValeur().trim());
                    desc.setLangue(item.getLangue());
                    desc.setEntity(refreshed);
                    refreshed.getDescriptions().add(desc);
                }
            }
        }

        refreshed.setTpq(candidatBean.getTpq());
        refreshed.setTaq(candidatBean.getTaq());
        refreshed.setCommentaire(candidatBean.getGroupDescription() != null ? candidatBean.getGroupDescription().trim() : null);

        Entity saved = entityRepository.save(refreshed);
        appBean.setSelectedEntity(saved);

        treeBean.updateEntityInTree(saved);
        entityEditModeBean.cancelEditing();

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO, JsfMessages.get("common.growl.success"), JsfMessages.get("common.save.success.detail")));
        log.info("Groupe modifié avec succès: {} (ID: {})", saved.getCode(), saved.getId());
    }

    public void resetGroupDialogForm() {
        groupCode = null;
        groupNames = new ArrayList<>();
        groupDescriptions = new ArrayList<>();
        newNameValue = null;
        newNameLangueCode = searchBean.getLangSelected();
        newDescriptionValue = null;
        newDescriptionLangueCode = searchBean.getLangSelected();
        initRedacteursPickList();
        initRelecteursPickList();
        initValidateursPickList();
    }

    /** Initialise le PickList des rédacteurs (source = IDs disponibles, target = vide) */
    private void initRedacteursPickList() {
        List<Utilisateur> source = getRedacteursList();
        List<Long> sourceIds = (source != null)
                ? source.stream().map(Utilisateur::getId).filter(Objects::nonNull).toList()
                : List.of();
        redacteursPickList = new DualListModel<>(new ArrayList<>(sourceIds), new ArrayList<>());
    }

    /** Retourne le DualListModel des rédacteurs, initialisé si besoin */
    public DualListModel<Long> getRedacteursPickList() {
        if (redacteursPickList == null) {
            initRedacteursPickList();
        }
        return redacteursPickList;
    }

    /** Initialise le PickList des validateurs (source = IDs disponibles, target = vide) */
    private void initValidateursPickList() {
        List<Utilisateur> source = getValidateursList();
        List<Long> sourceIds = (source != null)
                ? source.stream().map(Utilisateur::getId).filter(Objects::nonNull).toList()
                : List.of();
        validateursPickList = new DualListModel<>(new ArrayList<>(sourceIds), new ArrayList<>());
    }

    /** Retourne le DualListModel des validateurs, initialisé si besoin */
    public DualListModel<Long> getValidateursPickList() {
        if (validateursPickList == null) {
            initValidateursPickList();
        }
        return validateursPickList;
    }

    /** Liste des utilisateurs éligibles comme validateurs (groupe Utilisateur) */
    public List<Utilisateur> getValidateursList() {
        if (utilisateurRepository == null) return new ArrayList<>();
        List<Utilisateur> list = utilisateurRepository.findByGroupeNom(GroupEnum.UTILISATEUR.getLabel());
        return list != null ? list : new ArrayList<>();
    }

    /** Initialise le PickList des relecteurs (source = IDs disponibles, target = vide) */
    private void initRelecteursPickList() {
        List<Utilisateur> source = getRelecteursList();
        List<Long> sourceIds = (source != null)
                ? source.stream().map(Utilisateur::getId).filter(Objects::nonNull).toList()
                : List.of();
        relecteursPickList = new DualListModel<>(new ArrayList<>(sourceIds), new ArrayList<>());
    }

    /** Retourne le DualListModel des relecteurs, initialisé si besoin */
    public DualListModel<Long> getRelecteursPickList() {
        if (relecteursPickList == null) {
            initRelecteursPickList();
        }
        return relecteursPickList;
    }

    /** Libellé affiché pour un utilisateur dans le PickList (à partir de l'ID) */
    public String getUtilisateurDisplayName(Long userId) {
        if (userId == null || utilisateurRepository == null) return "";
        return utilisateurRepository.findById(userId)
                .map(u -> ((u.getPrenom() != null ? u.getPrenom() : "") + " " + (u.getNom() != null ? u.getNom().toUpperCase() : "")).trim())
                .orElse("");
    }

    /** Liste des utilisateurs avec le groupe "Utilisateur" (sélection rédacteur) */
    public List<Utilisateur> getRedacteursList() {
        if (utilisateurRepository == null) return new ArrayList<>();
        List<Utilisateur> list = utilisateurRepository.findByGroupeNom(GroupEnum.UTILISATEUR.getLabel());
        return list != null ? list : new ArrayList<>();
    }

    /** Liste des utilisateurs avec le groupe "Utilisateur" (sélection relecteurs) */
    public List<Utilisateur> getRelecteursList() {
        if (utilisateurRepository == null) return new ArrayList<>();
        List<Utilisateur> list = utilisateurRepository.findByGroupeNom(GroupEnum.UTILISATEUR.getLabel());
        return list != null ? list : new ArrayList<>();
    }

    /**
     * Retourne la liste des noms affichables des rédacteurs du groupe (rôle "Rédacteur" dans user_permission).
     */
    public List<String> getGroupRedacteursDisplayNames(Entity group) {
        if (group == null || group.getId() == null || userPermissionRepository == null) {
            return List.of();
        }
        List<Long> userIds = userPermissionRepository.findUserIdsByEntityIdAndRole(
                group.getId(), PermissionRoleEnum.REDACTEUR.getLabel());
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream()
                .map(this::getUtilisateurDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    /** Bloc intervenants : masqué hors brouillon si aucun rôle/auteur renseigné. */
    public boolean showIntervenantsBlock(Entity group) {
        if (group == null) {
            return false;
        }
        if (EntityStatusEnum.PROPOSITION.name().equals(group.getStatut())) {
            return true;
        }
        return !getGroupRedacteursDisplayNames(group).isEmpty()
                || !getGroupRelecteursDisplayNames(group).isEmpty()
                || !getGroupValidateursDisplayNames(group).isEmpty()
                || (group.getAuteurs() != null && !group.getAuteurs().isEmpty());
    }

    /**
     * Retourne la liste des noms affichables des relecteurs du groupe (rôle "Relecteur" dans user_permission).
     */
    public List<String> getGroupRelecteursDisplayNames(Entity group) {
        if (group == null || group.getId() == null || userPermissionRepository == null) {
            return List.of();
        }
        List<Long> userIds = userPermissionRepository.findUserIdsByEntityIdAndRole(
                group.getId(), PermissionRoleEnum.RELECTEUR.getLabel());
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream()
                .map(this::getUtilisateurDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    /**
     * Retourne la liste des noms affichables des validateurs du groupe (rôle "Valideur" dans user_permission).
     */
    public List<String> getGroupValidateursDisplayNames(Entity group) {
        if (group == null || group.getId() == null || userPermissionRepository == null) {
            return List.of();
        }
        List<Long> userIds = userPermissionRepository.findUserIdsByEntityIdAndRole(
                group.getId(), PermissionRoleEnum.VALIDEUR.getLabel());
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream()
                .map(this::getUtilisateurDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    public void prepareCreateGroup() {
        resetGroupDialogForm();
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

    public boolean isLangueAlreadyUsedInNames(String code, NameItem exclude) {
        if (groupNames == null || code == null) return false;
        return groupNames.stream()
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

    public void addNameFromInput() {
        if (newNameValue == null || newNameValue.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.fieldNameRequired")));
            return;
        }
        if (newNameLangueCode == null || newNameLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.languageRequired")));
            return;
        }
        if (isLangueAlreadyUsedInNames(newNameLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.languageAlreadyUsedName")));
            return;
        }
        if (groupNames == null) groupNames = new ArrayList<>();
        Langue langue = langueRepository.findByCode(newNameLangueCode);
        groupNames.add(new NameItem(newNameValue.trim(), newNameLangueCode, langue));
        newNameValue = null;
        newNameLangueCode = null;
    }

    public void removeName(NameItem item) {
        if (groupNames != null) groupNames.remove(item);
    }

    public boolean isLangueAlreadyUsedInDescriptions(String code, DescriptionItem exclude) {
        if (groupDescriptions == null || code == null) return false;
        return groupDescriptions.stream()
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

    public void addDescriptionFromInput() {
        if (newDescriptionValue == null || newDescriptionValue.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.descriptionRequired")));
            return;
        }
        if (newDescriptionLangueCode == null || newDescriptionLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.languageRequired")));
            return;
        }
        if (isLangueAlreadyUsedInDescriptions(newDescriptionLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, JsfMessages.get("common.growl.warn"), JsfMessages.get("common.validation.languageAlreadyUsedDescription")));
            return;
        }
        if (groupDescriptions == null) groupDescriptions = new ArrayList<>();
        Langue langue = langueRepository.findByCode(newDescriptionLangueCode);
        groupDescriptions.add(new DescriptionItem(newDescriptionValue.trim(), newDescriptionLangueCode, langue));
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
    }

    public void removeDescription(DescriptionItem item) {
        if (groupDescriptions != null) groupDescriptions.remove(item);
    }

    /**
     * Crée une ligne dans user_permission pour chaque rédacteur, validateur et relecteur sélectionné.
     * Un utilisateur ne peut avoir qu'un seul rôle par entité (contrainte user_id + entity_id).
     * Priorité : Rédacteur > Valideur > Relecteur.
     */
    private void saveUserPermissionsForGroup(Entity savedGroup) {
        if (userPermissionRepository == null || savedGroup == null || savedGroup.getId() == null) {
            return;
        }
        java.util.Set<Long> alreadyAssigned = new java.util.HashSet<>();

        // Rédacteurs : rôle "Rédacteur"
        List<?> redacteursTarget = (redacteursPickList != null && redacteursPickList.getTarget() != null)
                ? redacteursPickList.getTarget() : List.of();
        for (Object raw : redacteursTarget) {
            Utilisateur u = resolveUtilisateur(raw);
            if (u != null && u.getId() != null && alreadyAssigned.add(u.getId())) {
                saveUserPermission(savedGroup, u, PermissionRoleEnum.REDACTEUR.getLabel());
            }
        }

        // Validateurs : rôle "Valideur" (uniquement si pas déjà rédacteur)
        List<?> validateursTarget = (validateursPickList != null && validateursPickList.getTarget() != null)
                ? validateursPickList.getTarget() : List.of();
        for (Object raw : validateursTarget) {
            Utilisateur u = resolveUtilisateur(raw);
            if (u != null && u.getId() != null && alreadyAssigned.add(u.getId())) {
                saveUserPermission(savedGroup, u, PermissionRoleEnum.VALIDEUR.getLabel());
            }
        }

        // Relecteurs : rôle "Relecteur" (uniquement si pas déjà rédacteur ou validateur)
        List<?> relecteursTarget = (relecteursPickList != null && relecteursPickList.getTarget() != null)
                ? relecteursPickList.getTarget() : List.of();
        for (Object raw : relecteursTarget) {
            Utilisateur u = resolveUtilisateur(raw);
            if (u != null && u.getId() != null && alreadyAssigned.add(u.getId())) {
                saveUserPermission(savedGroup, u, PermissionRoleEnum.RELECTEUR.getLabel());
            }
        }
    }

    private void saveUserPermission(Entity savedGroup, Utilisateur utilisateur, String role) {
        UserPermission.UserPermissionId id = new UserPermission.UserPermissionId();
        id.setUserId(utilisateur.getId());
        id.setEntityId(savedGroup.getId());
        if (!userPermissionRepository.existsById(id)) {
            UserPermission permission = new UserPermission();
            permission.setUtilisateur(utilisateur);
            permission.setEntity(savedGroup);
            permission.setId(id);
            permission.setRole(role);
            permission.setCreateDate(LocalDateTime.now());
            userPermissionRepository.save(permission);
        }
    }

    /** Résout un objet (Utilisateur, Long ou String) en Utilisateur pour le PickList. */
    private Utilisateur resolveUtilisateur(Object value) {
        if (value == null || utilisateurRepository == null) return null;
        if (value instanceof Utilisateur u) return u;
        Long userId = null;
        if (value instanceof Long l) userId = l;
        else if (value instanceof Number n) userId = n.longValue();
        else if (value instanceof String s && !s.isBlank()) {
            try { userId = Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
        }
        return userId != null ? utilisateurRepository.findById(userId).orElse(null) : null;
    }

    @Transactional
    public void createGroupFromDialog() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        if (applicationBean == null || applicationBean.getSelectedCategory() == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"),
                    JsfMessages.get("entity.parent.required.group")));
            PrimeFaces.current().ajax().update(":groupDialogForm, :growl");
            return;
        }

        if (!canCreateGroup()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"),
                    JsfMessages.get("entity.permission.create.group")));
            PrimeFaces.current().ajax().update(":groupDialogForm, :growl");
            return;
        }

        Entity reference = typeService.findReferenceAncestor(applicationBean.getSelectedCategory());
        if (!EntityValidator.validateGroupCodeForCreate(groupCode, reference, entityCodeUniquenessService)) {
            return;
        }

        if (groupNames == null || groupNames.isEmpty()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"), JsfMessages.get("common.validation.nameRequired")));
            PrimeFaces.current().ajax().update(":groupDialogForm, :growl");
            return;
        }

        for (NameItem item : groupNames) {
            if (item.getNom() == null || item.getNom().trim().isEmpty()) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"), JsfMessages.get("common.validation.nameValueRequired")));
                PrimeFaces.current().ajax().update(":groupDialogForm, :growl");
                return;
            }
        }

        String codeTrimmed = groupCode.trim();

        try {
            EntityType groupType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_GROUP)
                    .orElseThrow(() -> new IllegalStateException(
                            JsfMessages.format("entity.type.missing", EntityConstants.ENTITY_TYPE_GROUP)));

            Entity newGroup = new Entity();
            newGroup.setCode(codeTrimmed);
            newGroup.setEntityType(groupType);
            newGroup.setCreateDate(LocalDateTime.now());

            List<Label> labels = new ArrayList<>();
            for (NameItem ni : groupNames) {
                if (ni != null && ni.getLangueCode() != null && StringUtils.hasText(ni.getNom())) {
                    Langue l = langueRepository.findByCode(ni.getLangueCode());
                    if (l != null) {
                        Label label = new Label();
                        label.setNom(ni.getNom().trim());
                        label.setLangue(l);
                        label.setEntity(newGroup);
                        labels.add(label);
                    }
                }
            }
            newGroup.setLabels(labels);

            List<Description> descriptions = new ArrayList<>();
            List<DescriptionItem> descList = groupDescriptions != null ? groupDescriptions : new ArrayList<>();
            for (DescriptionItem di : descList) {
                if (di != null && di.getLangueCode() != null && StringUtils.hasText(di.getValeur())) {
                    Langue l = langueRepository.findByCode(di.getLangueCode());
                    if (l != null) {
                        Description desc = new Description();
                        desc.setValeur(di.getValeur().trim());
                        desc.setLangue(l);
                        desc.setEntity(newGroup);
                        descriptions.add(desc);
                    }
                }
            }
            newGroup.setDescriptions(descriptions);

            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                newGroup.setCreateBy(currentUser.getEmail());
                List<Utilisateur> auteurs = new ArrayList<>();
                auteurs.add(currentUser);
                newGroup.setAuteurs(auteurs);
            }

            newGroup.setStatut(EntityStatusEnum.PROPOSITION.name());

            Entity savedGroup = entityRepository.save(newGroup);

            if (!entityRelationRepository.existsByParentAndChild(
                    applicationBean.getSelectedCategory().getId(), savedGroup.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(applicationBean.getSelectedCategory());
                relation.setChild(savedGroup);
                entityRelationRepository.save(relation);
            }

            applicationBean.refreshCategoryGroupsList();
            treeBean.addEntityToTree(savedGroup, applicationBean.getSelectedCategory());

            // Créer une ligne user_permission par utilisateur sélectionné (rédacteur + relecteurs)
            saveUserPermissionsForGroup(savedGroup);

            // Copier le paramétrage OpenTheso de la référence parente si il existe
            copyParametrageFromReferenceToGroup(applicationBean.getSelectedCategory(), savedGroup);

            String labelPrincipal = groupNames.get(0).getNom();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, JsfMessages.get("common.growl.success"),
                    JsfMessages.format("entity.create.success.group", labelPrincipal)));

            resetGroupDialogForm();
            PrimeFaces.current().executeScript("PF('groupDialog').hide();");
            PrimeFaces.current().ajax().update(":growl, :groupDialogForm, :groupesContent, :centerContent");
        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création du groupe", e);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"), e.getMessage()));
            PrimeFaces.current().ajax().update(":groupDialogForm, :growl");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création du groupe", e);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"),
                    JsfMessages.format("common.error.create.group", e.getMessage())));
            PrimeFaces.current().ajax().update(":groupDialogForm, :growl");
        }
    }

    /**
     * Copie le paramétrage OpenTheso et/ou la bibliographie Zotero du référentiel parent vers le nouveau groupe.
     */
    private void copyParametrageFromReferenceToGroup(Entity category, Entity newGroup) {
        if (category == null || newGroup == null || newGroup.getId() == null || parametrageRepository == null) {
            return;
        }
        Entity reference = typeService != null ? typeService.findReferenceAncestor(category) : null;
        if (reference == null || reference.getId() == null) {
            return;
        }
        Optional<Parametrage> refParamOpt = parametrageRepository.findByEntityId(reference.getId());
        if (refParamOpt.isEmpty()) {
            return;
        }
        Parametrage refParam = refParamOpt.get();

        boolean hasOpenTheso = StringUtils.hasText(refParam.getBaseUrl())
                && StringUtils.hasText(refParam.getIdTheso());
        boolean hasBibliographie = StringUtils.hasText(refParam.getBibliographieUrl());

        if (!hasOpenTheso && !hasBibliographie) {
            return;
        }

        Parametrage groupParam = new Parametrage();
        groupParam.setEntity(entityRepository.findById(newGroup.getId()).orElse(newGroup));
        if (hasOpenTheso) {
            groupParam.setBaseUrl(refParam.getBaseUrl());
            groupParam.setIdTheso(refParam.getIdTheso());
            groupParam.setIdLangue(refParam.getIdLangue());
            groupParam.setIdGroupe(refParam.getIdGroupe());
        }
        if (hasBibliographie) {
            groupParam.setBibliographieUrl(refParam.getBibliographieUrl());
        }
        parametrageRepository.save(groupParam);
        log.debug("Paramétrage hérité du référentiel {} vers le nouveau groupe {}", reference.getCode(), newGroup.getCode());
    }

    /**
     * Supprime le groupe sélectionné et toutes ses entités enfants (séries, types) de manière récursive.
     */
    @Transactional
    public void deleteGroup(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null || applicationBean.getSelectedEntity().getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"), JsfMessages.get("entity.none.group")));
            return;
        }
        if (!canDeleteOrChangeVisibilityGroup(applicationBean)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"),
                            JsfMessages.get("entity.permission.delete.group")));
            return;
        }
        try {
            Entity group = applicationBean.getSelectedEntity();
            String groupCode = group.getCode();
            Long groupId = group.getId();
            Entity parentCategory = applicationBean.getSelectedCategory();

            applicationBean.deleteEntityRecursively(group);

            applicationBean.setSelectedEntity(parentCategory);
            applicationBean.setChilds(new ArrayList<>());
            if (!applicationBean.getBreadCrumbElements().isEmpty()) {
                applicationBean.getBreadCrumbElements().removeLast();
            }
            if (parentCategory != null) {
                applicationBean.refreshChilds();
                applicationBean.getPanelState().showCategory();
            } else {
                applicationBean.getPanelState().showCollections();
            }

            treeBean.initializeTreeWithCollection();

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, JsfMessages.get("common.growl.success"),
                            JsfMessages.format("entity.delete.success.group", groupCode)));
            log.info("Groupe supprimé avec succès: {} (ID: {})", groupCode, groupId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du groupe", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"),
                            JsfMessages.format("common.error.delete", e.getMessage())));
        }
    }

    public boolean canCreateGroup() {
        if (!loginBean.isAuthenticated() || loginBean.getCurrentUser() == null) {
            return false;
        }
        Entity parent = applicationBean.getSelectedCategory() != null
                ? applicationBean.getSelectedCategory()
                : applicationBean.getSelectedEntity();
        if (parent == null || parent.getId() == null) {
            return false;
        }
        return entityAuthorityService.canCreate(
                loginBean.getCurrentUser(),
                EntityConstants.ENTITY_TYPE_GROUP,
                parent.getId());
    }

    public boolean canDeleteOrChangeVisibilityGroup(ApplicationBean applicationBean) {
        return canManageGroupActions(applicationBean);
    }

    public boolean canEditGroup(ApplicationBean applicationBean) {
        return canManageGroupActions(applicationBean);
    }

    /**
     * Actions de gestion sur un groupe (paramétrage, historique, modifier, supprimer, visibilité) :
     * administrateur technique/fonctionnel, gestionnaire du référentiel d'ancrage,
     * ou rédacteur du groupe.
     */
    public boolean canManageGroupActions(ApplicationBean applicationBean) {
        if (applicationBean == null) {
            return false;
        }
        return canManageGroupEntity(applicationBean.getSelectedEntity());
    }

    private boolean canManageGroupEntity(Entity group) {
        if (!loginBean.isAuthenticated() || loginBean.getCurrentUser() == null || group == null || group.getId() == null) {
            return false;
        }
        if (!isGroupEntity(group)) {
            return false;
        }
        if (loginBean.isAdminTechniqueOrFonctionnel()) {
            return true;
        }
        Long userId = loginBean.getCurrentUser().getId();
        Entity reference = typeService.findReferenceAncestor(group);
        if (reference != null && reference.getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                        userId, reference.getId(), PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        return userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                userId, group.getId(), PermissionRoleEnum.REDACTEUR.getLabel());
    }

    private boolean isGroupEntity(Entity entity) {
        if (entity.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_GROUP.equals(entity.getEntityType().getCode())) {
            return true;
        }
        return entityRepository.findById(entity.getId())
                .map(e -> e.getEntityType() != null
                        && EntityConstants.ENTITY_TYPE_GROUP.equals(e.getEntityType().getCode()))
                .orElse(false);
    }


    /**
     * Indique si l'utilisateur connecté peut publier ou refuser une proposition (groupe).
     * Visible si : administrateur technique, gestionnaire de la collection, validateur du groupe,
     * ou gestionnaire de la référence contenant le groupe.
     */
    public boolean canPublishOrRefuseProposition(ApplicationBean applicationBean) {
        if (!loginBean.isAuthenticated() || applicationBean.getSelectedEntity() == null
                || !EntityStatusEnum.PROPOSITION.name().equals(applicationBean.getSelectedEntity().getStatut())) {
            return false;
        }
        if (loginBean.isAdminTechniqueOrFonctionnel()) {
            return true;
        }
        Long userId = loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getId() : null;
        if (userId == null) return false;

        if (applicationBean.getSelectedCollection() != null && applicationBean.getSelectedCollection().getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, applicationBean.getSelectedCollection().getId(),
                PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())) {
            return true;
        }
        if (userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, applicationBean.getSelectedEntity().getId(),
                PermissionRoleEnum.VALIDEUR.getLabel())) {
            return true;
        }

        if (applicationBean.getSelectedReference() != null && applicationBean.getSelectedReference().getId() != null
                && userPermissionRepository.existsByUserIdAndEntityIdAndRole(userId, applicationBean.getSelectedReference().getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel())) {
            return true;
        }
        return false;
    }
}
