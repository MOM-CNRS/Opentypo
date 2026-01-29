package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
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
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
                treeBean.addSerieToTree(savedSerie, applicationBean.getSelectedGroup());
            }

            // Message de succès
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Succès",
                            "La série '" + labelTrimmed + "' a été créée avec succès."));

            resetSerieForm();

            // Mettre à jour les composants : growl, formulaire, arbre, et conteneur des séries
            PrimeFaces.current().ajax().update(":growl, :serieForm, :treeContainer, :seriesContainer");

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
}
