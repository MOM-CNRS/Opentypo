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

    private String groupCode;
    private String groupLabel;
    private String groupDescription;

    public void resetGroupForm() {
        groupCode = null;
        groupLabel = null;
        groupDescription = null;
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
                    .orElse(entityTypeRepository.findByCode("GROUPE")
                            .orElseThrow(() -> new IllegalStateException(
                                    "Le type d'entité 'GROUP' ou 'GROUPE' n'existe pas dans la base de données.")));

            // Créer la nouvelle entité groupe
            Entity newGroup = new Entity();
            newGroup.setCode(codeTrimmed);
            newGroup.setNom(labelTrimmed);
            newGroup.setCommentaire(descriptionTrimmed);
            newGroup.setEntityType(groupType);
            newGroup.setPublique(true);
            newGroup.setCreateDate(LocalDateTime.now());

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
            if (treeBean != null) {
                treeBean.addGroupToTree(savedGroup, applicationBean.getSelectedCategory());
            }

            // Message de succès
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Succès",
                            "Le groupe '" + labelTrimmed + "' a été créé avec succès."));

            resetGroupForm();

            // Mettre à jour les composants : growl, formulaire, arbre, et conteneur des groupes
            PrimeFaces.current().ajax().update(":growl, :groupForm, :treeContainer, :groupesContainer");

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
}
