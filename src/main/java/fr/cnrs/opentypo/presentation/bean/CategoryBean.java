package fr.cnrs.opentypo.presentation.bean;

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
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.util.EntityUtils;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
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
@Named(value = "categoryBean")
@Slf4j
public class CategoryBean implements Serializable {

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Inject
    private LoginBean loginBean;

    @Inject
    private Provider<TreeBean> treeBeanProvider;
    
    @Inject
    private Provider<ApplicationBean> applicationBeanProvider;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private SearchBean searchBean;

    @Inject
    private TreeBean treeBean;


    private String categoryCode;
    private String categoryLabel;
    private String categoryDescription;

    private boolean editingCategory = false;
    private String editingCategoryCode;
    private String editingLabelLangueCode;
    private String editingCategoryLabel;
    private String editingDescriptionLangueCode;
    private String editingCategoryDescription;
    private String editingCategoryBibliographie;
    private String editingCategoryCommentaire;


    public void resetCategoryForm() {
        categoryCode = null;
        categoryLabel = null;
        categoryDescription = null;
    }

    public void createCategory() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Validation des champs obligatoires
        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateCode(
                categoryCode, entityRepository, ":categoryForm")) {
            return;
        }

        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateLabel(
                categoryLabel, ":categoryForm")) {
            return;
        }

        ApplicationBean applicationBean = applicationBeanProvider.get();
        
        // Vérifier qu'un référentiel est sélectionné
        if (applicationBean == null || applicationBean.getSelectedReference() == null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Aucun référentiel n'est sélectionné. Veuillez sélectionner un référentiel avant de créer une catégorie."));
            PrimeFaces.current().ajax().update(":growl, :categoryForm");
            return;
        }

        String codeTrimmed = categoryCode.trim();
        String labelTrimmed = categoryLabel.trim();
        String descriptionTrimmed = (categoryDescription != null && !categoryDescription.trim().isEmpty())
                ? categoryDescription.trim() : null;

        try {
            // Récupérer le type d'entité CATEGORY
            // Essayer d'abord avec "CATEGORY" puis "CATEGORIE" pour compatibilité
            EntityType categoryType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_CATEGORY)
                    .orElse(entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_CATEGORY)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Le type d'entité 'CATEGORY' ou 'CATEGORIE' n'existe pas dans la base de données.")));

            // Créer la nouvelle entité catégorie
            Entity newCategory = new Entity();
            newCategory.setCode(codeTrimmed);
            newCategory.setCommentaire(descriptionTrimmed);
            newCategory.setEntityType(categoryType);
            newCategory.setPublique(true);
            newCategory.setCreateDate(LocalDateTime.now());

            Langue languePrincipale = langueRepository.findByCode(searchBean.getLangSelected());
            if (!StringUtils.isEmpty(labelTrimmed)) {
                Label labelPrincipal = new Label();
                labelPrincipal.setNom(labelTrimmed.trim());
                labelPrincipal.setLangue(languePrincipale);
                labelPrincipal.setEntity(newCategory);
                List<Label> labels = new ArrayList<>();
                labels.add(labelPrincipal);
                newCategory.setLabels(labels);
            }

            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                newCategory.setCreateBy(currentUser.getEmail());
                List<Utilisateur> auteurs = new ArrayList<>();
                auteurs.add(currentUser);
                newCategory.setAuteurs(auteurs);
            }

            // Sauvegarder la catégorie
            Entity savedCategory = entityRepository.save(newCategory);

            // Créer la relation entre le référentiel (parent) et la catégorie (child)
            if (!entityRelationRepository.existsByParentAndChild(applicationBean.getSelectedReference().getId(), savedCategory.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(applicationBean.getSelectedReference());
                relation.setChild(savedCategory);
                entityRelationRepository.save(relation);
            }

            // Recharger la liste des catégories
            applicationBean.refreshReferenceCategoriesList();

            // Ajouter la catégorie à l'arbre
            TreeBean treeBean = treeBeanProvider.get();
            if (treeBean != null) {
                treeBean.addEntityToTree(savedCategory, applicationBean.getSelectedReference());
            }

            // Message de succès
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Succès",
                            "La catégorie '" + labelTrimmed + "' a été créée avec succès."));

            resetCategoryForm();

            // Mettre à jour les composants : growl, formulaire, arbre, et conteneur des catégories
            PrimeFaces.current().ajax().update(":growl, :categoryForm, :treeContainer, :categoriesContainer");

        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création de la catégorie", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :categoryForm");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la catégorie", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Une erreur est survenue lors de la création de la catégorie : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :categoryForm");
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
        String codeLang = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        editingCategory = true;
        editingCategoryCode = applicationBean.getSelectedEntity().getCode() != null ? applicationBean.getSelectedEntity().getCode() : "";
        editingLabelLangueCode = codeLang;
        editingDescriptionLangueCode = codeLang;
        editingCategoryCommentaire = applicationBean.getSelectedEntity().getCommentaire() != null ? applicationBean.getSelectedEntity().getCommentaire() : "";
        editingCategoryDescription = EntityUtils.getDescriptionValueForLanguage(applicationBean.getSelectedEntity(), codeLang);
        editingCategoryLabel = EntityUtils.getLabelValueForLanguage(applicationBean.getSelectedEntity(), codeLang);
        editingCategoryBibliographie = applicationBean.getSelectedEntity().getBibliographie() != null ? applicationBean.getSelectedEntity().getBibliographie() : "";
    }

    /**
     * Appelé lorsque l'utilisateur change la langue du label dans le menu déroulant.
     * Recharge la valeur du label pour la nouvelle langue depuis l'entité.
     */
    public void onLabelLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedEntity() != null && editingLabelLangueCode != null) {
            editingCategoryLabel = EntityUtils.getLabelValueForLanguage(applicationBean.getSelectedEntity(), editingLabelLangueCode);
        }
    }

    /**
     * Appelé lorsque l'utilisateur change la langue de la description dans le menu déroulant.
     * Recharge la valeur de la description pour la nouvelle langue depuis l'entité.
     */
    public void onDescriptionLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedEntity() != null && editingDescriptionLangueCode != null) {
            editingCategoryDescription = EntityUtils.getDescriptionValueForLanguage(applicationBean.getSelectedEntity(), editingDescriptionLangueCode);
        }
    }

    /**
     * Supprime la category sélectionnée et toutes ses entités rattachées
     */
    @Transactional
    public void deleteReference(ApplicationBean applicationBean) {
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
        String newCode = editingCategoryCode != null ? editingCategoryCode.trim() : null;
        if (!Objects.equals(newCode, categoryToUpdate.getCode()) && newCode != null && !newCode.isEmpty()) {
            categoryToUpdate.setCode(newCode);
        }

        // Langue pour le label (celle choisie dans le menu)
        String labelLangueCode = editingLabelLangueCode != null ? editingLabelLangueCode : "fr";
        Langue labelLangue = langueRepository.findByCode(labelLangueCode);
        // Mettre à jour le label (selon la langue choisie) uniquement si modifié
        String newLabel = editingCategoryLabel != null ? editingCategoryLabel.trim() : "";
        String currentLabelValue = EntityUtils.getLabelValueForLanguage(categoryToUpdate, labelLangueCode);
        if (!Objects.equals(newLabel, currentLabelValue) && labelLangue != null) {
            Optional<Label> labelOpt = categoryToUpdate.getLabels() != null
                    ? categoryToUpdate.getLabels().stream()
                    .filter(l -> l.getLangue() != null && labelLangueCode.equalsIgnoreCase(l.getLangue().getCode()))
                    .findFirst()
                    : Optional.empty();
            if (labelOpt.isPresent()) {
                labelOpt.get().setNom(newLabel);
            } else {
                Label newLabelEntity = new Label();
                newLabelEntity.setNom(newLabel);
                newLabelEntity.setEntity(categoryToUpdate);
                newLabelEntity.setLangue(labelLangue);
                if (categoryToUpdate.getLabels() == null) {
                    categoryToUpdate.setLabels(new ArrayList<>());
                }
                categoryToUpdate.getLabels().add(newLabelEntity);
            }
        }

        // Langue pour la description (celle choisie dans le menu)
        String descLangueCode = editingDescriptionLangueCode != null ? editingDescriptionLangueCode : "fr";
        Langue descLangue = langueRepository != null ? langueRepository.findByCode(descLangueCode) : null;
        // Mettre à jour la description (selon la langue choisie) uniquement si modifiée
        String newDesc = editingCategoryDescription != null ? editingCategoryDescription.trim() : "";
        String currentDescValue = EntityUtils.getDescriptionValueForLanguage(categoryToUpdate, descLangueCode);
        if (!Objects.equals(newDesc, currentDescValue) && descLangue != null) {
            Optional<Description> descOpt = categoryToUpdate.getDescriptions() != null
                    ? categoryToUpdate.getDescriptions().stream()
                    .filter(d -> d.getLangue() != null && descLangueCode.equalsIgnoreCase(d.getLangue().getCode()))
                    .findFirst()
                    : Optional.empty();
            if (descOpt.isPresent()) {
                descOpt.get().setValeur(newDesc);
            } else {
                Description newDescription = new Description();
                newDescription.setValeur(newDesc);
                newDescription.setEntity(categoryToUpdate);
                newDescription.setLangue(descLangue);
                if (categoryToUpdate.getDescriptions() == null) {
                    categoryToUpdate.setDescriptions(new ArrayList<>());
                }
                categoryToUpdate.getDescriptions().add(newDescription);
            }
        }

        // Mettre à jour la bibliographique uniquement si modifiée
        String newBib = editingCategoryBibliographie != null ? editingCategoryBibliographie.trim() : null;
        String currentBib = categoryToUpdate.getBibliographie();
        if (!Objects.equals(newBib, currentBib)) {
            categoryToUpdate.setBibliographie(newBib);
        }

        // Mettre à jour la bibliographique uniquement si modifiée
        String newCom = editingCategoryCommentaire != null ? editingCategoryCommentaire.trim() : null;
        String currentCom = categoryToUpdate.getCommentaire();
        if (!Objects.equals(newCom, currentCom)) {
            categoryToUpdate.setCommentaire(newCom);
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

        applicationBean.getBeadCrumbElements().set(applicationBean.getBeadCrumbElements().size() - 1, categorySaved);

        // Actualiser l'arbre : déplier le chemin et sélectionner la catégorie sauvegardée
        treeBean.expandPathAndSelectEntity(categorySaved);

        editingCategory = false;
        editingCategoryCode = null;
        editingLabelLangueCode = null;
        editingCategoryLabel = null;
        editingDescriptionLangueCode = null;
        editingCategoryDescription = null;
        editingCategoryBibliographie = null;
        editingCategoryCommentaire = null;

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès", "Les modifications ont été enregistrées avec succès."));

        log.info("Référentiel mis à jour avec succès: {}", applicationBean.getSelectedReference().getCode());
    }
}
