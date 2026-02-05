package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.service.PactolsService;
import fr.cnrs.opentypo.application.dto.pactols.PactolsCollection;
import fr.cnrs.opentypo.application.dto.pactols.PactolsLangue;
import fr.cnrs.opentypo.application.dto.pactols.PactolsThesaurus;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Parametrage;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
}
