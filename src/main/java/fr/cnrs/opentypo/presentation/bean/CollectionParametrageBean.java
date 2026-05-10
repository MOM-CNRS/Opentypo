package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.service.PactolsService;
import fr.cnrs.opentypo.application.dto.pactols.PactolsCollection;
import fr.cnrs.opentypo.application.dto.pactols.PactolsLangue;
import fr.cnrs.opentypo.application.dto.pactols.PactolsThesaurus;
import fr.cnrs.opentypo.application.dto.zotero.ZoteroCollectionOption;
import fr.cnrs.opentypo.application.service.ZoteroApiService;
import fr.cnrs.opentypo.infrastructure.config.OpentypoArkProperties;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Parametrage;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ParametrageRepository;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Bean pour le paramétrage OpenTheso d'une collection (entité).
 * Gère l'ouverture du dialog, le chargement des listes thésaurus/langues/collections
 * et la sauvegarde (création ou mise à jour) du paramétrage.
 */
@Named("collectionParametrageBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class CollectionParametrageBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private PactolsService pactolsService;

    @Inject
    private SearchBean searchBean;

    @Inject
    private ParametrageRepository parametrageRepository;

    @Inject
    private EntityRepository entityRepository;

    @Autowired
    private EntityRelationRepository entityRelationRepository;

    @Inject
    private ZoteroApiService zoteroApiService;

    @Autowired
    private OpentypoArkProperties opentypoArkProperties;

    /** Entité (collection) dont on édite le paramétrage. */
    private Entity currentCollectionEntity;

    /** Liste des thésaurus disponibles. */
    private List<PactolsThesaurus> availableThesaurus = new ArrayList<>();

    /** Liste des langues du thésaurus sélectionné. */
    private List<PactolsLangue> availableLanguages = new ArrayList<>();

    /** Liste des collections du thésaurus sélectionné. */
    private List<PactolsCollection> availableCollections = new ArrayList<>();

    private String selectedThesaurusId;
    private String selectedLanguageId;
    private String selectedCollectionId;

    /** URL du serveur OpenTheso (base URL) vers laquelle le système va chercher. */
    private String baseUrl;
    /** URL du groupe Zotero (étape 1). */
    private String bibliographieGroupUrl;
    /** Group ID Zotero résolu depuis l'URL. */
    private Long bibliographieGroupId;
    /** Collections Zotero disponibles pour ce groupe (étape 2). */
    private List<ZoteroCollectionOption> availableBibliographieCollections = new ArrayList<>();
    /** Clé collection Zotero choisie (étape 3). */
    private String selectedBibliographieCollectionKey;

    /** ARK : NAAN du référentiel (prioritaire sur {@code opentypo.ark.naan} pour les typologies de l'arbre). */
    private String arkNaanEdit;
    /** ARK : épaule / préfixe local (prioritaire sur {@code opentypo.ark.shoulder} si renseigné). */
    private String arkShoulderEdit;
    /** ARK : URL de base du résolveur (ex. https://n2t.net) pour liens depuis les fiches typologie. */
    private String arkResolverBaseEdit;

    /** Valeur NAAN fallback (configuration application), pour aide dans le dialog. */
    public String getFallbackArkNaanHint() {
        if (opentypoArkProperties == null || !StringUtils.hasText(opentypoArkProperties.getNaan())) {
            return "—";
        }
        return opentypoArkProperties.getNaan().trim();
    }

    /** Valeur épaule fallback (configuration application), pour aide dans le dialog. */
    public String getFallbackArkShoulderHint() {
        if (opentypoArkProperties == null || !StringUtils.hasText(opentypoArkProperties.getShoulder())) {
            return "—";
        }
        return opentypoArkProperties.getShoulder().trim();
    }

    /**
     * Prépare l'ouverture du dialog de paramétrage pour la collection donnée.
     * Charge les thésaurus, et si un paramétrage existe déjà pour cette entité,
     * charge langues/collections et préremplit les champs.
     */
    public void prepareAndShowParametrageDialog(Entity collectionEntity) {
        if (collectionEntity == null) {
            log.warn("prepareAndShowParametrageDialog: collectionEntity is null");
            return;
        }
        this.currentCollectionEntity = collectionEntity;

        availableCollections = new ArrayList<>();
        availableLanguages = new ArrayList<>();
        selectedThesaurusId = null;
        selectedLanguageId = null;
        selectedCollectionId = null;
        bibliographieGroupUrl = null;
        bibliographieGroupId = null;
        selectedBibliographieCollectionKey = null;
        availableBibliographieCollections = new ArrayList<>();

        String selectedLangue = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";

        Optional<Parametrage> existing = parametrageRepository.findByEntityId(collectionEntity.getId());
        if (existing.isPresent()) {
            Parametrage p = existing.get();
            baseUrl = (p.getBaseUrl() != null && !p.getBaseUrl().isBlank()) ? p.getBaseUrl() : PactolsService.PACTOLS_BASE_URL;
            if (baseUrl != null && !baseUrl.trim().isEmpty()) {
                availableThesaurus = pactolsService.getThesaurusList(selectedLangue);
            }

            selectedThesaurusId = p.getIdTheso();
            selectedLanguageId = p.getIdLangue();
            selectedCollectionId = p.getIdGroupe();
            initBibliographieFromSavedUrl(p.getBibliographieUrl());

            if (selectedThesaurusId != null && !selectedThesaurusId.trim().isEmpty()) {
                availableLanguages = pactolsService.getThesaurusLanguages(selectedThesaurusId);
                availableCollections = pactolsService.getThesaurusCollections(selectedThesaurusId, selectedLangue);
            }
        }
    }

    /**
     * Actualise la liste des thésaurus (appelé par le bouton à côté de l'URL serveur).
     * Utilise l'URL par défaut du service ; si baseUrl du formulaire est renseignée, elle pourrait être utilisée ultérieurement.
     */
    public void loadThesaurusFromUrl() {
        String selectedLangue = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";

        availableThesaurus = pactolsService.getThesaurusList(selectedLangue);
        availableLanguages = new ArrayList<>();
        availableCollections = new ArrayList<>();
        selectedThesaurusId = null;
        selectedLanguageId = null;
        selectedCollectionId = null;
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Liste actualisée", "Les thésaurus ont été rechargés."));
        PrimeFaces.current().ajax().update(":collectionParametrageForm :growl");
    }

    /**
     * Appelé lors du changement de thésaurus : charge langues et collections.
     */
    public void onThesaurusSearch() {
        if (selectedThesaurusId == null || selectedThesaurusId.trim().isEmpty()) {
            availableLanguages = new ArrayList<>();
            availableCollections = new ArrayList<>();
            selectedLanguageId = null;
            selectedCollectionId = null;
            PrimeFaces.current().ajax().update(":collectionParametrageForm :growl");
            return;
        }
        String selectedLangue = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        availableLanguages = pactolsService.getThesaurusLanguages(selectedThesaurusId);
        if (!availableLanguages.isEmpty()) {
            var def = availableLanguages.stream()
                .filter(l -> selectedLangue.equals(l.getIdLang()))
                .findFirst();
            if (def.isEmpty()) {
                def = availableLanguages.stream().filter(l -> "fr".equals(l.getIdLang())).findFirst();
            }
            def.ifPresent(l -> selectedLanguageId = l.getIdLang());
        }
        availableCollections = pactolsService.getThesaurusCollections(selectedThesaurusId, selectedLangue);
        selectedCollectionId = null;
        PrimeFaces.current().ajax().update(":collectionParametrageForm :growl");
    }

    /**
     * Enregistre le paramétrage : crée une nouvelle entrée ou met à jour celle existante.
     */
    public void saveParametrage() {
        if (currentCollectionEntity == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Aucune collection sélectionnée."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        if (selectedThesaurusId == null || selectedThesaurusId.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner un thésaurus."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        if (selectedLanguageId == null || selectedLanguageId.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner une langue."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        if (selectedCollectionId == null || selectedCollectionId.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner une collection."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez saisir l'URL du serveur."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        String urlToSave = baseUrl.trim();
        Optional<Parametrage> existingOpt = parametrageRepository.findByEntityId(currentCollectionEntity.getId());
        Parametrage p;
        if (existingOpt.isPresent()) {
            p = existingOpt.get();
        } else {
            p = new Parametrage();
            Entity entity = entityRepository.findById(currentCollectionEntity.getId()).orElse(currentCollectionEntity);
            p.setEntity(entity);
        }
        p.setBaseUrl(urlToSave);
        p.setIdTheso(selectedThesaurusId);
        p.setIdLangue(selectedLanguageId);
        p.setIdGroupe(selectedCollectionId);
        parametrageRepository.save(p);

        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Paramétrage enregistré."));
        PrimeFaces.current().executeScript("PF('collectionParametrageDialog').hide();");
        PrimeFaces.current().ajax().update(":growl");
    }

    /**
     * Ouvre le dialog bibliographie Zotero pour un référentiel ou un groupe.
     * Référentiel : formulaire vide (pas de préremplissage).
     * Groupe : préremplissage depuis le paramétrage enregistré pour permettre la modification.
     */
    public void prepareAndShowBibliographieParametrageDialog(Entity collectionEntity) {
        if (collectionEntity == null) {
            log.warn("prepareAndShowBibliographieParametrageDialog: collectionEntity is null");
            return;
        }
        if (collectionEntity.getId() == null) {
            log.warn("prepareAndShowBibliographieParametrageDialog: collectionEntity id is null");
            return;
        }
        List<Entity> loaded = entityRepository.findByIdInWithEntityType(Collections.singletonList(collectionEntity.getId()));
        Entity resolved = loaded.isEmpty() ? collectionEntity : loaded.get(0);
        this.currentCollectionEntity = resolved;

        if (resolved.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_GROUP.equals(resolved.getEntityType().getCode())) {
            String savedUrl = parametrageRepository.findByEntityId(resolved.getId())
                    .map(Parametrage::getBibliographieUrl)
                    .orElse(null);
            initBibliographieFromSavedUrl(savedUrl);
            return;
        }

        bibliographieGroupUrl = null;
        bibliographieGroupId = null;
        selectedBibliographieCollectionKey = null;
        availableBibliographieCollections = new ArrayList<>();
    }

    /** Étape 2 : charger les collections Zotero d'un groupe à partir de l'URL saisie. */
    public void loadBibliographieCollectionsFromUrl() {
        availableBibliographieCollections = new ArrayList<>();
        selectedBibliographieCollectionKey = null;
        bibliographieGroupId = null;

        Optional<Long> groupIdOpt = zoteroApiService != null
                ? zoteroApiService.parseGroupIdFromGroupUrl(bibliographieGroupUrl)
                : Optional.empty();
        if (groupIdOpt.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Bibliographie",
                            "URL du groupe Zotero invalide. Format attendu : https://www.zotero.org/groups/{id}/..."));
            PrimeFaces.current().ajax().update(":collectionBibliographieParametrageForm :growl");
            return;
        }
        bibliographieGroupId = groupIdOpt.get();
        availableBibliographieCollections = zoteroApiService.listTopCollections(bibliographieGroupId, 100);
        if (availableBibliographieCollections.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Bibliographie",
                            "Aucune collection Zotero trouvée pour ce groupe."));
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Bibliographie",
                            availableBibliographieCollections.size() + " collection(s) Zotero trouvée(s)."));
        }
        PrimeFaces.current().ajax().update(":collectionBibliographieParametrageForm :growl");
    }

    /** Étape 4 : sauvegarde du choix de collection Zotero dans parametrage.bibliographie_url. */
    public void saveBibliographieParametrage() {
        if (currentCollectionEntity == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Aucune collection sélectionnée."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        if (bibliographieGroupId == null || !StringUtils.hasText(selectedBibliographieCollectionKey)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Bibliographie",
                            "Veuillez d'abord saisir l'URL du groupe puis sélectionner une collection Zotero."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        Optional<Parametrage> existingOpt = parametrageRepository.findByEntityId(currentCollectionEntity.getId());
        Parametrage p = existingOpt.orElseGet(() -> {
            Parametrage n = new Parametrage();
            Entity entity = entityRepository.findById(currentCollectionEntity.getId()).orElse(currentCollectionEntity);
            n.setEntity(entity);
            return n;
        });
        String bibUrl = resolveSelectedBibliographieUrl();
        p.setBibliographieUrl(bibUrl);
        parametrageRepository.save(p);

        Entity referenceForPropagation = entityRepository.findById(currentCollectionEntity.getId())
                .orElse(currentCollectionEntity);
        int propagated = propagateBibliographieToGroupsWithoutConfig(referenceForPropagation, bibUrl);

        String detail = propagated > 0
                ? "Paramétrage bibliographie enregistré. " + propagated + " groupe(s) sans configuration bibliographique ont reçu cette URL."
                : "Paramétrage bibliographie enregistré.";
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", detail));
        PrimeFaces.current().executeScript("PF('collectionBibliographieParametrageDialog').hide();");
        PrimeFaces.current().ajax().update(":growl");
    }

    /**
     * Dialogue paramétrage ARK : réservé à une entité de type référentiel.
     */
    public void prepareAndShowArkParametrageDialog(Entity referentielEntity) {
        arkNaanEdit = null;
        arkShoulderEdit = null;
        arkResolverBaseEdit = null;

        if (referentielEntity == null || referentielEntity.getId() == null) {
            log.warn("prepareAndShowArkParametrageDialog: entité ou id null");
            return;
        }

        List<Entity> loaded = entityRepository.findByIdInWithEntityType(Collections.singletonList(referentielEntity.getId()));
        Entity resolved = loaded.isEmpty() ? referentielEntity : loaded.get(0);

        if (resolved.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_REFERENCE.equals(resolved.getEntityType().getCode())) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "ARK",
                            "Le paramétrage ARK du serveur ne s'applique qu'à un référentiel."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        this.currentCollectionEntity = resolved;
        parametrageRepository.findByEntityId(resolved.getId()).ifPresent(p -> {
            arkNaanEdit = p.getArkNaan();
            arkShoulderEdit = p.getArkShoulder();
            arkResolverBaseEdit = p.getArkResolverBase();
        });
    }

    /** Persiste NAAN / épaule / résolveur ARK pour le référentiel courant. */
    public void saveArkParametrage() {
        if (currentCollectionEntity == null || currentCollectionEntity.getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Aucun référentiel sélectionné."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }
        if (currentCollectionEntity.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_REFERENCE.equals(currentCollectionEntity.getEntityType().getCode())) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                            "Le paramétrage ARK ne peut être enregistré que pour un référentiel."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        String naanTrim = trimToNull(arkNaanEdit);
        if (naanTrim != null && !Pattern.compile("\\d{5,}").matcher(naanTrim).matches()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "ARK",
                            "Le NAAN doit être numérique (au moins 5 chiffres), ou laisser vide pour utiliser la valeur de l'application."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        String shoulderTrim = trimToNull(arkShoulderEdit);
        String resolverTrim = trimToNull(arkResolverBaseEdit);

        Optional<Parametrage> existingOpt = parametrageRepository.findByEntityId(currentCollectionEntity.getId());
        Parametrage p = existingOpt.orElseGet(() -> {
            Parametrage n = new Parametrage();
            Entity entity = entityRepository.findById(currentCollectionEntity.getId()).orElse(currentCollectionEntity);
            n.setEntity(entity);
            return n;
        });
        p.setArkNaan(naanTrim);
        p.setArkShoulder(shoulderTrim);
        p.setArkResolverBase(resolverTrim);
        parametrageRepository.save(p);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Paramétrage ARK enregistré."));
        PrimeFaces.current().executeScript("PF('collectionArkParametrageDialog').hide();");
        PrimeFaces.current().ajax().update(":growl");
    }

    /**
     * Lorsque le paramétrage est enregistré sur un référentiel, recopie l’URL bibliographie Zotero
     * vers chaque groupe du sous-arbre n’ayant pas encore d’URL bibliographique ({@code bibliographie_url} vide ou absente).
     */
    private int propagateBibliographieToGroupsWithoutConfig(Entity referenceEntity, String bibliographieUrl) {
        if (referenceEntity == null
                || referenceEntity.getId() == null
                || !StringUtils.hasText(bibliographieUrl)
                || entityRelationRepository == null) {
            return 0;
        }
        if (referenceEntity.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_REFERENCE.equals(referenceEntity.getEntityType().getCode())) {
            return 0;
        }
        List<Object[]> rels = entityRelationRepository.findAllDescendantRelations(referenceEntity.getId());
        if (rels == null || rels.isEmpty()) {
            return 0;
        }
        Set<Long> descendantIds = new HashSet<>();
        for (Object[] row : rels) {
            if (row != null && row.length > 1 && row[1] != null) {
                descendantIds.add(((Number) row[1]).longValue());
            }
        }
        if (descendantIds.isEmpty()) {
            return 0;
        }
        List<Entity> entities = entityRepository.findByIdInWithEntityType(descendantIds);
        int updated = 0;
        for (Entity candidate : entities) {
            if (candidate == null
                    || candidate.getEntityType() == null
                    || !EntityConstants.ENTITY_TYPE_GROUP.equals(candidate.getEntityType().getCode())) {
                continue;
            }
            Optional<Parametrage> opt = parametrageRepository.findByEntityId(candidate.getId());
            if (opt.isEmpty()) {
                Parametrage np = new Parametrage();
                np.setEntity(entityRepository.findById(candidate.getId()).orElse(candidate));
                np.setBibliographieUrl(bibliographieUrl);
                parametrageRepository.save(np);
                updated++;
            } else {
                Parametrage gp = opt.get();
                if (!StringUtils.hasText(gp.getBibliographieUrl())) {
                    gp.setBibliographieUrl(bibliographieUrl);
                    parametrageRepository.save(gp);
                    updated++;
                }
            }
        }
        if (updated > 0) {
            log.info("{} groupe(s) ont hérité de l'URL bibliographie du référentiel id={}", updated, referenceEntity.getId());
        }
        return updated;
    }

    private void initBibliographieFromSavedUrl(String savedUrl) {
        String url = trimToNull(savedUrl);
        bibliographieGroupUrl = null;
        bibliographieGroupId = null;
        selectedBibliographieCollectionKey = null;
        availableBibliographieCollections = new ArrayList<>();
        if (url == null || zoteroApiService == null) {
            return;
        }
        Optional<ZoteroApiService.ZoteroScope> scopeOpt = zoteroApiService.parseScopeFromCollectionUrl(url);
        if (scopeOpt.isEmpty()) {
            return;
        }
        ZoteroApiService.ZoteroScope scope = scopeOpt.get();
        bibliographieGroupId = scope.groupId();
        bibliographieGroupUrl = "https://www.zotero.org/groups/" + scope.groupId();
        selectedBibliographieCollectionKey = scope.collectionKey();
        availableBibliographieCollections = zoteroApiService.listTopCollections(scope.groupId(), 100);
    }

    private String resolveSelectedBibliographieUrl() {
        if (zoteroApiService == null || bibliographieGroupId == null || !StringUtils.hasText(selectedBibliographieCollectionKey)) {
            return null;
        }
        return zoteroApiService.buildCollectionWebUrl(bibliographieGroupId, selectedBibliographieCollectionKey);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
