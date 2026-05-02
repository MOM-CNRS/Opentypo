package fr.cnrs.opentypo.application.import_typology;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.application.dto.ReferenceOpenthesoEnum;
import fr.cnrs.opentypo.application.service.CategoryService;
import fr.cnrs.opentypo.application.service.GroupService;
import fr.cnrs.opentypo.application.service.SerieService;
import fr.cnrs.opentypo.application.service.TypeService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Description;
import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
import fr.cnrs.opentypo.domain.entity.DescriptionPate;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityMetadata;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
 * Analyse sans écriture ; exécution transactionnelle tout-ou-rien.
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
    private AuteurScientifiqueRepository auteurScientifiqueRepository;

    /**
     * Analyse le fichier : classification, détection d'erreurs, aperçu création/mise à jour.
     */
    public TypologyImportAnalyzeResult analyze(Entity reference, TypologyCsvParser.ParsedCsv parsed,
                                               TypologyImportCollectionProfile collectionProfile) {
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

        List<Map<String, String>> rows = parsed.rows();
        int n = rows.size();
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
            String cc = getCell(row, TypologyImportConstants.COL_CODE_CATEGORIE);
            String cg = get(row, TypologyImportConstants.COL_CODE_GROUPE);
            String cs = get(row, TypologyImportConstants.COL_CODE_SERIE);
            String ct = get(row, TypologyImportConstants.COL_CODE_TYPE);
            ccArr[rowIndex] = cc;
            cgArr[rowIndex] = cg;
            csArr[rowIndex] = cs;

            Optional<ClassifyResult> clf = classify(cc, cg, cs, ct, err.get(rowIndex));
            if (clf.isEmpty()) {
                kinds[rowIndex] = TypologyImportKind.NON_CLASSIFIE;
                continue;
            }
            kinds[rowIndex] = clf.get().kind();
            targets[rowIndex] = clf.get().targetCode();

            validateCodeFormat(targets[rowIndex], err.get(rowIndex));

            Integer first = firstOccurrenceIdx.putIfAbsent(targets[rowIndex], rowIndex);
            if (first != null) {
                err.get(rowIndex).add("Code « " + targets[rowIndex] + " » dupliqué dans le fichier (ligne " + (first + 2) + ").");
                err.get(first).add("Code « " + targets[rowIndex] + " » dupliqué dans le fichier (ligne " + csvRowNumber + ").");
            }

            entityRepository.findByCode(targets[rowIndex]).ifPresent(existing -> {
                Entity anc = typeService.findReferenceAncestor(existing);
                if (anc == null || !Objects.equals(anc.getId(), reference.getId())) {
                    err.get(rowIndex).add("Le code « " + targets[rowIndex]
                            + " » existe déjà hors de ce référentiel ou sans rattachement attendu.");
                }
            });

            previewImages(row, warn.get(rowIndex));
            if (collectionProfile == TypologyImportCollectionProfile.MONNAIE) {
                previewOpenThesoMonnaie(row, err.get(rowIndex));
            } else if (collectionProfile == TypologyImportCollectionProfile.INSTRUMENTUM) {
                previewOpenThesoInstrumentum(row, err.get(rowIndex));
            } else {
                previewOpenThesoCeramique(row, err.get(rowIndex));
            }
            previewDatation(row, err.get(rowIndex));
            previewScientificAuthors(row, warn.get(rowIndex));
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
                            || findCategory(reference, cc).isPresent();
                    if (!catOk) {
                        e.add("Catégorie « " + cc + " » introuvable sous ce référentiel et non créée dans les lignes précédentes.");
                    }
                    virtualGroups.add(gKey(cc, cg));
                }
                case SERIE -> {
                    boolean gOk = virtualGroups.contains(gKey(cc, cg))
                            || findGroup(reference, cc, cg).isPresent();
                    if (!gOk) {
                        e.add("Groupe « " + cg + " » (catégorie « " + cc + " ») introuvable.");
                    }
                    virtualSeries.add(sKey(cc, cg, cs));
                }
                case TYPE_SOUS_SERIE -> {
                    boolean sOk = virtualSeries.contains(sKey(cc, cg, cs))
                            || findSerie(reference, cc, cg, cs).isPresent();
                    if (!sOk) {
                        e.add("Série « " + cs + " » introuvable pour ce groupe / catégorie.");
                    }
                }
                case TYPE_SOUS_GROUPE -> {
                    boolean gOk = virtualGroups.contains(gKey(cc, cg))
                            || findGroup(reference, cc, cg).isPresent();
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
                    && entityRepository.findByCode(tgt).isEmpty();
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

        return new TypologyImportAnalyzeResult(allOk && blocking.isEmpty(), blocking, previews, parsed);
    }

    /**
     * Applique l'import dans une transaction unique (tout ou rien).
     */
    @Transactional
    public void execute(Entity reference, TypologyCsvParser.ParsedCsv parsed, Utilisateur user,
                        TypologyImportCollectionProfile collectionProfile) {
        Entity ref = entityRepository.findById(Objects.requireNonNull(reference.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Référentiel introuvable."));
        if (ref.getEntityType() == null
                || !EntityConstants.ENTITY_TYPE_REFERENCE.equals(ref.getEntityType().getCode())) {
            throw new IllegalArgumentException("L'entité n'est pas un référentiel.");
        }
        if (collectionProfile == TypologyImportCollectionProfile.UNSUPPORTED) {
            throw new IllegalArgumentException("Typologie de collection non prise en charge pour l'import CSV.");
        }

        TypologyImportAnalyzeResult analysis = analyze(ref, parsed, collectionProfile);
        if (!analysis.successful()) {
            throw new IllegalStateException("L'analyse signale des erreurs bloquantes ; import annulé.");
        }

        List<Map<String, String>> rows = parsed.rows();
        int n = rows.size();
        TypologyImportKind[] kinds = new TypologyImportKind[n];
        String[] targets = new String[n];

        for (int i = 0; i < n; i++) {
            Map<String, String> row = rows.get(i);
            String cc = getCell(row, TypologyImportConstants.COL_CODE_CATEGORIE);
            String cg = get(row, TypologyImportConstants.COL_CODE_GROUPE);
            String cs = get(row, TypologyImportConstants.COL_CODE_SERIE);
            String ct = get(row, TypologyImportConstants.COL_CODE_TYPE);
            Optional<ClassifyResult> clf = classify(cc, cg, cs, ct, new ArrayList<>());
            if (clf.isPresent()) {
                kinds[i] = clf.get().kind();
                targets[i] = clf.get().targetCode();
            }
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

        for (int idx : order) {
            if (kinds[idx] == null) {
                continue;
            }
            Map<String, String> row = rows.get(idx);
            String cc = getCell(row, TypologyImportConstants.COL_CODE_CATEGORIE);
            String cg = get(row, TypologyImportConstants.COL_CODE_GROUPE);
            String cs = get(row, TypologyImportConstants.COL_CODE_SERIE);
            applyRow(ref, row, kinds[idx], targets[idx], cc, cg, cs,
                    catType, grpType, serType, typType, user, csvHeaders, collectionProfile);
        }
    }

    private void applyRow(Entity reference, Map<String, String> row, TypologyImportKind kind, String targetCode,
                          String cc, String cg, String cs,
                          EntityType catType, EntityType grpType, EntityType serType, EntityType typType,
                          Utilisateur user, Set<String> csvHeaders,
                          TypologyImportCollectionProfile collectionProfile) {

        Langue langLabel = resolveLangCode("fr", "fr");
        Langue langDesc = resolveLangCode("fr", "fr");

        String statutStr = null;

        switch (kind) {
            case CATEGORIE -> upsertCategory(reference, targetCode, langLabel, langDesc,
                    statutStr, row, catType, user, csvHeaders, collectionProfile);
            case GROUPE -> {
                Entity cat = findCategory(reference, cc).orElseThrow();
                upsertChild(cat, targetCode, EntityConstants.ENTITY_TYPE_GROUP, grpType,
                        langLabel, langDesc, statutStr, row, user, csvHeaders, collectionProfile);
            }
            case SERIE -> {
                Entity grp = findGroup(reference, cc, cg).orElseThrow();
                upsertChild(grp, targetCode, EntityConstants.ENTITY_TYPE_SERIES, serType,
                        langLabel, langDesc, statutStr, row, user, csvHeaders, collectionProfile);
            }
            case TYPE_SOUS_SERIE -> {
                Entity serie = findSerie(reference, cc, cg, cs).orElseThrow();
                upsertChild(serie, targetCode, EntityConstants.ENTITY_TYPE_TYPE, typType,
                        langLabel, langDesc, statutStr, row, user, csvHeaders, collectionProfile);
            }
            case TYPE_SOUS_GROUPE -> {
                Entity grp = findGroup(reference, cc, cg).orElseThrow();
                upsertChild(grp, targetCode, EntityConstants.ENTITY_TYPE_TYPE, typType,
                        langLabel, langDesc, statutStr, row, user, csvHeaders, collectionProfile);
            }
            case NON_CLASSIFIE ->
                    throw new IllegalStateException("Ligne non classifiable : ne doit pas être importée.");
        }
    }

    private void upsertCategory(Entity reference, String code,
                                Langue langLabel, Langue langDesc, String statutStr,
                                Map<String, String> row, EntityType catType, Utilisateur user,
                                Set<String> csvHeaders, TypologyImportCollectionProfile collectionProfile) {
        Optional<Entity> existingOpt = entityRepository.findByCode(code);
        boolean isCreate = existingOpt.isEmpty();
        Entity entity;
        if (existingOpt.isEmpty()) {
            entity = new Entity();
            entity.setEntityType(catType);
            entity.setCode(code);
            entity.setCreateDate(LocalDateTime.now());
            attachAuthor(entity, user);
            entity.setStatut(statutStr != null ? statutStr : EntityStatusEnum.PROPOSITION.name());
            entity = entityRepository.save(entity);
            linkParentChild(reference, entity);
        } else {
            entity = loadEntityForImport(existingOpt.get().getId());
            entity.setStatut(statutStr != null ? statutStr : entity.getStatut());
            if (!entityRelationRepository.existsByParentAndChild(reference.getId(), entity.getId())) {
                linkParentChild(reference, entity);
            }
        }
        applyFrenchPrimaryLabelsDescriptions(entity, code, langLabel, langDesc, row, csvHeaders, isCreate);
        replaceLocalizedTexts(entity, row, csvHeaders, isCreate);
        replaceMetadata(entity, row, csvHeaders, isCreate);
        replaceImages(entity, row, csvHeaders, isCreate);
        replaceOpenTheso(entity, row, csvHeaders, isCreate);
        applyTypologySpecificDetails(entity, row, csvHeaders, isCreate, collectionProfile);
        replaceAuteursScientifiques(entity, row, csvHeaders);
        entityRepository.save(entity);
    }

    private void upsertChild(Entity parent, String code, String expectedTypeCode, EntityType concreteType,
                             Langue langLabel, Langue langDesc, String statutStr,
                             Map<String, String> row, Utilisateur user, Set<String> csvHeaders,
                             TypologyImportCollectionProfile collectionProfile) {
        Optional<Entity> existingOpt = entityRepository.findByCode(code);
        boolean isCreate = existingOpt.isEmpty();
        Entity entity;
        if (existingOpt.isEmpty()) {
            entity = new Entity();
            entity.setEntityType(concreteType);
            entity.setCode(code);
            entity.setCreateDate(LocalDateTime.now());
            attachAuthor(entity, user);
            entity.setStatut(statutStr != null ? statutStr : EntityStatusEnum.PROPOSITION.name());
            entity = entityRepository.save(entity);
            linkParentChild(parent, entity);
        } else {
            entity = loadEntityForImport(existingOpt.get().getId());
            if (entity.getEntityType() == null || !expectedTypeCode.equals(entity.getEntityType().getCode())) {
                throw new IllegalStateException("Le code « " + code + " » existe avec un autre type d'entité.");
            }
            Entity currentParent = entityRelationRepository.findParentsByChild(entity).stream()
                    .findFirst().orElse(null);
            if (currentParent == null || !Objects.equals(currentParent.getId(), parent.getId())) {
                typeService.changeTypeParent(entity, parent);
                entity = loadEntityForImport(entity.getId());
            }
            entity.setStatut(statutStr != null ? statutStr : entity.getStatut());
        }
        applyFrenchPrimaryLabelsDescriptions(entity, code, langLabel, langDesc, row, csvHeaders, isCreate);
        replaceLocalizedTexts(entity, row, csvHeaders, isCreate);
        replaceMetadata(entity, row, csvHeaders, isCreate);
        replaceImages(entity, row, csvHeaders, isCreate);
        replaceOpenTheso(entity, row, csvHeaders, isCreate);
        applyTypologySpecificDetails(entity, row, csvHeaders, isCreate, collectionProfile);
        replaceAuteursScientifiques(entity, row, csvHeaders);
        entityRepository.save(entity);
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
        boolean touchDecors = shouldWriteField(csvHeaders, TypologyImportConstants.COL_DESCRIPTION_DECORS, row, isCreate);
        boolean touchMarques = shouldWriteField(csvHeaders, TypologyImportConstants.COL_DESCRIPTION_MARQUES, row, isCreate);
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
            dd.setDecors(trimToNull(getCell(row, TypologyImportConstants.COL_DESCRIPTION_DECORS)));
        }
        if (touchMarques) {
            dd.setMarques(listToSemicolon(getCell(row, TypologyImportConstants.COL_DESCRIPTION_MARQUES), false));
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
            cp.setFabrication(r);
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

    private void linkParentChild(Entity parent, Entity child) {
        if (!entityRelationRepository.existsByParentAndChild(parent.getId(), child.getId())) {
            EntityRelation rel = new EntityRelation();
            rel.setParent(parent);
            rel.setChild(child);
            entityRelationRepository.save(rel);
        }
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
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_APPELLATION_USUELLE, row, isCreate)) {
            ensureMetadataForTypologyImport(entity);
            String[] pair = parseLabelUrl(getCell(row, TypologyImportConstants.COL_APPELLATION_USUELLE));
            applySlot(entity, pair[1], pair[0], ReferenceOpenthesoEnum.APPELLATION_USUELLE,
                    () -> entity.getMetadata().getAppellationOpentheso(),
                    ref -> entity.getMetadata().setAppellationOpentheso(ref));
        }
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
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_PRODUCTION_ATELIERS, row, isCreate)) {
            entity.setAteliers(listToSemicolon(getCell(row, TypologyImportConstants.COL_PRODUCTION_ATELIERS), false));
        }
        if (shouldWriteField(csvHeaders, TypologyImportConstants.COL_ATTESTATIONS_CORPUS_LIE, row, isCreate)) {
            entity.setCorpusLies(trimToNull(getCell(row, TypologyImportConstants.COL_ATTESTATIONS_CORPUS_LIE)));
        }
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

    private void replaceImages(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
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
            if (!RemoteImageUrlValidator.isValidRemoteImageUrl(url)) {
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

        if (columnInCsv(csvHeaders, TypologyImportConstants.COL_PRODUCTION_VALUE)) {
            if (isCreate || StringUtils.hasText(getCell(row, TypologyImportConstants.COL_PRODUCTION_VALUE))) {
                String[] production = parseLabelUrl(getCell(row, TypologyImportConstants.COL_PRODUCTION_VALUE));
                applySlot(entity,
                        production[1],
                        production[0],
                        ReferenceOpenthesoEnum.PRODUCTION,
                        entity::getProduction,
                        entity::setProduction);
            }
        } else if (isCreate) {
            String[] production = parseLabelUrl(getCell(row, TypologyImportConstants.COL_PRODUCTION_VALUE));
            applySlot(entity,
                    production[1],
                    production[0],
                    ReferenceOpenthesoEnum.PRODUCTION,
                    entity::getProduction,
                    entity::setProduction);
        }

        if (isCreate) {
            applySlot(entity, null, null, ReferenceOpenthesoEnum.CATEGORIE_FONCTIONNELLE, entity::getCategorieFonctionnelle, entity::setCategorieFonctionnelle);
        }
    }

    private void replaceCeramiqueDetails(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        applyDescriptionDetail(entity, row, csvHeaders, isCreate);
        applyCaracteristiquePhysique(entity, row, csvHeaders, isCreate);
        applyDescriptionPate(entity, row, csvHeaders, isCreate);
        replaceAiresCirculation(entity, row, csvHeaders, isCreate);
    }

    private void applyDescriptionDetail(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        boolean touchDecors = shouldWriteField(csvHeaders, TypologyImportConstants.COL_DESCRIPTION_DECORS, row, isCreate);
        boolean touchMarques = shouldWriteField(csvHeaders, TypologyImportConstants.COL_DESCRIPTION_MARQUES, row, isCreate);
        boolean touchFonction = shouldWriteField(csvHeaders, TypologyImportConstants.COL_DESCRIPTION_FONCTION, row, isCreate);
        if (!touchDecors && !touchMarques && !touchFonction) {
            return;
        }
        DescriptionDetail dd = entity.getDescriptionDetail();
        if (dd == null) {
            dd = new DescriptionDetail();
            dd.setEntity(entity);
            entity.setDescriptionDetail(dd);
        }
        if (touchDecors) {
            dd.setDecors(trimToNull(getCell(row, TypologyImportConstants.COL_DESCRIPTION_DECORS)));
        }
        if (touchMarques) {
            dd.setMarques(listToSemicolon(getCell(row, TypologyImportConstants.COL_DESCRIPTION_MARQUES), false));
        }
        if (touchFonction) {
            dd.setFonction(saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_DESCRIPTION_FONCTION), "FONCTION_USAGE"));
        }
    }

    private void applyCaracteristiquePhysique(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        boolean touchForme = shouldWriteField(csvHeaders, TypologyImportConstants.COL_DESCRIPTION_FORM, row, isCreate);
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
            cp.setForme(saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_DESCRIPTION_FORM), "FORME"));
        }
        if (touchMetro) {
            cp.setMetrologie(saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE), "METROLOGIE"));
        }
        if (touchFab) {
            cp.setFabrication(saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_CARACT_PHYS_FABRICATION), "FABRICATION"));
        }
    }

    private void applyDescriptionPate(Entity entity, Map<String, String> row, Set<String> csvHeaders, boolean isCreate) {
        boolean touchDesc = shouldWriteField(csvHeaders, TypologyImportConstants.COL_CARACT_PHYS_DESCRIPTION_PATE, row, isCreate);
        boolean touchCouleur = shouldWriteField(csvHeaders, TypologyImportConstants.COL_CARACT_PHYS_COULEUR_PATE, row, isCreate);
        boolean touchNature = shouldWriteField(csvHeaders, TypologyImportConstants.COL_CARACT_PHYS_NATURE_PATE, row, isCreate);
        boolean touchIncl = shouldWriteField(csvHeaders, TypologyImportConstants.COL_CARACT_PHYS_INCLUSION, row, isCreate);
        boolean touchCuisson = shouldWriteField(csvHeaders, TypologyImportConstants.COL_CARACT_PHYS_CUISSON, row, isCreate);
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
            dp.setDescription(trimToNull(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_DESCRIPTION_PATE)));
        }
        if (touchCouleur) {
            dp.setCouleur(saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_CARACT_PHYS_COULEUR_PATE), "COULEUR_PATE"));
        }
        if (touchNature) {
            dp.setNature(saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_CARACT_PHYS_NATURE_PATE), "NATURE_PATE"));
        }
        if (touchIncl) {
            dp.setInclusion(saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_CARACT_PHYS_INCLUSION), "INCLUSION"));
        }
        if (touchCuisson) {
            dp.setCuisson(saveReferenceForEntity(entity, getCell(row, TypologyImportConstants.COL_CARACT_PHYS_CUISSON), "CUISSON"));
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
            if (!StringUtils.hasText(pair[1])) {
                continue;
            }
            ReferenceOpentheso ref = ReferenceOpentheso.builder()
                    .code("AIRE_CIRCULATION")
                    .valeur(limit500(firstNonBlank(pair[0], pair[1])))
                    .url(limit500(pair[1]))
                    .entity(entity)
                    .build();
            entity.getAiresCirculation().add(referenceOpenthesoRepository.save(ref));
        }
    }

    private void replaceAuteursScientifiques(Entity entity, Map<String, String> row, Set<String> csvHeaders) {
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
            AuteurScientifique author = auteurScientifiqueRepository
                    .findFirstByNomIgnoreCaseAndPrenomIgnoreCaseOrderByIdAsc(pn.nom(), pn.prenom())
                    .orElseGet(() -> {
                        AuteurScientifique created = new AuteurScientifique();
                        created.setNom(pn.nom());
                        created.setPrenom(pn.prenom());
                        created.setActive(true);
                        return auteurScientifiqueRepository.save(created);
                    });
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
        if (!StringUtils.hasText(pair[1])) {
            return null;
        }
        ReferenceOpentheso ref = ReferenceOpentheso.builder()
                .code(code)
                .valeur(limit500(firstNonBlank(pair[0], pair[1])))
                .url(limit500(pair[1]))
                .entity(entity)
                .build();
        return referenceOpenthesoRepository.save(ref);
    }

    private void applySlot(Entity entity, String url, String libelle, ReferenceOpenthesoEnum slot,
                           java.util.function.Supplier<ReferenceOpentheso> getter,
                           java.util.function.Consumer<ReferenceOpentheso> setter) {
        ReferenceOpentheso current = getter.get();
        if (!StringUtils.hasText(url)) {
            if (current != null && current.getEntity() != null
                    && Objects.equals(current.getEntity().getId(), entity.getId())) {
                setter.accept(null);
                entityRepository.saveAndFlush(entity);
                referenceOpenthesoRepository.deleteById(current.getId());
            } else if (current != null) {
                setter.accept(null);
                entityRepository.saveAndFlush(entity);
            }
            return;
        }
        if (current != null && current.getEntity() != null
                && Objects.equals(current.getEntity().getId(), entity.getId())) {
            setter.accept(null);
            entityRepository.saveAndFlush(entity);
            referenceOpenthesoRepository.deleteById(current.getId());
        } else if (current != null) {
            setter.accept(null);
            entityRepository.saveAndFlush(entity);
        }
        String valeur = StringUtils.hasText(libelle) ? libelle.trim() : url.trim();
        ReferenceOpentheso ref = ReferenceOpentheso.builder()
                .code(slot.name())
                .valeur(valeur.length() > 500 ? valeur.substring(0, 500) : valeur)
                .url(url.trim().length() > 500 ? url.trim().substring(0, 500) : url.trim())
                .entity(entity)
                .build();
        ReferenceOpentheso saved = referenceOpenthesoRepository.save(ref);
        setter.accept(saved);
    }

    private void ensureMetadataForTypologyImport(Entity entity) {
        if (entity.getMetadata() == null) {
            EntityMetadata em = new EntityMetadata();
            em.setEntity(entity);
            em.setCode(entity.getCode() != null ? entity.getCode() : "");
            entity.setMetadata(em);
        }
    }

    /**
     * Charge l'entité et initialise les collections modifiables (évite fetch multi-bags).
     */
    private Entity loadEntityForImport(Long id) {
        Entity e = entityRepository.findById(id).orElseThrow();
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

    private static void previewImages(Map<String, String> row, List<String> warnings) {
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
            if (!RemoteImageUrlValidator.isValidRemoteImageUrl(url)) {
                warnings.add("Image ignorée (URL invalide ou inaccessible) : " + url);
            }
        }
    }

    private static void previewOpenThesoCeramique(Map<String, String> row, List<String> errors) {
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_DATATION_PERIODE), TypologyImportConstants.COL_DATATION_PERIODE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_PRODUCTION_VALUE), TypologyImportConstants.COL_PRODUCTION_VALUE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_DESCRIPTION_FORM), TypologyImportConstants.COL_DESCRIPTION_FORM, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_DESCRIPTION_FONCTION), TypologyImportConstants.COL_DESCRIPTION_FONCTION, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE), TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_FABRICATION), TypologyImportConstants.COL_CARACT_PHYS_FABRICATION, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_COULEUR_PATE), TypologyImportConstants.COL_CARACT_PHYS_COULEUR_PATE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_NATURE_PATE), TypologyImportConstants.COL_CARACT_PHYS_NATURE_PATE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_INCLUSION), TypologyImportConstants.COL_CARACT_PHYS_INCLUSION, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_CUISSON), TypologyImportConstants.COL_CARACT_PHYS_CUISSON, errors);
    }

    private static void previewOpenThesoMonnaie(Map<String, String> row, List<String> errors) {
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_DATATION_PERIODE), TypologyImportConstants.COL_DATATION_PERIODE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_PRODUCTION_VALUE), TypologyImportConstants.COL_PRODUCTION_VALUE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_MATERIAU), TypologyImportMonnaieConstants.COL_CARACT_PHYS_MATERIAU, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_DENOMINATION), TypologyImportMonnaieConstants.COL_CARACT_PHYS_DENOMINATION, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE), TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_VALEUR), TypologyImportMonnaieConstants.COL_CARACT_PHYS_VALEUR, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportMonnaieConstants.COL_CARACT_PHYS_TECHNIQUE), TypologyImportMonnaieConstants.COL_CARACT_PHYS_TECHNIQUE, errors);
    }

    private static void previewOpenThesoInstrumentum(Map<String, String> row, List<String> errors) {
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_DATATION_PERIODE), TypologyImportConstants.COL_DATATION_PERIODE, errors);
        checkLabelUrlOptional(getCell(row, TypologyImportConstants.COL_PRODUCTION_VALUE), TypologyImportConstants.COL_PRODUCTION_VALUE, errors);
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

    private static void checkLabelUrlOptional(String raw, String col, List<String> errors) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String[] pair = parseLabelUrl(raw);
        if (!StringUtils.hasText(pair[1])) {
            errors.add(col + " doit être au format label:url.");
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

    private Optional<Entity> findCategory(Entity reference, String catCode) {
        return categoryService.loadCategoriesByReference(reference).stream()
                .filter(e -> catCode.equals(e.getCode()))
                .findFirst();
    }

    private Optional<Entity> findGroup(Entity reference, String catCode, String groupCode) {
        Optional<Entity> cat = findCategory(reference, catCode);
        return cat.flatMap(c -> groupService.loadCategoryGroups(c).stream()
                .filter(g -> groupCode.equals(g.getCode()))
                .findFirst());
    }

    private Optional<Entity> findSerie(Entity reference, String catCode, String groupCode, String serieCode) {
        Optional<Entity> grp = findGroup(reference, catCode, groupCode);
        return grp.flatMap(g -> serieService.loadGroupSeries(g).stream()
                .filter(s -> serieCode.equals(s.getCode()))
                .findFirst());
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

    private static Optional<ClassifyResult> classify(String cc, String cg, String cs, String ct, List<String> errors) {
        if (!StringUtils.hasText(cc)) {
            errors.add("code_categorie est obligatoire.");
            return Optional.empty();
        }
        cc = cc.trim();
        if (StringUtils.hasText(ct)) {
            ct = ct.trim();
            if (!StringUtils.hasText(cg)) {
                errors.add("code_groupe est requis lorsque code_type est renseigné.");
                return Optional.empty();
            }
            cg = cg.trim();
            if (StringUtils.hasText(cs)) {
                cs = cs.trim();
                return Optional.of(new ClassifyResult(TypologyImportKind.TYPE_SOUS_SERIE, ct));
            }
            return Optional.of(new ClassifyResult(TypologyImportKind.TYPE_SOUS_GROUPE, ct));
        }
        if (StringUtils.hasText(cs)) {
            if (!StringUtils.hasText(cg)) {
                errors.add("code_groupe est requis lorsque code_serie est renseigné.");
                return Optional.empty();
            }
            cg = cg.trim();
            cs = cs.trim();
            return Optional.of(new ClassifyResult(TypologyImportKind.SERIE, cs));
        }
        if (StringUtils.hasText(cg)) {
            cg = cg.trim();
            return Optional.of(new ClassifyResult(TypologyImportKind.GROUPE, cg));
        }
        return Optional.of(new ClassifyResult(TypologyImportKind.CATEGORIE, cc));
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

    private static String limit500(String v) {
        if (!StringUtils.hasText(v)) {
            return v;
        }
        return v.length() > 500 ? v.substring(0, 500) : v;
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
