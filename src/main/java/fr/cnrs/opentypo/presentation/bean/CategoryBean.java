package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.DescriptionItem;
import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.NameItem;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.Candidat;
import fr.cnrs.opentypo.presentation.bean.util.EntityValidator;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Getter
@Setter
@SessionScoped
@Named(value = "categoryBean")
@Slf4j
public class CategoryBean implements Serializable {

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private LoginBean loginBean;

    @Autowired
    private Provider<TreeBean> treeBeanProvider;

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Autowired
    private LangueRepository langueRepository;

    @Autowired
    private TreeBean treeBean;

    @Autowired
    private ApplicationBean applicationBean;

    @Autowired
    private UserPermissionRepository userPermissionRepository;
    
    private boolean editingCategory = false;
    private List<NameItem> categoryNames = new ArrayList<>();
    private List<DescriptionItem> categoryDescriptions = new ArrayList<>();

    private String categoryCode;
    private String labelLangueCode;
    private String categoryLabel;
    private String descriptionLangueCode;
    private String categoryDescription;
    private String categoryBibliographie;
    private String categoryCommentaire;
    private Boolean categoryPublique = true;
    private List<Langue> availableLanguages;


    public void resetCategoryDialogForm() {
        categoryCode = null;
        categoryNames = new ArrayList<>();
        categoryDescriptions = new ArrayList<>();
        categoryPublique = true;
        editingCategory = false;
        labelLangueCode = null;
        categoryLabel = null;
        descriptionLangueCode = null;
        categoryDescription = null;
        categoryBibliographie = null;
        categoryCommentaire = null;
    }

    public List<Langue> getAvailableLanguagesForNewName() {

        if (availableLanguages == null) {
            availableLanguages = langueRepository.findAllByOrderByNomAsc();
        }

        return availableLanguages.stream()
                .filter(l -> !isLangueAlreadyUsedInNames(l.getCode(), null))
                .collect(java.util.stream.Collectors.toList());
    }

    public boolean isLangueAlreadyUsedInNames(String code, NameItem exclude) {
        if (categoryNames == null || code == null) return false;
        return categoryNames.stream()
                .filter(element -> element != exclude && element.getLangueCode() != null)
                .anyMatch(element -> element.getLangueCode().equalsIgnoreCase(code));
    }

    public void addNameFromInput() {
        if (categoryLabel == null || categoryLabel.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Le nom est requis."));
            return;
        }
        if (labelLangueCode == null || labelLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La langue est requise."));
            return;
        }
        if (isLangueAlreadyUsedInNames(labelLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Cette langue est déjà utilisée pour un autre nom."));
            return;
        }
        if (categoryNames == null) categoryNames = new ArrayList<>();
        Langue langue = langueRepository.findByCode(labelLangueCode);
        categoryNames.add(new NameItem(categoryLabel.trim(), labelLangueCode, langue));
        categoryLabel = null;
        labelLangueCode = null;
    }

    public void removeName(NameItem item) {
        if (categoryNames != null) {
            categoryNames = categoryNames.stream()
                    .filter(element -> !element.getNom().equalsIgnoreCase(item.getNom())
                            && !element.getLangueCode().equalsIgnoreCase(item.getLangueCode()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    public boolean isLangueAlreadyUsedInDescriptions(String code, DescriptionItem exclude) {
        if (categoryDescriptions == null || code == null) return false;
        return categoryDescriptions.stream()
                .filter(i -> i != exclude && i.getLangueCode() != null)
                .anyMatch(i -> i.getLangueCode().equals(code));
    }

    public List<Langue> getAvailableLanguagesForNewDescription() {

        if (availableLanguages == null) {
            availableLanguages = langueRepository.findAllByOrderByNomAsc();
        }

        return availableLanguages.stream()
                .filter(l -> !isLangueAlreadyUsedInDescriptions(l.getCode(), null))
                .collect(java.util.stream.Collectors.toList());
    }

    public void addDescriptionFromInput() {
        if (categoryDescription == null || categoryDescription.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La description est requise."));
            return;
        }
        if (descriptionLangueCode == null || descriptionLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La langue est requise."));
            return;
        }
        if (isLangueAlreadyUsedInDescriptions(descriptionLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Cette langue est déjà utilisée pour une autre description."));
            return;
        }
        if (categoryDescriptions == null) categoryDescriptions = new ArrayList<>();

        categoryDescriptions.add(new DescriptionItem(categoryDescription.trim(), descriptionLangueCode,
                langueRepository.findByCode(descriptionLangueCode)));
        categoryDescription = null;
        descriptionLangueCode = null;
    }

    public void removeDescription(DescriptionItem item) {
        if (categoryDescriptions != null) {
            categoryDescriptions = categoryDescriptions.stream()
                    .filter(element -> !element.getValeur().equalsIgnoreCase(item.getValeur())
                            && !element.getLangueCode().equalsIgnoreCase(item.getLangueCode()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    @Transactional
    public void createCategoryFromDialog() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        if (applicationBean == null || applicationBean.getSelectedReference() == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Aucun référentiel n'est sélectionné. Veuillez sélectionner un référentiel avant de créer une catégorie."));
            PrimeFaces.current().ajax().update(":categoryDialogForm, :growl");
            return;
        }

        if (!EntityValidator.validateCode(categoryCode, entityRepository, ":categoryDialogForm")) {
            return;
        }

        if (categoryNames == null || categoryNames.isEmpty()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Au moins un nom est requis."));
            PrimeFaces.current().ajax().update(":categoryDialogForm, :growl");
            return;
        }

        for (NameItem item : categoryNames) {
            if (item.getNom() == null || item.getNom().trim().isEmpty()) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Tous les noms doivent avoir une valeur."));
                PrimeFaces.current().ajax().update(":categoryDialogForm, :growl");
                return;
            }
        }

        try {
            EntityType categoryType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_CATEGORY)
                    .orElseThrow(() -> new IllegalStateException(
                            "Le type d'entité '" + EntityConstants.ENTITY_TYPE_CATEGORY + "' n'existe pas dans la base de données."));

            Entity newCategory = new Entity();
            newCategory.setCode(categoryCode.trim());
            newCategory.setBibliographie(categoryBibliographie);
            newCategory.setEntityType(categoryType);
            newCategory.setStatut(EntityStatusEnum.ACCEPTED.name());
            newCategory.setPublique(categoryPublique != null ? categoryPublique : true);
            newCategory.setCreateDate(LocalDateTime.now());
            newCategory.setMetadataCommentaire(categoryCommentaire);

            List<Label> labels = new ArrayList<>();
            for (NameItem ni : categoryNames) {
                if (ni != null && ni.getLangueCode() != null && StringUtils.hasText(ni.getNom())) {
                    Langue l = langueRepository.findByCode(ni.getLangueCode());
                    if (l != null) {
                        Label label = new Label();
                        label.setNom(ni.getNom().trim());
                        label.setLangue(l);
                        label.setEntity(newCategory);
                        labels.add(label);
                    }
                }
            }
            newCategory.setLabels(labels);

            List<Description> descriptions = new ArrayList<>();
            List<DescriptionItem> descList = categoryDescriptions != null ? categoryDescriptions : new ArrayList<>();
            for (DescriptionItem di : descList) {
                if (di != null && di.getLangueCode() != null && StringUtils.hasText(di.getValeur())) {
                    Langue l = langueRepository.findByCode(di.getLangueCode());
                    if (l != null) {
                        Description desc = new Description();
                        desc.setValeur(di.getValeur().trim());
                        desc.setLangue(l);
                        desc.setEntity(newCategory);
                        descriptions.add(desc);
                    }
                }
            }
            newCategory.setDescriptions(descriptions);

            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                newCategory.setCreateBy(currentUser.getEmail());
                List<Utilisateur> auteurs = new ArrayList<>();
                auteurs.add(currentUser);
                newCategory.setAuteurs(auteurs);
            }

            Entity savedCategory = entityRepository.save(newCategory);

            if (!entityRelationRepository.existsByParentAndChild(applicationBean.getSelectedReference().getId(), savedCategory.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(applicationBean.getSelectedReference());
                relation.setChild(savedCategory);
                entityRelationRepository.save(relation);
            }

            applicationBean.refreshReferenceCategoriesList();
            treeBean.addEntityToTree(savedCategory, applicationBean.getSelectedReference());

            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    "La catégorie '" + categoryNames.get(0).getNom() + "' a été créée avec succès."));

            resetCategoryDialogForm();
            PrimeFaces.current().executeScript("PF('categoryDialog').hide();");
            PrimeFaces.current().ajax().update(":growl, :categoryDialogForm, :categoriesContainer, :centerContent");
        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création de la catégorie", e);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage()));
            PrimeFaces.current().ajax().update(":categoryDialogForm, :growl");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la catégorie", e);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    "Une erreur est survenue lors de la création de la catégorie : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":categoryDialogForm, :growl");
        }
    }

    /**
     * Active le mode édition pour le référentiel sélectionné.
     * Charge le code, la description, le label et la référence bibliographique.
     * Les langues d'édition label/description sont initialisées avec la langue sélectionnée (SearchBean).
     */
    public void startEditingCategory(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedEntity() == null) {
            return;
        }

        editingCategory = true;
        categoryCode = applicationBean.getSelectedEntity().getCode() != null ? applicationBean.getSelectedEntity().getCode() : "";
        categoryDescription = "";
        categoryLabel = "";
        categoryNames = applicationBean.getSelectedEntity().getLabels().stream()
                .map(element -> NameItem.builder()
                        .nom(element.getNom())
                        .langueCode(element.getLangue().getCode())
                        .langue(element.getLangue())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        categoryDescriptions = applicationBean.getSelectedEntity().getDescriptions().stream()
                .map(element -> DescriptionItem.builder()
                        .valeur(element.getValeur())
                        .langueCode(element.getLangue().getCode())
                        .langue(element.getLangue())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
        categoryBibliographie = applicationBean.getSelectedEntity().getBibliographie() != null ? applicationBean.getSelectedEntity().getBibliographie() : "";
        categoryCommentaire = applicationBean.getSelectedEntity().getMetadataCommentaire() != null ? applicationBean.getSelectedEntity().getMetadataCommentaire() : "";
    }

    /**
     * Supprime la category sélectionnée et toutes ses entités rattachées
     */
    @Transactional
    public void deleteCategory(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedEntity() == null || applicationBean.getSelectedEntity().getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur", "Aucune category sélectionnée."));
            return;
        }

        try {
            String referenceCode = applicationBean.getSelectedEntity().getCode();
            Long referenceId = applicationBean.getSelectedEntity().getId();

            // Supprimer récursivement le référentiel et toutes ses entités enfants
            applicationBean.deleteEntityRecursively(applicationBean.getSelectedEntity());

            // Réinitialiser la sélection
            applicationBean.setSelectedEntity(null);
            applicationBean.setChilds(new ArrayList<>());
            int index = applicationBean.getBeadCrumbElements().size() - 1;
            applicationBean.getBeadCrumbElements().remove(index);

            // Mettre à jour l'arbre
            treeBean.initializeTreeWithCollection();

            // Afficher un message de succès
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès", "Le référentiel '" + referenceCode + "' et toutes ses entités rattachées ont été supprimés avec succès."));

            // Afficher le panel de la collection
            if (applicationBean.getSelectedReference() != null) {
                applicationBean.getPanelState().showCollectionDetail();
            } else {
                applicationBean.getPanelState().showCollections();
            }

            log.info("Référentiel supprimé avec succès: {} (ID: {})", referenceCode, referenceId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du référentiel", e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur", "Une erreur est survenue lors de la suppression : " + e.getMessage()));
        }
    }

    /**
     * Sauvegarde les modifications du référentiel.
     * Enregistre : code, label (selon langue choisie), description (selon langue choisie),
     * référence bibliographique ; ajoute l'utilisateur courant aux auteurs s'il n'y figure pas.
     */
    @Transactional
    public void saveCategory(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedEntity() == null) {
            return;
        }

        Entity categoryToUpdate = entityRepository.findById(applicationBean.getSelectedEntity().getId()).get();

        // Mettre à jour le code uniquement si modifié
        String newCode = categoryCode != null ? categoryCode.trim() : null;
        if (!Objects.equals(newCode, categoryToUpdate.getCode()) && newCode != null && !newCode.isEmpty()) {
            categoryToUpdate.setCode(newCode);
        }

        // Mise à jour des labels
        if (categoryToUpdate.getLabels() == null) {
            categoryToUpdate.setLabels(new ArrayList<>());
        }
        if (categoryNames != null) {
            for (NameItem item : categoryNames) {
                if (item.getLangue() != null && StringUtils.hasText(item.getNom())) {
                    Label lbl = new Label();
                    lbl.setNom(item.getNom().trim());
                    lbl.setLangue(item.getLangue());
                    lbl.setEntity(categoryToUpdate);
                    categoryToUpdate.getLabels().add(lbl);
                }
            }
        }

        // Mise à jour des descriptions
        if (categoryToUpdate.getDescriptions() == null) {
            categoryToUpdate.setDescriptions(new ArrayList<>());
        }
        if (categoryDescriptions != null) {
            for (DescriptionItem item : categoryDescriptions) {
                if (item.getLangue() != null && StringUtils.hasText(item.getValeur())) {
                    Description desc = new Description();
                    desc.setValeur(item.getValeur().trim());
                    desc.setLangue(item.getLangue());
                    desc.setEntity(categoryToUpdate);
                    categoryToUpdate.getDescriptions().add(desc);
                }
            }
        }

        // Mettre à jour la bibliographique uniquement si modifiée
        String newCommentaire = categoryCommentaire != null ? categoryCommentaire.trim() : null;
        if (!Objects.equals(newCommentaire, categoryToUpdate.getMetadataCommentaire())) {
            categoryToUpdate.setMetadataCommentaire(newCommentaire);
        }

        String newBibliographie = categoryBibliographie != null ? categoryBibliographie.trim() : null;
        if (!Objects.equals(newBibliographie, categoryToUpdate.getBibliographie())) {
            categoryToUpdate.setBibliographie(newBibliographie);
        }

        // Ajouter l'utilisateur courant aux auteurs s'il n'y figure pas
        Utilisateur currentUser = loginBean != null ? loginBean.getCurrentUser() : null;
        if (currentUser != null && currentUser.getId() != null && utilisateurRepository != null) {
            Utilisateur managedUser = utilisateurRepository.findById(currentUser.getId()).orElse(null);
            if (managedUser != null) {
                List<Utilisateur> auteurs = categoryToUpdate.getAuteurs();
                if (auteurs == null) {
                    categoryToUpdate.setAuteurs(new ArrayList<>());
                    auteurs = categoryToUpdate.getAuteurs();
                }
                boolean alreadyAuthor = auteurs.stream()
                        .anyMatch(u -> u.getId() != null && u.getId().equals(managedUser.getId()));
                if (!alreadyAuthor) {
                    auteurs.add(managedUser);
                }
            }
        }

        Entity categorySaved = entityRepository.save(categoryToUpdate);
        applicationBean.setSelectedEntity(categorySaved);

        treeBean.updateEntityInTree(categorySaved);
        treeBean.expandPathAndSelectEntity(categorySaved);

        applicationBean.getBeadCrumbElements().set(applicationBean.getBeadCrumbElements().size() - 1, categorySaved);

        resetCategoryDialogForm();

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès", "Les modifications ont été enregistrées avec succès."));

        log.info("Référentiel mis à jour avec succès: {}", applicationBean.getSelectedReference().getCode());
    }

    public boolean canCreateCategory() {
        if (!loginBean.isAuthenticated()) return false;

        boolean isGestionnaireReference = userPermissionRepository.existsByUserIdAndEntityIdAndRole(
                loginBean.getCurrentUser().getId(),
                applicationBean.getSelectedEntity().getId(),
                PermissionRoleEnum.GESTIONNAIRE_REFERENTIEL.getLabel());

        return isGestionnaireReference || loginBean.isAdminTechnique();
    }
}
