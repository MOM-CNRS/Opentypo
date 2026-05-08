package fr.cnrs.opentypo.presentation.bean;

import fr.cnrs.opentypo.application.dto.EntityRevisionDTO;
import fr.cnrs.opentypo.application.service.AuditService;
import fr.cnrs.opentypo.common.constant.EntityConstants;
import fr.cnrs.opentypo.domain.entity.Entity;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bean pour gérer l'affichage de l'historique des modifications d'une entité
 */
@Slf4j
@Getter
@Setter
@SessionScoped
@Named("historyBean")
public class HistoryBean implements Serializable {

    @Inject
    private transient ApplicationBean applicationBean;

    @Inject
    private transient AuditService auditService;

    // Entité sélectionnée pour laquelle on affiche l'historique
    private Entity selectedEntity;

    // Liste des révisions de l'entité
    private List<EntityRevisionDTO> revisions = new ArrayList<>();

    // Révision actuellement sélectionnée pour afficher les détails
    private EntityRevisionDTO selectedRevision;

    // Champs modifiés dans la révision sélectionnée (pour l'affichage des différences)
    private Map<String, Map<String, Object>> changedFields = new HashMap<>();

    /**
     * Charge l'historique d'une entité
     */
    public String loadHistory(Entity entity) {
        if (entity == null || entity.getId() == null) {
            log.warn("Tentative de chargement de l'historique avec une entité null ou sans ID");
            return null;
        }

        this.selectedEntity = entity;
        this.revisions = auditService.getEntityRevisions(entity.getId());
        this.selectedRevision = null;
        this.changedFields = null;

        log.info("Historique chargé pour l'entité {} : {} révisions trouvées",
                entity.getCode(), revisions.size());

        return "/history/history-list.xhtml?faces-redirect=true";
    }

    /**
     * Charge les détails d'une révision spécifique
     */
    public String viewRevision(Long revisionNumber) {
        try {
            if (selectedEntity == null) {
                log.warn("Aucune entité sélectionnée pour afficher la révision {}", revisionNumber);
                return null;
            }

            // Trouver la révision dans la liste
            selectedRevision = revisions.stream()
                    .filter(r -> r.getRevisionNumber().equals(revisionNumber))
                    .findFirst()
                    .orElse(null);

            if (selectedRevision == null) {
                log.warn("Révision {} non trouvée pour l'entité {}", revisionNumber, selectedEntity.getId());
                return null;
            }

            // Calculer les champs modifiés
            changedFields = calculateChangedFields(selectedRevision);
            
            log.info("Détails de la révision {} chargés pour l'entité {} : {} champs modifiés", 
                    revisionNumber, selectedEntity.getId(), changedFields.size());
            
            return "/history/history-detail.xhtml?faces-redirect=true";
        } catch (Exception e) {
            log.error("Erreur lors du chargement des détails de la révision {} pour l'entité {}", 
                    revisionNumber, selectedEntity != null ? selectedEntity.getId() : "null", e);
            return null;
        }
    }

    /**
     * Retourne à l'entité depuis la liste des révisions
     */
    public void backToEntity() throws IOException {
        FacesContext.getCurrentInstance().getExternalContext()
                .redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/index.xhtml");
    }

    /**
     * Retourne à la liste des révisions depuis les détails
     */
    public String backToList() {
        return "/history/history-list.xhtml?faces-redirect=true";
    }

    /**
     * Retourne le libellé du type d'entité pour l'affichage.
     */
    public String getEntityTypeLabel() {
        if (selectedEntity == null || selectedEntity.getEntityType() == null) return "Entité";
        String code = selectedEntity.getEntityType().getCode();
        return switch (code) {
            case EntityConstants.ENTITY_TYPE_REFERENCE -> "Référentiel";
            case EntityConstants.ENTITY_TYPE_CATEGORY -> "Catégorie";
            case EntityConstants.ENTITY_TYPE_GROUP -> "Groupe";
            case EntityConstants.ENTITY_TYPE_SERIES -> "Série";
            case EntityConstants.ENTITY_TYPE_TYPE -> "Type";
            case EntityConstants.ENTITY_TYPE_COLLECTION -> "Collection";
            default -> "Entité";
        };
    }

    /**
     * Retourne le libellé du statut de l'entité pour l'affichage.
     */
    public String getEntityStatusLabel() {
        if (selectedEntity == null || selectedEntity.getStatut() == null) return "—";
        return switch (selectedEntity.getStatut()) {
            case "PUBLIQUE" -> "Public";
            case "PRIVEE" -> "Privé";
            case "PROPOSITION" -> "Proposition";
            case "REFUSE" -> "Refusé";
            default -> selectedEntity.getStatut();
        };
    }

    /**
     * Retourne l'icône PrimeIcons du statut de l'entité.
     */
    public String getEntityStatusIcon() {
        if (selectedEntity == null || selectedEntity.getStatut() == null) return "pi pi-circle";
        return switch (selectedEntity.getStatut()) {
            case "PUBLIQUE" -> "pi pi-globe";
            case "PROPOSITION" -> "pi pi-clock";
            case "REFUSE" -> "pi pi-times-circle";
            default -> "pi pi-lock";
        };
    }

    /**
     * Retourne les noms des champs modifiés pour l'affichage (ex: "Code, TPQ, Statut").
     */
    public String getChangedFieldsDisplay(EntityRevisionDTO revision) {
        if (revision == null) return "—";
        Map<String, Map<String, Object>> changes = calculateChangedFields(revision);
        if (changes.isEmpty()) return "—";
        return changes.keySet().stream()
                .sorted()
                .map(this::getFieldLabel)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    /**
     * Indique si cette révision est la création (chronologiquement la première).
     */
    public boolean isCreationRevision(EntityRevisionDTO revision) {
        if (revision == null || revisions == null || revisions.isEmpty()) return false;
        Long minRev = revisions.stream()
                .map(EntityRevisionDTO::getRevisionNumber)
                .filter(java.util.Objects::nonNull)
                .min(Long::compareTo)
                .orElse(null);
        return minRev != null && minRev.equals(revision.getRevisionNumber());
    }

    /**
     * Retourne le libellé du type de révision pour l'affichage (Création pour la 1re révision).
     */
    public String getDisplayRevisionTypeLabel(EntityRevisionDTO revision) {
        if (revision == null) return "Inconnu";
        return isCreationRevision(revision) ? "Création" : revision.getRevisionTypeLabel();
    }

    /**
     * Retourne l'icône du type de révision pour l'affichage.
     */
    public String getDisplayRevisionTypeIcon(EntityRevisionDTO revision) {
        if (revision == null) return "pi pi-circle";
        return isCreationRevision(revision) ? "pi pi-plus-circle" : revision.getRevisionTypeIcon();
    }

    /**
     * Retourne la classe CSS du type de révision pour l'affichage.
     */
    public String getDisplayRevisionTypeClass(EntityRevisionDTO revision) {
        if (revision == null) return "";
        return isCreationRevision(revision) ? "revision-add" : revision.getRevisionTypeClass();
    }

    private static final DateTimeFormatter REVISION_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    /**
     * Formate la date de révision pour l'affichage (ex : 23/03/2026 à 09:18).
     */
    public String formatRevisionDate(LocalDateTime date) {
        return date != null ? date.format(REVISION_DATE_FORMAT) : "N/A";
    }

    /** Ordre d'affichage des champs (type.xhtml et tous ses includes), sans ID */
    private static final String[] ORDERED_DISPLAY_KEYS = {
        // type.xhtml : en-tête (images avant descriptions, comme la fiche type)
        "code", "labels", "images", "descriptions", "statut", "appellation",
        // datation.xhtml
        "periode", "tpq", "taq", "commentaireDatation",
        // alignements.xhtml
        "interne", "alignementExterne",
        // production.xhtml
        "production", "ateliers", "airesCirculation", "categorieFonctionnelle",
        // description_monnaie.xhtml
        "droit", "legendeDroit", "revers", "legendeRevers",
        // caracteristiques_physique_monnaie.xhtml
        "materiauxMonnaie", "denomination", "metrologieMonnaie", "valeurMonnaie", "techniqueMonnaie",
        // description.xhtml
        "decors", "marques", "fonctionUsage", "denominationInstrumentum",
        // caracteristiques_physique.xhtml (céramique)
        "metrologiePhysique", "fabricationPhysique", "descriptionPate", "couleurPate", "naturePate", "inclusionPate", "cuissonPate",
        // caracteristiques_physique_instrumentum.xhtml
        "materiaux", "forme", "dimensions", "technique",
        // metrologie détail (description_detail)
        "metrologieDetail",
        // attestations, sites, gestion, commentaire, bibliographie
        "attestations", "corpusLies", "sitesArcheologiques", "reference", "typologieScientifique",
        "identifiantPerenne", "ancienneVersion", "commentaireMetadata", "bibliographie", "zoteroItemKeys"
    };

    /**
     * Section d'affichage groupée (comme type.xhtml).
     */
    public record DisplaySection(String id, String title, String icon, List<String> keys, String visibilityType) {}

    /** Sections groupées pour l'affichage (structure type.xhtml). */
    private static final List<DisplaySection> DISPLAY_SECTIONS = List.of(
        new DisplaySection("general", "Informations générales", "pi pi-info-circle",
            List.of("code", "labels", "images", "descriptions", "statut", "appellation"), "ALWAYS"),
        new DisplaySection("datation", "Datation", "pi pi-calendar",
            List.of("periode", "tpq", "taq", "commentaireDatation"), "DATATION"),
        new DisplaySection("alignements", "Relations", "pi pi-sitemap",
            List.of("interne", "alignementExterne"), "ALIGNEMENTS"),
        new DisplaySection("production", "Production", "pi pi-inbox",
            List.of("production", "ateliers", "airesCirculation", "categorieFonctionnelle"), "PRODUCTION"),
        new DisplaySection("descriptionMonnaie", "Description", "pi pi-book",
            List.of("droit", "legendeDroit", "revers", "legendeRevers"), "MONNAIE"),
        new DisplaySection("caracMonnaie", "Caractéristiques physiques", "pi pi-th-large",
            List.of("materiauxMonnaie", "denomination", "metrologieMonnaie", "valeurMonnaie", "techniqueMonnaie"), "MONNAIE"),
        new DisplaySection("description", "Description", "pi pi-book",
            List.of("decors", "marques", "fonctionUsage", "denominationInstrumentum", "metrologieDetail"), "CERAMIQUE_OR_INSTRUMENTUM"),
        new DisplaySection("caracCeramique", "Caractéristiques physiques", "pi pi-sliders-h",
            List.of("metrologiePhysique", "fabricationPhysique", "descriptionPate", "couleurPate", "naturePate", "inclusionPate", "cuissonPate"), "CERAMIQUE"),
        new DisplaySection("caracInstrumentum", "Caractéristiques physiques", "pi pi-th-large",
            List.of("materiaux", "forme", "dimensions", "technique", "fabricationPhysique"), "INSTRUMENTUM"),
        new DisplaySection("attestations", "Attestations", "pi pi-check-circle",
            List.of("attestations", "corpusLies"), "ALWAYS"),
        new DisplaySection("sites", "Sites archéologiques", "pi pi-map-marker",
            List.of("sitesArcheologiques"), "ALWAYS"),
        new DisplaySection("gestion", "Gestion", "pi pi-cog",
            List.of("reference", "typologieScientifique", "identifiantPerenne", "ancienneVersion"), "GESTION"),
        new DisplaySection("commentaire", "Commentaire", "pi pi-comment",
            List.of("commentaireMetadata"), "ALWAYS"),
        new DisplaySection("bibliographie", "Bibliographie", "pi pi-book",
            List.of("bibliographie", "zoteroItemKeys"), "ALWAYS")
    );

    /**
     * Retourne les sections groupées pour l'affichage.
     */
    public List<DisplaySection> getDisplaySections() {
        return DISPLAY_SECTIONS;
    }

    /**
     * Retourne le code du type d'entité (REFERENTIEL, CATEGORIE, GROUPE, SERIE, TYPE).
     */
    private String getEntityTypeCode() {
        if (selectedEntity == null || selectedEntity.getEntityType() == null) return null;
        return selectedEntity.getEntityType().getCode();
    }

    /**
     * Indique si une section doit être affichée, selon le type d'entité et la collection (comme les pages de détail).
     */
    public boolean isSectionVisible(DisplaySection section) {
        if (section == null || selectedEntity == null) return true;
        String entityType = getEntityTypeCode();
        boolean collectionCondition = switch (section.visibilityType()) {
            case "ALWAYS" -> true;
            case "DATATION" -> applicationBean.showDatationBlock();
            case "ALIGNEMENTS" -> applicationBean.showAlignementsBlock();
            case "PRODUCTION" -> applicationBean.showProductionBlock();
            case "MONNAIE" -> applicationBean.isCashTypo();
            case "CERAMIQUE_OR_INSTRUMENTUM" -> applicationBean.isCeramiqueTypo() || applicationBean.isInstrumentumTypo();
            case "CERAMIQUE" -> applicationBean.isCeramiqueTypo();
            case "INSTRUMENTUM" -> applicationBean.isInstrumentumTypo();
            case "GESTION" -> applicationBean.showGestionBlock();
            default -> true;
        };
        if (!collectionCondition) return false;
        // Filtrage par type d'entité (reference.xhtml, category.xhtml, groupe.xhtml, serie.xhtml, type.xhtml)
        return isSectionVisibleForEntityType(section, entityType);
    }

    /**
     * Filtre les sections selon le type d'entité, aligné sur les pages de détail.
     */
    private boolean isSectionVisibleForEntityType(DisplaySection section, String entityType) {
        if (entityType == null) return true;
        return switch (entityType) {
            case EntityConstants.ENTITY_TYPE_REFERENCE -> isSectionVisibleForReference(section);
            case EntityConstants.ENTITY_TYPE_CATEGORY -> isSectionVisibleForCategory(section);
            case EntityConstants.ENTITY_TYPE_GROUP -> isSectionVisibleForGroup(section);
            case EntityConstants.ENTITY_TYPE_SERIES -> isSectionVisibleForSerie(section);
            case EntityConstants.ENTITY_TYPE_TYPE -> true; // type.xhtml : toutes les sections
            default -> true;
        };
    }

    private boolean isSectionVisibleForReference(DisplaySection section) {
        return switch (section.id()) {
            case "general" -> true;
            default -> false; // reference.xhtml : uniquement description, periode, reference biblio, gestionnaires, auteurs
        };
    }

    private boolean isSectionVisibleForCategory(DisplaySection section) {
        return switch (section.id()) {
            case "general", "commentaire", "bibliographie" -> true;
            default -> false; // category.xhtml : description, commentaire, bibliographie, auteurs
        };
    }

    private boolean isSectionVisibleForGroup(DisplaySection section) {
        return switch (section.id()) {
            case "attestations", "sites", "gestion" -> false;
            default -> true; // groupe.xhtml : tout sauf attestations, sites, gestion
        };
    }

    private boolean isSectionVisibleForSerie(DisplaySection section) {
        return switch (section.id()) {
            case "alignements", "attestations", "sites", "gestion" -> false;
            default -> true; // serie.xhtml : tout sauf alignements, attestations, sites, gestion
        };
    }

    /**
     * Retourne les clés à afficher pour une section, selon le type d'entité.
     */
    public List<String> getKeysForSection(DisplaySection section) {
        if (section == null) return List.of();
        String entityType = getEntityTypeCode();
        if (entityType == null) return section.keys();
        if (EntityConstants.ENTITY_TYPE_REFERENCE.equals(entityType) && "general".equals(section.id())) {
            return List.of("code", "labels", "images", "descriptions", "statut", "periode", "rereferenceBibliographique");
        }
        if (EntityConstants.ENTITY_TYPE_CATEGORY.equals(entityType) && "general".equals(section.id())) {
            return List.of("code", "labels", "images", "descriptions", "statut");
        }
        if (EntityConstants.ENTITY_TYPE_GROUP.equals(entityType) && "alignements".equals(section.id())) {
            if (applicationBean.isInstrumentumTypo()) {
                return List.of("alignementExterne");
            }
            return List.of("corpusExterne");
        }
        return section.keys();
    }

    /**
     * Retourne la liste des clés à afficher (ordre type.xhtml, sans ID).
     */
    public List<String> getOrderedDisplayKeys() {
        return List.of(ORDERED_DISPLAY_KEYS);
    }

    /**
     * Retourne l'icône PrimeIcons pour un champ (comme type.xhtml).
     */
    public String getFieldIcon(String fieldName) {
        if (fieldName == null) return "pi pi-circle";
        return switch (fieldName) {
            case "code" -> "pi pi-hashtag";
            case "labels", "descriptions" -> "pi pi-file";
            case "statut" -> "pi pi-info-circle";
            case "appellation" -> "pi pi-tag";
            case "periode" -> "pi pi-calendar";
            case "tpq" -> "pi pi-calendar-minus";
            case "taq" -> "pi pi-calendar-plus";
            case "commentaireDatation" -> "pi pi-comment";
            case "interne" -> "pi pi-users";
            case "alignementExterne" -> "pi pi-globe";
            case "production" -> "pi pi-truck";
            case "ateliers" -> "pi pi-building";
            case "airesCirculation" -> "pi pi-map";
            case "categorieFonctionnelle" -> "pi pi-sitemap";
            case "droit" -> "pi pi-arrow-circle-right";
            case "legendeDroit", "legendeRevers" -> "pi pi-align-left";
            case "revers" -> "pi pi-arrow-circle-left";
            case "materiauxMonnaie", "materiaux" -> "pi pi-box";
            case "denomination" -> "pi pi-id-card";
            case "metrologieMonnaie", "metrologieDetail", "metrologiePhysique" -> "pi pi-sliders-h";
            case "valeurMonnaie" -> "pi pi-wallet";
            case "techniqueMonnaie", "technique" -> "pi pi-wrench";
            case "fabricationPhysique" -> "pi pi-cog";
            case "decors" -> "pi pi-palette";
            case "marques" -> "pi pi-tag";
            case "fonctionUsage" -> "pi pi-sitemap";
            case "denominationInstrumentum" -> "pi pi-tag";
            case "descriptionPate" -> "pi pi-arrows-h";
            case "couleurPate", "naturePate", "inclusionPate", "cuissonPate" -> "pi pi-wrench";
            case "forme" -> "pi pi-sliders-h";
            case "dimensions" -> "pi pi-id-card";
            case "attestations" -> "pi pi-check-circle";
            case "corpusLies" -> "pi pi-book";
            case "sitesArcheologiques" -> "pi pi-map-marker";
            case "commentaireMetadata" -> "pi pi-comment";
            case "bibliographie" -> "pi pi-book";
            case "zoteroItemKeys" -> "pi pi-link";
            case "reference" -> "pi pi-database";
            case "rereferenceBibliographique" -> "pi pi-book";
            case "corpusExterne" -> "pi pi-globe";
            case "typologieScientifique" -> "pi pi-book";
            case "identifiantPerenne" -> "pi pi-id-card";
            case "ancienneVersion" -> "pi pi-history";
            case "images" -> "pi pi-image";
            case "idArk" -> "pi pi-id-card";
            case "displayOrder" -> "pi pi-sort-numeric-down";
            default -> "pi pi-circle";
        };
    }

    /**
     * Retourne le libellé d'un champ pour l'affichage
     */
    public String getFieldLabel(String fieldName) {
        if (fieldName == null) {
            return "Champ inconnu";
        }

        // Gérer les champs avec des points (ex: "labels.fr", "descriptions.en")
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.", 2);
            String baseField = parts[0];
            String languageCode = parts.length > 1 ? parts[1].toUpperCase() : "";
            
            String baseLabel = getBaseFieldLabel(baseField);
            if (!languageCode.isEmpty()) {
                return baseLabel + " (" + languageCode + ")";
            }
            return baseLabel;
        }

        return getBaseFieldLabel(fieldName);
    }

    /**
     * Retourne le libellé de base d'un champ
     */
    private String getBaseFieldLabel(String fieldName) {
        switch (fieldName) {
            case "id":
                return "ID";
            case "code":
                return "Code";
            case "nom":
            case "labels":
                return "Label";
            case "commentaire":
            case "descriptions":
                return "Description";
            case "bibliographie":
                return "Bibliographie";
            case "zoteroItemKeys":
                return "Références Zotero";
            case "appellation":
                return "Appellation usuelle";
            case "reference":
                return "Référentiel";
            case "rereferenceBibliographique":
                return "Référence bibliographique";
            case "corpusExterne":
                return "Corpus externe";
            case "typologieScientifique":
                return "Typologie scientifique";
            case "identifiantPerenne":
                return "Identifiant pérenne";
            case "ancienneVersion":
                return "Ancienne version";
            case "statut":
                return "Statut";
            case "periode":
                return "Période";
            case "tpq":
                return "TPQ";
            case "taq":
                return "TAQ";
            case "commentaireDatation":
                return "Commentaire datation";
            case "publique":
                return "Public";
            case "interne":
                return "Alignement interne";
            case "alignementExterne":
                return "Alignement externe";
            case "production":
                return "Production";
            case "ateliers":
                return "Atelier(s)";
            case "airesCirculation":
                return "Aire de circulation";
            case "categorieFonctionnelle":
                return "Catégorie fonctionnelle";
            case "denominationInstrumentum":
                return "Dénomination";
            case "droit":
                return "Droit";
            case "legendeDroit":
                return "Légende du droit";
            case "revers":
                return "Revers";
            case "legendeRevers":
                return "Légende du revers";
            case "materiauxMonnaie":
                return "Matériau (monnaie)";
            case "denomination":
                return "Dénomination";
            case "metrologieMonnaie":
                return "Métrologie (monnaie)";
            case "valeurMonnaie":
                return "Valeur";
            case "techniqueMonnaie":
                return "Technique (monnaie)";
            case "attestations":
                return "Attestations";
            case "corpusLies":
                return "Corpus lié(s)";
            case "sitesArcheologiques":
                return "Sites archéologiques";
            case "commentaireMetadata":
                return "Commentaire";
            case "metrologieDetail":
                return "Métrologie (détail)";
            case "idArk":
                return "Identifiant ARK";
            case "displayOrder":
                return "Ordre d'affichage";
            case "decors":
                return "Décors";
            case "marques":
                return "Marques/estampilles";
            case "fonctionUsage":
                return "Fonction/usage";
            case "materiaux":
                return "Matériaux";
            case "forme":
                return "Forme";
            case "dimensions":
                return "Dimensions";
            case "technique":
                return "Technique";
            case "metrologiePhysique":
                return "Métrologie";
            case "fabricationPhysique":
                return "Fabrication/façonnage";
            case "descriptionPate":
                return "Description pâte";
            case "couleurPate":
                return "Couleur de pâte";
            case "naturePate":
                return "Nature de pâte";
            case "inclusionPate":
                return "Inclusions";
            case "cuissonPate":
                return "Cuisson/post-cuisson";
            case "images":
                return "Images";
            case "createDate":
                return "Date de création";
            case "createBy":
                return "Créé par";
            case "entityTypeCode":
                return "Type d'entité";
            case "referencesOpentheso":
                return "Références OpenTheso";
            default:
                // Capitaliser la première lettre et remplacer les majuscules par des espaces
                if (fieldName == null || fieldName.isEmpty()) return "Champ inconnu";
                return fieldName.substring(0, 1).toUpperCase() + 
                       fieldName.substring(1).replaceAll("([A-Z])", " $1").trim();
        }
    }

    /** Texte affiché lorsqu'une valeur (avant / après) est vide ou absente dans le diff. */
    private static final String DIFF_EMPTY_PLACEHOLDER = "(vide)";

    /**
     * Formate une valeur pour l'affichage dans le contexte des changements (ancienne/nouvelle valeur).
     * Les valeurs nulles ou vides sont toujours rendues visibles (placeholder) pour que l’ancienne valeur soit lisible.
     */
    public String formatChangeValue(Object value, boolean isOldValue) {
        if (isNullOrEmpty(value)) {
            return DIFF_EMPTY_PLACEHOLDER;
        }
        if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
            return DIFF_EMPTY_PLACEHOLDER;
        }
        if (value instanceof List && ((List<?>) value).isEmpty()) {
            return DIFF_EMPTY_PLACEHOLDER;
        }
        return formatValue(value);
    }

    /**
     * Formate une valeur pour l'affichage (texte brut, sans balises HTML)
     */
    public String formatValue(Object value) {
        if (value == null) {
            return "Aucune valeur";
        }

        // Gérer les Map (pour les labels et descriptions multilingues)
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) value;
            if (map.isEmpty()) {
                return "Aucune valeur";
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(" ; ");
                }
                sb.append(entry.getKey().toUpperCase())
                  .append(": ")
                  .append(entry.getValue() != null ? stripHtml(entry.getValue()) : "N/A");
            }
            return sb.toString();
        }

        // Gérer les List
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "Aucune valeur";
            }
            // Liste d'images (List<Map<String,String>>)
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(" | ");
                    @SuppressWarnings("unchecked")
                    Map<String, String> imgMap = (Map<String, String>) list.get(i);
                    String url = imgMap != null && imgMap.containsKey("url") ? stripHtml(String.valueOf(imgMap.get("url"))) : "";
                    String legende = imgMap != null && imgMap.containsKey("legende") ? stripHtml(String.valueOf(imgMap.get("legende"))) : "";
                    sb.append(url);
                    if (legende != null && !legende.isEmpty()) sb.append(" (").append(legende).append(")");
                }
                return sb.toString();
            }
            return String.join(", ", list.stream()
                    .map(item -> item != null ? stripHtml(item.toString()) : "N/A")
                    .toArray(String[]::new));
        }

        // Gérer les Boolean
        if (value instanceof Boolean) {
            return (Boolean) value ? "Oui" : "Non";
        }

        // Par défaut, convertir en String et retirer les balises HTML éventuelles
        return stripHtml(value.toString());
    }

    /**
     * Version "HTML" pour certains champs dont l'affichage nécessite des liens.
     * Utilisé par la page d'historique (escape=false).
     */
    public String formatValueForField(String key, Object value) {
        if (isNullOrEmpty(value)) {
            return "Aucune valeur";
        }
        if ("corpusLies".equals(key)) {
            return formatCorpusLinksHtml(String.valueOf(value));
        }
        return formatValue(value);
    }

    private String formatCorpusLinksHtml(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Aucune valeur";
        }
        String[] entries = raw.split("[;；]");
        StringBuilder sb = new StringBuilder();
        for (String e : entries) {
            if (e == null) continue;
            String t = e.trim();
            if (t.isEmpty()) continue;
            String[] pair = t.split("\\|", 2);
            if (sb.length() > 0) sb.append(" ; ");
            if (pair.length >= 2) {
                String label = pair[0] != null ? stripHtml(pair[0].trim()) : "";
                String url = pair[1] != null ? stripHtml(pair[1].trim()) : "";
                if (!label.isEmpty() && !url.isEmpty()) {
                    sb.append("<a href=\"")
                      .append(url.replace("\"", "%22"))
                      .append("\" target=\"_blank\" rel=\"noopener noreferrer\">")
                      .append(label)
                      .append("</a>");
                    continue;
                }
            }
            // ancien format / libellé seul
            sb.append(stripHtml(t));
        }
        return sb.length() == 0 ? "Aucune valeur" : sb.toString();
    }

    /**
     * Retire les balises HTML d'une chaîne pour un affichage en texte brut
     */
    private String stripHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String withoutTags = text.replaceAll("<[^>]+>", " ");
        return withoutTags.replaceAll("\\s+", " ").trim();
    }

    /**
     * Calcule les champs modifiés entre la révision actuelle et la précédente
     */
    private Map<String, Map<String, Object>> calculateChangedFields(EntityRevisionDTO revision) {
        Map<String, Map<String, Object>> changes = new HashMap<>();
        
        if (revision == null || revision.getEntityData() == null) {
            return changes;
        }

        Map<String, Object> currentData = revision.getEntityData();
        Map<String, Object> previousData = revision.getPreviousEntityData();

        // Si pas de données précédentes, tous les champs sont nouveaux (création)
        if (previousData == null || previousData.isEmpty()) {
            for (Map.Entry<String, Object> entry : currentData.entrySet()) {
                if ("id".equals(entry.getKey())) continue;  // Ne pas afficher l'ID
                Object newVal = entry.getValue();
                // Ne pas afficher null → "" ou null → null (pas de changement réel)
                if (!isConsideredNoChange(null, newVal)) {
                    Map<String, Object> change = new HashMap<>();
                    change.put("old", null);
                    change.put("new", newVal);
                    changes.put(entry.getKey(), change);
                }
            }
            return changes;
        }

        // Comparer tous les champs de currentData
        for (Map.Entry<String, Object> entry : currentData.entrySet()) {
            String fieldName = entry.getKey();
            if ("id".equals(fieldName)) continue;  // Ne pas afficher l'ID
            Object currentValue = entry.getValue();
            Object previousValue = previousData.get(fieldName);

            // Vérifier si la valeur a changé (exclure null ↔ "" considéré comme sans changement)
            if (!areEqual(currentValue, previousValue) && !isConsideredNoChange(previousValue, currentValue)) {
                Map<String, Object> change = new HashMap<>();
                change.put("old", previousValue);
                change.put("new", currentValue);
                changes.put(fieldName, change);
            }
        }

        // Vérifier les champs qui existent seulement dans previousData (supprimés)
        for (Map.Entry<String, Object> entry : previousData.entrySet()) {
            String fieldName = entry.getKey();
            if ("id".equals(fieldName)) continue;  // Ne pas afficher l'ID
            if (!currentData.containsKey(fieldName)) {
                Object oldVal = entry.getValue();
                // Ne pas afficher "" → null ou null → null (pas de changement réel)
                if (!isConsideredNoChange(oldVal, null)) {
                    Map<String, Object> change = new HashMap<>();
                    change.put("old", oldVal);
                    change.put("new", null);
                    changes.put(fieldName, change);
                }
            }
        }

        // Gestion spéciale pour les Map (labels, descriptions) - créer des entrées individuelles par langue
        for (Map.Entry<String, Object> entry : currentData.entrySet()) {
            String fieldName = entry.getKey();
            Object currentValue = entry.getValue();
            
            if (currentValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> currentMap = (Map<String, String>) currentValue;
                @SuppressWarnings("unchecked")
                Map<String, String> previousMap = previousData.get(fieldName) instanceof Map ? 
                    (Map<String, String>) previousData.get(fieldName) : null;

                // Comparer chaque clé de langue
                for (Map.Entry<String, String> mapEntry : currentMap.entrySet()) {
                    String languageKey = mapEntry.getKey();
                    String currentLangValue = mapEntry.getValue();
                    String previousLangValue = (previousMap != null && previousMap.containsKey(languageKey)) ? 
                        previousMap.get(languageKey) : null;

                    if (!areEqual(currentLangValue, previousLangValue) && !isConsideredNoChange(previousLangValue, currentLangValue)) {
                        String granularFieldName = fieldName + "." + languageKey;
                        Map<String, Object> change = new HashMap<>();
                        change.put("old", previousLangValue);
                        change.put("new", currentLangValue);
                        changes.put(granularFieldName, change);
                        
                        // Retirer l'entrée globale si elle existe
                        changes.remove(fieldName);
                    }
                }

                // Vérifier les clés qui existent seulement dans previousMap
                if (previousMap != null) {
                    for (Map.Entry<String, String> mapEntry : previousMap.entrySet()) {
                        String languageKey = mapEntry.getKey();
                        if (!currentMap.containsKey(languageKey)) {
                            Object oldVal = mapEntry.getValue();
                            if (!isConsideredNoChange(oldVal, null)) {
                                String granularFieldName = fieldName + "." + languageKey;
                                Map<String, Object> change = new HashMap<>();
                                change.put("old", oldVal);
                                change.put("new", null);
                                changes.put(granularFieldName, change);
                                
                                // Retirer l'entrée globale si elle existe
                                changes.remove(fieldName);
                            }
                        }
                    }
                }
            }
        }

        return changes;
    }

    /**
     * Indique si null et chaîne vide ("") sont considérés comme équivalents (aucun changement réel).
     */
    private boolean isConsideredNoChange(Object oldVal, Object newVal) {
        return isNullOrEmpty(oldVal) && isNullOrEmpty(newVal);
    }

    private boolean isNullOrEmpty(Object o) {
        if (o == null) return true;
        if (o instanceof String) return ((String) o).trim().isEmpty();
        return false;
    }

    /**
     * Compare deux valeurs pour déterminer si elles sont égales
     */
    private boolean areEqual(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }
        
        // Comparaison spéciale pour les Map
        if (value1 instanceof Map && value2 instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<?, ?> map1 = (Map<?, ?>) value1;
            @SuppressWarnings("unchecked")
            Map<?, ?> map2 = (Map<?, ?>) value2;
            return map1.equals(map2);
        }
        
        return value1.equals(value2);
    }
}
