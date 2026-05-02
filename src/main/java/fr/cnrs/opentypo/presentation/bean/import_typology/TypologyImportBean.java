package fr.cnrs.opentypo.presentation.bean.import_typology;

import fr.cnrs.opentypo.application.import_typology.TypologyCsvParser;
import fr.cnrs.opentypo.application.import_typology.TypologyImportAnalyzeResult;
import fr.cnrs.opentypo.application.import_typology.TypologyImportCollectionProfile;
import fr.cnrs.opentypo.application.import_typology.TypologyImportConstants;
import fr.cnrs.opentypo.application.import_typology.TypologyImportCsvExport;
import fr.cnrs.opentypo.application.import_typology.TypologyImportFieldDocumentation;
import fr.cnrs.opentypo.application.import_typology.TypologyImportPreviewLine;
import fr.cnrs.opentypo.application.import_typology.TypologyImportService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.presentation.bean.ApplicationBean;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.ReferenceBean;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Assistant 3 étapes : fichier → analyse → confirmation et import transactionnel.
 */
@SessionScoped
@Named(value = "typologyImportBean")
@Slf4j
public class TypologyImportBean implements Serializable {

    @Autowired
    private transient TypologyImportService typologyImportService;

    @Autowired
    private ApplicationBean applicationBean;

    @Autowired
    private LoginBean loginBean;

    @Autowired
    private ReferenceBean referenceBean;

    @Getter
    private int step = 1;

    @Getter
    private TypologyCsvParser.ParsedCsv parsedCsv;

    @Getter
    private TypologyImportAnalyzeResult analysis;

    private String uploadedFileName;

    public String getUploadedFileName() {
        return uploadedFileName;
    }

    public void onPageEnter() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }
        /*
         * À chaque ouverture « fraîche » de la vue (navigation GET), réinitialiser l’assistant :
         * ainsi, en quittant puis en revenant sur le module, l’écran repart à zéro (pas de fichier,
         * étape 1). Les requêtes AJAX sur cette page restent des postbacks : on ne réinitialise pas.
         */
        if (!ctx.isPostback()) {
            resetWizard();
        }
        if (!loginBean.isAuthenticated()) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Accès refusé",
                    "Vous devez être connecté."));
            return;
        }
        Entity sel = applicationBean.getSelectedEntity();
        if (sel == null || sel.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_REFERENCE.equals(sel.getEntityType().getCode())) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Référentiel requis",
                    "Sélectionnez un référentiel dans l’arborescence puis ouvrez à nouveau l’import."));
            return;
        }
        if (!referenceBean.canImportTypology(applicationBean)) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Droit insuffisant",
                    "Seuls les administrateurs et les gestionnaires de ce référentiel peuvent importer."));
        }
    }

    public boolean canUseImport() {
        Entity sel = applicationBean.getSelectedEntity();
        return loginBean.isAuthenticated()
                && sel != null && sel.getEntityType() != null
                && EntityConstants.ENTITY_TYPE_REFERENCE.equals(sel.getEntityType().getCode())
                && referenceBean.canImportTypology(applicationBean);
    }

    /**
     * Profil d’import selon la collection du référentiel (Céramique, Monnaie ou Instrumentum).
     */
    public TypologyImportCollectionProfile resolveImportCollectionProfile() {
        if (applicationBean.isCashTypo()) {
            return TypologyImportCollectionProfile.MONNAIE;
        }
        if (applicationBean.isCeramiqueTypo()) {
            return TypologyImportCollectionProfile.CERAMIQUE;
        }
        if (applicationBean.isInstrumentumTypo()) {
            return TypologyImportCollectionProfile.INSTRUMENTUM;
        }
        return TypologyImportCollectionProfile.UNSUPPORTED;
    }

    /**
     * Libellé court de la typologie d’import (collection du référentiel), pour l’UI du modèle CSV.
     */
    public String getImportCollectionTypologyLabel() {
        return switch (resolveImportCollectionProfile()) {
            case CERAMIQUE -> "Céramique";
            case MONNAIE -> "Monnaie";
            case INSTRUMENTUM -> "Instrumentum";
            case UNSUPPORTED -> "Céramique (générique)";
        };
    }

    public boolean isImportCollectionTypologyRecognized() {
        return resolveImportCollectionProfile() != TypologyImportCollectionProfile.UNSUPPORTED;
    }

    /**
     * Texte d’aide sous la carte « modèle » : rappelle le format attendu pour la typologie courante.
     */
    public String getImportCollectionModelDescription() {
        return switch (resolveImportCollectionProfile()) {
            case CERAMIQUE -> "En-tête + une ligne d’exemple complets, colonnes céramique (UTF-8).";
            case MONNAIE -> "En-tête + ligne d’exemple au format Monnaie (droit, revers, caractéristiques, etc.).";
            case INSTRUMENTUM -> "En-tête + ligne d’exemple au format Instrumentum (décors, marques, caract. physiques dédiées).";
            case UNSUPPORTED -> "Collection non reconnue : le fichier proposé reprend le modèle Céramique. Idéalement, rattachez le référentiel à une collection Céramique, Monnaie ou Instrumentum.";
        };
    }

    /**
     * Documentation des colonnes du CSV pour la typologie du référentiel (ordre du modèle téléchargé).
     */
    public List<TypologyImportFieldDocumentation.FieldDocRow> getImportFieldDocumentationRows() {
        return TypologyImportFieldDocumentation.rowsForProfile(resolveImportCollectionProfile());
    }

    public void handleFileUpload(FileUploadEvent event) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (!canUseImport()) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Import",
                    "Opération non autorisée."));
            return;
        }
        try {
            byte[] data = event.getFile().getContent();
            String text = new String(data, StandardCharsets.UTF_8);
            this.parsedCsv = TypologyCsvParser.parse(text);
            this.uploadedFileName = event.getFile().getFileName();
            this.analysis = null;
            this.step = 1;
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Fichier chargé",
                    parsedCsv.rows().size() + " ligne(s) de données (hors en-tête)."));
            PrimeFaces.current().ajax().update(":importForm :growl");
        } catch (Exception ex) {
            log.warn("CSV invalide", ex);
            this.parsedCsv = null;
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fichier invalide",
                    ex.getMessage()));
            PrimeFaces.current().ajax().update(":importForm :growl");
        }
    }

    public void runAnalyze() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (!canUseImport() || parsedCsv == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Analyse",
                    "Chargez d’abord un fichier CSV."));
            return;
        }
        try {
            analysis = typologyImportService.analyze(applicationBean.getSelectedEntity(), parsedCsv,
                    resolveImportCollectionProfile());
            step = 2;
            if (analysis.successful()) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Analyse terminée",
                        "Aucune erreur bloquante. Vous pouvez confirmer l’import."));
            } else {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Analyse terminée",
                        "Corrigez les erreurs ou le fichier avant de confirmer."));
            }
            PrimeFaces.current().ajax().update(":importForm :growl");
        } catch (Exception ex) {
            log.error("Erreur analyse import", ex);
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur",
                    ex.getMessage()));
            PrimeFaces.current().ajax().update(":importForm :growl");
        }
    }

    public void runConfirmImport() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (!canUseImport() || parsedCsv == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Import",
                    "Données manquantes."));
            return;
        }
        if (analysis == null || !analysis.successful()) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Import",
                    "L’analyse doit être valide avant confirmation."));
            return;
        }
        try {
            Entity referenceEntity = applicationBean.getSelectedEntity();
            String referenceCode = referenceEntity != null ? referenceEntity.getCode() : null;

            typologyImportService.execute(referenceEntity, parsedCsv,
                    loginBean.getCurrentUser(), resolveImportCollectionProfile());

            Flash flash = fc.getExternalContext().getFlash();
            flash.setKeepMessages(true);
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Import terminé",
                    "Les données ont été enregistrées. Ouverture du référentiel…"));

            applicationBean.refreshReferenceCategoriesList();
            if (applicationBean.getTreeBean() != null && referenceEntity != null && referenceEntity.getId() != null) {
                applicationBean.getTreeBean().loadChildForEntity(referenceEntity);
            }
            clearWizardState();
            clearTypologyFileUploadWidget();

            if (referenceCode != null && !referenceCode.isBlank()) {
                try {
                    String encoded = URLEncoder.encode(referenceCode, StandardCharsets.UTF_8).replace("+", "%20");
                    String path = fc.getExternalContext().getRequestContextPath() + "/" + encoded;
                    fc.getExternalContext().redirect(path);
                    fc.responseComplete();
                } catch (IOException ioe) {
                    log.warn("Redirection après import impossible : {}", ioe.getMessage());
                    fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Import terminé",
                            "Ouverture automatique impossible ; ouvrez le référentiel depuis le menu."));
                    PrimeFaces.current().ajax().update(":importForm :growl");
                }
            } else {
                PrimeFaces.current().ajax().update(":importForm :growl :centerContent");
            }
        } catch (Exception ex) {
            log.error("Échec import typologique", ex);
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Import annulé",
                    Objects.toString(ex.getMessage(), "Erreur technique.")));
            PrimeFaces.current().ajax().update(":importForm :growl");
        }
    }

    /** Réinitialise fichier chargé, analyse et étape ; vide aussi le widget PrimeFaces de sélection de fichier. */
    public void resetWizard() {
        clearWizardState();
        clearTypologyFileUploadWidget();
    }

    private void clearWizardState() {
        parsedCsv = null;
        analysis = null;
        uploadedFileName = null;
        step = 1;
    }

    private void clearTypologyFileUploadWidget() {
        PrimeFaces.current().executeScript(
                "try{if(typeof PF==='function'&&PF('typologyCsvUpload')){PF('typologyCsvUpload').clear();}}catch(e){}");
    }

    /**
     * Sortie du module (lien retour) : état réinitialisé pour la prochaine visite.
     */
    public void leaveImportModule() {
        clearWizardState();
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            return;
        }
        try {
            fc.getExternalContext().redirect(fc.getExternalContext().getRequestContextPath() + "/index.xhtml");
            fc.responseComplete();
        } catch (IOException e) {
            log.warn("Redirection impossible après sortie du module import", e);
        }
    }

    /** Retour à l’étape fichier sans perdre le CSV chargé. */
    public void goToStep1() {
        step = 1;
    }

    public String kindLabel(TypologyImportPreviewLine line) {
        if (line == null || line.kind() == null) {
            return "";
        }
        return switch (line.kind()) {
            case CATEGORIE -> "Catégorie";
            case GROUPE -> "Groupe";
            case SERIE -> "Série";
            case TYPE_SOUS_SERIE -> "Type (sous série)";
            case TYPE_SOUS_GROUPE -> "Type (sous groupe)";
            case NON_CLASSIFIE -> "—";
        };
    }

    /**
     * Lignes bloquantes ou invalides : statut « non OK » ou au moins un message d'erreur (avertissements seuls exclus).
     */
    public List<TypologyImportPreviewLine> getPreviewLinesErrorsOnly() {
        if (analysis == null || analysis.previewLines() == null) {
            return List.of();
        }
        return analysis.previewLines().stream()
                .filter(line -> !line.lineOk() || !line.errors().isEmpty())
                .toList();
    }

    public boolean isHasErrorLinesForExport() {
        return !getPreviewLinesErrorsOnly().isEmpty();
    }

    /** Erreurs globales (référentiel absent, etc.), sans lien forcé avec une ligne du CSV. */
    public List<String> getBlockingMessages() {
        if (analysis == null || analysis.blockingErrors() == null) {
            return List.of();
        }
        return analysis.blockingErrors();
    }

    /**
     * Indique s'il faut afficher l'encart « aucune ligne en erreur » (lignes de données présentes mais filtre vide).
     */
    public boolean isShowNoErrorRowsHint() {
        if (analysis == null || analysis.previewLines() == null) {
            return false;
        }
        return !analysis.previewLines().isEmpty() && getPreviewLinesErrorsOnly().isEmpty();
    }

    /**
     * Export CSV des lignes en erreur (données d'origine + colonnes {@code erreurs_detectees} et {@code avertissements_detectes}).
     */
    public StreamedContent getErrorLinesExportDownload() {
        byte[] bytes;
        if (parsedCsv == null || analysis == null) {
            bytes = new byte[0];
        } else {
            bytes = TypologyImportCsvExport.buildUtf8(parsedCsv, getPreviewLinesErrorsOnly());
        }
        return DefaultStreamedContent.builder()
                .name("import-typologique-lignes-en-erreur.csv")
                .contentType("text/csv; charset=UTF-8")
                .stream(() -> new ByteArrayInputStream(bytes))
                .build();
    }

    /**
     * Nom du fichier CSV modèle (identique à celui envoyé au navigateur).
     */
    public String getTemplateDownloadFilename() {
        TypologyImportCollectionProfile profile = resolveImportCollectionProfile();
        String suffix = switch (profile) {
            case MONNAIE -> "-monnaie";
            case INSTRUMENTUM -> "-instrumentum";
            case CERAMIQUE, UNSUPPORTED -> "-ceramique";
        };
        return "import-typologique-modele" + suffix + ".csv";
    }

    /**
     * Fichier CSV modèle : en-tête + une ligne d'exemple complète (UTF-8).
     */
    public StreamedContent getTemplateDownload() {
        TypologyImportCollectionProfile profile = resolveImportCollectionProfile();
        byte[] bytes = TypologyImportConstants.csvTemplateHeaderAndExample(profile).getBytes(StandardCharsets.UTF_8);
        return DefaultStreamedContent.builder()
                .name(getTemplateDownloadFilename())
                .contentType("text/csv; charset=UTF-8")
                .stream(() -> new ByteArrayInputStream(bytes))
                .build();
    }
}
