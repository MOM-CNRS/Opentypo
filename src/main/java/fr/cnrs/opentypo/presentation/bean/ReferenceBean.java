package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.DescriptionItem;
import fr.cnrs.opentypo.application.dto.NameItem;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.common.constant.ViewConstants;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.Entity;
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
import fr.cnrs.opentypo.presentation.bean.util.EntityValidator;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Getter
@Setter
@SessionScoped
@Named(value = "referenceBean")
@Slf4j
public class ReferenceBean implements Serializable {

    private static final String reference_FORM = ":referenceDialogForm";

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private LoginBean loginBean;

    @Autowired
    private TreeBean treeBean;
    
    @Autowired
    private ApplicationBean applicationBean;
    
    @Autowired
    private SearchBean searchBean;

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Autowired
    private LangueRepository langueRepository;


    // Propriétés pour le formulaire de création de référentiel
    private String referenceCode;
    private String periodeId; // ID de la période (referenceOpentheso)
    private List<String> referenceBibliographiqueList = new ArrayList<>();
    private String categorieIds; // IDs des catégories (Entity de type Catégorie)

    // Liste des noms (labels) multilingues
    private List<NameItem> referenceNames = new ArrayList<>();
    // Liste des descriptions multilingues
    private List<DescriptionItem> referenceDescriptions = new ArrayList<>();
    // Champs temporaires pour la saisie d'un nouveau nom
    private String newNameValue;
    private String newNameLangueCode;
    // Champs temporaires pour la saisie d'une nouvelle description
    private String newDescriptionValue;
    private String newDescriptionLangueCode;
    // Référentiel public ou privé
    private Boolean referencePublique = true;
    // Langues disponibles (chargées à la demande)
    private List<Langue> availableLanguages;

    /** ID du référentiel en cours d'édition dans le dialog (null = mode création). */
    private Long editingReferenceId;

    // État d'édition pour le référentiel
    private boolean editingReference = false;
    private String editingReferenceCode;
    private String editingReferenceDescription;
    private String editingReferenceLabel;
    private String editingReferenceBibliographie;
    /** Code langue pour laquelle on édite le label (ex: fr, en). */
    private String editingLabelLangueCode;
    /** Code langue pour laquelle on édite la description (ex: fr, en). */
    private String editingDescriptionLangueCode;

    
    public void resetReferenceForm() {
        editingReferenceId = null;
        referenceCode = null;
        periodeId = null;
        referenceBibliographiqueList = new ArrayList<>();
        categorieIds = null;
        referenceNames = new ArrayList<>();
        referenceDescriptions = new ArrayList<>();
        newNameValue = null;
        newNameLangueCode = null;
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        referencePublique = true;
    }

    /**
     * Prépare le dialog pour la création : réinitialise le formulaire et passe en mode création.
     */
    public void prepareCreateReference() {
        resetReferenceForm();
    }

    /**
     * Prépare le dialog pour l'édition à partir de l'ID du référentiel.
     * Utilisé lorsque le passage direct de l'entité depuis ui:repeat pose problème.
     */
    public void prepareEditReferenceInDialogById(Long referenceId) {
        if (referenceId == null) {
            return;
        }
        loadReferenceIntoForm(referenceId);
    }

    /**
     * Prépare le dialog pour l'édition : charge les données du référentiel sélectionné.
     */
    public void prepareEditReferenceInDialog(Entity reference) {
        if (reference == null || reference.getId() == null) {
            return;
        }
        loadReferenceIntoForm(reference.getId());
    }

    /**
     * Charge les données du référentiel dans le formulaire du dialog.
     */
    private void loadReferenceIntoForm(Long referenceId) {
        Entity refLoaded = entityRepository.findById(referenceId)
                .orElseThrow(() -> new IllegalStateException("Référentiel introuvable (id: " + referenceId + ")"));
        editingReferenceId = refLoaded.getId();
        referenceCode = refLoaded.getCode();
        referencePublique = refLoaded.getPublique() != null ? refLoaded.getPublique() : true;

        referenceNames = new ArrayList<>();
        if (refLoaded.getLabels() != null) {
            for (Label l : refLoaded.getLabels()) {
                if (l != null && l.getLangue() != null && StringUtils.hasText(l.getNom())) {
                    referenceNames.add(new NameItem(l.getNom(), l.getLangue().getCode(), l.getLangue()));
                }
            }
        }

        referenceDescriptions = new ArrayList<>();
        if (refLoaded.getDescriptions() != null) {
            for (Description d : refLoaded.getDescriptions()) {
                if (d != null && d.getLangue() != null && StringUtils.hasText(d.getValeur())) {
                    referenceDescriptions.add(new DescriptionItem(d.getValeur(), d.getLangue().getCode(), d.getLangue()));
                }
            }
        }

        referenceBibliographiqueList = new ArrayList<>();
        String bib = refLoaded.getBibliographie();
        if (StringUtils.hasText(bib)) {
            for (String part : bib.split(";")) {
                String t = part != null ? part.trim() : "";
                if (!t.isEmpty()) {
                    referenceBibliographiqueList.add(t);
                }
            }
        }

        newNameValue = null;
        newNameLangueCode = null;
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
    }

    /**
     * Indique si le dialog est en mode édition (true) ou création (false).
     */
    public boolean isEditModeInDialog() {
        return editingReferenceId != null;
    }

    private void loadAvailableLanguages() {
        if (availableLanguages == null) {
            try {
                availableLanguages = langueRepository.findAllByOrderByNomAsc();
            } catch (Exception e) {
                log.error("Erreur lors du chargement des langues", e);
                availableLanguages = new ArrayList<>();
            }
        }
    }

    public void addNameFromInput() {
        if (newNameValue == null || newNameValue.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Le nom est requis."));
            return;
        }
        if (newNameLangueCode == null || newNameLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La langue est requise."));
            return;
        }
        if (isLangueAlreadyUsedInNames(newNameLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Cette langue est déjà utilisée pour un autre nom."));
            return;
        }
        if (referenceNames == null) referenceNames = new ArrayList<>();
        Langue langue = langueRepository.findByCode(newNameLangueCode);
        referenceNames.add(new NameItem(newNameValue.trim(), newNameLangueCode, langue));
        newNameValue = null;
        newNameLangueCode = null;
    }

    public void removeName(NameItem item) {
        if (referenceNames != null) referenceNames.remove(item);
    }

    public boolean isLangueAlreadyUsedInNames(String code, NameItem exclude) {
        if (referenceNames == null || code == null) return false;
        return referenceNames.stream()
            .filter(i -> i != exclude && i.getLangueCode() != null)
            .anyMatch(i -> i.getLangueCode().equals(code));
    }

    public List<Langue> getAvailableLanguagesForNewName() {
        loadAvailableLanguages();
        if (availableLanguages == null) return new ArrayList<>();
        return availableLanguages.stream()
            .filter(l -> !isLangueAlreadyUsedInNames(l.getCode(), null))
            .collect(java.util.stream.Collectors.toList());
    }

    public void addDescriptionFromInput() {
        if (newDescriptionValue == null || newDescriptionValue.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La description est requise."));
            return;
        }
        if (newDescriptionLangueCode == null || newDescriptionLangueCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La langue est requise."));
            return;
        }
        if (isLangueAlreadyUsedInDescriptions(newDescriptionLangueCode, null)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Cette langue est déjà utilisée pour une autre description."));
            return;
        }
        if (referenceDescriptions == null) referenceDescriptions = new ArrayList<>();
        Langue langue = langueRepository.findByCode(newDescriptionLangueCode);
        referenceDescriptions.add(new DescriptionItem(newDescriptionValue.trim(), newDescriptionLangueCode, langue));
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
    }

    public void removeDescription(DescriptionItem item) {
        if (referenceDescriptions != null) referenceDescriptions.remove(item);
    }

    public boolean isLangueAlreadyUsedInDescriptions(String code, DescriptionItem exclude) {
        if (referenceDescriptions == null || code == null) return false;
        return referenceDescriptions.stream()
            .filter(i -> i != exclude && i.getLangueCode() != null)
            .anyMatch(i -> i.getLangueCode().equals(code));
    }

    public List<Langue> getAvailableLanguagesForNewDescription() {
        loadAvailableLanguages();
        if (availableLanguages == null) return new ArrayList<>();
        return availableLanguages.stream()
            .filter(l -> !isLangueAlreadyUsedInDescriptions(l.getCode(), null))
            .collect(java.util.stream.Collectors.toList());
    }
    
    public void createReference() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        boolean validCode;
        if (editingReferenceId != null) {
            validCode = EntityValidator.validateCodeForEdit(referenceCode, editingReferenceId, entityRepository, reference_FORM);
        } else {
            validCode = EntityValidator.validateCode(referenceCode, entityRepository, reference_FORM);
        }
        if (!validCode) {
            return;
        }

        if (referenceNames == null || referenceNames.isEmpty()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Au moins un nom est requis."));
            return;
        }

        for (NameItem item : referenceNames) {
            if (item.getNom() == null || item.getNom().trim().isEmpty()) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Tous les noms doivent avoir une valeur."));
                return;
            }
        }

        String codeTrimmed = referenceCode.trim();

        if (editingReferenceId != null) {
            updateReferenceFromDialog(facesContext, codeTrimmed);
            return;
        }

        try {
            EntityType referenceType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_REFERENCE)
                .orElseThrow(() -> new IllegalStateException(
                    "Le type d'entité '" + EntityConstants.ENTITY_TYPE_REFERENCE + "' n'existe pas dans la base de données."));

            Entity newReference = createNewReference(codeTrimmed, referenceType);
            entityRepository.save(newReference);
            
            // Rattacher le référentiel à la collection courante si une collection est sélectionnée
            if (applicationBean != null && applicationBean.getSelectedCollection() != null) {
                attachReferenceToCollection(newReference, applicationBean.getSelectedCollection());
            }
            
            refreshReferencesList();
            
            // Recharger les référentiels de la collection
            if (applicationBean != null && applicationBean.getSelectedCollection() != null) {
                applicationBean.refreshCollectionReferencesList();
            }

            updateTree(newReference);
            
            String labelPrincipal = referenceNames.get(0).getNom();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    "Le référentiel '" + labelPrincipal + "' a été créé avec succès."));

            resetReferenceForm();
            
            // Fermer le dialog
            PrimeFaces.current().executeScript("PF('referenceDialog').hide();");
            
            // Mettre à jour les composants
            PrimeFaces.current().ajax().update(
                ViewConstants.COMPONENT_GROWL + ", " + reference_FORM + ", " 
                + ViewConstants.COMPONENT_TREE_WIDGET + ", :collectionReferencesContainer");
            
        } catch (IllegalStateException e) {
            log.error("Erreur lors de la création du référentiel", e);
            addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création du référentiel", e);
            addErrorMessage("Une erreur est survenue lors de la création du référentiel : " + e.getMessage());
        }
    }

    /**
     * Annule l'édition du référentiel
     */
    public void cancelEditingReference() {
        editingReference = false;
        editingReferenceCode = null;
        editingReferenceDescription = null;
        editingReferenceLabel = null;
        editingReferenceBibliographie = null;
        editingLabelLangueCode = null;
        editingDescriptionLangueCode = null;
    }

    /**
     * Sauvegarde les modifications du référentiel.
     * Enregistre : code, label (selon langue choisie), description (selon langue choisie),
     * référence bibliographique ; ajoute l'utilisateur courant aux auteurs s'il n'y figure pas.
     */
    @Transactional
    public void saveReference(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() == null) {
            return;
        }

        try {
            Entity referenceToUpdate = entityRepository.findById(applicationBean.getSelectedReference().getId()).get();

            // Mettre à jour le code uniquement si modifié
            String newCode = editingReferenceCode != null ? editingReferenceCode.trim() : null;
            if (!Objects.equals(newCode, referenceToUpdate.getCode()) && newCode != null && !newCode.isEmpty()) {
                referenceToUpdate.setCode(newCode);
            }

            // Langue pour le label (celle choisie dans le menu)
            String labelLangueCode = editingLabelLangueCode != null ? editingLabelLangueCode : "fr";
            Langue labelLangue = langueRepository.findByCode(labelLangueCode);
            // Mettre à jour le label (selon la langue choisie) uniquement si modifié
            String newLabel = editingReferenceLabel != null ? editingReferenceLabel.trim() : "";
            String currentLabelValue = EntityUtils.getLabelValueForLanguage(referenceToUpdate, labelLangueCode);
            if (!Objects.equals(newLabel, currentLabelValue) && labelLangue != null) {
                Optional<Label> labelOpt = referenceToUpdate.getLabels() != null
                        ? referenceToUpdate.getLabels().stream()
                        .filter(l -> l.getLangue() != null && labelLangueCode.equalsIgnoreCase(l.getLangue().getCode()))
                        .findFirst()
                        : Optional.empty();
                if (labelOpt.isPresent()) {
                    labelOpt.get().setNom(newLabel);
                } else {
                    Label newLabelEntity = new Label();
                    newLabelEntity.setNom(newLabel);
                    newLabelEntity.setEntity(referenceToUpdate);
                    newLabelEntity.setLangue(labelLangue);
                    if (referenceToUpdate.getLabels() == null) {
                        referenceToUpdate.setLabels(new ArrayList<>());
                    }
                    referenceToUpdate.getLabels().add(newLabelEntity);
                }
            }

            // Langue pour la description (celle choisie dans le menu)
            String descLangueCode = editingDescriptionLangueCode != null ? editingDescriptionLangueCode : "fr";
            Langue descLangue = langueRepository != null ? langueRepository.findByCode(descLangueCode) : null;
            // Mettre à jour la description (selon la langue choisie) uniquement si modifiée
            String newDesc = editingReferenceDescription != null ? editingReferenceDescription.trim() : "";
            String currentDescValue = EntityUtils.getDescriptionValueForLanguage(referenceToUpdate, descLangueCode);
            if (!Objects.equals(newDesc, currentDescValue) && descLangue != null) {
                Optional<Description> descOpt = referenceToUpdate.getDescriptions() != null
                        ? referenceToUpdate.getDescriptions().stream()
                        .filter(d -> d.getLangue() != null && descLangueCode.equalsIgnoreCase(d.getLangue().getCode()))
                        .findFirst()
                        : Optional.empty();
                if (descOpt.isPresent()) {
                    descOpt.get().setValeur(newDesc);
                } else {
                    Description newDescription = new Description();
                    newDescription.setValeur(newDesc);
                    newDescription.setEntity(referenceToUpdate);
                    newDescription.setLangue(descLangue);
                    if (referenceToUpdate.getDescriptions() == null) {
                        referenceToUpdate.setDescriptions(new ArrayList<>());
                    }
                    referenceToUpdate.getDescriptions().add(newDescription);
                }
            }

            // Mettre à jour la référence bibliographique (liste des références) uniquement si modifiée
            String newBib = editingReferenceBibliographie != null ? editingReferenceBibliographie.trim() : null;
            String currentBib = referenceToUpdate.getBibliographie();
            if (!Objects.equals(newBib, currentBib)) {
                referenceToUpdate.setRereferenceBibliographique(newBib);
            }

            // Ajouter l'utilisateur courant aux auteurs s'il n'y figure pas
            Utilisateur currentUser = loginBean != null ? loginBean.getCurrentUser() : null;
            if (currentUser != null && currentUser.getId() != null && utilisateurRepository != null) {
                Utilisateur managedUser = utilisateurRepository.findById(currentUser.getId()).orElse(null);
                if (managedUser != null) {
                    List<Utilisateur> auteurs = referenceToUpdate.getAuteurs();
                    if (auteurs == null) {
                        referenceToUpdate.setAuteurs(new ArrayList<>());
                        auteurs = referenceToUpdate.getAuteurs();
                    }
                    boolean alreadyAuthor = auteurs.stream()
                            .anyMatch(u -> u.getId() != null && u.getId().equals(managedUser.getId()));
                    if (!alreadyAuthor) {
                        auteurs.add(managedUser);
                    }
                }
            }

            applicationBean.setSelectedEntity(entityRepository.save(referenceToUpdate));

            applicationBean.getBeadCrumbElements().set(applicationBean.getBeadCrumbElements().size() - 1, applicationBean.getSelectedReference());

            editingReference = false;
            editingReferenceCode = null;
            editingReferenceDescription = null;
            editingReferenceLabel = null;
            editingReferenceBibliographie = null;
            editingLabelLangueCode = null;
            editingDescriptionLangueCode = null;

            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "Les modifications ont été enregistrées avec succès."));
            }

            log.info("Référentiel mis à jour avec succès: {}", applicationBean.getSelectedReference().getCode());
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du référentiel", e);
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Une erreur est survenue lors de la sauvegarde : " + e.getMessage()));
            }
        }
    }

    /**
     * Supprime le référentiel par son ID et toutes ses entités rattachées (directement ou indirectement).
     * Utilisé depuis la liste des référentiels d'une collection.
     */
    @Transactional
    public void deleteReferenceById(ApplicationBean applicationBean, Long referenceId) {
        if (referenceId == null) {
            addErrorMessage("Aucun référentiel sélectionné.");
            return;
        }
        Entity reference = entityRepository.findById(referenceId).orElse(null);
        if (reference == null) {
            addErrorMessage("Référentiel introuvable.");
            return;
        }
        doDeleteReference(applicationBean, reference);
    }

    /**
     * Supprime le référentiel sélectionné et toutes ses entités rattachées
     */
    @Transactional
    public void deleteReference(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() == null || applicationBean.getSelectedReference().getId() == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Aucun référentiel sélectionné."));
            }
            return;
        }
        doDeleteReference(applicationBean, applicationBean.getSelectedReference());
    }

    private void doDeleteReference(ApplicationBean applicationBean, Entity reference) {
        try {
            String referenceCode = reference.getCode();
            String referenceName = reference.getNom();
            Long referenceId = reference.getId();

            // Supprimer récursivement le référentiel et toutes ses entités enfants
            applicationBean.deleteEntityRecursively(reference);

            // Réinitialiser la sélection
            applicationBean.setSelectedEntity(null);
            applicationBean.setChilds(new ArrayList<>());

            // Recharger les référentiels de la collection
            if (applicationBean.getSelectedCollection() != null) {
                applicationBean.refreshCollectionReferencesList();
            }

            // Mettre à jour l'arbre
            treeBean.initializeTreeWithCollection();

            // Afficher un message de succès
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "Le référentiel '" + referenceName + "' et toutes ses entités rattachées ont été supprimés avec succès."));
            }

            // Afficher le panel de la collection
            if (applicationBean.getSelectedReference() != null) {
                applicationBean.getPanelState().showCollectionDetail();
            } else {
                applicationBean.getPanelState().showCollections();
            }

            log.info("Référentiel supprimé avec succès: {} (ID: {})", referenceCode, referenceId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du référentiel", e);
            addErrorMessage("Une erreur est survenue lors de la suppression : " + e.getMessage());
        }
    }

    /**
     * Active le mode édition pour le référentiel sélectionné.
     * Charge le code, la description, le label et la référence bibliographique.
     * Les langues d'édition label/description sont initialisées avec la langue sélectionnée (SearchBean).
     */
    public void startEditingReference(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() == null) {
            return;
        }
        String codeLang = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        editingReference = true;
        editingReferenceCode = applicationBean.getSelectedReference().getCode() != null ? applicationBean.getSelectedReference().getCode() : "";
        editingLabelLangueCode = codeLang;
        editingDescriptionLangueCode = codeLang;
        // Description selon la langue choisie
        editingReferenceDescription = EntityUtils.getDescriptionValueForLanguage(applicationBean.getSelectedReference(), codeLang);
        // Label selon la langue choisie
        editingReferenceLabel = EntityUtils.getLabelValueForLanguage(applicationBean.getSelectedReference(), codeLang);
        editingReferenceBibliographie = applicationBean.getSelectedReference().getRereferenceBibliographique() != null
                ? applicationBean.getSelectedReference().getBibliographie()
                : "";
    }

    /**
     * Appelé lorsque l'utilisateur change la langue du label dans le menu déroulant.
     * Recharge la valeur du label pour la nouvelle langue depuis l'entité.
     */
    public void onLabelLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() != null && editingLabelLangueCode != null) {
            editingReferenceLabel = EntityUtils.getLabelValueForLanguage(applicationBean.getSelectedReference(), editingLabelLangueCode);
        }
    }

    /**
     * Appelé lorsque l'utilisateur change la langue de la description dans le menu déroulant.
     * Recharge la valeur de la description pour la nouvelle langue depuis l'entité.
     */
    public void onDescriptionLanguageChange(ApplicationBean applicationBean) {
        if (applicationBean.getSelectedReference() != null && editingDescriptionLangueCode != null) {
            editingReferenceDescription = EntityUtils.getDescriptionValueForLanguage(applicationBean.getSelectedReference(), editingDescriptionLangueCode);
        }
    }

    /**
     * Met à jour le référentiel à partir des données du dialog (mode édition).
     */
    @Transactional
    protected void updateReferenceFromDialog(FacesContext facesContext, String codeTrimmed) {
        try {
            Entity referenceToUpdate = entityRepository.findById(editingReferenceId)
                    .orElseThrow(() -> new IllegalStateException("Référentiel introuvable."));

            referenceToUpdate.setCode(codeTrimmed);
            referenceToUpdate.setPublique(referencePublique != null ? referencePublique : true);

            String bibJoined = null;
            if (referenceBibliographiqueList != null && !referenceBibliographiqueList.isEmpty()) {
                bibJoined = referenceBibliographiqueList.stream()
                        .filter(s -> s != null && !s.trim().isEmpty())
                        .map(String::trim)
                        .collect(Collectors.joining("; "));
                if (bibJoined.isEmpty()) bibJoined = null;
            }
            referenceToUpdate.setRereferenceBibliographique(bibJoined);

            // Mise à jour des labels
            if (referenceToUpdate.getLabels() == null) {
                referenceToUpdate.setLabels(new ArrayList<>());
            }
            referenceToUpdate.getLabels().clear();
            if (referenceNames != null) {
                for (NameItem item : referenceNames) {
                    if (item.getLangue() != null && StringUtils.hasText(item.getNom())) {
                        Label lbl = new Label();
                        lbl.setNom(item.getNom().trim());
                        lbl.setLangue(item.getLangue());
                        lbl.setEntity(referenceToUpdate);
                        referenceToUpdate.getLabels().add(lbl);
                    }
                }
            }

            // Mise à jour des descriptions
            if (referenceToUpdate.getDescriptions() == null) {
                referenceToUpdate.setDescriptions(new ArrayList<>());
            }
            referenceToUpdate.getDescriptions().clear();
            if (referenceDescriptions != null) {
                for (DescriptionItem item : referenceDescriptions) {
                    if (item.getLangue() != null && StringUtils.hasText(item.getValeur())) {
                        Description desc = new Description();
                        desc.setValeur(item.getValeur().trim());
                        desc.setLangue(item.getLangue());
                        desc.setEntity(referenceToUpdate);
                        referenceToUpdate.getDescriptions().add(desc);
                    }
                }
            }

            entityRepository.save(referenceToUpdate);

            if (editingReferenceId.equals(applicationBean.getSelectedEntity().getId())) {
                applicationBean.setSelectedEntity(referenceToUpdate);
            }

            applicationBean.refreshCollectionReferencesList();

            refreshReferencesList();
            if (treeBean != null) {
                treeBean.initializeTreeWithCollection();
            }

            String labelPrincipal = (referenceNames != null && !referenceNames.isEmpty())
                    ? referenceNames.get(0).getNom() : codeTrimmed;
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                    "Le référentiel '" + labelPrincipal + "' a été modifié avec succès."));

            resetReferenceForm();
            PrimeFaces.current().executeScript("PF('referenceDialog').hide();");
            PrimeFaces.current().ajax().update(
                    ViewConstants.COMPONENT_GROWL + ", " + reference_FORM + ", "
                            + ViewConstants.COMPONENT_TREE_WIDGET + ", :collectionReferencesContainer");
        } catch (IllegalStateException e) {
            log.error("Erreur lors de la modification du référentiel", e);
            addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la modification du référentiel", e);
            addErrorMessage("Une erreur est survenue lors de la modification : " + e.getMessage());
        }
    }

    /**
     * Crée une nouvelle entité référentiel à partir des listes noms/descriptions
     */
    private Entity createNewReference(String code, EntityType type) {
        Entity newReference = new Entity();
        newReference.setCode(code);
        String bibJoined = null;
        if (referenceBibliographiqueList != null && !referenceBibliographiqueList.isEmpty()) {
            bibJoined = referenceBibliographiqueList.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.joining("; "));
            if (bibJoined.isEmpty()) bibJoined = null;
        }
        newReference.setBibliographie(bibJoined);
        newReference.setEntityType(type);
        newReference.setPublique(referencePublique != null ? referencePublique : true);
        newReference.setCreateDate(LocalDateTime.now());

        if (referenceNames != null && !referenceNames.isEmpty()) {
            List<Label> labels = new ArrayList<>();
            for (NameItem item : referenceNames) {
                if (item.getLangue() != null && StringUtils.hasText(item.getNom())) {
                    Label lbl = new Label();
                    lbl.setNom(item.getNom().trim());
                    lbl.setLangue(item.getLangue());
                    lbl.setEntity(newReference);
                    labels.add(lbl);
                }
            }
            newReference.setLabels(labels);
        }

        if (referenceDescriptions != null && !referenceDescriptions.isEmpty()) {
            List<Description> descriptions = new ArrayList<>();
            for (DescriptionItem item : referenceDescriptions) {
                if (item.getLangue() != null && StringUtils.hasText(item.getValeur())) {
                    Description desc = new Description();
                    desc.setValeur(item.getValeur().trim());
                    desc.setLangue(item.getLangue());
                    desc.setEntity(newReference);
                    descriptions.add(desc);
                }
            }
            newReference.setDescriptions(descriptions);
        }

        Utilisateur currentUser = loginBean.getCurrentUser();
        if (currentUser != null) {
            newReference.setCreateBy(currentUser.getEmail());
            List<Utilisateur> auteurs = new ArrayList<>();
            auteurs.add(currentUser);
            newReference.setAuteurs(auteurs);
        }

        return newReference;
    }

    /**
     * Recharge les listes de référentiels dans les beans concernés
     */
    private void refreshReferencesList() {
        if (applicationBean != null) {
            applicationBean.loadReferences();
        }
        if (searchBean != null) {
            searchBean.loadReferences();
        }
    }

    /**
     * Rattache un référentiel à une collection
     */
    private void attachReferenceToCollection(Entity reference, Entity collection) {
        try {
            // Vérifier si la relation existe déjà
            if (!entityRelationRepository.existsByParentAndChild(collection.getId(), reference.getId())) {
                fr.cnrs.opentypo.domain.entity.EntityRelation relation = 
                    new fr.cnrs.opentypo.domain.entity.EntityRelation();
                relation.setParent(collection);
                relation.setChild(reference);
                entityRelationRepository.save(relation);
            }
        } catch (Exception e) {
            log.error("Erreur lors du rattachement du référentiel à la collection", e);
        }
    }

    /**
     * Met à jour l'arbre avec le nouveau référentiel
     */
    private void updateTree(Entity reference) {
        if (treeBean != null) {
            treeBean.addReferenceToTree(reference);
        }
    }

    /**
     * Ajoute un message d'erreur
     */
    private void addErrorMessage(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", message));
        PrimeFaces.current().ajax().update(ViewConstants.COMPONENT_GROWL + ", " + reference_FORM);
    }
}
