package fr.cnrs.opentypo.application.import_typology;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lance l'import typologique en arrière-plan (lots transactionnels, progression en session HTTP).
 */
@Slf4j
@Service
public class TypologyImportJobLauncher {

    private final TypologyImportService typologyImportService;

    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "typology-import-worker");
        t.setDaemon(true);
        return t;
    });

    public TypologyImportJobLauncher(TypologyImportService typologyImportService) {
        this.typologyImportService = typologyImportService;
    }

    public void launchExecute(
            Long referenceId,
            TypologyCsvParser.ParsedCsv parsed,
            Utilisateur user,
            TypologyImportCollectionProfile collectionProfile,
            HttpSession httpSession,
            TypologyImportAnalyzeResult priorAnalysis) {
        importExecutor.submit(() -> runImport(
                referenceId, parsed, user, collectionProfile, httpSession, priorAnalysis));
    }

    private void runImport(
            Long referenceId,
            TypologyCsvParser.ParsedCsv parsed,
            Utilisateur user,
            TypologyImportCollectionProfile collectionProfile,
            HttpSession httpSession,
            TypologyImportAnalyzeResult priorAnalysis) {
        TypologyImportProgress progress = TypologyImportProgressSession.getOrCreate(httpSession);
        log.info("Import typologique démarré (référentiel id={}, {} lignes CSV)",
                referenceId, parsed != null ? parsed.rows().size() : 0);
        try {
            Entity reference = new Entity();
            reference.setId(referenceId);
            TypologyImportExecutionResult result = typologyImportService.executeImport(
                    reference, parsed, user, collectionProfile, progress, true, priorAnalysis, httpSession);
            if (result.success()) {
                progress.completeImporting();
                log.info("Import typologique terminé (référentiel id={}, {} lignes)",
                        referenceId, result.importedCount());
            } else if (result.partial()) {
                progress.completeImportingPartial(
                        result.importedCount(), result.plannedCount(), result.message());
                log.warn("Import typologique partiel (référentiel id={}, {}/{} lignes) : {}",
                        referenceId, result.importedCount(), result.plannedCount(), result.message());
            } else {
                progress.fail(result.message());
                log.error("Import typologique échoué (référentiel id={}) : {}", referenceId, result.message());
            }
            TypologyImportProgressSession.publish(httpSession, progress);
        } catch (Exception ex) {
            log.error("Import typologique échoué (référentiel id={})", referenceId, ex);
            progress.fail(ex.getMessage());
            TypologyImportProgressSession.publish(httpSession, progress);
        }
    }
}
