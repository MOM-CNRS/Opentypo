package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.pactols.*;
import fr.cnrs.opentypo.application.service.PactolsService;
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

    // État de la boîte de dialogue
    private String dialogWidgetVar;
    private String targetFieldId; // ID du champ cible où insérer la valeur
    private Consumer<String> onValidateCallback; // Callback appelé lors de la validation

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

    @PostConstruct
    public void init() {
        String selectedLangue = searchBean.getLangSelected() != null ? searchBean.getLangSelected() : "fr";
        availableThesaurus = pactolsService.getThesaurusList(selectedLangue);
    }

    /**
     * Charge les thésaurus disponibles (appelé avant l'ouverture de la boîte de dialogue)
     */
    public void loadThesaurus() {
        // Réinitialiser l'état
        resetDialog();
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

        // Mettre à jour tous les composants de la boîte de dialogue
        PrimeFaces.current().ajax().update(":openThesoForm :growl");
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
     * Valide la sélection et ferme la boîte de dialogue
     */
    public void validateSelection() {
        if (selectedConcept == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Attention", "Veuillez sélectionner un concept."));
            return;
        }

        String selectedTerm = selectedConcept.getSelectedTerm();
        if (selectedTerm == null || selectedTerm.trim().isEmpty()) {
            selectedTerm = selectedConcept.getIdConcept();
        }

        // Appeler le callback si défini
        if (onValidateCallback != null) {
            onValidateCallback.accept(selectedTerm);
        }

        // Réinitialiser
        resetDialog();

        // Fermer la boîte de dialogue
        PrimeFaces.current().executeScript("setTimeout(function() { PF('" + dialogWidgetVar + "').hide(); }, 100);");

        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Succès", "Valeur sélectionnée avec succès."));
        
        PrimeFaces.current().ajax().update(":growl");
    }

    /**
     * Annule et ferme la boîte de dialogue
     */
    public void cancelDialog() {
        resetDialog();
        PrimeFaces.current().executeScript("PF('" + dialogWidgetVar + "').hide();");
    }
}
