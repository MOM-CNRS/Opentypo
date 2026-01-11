package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


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

    private String categoryCode;
    private String categoryLabel;
    private String categoryDescription;


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
                    .orElse(entityTypeRepository.findByCode("CATEGORIE")
                            .orElseThrow(() -> new IllegalStateException(
                                    "Le type d'entité 'CATEGORY' ou 'CATEGORIE' n'existe pas dans la base de données.")));

            // Créer la nouvelle entité catégorie
            Entity newCategory = new Entity();
            newCategory.setCode(codeTrimmed);
            newCategory.setNom(labelTrimmed);
            newCategory.setCommentaire(descriptionTrimmed);
            newCategory.setEntityType(categoryType);
            newCategory.setPublique(true);
            newCategory.setCreateDate(LocalDateTime.now());

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
                treeBean.addCategoryToTree(savedCategory, applicationBean.getSelectedReference());
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
}
