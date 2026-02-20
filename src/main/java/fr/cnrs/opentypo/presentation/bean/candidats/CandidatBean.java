package fr.cnrs.opentypo.presentation.bean.candidats;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.application.service.CandidatListService;
import fr.cnrs.opentypo.application.service.CandidatValidationService;
import fr.cnrs.opentypo.application.service.CollectionService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.application.service.IiifImageService;
import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.OpenThesoDialogBean;
import fr.cnrs.opentypo.presentation.bean.SearchBean;
import fr.cnrs.opentypo.presentation.bean.UserBean;
import fr.cnrs.opentypo.presentation.bean.candidats.converter.CandidatConverter;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CandidatSauvegardeRequest;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CandidatSauvegardeResult;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryDescriptionItem;
import fr.cnrs.opentypo.presentation.bean.candidats.model.CategoryLabelItem;
import fr.cnrs.opentypo.presentation.bean.candidats.model.Step3FormData;
import fr.cnrs.opentypo.presentation.bean.candidats.model.VisualisationPrepareResult;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatConfirmDeleteService;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatEntityService;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatWizardStepService;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatFormDataLoader;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatFormLoadService;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatFormSaveService;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatImageService;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatLabelDescriptionService;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatOpenThesoService;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatReferenceTreeService;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatSauvegardeService;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatValidationActionService;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatValidationActionService.ActionResult;
import fr.cnrs.opentypo.presentation.bean.candidats.service.CandidatVisualisationService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.model.TreeNode;
import org.primefaces.model.file.UploadedFile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Named("candidatBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class CandidatBean implements Serializable {

    @Inject
    private UserBean userBean;

    @Inject
    private LoginBean loginBean;

    @Inject
    private EntityTypeRepository entityTypeRepository;

    @Inject
    private LangueRepository langueRepository;

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private SearchBean searchBean;

    @Inject
    private IiifImageService iiifImageService;

    @Inject
    private OpenThesoDialogBean openThesoDialogBean;

    @Inject
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    @Inject
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private CollectionService collectionService;

    @Inject
    private CandidatListService candidatListService;

    @Inject
    private CandidatValidationService candidatValidationService;

    @Inject
    private CandidatEntityService candidatEntityService;

    @Inject
    private CandidatReferenceTreeService candidatReferenceTreeService;

    @Inject
    private CandidatFormDataLoader candidatFormDataLoader;

    @Inject
    private CandidatFormLoadService candidatFormLoadService;

    @Inject
    private CandidatConverter candidatConverter;

    @Inject
    private CandidatOpenThesoService candidatOpenThesoService;

    @Inject
    private CandidatFormSaveService candidatFormSaveService;

    @Inject
    private CandidatSauvegardeService candidatSauvegardeService;

    @Inject
    private CandidatLabelDescriptionService candidatLabelDescriptionService;

    @Inject
    private CandidatValidationActionService candidatValidationActionService;

    @Inject
    private CandidatVisualisationService candidatVisualisationService;

    @Inject
    private CandidatImageService candidatImageService;

    @Inject
    private CandidatConfirmDeleteService candidatConfirmDeleteService;

    @Inject
    private CandidatWizardStepService candidatWizardStepService;

    private List<Candidat> candidats = new ArrayList<>();
    private Candidat candidatSelectionne;
    private Candidat nouveauCandidat;
    private Candidat candidatAValider; // Candidat sélectionné pour validation
    private Candidat candidatARefuser; // Candidat sélectionné pour refus
    private Candidat candidatARemettreEnBrouillon; // Candidat sélectionné pour remise en brouillon
    private Candidat candidatASupprimer; // Candidat sélectionné pour suppression
    private int activeTabIndex = 0; // 0 = en cours, 1 = validés, 2 = refusés
    private boolean candidatsLoaded = false; // Flag pour savoir si les candidats ont été chargés
    
    // Champs pour l'étape 1 du formulaire
    private Long selectedEntityTypeId;
    private String entityCode;
    private String entityLabel;
    private String candidatProduction;
    private List<ReferenceOpentheso> airesCirculation = new ArrayList<>(); // Liste des aires de circulation
    private String decors; // Décors (sauvegardé seulement au clic sur Terminer)
    private List<String> marquesEstampilles = new ArrayList<>(); // Marques/estampilles (sauvegardé immédiatement)
    private ReferenceOpentheso fonctionUsage; // Fonction/usage (sauvegardé immédiatement via OpenTheso)
    
    // Caractéristiques physiques
    private ReferenceOpentheso metrologie; // Métrologie (sauvegardé immédiatement via OpenTheso)
    private ReferenceOpentheso fabricationFaconnage; // Fabrication/façonnage (sauvegardé immédiatement via OpenTheso)
    private String descriptionPate; // Description pâte (sauvegardé immédiatement)
    private ReferenceOpentheso couleurPate; // Couleur de pâte (sauvegardé immédiatement via OpenTheso)
    private ReferenceOpentheso naturePate; // Nature de pâte (sauvegardé immédiatement via OpenTheso)
    private ReferenceOpentheso inclusions; // Inclusions (sauvegardé immédiatement via OpenTheso)
    private ReferenceOpentheso cuissonPostCuisson; // Cuisson/post-cuisson (sauvegardé immédiatement via OpenTheso)
    // Description monnaie (collection MONNAIE)
    private String droit;
    private String legendeDroit;
    private String coinsMonetairesDroit;
    private String revers;
    private String legendeRevers;
    private String coinsMonetairesRevers;
    // Caractéristiques physiques monnaie (collection MONNAIE)
    private ReferenceOpentheso materiau;
    private ReferenceOpentheso denomination;
    private String metrologieMonnaie;
    private ReferenceOpentheso valeur;
    private ReferenceOpentheso technique;
    private ReferenceOpentheso fabrication;

    private String selectedLangueCode;
    private Long selectedCollectionId;
    private Entity selectedParentEntity;
    private Long selectedDirectEntityId; // Entité directement rattachée à la collection
    private Entity currentEntity; // Entité créée à l'étape 1, utilisée dans les étapes suivantes
    
    // Liste des données pour les sélecteurs
    private List<EntityType> availableEntityTypes;
    private List<Langue> availableLanguages;
    private List<Entity> availableCollections;
    private List<Entity> availableReferences;
    private List<Entity> availableDirectEntities; // Entités directement rattachées à la collection
    
    // Arbre pour les référentiels et leurs enfants
    private TreeNode referenceTreeRoot;
    private TreeNode selectedTreeNode;
    
    // Index du wizard (0 = étape 1, 1 = étape 2, 2 = étape 3)
    private int currentStep = 0;

    // Champs pour l'étape 2 selon le type
    private String typeDescription;
    private String serieDescription;
    private String groupDescription;
    private String categoryDescription; // Ancien champ, conservé pour compatibilité
    private String imagePrincipaleUrl;
    private UploadedFile uploadedImageFile;
    private List<CategoryLabelItem> candidatLabels = new ArrayList<>();
    private String newLabelValue;
    private String newLabelLangueCode;
    private List<CategoryDescriptionItem> descriptions = new ArrayList<>();
    private String newDescriptionValue;
    private String newDescriptionLangueCode;
    private String candidatCommentaireDatation;
    private String candidatBibliographie;
    private List<String> referencesBibliographiques = new ArrayList<>();
    private List<String> ateliers = new ArrayList<>();
    private List<String> attestations = new ArrayList<>();
    private List<String> sitesArcheologiques = new ArrayList<>();
    private List<String> references = new ArrayList<>();
    private String referentiel; // Référentiel (enregistré dans entity.reference)
    private String typologieScientifique; // Typologie scientifique (enregistré dans entity.typologieScientifique)
    private String identifiantPerenne; // Identifiant pérenne (enregistré dans entity.identifiantPerenne)
    private String ancienneVersion; // Ancienne version (enregistré dans entity.ancienneVersion)
    private String collectionDescription;
    private Boolean collectionPublique = true;
    private String corpusExterne;
    private String periode;
    private Integer tpq;
    private Integer taq;

    // Propriétés pour les auteurs
    private List<Utilisateur> selectedAuteurs = new ArrayList<>();
    private List<Utilisateur> availableAuteurs = new ArrayList<>();


    @PostConstruct
    public void init() {
        chargerCandidats();
        availableEntityTypes = candidatFormLoadService.loadEntityTypes();
        availableLanguages = candidatFormLoadService.loadLanguages();
        availableCollections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION);
        availableDirectEntities = new ArrayList<>();
    }

    public String nextStep() {
        log.info("nextStep() appelée - currentStep actuel: {}", currentStep);
        CandidatWizardStepService.NextStepResult r = candidatWizardStepService.nextStep(currentStep,
                selectedEntityTypeId, entityCode, entityLabel, selectedLangueCode, selectedCollectionId,
                selectedTreeNode, currentEntity, loginBean.getCurrentUser());
        if (r.errorMessage() != null) {
            addErrorMessage(r.errorMessage());
            PrimeFaces.current().ajax().update(":growl");
            return null;
        }
        if (r.success()) {
            if (r.createdEntity() != null) currentEntity = r.createdEntity();
            if (r.parentEntity() != null) selectedParentEntity = r.parentEntity();
            if (r.shouldLoadStep3Data()) loadExistingStep3Data();
            currentStep = r.newStep();
            log.info("Passage à l'étape {} - currentStep = {}", currentStep + 1, currentStep);
        }
        return null;
    }
    
    /**
     * Retourne à l'étape précédente du wizard
     */
    public String previousStep() {
        log.info("previousStep() appelée - currentStep actuel: {}", currentStep);
        if (currentStep > 0) {
            currentStep--;
            log.info("Retour à l'étape précédente - currentStep = {}", currentStep);
        }
        
        return null; // Reste sur la même page avec mise à jour AJAX
    }
    
    /**
     * Retourne true si on est sur l'étape 1
     */
    public boolean isStep1() {
        return currentStep == 0;
    }
    
    /**
     * Retourne true si on est sur l'étape 2
     */
    public boolean isStep2() {
        return currentStep == 1;
    }
    
    /**
     * Retourne true si on est sur l'étape 3
     */
    public boolean isStep3() {
        return currentStep == 2;
    }
    
    /**
     * Méthode appelée lors de l'accès à la page candidats
     * Recharge les données pour s'assurer qu'elles sont à jour
     */
    public void loadCandidatsPage() {
        log.debug("Chargement de la page candidats, rechargement des données");
        candidatsLoaded = false; // Invalider le cache
        chargerCandidats();
        
        // Vérifier les paramètres de requête pour afficher des messages
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String success = facesContext.getExternalContext().getRequestParameterMap().get("success");
            String error = facesContext.getExternalContext().getRequestParameterMap().get("error");
            
            if ("true".equals(success)) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "Le candidat a été créé avec succès. Vous pouvez le voir dans la liste des candidats en cours."));
                PrimeFaces.current().ajax().update(":growl");
            } else if ("true".equals(error)) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Une erreur est survenue lors de la sauvegarde."));
                PrimeFaces.current().ajax().update(":growl");
            }
        }
    }
    
    
    public void loadAvailableEntityTypes() { availableEntityTypes = candidatFormLoadService.loadEntityTypes(); }
    public void loadAvailableLanguages() { availableLanguages = candidatFormLoadService.loadLanguages(); }
    
    /**
     * Charge les référentiels d'une collection sélectionnée depuis la base de données
     * Construit un arbre avec les référentiels et leurs enfants
     * Filtre selon l'état d'authentification de l'utilisateur
     */
    public void loadReferencesForCollection() {
        log.debug("loadReferencesForCollection appelée - selectedCollectionId: {}", selectedCollectionId);
        referenceTreeRoot = null;
        selectedTreeNode = null;
        selectedDirectEntityId = null;

        boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
        CandidatReferenceTreeService.ReferenceTreeResult result =
                candidatReferenceTreeService.loadReferencesForCollection(selectedCollectionId, isAuthenticated);

        referenceTreeRoot = result.treeRoot();
        availableDirectEntities = result.directEntities();

        PrimeFaces.current().ajax().update(":createCandidatForm:referenceTreeContainer :createCandidatForm:directEntitySelect");
    }
    
    /**
     * Charge les entités directement rattachées à la collection sélectionnée
     */
    public void loadDirectEntitiesForCollection() {
        log.debug("loadDirectEntitiesForCollection appelée - selectedCollectionId: {}", selectedCollectionId);
        selectedDirectEntityId = null;
        boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
        availableDirectEntities = candidatReferenceTreeService.loadDirectEntitiesForCollection(
                selectedCollectionId, isAuthenticated);
    }
    
    /**
     * Récupère le label d'une entité pour l'affichage dans le selectOneMenu
     */
    public String getDirectEntityLabel(Entity entity) {
        return candidatReferenceTreeService.getDirectEntityLabel(entity, selectedLangueCode);
    }
    
    /**
     * Méthode appelée lors du changement de l'entité directe sélectionnée
     * Construit l'arbre à partir de la référence sélectionnée
     */
    public void onDirectEntityChange() {
        log.debug("onDirectEntityChange appelée - selectedDirectEntityId: {}", selectedDirectEntityId);
        referenceTreeRoot = null;
        selectedTreeNode = null;

        if (selectedDirectEntityId != null && !selectedDirectEntityId.toString().trim().isEmpty()) {
            boolean isAuthenticated = loginBean != null && loginBean.isAuthenticated();
            referenceTreeRoot = candidatReferenceTreeService.buildTreeFromDirectEntity(
                    selectedDirectEntityId, isAuthenticated);
        }

        PrimeFaces.current().ajax().update(":createCandidatForm:referenceTreeContainer");
    }

    /**
     * Retourne le nom du type d'entité pour l'affichage
     */
    public String getEntityTypeName(EntityType entityType) {
        return candidatReferenceTreeService.getEntityTypeName(entityType);
    }

    /**
     * Charge les candidats depuis la base de données
     * Cette méthode est appelée lors de l'initialisation et peut être appelée manuellement pour rafraîchir les données
     */
    public void chargerCandidats() {
        try {
            // Charger les entités avec statut PROPOSITION, ACCEPTED, REFUSED
            List<Entity> entitiesProposition = entityRepository.findByStatut(EntityStatusEnum.PROPOSITION.name());
            List<Entity> entitiesAccepted = entityRepository.findByStatut(EntityStatusEnum.ACCEPTED.name());
            List<Entity> entitiesRefused = entityRepository.findByStatut(EntityStatusEnum.REFUSED.name());
            
            // Convertir en Candidat et stocker dans la liste
            candidats.clear();
            candidats.addAll(entitiesProposition.stream()
                .map(candidatConverter::convertEntityToCandidat)
                .collect(Collectors.toList()));
            candidats.addAll(entitiesAccepted.stream()
                .map(candidatConverter::convertEntityToCandidat)
                .collect(Collectors.toList()));
            candidats.addAll(entitiesRefused.stream()
                .map(candidatConverter::convertEntityToCandidat)
                .collect(Collectors.toList()));
            
            candidatsLoaded = true;
            log.info("Chargement des candidats terminé: {} PROPOSITION, {} ACCEPTED, {} REFUSED", 
                entitiesProposition.size(), entitiesAccepted.size(), entitiesRefused.size());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des candidats depuis la base de données", e);
            candidats.clear();
            candidatsLoaded = false;
        }
    }

    /**
     * Retourne la liste des candidats en cours (statut PROPOSITION)
     */
    public List<Candidat> getCandidatsEnCours() {
        // Recharger les données si nécessaire (lazy loading)
        chargerCandidatsIfNeeded();
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.EN_COURS)
            .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des candidats validés (statut ACCEPTED)
     */
    public List<Candidat> getCandidatsValides() {
        // Recharger les données si nécessaire (lazy loading)
        chargerCandidatsIfNeeded();
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.VALIDE)
            .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des candidats refusés (statut REFUSED)
     */
    public List<Candidat> getCandidatsRefuses() {
        // Recharger les données si nécessaire (lazy loading)
        chargerCandidatsIfNeeded();
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.REFUSE)
            .collect(Collectors.toList());
    }

    /**
     * Indique si l'utilisateur connecté peut valider ou refuser un brouillon.
     * Groupe autorisé : Administrateur technique uniquement.
     */
    public boolean canValidateOrRefuseBrouillon() {
        Utilisateur user = loginBean != null ? loginBean.getCurrentUser() : null;
        if (user == null || user.getGroupe() == null) return false;
        String groupeNom = user.getGroupe().getNom();
        return GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel().equalsIgnoreCase(groupeNom);
    }

    /**
     * Indique si l'utilisateur connecté peut modifier un brouillon (statut PROPOSITION).
     * Groupe autorisé : Administrateur technique uniquement.
     */
    public boolean canEditBrouillon(Candidat candidat) {
        if (candidat == null || candidat.getStatut() != Candidat.Statut.EN_COURS) return false;
        Utilisateur user = loginBean != null ? loginBean.getCurrentUser() : null;
        if (user == null || user.getGroupe() == null) return false;
        String groupeNom = user.getGroupe().getNom();
        return GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel().equalsIgnoreCase(groupeNom);
    }

    /**
     * Indique si le brouillon courant (currentEntity) peut être modifié par l'utilisateur connecté.
     */
    public boolean canEditCurrentBrouillon() {
        if (currentEntity == null || currentEntity.getStatut() == null) return false;
        if (!EntityStatusEnum.PROPOSITION.name().equals(currentEntity.getStatut())) return false;
        Utilisateur user = loginBean != null ? loginBean.getCurrentUser() : null;
        if (user == null || user.getGroupe() == null) return false;
        String groupeNom = user.getGroupe().getNom();
        return GroupEnum.ADMINISTRATEUR_TECHNIQUE.getLabel().equalsIgnoreCase(groupeNom);
    }

    /**
     * Indique si le brouillon courant est en lecture seule (déjà validé ou refusé).
     * Dans ce cas, tous les utilisateurs ne peuvent que visualiser.
     */
    public boolean isCurrentBrouillonReadOnly() {
        if (currentEntity == null || currentEntity.getStatut() == null) return true;
        String s = currentEntity.getStatut();
        return EntityStatusEnum.ACCEPTED.name().equals(s) || EntityStatusEnum.REFUSED.name().equals(s);
    }

    /**
     * Indique si le brouillon courant est en statut PROPOSITION (en cours).
     */
    public boolean isCurrentBrouillonProposition() {
        return currentEntity != null && currentEntity.getStatut() != null
            && EntityStatusEnum.PROPOSITION.name().equals(currentEntity.getStatut());
    }

    /**
     * Recharge les candidats si nécessaire (lazy loading)
     */
    private void chargerCandidatsIfNeeded() {
        if (!candidatsLoaded) {
            chargerCandidats();
        }
    }

    /**
     * Appelé à l'affichage de la page de création (preRenderView).
     * Réinitialise tout le formulaire de création lorsqu'on arrive via "Nouveau brouillon".
     */
    public void initNouveauCandidat() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null && facesContext.isPostback()) {
            return;
        }
        nouveauCandidat = new Candidat();
        nouveauCandidat.setCreateur(userBean.getUsername());
        nouveauCandidat.setStatut(Candidat.Statut.EN_COURS);
        resetWizardFormCompletely();
        availableCollections = entityRepository.findByEntityTypeCode(EntityConstants.ENTITY_TYPE_COLLECTION);
    }
    
    /**
     * Réinitialise le formulaire du wizard (utilisé en interne après soumission, etc.).
     * Ne réinitialise pas si le formulaire est déjà vide pour éviter de tout effacer lors des AJAX.
     */
    public void resetWizardForm() {
        if (currentStep == 0 && selectedEntityTypeId == null && entityCode == null) {
            // Formulaire déjà vide, pas besoin de réinitialiser
        } else {
            return;
        }
        resetWizardFormCompletely();
    }

    /**
     * Réinitialise intégralement tous les champs du formulaire de création.
     * Appelé lors de l'ouverture de la page create (Nouveau brouillon) et après terminer/abandon.
     */
    public void resetWizardFormCompletely() {
        currentStep = 0;
        currentEntity = null;
        selectedEntityTypeId = null;
        entityCode = null;
        entityLabel = null;
        selectedLangueCode = searchBean != null ? searchBean.getLangSelected() : null;
        selectedCollectionId = null;
        selectedDirectEntityId = null;
        availableReferences = new ArrayList<>();
        availableDirectEntities = new ArrayList<>();
        referenceTreeRoot = null;
        selectedTreeNode = null;
        typeDescription = null;
        serieDescription = null;
        groupDescription = null;
        categoryDescription = null;
        imagePrincipaleUrl = null;
        candidatLabels = new ArrayList<>();
        newLabelValue = null;
        newLabelLangueCode = null;
        descriptions = new ArrayList<>();
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        candidatCommentaireDatation = null;
        candidatBibliographie = null;
        referencesBibliographiques = new ArrayList<>();
        ateliers = new ArrayList<>();
        airesCirculation = new ArrayList<>();
        decors = null;
        marquesEstampilles = new ArrayList<>();
        fonctionUsage = null;
        metrologie = null;
        fabricationFaconnage = null;
        descriptionPate = null;
        couleurPate = null;
        naturePate = null;
        inclusions = null;
        cuissonPostCuisson = null;
        droit = null;
        legendeDroit = null;
        coinsMonetairesDroit = null;
        revers = null;
        legendeRevers = null;
        coinsMonetairesRevers = null;
        materiau = null;
        denomination = null;
        metrologieMonnaie = null;
        valeur = null;
        technique = null;
        fabrication = null;
        collectionDescription = null;
        collectionPublique = true;
        tpq = null;
        taq = null;
        corpusExterne = null;
    }
    
    /**
     * Charge les données existantes depuis currentEntity pour l'étape 3
     */
    private void loadExistingStep3Data() {
        if (currentEntity == null || currentEntity.getId() == null) return;
        Step3FormData data = candidatFormDataLoader.loadForWizardStep3(currentEntity.getId(), selectedLangueCode);
        applyStep3FormData(data);
    }

    private void applyStep3FormData(Step3FormData data) {
        if (data == null) return;
        candidatLabels = data.getCandidatLabels() != null ? new ArrayList<>(data.getCandidatLabels()) : new ArrayList<>();
        descriptions = data.getDescriptions() != null ? new ArrayList<>(data.getDescriptions()) : new ArrayList<>();
        candidatCommentaireDatation = data.getCandidatCommentaireDatation();
        candidatBibliographie = data.getCandidatBibliographie();
        referencesBibliographiques = data.getReferencesBibliographiques() != null ? new ArrayList<>(data.getReferencesBibliographiques()) : new ArrayList<>();
        ateliers = data.getAteliers() != null ? new ArrayList<>(data.getAteliers()) : new ArrayList<>();
        attestations = data.getAttestations() != null ? new ArrayList<>(data.getAttestations()) : new ArrayList<>();
        sitesArcheologiques = data.getSitesArcheologiques() != null ? new ArrayList<>(data.getSitesArcheologiques()) : new ArrayList<>();
        references = data.getReferences() != null ? new ArrayList<>(data.getReferences()) : new ArrayList<>();
        airesCirculation = data.getAiresCirculation() != null ? new ArrayList<>(data.getAiresCirculation()) : new ArrayList<>();
        decors = data.getDecors();
        marquesEstampilles = data.getMarquesEstampilles() != null ? new ArrayList<>(data.getMarquesEstampilles()) : new ArrayList<>();
        fonctionUsage = data.getFonctionUsage();
        metrologie = data.getMetrologie();
        fabricationFaconnage = data.getFabricationFaconnage();
        descriptionPate = data.getDescriptionPate();
        couleurPate = data.getCouleurPate();
        naturePate = data.getNaturePate();
        inclusions = data.getInclusions();
        cuissonPostCuisson = data.getCuissonPostCuisson();
        droit = data.getDroit();
        legendeDroit = data.getLegendeDroit();
        coinsMonetairesDroit = data.getCoinsMonetairesDroit();
        revers = data.getRevers();
        legendeRevers = data.getLegendeRevers();
        coinsMonetairesRevers = data.getCoinsMonetairesRevers();
        materiau = data.getMateriau();
        denomination = data.getDenomination();
        metrologieMonnaie = data.getMetrologieMonnaie();
        valeur = data.getValeur();
        technique = data.getTechnique();
        fabrication = data.getFabrication();
        typologieScientifique = data.getTypologieScientifique();
        identifiantPerenne = data.getIdentifiantPerenne();
        ancienneVersion = data.getAncienneVersion();
        candidatProduction = data.getCandidatProduction();
        if (data.getCollectionPublique() != null) collectionPublique = data.getCollectionPublique();
        typeDescription = data.getTypeDescription();
        tpq = data.getTpq();
        taq = data.getTaq();
        periode = data.getPeriode();
        corpusExterne = data.getCorpusExterne();
        if (data.getSelectedAuteurs() != null) selectedAuteurs = new ArrayList<>(data.getSelectedAuteurs());
    }

    /**
     * Récupère le type d'entité sélectionné
     */
    public EntityType getSelectedEntityType() {
        if (selectedEntityTypeId == null) {
            return null;
        }
        return entityTypeRepository.findById(selectedEntityTypeId).orElse(null);
    }

    /**
     * Retourne true si le type d'entité sélectionné est « Type ».
     * Dans ce cas, les champs Langue et Label sont désactivés et non obligatoires.
     */
    public boolean isSelectedEntityTypeType() {
        EntityType et = getSelectedEntityType();
        return et != null && EntityConstants.ENTITY_TYPE_TYPE.equals(et.getCode());
    }

    /**
     * Récupère la collection sélectionnée
     */
    public Entity getSelectedCollection() {
        if (selectedCollectionId == null) {
            return null;
        }
        return entityRepository.findById(selectedCollectionId).orElse(null);
    }

    /**
     * Récupère la langue sélectionnée
     */
    public Langue getSelectedLangue() {
        if (selectedLangueCode == null) {
            return null;
        }
        return langueRepository.findByCode(selectedLangueCode);
    }

    /**
     * Récupère le nom de la langue sélectionnée
     */
    public String getSelectedLangueName() {
        // Si un candidat est sélectionné, utiliser sa langue
        if (candidatSelectionne != null && candidatSelectionne.getLangue() != null) {
            Langue langue = langueRepository.findByCode(candidatSelectionne.getLangue());
            return langue != null ? langue.getNom() : candidatSelectionne.getLangue();
        }
        // Sinon, utiliser la langue sélectionnée dans le wizard
        Langue langue = getSelectedLangue();
        return langue != null ? langue.getNom() : "";
    }

    /**
     * Crée une entité dans la base de données avec les données de l'étape 1
     * @return L'entité créée
     */
    /**
     * Termine le processus de création de candidat et redirige vers la liste des candidats
     * Met à jour l'entité avec les valeurs finales : période, références bibliographiques, bibliographie, TAQ, TPQ
     */
    public String terminerCandidat() {
        try {
            // Vérifier que currentEntity existe
            if (currentEntity == null || currentEntity.getId() == null) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Erreur", "L'entité n'a pas été créée. Veuillez compléter les étapes précédentes."));
                PrimeFaces.current().ajax().update(":growl");
                return null;
            }

            // Recharger l'entité depuis la base pour éviter les problèmes de détachement
            Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
            if (refreshedEntity == null) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur", "L'entité n'a pas été trouvée dans la base de données."));
                PrimeFaces.current().ajax().update(":growl");
                return null;
            }

            // Validation des champs obligatoires selon le type d'entité
            if (!candidatValidationService.validateRequiredFieldsForEntity(refreshedEntity)) {
                // Ne pas sauvegarder ni rediriger si la validation échoue
                return null;
            }

            // Recharger la liste des candidats pour avoir les données à jour
            chargerCandidats();

            resetWizardFormCompletely();
            currentStep = 0;

            // Rediriger vers la liste des candidats avec un paramètre de succès
            // Le message sera affiché dans loadCandidatsPage() en vérifiant le paramètre
            return "/candidats/candidats.xhtml?faces-redirect=true&success=true";
        } catch (Exception e) {
            log.error("Erreur lors de la finalisation du candidat", e);
            // Rediriger avec un paramètre d'erreur
            return "/candidats/candidats.xhtml?faces-redirect=true&error=true";
        }
    }

    public void sauvegarderCandidat() {
        CandidatSauvegardeRequest req = CandidatSauvegardeRequest.builder()
                .selectedEntityTypeId(selectedEntityTypeId)
                .entityCode(entityCode)
                .entityLabel(entityLabel)
                .selectedLangueCode(selectedLangueCode)
                .selectedCollectionId(selectedCollectionId)
                .selectedParentEntity(selectedParentEntity)
                .currentEntity(currentEntity)
                .candidatLabels(candidatLabels)
                .descriptions(descriptions)
                .candidatCommentaireDatation(candidatCommentaireDatation)
                .candidatBibliographie(candidatBibliographie)
                .referencesBibliographiques(referencesBibliographiques)
                .typeDescription(typeDescription)
                .serieDescription(serieDescription)
                .groupDescription(groupDescription)
                .collectionDescription(collectionDescription)
                .imagePrincipaleUrl(imagePrincipaleUrl)
                .periode(periode)
                .tpq(tpq)
                .taq(taq)
                .droit(droit)
                .legendeDroit(legendeDroit)
                .coinsMonetairesDroit(coinsMonetairesDroit)
                .revers(revers)
                .legendeRevers(legendeRevers)
                .coinsMonetairesRevers(coinsMonetairesRevers)
                .materiau(materiau)
                .denomination(denomination)
                .metrologieMonnaie(metrologieMonnaie)
                .valeur(valeur)
                .technique(technique)
                .fabrication(fabrication)
                .openThesoCreatedReference(openThesoDialogBean != null ? openThesoDialogBean.getCreatedReference() : null)
                .currentUser(loginBean != null ? loginBean.getCurrentUser() : null)
                .build();

        CandidatSauvegardeResult result = candidatSauvegardeService.executeSauvegarde(req);
        FacesContext fc = FacesContext.getCurrentInstance();
        if (result.isSuccess()) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", result.getSuccessMessage()));
            resetWizardFormCompletely();
            currentStep = 0;
            PrimeFaces.current().ajax().update(":growl", ":createCandidatForm");
            PrimeFaces.current().executeScript("setTimeout(function(){window.location.href='/candidats/candidats.xhtml';}, 1500);");
        } else {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", result.getErrorMessage()));
            PrimeFaces.current().ajax().update(":growl", ":createCandidatForm");
        }
    }

    public void prepareValidateCandidat(Candidat candidat) throws Exception {
        if (candidat == null) throw new Exception("Candidat null");
        CandidatVisualisationService.PrepareValidateResult res = candidatVisualisationService.prepareValidate(candidat.getId());
        if (!res.success()) throw new Exception(res.errorMessage() != null ? res.errorMessage() : "Le candidat n'existe pas");
        candidatAValider = candidat;
        selectedEntityTypeId = res.entityTypeId();
        candidatLabels = res.labels() != null ? new ArrayList<>(res.labels()) : new ArrayList<>();
        descriptions = res.descriptions() != null ? new ArrayList<>(res.descriptions()) : new ArrayList<>();
        periode = res.periode();
    }

    /**
     * Prépare le refus d'un candidat (stocke le candidat et ouvre le dialogue de confirmation)
     */
    public void prepareRefuseCandidat(Candidat candidat) {
        this.candidatARefuser = candidat;
    }

    /**
     * Refuse un candidat après confirmation (change le statut à REFUSED)
     */
    public void refuserCandidatConfirm() {
        if (candidatARefuser != null) {
            refuserCandidat(candidatARefuser);
            candidatARefuser = null;
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucun candidat sélectionné pour refus."));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    /**
     * Prépare la remise en brouillon d'un candidat (stocke le candidat et ouvre le dialogue)
     */
    public void prepareRemettreEnBrouillon(Candidat candidat) {
        this.candidatARemettreEnBrouillon = candidat;
    }

    /**
     * Remet un candidat en brouillon après confirmation (change le statut à PROPOSITION)
     */
    public void remettreEnBrouillonConfirm() {
        if (candidatARemettreEnBrouillon != null) {
            remettreEnBrouillon(candidatARemettreEnBrouillon);
            candidatARemettreEnBrouillon = null;
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucun candidat sélectionné pour remise en brouillon."));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    /**
     * Prépare la suppression d'un candidat (stocke le candidat et ouvre le dialogue)
     */
    public void prepareDeleteCandidat(Candidat candidat) {
        this.candidatASupprimer = candidat;
    }

    /**
     * Valide un candidat après confirmation (change le statut à ACCEPTED)
     */
    public void validerCandidatConfirm() {
        if (candidatAValider != null) {
            validerCandidat(candidatAValider);
            candidatAValider = null; // Réinitialiser après validation
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucun candidat sélectionné pour validation."));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    /**
     * Supprime un candidat après confirmation
     */
    public void supprimerCandidatConfirm() {
        if (candidatASupprimer != null) {
            supprimerCandidat(candidatASupprimer);
            candidatASupprimer = null; // Réinitialiser après suppression
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Aucun candidat sélectionné pour suppression."));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    public boolean hasProduction() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return false;
        }

        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        return refreshedEntity != null && refreshedEntity.getProduction() != null;
    }

    public boolean hasPeriode() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return false;
        }

        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        return refreshedEntity != null && refreshedEntity.getPeriode() != null;
    }

    public boolean hasAireCirculation() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return false;
        }

        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity != null && refreshedEntity.getAiresCirculation() != null) {
            // Filtrer pour ne garder que celles avec le code AIRE_CIRCULATION
            return refreshedEntity.getAiresCirculation().stream()
                    .anyMatch(ref -> ReferenceOpenthesoEnum.AIRE_CIRCULATION.name().equals(ref.getCode()));
        }
        return false;
    }

    /**
     * Récupère la liste des aires de circulation pour l'entité courante
     * Charge depuis la base de données si la liste n'est pas encore chargée
     */
    public List<ReferenceOpentheso> getAiresCirculation() {
        if (currentEntity == null || currentEntity.getId() == null) {
            return new ArrayList<>();
        }

        // Recharger l'entité pour avoir la liste à jour
        Entity refreshedEntity = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (refreshedEntity != null && refreshedEntity.getAiresCirculation() != null) {
            // Filtrer pour ne garder que celles avec le code AIRE_CIRCULATION
            airesCirculation = refreshedEntity.getAiresCirculation().stream()
                    .filter(ref -> ReferenceOpenthesoEnum.AIRE_CIRCULATION.name().equals(ref.getCode()))
                    .collect(Collectors.toList());
        } else {
            airesCirculation = new ArrayList<>();
        }
        return airesCirculation != null ? airesCirculation : new ArrayList<>();
    }

    public void validerCandidat(Candidat candidat) {
        ActionResult r = candidatValidationActionService.validerCandidat(candidat != null ? candidat.getId() : null, loginBean.getCurrentUser());
        applyActionResult(r, ":growl, :candidatsForm");
        if (r.success()) { candidatsLoaded = false; chargerCandidats(); }
    }

    public void refuserCandidat(Candidat candidat) {
        ActionResult r = candidatValidationActionService.refuserCandidat(candidat != null ? candidat.getId() : null, loginBean.getCurrentUser());
        applyActionResult(r, ":growl, :candidatsForm");
        if (r.success()) { candidatsLoaded = false; chargerCandidats(); }
    }

    public void remettreEnBrouillon(Candidat candidat) {
        ActionResult r = candidatValidationActionService.remettreEnBrouillon(candidat != null ? candidat.getId() : null, loginBean.getCurrentUser());
        applyActionResult(r, ":growl, :candidatsForm");
        if (r.success()) { candidatsLoaded = false; chargerCandidats(); }
    }

    public void supprimerCandidat(Candidat candidat) {
        ActionResult r = candidatValidationActionService.supprimerCandidat(candidat != null ? candidat.getId() : null);
        applyActionResult(r, ":growl, :candidatsForm");
        if (r.success()) { candidatsLoaded = false; chargerCandidats(); }
    }

    public String validerCandidatFromView() {
        if (currentEntity == null || currentEntity.getId() == null) { addErrorMessage("Aucune entité à valider."); return null; }
        ActionResult r = candidatValidationActionService.validerCandidatFromView(currentEntity.getId(), selectedAuteurs,
                attestations, sitesArcheologiques, referentiel, typologieScientifique, identifiantPerenne, ancienneVersion, loginBean.getCurrentUser());
        if (r.success()) { candidatsLoaded = false; chargerCandidats(); addInfoMessage(r.message()); return r.redirectUrl(); }
        addErrorMessage(r.errorMessage()); return null;
    }

    public String refuserCandidatFromView() {
        if (currentEntity == null || currentEntity.getId() == null) { addErrorMessage("Aucune entité à refuser."); return null; }
        ActionResult r = candidatValidationActionService.refuserCandidatFromView(currentEntity.getId(), selectedAuteurs,
                attestations, sitesArcheologiques, referentiel, typologieScientifique, identifiantPerenne, ancienneVersion, loginBean.getCurrentUser());
        if (r.success()) { candidatsLoaded = false; chargerCandidats(); addInfoMessage(r.message()); return r.redirectUrl(); }
        addErrorMessage(r.errorMessage()); return null;
    }

    private String applyActionResult(ActionResult r, String updateIds) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (r.success()) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", r.message()));
        } else {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", r.errorMessage() != null ? r.errorMessage() : "Erreur"));
        }
        PrimeFaces.current().ajax().update(updateIds);
        return r.redirectUrl();
    }

    public String visualiserCandidat(Candidat candidat) {
        if (candidat == null) return null;
        candidatSelectionne = candidat;
        String defaultLangue = candidat.getLangue() != null ? candidat.getLangue() : (searchBean != null ? searchBean.getLangSelected() : "fr");
        VisualisationPrepareResult res = candidatVisualisationService.prepareVisualisation(candidat.getId(), defaultLangue);
        if (!res.isSuccess()) {
            addErrorMessage(res.getErrorMessage());
            return null;
        }
        selectedEntityTypeId = res.getSelectedEntityTypeId();
        entityCode = res.getEntityCode();
        selectedLangueCode = res.getSelectedLangueCode();
        entityLabel = res.getEntityLabel();
        selectedCollectionId = res.getSelectedCollectionId();
        selectedParentEntity = res.getSelectedParentEntity();
        currentEntity = res.getCurrentEntity();
        candidatLabels = res.getCandidatLabels() != null ? new ArrayList<>(res.getCandidatLabels()) : new ArrayList<>();
        descriptions = res.getDescriptions() != null ? new ArrayList<>(res.getDescriptions()) : new ArrayList<>();
        periode = res.getPeriode();

        droit = res.getStep3Data().getDroit();
        legendeDroit = res.getStep3Data().getLegendeDroit();
        coinsMonetairesDroit = res.getStep3Data().getCoinsMonetairesDroit();
        ancienneVersion = res.getStep3Data().getAncienneVersion();
        identifiantPerenne = res.getStep3Data().getIdentifiantPerenne();
        typologieScientifique = res.getStep3Data().getTypologieScientifique();
        attestations = res.getStep3Data().getAttestations();
        sitesArcheologiques = res.getStep3Data().getSitesArcheologiques();
        references = res.getStep3Data().getReferences();

        if (res.getStep3Data() != null) applyStep3FormData(res.getStep3Data());
        loadAvailableAuteurs();
        return res.getRedirectUrl();
    }

    public void loadAvailableAuteurs() { availableAuteurs = candidatFormLoadService.loadAuteursSorted(); }

    public String enregistrerModifications() {
        if (currentEntity == null || currentEntity.getId() == null) {
            addErrorMessage("Aucune entité à enregistrer.");
            return null;
        }
        CandidatVisualisationService.EnregistrerResult res = candidatVisualisationService.enregistrerModifications(currentEntity.getId(), selectedAuteurs,
                attestations, sitesArcheologiques, referentiel, typologieScientifique, identifiantPerenne, ancienneVersion);
        if (res.success()) {
            addInfoMessage("Les modifications ont été enregistrées avec succès.");
            candidatsLoaded = false;
            chargerCandidats();
            return res.redirectUrl();
        }
        addErrorMessage(res.errorMessage());
        return null;
    }

    /**
     * Valide le candidat depuis la page de visualisation (change le statut à ACCEPTED)
     */
    public String getCollectionLabel(Entity collection) {
        return collection == null
                ? getCollectionLabel()
                : candidatReferenceTreeService.getCollectionLabel(collection, searchBean.getLangSelected());
    }

    public String getCollectionLabel() {
        Entity entity = collectionService.findCollectionIdByEntityId(candidatSelectionne.getId());
        return candidatReferenceTreeService.getCollectionLabel(entity, searchBean.getLangSelected());
    }

    public boolean isEntity(Object obj) { return candidatReferenceTreeService.isEntity(obj); }
    public boolean isNodeEntity(Object node) { return candidatReferenceTreeService.isNodeEntity(node); }
    public Entity getEntityFromNode(Object node) { return candidatReferenceTreeService.getEntityFromNode(node); }
    public String getNodeDisplayValue(Object node) { return candidatReferenceTreeService.getNodeDisplayValue(node); }
    public boolean isRootNode(Object node) { return candidatReferenceTreeService.isRootNode(node); }

    public void addLabelFromInput() {
        CandidatLabelDescriptionService.AddLabelResult r = candidatLabelDescriptionService.addLabel(
                currentEntity != null ? currentEntity.getId() : null, newLabelValue, newLabelLangueCode,
                selectedLangueCode, candidatLabels);
        if (!r.success()) {
            addErrorMessage(r.errorMessage() != null ? r.errorMessage() : "Erreur lors de l'ajout du label.");
            return;
        }
        if (candidatLabels == null) candidatLabels = new ArrayList<>();
        candidatLabels.add(r.addedItem());
        newLabelValue = null;
        newLabelLangueCode = null;
        addInfoMessage("Le label a été ajouté avec succès.");
    }

    public void removeCandidatLabel(CategoryLabelItem labelItem) {
        CandidatLabelDescriptionService.RemoveLabelResult r = candidatLabelDescriptionService.removeLabel(
                currentEntity != null ? currentEntity.getId() : null, labelItem);
        if (!r.success()) {
            addErrorMessage(r.errorMessage() != null ? r.errorMessage() : "Erreur lors de la suppression.");
            return;
        }
        if (candidatLabels != null) candidatLabels.remove(labelItem);
        addInfoMessage("Le label a été supprimé avec succès.");
    }

    public boolean isLangueAlreadyUsedIncandidatLabels(String langueCode, CategoryLabelItem currentItem) {
        return candidatLabelDescriptionService.isLangueAlreadyUsedInLabels(langueCode, currentItem, candidatLabels, currentEntity);
    }

    /**
     * Obtient les langues disponibles pour un nouveau label (excluant celle de l'étape 1 et celles déjà utilisées)
     */
    public List<Langue> getAvailableLanguagesForNewLabel() {
        if (availableLanguages == null) {
            return new ArrayList<>();
        }
        return availableLanguages.stream()
            .filter(langue -> {
                // Exclure la langue de l'étape 1
                if (selectedLangueCode != null && selectedLangueCode.equals(langue.getCode())) {
                    return false;
                }
                // Exclure les langues déjà utilisées
                return !isLangueAlreadyUsedIncandidatLabels(langue.getCode(), null);
            })
            .collect(Collectors.toList());
    }

    public void addCandidatDescriptionFromInput() {
        CandidatLabelDescriptionService.AddDescriptionResult r = candidatLabelDescriptionService.addDescription(
                currentEntity != null ? currentEntity.getId() : null, newDescriptionValue, newDescriptionLangueCode, descriptions);
        if (!r.success()) {
            addErrorMessage(r.errorMessage() != null ? r.errorMessage() : "Erreur lors de l'ajout de la description.");
            return;
        }
        if (descriptions == null) descriptions = new ArrayList<>();
        descriptions.add(r.addedItem());
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        addInfoMessage("La description a été ajoutée avec succès.");
    }

    public void removeCandidatDescription(CategoryDescriptionItem descriptionItem) {
        CandidatLabelDescriptionService.RemoveDescriptionResult r = candidatLabelDescriptionService.removeDescription(
                currentEntity != null ? currentEntity.getId() : null, descriptionItem);
        if (!r.success()) {
            addErrorMessage(r.errorMessage() != null ? r.errorMessage() : "Erreur lors de la suppression.");
            return;
        }
        if (descriptions != null) descriptions.remove(descriptionItem);
        addInfoMessage("La description a été supprimée avec succès.");
    }

    public boolean isLangueAlreadyUsedIndescriptions(String langueCode, CategoryDescriptionItem currentItem) {
        return candidatLabelDescriptionService.isLangueAlreadyUsedInDescriptions(langueCode, currentItem, descriptions, currentEntity);
    }

    /**
     * Obtient les langues disponibles pour une nouvelle description (toutes les langues disponibles, excluant seulement celles déjà utilisées)
     */
        public List<Langue> getAvailableLanguagesForNewDescription() {
        if (availableLanguages == null) {
            return new ArrayList<>();
        }
        return availableLanguages.stream()
            .filter(langue -> {
                // Exclure uniquement les langues déjà utilisées dans les descriptions (pour éviter les doublons)
                // La langue principale de l'étape 1 est incluse car on peut avoir une description dans la même langue
                return !isLangueAlreadyUsedIndescriptions(langue.getCode(), null);
            })
            .collect(Collectors.toList());
    }

    public void saveSitesArcheologiques() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveSitesArcheologiques(currentEntity.getId(), sitesArcheologiques);
        }
    }

    public void saveReferences() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveReferences(currentEntity.getId(), references);
        }
    }

    public void saveAttestations() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveAttestations(currentEntity.getId(), attestations);
        }
    }

    public void saveAncienneVersion() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveAncienneVersion(currentEntity.getId(), ancienneVersion);
        }
    }

    public void saveTypologieScientifique() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveTypologieScientifique(currentEntity.getId(), typologieScientifique);
        }
    }

    public void saveIdentifiantPerenne() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveIdentifiantPerenne(currentEntity.getId(), identifiantPerenne);
        }
    }

    public void saveCommentaireDatation() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveCommentaireDatation(currentEntity.getId(), candidatCommentaireDatation);
        }
    }

    public void saveBibliographie() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveBibliographie(currentEntity.getId(), candidatBibliographie);
        }
    }

    public void saveReferencesBibliographiques() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveReferencesBibliographiques(currentEntity.getId(), referencesBibliographiques);
        }
    }

    public void saveAteliers() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveAteliers(currentEntity.getId(), ateliers);
        }
    }

    public void saveMarquesEstampilles() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveMarquesEstampilles(currentEntity.getId(), marquesEstampilles);
        }
    }

    public boolean hasFonctionUsage() {
        if (currentEntity == null || currentEntity.getId() == null) return false;
        if (fonctionUsage != null) return true;
        fonctionUsage = candidatOpenThesoService.loadFonctionUsage(currentEntity.getId());
        return fonctionUsage != null;
    }

    public void updateFonctionUsageFromOpenTheso() {
        if (currentEntity == null || currentEntity.getId() == null) return;
        currentEntity = entityRepository.findById(currentEntity.getId()).orElse(currentEntity);
        fonctionUsage = candidatOpenThesoService.loadFonctionUsage(currentEntity.getId());
    }

    public void deleteFonctionUsage() {
        if (currentEntity == null || currentEntity.getId() == null) {
            addErrorMessage("Aucune entité sélectionnée.");
            return;
        }
        CandidatOpenThesoService.DeleteResult result = candidatOpenThesoService.deleteFonctionUsage(currentEntity.getId());
        if (result == CandidatOpenThesoService.DeleteResult.SUCCESS) {
            fonctionUsage = null;
            addInfoMessage("La fonction/usage a été supprimée avec succès.");
        } else {
            addWarnMessage("Aucune fonction/usage à supprimer.");
        }
        PrimeFaces.current().ajax().update(":growl");
    }

    public void saveDescriptionPate() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveDescriptionPate(currentEntity.getId(), descriptionPate);
        }
    }

    public void saveDroit() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveDroit(currentEntity.getId(), droit);
            addInfoMessage("Le droit a été enregistré.");
        }
    }

    public void saveLegendeDroit() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveLegendeDroit(currentEntity.getId(), legendeDroit);
            addInfoMessage("La légende du droit a été enregistrée.");
        }
    }

    public void saveCoinsMonetairesDroit() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveCoinsMonetairesDroit(currentEntity.getId(), coinsMonetairesDroit);
            addInfoMessage("Les coins monétaires droit ont été enregistrés.");
        }
    }

    public void saveRevers() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveRevers(currentEntity.getId(), revers);
            addInfoMessage("Le revers a été enregistré.");
        }
    }

    public void saveLegendeRevers() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveLegendeRevers(currentEntity.getId(), legendeRevers);
            addInfoMessage("La légende du revers a été enregistrée.");
        }
    }

    public void saveCoinsMonetairesRevers() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveCoinsMonetairesRevers(currentEntity.getId(), coinsMonetairesRevers);
            addInfoMessage("Les coins monétaires revers ont été enregistrés.");
        }
    }

    private void addErrorMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", msg));
    }
    private void addInfoMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", msg));
    }
    private void addWarnMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", msg));
    }

    private boolean hasRef(java.util.function.Supplier<ReferenceOpentheso> getter, java.util.function.Function<Long, ReferenceOpentheso> loader, java.util.function.Consumer<ReferenceOpentheso> setter) {
        if (currentEntity == null || currentEntity.getId() == null) return false;
        if (getter.get() != null) return true;
        ReferenceOpentheso r = loader.apply(currentEntity.getId()); setter.accept(r); return r != null;
    }
    private void refreshAndLoadRef(java.util.function.Function<Long, ReferenceOpentheso> loader, java.util.function.Consumer<ReferenceOpentheso> setter) {
        if (currentEntity == null || currentEntity.getId() == null) return;
        currentEntity = entityRepository.findById(currentEntity.getId()).orElse(currentEntity);
        setter.accept(loader.apply(currentEntity.getId()));
    }
    private void deleteRefAndRefresh(java.util.function.Function<Long, CandidatOpenThesoService.DeleteResult> deleter, java.util.function.Consumer<ReferenceOpentheso> clearer) {
        if (currentEntity == null || currentEntity.getId() == null) return;
        deleter.apply(currentEntity.getId());
        currentEntity = entityRepository.findById(currentEntity.getId()).orElse(currentEntity);
        clearer.accept(null);
    }

    public boolean hasMetrologie() { return hasRef(() -> metrologie, candidatOpenThesoService::loadMetrologie, r -> metrologie = r); }
    public boolean hasFabricationFaconnage() { return hasRef(() -> fabricationFaconnage, candidatOpenThesoService::loadFabrication, r -> fabricationFaconnage = r); }
    public boolean hasCouleurPate() { return hasRef(() -> couleurPate, candidatOpenThesoService::loadCouleurPate, r -> couleurPate = r); }
    public boolean hasNaturePate() { return hasRef(() -> naturePate, candidatOpenThesoService::loadNaturePate, r -> naturePate = r); }
    public boolean hasInclusions() { return hasRef(() -> inclusions, candidatOpenThesoService::loadInclusions, r -> inclusions = r); }
    public boolean hasCuissonPostCuisson() { return hasRef(() -> cuissonPostCuisson, candidatOpenThesoService::loadCuissonPostCuisson, r -> cuissonPostCuisson = r); }

    public void updateMetrologieFromOpenTheso() { refreshAndLoadRef(candidatOpenThesoService::loadMetrologie, r -> metrologie = r); }
    public void updateFabricationFaconnageFromOpenTheso() { refreshAndLoadRef(candidatOpenThesoService::loadFabrication, r -> fabricationFaconnage = r); }
    public void updateCouleurPateFromOpenTheso() { refreshAndLoadRef(candidatOpenThesoService::loadCouleurPate, r -> couleurPate = r); }
    public void updateNaturePateFromOpenTheso() { refreshAndLoadRef(candidatOpenThesoService::loadNaturePate, r -> naturePate = r); }
    public void updateInclusionsFromOpenTheso() { refreshAndLoadRef(candidatOpenThesoService::loadInclusions, r -> inclusions = r); }
    public void updateCuissonPostCuissonFromOpenTheso() { refreshAndLoadRef(candidatOpenThesoService::loadCuissonPostCuisson, r -> cuissonPostCuisson = r); }

    public void deleteMetrologie() { deleteRefAndRefresh(candidatOpenThesoService::deleteMetrologie, r -> metrologie = null); }
    public void deleteFabricationFaconnage() { deleteRefAndRefresh(candidatOpenThesoService::deleteFabrication, r -> fabricationFaconnage = null); }
    public void deleteCouleurPate() { deleteRefAndRefresh(candidatOpenThesoService::deleteCouleurPate, r -> couleurPate = null); }
    public void deleteNaturePate() { deleteRefAndRefresh(candidatOpenThesoService::deleteNaturePate, r -> naturePate = null); }
    public void deleteInclusions() { deleteRefAndRefresh(candidatOpenThesoService::deleteInclusions, r -> inclusions = null); }
    public void deleteCuissonPostCuisson() { deleteRefAndRefresh(candidatOpenThesoService::deleteCuissonPostCuisson, r -> cuissonPostCuisson = null); }

    public void deleteMateriau() { deleteRefAndRefresh(candidatOpenThesoService::deleteMateriau, r -> materiau = null); }
    public void deleteDenomination() { deleteRefAndRefresh(candidatOpenThesoService::deleteDenomination, r -> denomination = null); }
    public void deleteValeur() { deleteRefAndRefresh(candidatOpenThesoService::deleteValeur, r -> valeur = null); }
    public void deleteTechnique() { deleteRefAndRefresh(candidatOpenThesoService::deleteTechnique, r -> technique = null); }
    public void deleteFabricationMonnaie() { deleteRefAndRefresh(candidatOpenThesoService::deleteFabricationMonnaie, r -> fabrication = null); }

    private boolean hasRefMonnaie(java.util.function.Supplier<ReferenceOpentheso> getter, java.util.function.Function<Long, ReferenceOpentheso> loader, java.util.function.Consumer<ReferenceOpentheso> setter) {
        if (currentEntity == null || currentEntity.getId() == null) return false;
        if (getter.get() != null) return true;
        ReferenceOpentheso r = loader.apply(currentEntity.getId()); setter.accept(r); return r != null;
    }

    public boolean hasMateriau() { return hasRefMonnaie(() -> materiau, candidatOpenThesoService::loadMateriau, r -> materiau = r); }
    public boolean hasDenomination() { return hasRefMonnaie(() -> denomination, candidatOpenThesoService::loadDenomination, r -> denomination = r); }
    public boolean hasValeur() { return hasRefMonnaie(() -> valeur, candidatOpenThesoService::loadValeur, r -> valeur = r); }
    public boolean hasTechnique() { return hasRefMonnaie(() -> technique, candidatOpenThesoService::loadTechnique, r -> technique = r); }
    public boolean hasFabrication() { return hasRefMonnaie(() -> fabrication, candidatOpenThesoService::loadFabricationMonnaie, r -> fabrication = r); }

    public void updateMateriauFromOpenTheso() { refreshAndLoadRef(candidatOpenThesoService::loadMateriau, r -> materiau = r); }
    public void updateDenominationFromOpenTheso() { refreshAndLoadRef(candidatOpenThesoService::loadDenomination, r -> denomination = r); }
    public void updateValeurFromOpenTheso() { refreshAndLoadRef(candidatOpenThesoService::loadValeur, r -> valeur = r); }
    public void updateTechniqueFromOpenTheso() { refreshAndLoadRef(candidatOpenThesoService::loadTechnique, r -> technique = r); }
    public void updateFabricationMonnaieFromOpenTheso() { refreshAndLoadRef(candidatOpenThesoService::loadFabricationMonnaie, r -> fabrication = r); }

    public void saveMetrologieMonnaie() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveMetrologieMonnaie(currentEntity.getId(), metrologieMonnaie);
            addInfoMessage("La métrologie a été enregistrée.");
        }
    }

    public void saveTypeDescription() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveTypeDescription(currentEntity.getId(), typeDescription);
        }
    }

    public void saveCollectionDescription() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveCollectionDescription(currentEntity.getId(), collectionDescription);
        }
    }

    public void saveCollectionPublique() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveCollectionPublique(currentEntity.getId(), collectionPublique, currentEntity.getStatut());
        }
    }

    public void saveTpqFields() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveTpq(currentEntity.getId(), tpq);
        }
    }

    public void saveTaqFields() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveTaq(currentEntity.getId(), taq);
        }
    }

    public void saveDecors() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveDecors(currentEntity.getId(), decors);
        }
    }
    
    /**
     * Surcharge de getEntityTypeName pour accepter un Candidat
     */
    public String getEntityTypeName(Candidat candidat) {
        if (candidat == null || candidat.getTypeCode() == null) {
            return "";
        }
        String code = candidat.getTypeCode();
        return switch (code) {
            case EntityConstants.ENTITY_TYPE_COLLECTION -> "Collection";
            case EntityConstants.ENTITY_TYPE_REFERENCE -> "Référentiel";
            case EntityConstants.ENTITY_TYPE_CATEGORY -> "Catégorie";
            case EntityConstants.ENTITY_TYPE_GROUP -> "Groupe";
            case EntityConstants.ENTITY_TYPE_SERIES -> "Série";
            case EntityConstants.ENTITY_TYPE_TYPE -> "Type";
            default -> code;
        };
    }

    /**
     * Retourne la classe CSS du badge pour la colonne Type (design distinct par type d'entité).
     */
    public String getTypeBadgeCssClass(Candidat candidat) {
        if (candidat == null || candidat.getTypeCode() == null || candidat.getTypeCode().isBlank()) {
            return "candidat-type-badge candidat-type-default";
        }
        String code = candidat.getTypeCode().toLowerCase();
        String modifier = switch (code) {
            case "collection", "referentiel", "categorie", "groupe", "serie", "type" -> "candidat-type-" + code;
            default -> "candidat-type-default";
        };
        return "candidat-type-badge " + modifier;
    }

    /**
     * Retourne l'icône PrimeIcons pour le type d'entité.
     */
    public String getTypeIcon(Candidat candidat) {
        if (candidat == null || candidat.getTypeCode() == null) {
            return "pi pi-tag";
        }
        String code = candidat.getTypeCode();
        return switch (code.toUpperCase()) {
            case "COLLECTION" -> "pi pi-folder-open";
            case "REFERENTIEL" -> "pi pi-book";
            case "CATEGORIE" -> "pi pi-tags";
            case "GROUPE" -> "pi pi-users";
            case "SERIE" -> "pi pi-list";
            case "TYPE" -> "pi pi-tag";
            default -> "pi pi-tag";
        };
    }
    
    /**
     * Obtient le code de l'entité à partir du candidat sélectionné
     */
    public String getEntityCodeFromCandidat() {
        if (candidatSelectionne == null || candidatSelectionne.getId() == null) {
            return "Non sélectionné";
        }

        Optional<Entity> entityOpt = entityRepository.findById(candidatSelectionne.getId());
        return entityOpt.isPresent() ? entityOpt.get().getCode() : "Non sélectionné";
    }
    
    /**
     * Obtient le code de l'entité parente à partir du candidat sélectionné
     */
    public String getParentCodeFromCandidat() {
        if (candidatSelectionne == null || candidatSelectionne.getId() == null) {
            return "Non sélectionné";
        }

        Optional<Entity> entityOpt = entityRepository.findById(candidatSelectionne.getId());
        if (entityOpt.isPresent()) {
            Entity entity = entityOpt.get();
            // Trouver l'entité parente (non-collection)
            List<Entity> parents = entityRelationRepository.findParentsByChild(entity);
            if (parents != null && !parents.isEmpty()) {
                for (Entity parent : parents) {
                    if (parent.getEntityType() != null &&
                            !EntityConstants.ENTITY_TYPE_COLLECTION.equals(parent.getEntityType().getCode())) {
                        return parent.getCode();
                    }
                }
            }
        }
        return "Non sélectionné";
    }

    /**
     * Met à jour le champ candidatProduction avec le terme sélectionné depuis OpenTheso
     */
    public void updateProductionFromOpenTheso() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatProduction = candidatOpenThesoService.loadProductionValue(currentEntity.getId());
            currentEntity = entityRepository.findById(currentEntity.getId()).orElse(currentEntity);
        }
    }

    public void updateAireCirculationFromOpenTheso() {
        if (currentEntity != null && currentEntity.getId() != null) {
            currentEntity = entityRepository.findById(currentEntity.getId()).orElse(currentEntity);
            airesCirculation = candidatOpenThesoService.loadAiresCirculation(currentEntity.getId());
        }
    }

    public void deleteAireCirculation(Long referenceId) {
        if (currentEntity == null || currentEntity.getId() == null) {
            addErrorMessage("Aucune entité sélectionnée.");
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        if (referenceId == null) {
            addErrorMessage("Aucune référence sélectionnée pour suppression.");
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        CandidatOpenThesoService.DeleteResult result = candidatOpenThesoService.deleteAireCirculation(currentEntity.getId(), referenceId);
        if (result == CandidatOpenThesoService.DeleteResult.SUCCESS) {
            currentEntity = entityRepository.findById(currentEntity.getId()).orElse(currentEntity);
            airesCirculation = candidatOpenThesoService.loadAiresCirculation(currentEntity.getId());
            addInfoMessage("L'aire de circulation a été supprimée avec succès.");
        } else {
            addWarnMessage("La référence sélectionnée n'existe pas ou n'appartient pas à cette entité.");
        }
        PrimeFaces.current().ajax().update(":growl");
    }

    /**
     * Prépare la suppression d'une aire de circulation (pour le dialog de confirmation)
     */
    private Long aireCirculationToDeleteId;

    public void prepareDeleteAireCirculation(Long referenceId) {
        this.aireCirculationToDeleteId = referenceId;
    }

    public void deleteAireCirculation() {
        deleteAireCirculation(aireCirculationToDeleteId);
        aireCirculationToDeleteId = null;
    }

    /** Type d'élément à supprimer pour la boîte de dialogue de confirmation unique */
    private String confirmDeleteType;
    /** Message affiché dans la boîte de dialogue de confirmation */
    private String confirmDeleteMessage;
    /** Composants à mettre à jour après la suppression (ex: :createCandidatForm :growl) */
    private String confirmDeleteUpdate;

    public String getConfirmDeleteMessage() { return confirmDeleteMessage; }
    public String getConfirmDeleteUpdate() { return confirmDeleteUpdate; }

    private void applyConfirmConfig(CandidatConfirmDeleteService.ConfirmConfig c) {
        if (c != null) {
            confirmDeleteType = c.type();
            confirmDeleteMessage = c.message();
            confirmDeleteUpdate = c.update();
        }
    }

    public void prepareConfirmDeletePeriode() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("PERIODE")); }
    public void prepareConfirmDeleteProduction() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("PRODUCTION")); }
    public void prepareConfirmDeleteAireCirculation(Long referenceId) {
        prepareDeleteAireCirculation(referenceId);
        applyConfirmConfig(candidatConfirmDeleteService.getConfig("AIRE_CIRCULATION"));
    }
    public void prepareConfirmDeleteFonctionUsage() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("FONCTION_USAGE")); }
    public void prepareConfirmDeleteMetrologie() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("METROLOGIE")); }
    public void prepareConfirmDeleteFabricationFaconnage() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("FABRICATION_FACONNAGE")); }
    public void prepareConfirmDeleteCouleurPate() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("COULEUR_PATE")); }
    public void prepareConfirmDeleteNaturePate() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("NATURE_PATE")); }
    public void prepareConfirmDeleteInclusions() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("INCLUSIONS")); }
    public void prepareConfirmDeleteCuissonPostCuisson() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("CUISSON_POST_CUISSON")); }
    public void prepareConfirmDeleteMateriau() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("MATERIAU")); }
    public void prepareConfirmDeleteDenomination() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("DENOMINATION")); }
    public void prepareConfirmDeleteValeur() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("VALEUR")); }
    public void prepareConfirmDeleteTechnique() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("TECHNIQUE")); }
    public void prepareConfirmDeleteFabricationMonnaie() { applyConfirmConfig(candidatConfirmDeleteService.getConfig("FABRICATION_MONNAIE")); }

    /** Exécute la suppression selon le type préparé (appelé depuis la boîte de confirmation unique) */
    public void executeConfirmDelete() {
        if (confirmDeleteType == null) return;
        switch (confirmDeleteType) {
            case "PERIODE" -> deletePeriode();
            case "PRODUCTION" -> deleteProduction();
            case "AIRE_CIRCULATION" -> deleteAireCirculation();
            case "FONCTION_USAGE" -> deleteFonctionUsage();
            case "METROLOGIE" -> deleteMetrologie();
            case "MATERIAU" -> deleteMateriau();
            case "DENOMINATION" -> deleteDenomination();
            case "VALEUR" -> deleteValeur();
            case "TECHNIQUE" -> deleteTechnique();
            case "FABRICATION_MONNAIE" -> deleteFabricationMonnaie();
            case "FABRICATION_FACONNAGE" -> deleteFabricationFaconnage();
            case "COULEUR_PATE" -> deleteCouleurPate();
            case "NATURE_PATE" -> deleteNaturePate();
            case "INCLUSIONS" -> deleteInclusions();
            case "CUISSON_POST_CUISSON" -> deleteCuissonPostCuisson();
            default -> { }
        }
        confirmDeleteType = null;
        confirmDeleteMessage = null;
        confirmDeleteUpdate = null;
    }

    public void abandonnerProposition() {
        if (currentEntity == null || currentEntity.getId() == null) {
            resetWizardFormCompletely();
            return;
        }
        Entity entityToDelete = entityRepository.findById(currentEntity.getId()).orElse(null);
        if (entityToDelete != null) {
            candidatEntityService.deleteEntityWithRelations(entityToDelete);
            addInfoMessage("La proposition a été abandonnée et supprimée.");
        }
        resetWizardFormCompletely();
        currentEntity = null;
    }

    public void deletePeriode() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatOpenThesoService.deletePeriode(currentEntity.getId());
            currentEntity = entityRepository.findById(currentEntity.getId()).orElse(currentEntity);
        }
    }

    public void deleteProduction() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatOpenThesoService.deleteProduction(currentEntity.getId());
            currentEntity = entityRepository.findById(currentEntity.getId()).orElse(currentEntity);
            candidatProduction = null;
        }
    }

    public void updatePeriodeFromOpenTheso() {
        if (currentEntity != null && currentEntity.getId() != null) {
            currentEntity = entityRepository.findById(currentEntity.getId()).orElse(currentEntity);
        }
    }

    /**
     * Sauvegarde automatiquement les champs du groupe (période, TPQ, TAQ) dans la base de données
     */
    public void saveCorpus() {
        if (currentEntity != null && currentEntity.getId() != null) {
            candidatFormSaveService.saveCorpus(currentEntity.getId(), corpusExterne);
        }
    }
}

