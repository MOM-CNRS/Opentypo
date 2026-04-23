package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.DescriptionItem;
import fr.cnrs.opentypo.application.dto.GroupEnum;
import fr.cnrs.opentypo.application.dto.NameItem;
import fr.cnrs.opentypo.application.dto.PermissionRoleEnum;
import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.application.dto.pactols.PactolsConcept;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysique;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysiqueMonnaie;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
import fr.cnrs.opentypo.domain.entity.DescriptionMonnaie;
import fr.cnrs.opentypo.domain.entity.DescriptionPate;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityMetadata;
import fr.cnrs.opentypo.domain.entity.Image;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import fr.cnrs.opentypo.domain.entity.UserPermission;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.CaracteristiquePhysiqueMonnaieRepository;
import fr.cnrs.opentypo.infrastructure.persistence.CaracteristiquePhysiqueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionDetailRepository;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionMonnaieRepository;
import fr.cnrs.opentypo.application.service.EntityImageService;
import fr.cnrs.opentypo.infrastructure.persistence.DescriptionPateRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityMetadataRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ImageRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UserPermissionRepository;
import fr.cnrs.opentypo.infrastructure.persistence.UtilisateurRepository;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.servlet.http.Part;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DualListModel;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Getter
@Setter
@SessionScoped
@Named(value = "entityUpdateBean")
@Slf4j
public class EntityUpdateBean implements Serializable {

    private final ReferenceBean referenceBean;
    @Autowired
    private LoginBean loginBean;

    @Autowired
    private LangueRepository langueRepository;

    @Autowired
    private ApplicationBean applicationBean;

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private DescriptionDetailRepository descriptionDetailRepository;

    @Autowired
    private TreeBean treeBean;

    @Autowired
    private SearchBean searchBean;

    @Autowired
    private DescriptionMonnaieRepository descriptionMonnaieRepository;

    @Autowired
    private CaracteristiquePhysiqueRepository caracteristiquePhysiqueRepository;

    @Autowired
    private DescriptionPateRepository descriptionPateRepository;
    
    @Autowired
    private EntityMetadataRepository entityMetadataRepository;
    
    @Autowired
    private CaracteristiquePhysiqueMonnaieRepository caracteristiquePhysiqueMonnaieRepository;

    @Autowired
    private CollectionBean collectionBean;

    @Autowired
    private EntityImageService entityImageService;

    @Autowired
    private ImageRepository imageRepository;

    /** Images en cours d'édition (URL + légende) */
    private List<EditingImageItem> editingImages = new ArrayList<>();

    /** Retourne la liste des images sans doublons d'URL (évite duplication après rafraîchissement). */
    public List<EditingImageItem> getEditingImages() {
        if (editingImages == null) return new ArrayList<>();
        List<EditingImageItem> deduped = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        for (EditingImageItem item : editingImages) {
            if (item != null && StringUtils.hasText(item.getUrl())) {
                String url = item.getUrl().trim();
                if (!seenUrls.contains(url)) {
                    seenUrls.add(url);
                    deduped.add(item);
                }
            }
        }
        if (deduped.size() != editingImages.size()) {
            editingImages = deduped;
        }
        return new ArrayList<>(editingImages);
    }
    /** URL saisie pour ajouter une nouvelle image */
    private String newImageUrlInput;
    /** Emplacements pour fichiers en attente (stockage serveur à la validation uniquement) */
    private List<PendingFileSlot> pendingFileSlots = new ArrayList<>();
    /** Holder pour fichiers multiples (classe simple non proxied, évite PropertyNotWritableException). */
    private final PendingFilePartsHolder pendingFilePartsHolder = new PendingFilePartsHolder();

    public PendingFilePartsHolder getPendingFilePartsHolder() {
        return pendingFilePartsHolder;
    }

    /** Délégation pour lier h:inputFile (évite PropertyNotWritableException sur holder.parts). */
    public List<Part> getPendingFileParts() {
        return pendingFilePartsHolder != null ? pendingFilePartsHolder.getParts() : new ArrayList<>();
    }
    public void setPendingFileParts(List<Part> parts) {
        if (pendingFilePartsHolder != null) pendingFilePartsHolder.setParts(parts);
    }
    /** URLs ajoutées depuis pendingFileSlots lors de prepareSave (à annuler si l'utilisateur annule) */
    private List<String> uploadedUrlsToRevertOnCancel = new ArrayList<>();
    /** URLs des images déjà présentes à l'ouverture de l'édition. */
    private Set<String> initialEditingImageUrlKeys = new HashSet<>();
    /** URLs d'images supprimées par l'utilisateur ; les fichiers physiques seront effacés à l'enregistrement */
    private List<String> removedImageUrlsToDeleteOnSave = new ArrayList<>();
    /** Hash SHA-256 des fichiers uploadés (pour détecter les doublons) */
    private Map<String, String> uploadedFileUrlToHash = new HashMap<>();

    private DualListModel<Long> redacteursPickList;
    private DualListModel<Long> validateursPickList;
    private DualListModel<Long> relecteursPickList;
    private DualListModel<Long> gestionnairesPickList;

    private List<NameItem> noms = new ArrayList<>();
    private List<DescriptionItem> descriptions = new ArrayList<>();
    private List<Langue> availableTmpLanguagesForLabel, availableTmpLanguagesForDefinition;

    private boolean editingEntity;
    private String code;
    private String newLabelLangueCode;
    private String newLabelValue;
    private String newDescriptionLangueCode;
    private String newDescriptionValue;
    private String bibliographie;
    private String commentaire;
    private String commentaireDatation;
    private String decors;
    private String appellationUsuelle;
    private String identifiantPerenne;
    private String typologieScientifique;
    private String ancienneVersion;
    private String appartient;
    private String associe;
    private String ateliersValue;
    private List<String> ateliers;
    private String referentielValue;
    private List<String> referentiels;
    private String siteArcheologiqueValue;
    private List<String> sitesArcheologiques;
    private String attestationValue;
    private List<String> attestations;
    private List<Langue> availableLanguages;
    private List<ReferenceOpentheso> airesCirculation;
    private ReferenceOpentheso fonctionUsage;
    private List<String> marquesEstampilles = new ArrayList<>();
    private String marqueEstampilleValue;
    private String descriptionPate;
    private String corpusExterne;
    private String alignementExterne;
    private String periode;
    private Integer tpq;
    private Integer taq;
    private String metrologie;
    private String droit;
    private String legendeDroit;
    private String coinsMonetairesDroit;
    private String revers;
    private String legendeRevers;
    private String coinsMonetairesRevers;
    private List<String> referenceBibliographiqueList;

    private PactolsConcept periodeAutocompleteSelection;
    private PactolsConcept productionAutocompleteSelection = new PactolsConcept();
    private PactolsConcept aireCirculationAutocompleteSelection = new PactolsConcept();
    private PactolsConcept fonctionUsageAutocompleteSelection = new PactolsConcept();
    private PactolsConcept categorieFonctionnelleAutocompleteSelection = new PactolsConcept();
    private PactolsConcept metrologieAutocompleteSelection = new PactolsConcept();
    private PactolsConcept fabricationFaconnageAutocompleteSelection = new PactolsConcept();
    private PactolsConcept couleurPateAutocompleteSelection = new PactolsConcept();
    private PactolsConcept naturePateAutocompleteSelection = new PactolsConcept();
    private PactolsConcept inclusionsAutocompleteSelection = new PactolsConcept();
    private PactolsConcept dimensionsAutocompleteSelection = new PactolsConcept();
    private PactolsConcept cuissonPostCuissonAutocompleteSelection = new PactolsConcept();
    private PactolsConcept materiauxAutocompleteSelection = new PactolsConcept();
    private PactolsConcept denominationAutocompleteSelection = new PactolsConcept();
    private PactolsConcept valeurAutocompleteSelection = new PactolsConcept();
    private PactolsConcept techniqueAutocompleteSelection = new PactolsConcept();
    private PactolsConcept fabricationAutocompleteSelection = new PactolsConcept();
    private PactolsConcept formeAutocompleteSelection = new PactolsConcept();

    @Inject
    public EntityUpdateBean(@Named("referenceBean") ReferenceBean referenceBean) {
        this.referenceBean = referenceBean;
    }


    /** Active le mode édition in-place pour le groupe sélectionné (comme ReferenceBean.startEditingReference). */
    public void startEditing() {
        editingEntity = true;
        Entity entity = applicationBean.getSelectedEntity();
        code = entity.getCode() != null ? entity.getCode() : "";

        newLabelValue = "";
        newDescriptionValue = "";
        bibliographie = entity.getBibliographie();
        commentaire = entity.getCommentaire();

        List<ReferenceOpentheso> airesSrc = entity.getAiresCirculation();
        if (airesSrc == null) airesSrc = new ArrayList<>();
        airesCirculation = airesSrc.stream()
                .filter(ref -> ReferenceOpenthesoEnum.AIRE_CIRCULATION.name().equals(ref.getCode()))
                .collect(Collectors.toCollection(ArrayList::new));

        ateliersValue = entity.getAteliers();
        if (entity.getAteliers() != null && !entity.getAteliers().isEmpty()) {
            ateliers = new ArrayList<>(java.util.Arrays.asList(entity.getAteliers().split(";\\s*")));
        } else {
            ateliers = new ArrayList<>();
        }

        attestationValue = entity.getAttestations();
        if (entity.getAttestations() != null && !entity.getAttestations().isEmpty()) {
            attestations = new ArrayList<>(java.util.Arrays.asList(entity.getAttestations().split(";\\s*")));
        } else {
            attestations = new ArrayList<>();
        }

        siteArcheologiqueValue = entity.getSitesArcheologiques();
        if (entity.getSitesArcheologiques() != null && !entity.getSitesArcheologiques().isEmpty()) {
            sitesArcheologiques = new ArrayList<>(java.util.Arrays.asList(entity.getSitesArcheologiques().split(";\\s*")));
        } else {
            sitesArcheologiques = new ArrayList<>();
        }

        referentielValue = entity.getReference();
        if (entity.getReference() != null && !entity.getReference().isEmpty()) {
            referentiels = new ArrayList<>(java.util.Arrays.asList(entity.getReference().split(";\\s*")));
        } else {
            referentiels = new ArrayList<>();
        }

        decors = entity.getDescriptionDetail() == null ? "" : (entity.getDescriptionDetail().getDecors() != null ? entity.getDescriptionDetail().getDecors() : "");
        appellationUsuelle = entity.getAppellation() == null ? "" : entity.getAppellation();
        identifiantPerenne = entity.getIdentifiantPerenne() == null ? "" : entity.getIdentifiantPerenne();
        typologieScientifique = entity.getTypologieScientifique() == null ? "" : entity.getTypologieScientifique();

        ancienneVersion = entity.getAncienneVersion() == null ? "" : entity.getAncienneVersion();
        appartient = entity.getAppartient() == null ? "" : entity.getAppartient();
        associe = entity.getAssocie() == null ? "" : entity.getAssocie();
        commentaireDatation = entity.getCommentaireDatation() == null ? "" : entity.getCommentaireDatation();

        marquesEstampilles = new ArrayList<>();
        if (entity.getDescriptionDetail() != null && entity.getDescriptionDetail().getMarques() != null && !entity.getDescriptionDetail().getMarques().isEmpty()) {
            marquesEstampilles = new ArrayList<>(java.util.Arrays.asList(entity.getDescriptionDetail().getMarques().split(";\\s*")));
        }

        productionAutocompleteSelection = refToConcept(entity.getProduction());
        fonctionUsageAutocompleteSelection = refToConcept(entity.getDescriptionDetail() != null ? entity.getDescriptionDetail().getFonction() : null);
        categorieFonctionnelleAutocompleteSelection = refToConcept(entity.getDescriptionMonnaie() != null ? entity.getDescriptionMonnaie().getEntity().getCategorieFonctionnelle() : null);
        metrologie = entity.getCaracteristiquePhysiqueMonnaie() == null ? "" : entity.getCaracteristiquePhysiqueMonnaie().getMetrologie();
        descriptionPate = entity.getDescriptionPate() == null ? "" : (entity.getDescriptionPate().getDescription() != null ? entity.getDescriptionPate().getDescription() : "");

        periodeAutocompleteSelection = refToConcept(entity.getPeriode());
        metrologieAutocompleteSelection = refToConcept(entity.getCaracteristiquePhysique() != null ? entity.getCaracteristiquePhysique().getMetrologie() : null);
        fabricationFaconnageAutocompleteSelection = refToConcept(entity.getCaracteristiquePhysique() != null ? entity.getCaracteristiquePhysique().getFabrication() : null);
        couleurPateAutocompleteSelection = refToConcept(entity.getDescriptionPate() != null ? entity.getDescriptionPate().getCouleur() : null);
        naturePateAutocompleteSelection = refToConcept(entity.getDescriptionPate() != null ? entity.getDescriptionPate().getNature() : null);
        inclusionsAutocompleteSelection = refToConcept(entity.getDescriptionPate() != null ? entity.getDescriptionPate().getInclusion() : null);
        cuissonPostCuissonAutocompleteSelection = refToConcept(entity.getDescriptionPate() != null ? entity.getDescriptionPate().getCuisson() : null);
        dimensionsAutocompleteSelection = refToConcept(entity.getCaracteristiquePhysique() != null ? entity.getCaracteristiquePhysique().getDimensions() : null);
        formeAutocompleteSelection = refToConcept(entity.getCaracteristiquePhysique() != null ? entity.getCaracteristiquePhysique().getForme() : null);
        materiauxAutocompleteSelection = refToConcept(entity.getCaracteristiquePhysique() != null ? entity.getCaracteristiquePhysique().getMateriaux() : null);

        noms = entity.getLabels().stream()
                .map(element -> NameItem.builder()
                        .nom(element.getNom())
                        .langueCode(element.getLangue().getCode())
                        .langue(element.getLangue())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        descriptions = entity.getDescriptions().stream()
                .map(element -> DescriptionItem.builder()
                        .valeur(element.getValeur())
                        .langueCode(element.getLangue().getCode())
                        .langue(element.getLangue())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        tpq = entity.getTpq();
        taq = entity.getTaq();
        corpusExterne = entity.getMetadata() != null ? entity.getMetadata().getCorpusExterne() : null;
        alignementExterne = entity.getMetadata() != null ? entity.getMetadata().getAlignementExterne() : null;

        droit = entity.getDescriptionMonnaie() != null ? entity.getDescriptionMonnaie().getDroit() : null;
        legendeDroit = entity.getDescriptionMonnaie() != null ? entity.getDescriptionMonnaie().getLegendeDroit() : null;
        coinsMonetairesDroit = entity.getDescriptionMonnaie() != null ? entity.getDescriptionMonnaie().getCoinsMonetairesDroit() : null;
        revers = entity.getDescriptionMonnaie() != null ? entity.getDescriptionMonnaie().getRevers() : null;
        legendeRevers = entity.getDescriptionMonnaie() != null ? entity.getDescriptionMonnaie().getLegendeRevers() : null;
        coinsMonetairesRevers = entity.getDescriptionMonnaie() != null ? entity.getDescriptionMonnaie().getCoinsMonetairesRevers() : null;

        referenceBibliographiqueList = new ArrayList<>();
        if ( entity.getReferenceBibliographique() != null && ! entity.getReferenceBibliographique().isEmpty()) {
            referenceBibliographiqueList = Arrays.stream( entity.getReferenceBibliographique().split("[;；]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        // Charger les images existantes (via repository pour éviter LazyInitializationException)
        if (imageRepository != null && entity.getId() != null) {
            List<Image> images = imageRepository.findByEntity_Id(entity.getId());
            editingImages = images != null
                    ? images.stream()
                            .filter(img -> img != null && img.getUrl() != null && !img.getUrl().isBlank())
                            .map(img -> new EditingImageItem(img.getUrl(), img.getLegende()))
                            .collect(Collectors.toList())
                    : new ArrayList<>();
        } else {
            editingImages = new ArrayList<>();
        }
        initialEditingImageUrlKeys = editingImages.stream()
                .filter(Objects::nonNull)
                .map(EditingImageItem::getUrl)
                .map(this::normalizeUrlKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        newImageUrlInput = null;
        pendingFileSlots = new ArrayList<>();
        for (int i = 0; i < 10; i++) pendingFileSlots.add(new PendingFileSlot());
        if (pendingFilePartsHolder != null) pendingFilePartsHolder.setParts(new ArrayList<>());
        uploadedFileUrlToHash = new HashMap<>();
        removedImageUrlsToDeleteOnSave = new ArrayList<>();

        initHabilitationsPickLists(entity.getId());
        updateAvailableTmpLanguagesForLabel();
        updateAvailableTmpLanguagesForDefinition();

        if (availableTmpLanguagesForLabel.stream().anyMatch(element -> element.getCode().equals(searchBean.getLangSelected()))) {
            newLabelLangueCode = searchBean.getLangSelected();
        } else {
            newLabelLangueCode = CollectionUtils.isEmpty(availableTmpLanguagesForLabel)
                    ? "" : availableTmpLanguagesForLabel.stream().findFirst().get().getCode();
        }


        if (availableTmpLanguagesForLabel.stream().anyMatch(element -> element.getCode().equals(searchBean.getLangSelected()))) {
            newDescriptionLangueCode = searchBean.getLangSelected();
        } else {
            newDescriptionLangueCode = CollectionUtils.isEmpty(availableTmpLanguagesForLabel)
                    ? "" : availableTmpLanguagesForLabel.stream().findFirst().get().getCode();
        }

        if (entity.getEntityType().getId() == 6) {
            collectionBean.initEditingGestionnairesPickList(entity);
        } else if (entity.getEntityType().getId() == 1) {
            referenceBean.initEditingGestionnairesPickListForEdit(entity.getId());
        }
    }

    public void resetCategoryDialogForm() {
        code = null;
        newLabelLangueCode = null;
        newLabelValue = null;
        noms = new ArrayList<>();
        descriptions = new ArrayList<>();
        newDescriptionLangueCode = null;
        newDescriptionValue = null;
        bibliographie = null;
        commentaire = null;
        decors = null;
        commentaireDatation = null;
        appellationUsuelle = null;
        typologieScientifique = null;
        identifiantPerenne = null;
        ancienneVersion = null;
        appartient = null;
        associe = null;
        descriptionPate = null;
        ateliers = new ArrayList<>();
        ateliersValue = null;
        attestationValue = null;
        attestations = new ArrayList<>();
        referentielValue = null;
        referentiels = new ArrayList<>();
        siteArcheologiqueValue = null;
        sitesArcheologiques = new ArrayList<>();
        metrologie = null;
        airesCirculation = new ArrayList<>();
        marquesEstampilles = new ArrayList<>();
        periodeAutocompleteSelection = new PactolsConcept();
        productionAutocompleteSelection = new PactolsConcept();
        fonctionUsageAutocompleteSelection = new PactolsConcept();
        categorieFonctionnelleAutocompleteSelection = new PactolsConcept();
        aireCirculationAutocompleteSelection = new PactolsConcept();
        metrologieAutocompleteSelection = new PactolsConcept();
        fabricationFaconnageAutocompleteSelection = new PactolsConcept();
        couleurPateAutocompleteSelection = new PactolsConcept();
        naturePateAutocompleteSelection = new PactolsConcept();
        inclusionsAutocompleteSelection = new PactolsConcept();
        cuissonPostCuissonAutocompleteSelection = new PactolsConcept();
        dimensionsAutocompleteSelection = new PactolsConcept();
        formeAutocompleteSelection = new PactolsConcept();
        materiauxAutocompleteSelection = new PactolsConcept();
        tpq = null;
        taq = null;
        corpusExterne = null;
        alignementExterne = null;
        droit = null;
        legendeDroit = null;
        coinsMonetairesDroit = null;
        revers = null;
        legendeRevers = null;
        coinsMonetairesRevers = null;
        redacteursPickList = null;
        validateursPickList = null;
        relecteursPickList = null;
        referenceBibliographiqueList = new ArrayList<>();
        editingImages = new ArrayList<>();
        initialEditingImageUrlKeys = new HashSet<>();
        newImageUrlInput = null;
        pendingFileSlots = new ArrayList<>();
        for (int i = 0; i < 10; i++) pendingFileSlots.add(new PendingFileSlot());
        if (pendingFilePartsHolder != null) pendingFilePartsHolder.setParts(new ArrayList<>());
        uploadedFileUrlToHash = new HashMap<>();
        removedImageUrlsToDeleteOnSave = new ArrayList<>();
    }

    private void initHabilitationsPickLists(Long entityId) {
        if (userPermissionRepository == null || utilisateurRepository == null) return;
        List<Utilisateur> source = utilisateurRepository.findByGroupeNom(GroupEnum.UTILISATEUR.getLabel());
        List<Long> sourceIds = (source != null) ? source.stream().map(Utilisateur::getId).filter(Objects::nonNull).toList() : List.of();
        List<Long> redTarget = entityId != null ? userPermissionRepository.findUserIdsByEntityIdAndRole(entityId, PermissionRoleEnum.REDACTEUR.getLabel()) : List.of();
        List<Long> valTarget = entityId != null ? userPermissionRepository.findUserIdsByEntityIdAndRole(entityId, PermissionRoleEnum.VALIDEUR.getLabel()) : List.of();
        List<Long> relTarget = entityId != null ? userPermissionRepository.findUserIdsByEntityIdAndRole(entityId, PermissionRoleEnum.RELECTEUR.getLabel()) : List.of();
        List<Long> redSource = new ArrayList<>(sourceIds);
        redSource.removeAll(redTarget != null ? redTarget : List.of());
        List<Long> valSource = new ArrayList<>(sourceIds);
        valSource.removeAll(valTarget != null ? valTarget : List.of());
        List<Long> relSource = new ArrayList<>(sourceIds);
        relSource.removeAll(relTarget != null ? relTarget : List.of());
        redacteursPickList = new DualListModel<>(redSource != null ? redSource : new ArrayList<>(), redTarget != null ? new ArrayList<>(redTarget) : new ArrayList<>());
        validateursPickList = new DualListModel<>(valSource != null ? valSource : new ArrayList<>(), valTarget != null ? new ArrayList<>(valTarget) : new ArrayList<>());
        relecteursPickList = new DualListModel<>(relSource != null ? relSource : new ArrayList<>(), relTarget != null ? new ArrayList<>(relTarget) : new ArrayList<>());
    }

    public DualListModel<Long> getRedacteursPickList() {
        if (redacteursPickList == null && applicationBean != null && applicationBean.getSelectedEntity() != null) {
            initHabilitationsPickLists(applicationBean.getSelectedEntity().getId());
        }
        return redacteursPickList != null ? redacteursPickList : new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    }

    public DualListModel<Long> getValidateursPickList() {
        if (validateursPickList == null && applicationBean != null && applicationBean.getSelectedEntity() != null) {
            initHabilitationsPickLists(applicationBean.getSelectedEntity().getId());
        }
        return validateursPickList != null ? validateursPickList : new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    }

    public DualListModel<Long> getRelecteursPickList() {
        if (relecteursPickList == null && applicationBean != null && applicationBean.getSelectedEntity() != null) {
            initHabilitationsPickLists(applicationBean.getSelectedEntity().getId());
        }
        return relecteursPickList != null ? relecteursPickList : new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    }

    public List<Utilisateur> getRedacteursList() {
        return utilisateurRepository != null ? utilisateurRepository.findByGroupeNom(GroupEnum.UTILISATEUR.getLabel()) : new ArrayList<>();
    }

    public List<Utilisateur> getValidateursList() {
        return utilisateurRepository != null ? utilisateurRepository.findByGroupeNom(GroupEnum.UTILISATEUR.getLabel()) : new ArrayList<>();
    }

    public List<Utilisateur> getRelecteursList() {
        return utilisateurRepository != null ? utilisateurRepository.findByGroupeNom(GroupEnum.UTILISATEUR.getLabel()) : new ArrayList<>();
    }

    public String getUtilisateurDisplayName(Long userId) {
        if (userId == null || utilisateurRepository == null) return "";
        return utilisateurRepository.findById(userId)
                .map(u -> ((u.getPrenom() != null ? u.getPrenom() : "") + " " + (u.getNom() != null ? u.getNom().toUpperCase() : "")).trim())
                .orElse("");
    }

    public boolean isLangueAlreadyUsedInNames(String code, NameItem exclude) {
        if (noms == null || code == null) return false;
        return noms.stream()
                .filter(element -> element != exclude && element.getLangueCode() != null)
                .anyMatch(element -> element.getLangueCode().equalsIgnoreCase(code));
    }

    public void addTempLabel() {

        if (newLabelValue == null || newLabelValue.trim().isEmpty()) {
            addErrorMessage("Le label est requis.");
            return;
        }
        if (newLabelLangueCode == null || newLabelLangueCode.trim().isEmpty()) {
            addErrorMessage("La langue est requise.");
            return;
        }

        if (noms == null) noms = new ArrayList<>();
        noms.add(NameItem.builder().nom(newLabelValue).langueCode(newLabelLangueCode)
                .langue(langueRepository.findByCode(newLabelLangueCode)).build());
        updateAvailableTmpLanguagesForLabel();
        newLabelValue = null;
        newLabelLangueCode = null;
        addInfoMessage("Le label a été ajouté avec succès.");
    }

    public boolean isLangueAlreadyUsedInLabels(String langueCode, List<NameItem> candidatLabels) {
        if (langueCode == null || langueCode.isEmpty() || candidatLabels == null) return false;
        return candidatLabels.stream()
                .filter(element -> element.getLangueCode() != null)
                .anyMatch(element -> element.getLangueCode().equalsIgnoreCase(langueCode));
    }

    public void addTempDescription() {

        if (descriptions == null) descriptions = new ArrayList<>();

        if (newDescriptionValue == null || newDescriptionValue.trim().isEmpty()) {
            addErrorMessage("La description est requise.");
            return;
        }

        if (newDescriptionLangueCode == null || newDescriptionLangueCode.trim().isEmpty()) {
            addErrorMessage("La langue est requise.");
            return;
        }

        descriptions.add(DescriptionItem.builder().valeur(newDescriptionValue)
                .langueCode(newDescriptionLangueCode).langue(langueRepository.findByCode(newDescriptionLangueCode)).build());

        updateAvailableTmpLanguagesForDefinition();
        newDescriptionValue = null;
        newDescriptionLangueCode = null;
        addInfoMessage("La description a été ajoutée avec succès.");
    }

    public boolean isLangueAlreadyUsedInDescriptions(String langueCode, List<DescriptionItem> descriptions) {
        if (langueCode == null || langueCode.isEmpty() || descriptions == null) return false;

        return descriptions.stream()
                .filter(element -> element.getLangueCode() != null)
                .anyMatch(element -> element.getLangueCode().equalsIgnoreCase(langueCode));
    }

    public void removeTmpDescription(DescriptionItem descriptionItem) {
        if (descriptions != null && descriptionItem != null) {
            descriptions.remove(descriptionItem);
        }
        updateAvailableTmpLanguagesForDefinition();
        addInfoMessage("La description a été supprimée avec succès.");
    }

    public void removeTempLabel(NameItem labelItem) {

        if (noms != null) noms.remove(labelItem);
        updateAvailableTmpLanguagesForLabel();
        addInfoMessage("Le label a été supprimé avec succès.");
    }

    /** Indique si l'édition est autorisée (toujours true en mode modification catalogue). */
    public boolean canEditCurrentBrouillon() {
        return editingEntity;
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

    /**
     * Gère l'upload immédiat d'un fichier image (p:fileUpload).
     * Sauvegarde sur le serveur et ajoute l'URL à editingImageUrls.
     */
    public void handleFileUpload(FileUploadEvent event) {
        if (event == null || event.getFile() == null || entityImageService == null) {
            addErrorMessage("Impossible d'ajouter l'image.");
            PrimeFaces.current().ajax().update(":contentPanels", ":growl");
            return;
        }
        try {
            String contextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
            String url = entityImageService.saveUploadedImage(event.getFile(), contextPath);
            if (editingImages == null) editingImages = new ArrayList<>();
            if (editingImages.stream().anyMatch(i -> i != null && i.getUrl() != null && i.getUrl().trim().equalsIgnoreCase(url))) {
                addWarnMessage("Cette image a déjà été ajoutée.");
            } else {
                editingImages.add(new EditingImageItem(url, null));
                if (uploadedUrlsToRevertOnCancel == null) uploadedUrlsToRevertOnCancel = new ArrayList<>();
                uploadedUrlsToRevertOnCancel.add(url);
                addInfoMessage("Image ajoutée.");
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'upload d'une image", e);
            addErrorMessage("Impossible d'enregistrer l'image : " + (e.getMessage() != null ? e.getMessage() : "erreur inconnue"));
        }
        PrimeFaces.current().ajax().update(":contentPanels", ":growl");
    }

    /** Ajoute une image par URL. */
    public void addImageFromUrl() {
        if (newImageUrlInput == null || newImageUrlInput.trim().isEmpty()) {
            addErrorMessage("Veuillez saisir une URL.");
            return;
        }
        String url = newImageUrlInput.trim();
        if (!isValidRemoteImageUrl(url)) {
            addErrorMessage("L'URL n'est pas valide ou l'image n'est pas accessible.");
            PrimeFaces.current().ajax().update(":contentPanels", ":growl");
            return;
        }
        if (editingImages == null) {
            editingImages = new ArrayList<>();
        }
        if (editingImages.stream().anyMatch(i -> i != null && i.getUrl() != null && i.getUrl().trim().equalsIgnoreCase(url))) {
            addErrorMessage("Cette URL a déjà été ajoutée.");
            PrimeFaces.current().ajax().update(":contentPanels", ":growl");
            return;
        }
        editingImages.add(new EditingImageItem(url, null));
        newImageUrlInput = null;
        addInfoMessage("Image ajoutée.");
        PrimeFaces.current().ajax().update(":contentPanels", ":growl");
    }

    private boolean isValidRemoteImageUrl(String rawUrl) {
        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (IllegalArgumentException e) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            return false;
        }
        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            return false;
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("HEAD");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 400) {
                if (status == HttpURLConnection.HTTP_BAD_METHOD) {
                    return isValidRemoteImageUrlWithGet(uri);
                }
                return false;
            }
            String contentType = connection.getContentType();
            if (isImageContentType(contentType)) {
                return true;
            }
            return isLikelyImagePath(uri.getPath());
        } catch (IOException e) {
            log.warn("Impossible de vérifier l'URL image {}", rawUrl, e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean isValidRemoteImageUrlWithGet(URI uri) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Range", "bytes=0-1024");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 400) {
                return false;
            }

            String contentType = connection.getContentType();
            if (isImageContentType(contentType)) {
                return true;
            }
            return isLikelyImagePath(uri.getPath());
        } catch (IOException e) {
            log.warn("Impossible de vérifier l'URL image (GET) {}", uri, e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean isImageContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        String normalized = contentType.toLowerCase().split(";")[0].trim();
        return normalized.startsWith("image/");
    }

    private boolean isLikelyImagePath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".jpg")
                || lowerPath.endsWith(".jpeg")
                || lowerPath.endsWith(".png")
                || lowerPath.endsWith(".gif")
                || lowerPath.endsWith(".webp");
    }

    /**
     * Appelé quand l'utilisateur sélectionne un fichier : enregistre immédiatement et ajoute un nouveau slot.
     * Un seul slot vide reste affiché pour la prochaine sélection.
     * Vérifie les doublons par hash du contenu.
     */
    public void addImageFromFileAndAddSlot() {
        if (pendingFileSlots == null || pendingFileSlots.isEmpty() || entityImageService == null) {
            addErrorMessage("Impossible d'ajouter l'image.");
            PrimeFaces.current().ajax().update(":centerContent", ":growl");
            return;
        }
        PendingFileSlot slot = pendingFileSlots.get(0);
        if (slot.getPart() == null || slot.getPart().getSize() == 0) {
            addErrorMessage("Veuillez sélectionner un fichier.");
            PrimeFaces.current().ajax().update(":centerContent", ":growl");
            return;
        }
        try {
            byte[] content = slot.getPart().getInputStream().readAllBytes();
            String hash = computeSha256Hex(content);
            if (uploadedFileUrlToHash != null && uploadedFileUrlToHash.containsValue(hash)) {
                addErrorMessage("Ce fichier a déjà été ajouté.");
                pendingFileSlots.set(0, new PendingFileSlot());
                PrimeFaces.current().ajax().update(":centerContent", ":growl");
                return;
            }
            String contextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
            String filename = slot.getPart().getSubmittedFileName();
            String contentType = slot.getPart().getContentType();
            String url = entityImageService.saveUploadedImage(content, filename, contentType, contextPath);
            if (editingImages == null) editingImages = new ArrayList<>();
            editingImages.add(new EditingImageItem(url, null));
            if (uploadedUrlsToRevertOnCancel == null) uploadedUrlsToRevertOnCancel = new ArrayList<>();
            uploadedUrlsToRevertOnCancel.add(url);
            if (uploadedFileUrlToHash == null) uploadedFileUrlToHash = new HashMap<>();
            uploadedFileUrlToHash.put(url, hash);
            addInfoMessage("Image ajoutée.");
        } catch (IOException e) {
            log.error("Erreur lors de la lecture du fichier", e);
            addErrorMessage("Impossible de lire le fichier.");
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'image", e);
            addErrorMessage("Impossible d'enregistrer l'image : " + (e.getMessage() != null ? e.getMessage() : "erreur inconnue"));
        }
        pendingFileSlots.set(0, new PendingFileSlot());
        PrimeFaces.current().ajax().update(":centerContent", ":growl");
    }

    private static String computeSha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }

    /**
     * Upload les fichiers en attente et les fusionne dans editingImageUrls.
     * Appelé par ConfirmSaveBean dans prepareSaveForSelectedEntity (car Part n'est valide que pendant la requête).
     */
    public void uploadPendingFilesAndMergeToEditingUrls() {
        if (entityImageService == null) return;
        uploadedUrlsToRevertOnCancel = new ArrayList<>();
        String contextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
        if (editingImages == null) editingImages = new ArrayList<>();
        if (uploadedFileUrlToHash == null) uploadedFileUrlToHash = new HashMap<>();

        List<Part> toProcess = new ArrayList<>();
        if (pendingFilePartsHolder != null && pendingFilePartsHolder.getParts() != null && !pendingFilePartsHolder.getParts().isEmpty()) {
            toProcess.addAll(pendingFilePartsHolder.getParts());
        }
        List<PendingFileSlot> slots = pendingFileSlots != null ? pendingFileSlots : new ArrayList<>();
        for (PendingFileSlot slot : slots) {
            if (slot != null && slot.getPart() != null && slot.getPart().getSize() > 0) {
                toProcess.add(slot.getPart());
            }
        }

        for (Part part : toProcess) {
            if (part == null || part.getSize() <= 0) continue;
            try {
                byte[] content = part.getInputStream().readAllBytes();
                String hash = computeSha256Hex(content);
                if (uploadedFileUrlToHash != null && uploadedFileUrlToHash.containsValue(hash)) continue;
                String url = entityImageService.saveUploadedImage(content, part.getSubmittedFileName(),
                        part.getContentType(), contextPath);
                editingImages.add(new EditingImageItem(url, null));
                uploadedUrlsToRevertOnCancel.add(url);
                if (uploadedFileUrlToHash != null) uploadedFileUrlToHash.put(url, hash);
            } catch (Exception e) {
                log.error("Erreur lors de l'upload d'une image en attente", e);
                addErrorMessage("Impossible d'enregistrer un fichier : " + (e.getMessage() != null ? e.getMessage() : "erreur inconnue"));
            }
        }
        if (pendingFilePartsHolder != null) pendingFilePartsHolder.setParts(new ArrayList<>());
        pendingFileSlots = new ArrayList<>();
        for (int i = 0; i < 10; i++) pendingFileSlots.add(new PendingFileSlot());
    }

    /** Annule les uploads effectués lors du dernier prepareSave (si l'utilisateur a cliqué Annuler). */
    public void revertPendingFileUploads() {
        if (uploadedUrlsToRevertOnCancel != null && editingImages != null) {
            editingImages.removeIf(item -> item != null && uploadedUrlsToRevertOnCancel.contains(item.getUrl()));
            if (entityImageService != null) {
                for (String url : uploadedUrlsToRevertOnCancel) {
                    entityImageService.deletePhysicalFileByUrl(url);
                }
            }
            if (uploadedFileUrlToHash != null) {
                uploadedUrlsToRevertOnCancel.forEach(uploadedFileUrlToHash::remove);
            }
            uploadedUrlsToRevertOnCancel = new ArrayList<>();
        }
    }

    /** Supprime l'image à l'index donné. */
    public void removeImageAtIndex(int index) {
        if (editingImages != null && index >= 0 && index < editingImages.size()) {
            editingImages.remove(index);
            addInfoMessage("Image supprimée.");
        }
        PrimeFaces.current().ajax().update(":contentPanels", ":growl");
    }

    /** Retire l'image de la liste d'édition. La suppression physique du fichier est reportée à l'enregistrement. */
    public void removeImageByUrl(String url) {
        if (editingImages != null && url != null && editingImages.removeIf(item -> item != null && url.equals(item.getUrl()))) {
            if (url.contains("/uploaded-images/") && removedImageUrlsToDeleteOnSave != null) {
                removedImageUrlsToDeleteOnSave.add(url);
            }
            if (uploadedUrlsToRevertOnCancel != null) uploadedUrlsToRevertOnCancel.remove(url);
            if (uploadedFileUrlToHash != null) uploadedFileUrlToHash.remove(url);
            addInfoMessage("Image retirée (suppression effective à l'enregistrement).");
        }
        PrimeFaces.current().ajax().update(":contentPanels", ":growl");
    }

    public boolean isRecentlyAddedImage(String url) {
        String key = normalizeUrlKey(url);
        return key != null && !initialEditingImageUrlKeys.contains(key);
    }

    private String normalizeUrlKey(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        return url.trim().toLowerCase();
    }

    /**
     * Sauvegarde les modifications du référentiel.
     * Enregistre : code, label (selon langue choisie), description (selon langue choisie),
     * référence bibliographique ; ajoute l'utilisateur courant aux auteurs s'il n'y figure pas.
     */
    @Transactional
    public void saveModification() {

        Entity entityToUpdate = entityRepository.findById(applicationBean.getSelectedEntity().getId()).get();

        // Corpus externe (metadata)
        entityToUpdate.setMetadata(saveEntityMetadata(entityToUpdate));

        // Période
        if (periodeAutocompleteSelection != null && StringUtils.hasText(periodeAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso per = conceptToReferenceOpentheso(periodeAutocompleteSelection, ReferenceOpenthesoEnum.PERIODE.name(), entityToUpdate);
            entityToUpdate.setPeriode(referenceOpenthesoRepository.save(per));
        } else {
            entityToUpdate.setPeriode(null);
        }

        // Production
        if (productionAutocompleteSelection != null && StringUtils.hasText(productionAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso prod = conceptToReferenceOpentheso(productionAutocompleteSelection, ReferenceOpenthesoEnum.PRODUCTION.name(), entityToUpdate);
            entityToUpdate.setProduction(referenceOpenthesoRepository.save(prod));
        } else {
            entityToUpdate.setProduction(null);
        }

        // Aires de circulation (remplacer la liste)
        entityToUpdate.getAiresCirculation().clear();
        if (airesCirculation != null) {
            for (ReferenceOpentheso ref : airesCirculation) {
                ReferenceOpentheso r = ReferenceOpentheso.builder()
                        .valeur(ref.getValeur())
                        .conceptId(ref.getConceptId())
                        .url(ref.getUrl())
                        .code(ReferenceOpenthesoEnum.AIRE_CIRCULATION.name())
                        .entity(entityToUpdate)
                        .build();
                entityToUpdate.getAiresCirculation().add(referenceOpenthesoRepository.save(r));
            }
        }

        String newBib = (referenceBibliographiqueList != null && !referenceBibliographiqueList.isEmpty())
                ? referenceBibliographiqueList.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.joining("; "))
                : null;
        entityToUpdate.setReferenceBibliographique((newBib != null && newBib.isEmpty()) ? null : newBib);

        // DescriptionDetail
        entityToUpdate.setDescriptionDetail(saveDescriptionDetail(entityToUpdate));

        // CaracteristiquePhysique
        entityToUpdate.setCaracteristiquePhysique(saveCaracteristiquePhysique(entityToUpdate));

        // DescriptionPate
        entityToUpdate.setDescriptionPate(saveDescriptionPate(entityToUpdate));

        // Description - Monnaies
        entityToUpdate.setDescriptionMonnaie(saveDescriptionMonnaie(entityToUpdate));

        //caracteristiquePhysiqueMonnaie -------
        entityToUpdate.setCaracteristiquePhysiqueMonnaie(saveCaracteristiquePhysiqueMonnaie(entityToUpdate));

        // Mise à jour des labels (remplacer, pas ajouter)
        if (entityToUpdate.getLabels() == null) {
            entityToUpdate.setLabels(new ArrayList<>());
        }
        entityToUpdate.getLabels().clear();
        if (noms != null) {
            for (NameItem item : noms) {
                if (item.getLangue() != null && StringUtils.hasText(item.getNom())) {
                    Label lbl = new Label();
                    lbl.setNom(item.getNom().trim());
                    lbl.setLangue(item.getLangue());
                    lbl.setEntity(entityToUpdate);
                    entityToUpdate.getLabels().add(lbl);
                }
            }
        }

        if (categorieFonctionnelleAutocompleteSelection != null && StringUtils.hasText(categorieFonctionnelleAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso fonc = conceptToReferenceOpentheso(categorieFonctionnelleAutocompleteSelection, ReferenceOpenthesoEnum.CATEGORIE_FONCTIONNELLE.name(), entityToUpdate);
            entityToUpdate.setCategorieFonctionnelle(referenceOpenthesoRepository.save(fonc));
        } else {
            entityToUpdate.setCategorieFonctionnelle(null);
        }

        // Mise à jour des descriptions (remplacer, pas ajouter)
        if (entityToUpdate.getDescriptions() == null) {
            entityToUpdate.setDescriptions(new ArrayList<>());
        }
        entityToUpdate.getDescriptions().clear();
        if (descriptions != null) {
            for (DescriptionItem item : descriptions) {
                if (item.getLangue() != null && StringUtils.hasText(item.getValeur())) {
                    Description desc = new Description();
                    desc.setValeur(item.getValeur().trim());
                    desc.setLangue(item.getLangue());
                    desc.setEntity(entityToUpdate);
                    entityToUpdate.getDescriptions().add(desc);
                }
            }
        }

        // Ajouter l'utilisateur courant aux auteurs s'il n'y figure pas
        Utilisateur currentUser = loginBean != null ? loginBean.getCurrentUser() : null;
        if (currentUser != null && currentUser.getId() != null && utilisateurRepository != null) {
            Utilisateur managedUser = utilisateurRepository.findById(currentUser.getId()).orElse(null);
            if (managedUser != null) {
                List<Utilisateur> auteurs = entityToUpdate.getAuteurs();
                if (auteurs == null) {
                    entityToUpdate.setAuteurs(new ArrayList<>());
                    auteurs = entityToUpdate.getAuteurs();
                }
                boolean alreadyAuthor = auteurs.stream()
                        .anyMatch(u -> u.getId() != null && u.getId().equals(managedUser.getId()));
                if (!alreadyAuthor) {
                    auteurs.add(managedUser);
                }
            }
        }

        // Mise à jour des images (orphanRemoval : clear() déclenche la suppression des anciennes)
        if (entityToUpdate.getImages() != null) {
            entityToUpdate.getImages().clear();
        } else {
            entityToUpdate.setImages(new ArrayList<>());
        }
        if (editingImages != null) {
            for (EditingImageItem item : editingImages) {
                if (item != null && StringUtils.hasText(item.getUrl())) {
                    Image image = new Image();
                    image.setEntity(entityToUpdate);
                    image.setUrl(item.getUrl().trim());
                    image.setLegende(StringUtils.hasText(item.getLegende()) ? item.getLegende().trim() : null);
                    entityToUpdate.getImages().add(image);
                }
            }
        }

        Entity entitySaved = entityRepository.save(entityToUpdate);

        // Supprimer les fichiers physiques des images retirées par l'utilisateur
        if (entityImageService != null && removedImageUrlsToDeleteOnSave != null && !removedImageUrlsToDeleteOnSave.isEmpty()) {
            for (String url : removedImageUrlsToDeleteOnSave) {
                if (url != null && url.contains("/uploaded-images/")) {
                    entityImageService.deletePhysicalFileByUrl(url);
                }
            }
            removedImageUrlsToDeleteOnSave.clear();
        }

        if (entityToUpdate.getEntityType() != null && entityToUpdate.getEntityType().getId() != null) {
            long typeId = entityToUpdate.getEntityType().getId();
            if (typeId == 6) {
                collectionBean.saveCollectionGestionnaires(entitySaved);
            } else if (typeId == 1) {
                referenceBean.saveReferenceGestionnaires(entitySaved);
            } else if (typeId == 3) {
                saveUserPermissionsForGroup(entitySaved);
            }
        }

        applicationBean.setSelectedEntity(entitySaved);

        treeBean.updateEntityInTree(entitySaved);
        treeBean.expandPathAndSelectEntity(entitySaved);

        applicationBean.getBreadCrumbElements().set(applicationBean.getBreadCrumbElements().size() - 1, entitySaved);

        editingEntity = false;
        resetCategoryDialogForm();

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès", "Les modifications ont été enregistrées avec succès."));

        log.info("Groupe mis à jour avec succès: {}", applicationBean.getSelectedEntity().getCode());
    }

    private CaracteristiquePhysiqueMonnaie saveCaracteristiquePhysiqueMonnaie(Entity entityToUpdate) {
        CaracteristiquePhysiqueMonnaie caracteristiquePhysiqueMonnaie = entityToUpdate.getCaracteristiquePhysiqueMonnaie();
        if (caracteristiquePhysiqueMonnaie == null) {
            caracteristiquePhysiqueMonnaie = new CaracteristiquePhysiqueMonnaie();
            caracteristiquePhysiqueMonnaie.setEntity(entityToUpdate);
        }

        if (materiauxAutocompleteSelection != null && StringUtils.hasText(materiauxAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso m = conceptToReferenceOpentheso(materiauxAutocompleteSelection, ReferenceOpenthesoEnum.MATERIAUX.name(), entityToUpdate);
            caracteristiquePhysiqueMonnaie.setMateriaux(referenceOpenthesoRepository.save(m));
        } else {
            caracteristiquePhysiqueMonnaie.setMateriaux(null);
        }

        if (denominationAutocompleteSelection != null && StringUtils.hasText(denominationAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso m = conceptToReferenceOpentheso(denominationAutocompleteSelection, ReferenceOpenthesoEnum.DENOMINATION.name(), entityToUpdate);
            caracteristiquePhysiqueMonnaie.setDenomination(referenceOpenthesoRepository.save(m));
        } else {
            caracteristiquePhysiqueMonnaie.setDenomination(null);
        }

        caracteristiquePhysiqueMonnaie.setMetrologie(metrologie);

        if (valeurAutocompleteSelection != null && StringUtils.hasText(valeurAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso m = conceptToReferenceOpentheso(valeurAutocompleteSelection, ReferenceOpenthesoEnum.VALEUR.name(), entityToUpdate);
            caracteristiquePhysiqueMonnaie.setValeur(referenceOpenthesoRepository.save(m));
        } else {
            caracteristiquePhysiqueMonnaie.setValeur(null);
        }

        if (techniqueAutocompleteSelection != null && StringUtils.hasText(techniqueAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso m = conceptToReferenceOpentheso(techniqueAutocompleteSelection, ReferenceOpenthesoEnum.TECHNIQUE.name(), entityToUpdate);
            caracteristiquePhysiqueMonnaie.setTechnique(referenceOpenthesoRepository.save(m));
        } else {
            caracteristiquePhysiqueMonnaie.setTechnique(null);
        }

        if (fabricationAutocompleteSelection != null && StringUtils.hasText(fabricationAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso m = conceptToReferenceOpentheso(fabricationAutocompleteSelection, ReferenceOpenthesoEnum.FABRICATION.name(), entityToUpdate);
            caracteristiquePhysiqueMonnaie.setFabrication(referenceOpenthesoRepository.save(m));
        } else {
            caracteristiquePhysiqueMonnaie.setFabrication(null);
        }

        return caracteristiquePhysiqueMonnaieRepository.save(caracteristiquePhysiqueMonnaie);
    }

    private EntityMetadata saveEntityMetadata(Entity entityToUpdate) {

        EntityMetadata entityMetadata = entityToUpdate.getMetadata();
        if (entityMetadata == null) {
            entityMetadata = new EntityMetadata();
            entityMetadata.setEntity(entityToUpdate);
        }

        String newCode = code != null ? code.trim() : null;
        if (!Objects.equals(newCode, entityToUpdate.getCode()) && newCode != null && !newCode.isEmpty()) {
            entityMetadata.setCode(newCode);
        }

        entityMetadata.setCommentaireDatation(commentaireDatation);

        String newBibliographie = bibliographie != null ? bibliographie.trim() : null;
        if (!Objects.equals(newBibliographie, entityToUpdate.getBibliographie())) {
            entityMetadata.setBibliographie(newBibliographie);
        }

        entityMetadata.setAppellation(appellationUsuelle);

        entityMetadata.setAlignementExterne(alignementExterne != null && !alignementExterne.isBlank() ? alignementExterne.trim() : null);

        entityMetadata.setAppartient(appartient != null && !appartient.isBlank() ? appartient.trim() : null);

        entityMetadata.setAssocie(associe != null && !associe.isBlank() ? associe.trim() : null);

        entityMetadata.setTypologieScientifique(typologieScientifique);

        entityMetadata.setIdentifiantPerenne(identifiantPerenne);

        entityMetadata.setAncienneVersion(ancienneVersion);

        entityMetadata.setAppartient(appartient != null && !appartient.isBlank() ? appartient.trim() : null);
        entityMetadata.setAssocie(associe != null && !associe.isBlank() ? associe.trim() : null);

        entityMetadata.setTpq(tpq);

        entityMetadata.setTaq(taq);

        //relationExterne

        entityMetadata.setAteliers((ateliers != null && !ateliers.isEmpty())
                ? String.join("; ", ateliers.stream().filter(s -> s != null && !s.isBlank()).toList())
                : null);

        entityMetadata.setAttestations((attestations != null && !attestations.isEmpty())
                ? String.join("; ", attestations.stream().filter(s -> s != null && !s.isBlank()).toList())
                : null);

        entityMetadata.setSitesArcheologiques((sitesArcheologiques != null && !sitesArcheologiques.isEmpty())
                ? String.join("; ", sitesArcheologiques.stream().filter(s -> s != null && !s.isBlank()).toList())
                : null);

        entityMetadata.setReference((referentiels != null && !referentiels.isEmpty())
                ? String.join("; ", referentiels.stream().filter(s -> s != null && !s.isBlank()).toList())
                : null);

        entityMetadata.setCorpusExterne(corpusExterne != null && !corpusExterne.isBlank() ? corpusExterne.trim() : null);

        String newCommentaire = commentaire != null ? commentaire.trim() : null;
        entityMetadata.setCommentaire(newCommentaire);

        return entityMetadataRepository.save(entityMetadata);
    }

    private CaracteristiquePhysique saveCaracteristiquePhysique(Entity entityToUpdate) {

        CaracteristiquePhysique caracteristiquePhysique = entityToUpdate.getCaracteristiquePhysique();
        if (caracteristiquePhysique == null) {
            caracteristiquePhysique = new CaracteristiquePhysique();
            caracteristiquePhysique.setEntity(entityToUpdate);
        }

        if (metrologieAutocompleteSelection != null && StringUtils.hasText(metrologieAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso m = conceptToReferenceOpentheso(metrologieAutocompleteSelection, ReferenceOpenthesoEnum.METROLOGIE.name(), entityToUpdate);
            caracteristiquePhysique.setMetrologie(referenceOpenthesoRepository.save(m));
        } else {
            caracteristiquePhysique.setMetrologie(null);
        }

        if (materiauxAutocompleteSelection != null && StringUtils.hasText(materiauxAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso m = conceptToReferenceOpentheso(materiauxAutocompleteSelection, ReferenceOpenthesoEnum.MATERIAUX.name(), entityToUpdate);
            caracteristiquePhysique.setMateriaux(referenceOpenthesoRepository.save(m));
        } else {
            caracteristiquePhysique.setMateriaux(null);
        }

        if (formeAutocompleteSelection != null && StringUtils.hasText(formeAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso f = conceptToReferenceOpentheso(formeAutocompleteSelection, ReferenceOpenthesoEnum.FORME.name(), entityToUpdate);
            caracteristiquePhysique.setForme(referenceOpenthesoRepository.save(f));
        } else {
            caracteristiquePhysique.setForme(null);
        }

        if (dimensionsAutocompleteSelection != null && StringUtils.hasText(dimensionsAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso f = conceptToReferenceOpentheso(dimensionsAutocompleteSelection, ReferenceOpenthesoEnum.DIMENSIONS.name(), entityToUpdate);
            caracteristiquePhysique.setDimensions(referenceOpenthesoRepository.save(f));
        } else {
            caracteristiquePhysique.setDimensions(null);
        }

        if (techniqueAutocompleteSelection != null && StringUtils.hasText(techniqueAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso f = conceptToReferenceOpentheso(techniqueAutocompleteSelection, ReferenceOpenthesoEnum.TECHNIQUE.name(), entityToUpdate);
            caracteristiquePhysique.setTechnique(referenceOpenthesoRepository.save(f));
        } else {
            caracteristiquePhysique.setTechnique(null);
        }

        if (fabricationFaconnageAutocompleteSelection != null && StringUtils.hasText(fabricationFaconnageAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso f = conceptToReferenceOpentheso(fabricationFaconnageAutocompleteSelection, ReferenceOpenthesoEnum.FABRICATION_FACONNAGE.name(), entityToUpdate);
            caracteristiquePhysique.setFabrication(referenceOpenthesoRepository.save(f));
        } else {
            caracteristiquePhysique.setFabrication(null);
        }

        return caracteristiquePhysiqueRepository.save(caracteristiquePhysique);
    }

    private DescriptionPate saveDescriptionPate(Entity entityToUpdate) {

        DescriptionPate descriptionPate = entityToUpdate.getDescriptionPate();
        if ( descriptionPate== null) {
            descriptionPate = new DescriptionPate();
            descriptionPate.setEntity(entityToUpdate);
        }

        descriptionPate.setDescription(this.descriptionPate);

        if (couleurPateAutocompleteSelection != null && StringUtils.hasText(couleurPateAutocompleteSelection.getSelectedTerm())) {
            descriptionPate.setCouleur(referenceOpenthesoRepository.save(conceptToReferenceOpentheso(couleurPateAutocompleteSelection, ReferenceOpenthesoEnum.COULEUR_PATE.name(), entityToUpdate)));
        } else {
            descriptionPate.setCouleur(null);
        }
        if (naturePateAutocompleteSelection != null && StringUtils.hasText(naturePateAutocompleteSelection.getSelectedTerm())) {
            descriptionPate.setNature(referenceOpenthesoRepository.save(conceptToReferenceOpentheso(naturePateAutocompleteSelection, ReferenceOpenthesoEnum.NATURE_PATE.name(), entityToUpdate)));
        } else {
            descriptionPate.setNature(null);
        }
        if (inclusionsAutocompleteSelection != null && StringUtils.hasText(inclusionsAutocompleteSelection.getSelectedTerm())) {
            descriptionPate.setInclusion(referenceOpenthesoRepository.save(conceptToReferenceOpentheso(inclusionsAutocompleteSelection, ReferenceOpenthesoEnum.INCLUSIONS.name(), entityToUpdate)));
        } else {
            descriptionPate.setInclusion(null);
        }
        if (cuissonPostCuissonAutocompleteSelection != null && StringUtils.hasText(cuissonPostCuissonAutocompleteSelection.getSelectedTerm())) {
            descriptionPate.setCuisson(referenceOpenthesoRepository.save(conceptToReferenceOpentheso(cuissonPostCuissonAutocompleteSelection, ReferenceOpenthesoEnum.CUISSON_POST_CUISSON.name(), entityToUpdate)));
        } else {
            descriptionPate.setCuisson(null);
        }

        return descriptionPateRepository.save(descriptionPate);
    }

    private DescriptionDetail saveDescriptionDetail(Entity entityToUpdate) {

        DescriptionDetail descriptionDetail = entityToUpdate.getDescriptionDetail();
        if (descriptionDetail == null) {
            descriptionDetail = new DescriptionDetail();
            descriptionDetail.setEntity(entityToUpdate);
        }

        descriptionDetail.setDecors(decors != null && !decors.trim().isEmpty() ? decors.trim() : null);

        String marquesStr = (marquesEstampilles != null && !marquesEstampilles.isEmpty())
                ? String.join("; ", marquesEstampilles.stream().filter(s -> s != null && !s.isBlank()).toList())
                : null;
        descriptionDetail.setMarques(marquesStr);

        if (fonctionUsageAutocompleteSelection != null && StringUtils.hasText(fonctionUsageAutocompleteSelection.getSelectedTerm())) {
            ReferenceOpentheso fonc = conceptToReferenceOpentheso(fonctionUsageAutocompleteSelection, ReferenceOpenthesoEnum.FONCTION_USAGE.name(), entityToUpdate);
            descriptionDetail.setFonction(referenceOpenthesoRepository.save(fonc));
        } else {
            descriptionDetail.setFonction(null);
        }

        return descriptionDetailRepository.save(descriptionDetail);
    }

    private DescriptionMonnaie saveDescriptionMonnaie(Entity entityToUpdate) {
        DescriptionMonnaie dmToUpdate = entityToUpdate.getDescriptionMonnaie();
        if (dmToUpdate == null) {
            dmToUpdate = new DescriptionMonnaie();
        }
        dmToUpdate.setEntity(entityToUpdate);
        dmToUpdate.setDroit(droit);
        dmToUpdate.setLegendeDroit(legendeDroit);
        dmToUpdate.setCoinsMonetairesDroit(coinsMonetairesDroit);
        dmToUpdate.setRevers(revers);
        dmToUpdate.setLegendeRevers(legendeRevers);
        dmToUpdate.setCoinsMonetairesRevers(coinsMonetairesRevers);
        return descriptionMonnaieRepository.save(dmToUpdate);
    }

    /** Annule le mode édition et rafraîchit l'entité depuis la base. */
    public void cancelEditingGroupe() {
        editingEntity = false;
        resetCategoryDialogForm();
        if (applicationBean != null && applicationBean.getSelectedEntity() != null && applicationBean.getSelectedEntity().getId() != null) {
            applicationBean.setSelectedEntity(
                    entityRepository.findById(applicationBean.getSelectedEntity().getId())
                            .orElse(applicationBean.getSelectedEntity()));
        }
    }

    private void updateAvailableTmpLanguagesForDefinition() {
        if (availableLanguages == null) {
            availableLanguages = langueRepository.findAllByOrderByNomAsc();
        }
        availableTmpLanguagesForDefinition = availableLanguages.stream()
                .filter(langue ->  !isLangueAlreadyUsedInDescriptions(langue.getCode(), descriptions))
                .toList();
    }

    private void updateAvailableTmpLanguagesForLabel() {
        if (availableLanguages == null) {
            availableLanguages = langueRepository.findAllByOrderByNomAsc();
        }
        availableTmpLanguagesForLabel = availableLanguages.stream()
                .filter(langue -> !isLangueAlreadyUsedInLabels(langue.getCode(), noms))
                .collect(Collectors.toList());
    }

    public void addAireCirculationFromAutocomplete() {
        if (aireCirculationAutocompleteSelection == null || aireCirculationAutocompleteSelection.getSelectedTerm() == null) return;
        String valeur = aireCirculationAutocompleteSelection.getSelectedTerm().trim();
        if (valeur.isEmpty()) return;
        if (airesCirculation == null) airesCirculation = new ArrayList<>();
        // Ne pas autoriser les doublons (comparaison insensible à la casse)
        boolean dejaPresent = airesCirculation.stream()
                .anyMatch(ref -> ref.getValeur() != null && ref.getValeur().equalsIgnoreCase(valeur));
        if (dejaPresent) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Valeur déjà présente",
                            "« " + valeur + " » est déjà dans la liste des aires de circulation."));
            return;
        }
        airesCirculation.add(ReferenceOpentheso.builder()
                .valeur(valeur)
                .conceptId(aireCirculationAutocompleteSelection.getIdConcept())
                .url(aireCirculationAutocompleteSelection.getUri())
                .build());
        aireCirculationAutocompleteSelection = null;
    }

    public void deleteAireCirculation(String valeur) {
        if (airesCirculation == null) return;
        // Supprimer uniquement la première occurrence (liste mutable pour éviter UnsupportedOperationException)
        for (int i = 0; i < airesCirculation.size(); i++) {
            if (airesCirculation.get(i).getValeur() != null
                    && airesCirculation.get(i).getValeur().equalsIgnoreCase(valeur)) {
                airesCirculation.remove(i);
                break;
            }
        }
        PrimeFaces.current().ajax().update(":growl, :contentPanels");
    }

    /** Appelé quand l'utilisateur ajoute un atelier (compatibilité avec ancien flux). Les chips mettent à jour ateliers directement. */
    public void saveAteliers() {
        if (ateliersValue != null && !ateliersValue.isBlank()) {
            if (ateliers == null) ateliers = new ArrayList<>();
            ateliers.add(ateliersValue.trim());
            ateliersValue = "";
        }
    }

    public void saveReferences() {
        if (referentielValue != null && !referentielValue.isBlank()) {
            if (referentiels == null) referentiels = new ArrayList<>();
            referentiels.add(referentielValue.trim());
            referentielValue = "";
        }
    }

    public void saveAttestations() {
        if (attestationValue != null && !attestationValue.isBlank()) {
            if (attestations == null) attestations = new ArrayList<>();
            attestations.add(attestationValue.trim());
            attestationValue = "";
        }
    }

    public void saveSitesArcheologiques() {
        if (siteArcheologiqueValue != null && !siteArcheologiqueValue.isBlank()) {
            if (sitesArcheologiques == null) sitesArcheologiques = new ArrayList<>();
            sitesArcheologiques.add(siteArcheologiqueValue.trim());
            siteArcheologiqueValue = "";
        }
    }

    /** Les chips mettent à jour marquesEstampilles directement. No-op pour compatibilité. */
    public void saveMarquesEstampilles() {
        // no-op: p:chips met à jour marquesEstampilles directement
    }

    private static PactolsConcept refToConcept(ReferenceOpentheso ref) {
        return ref != null ? new PactolsConcept(ref.getConceptId(), ref.getUrl(), ref.getValeur()) : new PactolsConcept();
    }

    private void saveUserPermissionsForGroup(Entity savedGroup) {
        if (userPermissionRepository == null || savedGroup == null || savedGroup.getId() == null) return;
        userPermissionRepository.deleteByEntityIdAndRole(savedGroup.getId(), PermissionRoleEnum.REDACTEUR.getLabel());
        userPermissionRepository.deleteByEntityIdAndRole(savedGroup.getId(), PermissionRoleEnum.VALIDEUR.getLabel());
        userPermissionRepository.deleteByEntityIdAndRole(savedGroup.getId(), PermissionRoleEnum.RELECTEUR.getLabel());
        Set<Long> alreadyAssigned = new HashSet<>();
        List<?> redTarget = (redacteursPickList != null && redacteursPickList.getTarget() != null) ? redacteursPickList.getTarget() : List.of();
        for (Object raw : redTarget) {
            Utilisateur u = resolveUtilisateur(raw);
            if (u != null && u.getId() != null && alreadyAssigned.add(u.getId())) {
                saveUserPermission(savedGroup, u, PermissionRoleEnum.REDACTEUR.getLabel());
            }
        }
        List<?> valTarget = (validateursPickList != null && validateursPickList.getTarget() != null) ? validateursPickList.getTarget() : List.of();
        for (Object raw : valTarget) {
            Utilisateur u = resolveUtilisateur(raw);
            if (u != null && u.getId() != null && alreadyAssigned.add(u.getId())) {
                saveUserPermission(savedGroup, u, PermissionRoleEnum.VALIDEUR.getLabel());
            }
        }
        List<?> relTarget = (relecteursPickList != null && relecteursPickList.getTarget() != null) ? relecteursPickList.getTarget() : List.of();
        for (Object raw : relTarget) {
            Utilisateur u = resolveUtilisateur(raw);
            if (u != null && u.getId() != null && alreadyAssigned.add(u.getId())) {
                saveUserPermission(savedGroup, u, PermissionRoleEnum.RELECTEUR.getLabel());
            }
        }
    }

    private void saveUserPermission(Entity savedGroup, Utilisateur utilisateur, String role) {
        UserPermission.UserPermissionId id = new UserPermission.UserPermissionId();
        id.setUserId(utilisateur.getId());
        id.setEntityId(savedGroup.getId());
        if (!userPermissionRepository.existsById(id)) {
            UserPermission permission = new UserPermission();
            permission.setUtilisateur(utilisateur);
            permission.setEntity(savedGroup);
            permission.setId(id);
            permission.setRole(role);
            permission.setCreateDate(LocalDateTime.now());
            userPermissionRepository.save(permission);
        }
    }

    private Utilisateur resolveUtilisateur(Object value) {
        if (value == null || utilisateurRepository == null) return null;
        if (value instanceof Utilisateur u) return u;
        Long userId = null;
        if (value instanceof Long l) userId = l;
        else if (value instanceof Number n) userId = n.longValue();
        else if (value instanceof String s && !s.isBlank()) {
            try { userId = Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
        }
        return userId != null ? utilisateurRepository.findById(userId).orElse(null) : null;
    }

    private ReferenceOpentheso conceptToReferenceOpentheso(PactolsConcept concept, String code, Entity entity) {
        String valeur = concept.getSelectedTerm() != null ? concept.getSelectedTerm().trim() : concept.getIdConcept();
        if (valeur == null || valeur.isEmpty()) return null;
        return ReferenceOpentheso.builder()
                .code(code)
                .valeur(valeur)
                .conceptId(concept.getIdConcept())
                .url(concept.getUri())
                .entity(entity)
                .build();
    }

    public void onLangChange() {
        log.info(">> " + newLabelLangueCode);
    }


    /**
     * Initialise le PickList des gestionnaires pour l'édition d'une collection existante.
     * Les gestionnaires actuels sont placés dans la liste cible (à droite).
     */
    private void initGestionnairesPickListForEdit(Long entityId) {
        List<Long> sourceIds = getGestionnairesList().stream()
                .map(Utilisateur::getId)
                .filter(Objects::nonNull)
                .toList();
        List<Long> targetIds = (entityId != null && userPermissionRepository != null)
                ? userPermissionRepository.findUserIdsByEntityIdAndRole(entityId, PermissionRoleEnum.GESTIONNAIRE_COLLECTION.getLabel())
                : new ArrayList<>();
        List<Long> sourceFiltered = sourceIds.stream().filter(id -> !targetIds.contains(id)).toList();
        gestionnairesPickList = new DualListModel<>(new ArrayList<>(sourceFiltered), new ArrayList<>(targetIds));
    }


    /** Liste des utilisateurs éligibles comme gestionnaires (groupe Utilisateur) */
    public List<Utilisateur> getGestionnairesList() {
        if (utilisateurRepository == null) return new ArrayList<>();
        List<Utilisateur> list = utilisateurRepository.findByGroupeNom(GroupEnum.UTILISATEUR.getLabel());
        return list != null ? list : new ArrayList<>();
    }
}
