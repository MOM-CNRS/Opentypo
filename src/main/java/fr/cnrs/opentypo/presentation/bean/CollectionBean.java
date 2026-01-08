package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
    private LoginBean loginBean;

    @Inject
    private ApplicationBean applicationBean;

    @Inject
    private SearchBean searchBean;

    // Propriétés pour le formulaire de création de collection
    private String collectionNom;
    private String collectionDescription;

    public void resetCollectionForm() {
        collectionNom = null;
        collectionDescription = null;
    }

    public void creerCollection() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Validation des champs
        if (collectionNom == null || collectionNom.trim().isEmpty()) {
            addErrorMessage("Le nom de la collection est requis.");
            return;
        }

        String nomTrimmed = collectionNom.trim();
        String descriptionTrimmed = collectionDescription != null ? collectionDescription.trim() : null;

        try {
            EntityType collectionType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_COLLECTION)
                .orElseThrow(() -> new IllegalStateException(
                    "Le type d'entité '" + EntityConstants.ENTITY_TYPE_COLLECTION + "' n'existe pas dans la base de données."));

            // Générer un code unique pour la collection
            String code = generateUniqueCode(nomTrimmed);

            Entity nouvelleCollection = createNewCollection(code, nomTrimmed, descriptionTrimmed, collectionType);
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

    /**
     * Génère un code unique pour la collection basé sur le nom
     */
    private String generateUniqueCode(String nom) {
        // Créer un code à partir du nom (en majuscules, sans espaces, avec préfixe)
        String baseCode = "COL_" + nom.toUpperCase()
            .replaceAll("[^A-Z0-9]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
        
        // Vérifier si le code existe déjà, sinon ajouter un suffixe
        String code = baseCode;
        int suffix = 1;
        while (entityRepository.existsByCode(code)) {
            code = baseCode + "_" + suffix;
            suffix++;
        }
        
        return code;
    }

    /**
     * Crée une nouvelle entité collection
     */
    private Entity createNewCollection(String code, String nom, String description, EntityType type) {
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

        return nouvelleCollection;
    }

    /**
     * Recharge les listes de collections dans les beans concernés
     */
    private void refreshCollectionsList() {
        if (applicationBean != null) {
            applicationBean.loadCollections();
        }
        if (searchBean != null) {
            searchBean.loadCollections();
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
