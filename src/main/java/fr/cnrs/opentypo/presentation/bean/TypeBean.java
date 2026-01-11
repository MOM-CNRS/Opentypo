package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
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
@Named(value = "typeBean")
@Slf4j
public class TypeBean implements Serializable {

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

    private String typeCode;
    private String typeLabel;
    private String typeDescription;

    public void resetTypeForm() {
        typeCode = null;
        typeLabel = null;
        typeDescription = null;
    }

    public void createType() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Validation des champs obligatoires
        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateCode(
                typeCode, entityRepository, ":typeForm")) {
            return;
        }

        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateLabel(
                typeLabel, ":typeForm")) {
            return;
        }

        ApplicationBean applicationBean = applicationBeanProvider.get();
        
        // Vérifier qu'un groupe est sélectionné
        if (applicationBean == null || applicationBean.getSelectedGroup() == null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Aucun groupe n'est sélectionné. Veuillez sélectionner un groupe avant de créer un type."));
            PrimeFaces.current().ajax().update(":growl, :typeForm");
            return;
        }

        String codeTrimmed = typeCode.trim();
        String labelTrimmed = typeLabel.trim();
        String descriptionTrimmed = (typeDescription != null && !typeDescription.trim().isEmpty())
                ? typeDescription.trim() : null;

        try {
            // Récupérer le type d'entité TYPE
            EntityType typeEntityType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_TYPE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Le type d'entité 'TYPE' n'existe pas dans la base de données."));

            // Créer la nouvelle entité type
            Entity newType = new Entity();
            newType.setCode(codeTrimmed);
            newType.setNom(labelTrimmed);
            newType.setCommentaire(descriptionTrimmed);
            newType.setEntityType(typeEntityType);
            newType.setPublique(true);
            newType.setCreateDate(LocalDateTime.now());

            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                newType.setCreateBy(currentUser.getEmail());
                List<Utilisateur> auteurs = new ArrayList<>();
                auteurs.add(currentUser);
                newType.setAuteurs(auteurs);
            }

            // Sauvegarder le type
            Entity savedType = entityRepository.save(newType);

            // Créer la relation entre le groupe (parent) et le type (child)
            if (!entityRelationRepository.existsByParentAndChild(
                    applicationBean.getSelectedGroup().getId(), savedType.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(applicationBean.getSelectedGroup());
                relation.setChild(savedType);
                entityRelationRepository.save(relation);
            }

            // Recharger la liste des types
            applicationBean.refreshGroupTypesList();

            // Ajouter le type à l'arbre
            TreeBean treeBean = treeBeanProvider.get();
            if (treeBean != null) {
                treeBean.addTypeToTree(savedType, applicationBean.getSelectedGroup());
            }

            // Message de succès
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Succès",
                            "Le type '" + labelTrimmed + "' a été créé avec succès."));

            resetTypeForm();

            // Mettre à jour les composants : growl, formulaire, arbre, et conteneur des types
            PrimeFaces.current().ajax().update(":growl, :typeForm, :treeContainer, :typesContainer");

        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création du type", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :typeForm");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création du type", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Une erreur est survenue lors de la création du type : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :typeForm");
        }
    }
}
