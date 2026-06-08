package fr.cnrs.opentypo.presentation.bean.import_typology;

import fr.cnrs.opentypo.application.import_typology.TypologyCsvParser;
import fr.cnrs.opentypo.application.import_typology.TypologyImportAnalyzeResult;
import fr.cnrs.opentypo.application.import_typology.TypologyImportCollectionProfile;
import fr.cnrs.opentypo.application.import_typology.TypologyImportConstants;
import fr.cnrs.opentypo.application.import_typology.TypologyImportCsvExport;
import fr.cnrs.opentypo.application.import_typology.TypologyImportFieldDocumentation;
import fr.cnrs.opentypo.application.import_typology.TypologyImportJobLauncher;
import fr.cnrs.opentypo.application.import_typology.TypologyImportPreviewLine;
import fr.cnrs.opentypo.application.import_typology.TypologyImportProgress;
import fr.cnrs.opentypo.application.import_typology.TypologyImportProgressSession;
import fr.cnrs.opentypo.application.import_typology.TypologyImportService;
import jakarta.servlet.http.HttpSession;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.presentation.bean.ApplicationBean;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.ReferenceBean;
import fr.cnrs.opentypo.presentation.i18n.JsfMessages;
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
import org.springframework.util.StringUtils;

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
    private transient TypologyImportJobLauncher typologyImportJobLauncher;

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

    /** Message persistant sur l’écran d’import (échec / import partiel), distinct des popins growl. */
    private String screenStatusTitle;
    private String screenStatusDetail;
    /** error | warn | info */
    private String screenStatusSeverity = "error";

    public String getUploadedFileName() {
        return uploadedFileName;
    }

    public String getScreenStatusTitle() {
        return screenStatusTitle;
    }

    public String getScreenStatusDetail() {
        return screenStatusDetail;
    }

    public String getScreenStatusSeverity() {
        return screenStatusSeverity != null ? screenStatusSeverity : "error";
    }

    public boolean isScreenStatusVisible() {
        return StringUtils.hasText(screenStatusTitle) || StringUtils.hasText(screenStatusDetail);
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
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    JsfMessages.get("common.growl.accessDenied"),
                    JsfMessages.get("import.accessDenied.detail")));
            return;
        }
        Entity sel = applicationBean.getSelectedEntity();
        if (sel == null || sel.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_REFERENCE.equals(sel.getEntityType().getCode())) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    JsfMessages.get("import.referentialRequired.summary"),
                    JsfMessages.get("import.referentialRequired.detail")));
            return;
        }
        if (!referenceBean.canImportTypology(applicationBean)) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    JsfMessages.get("import.insufficientRights.summary"),
                    JsfMessages.get("import.insufficientRights.detail")));
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
            case CERAMIQUE -> JsfMessages.get("import.typology.ceramique");
            case MONNAIE -> JsfMessages.get("import.typology.monnaie");
            case INSTRUMENTUM -> JsfMessages.get("import.typology.instrumentum");
            case UNSUPPORTED -> JsfMessages.get("import.typology.ceramiqueGeneric");
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
            case CERAMIQUE -> JsfMessages.get("import.model.ceramique");
            case MONNAIE -> JsfMessages.get("import.model.monnaie");
            case INSTRUMENTUM -> JsfMessages.get("import.model.instrumentum");
            case UNSUPPORTED -> JsfMessages.get("import.model.unsupported");
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
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("import.title"),
                    JsfMessages.get("import.notAuthorized")));
            return;
        }
        try {
            byte[] data = event.getFile().getContent();
            String text = new String(data, StandardCharsets.UTF_8);
            this.parsedCsv = TypologyCsvParser.parse(text);
            this.uploadedFileName = event.getFile().getFileName();
            this.analysis = null;
            this.step = 1;
            clearScreenStatus();
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    JsfMessages.get("import.fileLoaded.summary"),
                    JsfMessages.format("import.fileLoaded.detail", parsedCsv.rows().size())));
            PrimeFaces.current().ajax().update(":importForm :growl");
        } catch (Exception ex) {
            log.warn("CSV invalide", ex);
            this.parsedCsv = null;
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    JsfMessages.get("import.invalidFile.summary"),
                    ex.getMessage()));
            PrimeFaces.current().ajax().update(":importForm :growl");
        }
    }

    public void runAnalyze() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (!canUseImport() || parsedCsv == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("import.analyze.title"),
                    JsfMessages.get("import.analyze.loadFileFirst")));
            stopProgressUi();
            return;
        }
        Entity reference = applicationBean.getSelectedEntity();
        if (reference == null || reference.getId() == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("import.analyze.title"),
                    JsfMessages.get("import.analyze.referentialNotFound")));
            stopProgressUi();
            return;
        }
        HttpSession httpSession = (HttpSession) fc.getExternalContext().getSession(false);
        TypologyImportProgress progress = TypologyImportProgressSession.getOrCreate(httpSession);
        progress.reset();
        TypologyImportProgressSession.bind(httpSession, progress);
        progress.beginAnalyzing(parsedCsv.rows().size());
        try {
            log.info("Analyse typologique démarrée (référentiel id={}, {} lignes)",
                    reference.getId(), parsedCsv.rows().size());
            analysis = typologyImportService.analyze(
                    reference,
                    parsedCsv,
                    resolveImportCollectionProfile(),
                    progress);
            progress.completeAnalyzing(analysis);
            step = 2;
            if (analysis.successful()) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                        JsfMessages.get("import.analyze.complete.summary"),
                        JsfMessages.get("import.analyze.complete.ok")));
            } else {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                        JsfMessages.get("import.analyze.complete.summary"),
                        JsfMessages.get("import.analyze.complete.hasErrors")));
            }
            log.info("Analyse typologique terminée (référentiel id={}, succès={})",
                    reference.getId(), analysis.successful());
        } catch (Exception ex) {
            log.error("Erreur analyse import (référentiel id={})", reference.getId(), ex);
            progress.fail(ex.getMessage());
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("common.growl.error"),
                    Objects.toString(ex.getMessage(), JsfMessages.get("import.error.technical"))));
        } finally {
            stopProgressUi();
            PrimeFaces.current().ajax().update(":importForm :growl :importProgressPanel");
        }
    }

    public void runConfirmImport() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (!canUseImport() || parsedCsv == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("import.title"),
                    JsfMessages.get("import.dataMissing")));
            stopProgressUi();
            return;
        }
        if (analysis == null || !analysis.successful()) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("import.title"),
                    JsfMessages.get("import.analysisRequired")));
            stopProgressUi();
            return;
        }
        Entity referenceEntity = applicationBean.getSelectedEntity();
        if (referenceEntity == null || referenceEntity.getId() == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, JsfMessages.get("import.title"),
                    JsfMessages.get("import.analyze.referentialNotFound")));
            stopProgressUi();
            return;
        }
        clearScreenStatus();
        HttpSession httpSession = (HttpSession) fc.getExternalContext().getSession(false);
        TypologyImportProgress progress = TypologyImportProgressSession.getOrCreate(httpSession);
        progress.reset();
        progress.beginImporting(countImportableRows());
        TypologyImportProgressSession.bind(httpSession, progress);
        try {
            typologyImportJobLauncher.launchExecute(
                    referenceEntity.getId(),
                    parsedCsv,
                    loginBean.getCurrentUser(),
                    resolveImportCollectionProfile(),
                    httpSession,
                    analysis);
            PrimeFaces.current().executeScript("typologyImportStartPoll()");
        } catch (Exception ex) {
            log.error("Démarrage import typologique impossible", ex);
            progress.fail(ex.getMessage());
            stopProgressUi();
            step = 2;
            showScreenStatus("error", JsfMessages.get("import.impossible.summary"),
                    Objects.toString(ex.getMessage(), JsfMessages.get("import.error.technical")));
            updateImportScreen();
        }
    }

    private TypologyImportProgress activeProgress() {
        return TypologyImportProgressSession.fromFacesContext();
    }

    private int countImportableRows() {
        if (analysis == null || analysis.previewLines() == null) {
            return parsedCsv != null ? parsedCsv.rows().size() : 0;
        }
        return (int) analysis.previewLines().stream()
                .filter(line -> line != null && line.lineOk())
                .count();
    }

    /**
     * Polling AJAX : met à jour la barre de progression et finalise analyse ou import lorsque le traitement est terminé.
     */
    public void pollImportProgress() {
        TypologyImportProgress progress = activeProgress();
        if (progress.isFinalized()) {
            return;
        }
        if (progress.isRunning()) {
            return;
        }
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            return;
        }
        if (progress.getPhase() == TypologyImportProgress.Phase.FAILED) {
            log.warn("Import typologique : échec remonté au poll ({})", progress.getErrorMessage());
            finalizeProgressFailure(fc, progress);
            return;
        }
        if (progress.getPhase() != TypologyImportProgress.Phase.COMPLETE) {
            return;
        }
        if (progress.getOperation() == TypologyImportProgress.Operation.IMPORT) {
            finalizeImportSuccess(fc, progress);
        }
    }

    private void finalizeImportSuccess(FacesContext fc, TypologyImportProgress progress) {
        progress.setFinalized(true);
        Entity referenceEntity = applicationBean.getSelectedEntity();
        String referenceCode = referenceEntity != null ? referenceEntity.getCode() : null;

        if (progress.isPartialSuccess()) {
            stopProgressUi();
            step = 2;
            String detail = Objects.toString(progress.getWarningMessage(),
                    progress.getCurrent() + " / " + progress.getTotal() + " lignes enregistrées.");
            showScreenStatus("warn", JsfMessages.get("import.partial.summary"), detail);
            applicationBean.refreshReferenceCategoriesList();
            if (applicationBean.getTreeBean() != null && referenceEntity != null && referenceEntity.getId() != null) {
                applicationBean.getTreeBean().loadChildForEntity(referenceEntity);
            }
            updateImportScreen();
            return;
        }

        Flash flash = fc.getExternalContext().getFlash();
        flash.setKeepMessages(true);
        fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                JsfMessages.get("import.complete.summary"),
                JsfMessages.get("import.complete.detail")));

        applicationBean.refreshReferenceCategoriesList();
        if (applicationBean.getTreeBean() != null && referenceEntity != null && referenceEntity.getId() != null) {
            applicationBean.getTreeBean().loadChildForEntity(referenceEntity);
        }
        clearWizardState();
        clearTypologyFileUploadWidget();
        stopProgressUi();

        if (referenceCode != null && !referenceCode.isBlank()) {
            try {
                String encoded = URLEncoder.encode(referenceCode, StandardCharsets.UTF_8).replace("+", "%20");
                String path = fc.getExternalContext().getRequestContextPath() + "/" + encoded;
                fc.getExternalContext().redirect(path);
                fc.responseComplete();
            } catch (IOException ioe) {
                log.warn("Redirection après import impossible : {}", ioe.getMessage());
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                        JsfMessages.get("import.complete.summary"),
                        JsfMessages.get("import.complete.redirectFailed")));
                PrimeFaces.current().ajax().update(":importForm :growl");
            }
        } else {
            PrimeFaces.current().ajax().update(":importForm :growl :centerContent");
        }
    }

    private void finalizeProgressFailure(FacesContext fc, TypologyImportProgress progress) {
        progress.setFinalized(true);
        stopProgressUi();
        if (progress.getOperation() == TypologyImportProgress.Operation.IMPORT) {
            step = 2;
        }
        String detail = Objects.toString(progress.getErrorMessage(), JsfMessages.get("import.error.technical"));
        String title = progress.getOperation() == TypologyImportProgress.Operation.IMPORT
                ? JsfMessages.get("import.interrupted.summary")
                : JsfMessages.get("common.growl.error");
        showScreenStatus("error", title, detail);
        updateImportScreen();
    }

    private static void stopProgressUi() {
        PrimeFaces.current().executeScript("typologyImportStopPoll()");
    }

    public int getImportProgressPercent() {
        return activeProgress().getPercent();
    }

    /** Anneau avec pourcentage (au moins une ligne traitée ou terminé). */
    public boolean isImportProgressShowPercent() {
        TypologyImportProgress progress = activeProgress();
        return progress.getTotal() > 0
                && (progress.getCurrent() > 0 || progress.getPhase() == TypologyImportProgress.Phase.COMPLETE);
    }

    /** Anneau animé sans pourcentage (démarrage ou préparation). */
    public boolean isImportProgressIndeterminateRing() {
        TypologyImportProgress progress = activeProgress();
        return progress.isRunning() && !isImportProgressShowPercent();
    }

    public boolean isImportProgressRunning() {
        return activeProgress().isRunning();
    }

    public String getImportProgressPhaseLabel() {
        return switch (activeProgress().getPhase()) {
            case ANALYZING -> JsfMessages.get("import.progress.analyzing");
            case IMPORTING -> JsfMessages.get("import.progress.importing");
            case COMPLETE -> JsfMessages.get("import.progress.done");
            case FAILED -> JsfMessages.get("import.progress.failed");
            default -> JsfMessages.get("import.progress.processing");
        };
    }

    public String getImportProgressDetailLabel() {
        TypologyImportProgress progress = activeProgress();
        int total = progress.getTotal();
        int current = progress.getCurrent();
        String detail = progress.getDetailMessage();
        if (total > 0) {
            if (current == 0 && detail != null && !detail.isBlank()) {
                return detail;
            }
            String lineInfo = detail != null && !detail.isBlank() ? detail + " — " : "";
            return lineInfo + current + " / " + total;
        }
        return detail != null ? detail : "";
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
        clearScreenStatus();
        TypologyImportProgressSession.fromFacesContext().reset();
    }

    private void clearScreenStatus() {
        screenStatusTitle = null;
        screenStatusDetail = null;
        screenStatusSeverity = "error";
    }

    private void showScreenStatus(String severity, String title, String detail) {
        screenStatusSeverity = severity != null ? severity : "error";
        screenStatusTitle = title;
        screenStatusDetail = detail;
    }

    private static void updateImportScreen() {
        PrimeFaces.current().ajax().update(":importScreenStatusPanel :importForm :importProgressPanel");
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
        clearScreenStatus();
    }

    public String kindLabel(TypologyImportPreviewLine line) {
        if (line == null || line.kind() == null) {
            return "";
        }
        return switch (line.kind()) {
            case CATEGORIE -> JsfMessages.get("import.kind.categorie");
            case GROUPE -> JsfMessages.get("import.kind.groupe");
            case SERIE -> JsfMessages.get("import.kind.serie");
            case TYPE_SOUS_SERIE -> JsfMessages.get("import.kind.typeSousSerie");
            case TYPE_SOUS_GROUPE -> JsfMessages.get("import.kind.typeSousGroupe");
            case NON_CLASSIFIE -> JsfMessages.get("import.kind.nonClasse");
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
