package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Image;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import jakarta.annotation.PostConstruct;
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
@Named(value = "collectionBean")
@Slf4j
public class CollectionBean implements Serializable {

    private static final String COLLECTION_FORM = ":collectionForm";

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private LoginBean loginBean;

    @Inject
    private Provider<ApplicationBean> applicationBeanProvider;

    @Inject
    private Provider<SearchBean> searchBeanProvider;

    // Propriétés pour le formulaire de création de collection
    private String collectionNom;
    private String collectionDescription;
    private String collectionLangueCode;
    private List<Langue> availableLanguages;

    @PostConstruct
    public void init() {
        loadAvailableLanguages();
    }

    /**
     * Charge les langues disponibles depuis la base de données
     */
    private void loadAvailableLanguages() {
        try {
            availableLanguages = langueRepository.findAllByOrderByNomAsc();
        } catch (Exception e) {
            log.error("Erreur lors du chargement des langues", e);
            availableLanguages = new ArrayList<>();
        }
    }

    public void resetCollectionForm() {
        collectionNom = null;
        collectionDescription = null;
        collectionLangueCode = null;
    }

    public void creerCollection() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Validation des champs
        if (collectionNom == null || collectionNom.trim().isEmpty()) {
            addErrorMessage("Le nom de la collection est requis.");
            return;
        }

        if (collectionLangueCode == null || collectionLangueCode.trim().isEmpty()) {
            addErrorMessage("La langue du label est requise.");
            return;
        }

        String nomTrimmed = collectionNom.trim();
        String descriptionTrimmed = collectionDescription != null ? collectionDescription.trim() : null;

        try {
            EntityType collectionType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_COLLECTION)
                .orElseThrow(() -> new IllegalStateException(
                    "Le type d'entité '" + EntityConstants.ENTITY_TYPE_COLLECTION + "' n'existe pas dans la base de données."));

            // Récupérer la langue sélectionnée
            Langue langue = langueRepository.findByCode(collectionLangueCode);
            if (langue == null) {
                addErrorMessage("La langue sélectionnée n'existe pas dans la base de données.");
                return;
            }

            Entity nouvelleCollection = createNewCollection(nomTrimmed.toUpperCase(), nomTrimmed, descriptionTrimmed, collectionType, langue);
            entityRepository.save(nouvelleCollection);

            refreshCollectionsList();

            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "La collection '" + nomTrimmed + "' a été créée avec succès."));

            resetCollectionForm();
            PrimeFaces.current().ajax().update(
                ViewConstants.COMPONENT_GROWL + ", " + COLLECTION_FORM + ", " 
                + ViewConstants.COMPONENT_CARDS_CONTAINER + ", :searchForm");

        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création de la collection", e);
            addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la collection", e);
            addErrorMessage("Une erreur est survenue lors de la création de la collection : " + e.getMessage());
        }
    }

    public String getImageUrlOrDefault(Image image) {
        if (image != null && image.getUrl() != null && !image.getUrl().trim().isEmpty()) {
            return image.getUrl();
        }
        // Utiliser le ResourceHandler JSF pour obtenir l'URL correcte de la ressource
        jakarta.faces.context.FacesContext facesContext = jakarta.faces.context.FacesContext.getCurrentInstance();
        if (facesContext != null) {
            jakarta.faces.application.ResourceHandler resourceHandler = facesContext.getApplication().getResourceHandler();
            jakarta.faces.application.Resource resource = resourceHandler.createResource("openTypo-defaut.svg", "img");
            if (resource != null) {
                return resource.getRequestPath();
            }
        }
        // Fallback : chemin direct (fonctionne si contextPath est vide)
        return "/resources/img/openTypo-defaut.svg";
    }

    /**
     * Crée une nouvelle entité collection avec un label dans la langue sélectionnée
     */
    private Entity createNewCollection(String code, String nom, String description, EntityType type, Langue langue) {
        Entity nouvelleCollection = new Entity();
        nouvelleCollection.setCode(code);
        nouvelleCollection.setNom(nom);
        nouvelleCollection.setCommentaire(description);
        nouvelleCollection.setEntityType(type);
        nouvelleCollection.setPublique(true);
        nouvelleCollection.setCreateDate(LocalDateTime.now());

        Utilisateur currentUser = loginBean.getCurrentUser();
        if (currentUser != null) {
            nouvelleCollection.setCreateBy(currentUser.getEmail());
            List<Utilisateur> auteurs = new ArrayList<>();
            auteurs.add(currentUser);
            nouvelleCollection.setAuteurs(auteurs);
        }

        // Créer un label pour le nom dans la langue sélectionnée
        Label label = new Label();
        label.setNom(nom);
        label.setEntity(nouvelleCollection);
        label.setLangue(langue);
        nouvelleCollection.getLabels().add(label);

        return nouvelleCollection;
    }

    /**
     * Recharge les listes de collections dans les beans concernés
     */
    private void refreshCollectionsList() {
        ApplicationBean appBean = applicationBeanProvider.get();
        if (appBean != null) {
            appBean.loadCollections();
        }
        SearchBean sb = searchBeanProvider.get();
        if (sb != null) {
            sb.loadCollections();
        }
    }

    /**
     * Ajoute un message d'erreur
     */
    private void addErrorMessage(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", message));
        PrimeFaces.current().ajax().update(ViewConstants.COMPONENT_GROWL + ", " + COLLECTION_FORM);
    }
}
