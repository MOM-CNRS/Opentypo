package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.application.dto.pactols.*;
import fr.cnrs.opentypo.application.service.PactolsService;
import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysique;
import fr.cnrs.opentypo.domain.entity.DescriptionPate;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.CandidatBean;
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
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bean pour gérer la boîte de dialogue de recherche PACTOLS
 * Réutilisable et dynamique pour différents champs
 */
@Named("openThesoDialogBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class OpenThesoDialogBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private PactolsService pactolsService;

    @Inject
    private SearchBean searchBean;

    @Inject
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    @Inject
    private EntityRepository entityRepository;

    // État de la boîte de dialogue
    private String dialogWidgetVar;
    private String targetFieldId; // ID du champ cible où insérer la valeur
    private Consumer<String> onValidateCallback; // Callback appelé lors de la validation
    private ReferenceOpenthesoEnum referenceCode; // Code pour la référence (PRODUCTION, PERIODE, etc.)
    private Long entityId; // ID de l'entité à mettre à jour
    private ReferenceOpentheso createdReference; // Référence créée lors de la validation

    // Données du formulaire
    private List<PactolsThesaurus> availableThesaurus = new ArrayList<>();
    private List<PactolsCollection> availableCollections = new ArrayList<>();
    private List<PactolsLangue> availableLanguages = new ArrayList<>();
    private String selectedLanguageId;
    private String selectedCollectionId;
    private String selectedThesaurusId;
    private String searchValue = "";
    private List<PactolsConcept> searchResults = new ArrayList<>();
    private PactolsConcept selectedConcept;

    private CandidatBean candidatBean;

    @PostConstruct
    public void init() {
        String selectedLangue = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        availableThesaurus = pactolsService.getThesaurusList(selectedLangue);
    }

    /**
     * Charge les thésaurus disponibles (appelé avant l'ouverture de la boîte de dialogue)
     * @param code Code de la référence à créer (PRODUCTION, PERIODE, etc.)
     * @param entityId ID de l'entité à mettre à jour
     */
    public void loadThesaurus(CandidatBean candidatBean, String code, Long entityId) {

        this.candidatBean = candidatBean;
        this.referenceCode = ReferenceOpenthesoEnum.fromString(code);
        this.entityId = entityId;
        
        // Recharger les thésaurus avec la langue sélectionnée
        String selectedLangue = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        availableThesaurus = pactolsService.getThesaurusList(selectedLangue);
        
        // Réinitialiser l'état
        resetDialog();
    }

    /**
     * Charge les thésaurus disponibles (appelé avant l'ouverture de la boîte de dialogue)
     * @param code Code de la référence à créer (PRODUCTION, PERIODE, etc.)
     */
    public void loadThesaurus(String code) {
        loadThesaurus(candidatBean, code, null);
    }

    /**
     * Charge les thésaurus disponibles (appelé avant l'ouverture de la boîte de dialogue)
     * Utilise "PRODUCTION" par défaut
     */
    public void loadThesaurus() {
        loadThesaurus(candidatBean, ReferenceOpenthesoEnum.PRODUCTION.name(), null);
    }

    /**
     * Initialise l'interface du dialog à l'ouverture
     */
    public void initializeDialog() {
        log.info("=== initializeDialog() appelée ===");
        
        // Recharger les thésaurus avec la langue sélectionnée
        String selectedLangue = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        availableThesaurus = pactolsService.getThesaurusList(selectedLangue);
        
        // Réinitialiser tous les champs
        resetDialog();
        
        // Mettre à jour l'interface
        PrimeFaces.current().ajax().update(":createCandidatForm:openThesoForm2");
    }

    /**
     * Ouvre la boîte de dialogue (les thésaurus doivent être déjà chargés)
     * @param widgetVar Widget var de la boîte de dialogue
     * @param fieldId ID du champ cible
     * @param callback Callback appelé lors de la validation
     */
    public void openDialog(String widgetVar, String fieldId, Consumer<String> callback) {
        this.dialogWidgetVar = widgetVar;
        this.targetFieldId = fieldId;
        this.onValidateCallback = callback;

        // Ouvrir la boîte de dialogue (les thésaurus sont déjà chargés)
        PrimeFaces.current().executeScript("setTimeout(function() { PF('" + widgetVar + "').show(); }, 100);");
    }

    /**
     * Réinitialise l'état de la boîte de dialogue
     */
    private void resetDialog() {
        selectedThesaurusId = null;
        availableCollections = new ArrayList<>();
        selectedCollectionId = null;
        availableLanguages = new ArrayList<>();
        selectedLanguageId = null;
        searchValue = "";
        searchResults = new ArrayList<>();
        selectedConcept = null;
        createdReference = null;
        // Note: referenceCode et entityId ne sont pas réinitialisés car ils doivent persister
    }

    /**
     * Recherche les collections et langues lorsqu'un thésaurus est sélectionné
     */
    public void onThesaurusSearch() {
        log.info("=== onThesaurusSearch() appelée ===");
        log.info("selectedThesaurusId: {}", selectedThesaurusId);
        
        if (selectedThesaurusId == null || selectedThesaurusId.trim().isEmpty()) {
            log.warn("Aucun thésaurus sélectionné (ID vide)");
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner un thésaurus."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        String selectedLangue = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";

        // Charger les langues disponibles
        availableLanguages = pactolsService.getThesaurusLanguages(selectedThesaurusId);
        
        // Sélectionner la langue par défaut si disponible
        if (!availableLanguages.isEmpty()) {
            var langue = availableLanguages.stream()
                .filter(l -> selectedLangue.equals(l.getIdLang()))
                .findFirst();
            langue.ifPresent(pactolsLangue -> selectedLanguageId = pactolsLangue.getIdLang());
        }

        // Charger les collections
        availableCollections = pactolsService.getThesaurusCollections(selectedThesaurusId, selectedLangue);

        // Réinitialiser les sélections pour les nouvelles listes
        selectedCollectionId = null;
    }

    /**
     * Recherche les concepts
     */
    public void onConceptSearch() {
        if (selectedThesaurusId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner un thésaurus."));
            return;
        }

        if (selectedCollectionId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner une collection."));
            return;
        }

        if (selectedLanguageId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner une langue."));
            return;
        }

        if (searchValue == null || searchValue.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez saisir un terme de recherche."));
            return;
        }
        
        searchResults = pactolsService.searchConcepts(selectedThesaurusId, searchValue.trim(), selectedLanguageId, selectedCollectionId);

        PrimeFaces.current().ajax().update(":openThesoForm:conceptsTable :growl");
    }

    /**
     * Valide la sélection et crée une instance de ReferenceOpentheso
     */
    public void validateSelection() {
        if (selectedConcept == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner un concept."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        if (selectedThesaurusId == null || selectedThesaurusId.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Thésaurus non sélectionné."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        if (referenceCode == null) {
            log.error("referenceCode est null, impossible de créer la référence");
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Code de référence non défini."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        if (entityId == null) {
            log.error("entityId est null, impossible de lier la référence à une entité");
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "ID d'entité non défini."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        String selectedTerm = selectedConcept.getSelectedTerm();
        if (selectedTerm == null || selectedTerm.trim().isEmpty()) {
            selectedTerm = selectedConcept.getIdConcept();
        }

        // Créer l'instance de ReferenceOpentheso avec toutes les informations
        String conceptId = selectedConcept.getIdConcept();
        String thesaurusId = selectedThesaurusId;
        String collectionId = selectedCollectionId != null ? selectedCollectionId : "";

        // Construire l'URL au format base_URL/?idc={id_concept}&idt={id_thesaurus}
        String baseUrl = "https://pactols.frantiq.fr";
        String url = baseUrl + "/?idc=" + conceptId + "&idt=" + thesaurusId;

        // Créer la nouvelle entrée ReferenceOpentheso
        ReferenceOpentheso referenceOpentheso = new ReferenceOpentheso();
        referenceOpentheso.setCode(referenceCode.name());
        referenceOpentheso.setValeur(selectedTerm);
        referenceOpentheso.setConceptId(conceptId);
        referenceOpentheso.setThesaurusId(thesaurusId);
        referenceOpentheso.setCollectionId(collectionId);
        referenceOpentheso.setUrl(url);

        // Lier la référence à l'entité
        Entity entity = entityRepository.findById(entityId).orElse(null);

        // Pour les codes qui utilisent une seule référence (PRODUCTION, PERIODE, etc.)
        // on met à jour directement la colonne dans Entity
        switch (referenceCode) {
            case ReferenceOpenthesoEnum.PRODUCTION:
                referenceOpentheso.setEntity(entity);
                // Sauvegarder la référence d'abord
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                entity.setProduction(createdReference);
                entityRepository.save(entity);
                candidatBean.updateProductionFromOpenTheso();
                break;
            case ReferenceOpenthesoEnum.PERIODE:
                referenceOpentheso.setEntity(entity);
                // Sauvegarder la référence d'abord
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                entity.setPeriode(createdReference);
                entityRepository.save(entity);
                break;
            case ReferenceOpenthesoEnum.CATEGORIE,
                 ReferenceOpenthesoEnum.CATEGORIE_FONCTIONNELLE:
                referenceOpentheso.setEntity(entity);
                // Sauvegarder la référence d'abord
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                entity.setCategorieFonctionnelle(createdReference);
                entityRepository.save(entity);
                break;
            case ReferenceOpenthesoEnum.AIRE_CIRCULATION:
                referenceOpentheso.setEntity(entity);
                if (CollectionUtils.isEmpty(entity.getAiresCirculation())) {
                    entity.setAiresCirculation(new ArrayList<>());
                }
                entity.getAiresCirculation().add(referenceOpentheso);
                // Sauvegarder l'entité (cascade ALL sauvegardera aussi la référence)
                Entity savedEntity = entityRepository.save(entity);
                // Récupérer la référence sauvegardée depuis l'entité
                if (savedEntity.getAiresCirculation() != null && !savedEntity.getAiresCirculation().isEmpty()) {
                    createdReference = savedEntity.getAiresCirculation().stream()
                            .filter(ref -> ref.getConceptId() != null && ref.getConceptId().equals(conceptId))
                            .findFirst()
                            .orElse(referenceOpentheso);
                } else {
                    createdReference = referenceOpentheso;
                }
                candidatBean.updateAireCirculationFromOpenTheso();
                log.info("Référence AIRE_CIRCULATION sauvegardée avec ID: {}", createdReference.getId());
                break;
            case ReferenceOpenthesoEnum.FONCTION_USAGE:
                referenceOpentheso.setEntity(entity);
                // Sauvegarder la référence d'abord
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                if (entity.getDescriptionDetail() == null) {
                    entity.setDescriptionDetail(new DescriptionDetail());
                    entity.getDescriptionDetail().setEntity(entity);
                }
                entity.getDescriptionDetail().setFonction(createdReference);
                entityRepository.save(entity);
                candidatBean.updateFonctionUsageFromOpenTheso();
                break;
            case ReferenceOpenthesoEnum.METROLOGIE:
                referenceOpentheso.setEntity(entity);
                // Sauvegarder la référence d'abord
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                if (entity.getCaracteristiquePhysique() == null) {
                    entity.setCaracteristiquePhysique(new CaracteristiquePhysique());
                    entity.getCaracteristiquePhysique().setEntity(entity);
                }
                entity.getCaracteristiquePhysique().setMetrologie(createdReference);
                entityRepository.save(entity);
                candidatBean.updateMetrologieFromOpenTheso();
                break;
            case ReferenceOpenthesoEnum.FABRICATION_FACONNAGE:
                referenceOpentheso.setEntity(entity);
                // Sauvegarder la référence d'abord
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                if (entity.getCaracteristiquePhysique() == null) {
                    entity.setCaracteristiquePhysique(new CaracteristiquePhysique());
                    entity.getCaracteristiquePhysique().setEntity(entity);
                }
                entity.getCaracteristiquePhysique().setFabrication(createdReference);
                entityRepository.save(entity);
                candidatBean.updateFabricationFaconnageFromOpenTheso();
                break;
            case ReferenceOpenthesoEnum.COULEUR_PATE:
                referenceOpentheso.setEntity(entity);
                // Sauvegarder la référence d'abord
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                if (entity.getDescriptionPate() == null) {
                    entity.setDescriptionPate(new DescriptionPate());
                    entity.getDescriptionPate().setEntity(entity);
                }
                entity.getDescriptionPate().setCouleur(createdReference);
                entityRepository.save(entity);
                candidatBean.updateCouleurPateFromOpenTheso();
                break;
            case ReferenceOpenthesoEnum.NATURE_PATE:
                referenceOpentheso.setEntity(entity);
                // Sauvegarder la référence d'abord
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                if (entity.getDescriptionPate() == null) {
                    entity.setDescriptionPate(new DescriptionPate());
                    entity.getDescriptionPate().setEntity(entity);
                }
                entity.getDescriptionPate().setNature(createdReference);
                entityRepository.save(entity);
                candidatBean.updateNaturePateFromOpenTheso();
                break;
            case ReferenceOpenthesoEnum.INCLUSIONS:
                referenceOpentheso.setEntity(entity);
                // Sauvegarder la référence d'abord
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                if (entity.getDescriptionPate() == null) {
                    entity.setDescriptionPate(new DescriptionPate());
                    entity.getDescriptionPate().setEntity(entity);
                }
                entity.getDescriptionPate().setInclusion(createdReference);
                entityRepository.save(entity);
                candidatBean.updateInclusionsFromOpenTheso();
                break;
            case ReferenceOpenthesoEnum.CUISSON_POST_CUISSON:
                referenceOpentheso.setEntity(entity);
                // Sauvegarder la référence d'abord
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                if (entity.getDescriptionPate() == null) {
                    entity.setDescriptionPate(new DescriptionPate());
                    entity.getDescriptionPate().setEntity(entity);
                }
                entity.getDescriptionPate().setCuisson(createdReference);
                entityRepository.save(entity);
                candidatBean.updateCuissonPostCuissonFromOpenTheso();
                break;
            default:
                log.warn("Code de référence non géré: {}", referenceCode);
                // Sauvegarder la référence sans lien spécifique
                referenceOpentheso.setEntity(entity);
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                break;
        }
        // Appeler le callback si défini avec le terme sélectionné
        if (onValidateCallback != null) {
            onValidateCallback.accept(selectedTerm);
        }

        // Fermer la boîte de dialogue
        PrimeFaces.current().executeScript("setTimeout(function() { PF('openthesoDialog').hide(); }, 100);");

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                        "Référence créée avec succès (Code: " + referenceCode + ")."));

        log.info("Référence ReferenceOpentheso créée avec succès - ID: {}, Code: '{}', Concept: {}, Entity ID: {}",
                createdReference.getId(), referenceCode, conceptId, entityId);
    }

    /**
     * Annule et ferme la boîte de dialogue
     */
    public void cancelDialog() {
        resetDialog();
        PrimeFaces.current().executeScript("PF('" + dialogWidgetVar + "').hide();");
    }
}
