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
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.Candidat;
import fr.cnrs.opentypo.presentation.bean.candidats.CandidatBean;
import fr.cnrs.opentypo.presentation.bean.candidats.converter.CandidatConverter;
import fr.cnrs.opentypo.presentation.bean.util.EntityUtils;
import fr.cnrs.opentypo.presentation.bean.util.EntityValidator;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
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

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private LoginBean loginBean;

    @Inject
    private Provider<TreeBean> treeBeanProvider;
    
    @Inject
    private Provider<ApplicationBean> applicationBeanProvider;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private SearchBean searchBean;

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Inject
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private ApplicationBean applicationBean;

    @Inject
    private CandidatBean candidatBean;

    private String groupCode;
    private String groupLabel;
    private String groupDescription;

    private boolean editingGroup = false;
    private String editingGroupCode;
    private String editingLabelLangueCode;
    private String editingGroupLabel;
    private String editingDescriptionLangueCode;
    private String editingGroupDescription;
    private Integer editingGroupTpq;
    private Integer editingGroupTaq;
    private String editingGroupStatut;
    private String editingGroupAlignementExterne;
    private String editingGroupPeriod;

    // Propriétés pour le dialog de création (sans bibliographique ni visibilité)
    private static final String GROUP_DIALOG_FORM = ":groupDialogForm";
    private List<NameItem> groupNames = new ArrayList<>();
    private List<DescriptionItem> groupDescriptions = new ArrayList<>();
    private String groupDialogCode;
    private String newNameValue;
    private String newNameLangueCode;
    private String newDescriptionValue;
    private String newDescriptionLangueCode;
    private List<Langue> availableLanguages;

    /** Rédacteur sélectionné (1 maximum, optionnel) */
    private Utilisateur selectedRedacteur;
    /** Relecteurs : modèle dual pour PickList (IDs utilisateurs - source = disponibles, target = sélectionnés) */
    private DualListModel<Long> relecteursPickList;

    public void resetGroupForm() {
        groupCode = null;
        groupLabel = null;
        groupDescription = null;
    }

    public void resetGroupDialogForm() {
        groupDialogCode = null;
        groupNames = new ArrayList<>();
        groupDescriptions = new ArrayList<>();
        newNameValue = null;
        newNameLangueCode = null;
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        selectedRedacteur = null;
        initRelecteursPickList();
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
     * Retourne le nom affichable du rédacteur du groupe (rôle "Rédacteur" dans user_permission).
     */
    public String getGroupRedacteurDisplayName(Entity group) {
        if (group == null || group.getId() == null || userPermissionRepository == null) {
            return null;
        }
        List<Long> userIds = userPermissionRepository.findUserIdsByEntityIdAndRole(
                group.getId(), PermissionRoleEnum.REDACTEUR.getLabel());
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }
        return getUtilisateurDisplayName(userIds.get(0));
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
     * Crée une ligne dans user_permission pour le rédacteur (rôle "Rédacteur") et
     * chaque relecteur (rôle "Relecteur") du groupe donné.
     */
    private void saveUserPermissionsForGroup(Entity savedGroup) {
        if (userPermissionRepository == null || savedGroup == null || savedGroup.getId() == null) {
            return;
        }
        // Rédacteur : rôle "Rédacteur"
        if (selectedRedacteur != null && selectedRedacteur.getId() != null) {
            saveUserPermission(savedGroup, selectedRedacteur, PermissionRoleEnum.REDACTEUR.getLabel());
        }

        // Relecteurs : rôle "Relecteur" (on évite les doublons avec le rédacteur)
        List<?> targetList = (relecteursPickList != null && relecteursPickList.getTarget() != null)
                ? relecteursPickList.getTarget() : List.of();
        for (Object raw : targetList) {
            Utilisateur u = resolveUtilisateur(raw);
            if (u != null && u.getId() != null
                    && (selectedRedacteur == null || !u.getId().equals(selectedRedacteur.getId()))) {
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
        ApplicationBean applicationBean = applicationBeanProvider.get();

        if (applicationBean == null || applicationBean.getSelectedCategory() == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Aucune catégorie n'est sélectionnée. Veuillez sélectionner une catégorie avant de créer un groupe."));
            PrimeFaces.current().ajax().update(GROUP_DIALOG_FORM + ", :growl");
            return;
        }

        if (!EntityValidator.validateCode(groupDialogCode, entityRepository, GROUP_DIALOG_FORM)) {
            return;
        }

        if (groupNames == null || groupNames.isEmpty()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Au moins un nom est requis."));
            PrimeFaces.current().ajax().update(GROUP_DIALOG_FORM + ", :growl");
            return;
        }

        for (NameItem item : groupNames) {
            if (item.getNom() == null || item.getNom().trim().isEmpty()) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Tous les noms doivent avoir une valeur."));
                PrimeFaces.current().ajax().update(GROUP_DIALOG_FORM + ", :growl");
                return;
            }
        }

        String codeTrimmed = groupDialogCode.trim();

        try {
            EntityType groupType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_GROUP)
                    .orElseThrow(() -> new IllegalStateException(
                            "Le type d'entité '" + EntityConstants.ENTITY_TYPE_GROUP + "' n'existe pas dans la base de données."));

            Entity newGroup = new Entity();
            newGroup.setCode(codeTrimmed);
            newGroup.setEntityType(groupType);
            newGroup.setPublique(true);
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
            TreeBean tb = treeBeanProvider.get();
            if (tb != null) {
                tb.addEntityToTree(savedGroup, applicationBean.getSelectedCategory());
            }

            // Créer une ligne user_permission par utilisateur sélectionné (rédacteur + relecteurs)
            saveUserPermissionsForGroup(savedGroup);

            String labelPrincipal = groupNames.get(0).getNom();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    "Le groupe '" + labelPrincipal + "' a été créé avec succès."));

            resetGroupDialogForm();
            PrimeFaces.current().executeScript("PF('groupDialog').hide();");
            PrimeFaces.current().ajax().update(":growl, " + GROUP_DIALOG_FORM + ", :groupesContent, :centerContent");
        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création du groupe", e);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage()));
            PrimeFaces.current().ajax().update(GROUP_DIALOG_FORM + ", :growl");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création du groupe", e);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Une erreur est survenue lors de la création du groupe : " + e.getMessage()));
            PrimeFaces.current().ajax().update(GROUP_DIALOG_FORM + ", :growl");
        }
    }

    public void createGroup() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Validation des champs obligatoires
        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateCode(
                groupCode, entityRepository, ":groupForm")) {
            return;
        }

        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateLabel(
                groupLabel, ":groupForm")) {
            return;
        }

        ApplicationBean applicationBean = applicationBeanProvider.get();
        
        // Vérifier qu'une catégorie est sélectionnée
        if (applicationBean == null || applicationBean.getSelectedCategory() == null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Aucune catégorie n'est sélectionnée. Veuillez sélectionner une catégorie avant de créer un groupe."));
            PrimeFaces.current().ajax().update(":growl, :groupForm");
            return;
        }

        String codeTrimmed = groupCode.trim();
        String labelTrimmed = groupLabel.trim();
        String descriptionTrimmed = (groupDescription != null && !groupDescription.trim().isEmpty())
                ? groupDescription.trim() : null;

        try {
            // Récupérer le type d'entité GROUP
            // Essayer d'abord avec "GROUP" puis "GROUPE" pour compatibilité
            EntityType groupType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_GROUP)
                    .orElse(entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_GROUP)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Le type d'entité 'GROUP' ou 'GROUPE' n'existe pas dans la base de données.")));

            // Créer la nouvelle entité groupe
            Entity newGroup = new Entity();
            newGroup.setCode(codeTrimmed);
            newGroup.setCommentaire(descriptionTrimmed);
            newGroup.setEntityType(groupType);
            newGroup.setPublique(true);
            newGroup.setCreateDate(LocalDateTime.now());

            Langue languePrincipale = langueRepository.findByCode(searchBean.getLangSelected());
            if (StringUtils.hasText(labelTrimmed)) {
                Label labelPrincipal = new Label();
                labelPrincipal.setNom(labelTrimmed.trim());
                labelPrincipal.setLangue(languePrincipale);
                labelPrincipal.setEntity(newGroup);
                List<Label> labels = new ArrayList<>();
                labels.add(labelPrincipal);
                newGroup.setLabels(labels);
            }

            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                newGroup.setCreateBy(currentUser.getEmail());
                List<Utilisateur> auteurs = new ArrayList<>();
                auteurs.add(currentUser);
                newGroup.setAuteurs(auteurs);
            }

            // Sauvegarder le groupe
            Entity savedGroup = entityRepository.save(newGroup);

            // Créer la relation entre la catégorie (parent) et le groupe (child)
            if (!entityRelationRepository.existsByParentAndChild(
                    applicationBean.getSelectedCategory().getId(), savedGroup.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(applicationBean.getSelectedCategory());
                relation.setChild(savedGroup);
                entityRelationRepository.save(relation);
            }

            // Recharger la liste des groupes
            applicationBean.refreshCategoryGroupsList();

            // Ajouter le groupe à l'arbre
            TreeBean treeBean = treeBeanProvider.get();
            treeBean.addEntityToTree(savedGroup, applicationBean.getSelectedCategory());

            // Message de succès
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Succès",
                            "Le groupe '" + labelTrimmed + "' a été créé avec succès."));

            resetGroupForm();

            // Mettre à jour les composants : growl, formulaire, arbre, et conteneur des groupes
            PrimeFaces.current().ajax().update(":growl, :groupForm, :groupesContainer");

        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création du groupe", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :groupForm");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création du groupe", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Une erreur est survenue lors de la création du groupe : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :groupForm");
        }
    }

    /**
     * Active le mode édition pour le groupe sélectionné.
     */
    public void startEditingGroup(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null) {
            return;
        }
        Entity group = applicationBean.getSelectedEntity();
        String codeLang = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        editingGroup = true;
        editingGroupCode = group.getCode() != null ? group.getCode() : "";
        editingLabelLangueCode = codeLang;
        editingDescriptionLangueCode = codeLang;
        editingGroupLabel = EntityUtils.getLabelValueForLanguage(group, codeLang);
        editingGroupDescription = EntityUtils.getDescriptionValueForLanguage(group, codeLang);
        editingGroupTpq = group.getTpq();
        editingGroupTaq = group.getTaq();
        editingGroupStatut = group.getStatut() != null ? group.getStatut() : "";
        editingGroupAlignementExterne = group.getAlignementExterne() != null ? group.getAlignementExterne() : "";
        editingGroupPeriod = group.getPeriode() != null ? group.getPeriode().getValeur() : "";
    }

    public void cancelEditingGroup() {
        editingGroup = false;
        editingGroupCode = null;
        editingLabelLangueCode = null;
        editingGroupLabel = null;
        editingDescriptionLangueCode = null;
        editingGroupDescription = null;
        editingGroupTpq = null;
        editingGroupTaq = null;
        editingGroupStatut = null;
        editingGroupAlignementExterne = null;
    }

    public void onLabelLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean != null && applicationBean.getSelectedEntity() != null && editingLabelLangueCode != null) {
            editingGroupLabel = EntityUtils.getLabelValueForLanguage(applicationBean.getSelectedEntity(), editingLabelLangueCode);
        }
    }

    public void onDescriptionLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean != null && applicationBean.getSelectedEntity() != null && editingDescriptionLangueCode != null) {
            editingGroupDescription = EntityUtils.getDescriptionValueForLanguage(applicationBean.getSelectedEntity(), editingDescriptionLangueCode);
        }
    }

    @Transactional
    public void saveGroup(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null) {
            return;
        }
        Entity groupToUpdate = entityRepository.findById(applicationBean.getSelectedEntity().getId()).orElse(null);
        if (groupToUpdate == null) {
            return;
        }

        String newCode = editingGroupCode != null ? editingGroupCode.trim() : null;
        if (newCode != null && !newCode.isEmpty() && !Objects.equals(newCode, groupToUpdate.getCode())) {
            groupToUpdate.setCode(newCode);
        }

        String labelLangueCode = editingLabelLangueCode != null ? editingLabelLangueCode : "fr";
        Langue labelLangue = langueRepository.findByCode(labelLangueCode);
        String newLabel = editingGroupLabel != null ? editingGroupLabel.trim() : "";
        String currentLabelValue = EntityUtils.getLabelValueForLanguage(groupToUpdate, labelLangueCode);
        if (labelLangue != null && !Objects.equals(newLabel, currentLabelValue)) {
            Optional<Label> labelOpt = groupToUpdate.getLabels() != null
                    ? groupToUpdate.getLabels().stream()
                    .filter(l -> l.getLangue() != null && labelLangueCode.equalsIgnoreCase(l.getLangue().getCode()))
                    .findFirst()
                    : Optional.empty();
            if (labelOpt.isPresent()) {
                labelOpt.get().setNom(newLabel);
            } else {
                Label newLabelEntity = new Label();
                newLabelEntity.setNom(newLabel);
                newLabelEntity.setEntity(groupToUpdate);
                newLabelEntity.setLangue(labelLangue);
                if (groupToUpdate.getLabels() == null) {
                    groupToUpdate.setLabels(new ArrayList<>());
                }
                groupToUpdate.getLabels().add(newLabelEntity);
            }
        }

        String descLangueCode = editingDescriptionLangueCode != null ? editingDescriptionLangueCode : "fr";
        Langue descLangue = langueRepository.findByCode(descLangueCode);
        String newDesc = editingGroupDescription != null ? editingGroupDescription.trim() : "";
        String currentDescValue = EntityUtils.getDescriptionValueForLanguage(groupToUpdate, descLangueCode);
        if (descLangue != null && !Objects.equals(newDesc, currentDescValue)) {
            Optional<Description> descOpt = groupToUpdate.getDescriptions() != null
                    ? groupToUpdate.getDescriptions().stream()
                    .filter(d -> d.getLangue() != null && descLangueCode.equalsIgnoreCase(d.getLangue().getCode()))
                    .findFirst()
                    : Optional.empty();
            if (descOpt.isPresent()) {
                descOpt.get().setValeur(newDesc);
            } else {
                Description newDescEntity = new Description();
                newDescEntity.setValeur(newDesc);
                newDescEntity.setEntity(groupToUpdate);
                newDescEntity.setLangue(descLangue);
                if (groupToUpdate.getDescriptions() == null) {
                    groupToUpdate.setDescriptions(new ArrayList<>());
                }
                groupToUpdate.getDescriptions().add(newDescEntity);
            }
        }

        if (editingGroupTpq != null) {
            groupToUpdate.setTpq(editingGroupTpq);
        }
        if (editingGroupTaq != null) {
            groupToUpdate.setTaq(editingGroupTaq);
        }
        if (editingGroupStatut != null) {
            groupToUpdate.setStatut(editingGroupStatut.trim().isEmpty() ? null : editingGroupStatut.trim());
        }
        if (editingGroupAlignementExterne != null) {
            groupToUpdate.setAlignementExterne(editingGroupAlignementExterne.trim().isEmpty() ? null : editingGroupAlignementExterne.trim());
        }

        Entity saved = entityRepository.save(groupToUpdate);
        applicationBean.setSelectedEntity(saved);
        int idx = applicationBean.getBeadCrumbElements().size() - 1;
        if (idx >= 0) {
            applicationBean.getBeadCrumbElements().set(idx, saved);
        }
        TreeBean tb = treeBeanProvider != null ? treeBeanProvider.get() : null;
        if (tb != null) {
            tb.expandPathAndSelectEntity(saved);
        }
        cancelEditingGroup();
        applicationBean.refreshCategoryGroupsList();
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Les modifications du groupe ont été enregistrées."));
    }

    /**
     * Supprime le groupe sélectionné et toutes ses entités enfants (séries, types) de manière récursive.
     */
    @Transactional
    public void deleteGroup(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null || applicationBean.getSelectedEntity().getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Aucun groupe sélectionné."));
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
            if (!applicationBean.getBeadCrumbElements().isEmpty()) {
                applicationBean.getBeadCrumbElements().removeLast();
            }
            if (parentCategory != null) {
                applicationBean.refreshChilds();
                applicationBean.getPanelState().showCategory();
            } else {
                applicationBean.getPanelState().showCollections();
            }
            TreeBean tb = treeBeanProvider != null ? treeBeanProvider.get() : null;
            if (tb != null) {
                tb.initializeTreeWithCollection();
            }
            cancelEditingGroup();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                            "Le groupe '" + groupCode + "' et toutes les entités rattachées ont été supprimés."));
            log.info("Groupe supprimé avec succès: {} (ID: {})", groupCode, groupId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du groupe", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                            "Une erreur est survenue lors de la suppression : " + e.getMessage()));
        }
    }

    public void editCandidat() throws IOException {
        Candidat candidat = new CandidatConverter().convertEntityToCandidat(applicationBean.getSelectedEntity());
        candidatBean.visualiserCandidat(candidat);

        FacesContext.getCurrentInstance().getExternalContext()
                .redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/candidats/view.xhtml");
    }
}
