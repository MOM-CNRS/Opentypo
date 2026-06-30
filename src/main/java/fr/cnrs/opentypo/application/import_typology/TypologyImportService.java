package fr.cnrs.opentypo.application.import_typology;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.application.service.ArkIdentifierService;
import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.EntityCodeUniquenessService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.SerieService;
import fr.cnrs.opentypo.application.service.TypeService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
import fr.cnrs.opentypo.domain.entity.DescriptionPate;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Image;
import fr.cnrs.opentypo.domain.entity.Label;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.domain.entity.ReferenceOpentheso;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysique;
import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysiqueMonnaie;
import fr.cnrs.opentypo.domain.entity.DescriptionMonnaie;
import fr.cnrs.opentypo.domain.entity.AuteurScientifique;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.AuteurScientifiqueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRelationRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import fr.cnrs.opentypo.infrastructure.persistence.ReferenceOpenthesoRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Import CSV des catégories, groupes, séries et types pour un référentiel donné.
 * Analyse sans écriture ; exécution par lots (commit partiel possible après analyse).
 */
@Slf4j
@Service
public class TypologyImportService {

    private static final String KEY_SEP = "\u001F";
    private static final Pattern LIST_SPLIT = Pattern.compile(TypologyImportConstants.LIST_SEPARATOR);

    private static boolean columnInCsv(Set<String> csvHeaders, String columnConstant) {
        return csvHeaders.contains(columnConstant.toLowerCase(Locale.ROOT));
    }

    /**
     * En mise à jour : une colonne du fichier n'est écrite que si elle est présente dans l'en-tête CSV
     * et que la cellule est non vide (évite d'effacer une valeur existante avec une cellule vide).
     * En création : toute colonne présente dans le fichier est prise en compte ; les colonnes absentes ne sont pas initialisées.
     */
    private boolean shouldWriteField(Set<String> csvHeaders, String columnConstant, Map<String, String> row, boolean isCreate) {
        if (!columnInCsv(csvHeaders, columnConstant)) {
            return false;
        }
        if (isCreate) {
            return true;
        }
        return StringUtils.hasText(getCell(row, columnConstant));
    }

    @Autowired
    private EntityRepository entityRepository;
    @Autowired
    private EntityRelationRepository entityRelationRepository;
    @Autowired
    private EntityTypeRepository entityTypeRepository;
    @Autowired
    private LangueRepository langueRepository;
    @Autowired
    private ReferenceOpenthesoRepository referenceOpenthesoRepository;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private SerieService serieService;
    @Autowired
    private TypeService typeService;
    @Autowired
    private EntityCodeUniquenessService entityCodeUniquenessService;
    @Autowired
    private ArkIdentifierService arkIdentifierService;
    @Autowired
    private AuteurScientifiqueRepository auteurScientifiqueRepository;
    @Autowired
    private PlatformTransactionManager platformTransactionManager;
    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate importTransactionTemplate;

    @PostConstruct
    void initImportTransactionTemplate() {
        importTransactionTemplate = new TransactionTemplate(platformTransactionManager);
        importTransactionTemplate.setTimeout(3_600);
    }

    /**
     * Analyse le fichier : classification, détection d'erreurs, aperçu création/mise à jour.
     */
    public TypologyImportAnalyzeResult analyze(Entity reference, TypologyCsvParser.ParsedCsv parsed,
                                               TypologyImportCollectionProfile collectionProfile) {
        return analyze(reference, parsed, collectionProfile, null);
    }

    /**
     * Analyse avec remontée de progression (optionnelle).
     * Exécutée sur le thread de la requête JSF (contexte JPA / sécurité).
     */
    @Transactional(readOnly = true)
    public TypologyImportAnalyzeResult analyze(Entity reference, TypologyCsvParser.ParsedCsv parsed,
                                               TypologyImportCollectionProfile collectionProfile,
                                               TypologyImportProgress progress) {
        List<String> blocking = new ArrayList<>();
        if (reference == null || reference.getId() == null) {
            blocking.add("Aucun référentiel sélectionné.");
            return new TypologyImportAnalyzeResult(false, blocking, List.of(), parsed);
        }
        if (reference.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_REFERENCE.equals(reference.getEntityType().getCode())) {
            blocking.add("L'entité sélectionnée n'est pas un référentiel.");
            return new TypologyImportAnalyzeResult(false, blocking, List.of(), parsed);
        }
        if (collectionProfile == TypologyImportCollectionProfile.UNSUPPORTED) {
            blocking.add("Typologie de collection non prise en charge pour l'import CSV (référentiels Céramique, Monnaie ou Instrumentum uniquement).");
            return new TypologyImportAnalyzeResult(false, blocking, List.of(), parsed);
        }

        TypologyImportImageUrlCache imageUrlCache = new TypologyImportImageUrlCache();
        TypologyImportLookupCache lookupCache = TypologyImportLookupCache.warm(
                reference, categoryService, groupService, serieService, typeService, entityRepository);

        List<Map<String, String>> rows = parsed.rows();
        int n = rows.size();
        if (progress != null) {
            progress.beginAnalyzing(n);
        }
        List<List<String>> err = new ArrayList<>();
        List<List<String>> warn = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            err.add(new ArrayList<>());
            warn.add(new ArrayList<>());
        }

        TypologyImportKind[] kinds = new TypologyImportKind[n];
        String[] targets = new String[n];
        String[] ccArr = new String[n];
        String[] cgArr = new String[n];
        String[] csArr = new String[n];

        Map<String, Integer> firstOccurrenceIdx = new HashMap<>();

        for (int i = 0; i < n; i++) {
            final int rowIndex = i;
            int csvRowNumber = rowIndex + 2;
            Map<String, String> row = rows.get(rowIndex);
            HierarchyCodes codes = hierarchyCodesFromRow(row);
            ccArr[rowIndex] = codes.categorie() != null ? codes.categorie() : "";
            cgArr[rowIndex] = codes.groupe() != null ? codes.groupe() : "";
            csArr[rowIndex] = codes.serie() != null ? codes.serie() : "";

            Optional<ClassifyResult> clf = classify(codes, err.get(rowIndex));
            if (clf.isEmpty()) {
                kinds[rowIndex] = TypologyImportKind.NON_CLASSIFIE;
                continue;
            }
            kinds[rowIndex] = clf.get().kind();
            targets[rowIndex] = clf.get().targetCode();

            validateCodeFormat(targets[rowIndex], err.get(rowIndex));

            String dupKey = importDuplicateKey(kinds[rowIndex], ccArr[rowIndex], cgArr[rowIndex], csArr[rowIndex], targets[rowIndex]);
            Integer first = firstOccurrenceIdx.putIfAbsent(dupKey, rowIndex);
            if (first != null) {
                err.get(rowIndex).add("Code « " + targets[rowIndex] + " » dupliqué dans le fichier (ligne " + (first + 2) + ").");
                err.get(first).add("Code « " + targets[rowIndex] + " » dupliqué dans le fichier (ligne " + csvRowNumber + ").");
            }

            validateImportScopedCode(lookupCache, kinds[rowIndex], targets[rowIndex], ccArr[rowIndex], cgArr[rowIndex],
                    csArr[rowIndex], err.get(rowIndex));

            previewImages(row, warn.get(rowIndex), imageUrlCache);
            if (collectionProfile == TypologyImportCollectionProfile.MONNAIE) {
                previewOpenThesoMonnaie(row, err.get(rowIndex));
            } else if (collectionProfile == TypologyImportCollectionProfile.INSTRUMENTUM) {
                previewOpenThesoInstrumentum(row, err.get(rowIndex));
            } else {
                previewOpenThesoCeramique(row, err.get(rowIndex));
            }
            previewDatation(row, err.get(rowIndex));
            previewScientificAuthors(row, warn.get(rowIndex));
            if (progress != null) {
                progress.tickAnalyzing(rowIndex, n, csvRowNumber);
            }
        }

        List<Integer> order = sortedRowIndices(n, kinds);
        Set<String> virtualCats = new HashSet<>();
        Set<String> virtualGroups = new HashSet<>();
        Set<String> virtualSeries = new HashSet<>();

        for (int idx : order) {
            if (kinds[idx] == null || targets[idx] == null) {
                continue;
            }
            List<String> e = err.get(idx);
            String cc = ccArr[idx];
            String cg = cgArr[idx];
            String cs = csArr[idx];

            switch (kinds[idx]) {
                case NON_CLASSIFIE -> {
                }
                case CATEGORIE -> virtualCats.add(cc);
                case GROUPE -> {
                    boolean catOk = virtualCats.contains(cc)
                            || lookupCache.findCategory(cc).isPresent();
                    if (!catOk) {
                        e.add("Catégorie « " + cc + " » introuvable sous ce référentiel et non créée dans les lignes précédentes.");
                    }
                    virtualGroups.add(gKey(cc, cg));
                }
                case SERIE -> {
                    boolean gOk = virtualGroups.contains(gKey(cc, cg))
                            || lookupCache.findGroup(cc, cg).isPresent();
                    if (!gOk) {
                        e.add("Groupe « " + cg + " » (catégorie « " + cc + " ») introuvable.");
                    }
                    virtualSeries.add(sKey(cc, cg, cs));
                }
                case TYPE_SOUS_SERIE -> {
                    boolean sOk = virtualSeries.contains(sKey(cc, cg, cs))
                            || lookupCache.findSerie(cc, cg, cs).isPresent();
                    if (!sOk) {
                        e.add("Série « " + cs + " » introuvable pour ce groupe / catégorie.");
                    }
                }
                case TYPE_SOUS_GROUPE -> {
                    boolean gOk = virtualGroups.contains(gKey(cc, cg))
                            || lookupCache.findGroup(cc, cg).isPresent();
                    if (!gOk) {
                        e.add("Groupe « " + cg + " » introuvable pour cette catégorie.");
                    }
                }
                default -> {
                }
            }
        }

        List<TypologyImportPreviewLine> previews = new ArrayList<>(n);
        boolean allOk = blocking.isEmpty();
        for (int i = 0; i < n; i++) {
            int csvRowNumber = i + 2;
            TypologyImportKind k = kinds[i];
            String tgt = targets[i];
            boolean hasErr = !err.get(i).isEmpty();
            boolean create = k != null && k != TypologyImportKind.NON_CLASSIFIE && tgt != null && !hasErr
                    && !importEntityExistsInScope(lookupCache, k, tgt, ccArr[i], cgArr[i], csArr[i]);
            boolean okLine = k != null && k != TypologyImportKind.NON_CLASSIFIE && tgt != null && !hasErr;
            if (!okLine) {
                allOk = false;
            }
            previews.add(new TypologyImportPreviewLine(
                    csvRowNumber,
                    kinds[i] != null ? kinds[i] : TypologyImportKind.NON_CLASSIFIE,
                    tgt != null ? tgt : "",
                    create,
                    okLine,
                    err.get(i),
                    warn.get(i)));
        }

        return new TypologyImportAnalyzeResult(allOk && blocking.isEmpty(), blocking, previews, parsed, imageUrlCache);
    }

    /**
     * Applique l'import (lots transactionnels ; échec possible avec lignes déjà enregistrées).
     */
    public void execute(Entity reference, TypologyCsvParser.ParsedCsv parsed, Utilisateur user,
                        TypologyImportCollectionProfile collectionProfile) {
        failIfNotSuccessful(executeImport(reference, parsed, user, collectionProfile, null, false, null, null));
    }

    /**
     * Applique l'import avec remontée de progression (optionnelle).
     */
    public void execute(Entity reference, TypologyCsvParser.ParsedCsv parsed, Utilisateur user,
                        TypologyImportCollectionProfile collectionProfile, TypologyImportProgress progress) {
        failIfNotSuccessful(executeImport(reference, parsed, user, collectionProfile, progress, false, null, null));
    }

    /**
     * Applique l'import avec progression. Si {@code skipPriorAnalysis}, ne relance pas l'analyse (déjà validée à l'étape 2).
     */
    public void execute(Entity reference, TypologyCsvParser.ParsedCsv parsed, Utilisateur user,
                        TypologyImportCollectionProfile collectionProfile, TypologyImportProgress progress,
                        boolean skipPriorAnalysis) {
        failIfNotSuccessful(executeImport(reference, parsed, user, collectionProfile, progress, skipPriorAnalysis, null, null));
    }

    private static final int IMPORT_BATCH_SIZE = 250;
    private static final int IMPORT_PROGRESS_LOG_EVERY = 250;
    private static final int IMPORT_PROGRESS_PUBLISH_EVERY = 50;
    private static final int IMPORT_FLUSH_EVERY = 50;

    /**
     * Applique l'import par lots avec progression et publication session HTTP pour le poll UI.
     */
    public TypologyImportExecutionResult executeImport(Entity reference, TypologyCsvParser.ParsedCsv parsed, Utilisateur user,
                        TypologyImportCollectionProfile collectionProfile, TypologyImportProgress progress,
                        boolean skipPriorAnalysis, TypologyImportAnalyzeResult priorAnalysis,
                        jakarta.servlet.http.HttpSession httpSession) {
        Entity ref = entityRepository.findById(Objects.requireNonNull(reference.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Référentiel introuvable."));
        if (ref.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_REFERENCE.equals(ref.getEntityType().getCode())) {
            throw new IllegalArgumentException("L'entité n'est pas un référentiel.");
        }
        if (collectionProfile == TypologyImportCollectionProfile.UNSUPPORTED) {
            throw new IllegalArgumentException("Typologie de collection non prise en charge pour l'import CSV.");
        }

        TypologyImportImageUrlCache imageUrlCache = resolveImageUrlCache(skipPriorAnalysis, priorAnalysis);

        if (!skipPriorAnalysis) {
            TypologyImportAnalyzeResult analysis = analyze(ref, parsed, collectionProfile);
            if (!analysis.successful()) {
                throw new IllegalStateException("L'analyse signale des erreurs bloquantes ; import annulé.");
            }
            imageUrlCache = analysis.imageUrlCache() != null ? analysis.imageUrlCache() : imageUrlCache;
        } else if (progress != null) {
            progress.setPreparingImport("Préparation de l'import…");
            publishProgress(httpSession, progress);
        }

        List<Map<String, String>> rows = parsed.rows();
        int n = rows.size();
        TypologyImportKind[] kinds = new TypologyImportKind[n];
        String[] targets = new String[n];

        boolean reusedAnalysis = skipPriorAnalysis
                && fillKindsFromPriorAnalysis(n, kinds, targets, priorAnalysis);
        if (!reusedAnalysis) {
            for (int i = 0; i < n; i++) {
                Map<String, String> row = rows.get(i);
                HierarchyCodes codes = hierarchyCodesFromRow(row);
                Optional<ClassifyResult> clf = classify(codes, new ArrayList<>());
                if (clf.isPresent()) {
                    kinds[i] = clf.get().kind();
                    targets[i] = clf.get().targetCode();
                }
                if (progress != null && (i % 200 == 0 || i == n - 1)) {
                    progress.setPreparingImport("Classification des lignes…");
                    progress.tickImporting(0, n, i + 2);
                    publishProgress(httpSession, progress);
                }
            }
        } else {
            log.info("Import : réutilisation de la classification de l'étape analyse ({} lignes)", n);
        }

        List<Integer> order = sortedRowIndices(n, kinds);
        EntityType catType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_CATEGORY)
                .orElseThrow(() -> new IllegalStateException("Type CATEGORIE manquant."));
        EntityType grpType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_GROUP)
                .orElseThrow(() -> new IllegalStateException("Type GROUPE manquant."));
        EntityType serType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_SERIES)
                .orElseGet(() -> entityTypeRepository.findByCode("SERIE")
                        .orElseThrow(() -> new IllegalStateException("Type SERIE manquant.")));
        EntityType typType = entityTypeRepository.findByCode(EntityConstants.ENTITY_TYPE_TYPE)
                .orElseThrow(() -> new IllegalStateException("Type TYPE manquant."));

        Set<String> csvHeaders = parsed.headerKeySet();

        int importTotal = 0;
        for (int idx : order) {
            if (kinds[idx] != null) {
                importTotal++;
            }
        }
        if (progress != null) {
            if (progress.getPhase() == TypologyImportProgress.Phase.IMPORTING && progress.getTotal() > 0) {
                progress.syncImportTotal(importTotal);
            } else {
                progress.beginImporting(importTotal);
            }
        }

        log.info("Import : enregistrement de {} lignes en lots de {} (référentiel id={})",
                importTotal, IMPORT_BATCH_SIZE, ref.getId());
        if (progress != null) {
            progress.setPreparingImport("Enregistrement en base…");
            publishProgress(httpSession, progress);
        }

        TypologyImportLookupCache lookupCache = TypologyImportLookupCache.warm(
                ref, categoryService, groupService, serieService, typeService, entityRepository);
        TypologyImportAuthorCache authorCache = new TypologyImportAuthorCache(auteurScientifiqueRepository);
        Langue langLabel = resolveLangCode("fr", "fr");
        Langue langDesc = langLabel;
        ImportExecutionContext ctx = new ImportExecutionContext(
                ref.getId(), rows, kinds, targets, order, catType, grpType, serType, typType,
                csvHeaders, collectionProfile, importTotal);

        List<List<Integer>> batches = buildImportBatches(order, kinds);
        int importDone = 0;
        for (List<Integer> batch : batches) {
            final int batchSize = batch.size();
            final TypologyImportImageUrlCache batchImageCache = imageUrlCache;
            try {
                importTransactionTemplate.executeWithoutResult(status ->
                        processImportBatch(ctx, batch, lookupCache, authorCache, batchImageCache,
                                user, langLabel, langDesc));
            } catch (Exception ex) {
                String rootMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                if (importDone > 0) {
                    String partialMsg = importDone + " / " + importTotal
                            + " lignes enregistrées avant l'arrêt : " + rootMessage;
                    log.warn("Import partiel (référentiel id={}) : {}", ref.getId(), partialMsg, ex);
                    if (progress != null) {
                        progress.tickImporting(importDone, importTotal, batch.get(0) + 2);
                        publishProgress(httpSession, progress);
                    }
                    return TypologyImportExecutionResult.partial(
                            importDone, importTotal, batch.get(0) + 2, partialMsg);
                }
                log.error("Import échoué avant toute ligne (référentiel id={})", ref.getId(), ex);
                return TypologyImportExecutionResult.failure(rootMessage);
            }
            entityManager.clear();
            lookupCache.clearEntityInstances();
            for (int idx : batch) {
                importDone++;
                if (progress != null && (importDone % IMPORT_PROGRESS_PUBLISH_EVERY == 0 || importDone == importTotal)) {
                    progress.tickImporting(importDone, importTotal, idx + 2);
                    publishProgress(httpSession, progress);
                }
            }
            if (importDone % IMPORT_PROGRESS_LOG_EVERY == 0 || importDone == importTotal) {
                log.info("Import progression : {}/{} (référentiel id={})", importDone, importTotal, ref.getId());
            }
            log.debug("Import : lot validé (+{} lignes, total {}/{})", batchSize, importDone, importTotal);
        }
        return TypologyImportExecutionResult.ok(importTotal);
    }

    private static void failIfNotSuccessful(TypologyImportExecutionResult result) {
        if (result == null || result.success()) {
            return;
        }
        throw new IllegalStateException(result.message() != null ? result.message() : "Import échoué.");
    }

    private record ImportExecutionContext(
            Long referenceId,
            List<Map<String, String>> rows,
            TypologyImportKind[] kinds,
            String[] targets,
            List<Integer> order,
            EntityType catType,
            EntityType grpType,
            EntityType serType,
            EntityType typType,
            Set<String> csvHeaders,
            TypologyImportCollectionProfile collectionProfile,
            int importTotal) {
    }

    private static List<List<Integer>> buildImportBatches(List<Integer> order, TypologyImportKind[] kinds) {
        List<List<Integer>> batches = new ArrayList<>();
        List<Integer> current = new ArrayList<>(IMPORT_BATCH_SIZE);
        for (int idx : order) {
            if (kinds[idx] == null) {
                continue;
            }
            current.add(idx);
            if (current.size() >= IMPORT_BATCH_SIZE) {
                batches.add(current);
                current = new ArrayList<>(IMPORT_BATCH_SIZE);
            }
        }
        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }

    private static TypologyImportImageUrlCache resolveImageUrlCache(
            boolean skipPriorAnalysis, TypologyImportAnalyzeResult priorAnalysis) {
        if (skipPriorAnalysis && priorAnalysis != null && priorAnalysis.imageUrlCache() != null) {
            return priorAnalysis.imageUrlCache();
        }
        return new TypologyImportImageUrlCache();
    }

    private void processImportBatch(
            ImportExecutionContext ctx,
            List<Integer> batchIndices,
            TypologyImportLookupCache lookupCache,
            TypologyImportAuthorCache authorCache,
            TypologyImportImageUrlCache imageUrlCache,
            Utilisateur user,
            Langue langLabel,
            Langue langDesc) {
        Entity ref = entityRepository.findById(Objects.requireNonNull(ctx.referenceId()))
                .orElseThrow(() -> new IllegalArgumentException("Référentiel introuvable."));
        lookupCache.registerLoadedEntity(ref);
        int flushed = 0;
        for (int idx : batchIndices) {
            Map<String, String> row = ctx.rows().get(idx);
            HierarchyCodes codes = hierarchyCodesFromRow(row);
            applyRow(ref, row, ctx.kinds()[idx], ctx.targets()[idx],
                    codes.categorie(), codes.groupe(), codes.serie(),
                    ctx.catType(), ctx.grpType(), ctx.serType(), ctx.typType(),
                    user, ctx.csvHeaders(), ctx.collectionProfile(), lookupCache, authorCache, imageUrlCache,
                    langLabel, langDesc);
            flushed++;
            if (flushed % IMPORT_FLUSH_EVERY == 0) {
                entityRepository.flush();
            }
        }
    }

    private static void publishProgress(jakarta.servlet.http.HttpSession httpSession, TypologyImportProgress progress) {
        if (httpSession != null && progress != null) {
            TypologyImportProgressSession.publish(httpSession, progress);
        }
    }

    private static boolean fillKindsFromPriorAnalysis(
            int rowCount,
            TypologyImportKind[] kinds,
            String[] targets,
            TypologyImportAnalyzeResult priorAnalysis) {
        if (priorAnalysis == null || priorAnalysis.previewLines() == null) {
            return false;
        }
        List<TypologyImportPreviewLine> lines = priorAnalysis.previewLines();
        if (lines.size() != rowCount) {
            return false;
        }
        for (int i = 0; i < rowCount; i++) {
            TypologyImportPreviewLine line = lines.get(i);
            if (line != null && line.lineOk() && line.kind() != null
                    && line.kind() != TypologyImportKind.NON_CLASSIFIE
                    && line.targetCode() != null && !line.targetCode().isBlank()) {
                kinds[i] = line.kind();
                targets[i] = line.targetCode();
            }
        }
        return true;
    }

    private void applyRow(Entity reference, Map<String, String> row, TypologyImportKind kind, String targetCode,
                          String cc, String cg, String cs,
                          EntityType catType, EntityType grpType, EntityType serType, EntityType typType,
                          Utilisateur user, Set<String> csvHeaders,
                          TypologyImportCollectionProfile collectionProfile,
                          TypologyImportLookupCache lookupCache,
                          TypologyImportAuthorCache authorCache,
                          TypologyImportImageUrlCache imageUrlCache,
                          Langue langLabel, Langue langDesc) {

        String statutStr = null;

        switch (kind) {
            case CATEGORIE -> {
                Entity saved = upsertCategory(reference, targetCode, langLabel, langDesc,
                        statutStr, row, catType, user, csvHeaders, collectionProfile,
                        lookupCache, authorCache, imageUrlCache);
                lookupCache.registerCategory(saved);
            }
            case GROUPE -> {
                Entity cat = lookupCache.findCategory(cc).orElseThrow();
                Entity saved = upsertChild(cat, targetCode, EntityConstants.ENTITY_TYPE_GROUP, grpType,
                        langLabel, langDesc, statutStr, row, user, csvHeaders, collectionProfile,
                        lookupCache, authorCache, imageUrlCache);
                lookupCache.registerGroup(cc, saved);
            }
            case SERIE -> {
                Entity grp = lookupCache.findGroup(cc, cg).orElseThrow();
                Entity saved = upsertChild(grp, targetCode, EntityConstants.ENTITY_TYPE_SERIES, serType,
                        langLabel, langDesc, statutStr, row, user, csvHeaders, collectionProfile,
                        lookupCache, authorCache, imageUrlCache);
                lookupCache.registerSerie(cc, cg, saved);
            }
            case TYPE_SOUS_SERIE -> {
                Entity serie = lookupCache.findSerie(cc, cg, cs).orElseThrow();
                Entity saved = upsertChild(serie, targetCode, EntityConstants.ENTITY_TYPE_TYPE, typType,
                        langLabel, langDesc, statutStr, row, user, csvHeaders, collectionProfile,
                        lookupCache, authorCache, imageUrlCache);
                lookupCache.registerType(cc, cg, saved);
            }
            case TYPE_SOUS_GROUPE -> {
                Entity grp = lookupCache.findGroup(cc, cg).orElseThrow();
                Entity saved = upsertChild(grp, targetCode, EntityConstants.ENTITY_TYPE_TYPE, typType,
                        langLabel, langDesc, statutStr, row, user, csvHeaders, collectionProfile,
                        lookupCache, authorCache, imageUrlCache);
                lookupCache.registerType(cc, cg, saved);
            }
            case NON_CLASSIFIE ->
                    throw new IllegalStateException("Ligne non classifiable : ne doit pas être importée.");
        }
    }

    private Entity upsertCategory(Entity reference, String code,
                                Langue langLabel, Langue langDesc, String statutStr,
                                Map<String, String> row, EntityType catType, Utilisateur user,
                                Set<String> csvHeaders, TypologyImportCollectionProfile collectionProfile,
                                TypologyImportLookupCache lookupCache,
                                TypologyImportAuthorCache authorCache,
                                TypologyImportImageUrlCache imageUrlCache) {
        Optional<Long> existingId = lookupCache.findEntityIdByCategoryCode(code);
        boolean isCreate = existingId.isEmpty();
        Entity entity;
        if (existingId.isEmpty()) {
            entity = new Entity();
            entity.setEntityType(catType);
            entity.setCode(code);
            entity.setCreateDate(LocalDateTime.now());
            attachAuthor(entity, user);
            entity.setStatut(statutStr != null ? statutStr : EntityStatusEnum.PROPOSITION.name());
            entity = entityRepository.save(entity);
            linkParentChild(reference, entity, lookupCache);
        } else {
            entity = loadEntityForImport(existingId.get(), lookupCache);
            entity.setStatut(statutStr != null ? statutStr : entity.getStatut());
            if (!lookupCache.isKnownLinked(reference.getId(), entity.getId())
                    && !entityRelationRepository.existsByParentAndChild(reference.getId(), entity.getId())) {
                linkParentChild(reference, entity, lookupCache);
            }
        }
        applyFrenchPrimaryLabelsDescriptions(entity, code, langLabel, langDesc, row, csvHeaders, isCreate);
        replaceLocalizedTexts(entity, row, csvHeaders, isCreate);
        replaceMetadata(entity, row, csvHeaders, isCreate);
        replaceImages(entity, row, csvHeaders, isCreate, imageUrlCache);
        replaceOpenTheso(entity, row, csvHeaders, isCreate);
        applyTypologySpecificDetails(entity, row, csvHeaders, isCreate, collectionProfile);
        replaceAuteursScientifiques(entity, row, csvHeaders, authorCache);
        return entityRepository.save(entity);
    }

    private Entity resolveLookupParent(Entity parent, String expectedTypeCode) {
        if (EntityConstants.ENTITY_TYPE_TYPE.equals(expectedTypeCode)) {
            return entityCodeUniquenessService.resolveGroup(parent).orElse(parent);
        }
        return parent;
    }

    private Entity upsertChild(Entity parent, String code, String expectedTypeCode, EntityType concreteType,
                             Langue langLabel, Langue langDesc, String statutStr,
                             Map<String, String> row, Utilisateur user, Set<String> csvHeaders,
                             TypologyImportCollectionProfile collectionProfile,
                             TypologyImportLookupCache lookupCache,
                             TypologyImportAuthorCache authorCache,
                             TypologyImportImageUrlCache imageUrlCache) {
        Optional<Long> existingId = lookupCache.findExistingChildEntityId(
                resolveLookupParent(parent, expectedTypeCode), expectedTypeCode, code);
        boolean isCreate = existingId.isEmpty();
        Entity entity;
        if (existingId.isEmpty()) {
            assertImportCodeAvailableOnCreate(
                    resolveLookupParent(parent, expectedTypeCode), expectedTypeCode, code, lookupCache);
            entity = new Entity();
            entity.setEntityType(concreteType);
            entity.setCode(code);
            entity.setCreateDate(LocalDateTime.now());
            attachAuthor(entity, user);
            entity.setStatut(statutStr != null ? statutStr : EntityStatusEnum.PROPOSITION.name());
            entity = entityRepository.save(entity);
            linkParentChild(parent, entity, lookupCache);
        } else {
            entity = loadEntityForImport(existingId.get(), lookupCache);
            if (entity.getEntityType() == null || !expectedTypeCode.equals(entity.getEntityType().getCode())) {
                throw new IllegalStateException("Le code « " + code + " » existe avec un autre type d'entité.");
            }
            Optional<Long> knownParentId = lookupCache.getKnownParentId(entity.getId());
            if (knownParentId.isPresent()) {
                if (!Objects.equals(knownParentId.get(), parent.getId())) {
                    typeService.changeTypeParent(entity, parent);
                    entity = loadEntityForImport(entity.getId(), lookupCache);
                }
            } else {
                Entity currentParent = entityRelationRepository.findParentsByChild(entity).stream()
                        .findFirst().orElse(null);
                if (currentParent == null || !Objects.equals(currentParent.getId(), parent.getId())) {
                    typeService.changeTypeParent(entity, parent);
                    entity = loadEntityForImport(entity.getId(), lookupCache);
                }
                lookupCache.registerParentChild(parent.getId(), entity.getId());
            }
            entity.setStatut(statutStr != null ? statutStr : entity.getStatut());
        }
        applyFrenchPrimaryLabelsDescriptions(entity, code, langLabel, langDesc, row, csvHeaders, isCreate);
        replaceLocalizedTexts(entity, row, csvHeaders, isCreate);
        replaceMetadata(entity, row, csvHeaders, isCreate);
        replaceImages(entity, row, csvHeaders, isCreate, imageUrlCache);
        replaceOpenTheso(entity, row, csvHeaders, isCreate);
        applyTypologySpecificDetails(entity, row, csvHeaders, isCreate, collectionProfile);
        replaceAuteursScientifiques(entity, row, csvHeaders, authorCache);
        arkIdentifierService.ensureArkIfAbsentForPublishedTypologyEntity(entity);
        return entityRepository.save(entity);
    }

    private void applyTypologySpecificDetails(Entity entity, Map<String, String> row, Set<String> csvHeaders,
                                              boolean isCreate, TypologyImportCollectionProfile collectionProfile) {
        if (collectionProfile == TypologyImportCollectionProfile.MONNAIE) {
            replaceMonnaieDetails(entity, row, csvHeaders, isCreate);
        } else if (collectionProfile == TypologyImportCollectionProfile.INSTRUMENTUM) {
            replaceInstrumentumDetails(entity, row, csvHeaders, isCreate);
        } else {
            replaceCeramiqueDetails(entity, row, csvHeaders, isCreate);
        }
    }

    private void replaceInstrumentumDetails(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        applyDescriptionDetailInstrumentum(entity, row, csvHeaders, isCreate);
        applyInstrumentumOpenthesoReferenceFields(entity, row, csvHeaders, isCreate);
        applyCaracteristiquePhysiqueInstrumentum(entity, row, csvHeaders, isCreate);
        replaceAiresCirculation(entity, row, csvHeaders, isCreate);
    }

    private void applyDescriptionDetailInstrumentum(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        boolean touchDecors = shouldWriteField(csvHeaders, TypologyImportCeramiqueConstants.COL_DESCRIPTION_DECORS, row, isCreate);
        boolean touchMarques = shouldWriteField(csvHeaders, TypologyImportCeramiqueConstants.COL_DESCRIPTION_MARQUES, row, isCreate);
        if (!touchDecors && !touchMarques) {
            return;
        }
        DescriptionDetail dd = entity.getDescriptionDetail();
        if (dd == null) {
            dd = new DescriptionDetail();
            dd.setEntity(entity);
            entity.setDescriptionDetail(dd);
        }
        if (touchDecors) {
            dd.setDecors(trimToNull(getCell(row, TypologyImportCeramiqueConstants.COL_DESCRIPTION_DECORS)));
        }
        if (touchMarques) {
            dd.setMarques(listToSemicolon(getCell(row, TypologyImportCeramiqueConstants.COL_DESCRIPTION_MARQUES), false));
        }
    }

    /**
     * Catégorie fonctionnelle (lien entité) ; relation d'imitation et dénomination (métadonnées texte alimentées depuis OpenTheso).
     */
    private void applyInstrumentumOpenthesoReferenceFields(Entity entity, Map<String, String> row, Set<String> csvHeaders,
                                                            boolean isCreate) {
        boolean touchCat = shouldWriteField(csvHeaders, TypologyImportInstrumentumConstants.COL_DESCRIPTION_CATEGORIE_FONCTIONNELLE, row, isCreate);
        boolean touchRel = shouldWriteField(csvHeaders, TypologyImportInstrumentumConstants.COL_DESCRIPTION_RELATION_IMITATION, row, isCreate);
        boolean touchDen = shouldWriteField(csvHeaders, TypologyImportInstrumentumConstants.COL_DESCRIPTION_DENOMINATION, row, isCreate);
        if (!touchCat && !touchRel && !touchDen) {
            return;
        }
        if (touchCat) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportInstrumentumConstants.COL_DESCRIPTION_CATEGORIE_FONCTIONNELLE),
                    ReferenceOpenthesoEnum.CATEGORIE_FONCTIONNELLE.name());
            entity.setCategorieFonctionnelle(r);
        }
        if (touchRel) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportInstrumentumConstants.COL_DESCRIPTION_RELATION_IMITATION),
                    TypologyImportInstrumentumConstants.OPENTHESO_CODE_RELATION_IMITATION);
            entity.setRelationImitation(r != null ? r.getValeur() : null);
        }
        if (touchDen) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportInstrumentumConstants.COL_DESCRIPTION_DENOMINATION),
                    ReferenceOpenthesoEnum.DENOMINATION.name());
            entity.setDenominationInstrumentum(r != null ? r.getValeur() : null);
        }
    }

    private void applyCaracteristiquePhysiqueInstrumentum(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        boolean m = shouldWriteField(csvHeaders, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_MATERIAUX, row, isCreate);
        boolean f = shouldWriteField(csvHeaders, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_FORME, row, isCreate);
        boolean d = shouldWriteField(csvHeaders, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_DIMENSIONS, row, isCreate);
        boolean t = shouldWriteField(csvHeaders, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_TECHNIQUE, row, isCreate);
        boolean fab = shouldWriteField(csvHeaders, TypologyImportConstants.COL_CARACT_PHYS_FABRICATION, row, isCreate);
        if (!m && !f && !d && !t && !fab) {
            return;
        }
        CaracteristiquePhysique cp = entity.getCaracteristiquePhysique();
        if (cp == null) {
            cp = new CaracteristiquePhysique();
            cp.setEntity(entity);
            entity.setCaracteristiquePhysique(cp);
        }
        if (m) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_MATERIAUX),
                    ReferenceOpenthesoEnum.MATERIAUX.name());
            cp.setMateriaux(r);
        }
        if (f) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_FORME),
                    ReferenceOpenthesoEnum.FORME.name());
            cp.setForme(r);
        }
        if (d) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_DIMENSIONS),
                    ReferenceOpenthesoEnum.DIMENSIONS.name());
            cp.setDimensions(r);
        }
        if (t) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_TECHNIQUE),
                    ReferenceOpenthesoEnum.TECHNIQUE.name());
            cp.setTechnique(r);
        }
        if (fab) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_CARACT_PHYS_FABRICATION),
                    ReferenceOpenthesoEnum.FABRICATION_FACONNAGE.name());
            entity.setFabricationFaconnage(r);
        }
    }

    private void replaceMonnaieDetails(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        applyDescriptionMonnaie(entity, row, csvHeaders, isCreate);
        applyCaracteristiquePhysiqueMonnaie(entity, row, csvHeaders, isCreate);
        replaceAiresCirculation(entity, row, csvHeaders, isCreate);
    }

    private void applyDescriptionMonnaie(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        boolean tDroit = shouldWriteField(csvHeaders, TypologyImportMonnaieConstants.COL_DESCRIPTION_DROIT, row, isCreate);
        boolean tLegD = shouldWriteField(csvHeaders, TypologyImportMonnaieConstants.COL_DESCRIPTION_LEGENDE_DROIT, row, isCreate);
        boolean tRev = shouldWriteField(csvHeaders, TypologyImportMonnaieConstants.COL_DESCRIPTION_REVERS, row, isCreate);
        boolean tLegR = shouldWriteField(csvHeaders, TypologyImportMonnaieConstants.COL_DESCRIPTION_LEGENDE_REVERS, row, isCreate);
        if (!tDroit && !tLegD && !tRev && !tLegR) {
            return;
        }
        DescriptionMonnaie dm = entity.getDescriptionMonnaie();
        if (dm == null) {
            dm = new DescriptionMonnaie();
            dm.setEntity(entity);
            entity.setDescriptionMonnaie(dm);
        }
        if (tDroit) {
            dm.setDroit(trimToNull(getCell(row, TypologyImportMonnaieConstants.COL_DESCRIPTION_DROIT)));
        }
        if (tLegD) {
            dm.setLegendeDroit(trimToNull(getCell(row, TypologyImportMonnaieConstants.COL_DESCRIPTION_LEGENDE_DROIT)));
        }
        if (tRev) {
            dm.setRevers(trimToNull(getCell(row, TypologyImportMonnaieConstants.COL_DESCRIPTION_REVERS)));
        }
        if (tLegR) {
            dm.setLegendeRevers(trimToNull(getCell(row, TypologyImportMonnaieConstants.COL_DESCRIPTION_LEGENDE_REVERS)));
        }
    }

    private void applyCaracteristiquePhysiqueMonnaie(Entity entity, Map<String, String> row, Set<String> csvHeaders,
                                                     boolean isCreate) {
        boolean touchMat = shouldWriteField(csvHeaders, TypologyImportMonnaieConstants.COL_CARACT_PHYS_MATERIAU, row, isCreate);
        boolean touchDen = shouldWriteField(csvHeaders, TypologyImportMonnaieConstants.COL_CARACT_PHYS_DENOMINATION, row, isCreate);
        boolean touchMet = shouldWriteField(csvHeaders, TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE, row, isCreate);
        boolean touchVal = shouldWriteField(csvHeaders, TypologyImportMonnaieConstants.COL_CARACT_PHYS_VALEUR, row, isCreate);
        boolean touchTec = shouldWriteField(csvHeaders, TypologyImportMonnaieConstants.COL_CARACT_PHYS_TECHNIQUE, row, isCreate);
        if (!touchMat && !touchDen && !touchMet && !touchVal && !touchTec) {
            return;
        }
        CaracteristiquePhysiqueMonnaie cpm = entity.getCaracteristiquePhysiqueMonnaie();
        if (cpm == null) {
            cpm = new CaracteristiquePhysiqueMonnaie();
            cpm.setEntity(entity);
            entity.setCaracteristiquePhysiqueMonnaie(cpm);
        }
        if (touchMat) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_MATERIAU),
                    ReferenceOpenthesoEnum.MATERIAUX.name());
            cpm.setMateriaux(r);
        }
        if (touchDen) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_DENOMINATION),
                    ReferenceOpenthesoEnum.DENOMINATION.name());
            cpm.setDenomination(r);
        }
        if (touchMet) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE),
                    ReferenceOpenthesoEnum.METROLOGIE.name());
            cpm.setMetrologie(r != null ? r.getValeur() : null);
        }
        if (touchVal) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_VALEUR),
                    ReferenceOpenthesoEnum.VALEUR.name());
            cpm.setValeur(r);
        }
        if (touchTec) {
            ReferenceOpentheso r = saveReferenceForEntity(entity, getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_TECHNIQUE),
                    ReferenceOpenthesoEnum.TECHNIQUE.name());
            cpm.setTechnique(r);
        }
    }

    private void applyFrenchPrimaryLabelsDescriptions(Entity entity, String code, Langue langLabel, Langue langDesc,
                                                      Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        if (isCreate) {
            String labelFr = trimToNull(getCell(row, TypologyImportConstants.COL_NOM_COMPLET_FR));
            if (!columnInCsv(csvHeaders, TypologyImportConstants.COL_NOM_COMPLET_FR) || !StringUtils.hasText(labelFr)) {
                labelFr = code;
            }
            replaceLabels(entity, langLabel, labelFr);
            if (columnInCsv(csvHeaders, TypologyImportConstants.COL_DESCRIPTION_FR)) {
                replaceDescriptions(entity, langDesc, trimToNull(getCell(row, TypologyImportConstants.COL_DESCRIPTION_FR)));
            }
        } else {
            if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_NOM_COMPLET_FR, row, false)) {
                replaceLabels(entity, langLabel, trimToNull(getCell(row, TypologyImportConstants.COL_NOM_COMPLET_FR)));
            }
            if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_DESCRIPTION_FR, row, false)) {
                replaceDescriptions(entity, langDesc, trimToNull(getCell(row, TypologyImportConstants.COL_DESCRIPTION_FR)));
            }
        }
    }

    private void replaceLocalizedTexts(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        Langue langEn = resolveLangCode("en", "fr");
        if (isCreate || columnInCsv(csvHeaders, TypologyImportConstants.COL_NOM_COMPLET_EN)) {
            String labelEn = trimToNull(getCell(row, TypologyImportConstants.COL_NOM_COMPLET_EN));
            if (isCreate || StringUtils.hasText(labelEn)) {
                if (StringUtils.hasText(labelEn)) {
                    replaceLabels(entity, langEn, labelEn);
                }
            }
        }
        if (isCreate || columnInCsv(csvHeaders, TypologyImportConstants.COL_DESCRIPTION_EN)) {
            String descEn = trimToNull(getCell(row, TypologyImportConstants.COL_DESCRIPTION_EN));
            if (isCreate || StringUtils.hasText(descEn)) {
                if (StringUtils.hasText(descEn)) {
                    replaceDescriptions(entity, langEn, descEn);
                }
            }
        }
    }

    private void attachAuthor(Entity entity, Utilisateur user) {
        if (user != null) {
            entity.setCreateBy(user.getEmail());
            ArrayList<Utilisateur> a = new ArrayList<>();
            a.add(user);
            entity.setAuteurs(a);
        }
    }

    private void linkParentChild(Entity parent, Entity child, TypologyImportLookupCache lookupCache) {
        if (lookupCache.isKnownLinked(parent.getId(), child.getId())) {
            return;
        }
        if (!entityRelationRepository.existsByParentAndChild(parent.getId(), child.getId())) {
            EntityRelation rel = new EntityRelation();
            rel.setParent(parent);
            rel.setChild(child);
            entityRelationRepository.save(rel);
        }
        lookupCache.registerParentChild(parent.getId(), child.getId());
    }

    private void replaceLabels(Entity entity, Langue lang, String nom) {
        if (entity.getLabels() == null) {
            entity.setLabels(new ArrayList<>());
        }
        entity.getLabels().removeIf(l -> l.getLangue() != null && Objects.equals(l.getLangue().getCode(), lang.getCode()));
        if (StringUtils.hasText(nom)) {
            Label lb = new Label();
            lb.setEntity(entity);
            lb.setLangue(lang);
            lb.setNom(nom.trim());
            entity.getLabels().add(lb);
        }
    }

    private void replaceDescriptions(Entity entity, Langue lang, String text) {
        if (entity.getDescriptions() == null) {
            entity.setDescriptions(new ArrayList<>());
        }
        entity.getDescriptions().removeIf(d -> d.getLangue() != null && Objects.equals(d.getLangue().getCode(), lang.getCode()));
        if (StringUtils.hasText(text)) {
            Description d = new Description();
            d.setEntity(entity);
            d.setLangue(lang);
            d.setValeur(text.trim());
            entity.getDescriptions().add(d);
        }
    }

    private void replaceMetadata(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_COMMENTAIRE, row, isCreate)) {
            entity.setMetadataCommentaire(trimToNull(getCell(row, TypologyImportConstants.COL_COMMENTAIRE)));
        }
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_DATATION_COMMENTAIRE, row, isCreate)) {
            entity.setCommentaireDatation(trimToNull(getCell(row, TypologyImportConstants.COL_DATATION_COMMENTAIRE)));
        }
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_DATATION_TPQ, row, isCreate)) {
            entity.setTpq(parseIntegerCell(getCell(row, TypologyImportConstants.COL_DATATION_TPQ)));
        }
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_DATATION_TAQ, row, isCreate)) {
            entity.setTaq(parseIntegerCell(getCell(row, TypologyImportConstants.COL_DATATION_TAQ)));
        }
        replaceAppellationsUsuelles(entity, row, csvHeaders, isCreate);
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_REFERENCES_TYPOLOGIE_SCIENTIFIQUE, row, isCreate)) {
            entity.setTypologieScientifique(trimToNull(getCell(row, TypologyImportConstants.COL_REFERENCES_TYPOLOGIE_SCIENTIFIQUE)));
        }
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_REFERENCES_REFERENTIEL, row, isCreate)) {
            entity.setReference(listToSemicolon(getCell(row, TypologyImportConstants.COL_REFERENCES_REFERENTIEL), true));
        }
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_EXTERNE, row, isCreate)) {
            entity.setAlignementExterne(trimToNull(getCell(row, TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_EXTERNE)));
        }
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_INTERNE, row, isCreate)) {
            entity.setInterne(trimToNull(getCell(row, TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_INTERNE)));
        }
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_ATTESTATIONS_VALEUR, row, isCreate)) {
            entity.setAttestations(listToSemicolon(getCell(row, TypologyImportConstants.COL_ATTESTATIONS_VALEUR), false));
        }
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_ATTESTATIONS_SITES_ARCHEOLOGIQUES, row, isCreate)) {
            entity.setSitesArcheologiques(listToSemicolon(getCell(row, TypologyImportConstants.COL_ATTESTATIONS_SITES_ARCHEOLOGIQUES), false));
        }
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_PRODUCTION_ATELIERS, row, isCreate)) {
            entity.setAteliers(listToSemicolon(getCell(row, TypologyImportConstants.COL_PRODUCTION_ATELIERS), false));
        }
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_ATTESTATIONS_CORPUS_LIE, row, isCreate)) {
            entity.setCorpusLies(corpusLinksToStorage(getCell(row, TypologyImportConstants.COL_ATTESTATIONS_CORPUS_LIE)));
        }
    }

    /**
     * Corpus liés : liste d'entrées {@code libellé|url} séparées par {@code ||} dans le CSV.
     * Stockage en base : {@code libellé|url; libellé|url} (séparateur {@code ;}).
     * Déduplique sur le couple (libellé, url) insensible à la casse.
     */
    private String corpusLinksToStorage(String raw) {
        String t = trimToNull(raw);
        if (!StringUtils.hasText(t)) {
            return null;
        }
        String normalized = t.replace("##", "||");
        String[] parts = LIST_SPLIT.split(normalized);
        ArrayList<String> unique = new ArrayList<>();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) continue;
            String entry = part.trim();
            if (!StringUtils.hasText(entry)) continue;
            String[] pair = entry.split("\\|", 2);
            if (pair.length < 2) {
                continue;
            }
            String label = pair[0] != null ? pair[0].trim() : "";
            String url = pair[1] != null ? pair[1].trim() : "";
            if (!StringUtils.hasText(label) || !StringUtils.hasText(url)) {
                continue;
            }
            boolean dup = unique.stream().anyMatch(u -> {
                String[] up = u.split("\\|", 2);
                if (up.length < 2) return false;
                return up[0].trim().equalsIgnoreCase(label) && up[1].trim().equalsIgnoreCase(url);
            });
            if (!dup) {
                unique.add(label.replace("|", " ").replace(";", " ").trim() + "|" + url.replace("|", "%7C").replace(";", "%3B").trim());
            }
        }
        if (unique.isEmpty()) {
            return null;
        }
        return String.join("; ", unique);
    }

    /**
     * Champs listes : le CSV utilise {@link TypologyImportConstants#LIST_SEPARATOR} ; la base attend des {@code ;}
     * (aligné sur l'UI attestations / corpus / etc.).
     */
    private String listToSemicolon(String raw, boolean referencesFormat) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.replace("##", "||");
        String[] parts = LIST_SPLIT.split(normalized);
        ArrayList<String> trimmed = new ArrayList<>();
        for (String part : parts) {
            String t = part.trim();
            if (!StringUtils.hasText(t)) {
                continue;
            }
            if (referencesFormat) {
                String[] pair = splitPair(t);
                t = pair[0];
            } else if (t.contains(":")) {
                t = String.join(";", splitColonValues(t));
            }
            if (StringUtils.hasText(t)) {
                trimmed.add(t);
            }
        }
        if (trimmed.isEmpty()) {
            return null;
        }
        return String.join(";", trimmed);
    }

    private static Integer parseIntegerCell(String raw) {
        String t = trimToNull(raw);
        if (t == null) {
            return null;
        }
        return Integer.parseInt(t.trim());
    }

    private void replaceImages(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate,
                               TypologyImportImageUrlCache imageUrlCache) {
        if (!columnInCsv(csvHeaders, TypologyImportConstants.COL_ILLUSTRATIONS)) {
            return;
        }
        String raw = getCell(row, TypologyImportConstants.COL_ILLUSTRATIONS);
        if (!isCreate && !StringUtils.hasText(raw)) {
            return;
        }
        if (entity.getImages() == null) {
            entity.setImages(new ArrayList<>());
        }
        entity.getImages().clear();
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String normalized = raw.replace("##", "||");
        for (String part : LIST_SPLIT.split(normalized)) {
            String token = part.trim();
            String legende = "";
            String url = token;
            if (token.contains(":")) {
                String[] pair = splitPair(token);
                legende = pair[0];
                url = pair[1];
            }
            if (!StringUtils.hasText(url)) {
                continue;
            }
            if (!imageUrlCache.isValidForImport(url)) {
                continue;
            }
            Image img = new Image();
            img.setEntity(entity);
            img.setUrl(url);
            img.setLegende(legende != null ? legende : "");
            entity.getImages().add(img);
        }
    }

    private void replaceOpenTheso(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        if (columnInCsv(csvHeaders, TypologyImportConstants.COL_DATATION_PERIODE)) {
            if (isCreate || StringUtils.hasText(getCell(row, TypologyImportConstants.COL_DATATION_PERIODE))) {
                String[] periode = parseLabelUrl(getCell(row, TypologyImportConstants.COL_DATATION_PERIODE));
                applySlot(entity,
                        periode[1],
                        periode[0],
                        ReferenceOpenthesoEnum.PERIODE,
                        entity::getPeriode,
                        entity::setPeriode);
            }
        } else if (isCreate) {
            String[] periode = parseLabelUrl(getCell(row, TypologyImportConstants.COL_DATATION_PERIODE));
            applySlot(entity,
                    periode[1],
                    periode[0],
                    ReferenceOpenthesoEnum.PERIODE,
                    entity::getPeriode,
                    entity::setPeriode);
        }

        replaceProductions(entity, row, csvHeaders, isCreate);

        if (isCreate) {
            applySlot(entity, null, null, ReferenceOpenthesoEnum.CATEGORIE_FONCTIONNELLE, entity::getCategorieFonctionnelle, entity::setCategorieFonctionnelle);
        }
    }

    private void replaceCeramiqueDetails(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        applyDescriptionDetail(entity, row, csvHeaders, isCreate);
        applyCaracteristiquePhysique(entity, row, csvHeaders, isCreate);
        applyDescriptionPate(entity, row, csvHeaders, isCreate);
        replaceAiresCirculation(entity, row, csvHeaders, isCreate);
        replaceFonctionsUsage(entity, row, csvHeaders, isCreate);
    }

    private void applyDescriptionDetail(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        boolean touchDecors = shouldWriteField(csvHeaders, TypologyImportCeramiqueConstants.COL_DESCRIPTION_DECORS, row, isCreate);
        boolean touchMarques = shouldWriteField(csvHeaders, TypologyImportCeramiqueConstants.COL_DESCRIPTION_MARQUES, row, isCreate);
        if (!touchDecors && !touchMarques) {
            return;
        }
        DescriptionDetail dd = entity.getDescriptionDetail();
        if (dd == null) {
            dd = new DescriptionDetail();
            dd.setEntity(entity);
            entity.setDescriptionDetail(dd);
        }
        if (touchDecors) {
            dd.setDecors(trimToNull(getCell(row, TypologyImportCeramiqueConstants.COL_DESCRIPTION_DECORS)));
        }
        if (touchMarques) {
            dd.setMarques(listToSemicolon(getCell(row, TypologyImportCeramiqueConstants.COL_DESCRIPTION_MARQUES), false));
        }
    }

    private void applyCaracteristiquePhysique(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        boolean touchForme = shouldWriteField(csvHeaders, TypologyImportCeramiqueConstants.COL_DESCRIPTION_FORM, row, isCreate);
        boolean touchMetro = shouldWriteField(csvHeaders, TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE, row, isCreate);
        boolean touchFab = shouldWriteField(csvHeaders, TypologyImportConstants.COL_CARACT_PHYS_FABRICATION, row, isCreate);
        if (!touchForme && !touchMetro && !touchFab) {
            return;
        }
        CaracteristiquePhysique cp = entity.getCaracteristiquePhysique();
        if (cp == null) {
            cp = new CaracteristiquePhysique();
            cp.setEntity(entity);
            entity.setCaracteristiquePhysique(cp);
        }
        if (touchForme) {
            cp.setForme(saveReferenceForEntity(entity, getCell(row, TypologyImportCeramiqueConstants.COL_DESCRIPTION_FORM), "FORME"));
        }
        if (touchMetro) {
            cp.setMetrologie(saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE), "METROLOGIE"));
        }
        if (touchFab) {
            entity.setFabricationFaconnage(saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_CARACT_PHYS_FABRICATION),
                    ReferenceOpenthesoEnum.FABRICATION_FACONNAGE.name()));
        }
    }

    private void applyDescriptionPate(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        boolean touchDesc = shouldWriteField(csvHeaders, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_DESCRIPTION_PATE, row, isCreate);
        boolean touchCouleur = shouldWriteField(csvHeaders, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_COULEUR_PATE, row, isCreate);
        boolean touchNature = shouldWriteField(csvHeaders, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_NATURE_PATE, row, isCreate);
        boolean touchIncl = shouldWriteField(csvHeaders, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_INCLUSION, row, isCreate);
        boolean touchCuisson = shouldWriteField(csvHeaders, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_CUISSON, row, isCreate);
        if (!touchDesc && !touchCouleur && !touchNature && !touchIncl && !touchCuisson) {
            return;
        }
        DescriptionPate dp = entity.getDescriptionPate();
        if (dp == null) {
            dp = new DescriptionPate();
            dp.setEntity(entity);
            entity.setDescriptionPate(dp);
        }
        if (touchDesc) {
            dp.setDescription(trimToNull(getCell(row, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_DESCRIPTION_PATE)));
        }
        if (touchCouleur) {
            entity.setCouleurPate(saveReferenceForEntity(entity, getCell(row, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_COULEUR_PATE),
                    ReferenceOpenthesoEnum.COULEUR_PATE.name()));
        }
        if (touchNature) {
            entity.setNaturePate(saveReferenceForEntity(entity, getCell(row, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_NATURE_PATE),
                    ReferenceOpenthesoEnum.NATURE_PATE.name()));
        }
        if (touchIncl) {
            entity.setInclusionPate(saveReferenceForEntity(entity, getCell(row, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_INCLUSION),
                    ReferenceOpenthesoEnum.INCLUSIONS.name()));
        }
        if (touchCuisson) {
            entity.setCuissonPostCuissonRef(saveReferenceForEntity(entity, getCell(row, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_CUISSON),
                    ReferenceOpenthesoEnum.CUISSON_POST_CUISSON.name()));
        }
    }

    private void replaceAppellationsUsuelles(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        if (!columnInCsv(csvHeaders, TypologyImportConstants.COL_APPELLATION_USUELLE)) {
            return;
        }
        String raw = trimToNull(getCell(row, TypologyImportConstants.COL_APPELLATION_USUELLE));
        if (!isCreate && raw == null) {
            return;
        }
        if (entity.getAppellationsUsuelles() == null) {
            entity.setAppellationsUsuelles(new ArrayList<>());
        }
        entity.getAppellationsUsuelles().clear();
        if (raw == null) {
            return;
        }
        String normalized = raw.replace("##", "||");
        for (String token : LIST_SPLIT.split(normalized)) {
            String t = token.trim();
            if (!StringUtils.hasText(t)) {
                continue;
            }
            String[] pair = splitPair(t);
            String valeur;
            String urlToken = StringUtils.hasText(pair[1]) ? pair[1].trim() : null;
            if (StringUtils.hasText(urlToken)) {
                valeur = limitVarcharColumn(firstNonBlank(pair[0], pair[1]));
            } else if (StringUtils.hasText(pair[0])) {
                valeur = limitVarcharColumn(pair[0].trim());
                urlToken = null;
            } else {
                continue;
            }
            ReferenceOpentheso ref = ReferenceOpentheso.builder()
                    .code(ReferenceOpenthesoEnum.APPELLATION_USUELLE.name())
                    .valeur(valeur)
                    .url(urlToken != null ? limitVarcharColumn(urlToken) : null)
                    .entity(entity)
                    .build();
            entity.getAppellationsUsuelles().add(referenceOpenthesoRepository.save(ref));
        }
    }

    private void replaceAiresCirculation(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        if (!columnInCsv(csvHeaders, TypologyImportConstants.COL_PRODUCTION_AIRE_CIRCULATION)) {
            return;
        }
        String raw = trimToNull(getCell(row, TypologyImportConstants.COL_PRODUCTION_AIRE_CIRCULATION));
        if (!isCreate && raw == null) {
            return;
        }
        if (entity.getAiresCirculation() == null) {
            entity.setAiresCirculation(new ArrayList<>());
        }
        entity.getAiresCirculation().clear();
        if (raw == null) {
            return;
        }
        String normalized = raw.replace("##", "||");
        for (String token : LIST_SPLIT.split(normalized)) {
            String t = token.trim();
            if (!StringUtils.hasText(t)) {
                continue;
            }
            String[] pair = splitPair(t);
            String valeur = limitVarcharColumn(firstNonBlank(pair[0], pair[1]));
            if (!StringUtils.hasText(valeur)) {
                continue;
            }
            ReferenceOpentheso ref = ReferenceOpentheso.builder()
                    .code("AIRE_CIRCULATION")
                    .valeur(valeur)
                    .url(StringUtils.hasText(pair[1]) ? limitVarcharColumn(pair[1]) : null)
                    .entity(entity)
                    .build();
            entity.getAiresCirculation().add(referenceOpenthesoRepository.save(ref));
        }
    }

    private void replaceProductions(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        if (!columnInCsv(csvHeaders, TypologyImportConstants.COL_PRODUCTION_VALUE)) {
            return;
        }
        String raw = trimToNull(getCell(row, TypologyImportConstants.COL_PRODUCTION_VALUE));
        if (!isCreate && raw == null) {
            return;
        }
        if (entity.getProductions() == null) {
            entity.setProductions(new ArrayList<>());
        }
        entity.getProductions().clear();
        if (raw == null) {
            return;
        }
        String normalized = raw.replace("##", "||");
        for (String token : LIST_SPLIT.split(normalized)) {
            String t = token.trim();
            if (!StringUtils.hasText(t)) {
                continue;
            }
            String[] pair = splitPair(t);
            String valeur = limitVarcharColumn(firstNonBlank(pair[0], pair[1]));
            if (!StringUtils.hasText(valeur)) {
                continue;
            }
            ReferenceOpentheso ref = ReferenceOpentheso.builder()
                    .code(ReferenceOpenthesoEnum.PRODUCTION.name())
                    .valeur(valeur)
                    .url(StringUtils.hasText(pair[1]) ? limitVarcharColumn(pair[1]) : null)
                    .entity(entity)
                    .build();
            entity.getProductions().add(referenceOpenthesoRepository.save(ref));
        }
    }

    private void replaceFonctionsUsage(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        if (!columnInCsv(csvHeaders, TypologyImportCeramiqueConstants.COL_DESCRIPTION_FONCTION)) {
            return;
        }
        String raw = trimToNull(getCell(row, TypologyImportCeramiqueConstants.COL_DESCRIPTION_FONCTION));
        if (!isCreate && raw == null) {
            return;
        }
        if (entity.getFonctionsUsage() == null) {
            entity.setFonctionsUsage(new ArrayList<>());
        }
        entity.getFonctionsUsage().clear();
        if (raw == null) {
            return;
        }
        String normalized = raw.replace("##", "||");
        for (String token : LIST_SPLIT.split(normalized)) {
            String t = token.trim();
            if (!StringUtils.hasText(t)) {
                continue;
            }
            String[] pair = splitPair(t);
            String valeur = limitVarcharColumn(firstNonBlank(pair[0], pair[1]));
            if (!StringUtils.hasText(valeur)) {
                continue;
            }
            ReferenceOpentheso ref = ReferenceOpentheso.builder()
                    .code(ReferenceOpenthesoEnum.FONCTION_USAGE.name())
                    .valeur(valeur)
                    .url(StringUtils.hasText(pair[1]) ? limitVarcharColumn(pair[1]) : null)
                    .entity(entity)
                    .build();
            entity.getFonctionsUsage().add(referenceOpenthesoRepository.save(ref));
        }
    }

    private void replaceAuteursScientifiques(Entity entity, Map<String, String> row, Set<String> csvHeaders,
                                             TypologyImportAuthorCache authorCache) {
        if (!columnInCsvScientificAuthors(csvHeaders)) {
            return;
        }
        String raw = trimToNull(getAuthorImportRaw(row));
        if (raw == null) {
            return;
        }
        if (entity.getAuteursScientifiques() == null) {
            entity.setAuteursScientifiques(new ArrayList<>());
        }
        entity.getAuteursScientifiques().clear();
        String normalized = raw.replace("##", "||");
        for (String token : LIST_SPLIT.split(normalized)) {
            Optional<ParsedScientificAuthor> parsed = parseScientificAuthorToken(token);
            if (parsed.isEmpty()) {
                continue;
            }
            ParsedScientificAuthor pn = parsed.get();
            AuteurScientifique author = authorCache.resolve(pn.nom(), pn.prenom());
            entity.getAuteursScientifiques().add(author);
        }
    }

    private boolean columnInCsvScientificAuthors(Set<String> csvHeaders) {
        return columnInCsv(csvHeaders, TypologyImportConstants.COL_AUTEUR_SCIENTIFIQUE)
                || columnInCsv(csvHeaders, TypologyImportConstants.COL_AUTEURS_SCIENTIFIQUES);
    }

    private static String getAuthorImportRaw(Map<String, String> row) {
        return getCell(row, TypologyImportConstants.COL_AUTEUR_SCIENTIFIQUE, TypologyImportConstants.COL_AUTEURS_SCIENTIFIQUES);
    }

    /**
     * Formats acceptés par segment : {@code {Prénom, Nom}} (recommandé), {@code Prénom, Nom},
     * ou ancien {@code Nom:Prénom}.
     */
    private static Optional<ParsedScientificAuthor> parseScientificAuthorToken(String token) {
        String t = token.trim();
        if (!StringUtils.hasText(t)) {
            return Optional.empty();
        }
        if (t.startsWith("{") && t.endsWith("}")) {
            t = t.substring(1, t.length() - 1).trim();
        }
        int comma = t.indexOf(',');
        if (comma > 0 && comma < t.length() - 1) {
            String prenom = t.substring(0, comma).trim();
            String nom = t.substring(comma + 1).trim();
            if (StringUtils.hasText(prenom) && StringUtils.hasText(nom)) {
                return Optional.of(new ParsedScientificAuthor(prenom, nom));
            }
        }
        if (!t.contains(",") && t.contains(":")) {
            String[] pair = splitPair(t);
            String left = trimToNull(pair[0]);
            String right = trimToNull(pair[1]);
            if (left != null && right != null) {
                return Optional.of(new ParsedScientificAuthor(right, left));
            }
        }
        return Optional.empty();
    }

    private static void previewScientificAuthors(Map<String, String> row, List<String> warnings) {
        String raw = trimToNull(getAuthorImportRaw(row));
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String normalized = raw.replace("##", "||");
        for (String part : LIST_SPLIT.split(normalized)) {
            String token = part.trim();
            if (!StringUtils.hasText(token)) {
                continue;
            }
            if (parseScientificAuthorToken(token).isEmpty()) {
                String shortTok = token.length() > 100 ? token.substring(0, 100) + "…" : token;
                warnings.add("auteur_scientifique : segment non reconnu (attendu {Prénom, Nom}, Prénom, Nom ou ancien Nom:Prénom) : "
                        + shortTok);
            }
        }
    }

    private record ParsedScientificAuthor(String prenom, String nom) {
    }

    private ReferenceOpentheso saveReferenceForEntity(Entity entity, String labelUrlValue, String code) {
        String raw = trimToNull(labelUrlValue);
        if (raw == null) {
            return null;
        }
        String[] pair = parseLabelUrl(raw);
        String valeur = limitVarcharColumn(firstNonBlank(pair[0], pair[1]));
        if (!StringUtils.hasText(valeur)) {
            return null;
        }
        ReferenceOpentheso ref = ReferenceOpentheso.builder()
                .code(code)
                .valeur(valeur)
                .url(StringUtils.hasText(pair[1]) ? limitVarcharColumn(pair[1]) : null)
                .entity(entity)
                .build();
        return referenceOpenthesoRepository.save(ref);
    }

    private void applySlot(Entity entity, String url, String libelle, ReferenceOpenthesoEnum slot,
                           java.util.function.Supplier<ReferenceOpentheso> getter,
                           java.util.function.Consumer<ReferenceOpentheso> setter) {
        ReferenceOpentheso current = getter.get();
        boolean hasUrl = StringUtils.hasText(url);
        boolean hasLabel = StringUtils.hasText(libelle);
        if (!hasUrl && !hasLabel) {
            if (current != null && current.getEntity() != null
                    && Objects.equals(current.getEntity().getId(), entity.getId())) {
                setter.accept(null);
                entityRepository.save(entity);
                referenceOpenthesoRepository.deleteById(current.getId());
            } else if (current != null) {
                setter.accept(null);
                entityRepository.save(entity);
            }
            return;
        }
        if (current != null && current.getEntity() != null
                && Objects.equals(current.getEntity().getId(), entity.getId())) {
            setter.accept(null);
            entityRepository.save(entity);
            referenceOpenthesoRepository.deleteById(current.getId());
        } else if (current != null) {
            setter.accept(null);
            entityRepository.save(entity);
        }
        String valeur = hasLabel ? libelle.trim() : url.trim();
        String cleanedUrl = hasUrl ? url.trim() : null;
        ReferenceOpentheso ref = ReferenceOpentheso.builder()
                .code(slot.name())
                .valeur(limitVarcharColumn(valeur))
                .url(cleanedUrl != null
                        ? limitVarcharColumn(cleanedUrl)
                        : null)
                .entity(entity)
                .build();
        ReferenceOpentheso saved = referenceOpenthesoRepository.save(ref);
        setter.accept(saved);
    }

    /**
     * Charge l'entité et initialise les collections modifiables (évite fetch multi-bags).
     */
    private Entity loadEntityForImport(Long id, TypologyImportLookupCache lookupCache) {
        Optional<Entity> cached = lookupCache.findEntity(id);
        Entity e = cached.orElseGet(() -> entityRepository.findById(id).orElseThrow());
        if (e.getLabels() != null) {
            e.getLabels().size();
        }
        if (e.getDescriptions() != null) {
            e.getDescriptions().size();
        }
        if (e.getImages() != null) {
            e.getImages().size();
        }
        if (e.getDescriptionDetail() != null) {
            e.getDescriptionDetail().getId();
        }
        if (e.getCaracteristiquePhysique() != null) {
            e.getCaracteristiquePhysique().getId();
        }
        if (e.getDescriptionMonnaie() != null) {
            e.getDescriptionMonnaie().getId();
        }
        if (e.getCaracteristiquePhysiqueMonnaie() != null) {
            e.getCaracteristiquePhysiqueMonnaie().getId();
        }
        if (e.getAuteursScientifiques() != null) {
            e.getAuteursScientifiques().size();
        }
        if (e.getAppellationsUsuelles() != null) {
            e.getAppellationsUsuelles().size();
        }
        lookupCache.registerLoadedEntity(e);
        return e;
    }

    private Langue resolveLangCode(String code, String fallbackCode) {
        Langue l = langueRepository.findByCode(code);
        if (l == null) {
            l = langueRepository.findByCode(fallbackCode);
        }
        if (l == null) {
            throw new IllegalStateException("Aucune langue « " + fallbackCode + " » en base ; impossible de poursuivre l'import.");
        }
        return l;
    }

    private static void previewImages(Map<String, String> row, List<String> warnings,
                                      TypologyImportImageUrlCache imageUrlCache) {
        String raw = getCell(row, TypologyImportConstants.COL_ILLUSTRATIONS);
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String normalized = raw.replace("##", "||");
        for (String part : LIST_SPLIT.split(normalized)) {
            String token = part.trim();
            String url = token.contains(":") ? splitPair(token)[1] : token;
            if (!StringUtils.hasText(url)) {
                continue;
            }
            if (!imageUrlCache.validateWithNetwork(url)) {
                warnings.add("Image ignorée (URL invalide ou inaccessible) : " + url);
            }
        }
    }

    private static void previewOpenThesoCeramique(Map<String, String> row, List<String> errors) {
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_DATATION_PERIODE), TypologyImportConstants.COL_DATATION_PERIODE, errors);
        checkLabelUrlListOptional(getCell(row, TypologyImportConstants.COL_PRODUCTION_VALUE), TypologyImportConstants.COL_PRODUCTION_VALUE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportCeramiqueConstants.COL_DESCRIPTION_FORM), TypologyImportCeramiqueConstants.COL_DESCRIPTION_FORM, errors);
        checkLabelUrlListOptional(getCell(row, TypologyImportCeramiqueConstants.COL_DESCRIPTION_FONCTION), TypologyImportCeramiqueConstants.COL_DESCRIPTION_FONCTION, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE), TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_FABRICATION), TypologyImportConstants.COL_CARACT_PHYS_FABRICATION, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_COULEUR_PATE), TypologyImportCeramiqueConstants.COL_CARACT_PHYS_COULEUR_PATE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_NATURE_PATE), TypologyImportCeramiqueConstants.COL_CARACT_PHYS_NATURE_PATE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_INCLUSION), TypologyImportCeramiqueConstants.COL_CARACT_PHYS_INCLUSION, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportCeramiqueConstants.COL_CARACT_PHYS_CUISSON), TypologyImportCeramiqueConstants.COL_CARACT_PHYS_CUISSON, errors);
    }

    private static void previewOpenThesoMonnaie(Map<String, String> row, List<String> errors) {
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_DATATION_PERIODE), TypologyImportConstants.COL_DATATION_PERIODE, errors);
        checkLabelUrlListOptional(getCell(row, TypologyImportConstants.COL_PRODUCTION_VALUE), TypologyImportConstants.COL_PRODUCTION_VALUE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_MATERIAU), TypologyImportMonnaieConstants.COL_CARACT_PHYS_MATERIAU, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_DENOMINATION), TypologyImportMonnaieConstants.COL_CARACT_PHYS_DENOMINATION, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE), TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_VALEUR), TypologyImportMonnaieConstants.COL_CARACT_PHYS_VALEUR, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_TECHNIQUE), TypologyImportMonnaieConstants.COL_CARACT_PHYS_TECHNIQUE, errors);
    }

    private static void previewOpenThesoInstrumentum(Map<String, String> row, List<String> errors) {
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_DATATION_PERIODE), TypologyImportConstants.COL_DATATION_PERIODE, errors);
        checkLabelUrlListOptional(getCell(row, TypologyImportConstants.COL_PRODUCTION_VALUE), TypologyImportConstants.COL_PRODUCTION_VALUE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportInstrumentumConstants.COL_DESCRIPTION_CATEGORIE_FONCTIONNELLE),
                TypologyImportInstrumentumConstants.COL_DESCRIPTION_CATEGORIE_FONCTIONNELLE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportInstrumentumConstants.COL_DESCRIPTION_RELATION_IMITATION),
                TypologyImportInstrumentumConstants.COL_DESCRIPTION_RELATION_IMITATION, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportInstrumentumConstants.COL_DESCRIPTION_DENOMINATION),
                TypologyImportInstrumentumConstants.COL_DESCRIPTION_DENOMINATION, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_MATERIAUX),
                TypologyImportInstrumentumConstants.COL_CARACT_PHYS_MATERIAUX, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_FORME),
                TypologyImportInstrumentumConstants.COL_CARACT_PHYS_FORME, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_DIMENSIONS),
                TypologyImportInstrumentumConstants.COL_CARACT_PHYS_DIMENSIONS, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportInstrumentumConstants.COL_CARACT_PHYS_TECHNIQUE),
                TypologyImportInstrumentumConstants.COL_CARACT_PHYS_TECHNIQUE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_FABRICATION), TypologyImportConstants.COL_CARACT_PHYS_FABRICATION, errors);
    }

    private static void previewDatation(Map<String, String> row, List<String> errors) {
        checkIntegerOptional(getCell(row, TypologyImportConstants.COL_DATATION_TPQ), "tpq", errors);
        checkIntegerOptional(getCell(row, TypologyImportConstants.COL_DATATION_TAQ), "taq", errors);
    }

    private static void checkLabelUrlListOptional(String raw, String col, List<String> errors) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String normalized = raw.replace("##", "||");
        for (String token : LIST_SPLIT.split(normalized)) {
            String t = token.trim();
            if (!StringUtils.hasText(t)) {
                continue;
            }
            checkLabelUrlOptional(t, col, errors);
        }
    }

    private static void checkLabelUrlOptional(String raw, String col, List<String> errors) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String[] pair = parseLabelUrl(raw);
        if (!StringUtils.hasText(pair[0]) && !StringUtils.hasText(pair[1])) {
            errors.add(col + " doit être au format label:url ou label.");
            return;
        }
        checkUrlOptional(pair[1], col, errors);
    }

    private static void checkIntegerOptional(String raw, String col, List<String> errors) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        try {
            Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            errors.add(col + " doit être un entier.");
        }
    }

    private static void checkUrlOptional(String url, String col, List<String> errors) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        try {
            URI u = URI.create(url.trim());
            String s = u.getScheme();
            if (s == null || (!"http".equalsIgnoreCase(s) && !"https".equalsIgnoreCase(s))) {
                errors.add(col + " doit être une URL http(s) valide.");
            }
        } catch (IllegalArgumentException e) {
            errors.add(col + " : URL illisible.");
        }
    }

    private static void validateCodeFormat(String code, List<String> errors) {
        if (!StringUtils.hasText(code)) {
            errors.add("Code manquant.");
            return;
        }
        if (code.length() > EntityConstants.MAX_CODE_LENGTH) {
            errors.add(EntityConstants.ERROR_CODE_TOO_LONG);
        }
    }

    private static String importDuplicateKey(
            TypologyImportKind kind, String catCode, String groupCode, String serieCode, String targetCode) {
        if (kind == null || targetCode == null) {
            return "";
        }
        return switch (kind) {
            case CATEGORIE -> "CAT:" + targetCode;
            case GROUPE -> "GRP:" + targetCode;
            case SERIE -> "SER:" + catCode + KEY_SEP + groupCode + KEY_SEP + targetCode;
            case TYPE_SOUS_SERIE, TYPE_SOUS_GROUPE -> "TYP:" + catCode + KEY_SEP + groupCode + KEY_SEP + targetCode;
            default -> "UNK:" + targetCode;
        };
    }

    private void validateImportScopedCode(
            TypologyImportLookupCache lookupCache,
            TypologyImportKind kind,
            String targetCode,
            String catCode,
            String groupCode,
            String serieCode,
            List<String> errors) {
        if (kind == null || targetCode == null || kind == TypologyImportKind.NON_CLASSIFIE) {
            return;
        }
        if (importEntityExistsInScope(lookupCache, kind, targetCode, catCode, groupCode, serieCode)) {
            return;
        }
        String message = switch (kind) {
            case CATEGORIE -> EntityConstants.ERROR_CATEGORY_CODE_EXISTS_IN_REFERENCE;
            case GROUPE -> EntityConstants.ERROR_GROUP_CODE_EXISTS_IN_REFERENCE;
            case SERIE -> EntityConstants.ERROR_SERIE_CODE_EXISTS_IN_GROUP;
            case TYPE_SOUS_SERIE, TYPE_SOUS_GROUPE -> EntityConstants.ERROR_TYPE_CODE_EXISTS_IN_GROUP;
            default -> EntityConstants.ERROR_CODE_ALREADY_EXISTS;
        };
        if (isImportCodeTakenOnCreate(lookupCache, kind, targetCode, catCode, groupCode, serieCode)) {
            errors.add("Le code « " + targetCode + " » : " + message);
        }
    }

    private boolean isImportCodeTakenOnCreate(
            TypologyImportLookupCache lookupCache,
            TypologyImportKind kind,
            String targetCode,
            String catCode,
            String groupCode,
            String serieCode) {
        return switch (kind) {
            case CATEGORIE -> lookupCache.findEntityIdByCategoryCode(targetCode).isPresent();
            case GROUPE -> lookupCache.findEntityIdByGroupInReference(targetCode).isPresent();
            case SERIE -> lookupCache.findEntityIdByGroupKey(catCode, groupCode)
                    .flatMap(groupId -> lookupCache.findExistingChildEntityId(
                            entityStub(groupId), EntityConstants.ENTITY_TYPE_SERIES, targetCode))
                    .isPresent();
            case TYPE_SOUS_SERIE, TYPE_SOUS_GROUPE -> lookupCache.findEntityIdByGroupKey(catCode, groupCode)
                    .flatMap(groupId -> lookupCache.findExistingChildEntityId(
                            entityStub(groupId), EntityConstants.ENTITY_TYPE_TYPE, targetCode))
                    .isPresent();
            default -> false;
        };
    }

    private static Entity entityStub(Long id) {
        Entity entity = new Entity();
        entity.setId(id);
        return entity;
    }

    private boolean importEntityExistsInScope(
            TypologyImportLookupCache lookupCache,
            TypologyImportKind kind,
            String targetCode,
            String catCode,
            String groupCode,
            String serieCode) {
        return lookupCache.findExistingEntityId(kind, targetCode, catCode, groupCode).isPresent();
    }

    private void assertImportCodeAvailableOnCreate(
            Entity parent, String expectedTypeCode, String code, TypologyImportLookupCache lookupCache) {
        if (lookupCache.isCodeRegisteredInScope(parent, expectedTypeCode, code)) {
            String message = switch (expectedTypeCode) {
                case EntityConstants.ENTITY_TYPE_GROUP -> EntityConstants.ERROR_GROUP_CODE_EXISTS_IN_REFERENCE;
                case EntityConstants.ENTITY_TYPE_SERIES -> EntityConstants.ERROR_SERIE_CODE_EXISTS_IN_GROUP;
                case EntityConstants.ENTITY_TYPE_TYPE -> EntityConstants.ERROR_TYPE_CODE_EXISTS_IN_GROUP;
                default -> EntityConstants.ERROR_CODE_ALREADY_EXISTS;
            };
            throw new IllegalStateException(message);
        }
        if (entityCodeUniquenessService.isCodeTakenForCreate(expectedTypeCode, parent, code, null)) {
            String message = switch (expectedTypeCode) {
                case EntityConstants.ENTITY_TYPE_GROUP -> EntityConstants.ERROR_GROUP_CODE_EXISTS_IN_REFERENCE;
                case EntityConstants.ENTITY_TYPE_SERIES -> EntityConstants.ERROR_SERIE_CODE_EXISTS_IN_GROUP;
                case EntityConstants.ENTITY_TYPE_TYPE -> EntityConstants.ERROR_TYPE_CODE_EXISTS_IN_GROUP;
                default -> EntityConstants.ERROR_CODE_ALREADY_EXISTS;
            };
            throw new IllegalStateException(message);
        }
    }

    private static List<Integer> sortedRowIndices(int n, TypologyImportKind[] kinds) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            idx.add(i);
        }
        Map<TypologyImportKind, Integer> orderRank = new EnumMap<>(TypologyImportKind.class);
        orderRank.put(TypologyImportKind.CATEGORIE, 0);
        orderRank.put(TypologyImportKind.GROUPE, 1);
        orderRank.put(TypologyImportKind.SERIE, 2);
        orderRank.put(TypologyImportKind.TYPE_SOUS_SERIE, 3);
        orderRank.put(TypologyImportKind.TYPE_SOUS_GROUPE, 3);
        orderRank.put(TypologyImportKind.NON_CLASSIFIE, 99);
        idx.sort(Comparator.comparingInt(i -> kinds[i] == null ? 99 : orderRank.getOrDefault(kinds[i], 99)));
        return idx;
    }

    /**
     * Détermine le niveau hiérarchique créé/mis à jour selon les codes renseignés :
     * <ul>
     *   <li>{@code code_categorie} seul → catégorie</li>
     *   <li>+ {@code code_groupe} (sans série ni type) → groupe</li>
     *   <li>+ {@code code_serie} (sans type) → série</li>
     *   <li>+ {@code code_type} → type (sous série si série renseignée, sinon sous groupe)</li>
     * </ul>
     */
    private static Optional<ClassifyResult> classify(HierarchyCodes codes, List<String> errors) {
        String cc = codes.categorie();
        if (!StringUtils.hasText(cc)) {
            errors.add("code_categorie est obligatoire.");
            return Optional.empty();
        }
        String cg = codes.groupe();
        String cs = codes.serie();
        String ct = codes.type();
        if (StringUtils.hasText(ct)) {
            if (!StringUtils.hasText(cg)) {
                errors.add("code_groupe est requis lorsque code_type est renseigné.");
                return Optional.empty();
            }
            if (StringUtils.hasText(cs)) {
                return Optional.of(new ClassifyResult(TypologyImportKind.TYPE_SOUS_SERIE, ct));
            }
            return Optional.of(new ClassifyResult(TypologyImportKind.TYPE_SOUS_GROUPE, ct));
        }
        if (StringUtils.hasText(cs)) {
            if (!StringUtils.hasText(cg)) {
                errors.add("code_groupe est requis lorsque code_serie est renseigné.");
                return Optional.empty();
            }
            return Optional.of(new ClassifyResult(TypologyImportKind.SERIE, cs));
        }
        if (StringUtils.hasText(cg)) {
            return Optional.of(new ClassifyResult(TypologyImportKind.GROUPE, cg));
        }
        return Optional.of(new ClassifyResult(TypologyImportKind.CATEGORIE, cc));
    }

    private static HierarchyCodes hierarchyCodesFromRow(Map<String, String> row) {
        return new HierarchyCodes(
                trimToNull(getCell(row, TypologyImportConstants.COL_CODE_CATEGORIE)),
                trimToNull(getCell(row, TypologyImportConstants.COL_CODE_GROUPE)),
                trimToNull(getCell(row, TypologyImportConstants.COL_CODE_SERIE)),
                trimToNull(getCell(row, TypologyImportConstants.COL_CODE_TYPE)));
    }

    private record HierarchyCodes(String categorie, String groupe, String serie, String type) {
    }

    private record ClassifyResult(TypologyImportKind kind, String targetCode) {
    }

    private static String gKey(String cc, String cg) {
        return cc.trim() + KEY_SEP + cg.trim();
    }

    private static String sKey(String cc, String cg, String cs) {
        return cc.trim() + KEY_SEP + cg.trim() + KEY_SEP + cs.trim();
    }

    private static String get(Map<String, String> row, String key) {
        if (row == null) {
            return "";
        }
        String v = row.get(key.toLowerCase(Locale.ROOT));
        return v != null ? v : "";
    }

    private static String getCell(Map<String, String> row, String... keys) {
        if (row == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            if (!StringUtils.hasText(key)) {
                continue;
            }
            String v = row.get(key.toLowerCase(Locale.ROOT));
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private static String[] parseLabelUrl(String raw) {
        String t = trimToNull(raw);
        if (t == null) {
            return new String[]{"", ""};
        }
        return splitPair(t);
    }

    private static String[] splitPair(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new String[]{"", ""};
        }
        String v = raw.trim();
        if (v.startsWith("http://") || v.startsWith("https://")) {
            return new String[]{"", v};
        }
        int schemeIdx = v.indexOf("http://");
        if (schemeIdx < 0) {
            schemeIdx = v.indexOf("https://");
        }
        if (schemeIdx > 0) {
            int sep = schemeIdx - 1;
            if (v.charAt(sep) == ':') {
                String left = v.substring(0, sep).trim();
                String right = v.substring(schemeIdx).trim();
                return new String[]{left, right};
            }
        }
        int idx = v.indexOf(':');
        if (idx < 0) {
            return new String[]{v, ""};
        }
        String left = v.substring(0, idx).trim();
        String right = v.substring(idx + 1).trim();
        return new String[]{left, right};
    }

    private static String[] splitColonValues(String raw) {
        String[] vals = raw.split(":");
        ArrayList<String> cleaned = new ArrayList<>();
        for (String v : vals) {
            String t = v.trim();
            if (StringUtils.hasText(t)) {
                cleaned.add(t);
            }
        }
        return cleaned.toArray(new String[0]);
    }

    private static String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private static String limitVarcharColumn(String v) {
        if (!StringUtils.hasText(v)) {
            return v;
        }
        int max = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH;
        return v.length() > max ? v.substring(0, max) : v;
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
