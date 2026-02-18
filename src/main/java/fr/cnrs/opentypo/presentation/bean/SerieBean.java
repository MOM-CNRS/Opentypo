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
@Named(value = "serieBean")
@Slf4j
public class SerieBean implements Serializable {

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

    private String serieCode;
    private String serieLabel;
    private String serieDescription;

    private boolean editingSerie = false;
    private String editingSerieCode;
    private String editingLabelLangueCode;
    private String editingSerieLabel;
    private String editingDescriptionLangueCode;
    private String editingSerieDescription;
    private String editingSerieCommentaire;
    private String editingSerieAppellation;
    private String editingSerieStatut;
    private Integer editingSerieTpq;
    private Integer editingSerieTaq;

    public void resetSerieForm() {
        serieCode = null;
        serieLabel = null;
        serieDescription = null;
    }

    public void createSerie() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Validation des champs obligatoires
        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateCode(
                serieCode, entityRepository, ":serieForm")) {
            return;
        }

        if (!fr.cnrs.opentypo.presentation.bean.util.EntityValidator.validateLabel(
                serieLabel, ":serieForm")) {
            return;
        }

        ApplicationBean applicationBean = applicationBeanProvider.get();
        
        // Vérifier qu'un groupe est sélectionné
        if (applicationBean == null || applicationBean.getSelectedGroup() == null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Aucun groupe n'est sélectionné. Veuillez sélectionner un groupe avant de créer une série."));
            PrimeFaces.current().ajax().update(":growl, :serieForm");
            return;
        }

        String codeTrimmed = serieCode.trim();
        String labelTrimmed = serieLabel.trim();
        String descriptionTrimmed = (serieDescription != null && !serieDescription.trim().isEmpty())
                ? serieDescription.trim() : null;

        try {
            // Récupérer le type d'entité SERIES
            // Essayer d'abord avec "SERIES" puis "SERIE" pour compatibilité
            EntityType serieType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_SERIES)
                    .orElse(entityTypeRepository.findByCode("SERIE")
                            .orElseThrow(() -> new IllegalStateException(
                                    "Le type d'entité 'SERIES' ou 'SERIE' n'existe pas dans la base de données.")));

            // Créer la nouvelle entité série
            Entity newSerie = new Entity();
            newSerie.setCode(codeTrimmed);
            newSerie.setCommentaire(descriptionTrimmed);
            newSerie.setEntityType(serieType);
            newSerie.setPublique(true);
            newSerie.setCreateDate(LocalDateTime.now());

            Langue languePrincipale = langueRepository.findByCode(searchBean.getLangSelected());
            if (!StringUtils.isEmpty(labelTrimmed)) {
                Label labelPrincipal = new Label();
                labelPrincipal.setNom(labelTrimmed.trim());
                labelPrincipal.setLangue(languePrincipale);
                labelPrincipal.setEntity(newSerie);
                List<Label> labels = new ArrayList<>();
                labels.add(labelPrincipal);
                newSerie.setLabels(labels);
            }

            Utilisateur currentUser = loginBean.getCurrentUser();
            if (currentUser != null) {
                newSerie.setCreateBy(currentUser.getEmail());
                List<Utilisateur> auteurs = new ArrayList<>();
                auteurs.add(currentUser);
                newSerie.setAuteurs(auteurs);
            }

            // Sauvegarder la série
            Entity savedSerie = entityRepository.save(newSerie);

            // Créer la relation entre le groupe (parent) et la série (child)
            if (!entityRelationRepository.existsByParentAndChild(
                    applicationBean.getSelectedGroup().getId(), savedSerie.getId())) {
                EntityRelation relation = new EntityRelation();
                relation.setParent(applicationBean.getSelectedGroup());
                relation.setChild(savedSerie);
                entityRelationRepository.save(relation);
            }

            // Recharger la liste des séries
            applicationBean.refreshGroupSeriesList();

            // Ajouter la série à l'arbre
            TreeBean treeBean = treeBeanProvider.get();
            if (treeBean != null) {
                treeBean.addEntityToTree(savedSerie, applicationBean.getSelectedGroup());
            }

            // Message de succès
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Succès",
                            "La série '" + labelTrimmed + "' a été créée avec succès."));

            resetSerieForm();

            // Mettre à jour les composants : growl, formulaire, arbre, et conteneur des séries
            PrimeFaces.current().ajax().update(":growl, :serieForm, :seriesContainer");

        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création de la série", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :serieForm");
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la série", e);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur",
                            "Une erreur est survenue lors de la création de la série : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl, :serieForm");
        }
    }

    public void startEditingSerie(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null) {
            return;
        }
        Entity serie = applicationBean.getSelectedEntity();
        String codeLang = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        editingSerie = true;
        editingSerieCode = serie.getCode() != null ? serie.getCode() : "";
        editingLabelLangueCode = codeLang;
        editingDescriptionLangueCode = codeLang;
        editingSerieLabel = EntityUtils.getLabelValueForLanguage(serie, codeLang);
        editingSerieDescription = EntityUtils.getDescriptionValueForLanguage(serie, codeLang);
        editingSerieCommentaire = serie.getCommentaire() != null ? serie.getCommentaire() : "";
        editingSerieAppellation = serie.getAppellation() != null ? serie.getAppellation() : "";
        editingSerieStatut = serie.getStatut() != null ? serie.getStatut() : "";
        editingSerieTpq = serie.getTpq();
        editingSerieTaq = serie.getTaq();
    }

    public void cancelEditingSerie() {
        editingSerie = false;
        editingSerieCode = null;
        editingLabelLangueCode = null;
        editingDescriptionLangueCode = null;
        editingSerieLabel = null;
        editingSerieDescription = null;
        editingSerieCommentaire = null;
        editingSerieAppellation = null;
        editingSerieStatut = null;
        editingSerieTpq = null;
        editingSerieTaq = null;
    }

    public void onLabelLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean != null && applicationBean.getSelectedEntity() != null && editingLabelLangueCode != null) {
            editingSerieLabel = EntityUtils.getLabelValueForLanguage(applicationBean.getSelectedEntity(), editingLabelLangueCode);
        }
    }

    public void onDescriptionLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean != null && applicationBean.getSelectedEntity() != null && editingDescriptionLangueCode != null) {
            editingSerieDescription = EntityUtils.getDescriptionValueForLanguage(applicationBean.getSelectedEntity(), editingDescriptionLangueCode);
        }
    }

    @Transactional
    public void saveSerie(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null) {
            return;
        }
        Entity serieToUpdate = entityRepository.findById(applicationBean.getSelectedEntity().getId()).orElse(null);
        if (serieToUpdate == null) {
            return;
        }

        String newCode = editingSerieCode != null ? editingSerieCode.trim() : null;
        if (newCode != null && !newCode.isEmpty() && !Objects.equals(newCode, serieToUpdate.getCode())) {
            serieToUpdate.setCode(newCode);
        }

        String labelLangueCode = editingLabelLangueCode != null ? editingLabelLangueCode : "fr";
        Langue labelLangue = langueRepository.findByCode(labelLangueCode);
        String newLabel = editingSerieLabel != null ? editingSerieLabel.trim() : "";
        String currentLabelValue = EntityUtils.getLabelValueForLanguage(serieToUpdate, labelLangueCode);
        if (labelLangue != null && !Objects.equals(newLabel, currentLabelValue)) {
            Optional<Label> labelOpt = serieToUpdate.getLabels() != null
                    ? serieToUpdate.getLabels().stream()
                    .filter(l -> l.getLangue() != null && labelLangueCode.equalsIgnoreCase(l.getLangue().getCode()))
                    .findFirst()
                    : Optional.empty();
            if (labelOpt.isPresent()) {
                labelOpt.get().setNom(newLabel);
            } else {
                Label newLabelEntity = new Label();
                newLabelEntity.setNom(newLabel);
                newLabelEntity.setEntity(serieToUpdate);
                newLabelEntity.setLangue(labelLangue);
                if (serieToUpdate.getLabels() == null) {
                    serieToUpdate.setLabels(new ArrayList<>());
                }
                serieToUpdate.getLabels().add(newLabelEntity);
            }
        }

        String descLangueCode = editingDescriptionLangueCode != null ? editingDescriptionLangueCode : "fr";
        Langue descLangue = langueRepository.findByCode(descLangueCode);
        String newDesc = editingSerieDescription != null ? editingSerieDescription.trim() : "";
        String currentDescValue = EntityUtils.getDescriptionValueForLanguage(serieToUpdate, descLangueCode);
        if (descLangue != null && !Objects.equals(newDesc, currentDescValue)) {
            Optional<Description> descOpt = serieToUpdate.getDescriptions() != null
                    ? serieToUpdate.getDescriptions().stream()
                    .filter(d -> d.getLangue() != null && descLangueCode.equalsIgnoreCase(d.getLangue().getCode()))
                    .findFirst()
                    : Optional.empty();
            if (descOpt.isPresent()) {
                descOpt.get().setValeur(newDesc);
            } else {
                Description newDescEntity = new Description();
                newDescEntity.setValeur(newDesc);
                newDescEntity.setEntity(serieToUpdate);
                newDescEntity.setLangue(descLangue);
                if (serieToUpdate.getDescriptions() == null) {
                    serieToUpdate.setDescriptions(new ArrayList<>());
                }
                serieToUpdate.getDescriptions().add(newDescEntity);
            }
        }

        if (editingSerieCommentaire != null) {
            serieToUpdate.setCommentaire(editingSerieCommentaire.trim().isEmpty() ? null : editingSerieCommentaire.trim());
        }
        if (editingSerieAppellation != null) {
            serieToUpdate.setAppellation(editingSerieAppellation.trim().isEmpty() ? null : editingSerieAppellation.trim());
        }
        if (editingSerieStatut != null) {
            serieToUpdate.setStatut(editingSerieStatut.trim().isEmpty() ? null : editingSerieStatut.trim());
        }
        if (editingSerieTpq != null) {
            serieToUpdate.setTpq(editingSerieTpq);
        }
        if (editingSerieTaq != null) {
            serieToUpdate.setTaq(editingSerieTaq);
        }

        Entity saved = entityRepository.save(serieToUpdate);
        applicationBean.setSelectedEntity(saved);
        int idx = applicationBean.getBeadCrumbElements().size() - 1;
        if (idx >= 0) {
            applicationBean.getBeadCrumbElements().set(idx, saved);
        }
        TreeBean tb = treeBeanProvider != null ? treeBeanProvider.get() : null;
        if (tb != null) {
            tb.expandPathAndSelectEntity(saved);
        }
        cancelEditingSerie();
        applicationBean.refreshGroupSeriesList();
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Les modifications de la série ont été enregistrées."));
    }

    /**
     * Supprime la série sélectionnée et toutes ses entités enfants (types) de manière récursive.
     */
    @Transactional
    public void deleteSerie(ApplicationBean applicationBean) {
        if (applicationBean == null || applicationBean.getSelectedEntity() == null || applicationBean.getSelectedEntity().getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Aucune série sélectionnée."));
            return;
        }
        try {
            Entity serie = applicationBean.getSelectedEntity();
            String serieCode = serie.getCode();
            Long serieId = serie.getId();
            Entity parentGroup = applicationBean.getSelectedGroup();

            applicationBean.deleteEntityRecursively(serie);

            applicationBean.setSelectedEntity(parentGroup);
            applicationBean.setChilds(parentGroup != null ? new ArrayList<>() : new ArrayList<>());
            if (applicationBean.getBeadCrumbElements().size() > 0) {
                applicationBean.getBeadCrumbElements().remove(applicationBean.getBeadCrumbElements().size() - 1);
            }
            if (parentGroup != null) {
                applicationBean.refreshChilds();
                applicationBean.getPanelState().showGroupe();
            } else {
                applicationBean.getPanelState().showCollections();
            }
            TreeBean tb = treeBeanProvider != null ? treeBeanProvider.get() : null;
            if (tb != null) {
                tb.initializeTreeWithCollection();
            }
            cancelEditingSerie();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                            "La série '" + serieCode + "' et toutes les entités rattachées ont été supprimées."));
            log.info("Série supprimée avec succès: {} (ID: {})", serieCode, serieId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la série", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                            "Une erreur est survenue lors de la suppression : " + e.getMessage()));
        }
    }
}
