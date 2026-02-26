package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.application.dto.pactols.*;
import fr.cnrs.opentypo.application.service.CollectionService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.PactolsService;
import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Parametrage;
import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysique;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysiqueMonnaie;
import fr.cnrs.opentypo.domain.entity.DescriptionPate;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ParametrageRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import fr.cnrs.opentypo.presentation.bean.candidats.CandidatBean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Autowired
    private PactolsService pactolsService;

    @Autowired
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;

    @Autowired
    private ParametrageRepository parametrageRepository;

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    @Lazy
    private CandidatBean candidatBeanForAutocomplete;

    @Autowired
    private GroupService groupService;

    // État de la boîte de dialogue
    private String dialogWidgetVar;
    private String targetFieldId; // ID du champ cible où insérer la valeur
    private Consumer<String> onValidateCallback; // Callback appelé lors de la validation
    private ReferenceOpenthesoEnum referenceCode; // Code pour la référence (PRODUCTION, PERIODE, etc.)
    private Long entityId; // ID de l'entité à mettre à jour
    private ReferenceOpentheso createdReference; // Référence créée lors de la validation

    // Données du formulaire
    private String searchValue = "";
    private List<PactolsConcept> searchResults = new ArrayList<>();
    private PactolsConcept selectedConcept;
    /** Valeur saisie manuellement lorsque la recherche ne retourne aucun résultat (enregistrée dans reference-opentheso.valeur). */
    private String manualValue = "";
    /** Dernière requête saisie dans l'autocomplete Période (pour enregistrement en texte libre si aucun résultat). */
    private String lastPeriodeQuery = "";
    /** Dernière requête par type de thésaurus (pour enregistrement en texte libre si aucun résultat). */
    private final Map<String, String> lastQueryByType = new HashMap<>();
    private CandidatBean candidatBean;
    private Parametrage collectionParametrage;

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
        Optional<Entity> groupeParent = groupService.findGroupByEntityId(entityId);
        if (groupeParent.isPresent()) {
            Optional<Parametrage> parametrage = parametrageRepository.findByEntityId(groupeParent.get().getId());
            if (parametrage.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La collection "
                                + groupeParent.get().getCode() + " n'est pas paramétrée"));
                return;
            }
            collectionParametrage = parametrage.get();
            // Réinitialiser l'état
            resetDialog();
        }
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
        searchValue = "";
        searchResults = new ArrayList<>();
        selectedConcept = null;
        manualValue = "";
        createdReference = null;
    }

    /**
     * Recherche les concepts
     */
    public void onConceptSearch() {
        if (collectionParametrage == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Le thésaurus n'est pas encore paramétrer pour ce référence"));
            return;
        }

        if (collectionParametrage.getIdTheso() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner un thésaurus."));
            return;
        }

        if (collectionParametrage.getIdGroupe() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner une collection."));
            return;
        }

        if (collectionParametrage.getIdLangue() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner une langue."));
            return;
        }

        if (searchValue == null || searchValue.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez saisir un terme de recherche."));
            return;
        }
        
        searchResults = pactolsService.searchConcepts(collectionParametrage.getIdTheso(), searchValue.trim(),
                collectionParametrage.getIdLangue(),
                collectionParametrage.getIdGroupe());

        PrimeFaces.current().ajax().update(":openThesoForm:conceptsTable :growl");
    }

    /**
     * Méthode de complétion pour l'autocomplete Période.
     * Même logique que onConceptSearch - recherche dans le thésaurus Pactols.
     */
    public List<PactolsConcept> completePeriode(String query) {
        if (candidatBeanForAutocomplete == null
                || candidatBeanForAutocomplete.getCurrentEntity() == null
                || candidatBeanForAutocomplete.getCurrentEntity().getId() == null) {
            return new ArrayList<>();
        }
        ensurePeriodeThesaurusLoaded(candidatBeanForAutocomplete);
        if (collectionParametrage == null || collectionParametrage.getIdTheso() == null
                || collectionParametrage.getIdGroupe() == null || collectionParametrage.getIdLangue() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Connexion OpenTheso",
                            "Le gestionnaire du référentiel doit paramétrer la connexion du groupe avec OpenTheso pour permettre la recherche dans le thésaurus."));
            PrimeFaces.current().ajax().update(":growl");
            return new ArrayList<>();
        }
        String term = (query != null && !query.trim().isEmpty()) ? query.trim() : "";
        if (term.isEmpty()) {
            lastPeriodeQuery = "";
            return new ArrayList<>();
        }
        lastPeriodeQuery = term;
        return pactolsService.searchConcepts(collectionParametrage.getIdTheso(), term,
                collectionParametrage.getIdLangue(), collectionParametrage.getIdGroupe());
    }

    private void ensurePeriodeThesaurusLoaded(CandidatBean cb) {
        ensureThesaurusLoaded(cb, ReferenceOpenthesoEnum.PERIODE);
    }

    /**
     * Charge le thésaurus pour un type donné si pas déjà chargé pour cette entité.
     */
    private void ensureThesaurusLoaded(CandidatBean cb, ReferenceOpenthesoEnum type) {
        if (collectionParametrage != null && referenceCode == type && entityId != null
                && cb.getCurrentEntity() != null && cb.getCurrentEntity().getId() != null
                && entityId.equals(cb.getCurrentEntity().getId())) {
            return;
        }
        Long eid = (cb.getCurrentEntity() != null && cb.getCurrentEntity().getId() != null)
                ? cb.getCurrentEntity().getId() : null;
        loadThesaurus(cb, type.name(), eid);
    }

    /**
     * Méthode de complétion générique pour un type de thésaurus.
     */
    private List<PactolsConcept> completeThesaurus(ReferenceOpenthesoEnum type, String query) {
        CandidatBean cb = candidatBeanForAutocomplete;
        if (cb == null || cb.getCurrentEntity() == null || cb.getCurrentEntity().getId() == null) {
            return new ArrayList<>();
        }
        ensureThesaurusLoaded(cb, type);
        if (collectionParametrage == null || collectionParametrage.getIdTheso() == null
                || collectionParametrage.getIdGroupe() == null || collectionParametrage.getIdLangue() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Connexion OpenTheso",
                            "Le gestionnaire du référentiel doit paramétrer la connexion du groupe avec OpenTheso pour permettre la recherche dans le thésaurus."));
            PrimeFaces.current().ajax().update(":growl");
            return new ArrayList<>();
        }
        String term = (query != null && !query.trim().isEmpty()) ? query.trim() : "";
        if (term.isEmpty()) {
            lastQueryByType.put(type.name(), "");
            return new ArrayList<>();
        }
        lastQueryByType.put(type.name(), term);
        return pactolsService.searchConcepts(collectionParametrage.getIdTheso(), term,
                collectionParametrage.getIdLangue(), collectionParametrage.getIdGroupe());
    }

    public List<PactolsConcept> completeProduction(String query) { return completeThesaurus(ReferenceOpenthesoEnum.PRODUCTION, query); }
    public List<PactolsConcept> completeAireCirculation(String query) { return completeThesaurus(ReferenceOpenthesoEnum.AIRE_CIRCULATION, query); }
    public List<PactolsConcept> completeFonctionUsage(String query) { return completeThesaurus(ReferenceOpenthesoEnum.FONCTION_USAGE, query); }
    public List<PactolsConcept> completeMetrologie(String query) { return completeThesaurus(ReferenceOpenthesoEnum.METROLOGIE, query); }
    public List<PactolsConcept> completeFabricationFaconnage(String query) { return completeThesaurus(ReferenceOpenthesoEnum.FABRICATION_FACONNAGE, query); }
    public List<PactolsConcept> completeCouleurPate(String query) { return completeThesaurus(ReferenceOpenthesoEnum.COULEUR_PATE, query); }
    public List<PactolsConcept> completeNaturePate(String query) { return completeThesaurus(ReferenceOpenthesoEnum.NATURE_PATE, query); }
    public List<PactolsConcept> completeInclusions(String query) { return completeThesaurus(ReferenceOpenthesoEnum.INCLUSIONS, query); }
    public List<PactolsConcept> completeCuissonPostCuisson(String query) { return completeThesaurus(ReferenceOpenthesoEnum.CUISSON_POST_CUISSON, query); }
    public List<PactolsConcept> completeMateriau(String query) { return completeThesaurus(ReferenceOpenthesoEnum.MATERIAU, query); }
    public List<PactolsConcept> completeDenomination(String query) { return completeThesaurus(ReferenceOpenthesoEnum.DENOMINATION, query); }
    public List<PactolsConcept> completeValeur(String query) { return completeThesaurus(ReferenceOpenthesoEnum.VALEUR, query); }
    public List<PactolsConcept> completeTechnique(String query) { return completeThesaurus(ReferenceOpenthesoEnum.TECHNIQUE, query); }
    public List<PactolsConcept> completeFabrication(String query) { return completeThesaurus(ReferenceOpenthesoEnum.FABRICATION, query); }

    /** Retourne la dernière requête saisie pour un type (pour enregistrement en texte libre). */
    public String getLastQueryFor(String typeCode) {
        return lastQueryByType.getOrDefault(typeCode != null ? typeCode : "", "");
    }

    public String getEmptyMessageFor(String typeCode) {
        return isPeriodeParametrageMissing()
                ? "Connexion OpenTheso non paramétrée pour ce groupe. Le gestionnaire du référentiel doit configurer le paramétrage."
                : "Aucun résultat trouvé";
    }

    /**
     * Indique si le paramétrage OpenTheso est manquant pour le groupe de l'entité en cours.
     * Utilisé pour afficher un message d'information dans le champ Période.
     */
    public boolean isPeriodeParametrageMissing() {
        CandidatBean cb = candidatBeanForAutocomplete;
        if (cb == null || cb.getCurrentEntity() == null || cb.getCurrentEntity().getId() == null) {
            return false;
        }
        Optional<Entity> groupeOpt = groupService.findGroupByEntityId(cb.getCurrentEntity().getId());
        if (groupeOpt.isEmpty()) {
            return false;
        }
        Optional<Parametrage> pOpt = parametrageRepository.findByEntityId(groupeOpt.get().getId());
        if (pOpt.isEmpty()) {
            return true;
        }
        Parametrage p = pOpt.get();
        return p.getIdTheso() == null || p.getIdTheso().trim().isEmpty()
                || p.getIdLangue() == null || p.getIdLangue().trim().isEmpty()
                || p.getIdGroupe() == null || p.getIdGroupe().trim().isEmpty();
    }

    /**
     * Message à afficher dans la liste vide de l'autocomplete Période.
     * Différencie le cas « paramétrage manquant » du cas « aucun résultat ».
     */
    public String getPeriodeEmptyMessage() {
        return isPeriodeParametrageMissing()
                ? "Connexion OpenTheso non paramétrée pour ce groupe. Le gestionnaire du référentiel doit configurer le paramétrage."
                : "Aucun résultat trouvé";
    }

    /**
     * Enregistre une période sélectionnée depuis l'autocomplete.
     */
    public void savePeriodeFromConcept(PactolsConcept concept) {
        saveThesaurusFromConcept(ReferenceOpenthesoEnum.PERIODE, concept, "période");
    }

    /**
     * Enregistre un concept sélectionné depuis l'autocomplete pour un type de thésaurus donné.
     * @param type   type de référence (PRODUCTION, AIRE_CIRCULATION, etc.)
     * @param concept concept sélectionné ou null pour utiliser la dernière requête saisie
     * @param label   libellé pour les messages (ex: "production", "aire de circulation")
     */
    public void saveThesaurusFromConcept(ReferenceOpenthesoEnum type, PactolsConcept concept, String label) {
        CandidatBean cb = candidatBeanForAutocomplete;
        if (cb == null || cb.getCurrentEntity() == null || cb.getCurrentEntity().getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Aucune entité en cours d'édition."));
            return;
        }
        PactolsConcept conceptToSave = concept;
        if (conceptToSave == null) {
            String lastQuery = lastQueryByType.getOrDefault(type.name(), "");
            if (lastQuery != null && !lastQuery.trim().isEmpty()) {
                conceptToSave = new PactolsConcept(null, null, lastQuery.trim());
            }
        }
        if (conceptToSave == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention",
                            "Veuillez saisir ou sélectionner une " + (label != null ? label : "valeur") + "."));
            return;
        }
        ensureThesaurusLoaded(cb, type);
        if (collectionParametrage == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Le thésaurus n'est pas paramétré."));
            return;
        }
        this.candidatBean = cb;
        this.referenceCode = type;
        this.entityId = cb.getCurrentEntity().getId();
        this.selectedConcept = conceptToSave;
        this.manualValue = null;
        validateSelection();
        lastQueryByType.put(type.name(), "");
    }

    /**
     * Valide la sélection (concept issu de la recherche ou valeur manuelle) et crée une instance de ReferenceOpentheso.
     * Si un concept est sélectionné, il est utilisé ; sinon la valeur manuelle est enregistrée dans reference-opentheso.valeur.
     */
    public void validateSelection() {
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

        Entity entity = entityRepository.findById(entityId).orElse(null);
        if (entity == null) {
            log.error("Aucun element trouvée avec l'id {}", entityId);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Entité introuvable."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        String valueToSave;
        ReferenceOpentheso referenceOpentheso = new ReferenceOpentheso();
        referenceOpentheso.setCode(referenceCode.name());
        referenceOpentheso.setEntity(entity);

        if (selectedConcept != null) {
            // Cas : sélection d'un concept issu de la recherche
            if (collectionParametrage == null || collectionParametrage.getIdTheso() == null || collectionParametrage.getIdTheso().trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Thésaurus non sélectionné."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }

            valueToSave = selectedConcept.getSelectedTerm();
            if (valueToSave == null || valueToSave.trim().isEmpty()) {
                valueToSave = selectedConcept.getIdConcept();
            }

            referenceOpentheso.setValeur(valueToSave);
            referenceOpentheso.setConceptId(selectedConcept.getIdConcept());
            referenceOpentheso.setThesaurusId(collectionParametrage.getIdTheso());
            referenceOpentheso.setCollectionId(collectionParametrage.getIdGroupe());
            referenceOpentheso.setUrl(selectedConcept.getUri());
        } else if (manualValue != null && !manualValue.trim().isEmpty()) {
            // Cas : saisie manuelle (aucun résultat ou choix de l'utilisateur)
            valueToSave = manualValue.trim();
            referenceOpentheso.setValeur(valueToSave);
            // thesaurusId, conceptId, collectionId, url restent null
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention",
                    "Veuillez sélectionner un concept dans les résultats ou saisir une valeur manuelle."));
            PrimeFaces.current().ajax().update(":growl");
            return;
        }

        final String valueForCallbackAndLambda = valueToSave;

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
                candidatBean.updatePeriodeFromOpenTheso();
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
                /* Ne jamais remplacer la collection : avec orphanRemoval, setXxx(new ArrayList())
                   provoquerait l'erreur "collection no longer referenced". Toujours modifier en place. */
                List<ReferenceOpentheso> aires = entity.getAiresCirculation();
                if (aires == null) {
                    entity.setAiresCirculation(aires = new ArrayList<>());
                }
                aires.add(referenceOpentheso);
                // Sauvegarder l'entité (cascade ALL sauvegardera aussi la référence)
                Entity savedEntity = entityRepository.save(entity);
                // Récupérer la référence sauvegardée (par valeur, pour gérer concept ou saisie manuelle)
                if (savedEntity.getAiresCirculation() != null && !savedEntity.getAiresCirculation().isEmpty()) {
                    createdReference = savedEntity.getAiresCirculation().stream()
                            .filter(ref -> valueForCallbackAndLambda.equals(ref.getValeur()))
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
            case ReferenceOpenthesoEnum.MATERIAU:
            case ReferenceOpenthesoEnum.DENOMINATION:
            case ReferenceOpenthesoEnum.VALEUR:
            case ReferenceOpenthesoEnum.TECHNIQUE:
            case ReferenceOpenthesoEnum.FABRICATION:
                createdReference = saveToCaracteristiquePhysiqueMonnaie(entity, referenceOpentheso, referenceCode);
                break;
            default:
                log.warn("Code de référence non géré: {}", referenceCode);
                // Sauvegarder la référence sans lien spécifique
                referenceOpentheso.setEntity(entity);
                createdReference = referenceOpenthesoRepository.save(referenceOpentheso);
                break;
        }
        // Appeler le callback si défini avec la valeur enregistrée
        if (onValidateCallback != null) {
            onValidateCallback.accept(valueForCallbackAndLambda);
        }

        // Fermer la boîte de dialogue
        PrimeFaces.current().executeScript("setTimeout(function() { PF('openthesoDialog').hide(); }, 100);");

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès",
                        "Référence créée avec succès (Code: " + referenceCode + ")."));

        log.info("Référence ReferenceOpentheso créée avec succès - ID: {}, Code: '{}', Valeur: '{}', Entity ID: {}",
                createdReference.getId(), referenceCode, valueForCallbackAndLambda, entityId);
    }

    private ReferenceOpentheso saveToCaracteristiquePhysiqueMonnaie(Entity entity, ReferenceOpentheso ref, ReferenceOpenthesoEnum code) {
        if (entity == null || ref == null) return null;
        ref.setEntity(entity);
        CaracteristiquePhysiqueMonnaie cpm = entity.getCaracteristiquePhysiqueMonnaie();
        if (cpm == null) {
            cpm = new CaracteristiquePhysiqueMonnaie();
            cpm.setEntity(entity);
            entity.setCaracteristiquePhysiqueMonnaie(cpm);
        }
        ref = referenceOpenthesoRepository.save(ref);
        switch (code) {
            case MATERIAU -> { cpm.setMateriau(ref); candidatBean.updateMateriauFromOpenTheso(); }
            case DENOMINATION -> { cpm.setDenomination(ref); candidatBean.updateDenominationFromOpenTheso(); }
            case VALEUR -> { cpm.setValeur(ref); candidatBean.updateValeurFromOpenTheso(); }
            case TECHNIQUE -> { cpm.setTechnique(ref); candidatBean.updateTechniqueFromOpenTheso(); }
            case FABRICATION -> { cpm.setFabrication(ref); candidatBean.updateFabricationMonnaieFromOpenTheso(); }
            default -> { }
        }
        entityRepository.save(entity);
        return ref;
    }

    /**
     * Annule et ferme la boîte de dialogue
     */
    public void cancelDialog() {
        resetDialog();
        PrimeFaces.current().executeScript("PF('" + dialogWidgetVar + "').hide();");
    }
}
