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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private SearchBean searchBean;

    private String typeCode;
    private String typeLabel;
    private String typeDescription;

    private boolean editingType = false;
    private String editingTypeCode;
    private String editingLabelLangueCode;
    private String editingTypeLabel;
    private String editingDescriptionLangueCode;
    private String editingTypeDescription;
    private String editingTypeCommentaire;
    private String editingTypeStatut;
    private Integer editingTypeTpq;
    private Integer editingTypeTaq;

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
                treeBean.addEntityToTree(savedType, applicationBean.getSelectedGroup());
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

    public void startEditingType(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null) {
            return;
        }
        Entity type = applicationBean.getSelectedEntity();
        String codeLang = searchBean != null && searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        editingType = true;
        editingTypeCode = type.getCode() != null ? type.getCode() : "";
        editingLabelLangueCode = codeLang;
        editingDescriptionLangueCode = codeLang;
        editingTypeLabel = EntityUtils.getLabelValueForLanguage(type, codeLang);
        editingTypeDescription = EntityUtils.getDescriptionValueForLanguage(type, codeLang);
        editingTypeCommentaire = type.getCommentaire() != null ? type.getCommentaire() : "";
        editingTypeStatut = type.getStatut() != null ? type.getStatut() : "";
        editingTypeTpq = type.getTpq();
        editingTypeTaq = type.getTaq();
    }

    public void cancelEditingType() {
        editingType = false;
        editingTypeCode = null;
        editingLabelLangueCode = null;
        editingDescriptionLangueCode = null;
        editingTypeLabel = null;
        editingTypeDescription = null;
        editingTypeCommentaire = null;
        editingTypeStatut = null;
        editingTypeTpq = null;
        editingTypeTaq = null;
    }

    public void onLabelLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean != null && applicationBean.getSelectedEntity() != null && editingLabelLangueCode != null) {
            editingTypeLabel = EntityUtils.getLabelValueForLanguage(applicationBean.getSelectedEntity(), editingLabelLangueCode);
        }
    }

    public void onDescriptionLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean != null && applicationBean.getSelectedEntity() != null && editingDescriptionLangueCode != null) {
            editingTypeDescription = EntityUtils.getDescriptionValueForLanguage(applicationBean.getSelectedEntity(), editingDescriptionLangueCode);
        }
    }

    @Transactional
    public void saveType(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null) {
            return;
        }
        Entity typeToUpdate = entityRepository.findById(applicationBean.getSelectedEntity().getId()).orElse(null);
        if (typeToUpdate == null) {
            return;
        }

        String newCode = editingTypeCode != null ? editingTypeCode.trim() : null;
        if (newCode != null && !newCode.isEmpty() && !Objects.equals(newCode, typeToUpdate.getCode())) {
            typeToUpdate.setCode(newCode);
        }

        String labelLangueCode = editingLabelLangueCode != null ? editingLabelLangueCode : "fr";
        Langue labelLangue = langueRepository.findByCode(labelLangueCode);
        String newLabel = editingTypeLabel != null ? editingTypeLabel.trim() : "";
        String currentLabelValue = EntityUtils.getLabelValueForLanguage(typeToUpdate, labelLangueCode);
        if (labelLangue != null && !Objects.equals(newLabel, currentLabelValue)) {
            Optional<Label> labelOpt = typeToUpdate.getLabels() != null
                    ? typeToUpdate.getLabels().stream()
                    .filter(l -> l.getLangue() != null && labelLangueCode.equalsIgnoreCase(l.getLangue().getCode()))
                    .findFirst()
                    : Optional.empty();
            if (labelOpt.isPresent()) {
                labelOpt.get().setNom(newLabel);
            } else {
                Label newLabelEntity = new Label();
                newLabelEntity.setNom(newLabel);
                newLabelEntity.setEntity(typeToUpdate);
                newLabelEntity.setLangue(labelLangue);
                if (typeToUpdate.getLabels() == null) {
                    typeToUpdate.setLabels(new ArrayList<>());
                }
                typeToUpdate.getLabels().add(newLabelEntity);
            }
        }

        String descLangueCode = editingDescriptionLangueCode != null ? editingDescriptionLangueCode : "fr";
        Langue descLangue = langueRepository.findByCode(descLangueCode);
        String newDesc = editingTypeDescription != null ? editingTypeDescription.trim() : "";
        String currentDescValue = EntityUtils.getDescriptionValueForLanguage(typeToUpdate, descLangueCode);
        if (descLangue != null && !Objects.equals(newDesc, currentDescValue)) {
            Optional<Description> descOpt = typeToUpdate.getDescriptions() != null
                    ? typeToUpdate.getDescriptions().stream()
                    .filter(d -> d.getLangue() != null && descLangueCode.equalsIgnoreCase(d.getLangue().getCode()))
                    .findFirst()
                    : Optional.empty();
            if (descOpt.isPresent()) {
                descOpt.get().setValeur(newDesc);
            } else {
                Description newDescEntity = new Description();
                newDescEntity.setValeur(newDesc);
                newDescEntity.setEntity(typeToUpdate);
                newDescEntity.setLangue(descLangue);
                if (typeToUpdate.getDescriptions() == null) {
                    typeToUpdate.setDescriptions(new ArrayList<>());
                }
                typeToUpdate.getDescriptions().add(newDescEntity);
            }
        }

        if (editingTypeCommentaire != null) {
            typeToUpdate.setCommentaire(editingTypeCommentaire.trim().isEmpty() ? null : editingTypeCommentaire.trim());
        }
        if (editingTypeStatut != null) {
            typeToUpdate.setStatut(editingTypeStatut.trim().isEmpty() ? null : editingTypeStatut.trim());
        }
        if (editingTypeTpq != null) {
            typeToUpdate.setTpq(editingTypeTpq);
        }
        if (editingTypeTaq != null) {
            typeToUpdate.setTaq(editingTypeTaq);
        }

        Entity saved = entityRepository.save(typeToUpdate);
        applicationBean.setSelectedEntity(saved);
        int idx = applicationBean.getBeadCrumbElements().size() - 1;
        if (idx >= 0) {
            applicationBean.getBeadCrumbElements().set(idx, saved);
        }
        TreeBean tb = treeBeanProvider != null ? treeBeanProvider.get() : null;
        if (tb != null) {
            tb.expandPathAndSelectEntity(saved);
        }
        cancelEditingType();
        applicationBean.refreshGroupTypesList();
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Les modifications du type ont été enregistrées."));
    }

    /**
     * Supprime le type sélectionné (et ses éventuels enfants) de manière récursive.
     */
    @Transactional
    public void deleteType(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null || applicationBean.getSelectedEntity().getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Aucun type sélectionné."));
            return;
        }
        try {
            Entity type = applicationBean.getSelectedEntity();
            String typeCode = type.getCode();
            Long typeId = type.getId();
            Entity parentSerie = applicationBean.getSelectedSerie();

            applicationBean.deleteEntityRecursively(type);

            applicationBean.setSelectedEntity(parentSerie);
            applicationBean.setChilds(parentSerie != null ? new ArrayList<>() : new ArrayList<>());
            if (applicationBean.getBeadCrumbElements().size() > 0) {
                applicationBean.getBeadCrumbElements().remove(applicationBean.getBeadCrumbElements().size() - 1);
            }
            if (parentSerie != null) {
                applicationBean.refreshChilds();
                applicationBean.getPanelState().showSerie();
            } else {
                applicationBean.getPanelState().showCollections();
            }
            TreeBean tb = treeBeanProvider != null ? treeBeanProvider.get() : null;
            if (tb != null) {
                tb.initializeTreeWithCollection();
            }
            cancelEditingType();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                            "Le type '" + typeCode + "' et toutes les entités rattachées ont été supprimés."));
            log.info("Type supprimé avec succès: {} (ID: {})", typeCode, typeId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du type", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                            "Une erreur est survenue lors de la suppression : " + e.getMessage()));
        }
    }
}
