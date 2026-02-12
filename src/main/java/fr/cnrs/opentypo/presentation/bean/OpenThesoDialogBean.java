package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.application.dto.pactols.*;
import fr.cnrs.opentypo.application.service.CollectionService;
import fr.cnrs.opentypo.application.service.PactolsService;
import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Parametrage;
import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysique;
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
        Entity collectionParent = collectionService.findCollectionIdByEntityId(entityId);
        collectionParametrage = parametrageRepository.findByEntityId(collectionParent.getId()).orElse(null);
        if (collectionParametrage == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "La collection "
                            + collectionParent.getCode()
                            + " n'est pas paramétrée"));
            return;
        }
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
                if (CollectionUtils.isEmpty(entity.getAiresCirculation())) {
                    entity.setAiresCirculation(new ArrayList<>());
                }
                entity.getAiresCirculation().add(referenceOpentheso);
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

    /**
     * Annule et ferme la boîte de dialogue
     */
    public void cancelDialog() {
        resetDialog();
        PrimeFaces.current().executeScript("PF('" + dialogWidgetVar + "').hide();");
    }
}
