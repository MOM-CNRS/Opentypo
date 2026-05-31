package fr.cnrs.opentypo.application.import_typology;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * État d'avancement partagé entre le thread d'import/analyse et les requêtes AJAX de polling.
 */
public class TypologyImportProgress implements Serializable {

    public enum Phase {
        IDLE,
        ANALYZING,
        IMPORTING,
        COMPLETE,
        FAILED
    }

    public enum Operation {
        NONE,
        ANALYZE,
        IMPORT
    }

    private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.IDLE);
    private final AtomicReference<Operation> operation = new AtomicReference<>(Operation.NONE);
    private final AtomicInteger current = new AtomicInteger(0);
    private final AtomicInteger total = new AtomicInteger(0);
    private final AtomicReference<String> detailMessage = new AtomicReference<>("");
    private final AtomicReference<String> errorMessage = new AtomicReference<>();
    private final AtomicReference<String> warningMessage = new AtomicReference<>();
    private volatile boolean partialSuccess;
    private volatile boolean finalized;

    private transient TypologyImportAnalyzeResult analyzeResult;

    public void reset() {
        phase.set(Phase.IDLE);
        operation.set(Operation.NONE);
        current.set(0);
        total.set(0);
        detailMessage.set("");
        errorMessage.set(null);
        warningMessage.set(null);
        partialSuccess = false;
        finalized = false;
        analyzeResult = null;
    }

    public void beginAnalyzing(int totalRows) {
        operation.set(Operation.ANALYZE);
        phase.set(Phase.ANALYZING);
        current.set(0);
        total.set(Math.max(0, totalRows));
        detailMessage.set("");
        errorMessage.set(null);
        warningMessage.set(null);
        partialSuccess = false;
        finalized = false;
        analyzeResult = null;
    }

    public void tickAnalyzing(int rowIndex, int totalRows, int csvRowNumber) {
        current.set(Math.min(rowIndex + 1, totalRows));
        total.set(totalRows);
        detailMessage.set("Ligne " + csvRowNumber);
    }

    public void completeAnalyzing(TypologyImportAnalyzeResult result) {
        analyzeResult = result;
        current.set(total.get());
        operation.set(Operation.ANALYZE);
        phase.set(Phase.COMPLETE);
    }

    public void beginImporting(int totalRows) {
        operation.set(Operation.IMPORT);
        phase.set(Phase.IMPORTING);
        current.set(0);
        total.set(Math.max(0, totalRows));
        detailMessage.set("");
        errorMessage.set(null);
        warningMessage.set(null);
        partialSuccess = false;
        finalized = false;
        analyzeResult = null;
    }

    /** Met à jour le total sans remettre le compteur à zéro (import déjà démarré côté UI). */
    public void syncImportTotal(int totalRows) {
        operation.set(Operation.IMPORT);
        phase.set(Phase.IMPORTING);
        total.set(Math.max(0, totalRows));
    }

    public void setPreparingImport(String message) {
        operation.set(Operation.IMPORT);
        phase.set(Phase.IMPORTING);
        detailMessage.set(message != null ? message : "Préparation…");
    }

    public void tickImporting(int done, int totalRows, int csvRowNumber) {
        current.set(Math.min(done, totalRows));
        total.set(totalRows);
        detailMessage.set("Ligne " + csvRowNumber);
    }

    public void completeImporting() {
        partialSuccess = false;
        warningMessage.set(null);
        current.set(total.get());
        operation.set(Operation.IMPORT);
        phase.set(Phase.COMPLETE);
    }

    /** Import arrêté en cours de route : les lots déjà validés restent en base. */
    public void completeImportingPartial(int imported, int planned, String message) {
        partialSuccess = true;
        warningMessage.set(message != null ? message : "Import interrompu.");
        current.set(Math.min(Math.max(0, imported), Math.max(0, planned)));
        total.set(Math.max(0, planned));
        operation.set(Operation.IMPORT);
        phase.set(Phase.COMPLETE);
    }

    public boolean isPartialSuccess() {
        return partialSuccess;
    }

    public String getWarningMessage() {
        return warningMessage.get();
    }

    public Operation getOperation() {
        return operation.get();
    }

    public void fail(String message) {
        errorMessage.set(message != null ? message : "Erreur inconnue.");
        phase.set(Phase.FAILED);
    }

    public Phase getPhase() {
        return phase.get();
    }

    public int getCurrent() {
        return current.get();
    }

    public int getTotal() {
        return total.get();
    }

    public int getPercent() {
        int t = total.get();
        if (t <= 0) {
            return phase.get() == Phase.COMPLETE ? 100 : 0;
        }
        return Math.min(100, (int) Math.round(100.0 * current.get() / t));
    }

    public boolean isDeterminate() {
        return total.get() > 0 && (phase.get() == Phase.ANALYZING || phase.get() == Phase.IMPORTING);
    }

    public boolean isRunning() {
        Phase p = phase.get();
        return p == Phase.ANALYZING || p == Phase.IMPORTING;
    }

    public boolean isTerminal() {
        Phase p = phase.get();
        return p == Phase.COMPLETE || p == Phase.FAILED;
    }

    public String getDetailMessage() {
        return detailMessage.get();
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public TypologyImportAnalyzeResult getAnalyzeResult() {
        return analyzeResult;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean value) {
        finalized = value;
    }
}
