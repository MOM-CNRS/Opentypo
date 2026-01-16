package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.domain.entity.Description;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    private String collectionDescription;
    private List<Langue> availableLanguages;
    
    // Liste des noms (un par langue)
    private List<NameItem> collectionNames = new ArrayList<>();
    
    // Liste des descriptions (une par langue)
    private List<DescriptionItem> collectionDescriptions = new ArrayList<>();
    
    // Champs temporaires pour la saisie d'un nouveau nom
    private String newNameValue;
    private String newNameLangueCode;
    
    // Champs temporaires pour la saisie d'une nouvelle description
    private String newDescriptionValue;
    private String newDescriptionLangueCode;
    
    // Collection publique ou privée
    private Boolean collectionPublique = true; // Par défaut, la collection est publique
    
    /**
     * Classe interne pour gérer les noms multilingues
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String nom;
        private String langueCode;
        private Langue langue;
    }
    
    /**
     * Classe interne pour gérer les descriptions multilingues
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DescriptionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String valeur;
        private String langueCode;
        private Langue langue;
    }

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
    
    /**
     * Réinitialise tous les champs du formulaire de création de collection
     */
    public void resetCollectionForm() {
        collectionNames = new ArrayList<>();
        collectionDescriptions = new ArrayList<>();
        newNameValue = null;
        newNameLangueCode = null;
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        collectionPublique = true; // Par défaut, la collection est publique
        log.debug("Formulaire de collection réinitialisé");
    }
    
    /**
     * Ajoute un nouveau nom depuis les champs de saisie
     */
    public void addNameFromInput() {
        if (newNameValue == null || newNameValue.trim().isEmpty()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Le nom est requis."));
            }
            return;
        }
        
        if (newNameLangueCode == null || newNameLangueCode.trim().isEmpty()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "La langue est requise."));
            }
            return;
        }
        
        // Vérifier si la langue est déjà utilisée
        if (isLangueAlreadyUsedInNames(newNameLangueCode, null)) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Cette langue est déjà utilisée pour un autre nom."));
            }
            return;
        }
        
        if (collectionNames == null) {
            collectionNames = new ArrayList<>();
        }
        
        Langue langue = langueRepository.findByCode(newNameLangueCode);
        NameItem newItem = new NameItem(newNameValue.trim(), newNameLangueCode, langue);
        collectionNames.add(newItem);
        
        // Réinitialiser les champs de saisie
        newNameValue = null;
        newNameLangueCode = null;
    }
    
    /**
     * Supprime un nom de la liste
     */
    public void removeName(NameItem nameItem) {
        if (collectionNames != null) {
            collectionNames.remove(nameItem);
        }
    }
    
    /**
     * Vérifie si une langue est déjà utilisée dans les noms
     */
    public boolean isLangueAlreadyUsedInNames(String langueCode, NameItem currentItem) {
        if (collectionNames == null || langueCode == null || langueCode.isEmpty()) {
            return false;
        }
        return collectionNames.stream()
            .filter(item -> item != currentItem && item.getLangueCode() != null)
            .anyMatch(item -> item.getLangueCode().equals(langueCode));
    }
    
    /**
     * Obtient les langues disponibles pour un nouveau nom (excluant celles déjà utilisées)
     */
    public List<Langue> getAvailableLanguagesForNewName() {
        if (availableLanguages == null) {
            return new ArrayList<>();
        }
        return availableLanguages.stream()
            .filter(langue -> !isLangueAlreadyUsedInNames(langue.getCode(), null))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Ajoute une nouvelle description depuis les champs de saisie
     */
    public void addDescriptionFromInput() {
        if (newDescriptionValue == null || newDescriptionValue.trim().isEmpty()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "La description est requise."));
            }
            return;
        }
        
        if (newDescriptionLangueCode == null || newDescriptionLangueCode.trim().isEmpty()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "La langue est requise."));
            }
            return;
        }
        
        // Vérifier si la langue est déjà utilisée
        if (isLangueAlreadyUsedInDescriptions(newDescriptionLangueCode, null)) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Attention",
                        "Cette langue est déjà utilisée pour une autre description."));
            }
            return;
        }
        
        if (collectionDescriptions == null) {
            collectionDescriptions = new ArrayList<>();
        }
        
        Langue langue = langueRepository.findByCode(newDescriptionLangueCode);
        DescriptionItem newItem = new DescriptionItem(newDescriptionValue.trim(), newDescriptionLangueCode, langue);
        collectionDescriptions.add(newItem);
        
        // Réinitialiser les champs de saisie
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
    }
    
    /**
     * Supprime une description de la liste
     */
    public void removeDescription(DescriptionItem descriptionItem) {
        if (collectionDescriptions != null) {
            collectionDescriptions.remove(descriptionItem);
        }
    }
    
    /**
     * Vérifie si une langue est déjà utilisée dans les descriptions
     */
    public boolean isLangueAlreadyUsedInDescriptions(String langueCode, DescriptionItem currentItem) {
        if (collectionDescriptions == null || langueCode == null || langueCode.isEmpty()) {
            return false;
        }
        return collectionDescriptions.stream()
            .filter(item -> item != currentItem && item.getLangueCode() != null)
            .anyMatch(item -> item.getLangueCode().equals(langueCode));
    }
    
    /**
     * Obtient les langues disponibles pour une nouvelle description (excluant celles déjà utilisées)
     */
    public List<Langue> getAvailableLanguagesForNewDescription() {
        if (availableLanguages == null) {
            return new ArrayList<>();
        }
        return availableLanguages.stream()
            .filter(langue -> !isLangueAlreadyUsedInDescriptions(langue.getCode(), null))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Met à jour la langue d'un nom
     */
    public void updateNameLangue(NameItem nameItem) {
        if (nameItem != null && nameItem.getLangueCode() != null && !nameItem.getLangueCode().isEmpty()) {
            Langue langue = langueRepository.findByCode(nameItem.getLangueCode());
            nameItem.setLangue(langue);
        }
    }

    public void creerCollection() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        // Validation : au moins un nom doit être défini
        if (collectionNames == null || collectionNames.isEmpty()) {
            addErrorMessage("Au moins un nom est requis.");
            return;
        }

        // Validation : tous les noms doivent avoir un nom et une langue
        for (NameItem nameItem : collectionNames) {
            if (nameItem.getNom() == null || nameItem.getNom().trim().isEmpty()) {
                addErrorMessage("Tous les noms doivent avoir une valeur.");
                return;
            }
            if (nameItem.getLangueCode() == null || nameItem.getLangueCode().trim().isEmpty()) {
                addErrorMessage("Tous les noms doivent avoir une langue sélectionnée.");
                return;
            }
        }

        // Validation : pas de doublons de langues pour les noms
        List<String> languesUtiliseesNames = new ArrayList<>();
        for (NameItem nameItem : collectionNames) {
            if (languesUtiliseesNames.contains(nameItem.getLangueCode())) {
                addErrorMessage("Chaque langue ne peut être utilisée qu'une seule fois pour les noms.");
                return;
            }
            languesUtiliseesNames.add(nameItem.getLangueCode());
        }

        // Validation : toutes les descriptions doivent avoir une langue (si elles ont une valeur)
        for (DescriptionItem descriptionItem : collectionDescriptions) {
            if (descriptionItem.getValeur() != null && !descriptionItem.getValeur().trim().isEmpty()) {
                if (descriptionItem.getLangueCode() == null || descriptionItem.getLangueCode().trim().isEmpty()) {
                    addErrorMessage("Toutes les descriptions doivent avoir une langue sélectionnée.");
                    return;
                }
            }
        }

        // Validation : pas de doublons de langues pour les descriptions
        List<String> languesUtiliseesDescriptions = new ArrayList<>();
        for (DescriptionItem descriptionItem : collectionDescriptions) {
            if (descriptionItem.getLangueCode() != null && !descriptionItem.getLangueCode().trim().isEmpty()) {
                if (languesUtiliseesDescriptions.contains(descriptionItem.getLangueCode())) {
                    addErrorMessage("Chaque langue ne peut être utilisée qu'une seule fois pour les descriptions.");
                    return;
                }
                languesUtiliseesDescriptions.add(descriptionItem.getLangueCode());
            }
        }

        // Utiliser le premier nom comme nom principal et code
        String nomPrincipal = collectionNames.getFirst().getNom().trim();
        String code = nomPrincipal.toUpperCase();

        try {
            EntityType collectionType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_COLLECTION)
                .orElseThrow(() -> new IllegalStateException(
                    "Le type d'entité '" + EntityConstants.ENTITY_TYPE_COLLECTION + "' n'existe pas dans la base de données."));

            Entity nouvelleCollection = createNewCollection(code, nomPrincipal, collectionType);
            entityRepository.save(nouvelleCollection);

            refreshCollectionsList();

            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "La collection '" + nomPrincipal + "' a été créée avec succès."));

            collectionNames = new ArrayList<>();
            collectionDescriptions = new ArrayList<>();
            newNameValue = null;
            newNameLangueCode = null;
            newDescriptionValue = null;
            newDescriptionLangueCode = null;

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
     * Crée une nouvelle entité collection avec les labels et descriptions pour chaque langue
     */
    private Entity createNewCollection(String code, String nomPrincipal, EntityType type) {
        Entity nouvelleCollection = new Entity();
        nouvelleCollection.setCode(code);
        nouvelleCollection.setNom(nomPrincipal);
        nouvelleCollection.setEntityType(type);
        nouvelleCollection.setPublique(collectionPublique != null ? collectionPublique : true);
        nouvelleCollection.setCreateDate(LocalDateTime.now());

        Utilisateur currentUser = loginBean.getCurrentUser();
        if (currentUser != null) {
            nouvelleCollection.setCreateBy(currentUser.getEmail());
            List<Utilisateur> auteurs = new ArrayList<>();
            auteurs.add(currentUser);
            nouvelleCollection.setAuteurs(auteurs);
        }

        // Créer les labels pour chaque langue
        if (collectionNames != null && !collectionNames.isEmpty()) {
            for (NameItem nameItem : collectionNames) {
                if (nameItem.getNom() != null && !nameItem.getNom().trim().isEmpty() &&
                    nameItem.getLangueCode() != null && !nameItem.getLangueCode().trim().isEmpty()) {
                    
                    Langue langue = langueRepository.findByCode(nameItem.getLangueCode());
                    if (langue != null) {
                        Label label = new Label();
                        label.setNom(nameItem.getNom().trim());
                        label.setEntity(nouvelleCollection);
                        label.setLangue(langue);
                        nouvelleCollection.getLabels().add(label);
                    }
                }
            }
        }

        // Créer les descriptions pour chaque langue
        if (collectionDescriptions != null && !collectionDescriptions.isEmpty()) {
            for (DescriptionItem descriptionItem : collectionDescriptions) {
                if (descriptionItem.getValeur() != null && !descriptionItem.getValeur().trim().isEmpty() &&
                    descriptionItem.getLangueCode() != null && !descriptionItem.getLangueCode().trim().isEmpty()) {
                    
                    Langue langue = langueRepository.findByCode(descriptionItem.getLangueCode());
                    if (langue != null) {
                        Description description = new Description();
                        description.setValeur(descriptionItem.getValeur().trim());
                        description.setEntity(nouvelleCollection);
                        description.setLangue(langue);
                        nouvelleCollection.getDescriptions().add(description);
                    }
                }
            }
        }

        return nouvelleCollection;
    }

    /**
     * Recharge les listes de collections dans les beans concernés
     */
    private void refreshCollectionsList() {
        ApplicationBean appBean = applicationBeanProvider.get();
        if (appBean != null) {
            appBean.loadPublicCollections();
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
